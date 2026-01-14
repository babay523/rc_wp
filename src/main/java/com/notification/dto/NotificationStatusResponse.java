package com.notification.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 通知状态查询响应 DTO
 * 
 * @author Notification System
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class NotificationStatusResponse {
    
    /**
     * 通知任务ID
     */
    private String notificationId;
    
    /**
     * 供应商编码
     */
    private String vendorCode;
    
    /**
     * 目标URL
     */
    private String targetUrl;
    
    /**
     * HTTP方法
     */
    private String httpMethod;
    
    /**
     * 任务状态
     */
    private String status;
    
    /**
     * 当前重试次数
     */
    private Integer retryCount;
    
    /**
     * 最大重试次数
     */
    private Integer maxRetry;
    
    /**
     * 最近错误代码
     */
    private String lastErrorCode;
    
    /**
     * 最近错误信息
     */
    private String lastErrorMessage;
    
    /**
     * 创建时间
     */
    private LocalDateTime createdAt;
    
    /**
     * 更新时间
     */
    private LocalDateTime updatedAt;
    
    /**
     * 最近一次尝试时间
     */
    private LocalDateTime lastAttemptAt;
}
