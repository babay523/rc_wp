package com.notification.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 错误响应 DTO
 * 
 * @author Notification System
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ErrorResponse {
    
    /**
     * 错误代码
     */
    private String error;
    
    /**
     * 错误消息
     */
    private String message;
    
    /**
     * 详细错误信息列表
     */
    private List<FieldError> details;
    
    /**
     * 时间戳
     */
    @Builder.Default
    private LocalDateTime timestamp = LocalDateTime.now();
    
    /**
     * 字段错误详情
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class FieldError {
        /**
         * 字段名
         */
        private String field;
        
        /**
         * 错误消息
         */
        private String message;
    }
    
    /**
     * 创建简单错误响应
     */
    public static ErrorResponse of(String error, String message) {
        return ErrorResponse.builder()
                .error(error)
                .message(message)
                .build();
    }
}
