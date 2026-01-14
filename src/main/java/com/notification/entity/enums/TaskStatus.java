package com.notification.entity.enums;

import lombok.Getter;

/**
 * 任务状态枚举
 * 
 * @author Notification System
 */
@Getter
public enum TaskStatus {
    
    PENDING("PENDING", "待处理"),
    RETRYING("RETRYING", "重试中"),
    SUCCESS("SUCCESS", "成功"),
    FAILED("FAILED", "失败");
    
    private final String code;
    private final String description;
    
    TaskStatus(String code, String description) {
        this.code = code;
        this.description = description;
    }
    
    /**
     * 根据代码获取枚举
     */
    public static TaskStatus fromCode(String code) {
        for (TaskStatus status : values()) {
            if (status.code.equals(code)) {
                return status;
            }
        }
        throw new IllegalArgumentException("Unknown task status code: " + code);
    }
}
