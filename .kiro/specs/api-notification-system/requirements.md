# Requirements Document

## Introduction

API 通知系统是一个企业级的异步通知投递平台，旨在为内部多个业务系统提供统一、可靠的外部 HTTP API 调用能力。系统采用"至少一次投递（At-Least-Once Delivery）"语义，通过消息队列和智能重试机制确保通知可靠送达外部供应商系统。

## Glossary

- **Notification_System**: API 通知系统，本系统的总称
- **Business_System**: 业务系统，内部调用通知系统的各类业务应用（如注册系统、支付系统、订单系统）
- **Vendor**: 供应商，接收通知的外部第三方系统（如广告平台、CRM、库存系统）
- **Notification_Task**: 通知任务，系统中的一条待投递或已投递的通知记录
- **Message_Queue**: 消息队列，用于异步处理通知任务的 RocketMQ 队列
- **Dispatcher**: 投递服务，从消息队列消费任务并调用外部 API 的后台服务
- **Retry_Policy**: 重试策略，定义失败后的重试次数、间隔和退避算法
- **Idempotency_Key**: 幂等性键，业务方提供的唯一标识（如 eventId、orderId）用于去重

## Requirements

### Requirement 1: 统一通知接入

**User Story:** 作为业务系统开发者，我希望通过统一的 REST API 提交通知任务，这样我就不需要关心不同外部供应商的 API 差异。

#### Acceptance Criteria

1. WHEN Business_System 发送 POST 请求到 `/notifications` 端点 THEN Notification_System SHALL 接受包含 vendorCode、targetUrl、httpMethod、headers、body、maxRetry、callbackTimeoutMs 的请求体
2. WHEN Notification_System 接收到有效的通知请求 THEN Notification_System SHALL 在 200 毫秒内返回 HTTP 200 状态码和唯一的 notificationId
3. WHEN 请求体中缺少必填字段（targetUrl 或 vendorCode、httpMethod、body）THEN Notification_System SHALL 返回 HTTP 400 错误和详细的验证错误信息
4. WHEN 请求体中的 httpMethod 不是 GET、POST、PUT、DELETE 之一 THEN Notification_System SHALL 返回 HTTP 400 错误
5. WHEN 请求体中的 targetUrl 格式不合法 THEN Notification_System SHALL 返回 HTTP 400 错误

### Requirement 2: 异步任务持久化

**User Story:** 作为系统架构师，我希望所有通知任务在接收后立即持久化到数据库，这样即使系统重启也不会丢失任务。

#### Acceptance Criteria

1. WHEN Notification_System 接收到有效的通知请求 THEN Notification_System SHALL 在返回响应前将任务记录写入 notification_task 表
2. WHEN 写入数据库时 THEN Notification_System SHALL 设置任务状态为 PENDING
3. WHEN 写入数据库时 THEN Notification_System SHALL 生成唯一的 notificationId 作为主键
4. WHEN 写入数据库时 THEN Notification_System SHALL 记录 created_at 时间戳
5. WHEN 数据库写入失败 THEN Notification_System SHALL 返回 HTTP 500 错误并记录错误日志

### Requirement 3: 消息队列投递

**User Story:** 作为系统架构师，我希望通知任务通过消息队列异步处理，这样可以解耦接入层和投递层，提高系统吞吐量。

#### Acceptance Criteria

1. WHEN Notification_Task 成功写入数据库 THEN Notification_System SHALL 将包含 notificationId、vendorCode、retryCount 的消息发送到 RocketMQ 主任务队列
2. WHEN 消息发送到队列失败 THEN Notification_System SHALL 记录错误日志但仍返回成功响应（任务已持久化）
3. WHEN 需要延迟重试时 THEN Dispatcher SHALL 使用 RocketMQ 的延迟消息功能发送到重试队列
4. WHEN 消息体序列化时 THEN Notification_System SHALL 使用 JSON 格式

### Requirement 4: 外部 HTTP 调用

**User Story:** 作为投递服务，我需要根据任务配置调用外部供应商的 HTTP API，并正确处理响应和异常。

