-- H2 数据库初始化脚本（用于快速测试）

-- 通知任务表
CREATE TABLE IF NOT EXISTS notification_task (
    id VARCHAR(50) PRIMARY KEY,
    vendor_code VARCHAR(50),
    target_url VARCHAR(500) NOT NULL,
    http_method VARCHAR(10) NOT NULL,
    headers_json TEXT,
    body_json TEXT,
    status VARCHAR(20) NOT NULL,
    retry_count INT NOT NULL DEFAULT 0,
    max_retry INT NOT NULL DEFAULT 5,
    callback_timeout_ms INT NOT NULL DEFAULT 3000,
    last_error_code VARCHAR(50),
    last_error_message VARCHAR(500),
    event_id VARCHAR(100),
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP NOT NULL,
    last_attempt_at TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_status_vendor_created ON notification_task(status, vendor_code, created_at);
CREATE INDEX IF NOT EXISTS idx_event_id ON notification_task(event_id);

-- 供应商配置表
CREATE TABLE IF NOT EXISTS vendor_config (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    vendor_code VARCHAR(50) UNIQUE NOT NULL,
    base_url VARCHAR(200),
    default_path VARCHAR(200),
    default_http_method VARCHAR(10),
    default_headers_json TEXT,
    auth_type VARCHAR(20),
    auth_config_json TEXT,
    default_max_retry INT DEFAULT 5,
    default_timeout_ms INT DEFAULT 3000,
    enabled BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP NOT NULL
);

-- 通知尝试记录表
CREATE TABLE IF NOT EXISTS notification_attempt (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    notification_id VARCHAR(50) NOT NULL,
    attempt_no INT NOT NULL,
    request_headers TEXT,
    request_body TEXT,
    response_status INT,
    response_body TEXT,
    error_code VARCHAR(50),
    error_message VARCHAR(500),
    cost_ms INT,
    created_at TIMESTAMP NOT NULL
);

-- 插入示例供应商配置
INSERT INTO vendor_config (vendor_code, base_url, default_path, default_http_method, auth_type, default_max_retry, default_timeout_ms, enabled, created_at, updated_at)
VALUES 
('AD_SYSTEM_A', 'https://api.ad-system-a.com', '/notify', 'POST', 'TOKEN', 5, 3000, TRUE, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
('CRM_SYSTEM', 'https://api.crm-system.com', '/webhook', 'POST', 'BASIC', 3, 5000, TRUE, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
('INVENTORY_SYSTEM', 'https://api.inventory.com', '/update', 'PUT', 'NONE', 5, 3000, TRUE, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP);
