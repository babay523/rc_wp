package com.notification.entity;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 供应商配置实体类
 * 对应数据库表: vendor_config
 * 
 * @author Notification System
 */
@Data
@TableName("vendor_config")
public class VendorConfig {
    
    /**
     * 主键
     */
    @TableId(type = IdType.AUTO)
    private Long id;
    
    /**
     * 供应商编码
     */
    @TableField("vendor_code")
    private String vendorCode;
    
    /**
     * 基础URL
     */
    @TableField("base_url")
    private String baseUrl;
    
    /**
     * 默认路径
     */
    @TableField("default_path")
    private String defaultPath;
    
    /**
     * 默认HTTP方法
     */
    @TableField("default_http_method")
    private String defaultHttpMethod;
    
    /**
     * 默认公共Header
     */
    @TableField("default_headers_json")
    private String defaultHeadersJson;
    
    /**
     * 认证类型: NONE/TOKEN/BASIC/HMAC
     */
    @TableField("auth_type")
    private String authType;
    
    /**
     * 认证配置
     */
    @TableField("auth_config_json")
    private String authConfigJson;
    
    /**
     * 默认最大重试次数
     */
    @TableField("default_max_retry")
    private Integer defaultMaxRetry;
    
    /**
     * 默认超时时间(毫秒)
     */
    @TableField("default_timeout_ms")
    private Integer defaultTimeoutMs;
    
    /**
     * 是否启用: 0-禁用, 1-启用
     */
    @TableField("enabled")
    private Boolean enabled;
    
    /**
     * 创建时间
     */
    @TableField(value = "created_at", fill = FieldFill.INSERT)
    private LocalDateTime createdAt;
    
    /**
     * 更新时间
     */
    @TableField(value = "updated_at", fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;
}
