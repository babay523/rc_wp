package com.notification.util;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Random;

/**
 * ID 生成器工具类
 * 
 * @author Notification System
 */
public class IdGenerator {
    
    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("yyyyMMddHHmmss");
    private static final Random RANDOM = new Random();
    
    /**
     * 生成通知任务ID
     * 格式: ntf_yyyyMMddHHmmss_随机数
     */
    public static String generateNotificationId() {
        String timestamp = LocalDateTime.now().format(FORMATTER);
        int randomNum = RANDOM.nextInt(10000);
        return String.format("ntf_%s_%04d", timestamp, randomNum);
    }
}
