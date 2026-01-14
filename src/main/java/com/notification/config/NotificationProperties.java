package com.notification.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * 通知系统配置属性
 * 
 * @author Notification System
 */
@Data
@Component
@ConfigurationProperties(prefix = "notification")
public class NotificationProperties {
    
    /**
     * 消息队列配置
     */
    private MqConfig mq = new MqConfig();
    
    @Data
    public static class MqConfig {
        /**
         * MQ 模式：mock 或 rocketmq
         */
        private String mode = "mock";
        
        /**
         * Mock 模式配置
         */
        private MockConfig mock = new MockConfig();
    }
    
    @Data
    public static class MockConfig {
        /**
         * 延迟时间缩放因子
         */
        private int delayScaleFactor = 10;
        
        /**
         * 是否启用异步投递
         */
        private boolean asyncDispatch = true;
    }
}
