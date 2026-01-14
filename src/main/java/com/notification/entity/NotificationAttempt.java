package com.notification.entity;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 通知尝试记录实体类（可选）
 * 对应数据库表: notification_attempt
 * 用于记录每一次对外调用的详细情况
 * 
 * @author Notification System
 */
@Data
@TableName("notification_attempt")
public class NotificationAttempt {
    
    /**
     * 尝试记录ID
     */
    @TableId(type = IdType.AUTO)
    private Long id;
    
    /**
     * 关联通知任务ID
     */
    @TableField("notification_id")
    private String notificationId;
    
    /**
     * 第几次尝试(从1开始)
     */
    @TableField("attempt_no")
    private Integer attemptNo;
    
    /**
     * 本次请求头
     */
    @TableField("request_headers")
    private String requestHeaders;
    
    /**
     * 本次请求体
     */
    @TableField("request_body")
    private String requestBody;
    
    /**
     * HTTP状态码
     */
    @TableField("response_status")
    private Integer responseStatus;
    
    /**
     * 响应体
     */
    @TableField("response_body")
    private String responseBody;
    
    /**
     * 错误代码
     */
    @TableField("error_code")
    private String errorCode;
    
    /**
     * 错误信息摘要
     */
    @TableField("error_message")
    private String errorMessage;
    
    /**
     * 本次调用耗时(毫秒)
     */
    @TableField("cost_ms")
    private Integer costMs;
    
    /**
     * 尝试时间
     */
    @TableField(value = "created_at", fill = FieldFill.INSERT)
    private LocalDateTime createdAt;
}
