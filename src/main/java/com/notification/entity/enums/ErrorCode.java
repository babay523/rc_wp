package com.notification.entity.enums;

import lombok.Getter;

/**
 * 错误码枚举
 * 
 * @author Notification System
 */
@Getter
public enum ErrorCode {
    
    // 客户端错误 (4xx)
    VALIDATION_ERROR("VALIDATION_ERROR", "Request validation failed"),
    RESOURCE_NOT_FOUND("RESOURCE_NOT_FOUND", "Resource not found"),
    IDEMPOTENCY_CONFLICT("IDEMPOTENCY_CONFLICT", "Duplicate eventId detected"),
    VENDOR_DISABLED("VENDOR_DISABLED", "Vendor is disabled"),
    
    // 服务端错误 (5xx)
    DATABASE_ERROR("DATABASE_ERROR", "Database operation failed"),
    MQ_ERROR("MQ_ERROR", "Message queue operation failed"),
    INTERNAL_ERROR("INTERNAL_ERROR", "Internal server error"),
    
    // 外部调用错误
    HTTP_TIMEOUT("HTTP_TIMEOUT", "External API call timeout"),
    HTTP_4XX("HTTP_4XX", "External API returned 4xx error"),
    HTTP_5XX("HTTP_5XX", "External API returned 5xx error"),
    NETWORK_ERROR("NETWORK_ERROR", "Network connection failed");
    
    private final String code;
    private final String message;
    
    ErrorCode(String code, String message) {
        this.code = code;
        this.message = message;
    }
}
