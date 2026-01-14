# Implementation Plan: API 通知系统

## Overview

本实现计划将 API 通知系统分解为一系列增量式开发任务。系统基于 Java 17 + Spring Boot 3.2 + MyBatis Plus + RocketMQ 技术栈，采用分层架构设计。实现过程遵循"先核心后扩展"的原则，优先完成通知接收、持久化、队列投递和外部调用的主流程，然后逐步添加重试、查询、配置管理等功能。

每个任务都包含明确的实现目标和对应的需求引用，确保实现与设计文档保持一致。

## Tasks

- [x] 1. 项目初始化与基础配置
  - 创建 Spring Boot 项目，配置 Maven 依赖（Spring Boot Web、MyBatis Plus、RocketMQ、MySQL Driver、Lombok）
  - 配置 application.yml（数据库连接、MyBatis Plus、RocketMQ）
  - 创建项目包结构（controller、service、mapper、entity、dto、config、util）
  - 配置 MyBatis Plus（分页插件、自动填充处理器）
  - _Requirements: 所有需求的基础_

- [ ] 2. 数据库表结构与实体类
  - [x] 2.1 创建数据库表 DDL
    - 编写 notification_task 表创建脚本（包含索引）
    - 编写 vendor_config 表创建脚本
    - 编写 notification_attempt 表创建脚本（可选）
    - _Requirements: 2.1, 2.2, 2.3_

  - [x] 2.2 实现实体类
    - 创建 NotificationTask 实体类（使用 MyBatis Plus 注解）
    - 创建 VendorConfig 实体类
    - 创建 NotificationAttempt 实体类（可选）
    - 创建枚举类（TaskStatus、HttpMethod、AuthType、ErrorCode）
    - _Requirements: 2.1, 7.1_

  - [ ]* 2.3 编写实体类属性测试
    - **Property 3: 任务持久化与初始状态**
    - **Validates: Requirements 2.1, 2.2, 2.3**

- [ ] 3. 数据访问层（Mapper）
  - [x] 3.1 实现 NotificationTaskMapper
    - 继承 BaseMapper<NotificationTask>
    - 添加自定义查询方法（按 eventId 和状态查询、按状态和时间查询）
    - _Requirements: 2.1, 9.2, 12.1, 12.2_

  - [x] 3.2 实现 VendorConfigMapper
    - 继承 BaseMapper<VendorConfig>
    - 添加自定义查询方法（按 vendorCode 查询启用配置、查询所有启用配置）
    - _Requirements: 7.1, 7.5_

  - [ ]* 3.3 编写 Mapper 单元测试
    - 测试基本 CRUD 操作
    - 测试自定义查询方法
    - _Requirements: 2.1, 7.1_

- [ ] 4. DTO 和请求响应对象
  - [x] 4.1 创建 API 请求响应 DTO
    - CreateNotificationRequest（包含验证注解）
    - CreateNotificationResponse
    - NotificationStatusResponse
    - ErrorResponse
    - _Requirements: 1.1, 1.3, 6.1_

  - [x] 4.2 创建消息队列 DTO
    - NotificationMessage（notificationId、vendorCode、retryCount）
    - _Requirements: 3.1_

  - [ ]* 4.3 编写 DTO 序列化属性测试
    - **Property 5: 消息序列化 Round-Trip**
    - **Validates: Requirements 3.4**

- [ ] 5. 核心业务服务层
  - [x] 5.1 实现 NotificationService
    - 实现 createNotification 方法（参数验证、幂等性检查、供应商配置合并、任务创建）
    - 实现 getNotificationStatus 方法
    - 实现 checkIdempotency 方法
    - _Requirements: 1.1, 1.2, 6.1, 9.2, 9.3_

  - [ ]* 5.2 编写 NotificationService 属性测试
    - **Property 1: API 请求接受与响应**
    - **Property 22: 幂等性键存储**
    - **Property 23: 幂等性冲突检测**
    - **Validates: Requirements 1.1, 1.2, 9.1, 9.2, 9.3, 9.4, 9.5**

  - [x] 5.3 实现 VendorConfigService
    - 实现加载供应商配置方法
    - 实现配置合并逻辑（默认值应用）
    - _Requirements: 7.1, 7.2, 7.3, 7.4_

  - [ ]* 5.4 编写 VendorConfigService 属性测试
    - **Property 17: 供应商配置默认值应用**
    - **Property 18: 禁用供应商拒绝**
    - **Validates: Requirements 7.2, 7.3, 7.4, 7.5**

