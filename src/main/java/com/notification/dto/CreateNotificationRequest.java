package com.notification.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Positive;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;

/**
 * 创建通知请求 DTO
 * 
 * @author Notification System
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreateNotificationRequest {
    
    /**
     * 供应商编码（可选，与 targetUrl 二选一）
     */
    private String vendorCode;
    
    /**
     * 目标回调地址（可选，与 vendorCode 二选一）
     */
    private String targetUrl;
    
    /**
     * HTTP 方法（必填）
     */
    @NotBlank(message = "httpMethod is required")
    @Pattern(regexp = "GET|POST|PUT|DELETE", message = "httpMethod must be one of: GET, POST, PUT, DELETE")
    private String httpMethod;
    
    /**
     * 请求头（可选）
     */
    private Map<String, String> headers;
    
    /**
     * 请求体（必填）
     */
    @NotNull(message = "body is required")
    private Object body;
    
    /**
     * 最大重试次数（可选，默认使用供应商配置或系统默认值）
     */
    @Positive(message = "maxRetry must be positive")
    private Integer maxRetry;
    
    /**
     * 回调超时时间（毫秒）（可选，默认使用供应商配置或系统默认值）
     */
    @Positive(message = "callbackTimeoutMs must be positive")
    private Integer callbackTimeoutMs;
    
    /**
     * 业务幂等ID（可选，用于防重复）
     */
    private String eventId;
}
