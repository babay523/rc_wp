package com.notification.service;

import com.notification.entity.NotificationTask;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * 重试策略服务
 * 
 * @author Notification System
 */
@Slf4j
@Service
public class RetryPolicyService {
    
    /**
     * 判断是否应该重试
     */
    public boolean shouldRetry(NotificationTask task) {
        return task.getRetryCount() < task.getMaxRetry();
    }
    
    /**
     * 计算延迟秒数（指数退避算法）
     * delaySeconds = min(60 * 2^retryCount, 3600)
     */
    public int calculateDelaySeconds(int retryCount) {
        int delaySeconds = (int) (60 * Math.pow(2, retryCount));
        return Math.min(delaySeconds, 3600);
    }
    
    /**
     * 将延迟秒数映射到 RocketMQ 延迟级别
     * 
     * RocketMQ 延迟级别:
     * 1=1s, 2=5s, 3=10s, 4=30s, 5=1min, 6=2min, 7=3min, 8=4min,
     * 9=5min, 10=6min, 11=7min, 12=8min, 13=9min, 14=10min,
     * 15=20min, 16=30min, 17=1h, 18=2h
     */
    public int mapToRocketMQDelayLevel(int delaySeconds) {
        if (delaySeconds <= 1) return 1;
        if (delaySeconds <= 5) return 2;
        if (delaySeconds <= 10) return 3;
        if (delaySeconds <= 30) return 4;
        if (delaySeconds <= 60) return 5;
        if (delaySeconds <= 120) return 6;
        if (delaySeconds <= 180) return 7;
        if (delaySeconds <= 240) return 8;
        if (delaySeconds <= 300) return 9;
        if (delaySeconds <= 360) return 10;
        if (delaySeconds <= 420) return 11;
        if (delaySeconds <= 480) return 12;
        if (delaySeconds <= 540) return 13;
        if (delaySeconds <= 600) return 14;
        if (delaySeconds <= 1200) return 15;
        if (delaySeconds <= 1800) return 16;
        if (delaySeconds <= 3600) return 17;
        return 18; // > 1h
    }
}
