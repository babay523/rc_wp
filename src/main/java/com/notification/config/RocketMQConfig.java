package com.notification.config;

import org.springframework.context.annotation.Configuration;

/**
 * RocketMQ 配置类
 * 
 * @author Notification System
 * 
 * RocketMQ 配置通过 application.yml 自动配置
 * 需要配置 rocketmq.name-server 和 rocketmq.producer.group
 */
@Configuration
public class RocketMQConfig {
    // RocketMQ 通过 Spring Boot Auto-Configuration 自动配置
    // 配置项在 application-dev.yml 中定义
}