#### Acceptance Criteria

1. WHEN Dispatcher 从队列消费到通知任务 THEN Dispatcher SHALL 从数据库读取完整的任务详情和供应商配置
2. WHEN 调用外部 API 时 THEN Dispatcher SHALL 使用任务中指定的 httpMethod、targetUrl、headers 和 body
3. WHEN 外部 API 返回 2xx 状态码 THEN Dispatcher SHALL 将任务状态更新为 SUCCESS
4. WHEN 外部 API 返回 4xx 状态码 THEN Dispatcher SHALL 将任务状态更新为 FAILED 且不进行重试
5. WHEN 外部 API 返回 5xx 状态码或网络超时 THEN Dispatcher SHALL 根据 Retry_Policy 决定是否重试
6. WHEN HTTP 调用超过 callbackTimeoutMs 毫秒 THEN Dispatcher SHALL 中断请求并标记为超时失败

### Requirement 5: 智能重试策略

**User Story:** 作为系统运维人员，我希望系统能够自动重试失败的通知，使用指数退避策略避免对外部系统造成压力。

#### Acceptance Criteria

1. WHEN 外部 API 调用失败且 retry_count 小于 max_retry THEN Dispatcher SHALL 增加 retry_count 并更新任务状态为 RETRYING
2. WHEN 计算重试延迟时 THEN Dispatcher SHALL 使用指数退避算法：delay = min(60 * 2^retry_count, 3600) 秒
3. WHEN 发送重试消息时 THEN Dispatcher SHALL 使用 RocketMQ 延迟消息功能设置对应的延迟时间
4. WHEN retry_count 达到 max_retry THEN Dispatcher SHALL 将任务状态更新为 FAILED 且不再重试
5. WHEN 任务状态更新为 FAILED THEN Dispatcher SHALL 记录 last_error_code 和 last_error_message

### Requirement 6: 任务状态查询

**User Story:** 作为业务系统开发者，我希望能够查询通知任务的当前状态和历史信息，以便追踪通知是否成功送达。

#### Acceptance Criteria

1. WHEN Business_System 发送 GET 请求到 `/notifications/{notificationId}` THEN Notification_System SHALL 返回任务的完整状态信息
2. WHEN 查询的 notificationId 不存在 THEN Notification_System SHALL 返回 HTTP 404 错误
3. WHEN 查询成功时 THEN Notification_System SHALL 返回包含 notificationId、vendorCode、targetUrl、httpMethod、status、retryCount、maxRetry、lastErrorCode、lastErrorMessage、createdAt、updatedAt、lastAttemptAt 的 JSON 响应
4. WHEN 任务状态为 PENDING THEN lastAttemptAt SHALL 为 NULL
5. WHEN 任务状态为 SUCCESS 或 FAILED THEN lastAttemptAt SHALL 包含最后一次尝试的时间戳

### Requirement 7: 供应商配置管理

**User Story:** 作为系统管理员，我希望能够为不同供应商配置默认的调用参数和重试策略，简化业务系统的调用复杂度。

#### Acceptance Criteria

1. WHEN Notification_System 启动时 THEN Notification_System SHALL 从 vendor_config 表加载所有启用的供应商配置
2. WHEN Business_System 提供 vendorCode 但未提供 targetUrl THEN Notification_System SHALL 使用供应商配置中的 base_url 和 default_path 组合生成 targetUrl
3. WHEN Business_System 未提供 maxRetry THEN Notification_System SHALL 使用供应商配置中的 default_max_retry
4. WHEN Business_System 未提供 callbackTimeoutMs THEN Notification_System SHALL 使用供应商配置中的 default_timeout_ms
5. WHEN 供应商配置中 enabled 为 false THEN Notification_System SHALL 拒绝该供应商的通知请求并返回 HTTP 400 错误

### Requirement 8: 调用日志记录

**User Story:** 作为系统运维人员，我希望系统记录每次外部 API 调用的详细日志，以便故障排查和审计。

#### Acceptance Criteria

