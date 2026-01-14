-- API 通知系统数据库初始化脚本
-- 数据库: notification_system

-- 创建数据库
CREATE DATABASE IF NOT EXISTS notification_system 
DEFAULT CHARACTER SET utf8mb4 
DEFAULT COLLATE utf8mb4_unicode_ci;

USE notification_system;

-- ============================================
-- 表1: notification_task (通知任务表)
-- 用途: 记录每一条通知任务的主信息与当前状态
-- ============================================
CREATE TABLE IF NOT EXISTS notification_task (
    id VARCHAR(50) PRIMARY KEY COMMENT '通知任务ID，格式: ntf_yyyyMMddHHmmss_随机数',
    vendor_code VARCHAR(50) COMMENT '供应商编码',
    target_url VARCHAR(500) NOT NULL COMMENT '实际调用URL',
    http_method VARCHAR(10) NOT NULL COMMENT 'HTTP方法: GET/POST/PUT/DELETE',
    headers_json TEXT COMMENT '请求头JSON',
    body_json TEXT COMMENT '请求体JSON',
    status VARCHAR(20) NOT NULL COMMENT '任务状态: PENDING/RETRYING/SUCCESS/FAILED',
    retry_count INT NOT NULL DEFAULT 0 COMMENT '当前已重试次数',
    max_retry INT NOT NULL DEFAULT 5 COMMENT '最大重试次数',
    callback_timeout_ms INT NOT NULL DEFAULT 3000 COMMENT '调用外部接口超时时间(毫秒)',
    last_error_code VARCHAR(50) COMMENT '最近错误代码',
    last_error_message VARCHAR(500) COMMENT '最近错误信息简要描述',
    event_id VARCHAR(100) COMMENT '业务幂等ID(如订单号/事件ID)',
    created_at DATETIME NOT NULL COMMENT '创建时间',
    updated_at DATETIME NOT NULL COMMENT '最近更新时间',
    last_attempt_at DATETIME COMMENT '最近一次尝试时间',
    INDEX idx_status_vendor_created (status, vendor_code, created_at),
    INDEX idx_event_id (event_id),
    INDEX idx_created_at (created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='通知任务表';


-- ============================================
-- 表2: vendor_config (供应商配置表)
-- 用途: 统一管理不同供应商的基础配置与默认策略
-- ============================================
CREATE TABLE IF NOT EXISTS vendor_config (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '主键',
    vendor_code VARCHAR(50) UNIQUE NOT NULL COMMENT '供应商编码',
    base_url VARCHAR(200) COMMENT '基础URL',
    default_path VARCHAR(200) COMMENT '默认路径',
    default_http_method VARCHAR(10) COMMENT '默认HTTP方法',
    default_headers_json TEXT COMMENT '默认公共Header',
    auth_type VARCHAR(20) COMMENT '认证类型: NONE/TOKEN/BASIC/HMAC',
    auth_config_json TEXT COMMENT '认证配置',
    default_max_retry INT DEFAULT 5 COMMENT '默认最大重试次数',
    default_timeout_ms INT DEFAULT 3000 COMMENT '默认超时时间(毫秒)',
    enabled TINYINT(1) NOT NULL DEFAULT 1 COMMENT '是否启用: 0-禁用, 1-启用',
    created_at DATETIME NOT NULL COMMENT '创建时间',
    updated_at DATETIME NOT NULL COMMENT '更新时间',
    INDEX idx_vendor_code (vendor_code),
    INDEX idx_enabled (enabled)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='供应商配置表';

-- ============================================
-- 表3: notification_attempt (通知尝试记录表 - 可选)
-- 用途: 记录每一次对外调用的详细情况，用于故障排查
-- ============================================
CREATE TABLE IF NOT EXISTS notification_attempt (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '尝试记录ID',
    notification_id VARCHAR(50) NOT NULL COMMENT '关联通知任务ID',
    attempt_no INT NOT NULL COMMENT '第几次尝试(从1开始)',
    request_headers TEXT COMMENT '本次请求头',
    request_body TEXT COMMENT '本次请求体',
    response_status INT COMMENT 'HTTP状态码',
    response_body TEXT COMMENT '响应体',
    error_code VARCHAR(50) COMMENT '错误代码',
    error_message VARCHAR(500) COMMENT '错误信息摘要',
    cost_ms INT COMMENT '本次调用耗时(毫秒)',
    created_at DATETIME NOT NULL COMMENT '尝试时间',
    INDEX idx_notification_id (notification_id),
    INDEX idx_created_at (created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='通知尝试记录表';

-- ============================================
-- 初始化供应商配置示例数据
-- ============================================
INSERT INTO vendor_config (vendor_code, base_url, default_path, default_http_method, auth_type, default_max_retry, default_timeout_ms, enabled, created_at, updated_at)
VALUES 
('AD_SYSTEM_A', 'https://api.ad-system-a.com', '/notify', 'POST', 'TOKEN', 5, 3000, 1, NOW(), NOW()),
('CRM_SYSTEM', 'https://api.crm-system.com', '/webhook', 'POST', 'BASIC', 3, 5000, 1, NOW(), NOW()),
('INVENTORY_SYSTEM', 'https://api.inventory.com', '/update', 'PUT', 'NONE', 5, 3000, 1, NOW(), NOW());