- [x] 6. RocketMQ 集成
  - [x] 6.1 实现 RocketMQProducer
    - 实现发送普通消息方法
    - 实现发送延迟消息方法（支持延迟级别映射）
    - 添加异常处理和日志记录
    - _Requirements: 3.1, 3.3_

  - [x] 6.2 实现 RocketMQConsumer
    - 实现消息监听器（@RocketMQMessageListener）
    - 实现 onMessage 方法（调用 Dispatcher）
    - 添加异常处理和消息重投逻辑
    - _Requirements: 3.1, 10.5_

  - [ ]* 6.3 编写 RocketMQ 集成测试
    - 测试消息发送和消费
    - 测试延迟消息功能
    - _Requirements: 3.1, 3.3_

- [ ] 7. Checkpoint - 确保基础架构测试通过
  - 确保所有测试通过，如有问题请向用户提问

- [x] 8. HTTP 客户端服务
  - [x] 8.1 实现 HttpClientService
    - 使用 Spring WebClient 实现 HTTP 调用
    - 支持 GET、POST、PUT、DELETE 方法
    - 实现超时控制
    - 实现异常捕获和转换
    - _Requirements: 4.2, 4.6_

  - [ ]* 8.2 编写 HttpClientService 属性测试
    - **Property 7: HTTP 请求参数一致性**
    - **Property 9: 超时控制**
    - **Validates: Requirements 4.2, 4.6**

  - [ ]* 8.3 编写 HTTP 调用单元测试
    - 使用 WireMock 模拟外部 API
    - 测试各种响应状态码处理
    - 测试超时场景
    - _Requirements: 4.2, 4.3, 4.4, 4.5, 4.6_

- [x] 9. 重试策略服务
  - [x] 9.1 实现 RetryPolicyService
    - 实现 shouldRetry 方法（判断是否应该重试）
    - 实现 calculateDelaySeconds 方法（指数退避算法）
    - 实现 mapToRocketMQDelayLevel 方法（延迟秒数映射到 RocketMQ 延迟级别）
    - _Requirements: 5.1, 5.2, 5.3, 5.4_

  - [ ]* 9.2 编写 RetryPolicyService 属性测试
    - **Property 11: 指数退避延迟计算**
    - **Property 12: 重试上限终止**
    - **Property 13: 延迟消息设置**
    - **Validates: Requirements 5.2, 5.4, 5.3**

- [x] 10. 通知投递核心服务
  - [x] 10.1 实现 NotificationDispatcher
    - 实现 dispatch 方法（主流程：加载任务、调用外部 API、处理结果）
    - 实现 handleSuccess 方法（更新状态为 SUCCESS）
    - 实现 handleFailure 方法（记录错误、判断是否重试）
    - 实现 handleRetry 方法（增加重试计数、发送延迟消息）
    - _Requirements: 4.1, 4.3, 4.4, 4.5, 5.1, 5.5_

  - [ ]* 10.2 编写 NotificationDispatcher 属性测试
    - **Property 6: 任务详情加载完整性**
    - **Property 8: HTTP 响应状态码处理**
    - **Property 10: 重试计数递增**
    - **Validates: Requirements 4.1, 4.3, 4.4, 4.5, 5.1**

  - [ ]* 10.3 编写投递流程集成测试
    - 测试完整的投递流程（从队列消费到外部调用）
    - 测试成功场景
    - 测试失败重试场景
    - 测试达到最大重试次数场景
    - _Requirements: 4.1, 4.3, 4.4, 4.5, 5.1, 5.4, 5.5_

- [ ] 11. REST API 控制器
  - [ ] 11.1 实现 NotificationController
    - 实现 POST /notifications 端点（创建通知）
    - 实现 GET /notifications/{notificationId} 端点（查询状态）
    - 添加参数验证（@Valid）
    - 添加全局异常处理器（@ControllerAdvice）
    - _Requirements: 1.1, 1.2, 1.3, 1.4, 1.5, 6.1, 6.2_

  - [ ]* 11.2 编写 Controller 属性测试
    - **Property 2: 输入验证拒绝无效请求**
    - **Property 14: 任务状态查询完整性**
    - **Property 15: 不存在资源返回 404**
    - **Validates: Requirements 1.3, 1.4, 1.5, 6.1, 6.2, 6.3**

  - [ ]* 11.3 编写 API 集成测试
    - 测试创建通知端到端流程
    - 测试查询通知状态
    - 测试各种验证错误场景
    - _Requirements: 1.1, 1.2, 1.3, 1.4, 1.5, 6.1, 6.2, 6.3_

