package com.notification;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * API 通知系统主启动类
 * 
 * @author Notification System
 * @version 1.0.0
 */
@SpringBootApplication
@MapperScan("com.notification.mapper")
@EnableScheduling
@EnableAsync
public class NotificationSystemApplication {

    public static void main(String[] args) {
        SpringApplication.run(NotificationSystemApplication.class, args);
    }
}
