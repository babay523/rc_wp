package com.notification.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

/**
 * 通知消息 DTO
 * 用于 RocketMQ 消息传递
 * 
 * @author Notification System
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class NotificationMessage implements Serializable {
    
    private static final long serialVersionUID = 1L;
    
    /**
     * 通知任务ID
     */
    private String notificationId;
    
    /**
     * 供应商编码
     */
    private String vendorCode;
    
    /**
     * 当前重试次数
     */
    @Builder.Default
    private Integer retryCount = 0;
}
