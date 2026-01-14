package com.notification.service;

import com.notification.dto.NotificationMessage;
import com.notification.entity.NotificationTask;
import com.notification.entity.enums.ErrorCode;
import com.notification.entity.enums.TaskStatus;
import com.notification.mapper.NotificationTaskMapper;
import com.notification.mq.MockRocketMQProducer;
import com.notification.mq.RocketMQProducer;
import com.notification.service.HttpClientService.HttpResponse;
import com.notification.util.JsonUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpMethod;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Map;

/**
 * 通知投递核心服务
 * 
 * @author Notification System
 */
@Slf4j
@Service
public class NotificationDispatcher {
    
    @Autowired
    private NotificationTaskMapper taskMapper;
    
    @Autowired
    private HttpClientService httpClientService;
    
    @Autowired
    private RetryPolicyService retryPolicyService;
    
    @Autowired(required = false)
    private RocketMQProducer rocketMQProducer;
    
    @Autowired(required = false)
    private MockRocketMQProducer mockRocketMQProducer;
    
    /**
     * 投递通知
     */
    @Transactional(rollbackFor = Exception.class)
    public void dispatch(String notificationId) {
        // 1. 加载任务详情
        NotificationTask task = taskMapper.selectById(notificationId);
        if (task == null) {
            log.error("Notification task not found: {}", notificationId);
            return;
        }
        
        log.info("Dispatching notification: notificationId={}, retryCount={}, targetUrl={}", 
                task.getId(), task.getRetryCount(), task.getTargetUrl());
        
        // 2. 构建 HTTP 请求参数
        HttpMethod httpMethod = HttpMethod.valueOf(task.getHttpMethod());
        Map<String, String> headers = JsonUtil.fromJson(task.getHeadersJson(), Map.class);
        String body = task.getBodyJson();
        
        // 3. 调用外部 API
        HttpResponse response = httpClientService.call(
                task.getTargetUrl(),
                httpMethod,
                headers,
                body,
                task.getCallbackTimeoutMs()
        );
        
        // 4. 更新最后尝试时间
        task.setLastAttemptAt(LocalDateTime.now());
        
        // 5. 根据响应处理结果
        if (response.isSuccess()) {
            handleSuccess(task, response);
        } else if (response.isClientError()) {
            handleFailure(task, response, false); // 4xx 不重试
        } else {
            handleFailure(task, response, true); // 5xx 和超时需要重试
        }
    }
    
    /**
     * 处理成功响应
     */
    private void handleSuccess(NotificationTask task, HttpResponse response) {
        task.setStatus(TaskStatus.SUCCESS.getCode());
        task.setLastErrorCode(null);
        task.setLastErrorMessage(null);
        taskMapper.updateById(task);
        
        log.info("Notification dispatched successfully: notificationId={}, statusCode={}, costMs={}", 
                task.getId(), response.getStatusCode(), response.getCostMs());
    }
    
    /**
     * 处理失败响应
     */
    private void handleFailure(NotificationTask task, HttpResponse response, boolean shouldRetry) {
        // 记录错误信息
        String errorCode = response.isTimeout() ? 
                ErrorCode.HTTP_TIMEOUT.getCode() : 
                (response.isServerError() ? ErrorCode.HTTP_5XX.getCode() : ErrorCode.HTTP_4XX.getCode());
        
        task.setLastErrorCode(errorCode);
        task.setLastErrorMessage(truncateErrorMessage(response.getErrorMessage()));
        
        // 判断是否需要重试
        if (shouldRetry && retryPolicyService.shouldRetry(task)) {
            handleRetry(task);
        } else {
            // 达到最大重试次数或不应重试，标记为失败
            task.setStatus(TaskStatus.FAILED.getCode());
            taskMapper.updateById(task);
            
            log.warn("Notification failed permanently: notificationId={}, retryCount={}, errorCode={}", 
                    task.getId(), task.getRetryCount(), errorCode);
        }
    }
    
    /**
     * 处理重试逻辑
     */
    private void handleRetry(NotificationTask task) {
        // 增加重试计数
        task.setRetryCount(task.getRetryCount() + 1);
        task.setStatus(TaskStatus.RETRYING.getCode());
        taskMapper.updateById(task);
        
        // 计算延迟时间
        int delaySeconds = retryPolicyService.calculateDelaySeconds(task.getRetryCount());
        int delayLevel = retryPolicyService.mapToRocketMQDelayLevel(delaySeconds);
        
        // 构建消息
        NotificationMessage message = NotificationMessage.builder()
                .notificationId(task.getId())
                .vendorCode(task.getVendorCode())
                .retryCount(task.getRetryCount())
                .build();
        
        // 发送延迟消息到队列
        if (rocketMQProducer != null) {
            rocketMQProducer.sendDelayMessage(message, delayLevel);
            log.info("Scheduled retry for notification (RocketMQ): notificationId={}, retryCount={}, delaySeconds={}, delayLevel={}", 
                    task.getId(), task.getRetryCount(), delaySeconds, delayLevel);
        } else if (mockRocketMQProducer != null) {
            mockRocketMQProducer.sendDelayMessage(message, delayLevel);
            log.info("Scheduled retry for notification (Mock MQ): notificationId={}, retryCount={}, delaySeconds={}, delayLevel={}", 
                    task.getId(), task.getRetryCount(), delaySeconds, delayLevel);
        } else {
            log.warn("No MQ producer available, retry will not be scheduled automatically: notificationId={}, retryCount={}", 
                    task.getId(), task.getRetryCount());
        }
    }
    
    /**
     * 截断错误消息（最多保留 500 字符）
     */
    private String truncateErrorMessage(String message) {
        if (message == null) {
            return null;
        }
        return message.length() > 500 ? message.substring(0, 500) + "..." : message;
    }
}