- [ ] 12. Checkpoint - 确保核心功能测试通过
  - 确保所有测试通过，如有问题请向用户提问

- [ ] 13. 日志和监控
  - [ ] 13.1 实现日志记录工具类
    - 实现 SensitiveDataMasker（敏感信息脱敏）
    - 在关键位置添加日志（接收请求、发送消息、调用外部 API、状态变更）
    - 实现日志级别控制（INFO、WARN、ERROR）
    - _Requirements: 8.1, 8.2, 8.3, 8.4, 8.5, 10.4_

  - [ ]* 13.2 编写日志记录属性测试
    - **Property 19: 外部调用日志记录**
    - **Property 20: 状态变更更新时间戳**
    - **Property 21: 失败日志级别**
    - **Property 24: 敏感信息脱敏**
    - **Property 25: 失败任务详细日志**
    - **Validates: Requirements 8.1, 8.2, 8.3, 8.4, 8.5, 10.4, 11.3**

  - [ ] 13.3 实现监控指标输出
    - 添加请求计数日志
    - 添加成功率和响应时间统计日志
    - 添加队列积压告警日志
    - _Requirements: 11.1, 11.2, 11.4_

- [ ] 14. 数据清理定时任务
  - [ ] 14.1 实现 DataCleanupScheduler
    - 使用 @Scheduled 注解创建定时任务
    - 实现清理 30 天前成功任务逻辑
    - 实现清理 90 天前失败任务逻辑
    - 实现级联删除 notification_attempt 记录
    - 添加清理日志记录
    - _Requirements: 12.1, 12.2, 12.3, 12.4, 12.5_

  - [ ]* 14.2 编写数据清理属性测试
    - **Property 26: 数据清理策略**
    - **Property 27: 级联删除关联记录**
    - **Property 28: 清理任务日志记录**
    - **Validates: Requirements 12.1, 12.2, 12.3, 12.4**

- [ ] 15. 异常处理增强
  - [ ] 15.1 实现全局异常处理
    - 处理数据库异常（返回 503）
    - 处理 MQ 异常（记录日志但返回成功）
    - 处理超大响应体（截断处理）
    - 处理未预期异常（记录日志并重投消息）
    - _Requirements: 2.5, 3.2, 10.1, 10.2, 10.3, 10.5_

  - [ ]* 15.2 编写异常处理单元测试
    - 测试各种异常场景
    - 验证错误响应格式
    - 验证日志记录
    - _Requirements: 2.5, 3.2, 10.1, 10.2, 10.3, 10.5_

- [ ] 16. 配置文件和文档
  - [ ] 16.1 完善配置文件
    - 创建 application-dev.yml（开发环境配置）
    - 创建 application-prod.yml（生产环境配置）
    - 添加配置说明注释
    - _Requirements: 所有需求_

  - [ ] 16.2 编写 API 文档
    - 使用 Swagger/OpenAPI 生成 API 文档
    - 添加接口描述和示例
    - _Requirements: 1.1, 6.1_

  - [ ] 16.3 编写部署文档
    - 编写数据库初始化脚本
    - 编写 Docker Compose 配置（MySQL + RocketMQ）
    - 编写部署说明文档
    - _Requirements: 所有需求_

- [ ] 17. 最终集成测试
  - [ ]* 17.1 编写端到端集成测试
    - 使用 TestContainers 启动真实的 MySQL 和 RocketMQ
    - 测试完整的通知流程（创建 → 队列 → 投递 → 重试 → 成功/失败）
    - 测试幂等性场景
    - 测试供应商配置场景
    - _Requirements: 所有核心需求_

  - [ ]* 17.2 编写性能测试
    - 测试 API 响应时间（< 200ms）
    - 测试并发创建通知
    - 测试队列吞吐量
    - _Requirements: 1.2_

- [ ] 18. Final Checkpoint - 确保所有测试通过
  - 确保所有测试通过，如有问题请向用户提问

## Notes

- 任务标记 `*` 的为可选测试任务，可以跳过以加快 MVP 开发
- 每个任务都引用了具体的需求编号，确保可追溯性
- Checkpoint 任务用于阶段性验证，确保增量开发的质量
- 属性测试使用 jqwik 框架，每个测试运行 100+ 次迭代
- 单元测试使用 JUnit 5 + Mockito
- 集成测试使用 Spring Boot Test + TestContainers
