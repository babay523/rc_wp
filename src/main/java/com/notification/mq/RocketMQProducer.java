package com.notification.mq;

import com.notification.dto.NotificationMessage;
import com.notification.util.JsonUtil;
import lombok.extern.slf4j.Slf4j;
import org.apache.rocketmq.spring.core.RocketMQTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.stereotype.Component;

/**
 * RocketMQ 消息生产者
 * 
 * 当配置 notification.mq.mode=rocketmq 时启用
 * 
 * @author Notification System
 */
@Slf4j
@Component
@ConditionalOnProperty(name = "notification.mq.mode", havingValue = "rocketmq")
@ConditionalOnBean(RocketMQTemplate.class)
public class RocketMQProducer {
    
    @Autowired
    private RocketMQTemplate rocketMQTemplate;
    
    @Value("${rocketmq.topic.notification:notification-task}")
    private String topic;
    
    /**
     * 发送普通消息
     */
    public void sendMessage(NotificationMessage message) {
        try {
            rocketMQTemplate.syncSend(topic, MessageBuilder.withPayload(message).build());
            log.info("Sent notification message to RocketMQ: notificationId={}, retryCount={}", 
                    message.getNotificationId(), message.getRetryCount());
        } catch (Exception e) {
            log.error("Failed to send message to RocketMQ: notificationId={}", 
                    message.getNotificationId(), e);
            throw new RuntimeException("MQ send failed", e);
        }
    }
    
    /**
     * 发送延迟消息
     * 
     * @param message 消息内容
     * @param delayLevel RocketMQ 延迟级别 (1-18)
     */
    public void sendDelayMessage(NotificationMessage message, int delayLevel) {
        try {
            rocketMQTemplate.syncSend(
                    topic, 
                    MessageBuilder.withPayload(message).build(),
                    3000,
                    delayLevel
            );
            log.info("Sent delayed notification message to RocketMQ: notificationId={}, retryCount={}, delayLevel={}", 
                    message.getNotificationId(), message.getRetryCount(), delayLevel);
        } catch (Exception e) {
            log.error("Failed to send delayed message to RocketMQ: notificationId={}", 
                    message.getNotificationId(), e);
            throw new RuntimeException("MQ send failed", e);
        }
    }
}
