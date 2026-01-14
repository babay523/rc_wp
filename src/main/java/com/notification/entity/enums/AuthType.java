package com.notification.entity.enums;

import lombok.Getter;

/**
 * 认证类型枚举
 * 
 * @author Notification System
 */
@Getter
public enum AuthType {
    
    NONE("NONE", "无认证"),
    TOKEN("TOKEN", "Token认证"),
    BASIC("BASIC", "Basic认证"),
    HMAC("HMAC", "HMAC签名");
    
    private final String code;
    private final String description;
    
    AuthType(String code, String description) {
        this.code = code;
        this.description = description;
    }
    
    /**
     * 根据代码获取枚举
     */
    public static AuthType fromCode(String code) {
        for (AuthType type : values()) {
            if (type.code.equals(code)) {
                return type;
            }
        }
        throw new IllegalArgumentException("Unknown auth type code: " + code);
    }
}