1. WHEN Dispatcher 调用外部 API 时 THEN Dispatcher SHALL 记录包含 notificationId、targetUrl、httpMethod、attempt_no 的开始日志
2. WHEN 外部 API 调用完成时 THEN Dispatcher SHALL 记录包含 response_status、cost_ms、error_code、error_message 的结束日志
3. WHEN 任务状态变更时 THEN Notification_System SHALL 更新 notification_task 表的 updated_at 字段
4. WHEN 调用失败时 THEN Dispatcher SHALL 记录 ERROR 级别日志
5. WHEN 任务最终失败（超过最大重试次数）THEN Dispatcher SHALL 记录 WARN 级别的告警日志

### Requirement 9: 幂等性支持

**User Story:** 作为业务系统开发者，我希望能够提供业务唯一标识，防止同一事件被重复处理。

#### Acceptance Criteria

1. WHEN Business_System 在请求体中提供 eventId 字段 THEN Notification_System SHALL 将其存储在 notification_task 表的 event_id 字段
2. WHEN Business_System 提交相同 eventId 的通知请求 THEN Notification_System SHALL 检查是否存在相同 event_id 且状态为 PENDING 或 RETRYING 的任务
3. WHEN 存在相同 eventId 的进行中任务 THEN Notification_System SHALL 返回 HTTP 409 冲突错误和现有任务的 notificationId
4. WHEN 存在相同 eventId 但状态为 SUCCESS 或 FAILED 的任务 THEN Notification_System SHALL 允许创建新任务
5. WHEN Business_System 未提供 eventId THEN Notification_System SHALL 允许创建任务但不进行去重检查

### Requirement 10: 错误处理与边界情况

**User Story:** 作为系统架构师，我希望系统能够优雅地处理各种异常情况，确保系统稳定性。

#### Acceptance Criteria

1. WHEN 数据库连接失败 THEN Notification_System SHALL 返回 HTTP 503 错误并记录错误日志
2. WHEN 消息队列连接失败 THEN Notification_System SHALL 记录错误日志但仍返回成功响应（任务已持久化）
3. WHEN 外部 API 响应体过大（超过 1MB）THEN Dispatcher SHALL 截断响应体并记录警告日志
4. WHEN 请求体中的 headers 包含敏感信息（如 Authorization）THEN Notification_System SHALL 在日志中脱敏处理
5. WHEN Dispatcher 处理消息时发生未预期异常 THEN Dispatcher SHALL 记录错误日志并将消息重新投递到队列

### Requirement 11: 系统监控指标

**User Story:** 作为系统运维人员，我希望系统暴露关键性能指标，以便监控系统健康状态。

#### Acceptance Criteria

1. WHEN Notification_System 运行时 THEN Notification_System SHALL 通过日志输出每分钟接收的通知请求数量
2. WHEN Dispatcher 完成外部调用时 THEN Dispatcher SHALL 通过日志输出调用成功率和平均响应时间
3. WHEN 任务状态变更为 FAILED THEN Notification_System SHALL 通过日志输出失败任务的 vendorCode 和错误原因
4. WHEN 队列中积压任务超过 1000 条 THEN Notification_System SHALL 记录 WARN 级别日志
5. WHEN 某个供应商的失败率超过 50% THEN Dispatcher SHALL 记录 ERROR 级别告警日志

### Requirement 12: 数据保留策略

**User Story:** 作为系统管理员，我希望系统能够自动清理历史数据，避免数据库无限增长。

#### Acceptance Criteria

1. WHEN 定时任务执行时 THEN Notification_System SHALL 删除 created_at 超过 30 天且状态为 SUCCESS 的任务记录
2. WHEN 定时任务执行时 THEN Notification_System SHALL 删除 created_at 超过 90 天且状态为 FAILED 的任务记录
3. WHEN 删除任务记录时 THEN Notification_System SHALL 同时删除关联的 notification_attempt 记录
4. WHEN 数据清理任务执行时 THEN Notification_System SHALL 记录删除的记录数量
5. WHEN 数据清理任务失败时 THEN Notification_System SHALL 记录错误日志但不影响系统正常运行
