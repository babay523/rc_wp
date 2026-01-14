package com.notification.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 创建通知响应 DTO
 * 
 * @author Notification System
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreateNotificationResponse {
    
    /**
     * 通知任务ID
     */
    private String notificationId;
    
    /**
     * 状态（固定为 ACCEPTED）
     */
    private String status;
    
    /**
     * 创建成功响应
     */
    public static CreateNotificationResponse accepted(String notificationId) {
        return CreateNotificationResponse.builder()
                .notificationId(notificationId)
                .status("ACCEPTED")
                .build();
    }
}
