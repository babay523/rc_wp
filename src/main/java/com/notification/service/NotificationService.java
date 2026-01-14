package com.notification.service;

import com.notification.dto.CreateNotificationRequest;
import com.notification.dto.CreateNotificationResponse;
import com.notification.dto.NotificationMessage;
import com.notification.dto.NotificationStatusResponse;
import com.notification.entity.NotificationTask;
import com.notification.entity.VendorConfig;
import com.notification.entity.enums.ErrorCode;
import com.notification.entity.enums.TaskStatus;
import com.notification.mapper.NotificationTaskMapper;
import com.notification.mq.MockRocketMQProducer;
import com.notification.mq.RocketMQProducer;
import com.notification.util.IdGenerator;
import com.notification.util.JsonUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.Arrays;
import java.util.List;

/**
 * 通知服务
 * 
 * @author Notification System
 */
@Slf4j
@Service
public class NotificationService {
    
    private final NotificationTaskMapper taskMapper;
    private final VendorConfigService vendorConfigService;
    
    @Autowired(required = false)
    private RocketMQProducer rocketMQProducer;
    
    @Autowired(required = false)
    private MockRocketMQProducer mockRocketMQProducer;
    
    @Autowired
    public NotificationService(
            NotificationTaskMapper taskMapper, 
            VendorConfigService vendorConfigService) {
        this.taskMapper = taskMapper;
        this.vendorConfigService = vendorConfigService;
    }
    
    /**
     * 创建通知任务
     */
    @Transactional(rollbackFor = Exception.class)
    public CreateNotificationResponse createNotification(CreateNotificationRequest request) {
        // 1. 参数验证
        validateRequest(request);
        
        // 2. 幂等性检查
        if (StringUtils.hasText(request.getEventId())) {
            checkIdempotency(request.getEventId());
        }
        
        // 3. 查询供应商配置并合并默认值
        VendorConfig vendorConfig = null;
        if (StringUtils.hasText(request.getVendorCode())) {
            vendorConfig = vendorConfigService.getVendorConfig(request.getVendorCode());
            if (vendorConfig == null || !vendorConfig.getEnabled()) {
                throw new IllegalArgumentException(ErrorCode.VENDOR_DISABLED.getMessage());
            }
        }
        
        // 4. 创建通知任务
        NotificationTask task = buildNotificationTask(request, vendorConfig);
        
        // 5. 持久化到数据库
        taskMapper.insert(task);
        log.info("Created notification task: {}", task.getId());
        
        // 6. 发送消息到 RocketMQ（或 Mock）
        try {
            NotificationMessage message = NotificationMessage.builder()
                    .notificationId(task.getId())
                    .vendorCode(task.getVendorCode())
                    .retryCount(0)
                    .build();
            
            if (rocketMQProducer != null) {
                rocketMQProducer.sendMessage(message);
                log.info("Sent message to RocketMQ: {}", task.getId());
            } else if (mockRocketMQProducer != null) {
                mockRocketMQProducer.sendMessage(message);
                log.info("Sent message to Mock MQ (will be dispatched asynchronously): {}", task.getId());
            } else {
                log.warn("No MQ producer available, task will not be dispatched automatically: {}", task.getId());
            }
        } catch (Exception e) {
            // MQ 发送失败不影响接口响应，任务已持久化，可通过定时任务补偿
            log.error("Failed to send message to MQ, but task is persisted: {}", task.getId(), e);
        }
        
        // 7. 返回响应
        return CreateNotificationResponse.accepted(task.getId());
    }
    
    /**
     * 查询通知状态
     */
    public NotificationStatusResponse getNotificationStatus(String notificationId) {
        NotificationTask task = taskMapper.selectById(notificationId);
        if (task == null) {
            return null;
        }
        
        return NotificationStatusResponse.builder()
                .notificationId(task.getId())
                .vendorCode(task.getVendorCode())
                .targetUrl(task.getTargetUrl())
                .httpMethod(task.getHttpMethod())
                .status(task.getStatus())
                .retryCount(task.getRetryCount())
                .maxRetry(task.getMaxRetry())
                .lastErrorCode(task.getLastErrorCode())
                .lastErrorMessage(task.getLastErrorMessage())
                .createdAt(task.getCreatedAt())
                .updatedAt(task.getUpdatedAt())
                .lastAttemptAt(task.getLastAttemptAt())
                .build();
    }
    
    /**
     * 幂等性检查
     */
    public void checkIdempotency(String eventId) {
        List<String> inProgressStatuses = Arrays.asList(
                TaskStatus.PENDING.getCode(),
                TaskStatus.RETRYING.getCode()
        );
        
        NotificationTask existingTask = taskMapper.selectByEventIdAndStatusIn(eventId, inProgressStatuses);
        if (existingTask != null) {
            throw new IllegalStateException(
                    String.format("Duplicate eventId detected. Existing notificationId: %s", 
                            existingTask.getId())
            );
        }
    }
    
    /**
     * 验证请求参数
     */
    private void validateRequest(CreateNotificationRequest request) {
        // targetUrl 和 vendorCode 至少提供一个
        if (!StringUtils.hasText(request.getTargetUrl()) && 
            !StringUtils.hasText(request.getVendorCode())) {
            throw new IllegalArgumentException("Either targetUrl or vendorCode must be provided");
        }
    }
    
    /**
     * 构建通知任务实体
     */
    private NotificationTask buildNotificationTask(
            CreateNotificationRequest request, 
            VendorConfig vendorConfig) {
        
        NotificationTask task = new NotificationTask();
        task.setId(IdGenerator.generateNotificationId());
        task.setVendorCode(request.getVendorCode());
        
        // 设置 targetUrl
        if (StringUtils.hasText(request.getTargetUrl())) {
            task.setTargetUrl(request.getTargetUrl());
        } else if (vendorConfig != null) {
            task.setTargetUrl(vendorConfig.getBaseUrl() + vendorConfig.getDefaultPath());
        }
        
        task.setHttpMethod(request.getHttpMethod());
        task.setHeadersJson(JsonUtil.toJson(request.getHeaders()));
        task.setBodyJson(JsonUtil.toJson(request.getBody()));
        task.setStatus(TaskStatus.PENDING.getCode());
        task.setRetryCount(0);
        
        // 设置 maxRetry
        if (request.getMaxRetry() != null) {
            task.setMaxRetry(request.getMaxRetry());
        } else if (vendorConfig != null) {
            task.setMaxRetry(vendorConfig.getDefaultMaxRetry());
        } else {
            task.setMaxRetry(5); // 默认值
        }
        
        // 设置 callbackTimeoutMs
        if (request.getCallbackTimeoutMs() != null) {
            task.setCallbackTimeoutMs(request.getCallbackTimeoutMs());
        } else if (vendorConfig != null) {
            task.setCallbackTimeoutMs(vendorConfig.getDefaultTimeoutMs());
        } else {
            task.setCallbackTimeoutMs(3000); // 默认值
        }
        
        task.setEventId(request.getEventId());
        
        return task;
    }
}
