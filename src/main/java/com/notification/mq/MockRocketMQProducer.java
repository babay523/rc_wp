package com.notification.mq;

import com.notification.config.NotificationProperties;
import com.notification.dto.NotificationMessage;
import com.notification.service.NotificationDispatcher;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Lazy;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

/**
 * Mock RocketMQ 生产者（用于本地测试，不依赖真实 RocketMQ）
 * 
 * 当配置 notification.mq.mode=mock 时启用
 * 
 * @author Notification System
 */
@Slf4j
@Component
@ConditionalOnProperty(name = "notification.mq.mode", havingValue = "mock", matchIfMissing = true)
public class MockRocketMQProducer {
    
    @Lazy
    @Autowired
    private NotificationDispatcher dispatcher;
    
    @Autowired
    private NotificationProperties properties;
    
    /**
     * 发送普通消息（立即触发投递）
     */
    @Async
    public void sendMessage(NotificationMessage message) {
        log.info("[MOCK MQ] Sending notification message: notificationId={}, retryCount={}", 
                message.getNotificationId(), message.getRetryCount());
        
        if (!properties.getMq().getMock().isAsyncDispatch()) {
            log.info("[MOCK MQ] Async dispatch is disabled, skipping automatic dispatch");
            return;
        }
        
        // 模拟异步处理，立即触发投递
        try {
            Thread.sleep(100); // 模拟网络延迟
            dispatcher.dispatch(message.getNotificationId());
        } catch (Exception e) {
            log.error("[MOCK MQ] Failed to dispatch notification: notificationId={}", 
                    message.getNotificationId(), e);
        }
    }
    
    /**
     * 发送延迟消息（模拟延迟后触发投递）
     * 
     * @param message 消息内容
     * @param delayLevel RocketMQ 延迟级别 (1-18)
     */
    @Async
    public void sendDelayMessage(NotificationMessage message, int delayLevel) {
        int delaySeconds = mapDelayLevelToSeconds(delayLevel);
        int scaleFactor = properties.getMq().getMock().getDelayScaleFactor();
        int actualDelayMs = (delaySeconds * 1000) / scaleFactor;
        
        log.info("[MOCK MQ] Sending delayed notification message: notificationId={}, retryCount={}, delayLevel={}, originalDelaySeconds={}, actualDelayMs={}", 
                message.getNotificationId(), message.getRetryCount(), delayLevel, delaySeconds, actualDelayMs);
        
        if (!properties.getMq().getMock().isAsyncDispatch()) {
            log.info("[MOCK MQ] Async dispatch is disabled, skipping automatic dispatch");
            return;
        }
        
        // 模拟延迟处理
        try {
            Thread.sleep(actualDelayMs);
            dispatcher.dispatch(message.getNotificationId());
        } catch (Exception e) {
            log.error("[MOCK MQ] Failed to dispatch delayed notification: notificationId={}", 
                    message.getNotificationId(), e);
        }
    }
    
    /**
     * 将 RocketMQ 延迟级别映射为秒数
     */
    private int mapDelayLevelToSeconds(int delayLevel) {
        switch (delayLevel) {
            case 1: return 1;
            case 2: return 5;
            case 3: return 10;
            case 4: return 30;
            case 5: return 60;
            case 6: return 120;
            case 7: return 180;
            case 8: return 240;
            case 9: return 300;
            case 10: return 360;
            case 11: return 420;
            case 12: return 480;
            case 13: return 540;
            case 14: return 600;
            case 15: return 1200;
            case 16: return 1800;
            case 17: return 3600;
            case 18: return 7200;
            default: return 60;
        }
    }
}
