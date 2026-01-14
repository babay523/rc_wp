package com.notification.entity;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 通知任务实体类
 * 对应数据库表: notification_task
 * 
 * @author Notification System
 */
@Data
@TableName("notification_task")
public class NotificationTask {
    
    /**
     * 通知任务ID，格式: ntf_yyyyMMddHHmmss_随机数
     */
    @TableId(type = IdType.ASSIGN_ID)
    private String id;
    
    /**
     * 供应商编码
     */
    @TableField("vendor_code")
    private String vendorCode;
    
    /**
     * 实际调用URL
     */
    @TableField("target_url")
    private String targetUrl;
    
    /**
     * HTTP方法: GET/POST/PUT/DELETE
     */
    @TableField("http_method")
    private String httpMethod;
    
    /**
     * 请求头JSON
     */
    @TableField("headers_json")
    private String headersJson;
    
    /**
     * 请求体JSON
     */
    @TableField("body_json")
    private String bodyJson;
    
    /**
     * 任务状态: PENDING/RETRYING/SUCCESS/FAILED
     */
    @TableField("status")
    private String status;
    
    /**
     * 当前已重试次数
     */
    @TableField("retry_count")
    private Integer retryCount;
    
    /**
     * 最大重试次数
     */
    @TableField("max_retry")
    private Integer maxRetry;
    
    /**
     * 调用外部接口超时时间(毫秒)
     */
    @TableField("callback_timeout_ms")
    private Integer callbackTimeoutMs;
    
    /**
     * 最近错误代码
     */
    @TableField("last_error_code")
    private String lastErrorCode;
    
    /**
     * 最近错误信息简要描述
     */
    @TableField("last_error_message")
    private String lastErrorMessage;
    
    /**
     * 业务幂等ID(如订单号/事件ID)
     */
    @TableField("event_id")
    private String eventId;
    
    /**
     * 创建时间
     */
    @TableField(value = "created_at", fill = FieldFill.INSERT)
    private LocalDateTime createdAt;
    
    /**
     * 最近更新时间
     */
    @TableField(value = "updated_at", fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;
    
    /**
     * 最近一次尝试时间
     */
    @TableField("last_attempt_at")
    private LocalDateTime lastAttemptAt;
}
