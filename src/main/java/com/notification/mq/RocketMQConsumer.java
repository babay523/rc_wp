package com.notification.mq;

import com.notification.dto.NotificationMessage;
import com.notification.service.NotificationDispatcher;
import lombok.extern.slf4j.Slf4j;
import org.apache.rocketmq.spring.annotation.RocketMQMessageListener;
import org.apache.rocketmq.spring.core.RocketMQListener;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * RocketMQ 消息消费者
 * 
 * @author Notification System
 */
@Slf4j
@Component
@RocketMQMessageListener(
        topic = "${rocketmq.topic.notification:notification-task}",
        consumerGroup = "${rocketmq.consumer.group:notification-dispatcher-group}"
)
public class RocketMQConsumer implements RocketMQListener<NotificationMessage> {
    
    @Autowired
    private NotificationDispatcher dispatcher;
    
    @Override
    public void onMessage(NotificationMessage message) {
        log.info("Received notification message from MQ: notificationId={}, retryCount={}", 
                message.getNotificationId(), message.getRetryCount());
        
        try {
            dispatcher.dispatch(message.getNotificationId());
        } catch (Exception e) {
            log.error("Failed to dispatch notification: notificationId={}", 
                    message.getNotificationId(), e);
            // RocketMQ 会自动重投消息
            throw new RuntimeException("Dispatch failed", e);
        }
    }
}
