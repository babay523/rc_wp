# Design Document

## Overview

API 通知系统是一个基于 Java/Spring Boot 的企业级异步通知投递平台。系统采用分层架构，通过 REST API 接入层、消息队列中间层和投递服务层实现业务系统与外部供应商之间的解耦。核心设计目标是提供可靠的"至少一次投递"语义，同时保持系统的高可用性和可扩展性。

系统使用 RocketMQ 作为消息队列，MySQL 作为持久化存储，通过智能重试策略和完善的日志记录确保通知可靠送达。设计遵循单一职责原则，每个模块专注于特定功能，便于后续扩展和维护。

## Architecture

### 系统分层架构

系统采用经典的三层架构模式：

1. **接入层（API Layer）**
   - 职责：接收业务系统的 HTTP 请求，进行参数验证和初步处理
   - 组件：NotificationController、RequestValidator、NotificationService
   - 技术栈：Spring Boot Web、Spring Validation

2. **持久化层（Persistence Layer）**
   - 职责：管理通知任务和供应商配置的数据存储
   - 组件：NotificationTaskRepository、VendorConfigRepository、Entity 类
   - 技术栈： MyBatis Plus、MySQL

3. **消息队列层（Message Queue Layer）**
   - 职责：实现异步解耦和任务调度
   - 组件：RocketMQProducer、RocketMQConsumer
   - 技术栈：RocketMQ Spring Boot Starter

4. **投递层（Dispatcher Layer）**
   - 职责：消费队列消息，调用外部 API，处理重试逻辑
   - 组件：NotificationDispatcher、HttpClientService、RetryPolicyService
   - 技术栈：Spring WebClient、自定义重试逻辑

### 技术选型

- **开发语言**: Java 17
- **应用框架**: Spring Boot 3.2.x
- **数据库**: MySQL 8.0
- **消息队列**: RocketMQ 5.x
- **HTTP 客户端**: Spring WebClient (响应式)
- **日志框架**: SLF4J + Logback
- **构建工具**: Maven 3.9.x

## Components and Interfaces

### 1. NotificationController (REST 接入控制器)


**职责**: 处理 HTTP 请求，提供通知创建和查询接口

**接口定义**:
```java
@RestController
@RequestMapping("/notifications")
public class NotificationController {
    
    @PostMapping
    public ResponseEntity<CreateNotificationResponse> createNotification(
        @Valid @RequestBody CreateNotificationRequest request
    );
    
    @GetMapping("/{notificationId}")
    public ResponseEntity<NotificationStatusResponse> getNotificationStatus(
        @PathVariable String notificationId
    );
}
```

**输入**: CreateNotificationRequest (vendorCode, targetUrl, httpMethod, headers, body, maxRetry, callbackTimeoutMs, eventId)

**输出**: CreateNotificationResponse (notificationId, status)

**异常处理**: 
- 参数验证失败 → HTTP 400
- 数据库异常 → HTTP 500
- 幂等性冲突 → HTTP 409

### 2. NotificationService (业务逻辑服务)

**职责**: 协调通知任务的创建、持久化和队列投递

**核心方法**:
```java
public interface NotificationService {
    CreateNotificationResponse createNotification(CreateNotificationRequest request);
    NotificationStatusResponse getNotificationStatus(String notificationId);
    void checkIdempotency(String eventId);
}
```

**业务流程**:
1. 验证请求参数
2. 检查幂等性（如果提供 eventId）
3. 查询供应商配置（如果提供 vendorCode）
4. 创建 NotificationTask 实体
5. 持久化到数据库
6. 发送消息到 RocketMQ
7. 返回响应

### 3. NotificationTaskRepository (任务仓储)

**职责**: 管理通知任务的数据访问

**接口定义**:
```java
public interface NotificationTaskRepository extends JpaRepository<NotificationTask, String> {
    Optional<NotificationTask> findByEventIdAndStatusIn(
        String eventId, 
        List<TaskStatus> statuses
    );
    
    List<NotificationTask> findByStatusAndCreatedAtBefore(
        TaskStatus status, 
        LocalDateTime before
    );
}
```

### 4. VendorConfigRepository (供应商配置仓储)

**职责**: 管理供应商配置的数据访问

**接口定义**:
```java
public interface VendorConfigRepository extends JpaRepository<VendorConfig, Long> {
    Optional<VendorConfig> findByVendorCodeAndEnabledTrue(String vendorCode);
    List<VendorConfig> findByEnabledTrue();
}
```

### 5. RocketMQProducer (消息生产者)

**职责**: 将通知任务消息发送到 RocketMQ

**接口定义**:
```java
@Component
public class RocketMQProducer {
    
    public void sendNotificationMessage(NotificationMessage message);
    
    public void sendDelayedNotificationMessage(
        NotificationMessage message, 
        int delayLevel
    );
}
```

**消息格式**:
```java
public class NotificationMessage {
    private String notificationId;
    private String vendorCode;
    private int retryCount;
}
```

### 6. RocketMQConsumer (消息消费者)

**职责**: 从 RocketMQ 消费通知任务消息

**接口定义**:
```java
@Component
@RocketMQMessageListener(
    topic = "notification-task",
    consumerGroup = "notification-dispatcher-group"
)
public class RocketMQConsumer implements RocketMQListener<NotificationMessage> {
    
    @Override
    public void onMessage(NotificationMessage message);
}
```

### 7. NotificationDispatcher (通知投递核心)

**职责**: 执行外部 API 调用和重试逻辑

**核心方法**:
```java
public interface NotificationDispatcher {
    void dispatch(String notificationId);
    void handleSuccess(NotificationTask task, HttpResponse response);
    void handleFailure(NotificationTask task, Exception exception);
    void handleRetry(NotificationTask task);
}
```

**处理流程**:
1. 从数据库加载任务详情
2. 构建 HTTP 请求
3. 调用外部 API
4. 根据响应状态码判断成功/失败
5. 更新任务状态
6. 记录日志
7. 如需重试，计算延迟并发送到队列

### 8. HttpClientService (HTTP 客户端服务)

**职责**: 封装外部 HTTP 调用逻辑

**接口定义**:
```java
public interface HttpClientService {
    HttpResponse call(
        String url,
        HttpMethod method,
        Map<String, String> headers,
        String body,
        int timeoutMs
    );
}
```

**实现细节**:
- 使用 Spring WebClient 进行响应式调用
- 支持超时控制
- 捕获网络异常和超时异常
- 记录请求和响应日志

### 9. RetryPolicyService (重试策略服务)

**职责**: 计算重试延迟和判断是否应该重试

**接口定义**:
```java
public interface RetryPolicyService {
    boolean shouldRetry(NotificationTask task);
    int calculateDelaySeconds(int retryCount);
    int mapToRocketMQDelayLevel(int delaySeconds);
}
```

**重试算法**:
```
delaySeconds = min(60 * 2^retryCount, 3600)
```

**RocketMQ 延迟级别映射**:
- Level 1: 1s
- Level 2: 5s
- Level 3: 10s
- Level 4: 30s
- Level 5: 1min
- Level 6: 2min
- Level 7: 3min
- Level 8: 4min
- Level 9: 5min
- Level 10: 6min
- Level 11: 7min
- Level 12: 8min
- Level 13: 9min
- Level 14: 10min
- Level 15: 20min
- Level 16: 30min
- Level 17: 1h
- Level 18: 2h

## Data Models

### NotificationTask (通知任务实体)

```java
@Entity
@Table(name = "notification_task", indexes = {
    @Index(name = "idx_status_vendor_created", columnList = "status,vendor_code,created_at"),
    @Index(name = "idx_event_id", columnList = "event_id")
})
public class NotificationTask {
    
    @Id
    @Column(length = 50)
    private String id;  // 格式: ntf_yyyyMMddHHmmss_随机数
    
    @Column(name = "vendor_code", length = 50)
    private String vendorCode;
    
    @Column(name = "target_url", length = 500, nullable = false)
    private String targetUrl;
    
    @Column(name = "http_method", length = 10, nullable = false)
    @Enumerated(EnumType.STRING)
    private HttpMethod httpMethod;
    
    @Column(name = "headers_json", columnDefinition = "TEXT")
    private String headersJson;
    
    @Column(name = "body_json", columnDefinition = "TEXT")
    private String bodyJson;
    
    @Column(name = "status", length = 20, nullable = false)
    @Enumerated(EnumType.STRING)
    private TaskStatus status;
    
    @Column(name = "retry_count", nullable = false)
    private Integer retryCount = 0;
    
    @Column(name = "max_retry", nullable = false)
    private Integer maxRetry = 5;
    
    @Column(name = "callback_timeout_ms", nullable = false)
    private Integer callbackTimeoutMs = 3000;
    
    @Column(name = "last_error_code", length = 50)
    private String lastErrorCode;
    
    @Column(name = "last_error_message", length = 500)
    private String lastErrorMessage;
    
    @Column(name = "event_id", length = 100)
    private String eventId;
    
    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;
    
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;
    
    @Column(name = "last_attempt_at")
    private LocalDateTime lastAttemptAt;
}
```

**枚举定义**:
```java
public enum TaskStatus {
    PENDING,    // 待处理
    RETRYING,   // 重试中
    SUCCESS,    // 成功
    FAILED      // 失败
}

public enum HttpMethod {
    GET, POST, PUT, DELETE
}
```

### VendorConfig (供应商配置实体)

```java
@Entity
@Table(name = "vendor_config")
public class VendorConfig {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(name = "vendor_code", length = 50, unique = true, nullable = false)
    private String vendorCode;
    
    @Column(name = "base_url", length = 200)
    private String baseUrl;
    
    @Column(name = "default_path", length = 200)
    private String defaultPath;
    
    @Column(name = "default_http_method", length = 10)
    @Enumerated(EnumType.STRING)
    private HttpMethod defaultHttpMethod;
    
    @Column(name = "default_headers_json", columnDefinition = "TEXT")
    private String defaultHeadersJson;
    
    @Column(name = "auth_type", length = 20)
    @Enumerated(EnumType.STRING)
    private AuthType authType;
    
    @Column(name = "auth_config_json", columnDefinition = "TEXT")
    private String authConfigJson;
    
    @Column(name = "default_max_retry")
    private Integer defaultMaxRetry = 5;
    
    @Column(name = "default_timeout_ms")
    private Integer defaultTimeoutMs = 3000;
    
    @Column(name = "enabled", nullable = false)
    private Boolean enabled = true;
    
    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;
    
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;
}
```

**枚举定义**:
```java
public enum AuthType {
    NONE,       // 无认证
    TOKEN,      // Token 认证
    BASIC,      // Basic 认证
    HMAC        // HMAC 签名
}
```

### NotificationAttempt (调用尝试记录实体 - 可选)

```java
@Entity
@Table(name = "notification_attempt")
public class NotificationAttempt {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(name = "notification_id", length = 50, nullable = false)
    private String notificationId;
    
    @Column(name = "attempt_no", nullable = false)
    private Integer attemptNo;
    
    @Column(name = "request_headers", columnDefinition = "TEXT")
    private String requestHeaders;
    
    @Column(name = "request_body", columnDefinition = "TEXT")
    private String requestBody;
    
    @Column(name = "response_status")
    private Integer responseStatus;
    
    @Column(name = "response_body", columnDefinition = "TEXT")
    private String responseBody;
    
    @Column(name = "error_code", length = 50)
    private String errorCode;
    
    @Column(name = "error_message", length = 500)
    private String errorMessage;
    
    @Column(name = "cost_ms")
    private Integer costMs;
    
    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;
}
```


## Correctness Properties

*A property is a characteristic or behavior that should hold true across all valid executions of a system—essentially, a formal statement about what the system should do. Properties serve as the bridge between human-readable specifications and machine-verifiable correctness guarantees.*

### Property 1: API 请求接受与响应

*For any* valid notification request containing required fields (targetUrl/vendorCode, httpMethod, body), the system should accept the request and return HTTP 200 with a unique notificationId within 200ms.

**Validates: Requirements 1.1, 1.2**

### Property 2: 输入验证拒绝无效请求

*For any* notification request missing required fields or containing invalid values (invalid httpMethod, malformed targetUrl), the system should reject the request and return HTTP 400 with validation error details.

**Validates: Requirements 1.3, 1.4, 1.5**

### Property 3: 任务持久化与初始状态

*For any* accepted notification request, the system should persist a task record to the database with status=PENDING, a unique notificationId, and a created_at timestamp before returning the response.

**Validates: Requirements 2.1, 2.2, 2.3, 2.4**

### Property 4: 消息队列投递

*For any* successfully persisted notification task, the system should send a message containing notificationId, vendorCode, and retryCount=0 to the RocketMQ queue.

**Validates: Requirements 3.1**

### Property 5: 消息序列化 Round-Trip

*For any* NotificationMessage object, serializing to JSON and then deserializing should produce an equivalent object with the same notificationId, vendorCode, and retryCount.

**Validates: Requirements 3.4**

### Property 6: 任务详情加载完整性

*For any* notification message consumed from the queue, the Dispatcher should successfully load the complete task details and vendor configuration from the database.

**Validates: Requirements 4.1**

### Property 7: HTTP 请求参数一致性

*For any* notification task being dispatched, the HTTP request sent to the external API should use exactly the httpMethod, targetUrl, headers, and body specified in the task configuration.

**Validates: Requirements 4.2**

### Property 8: HTTP 响应状态码处理

*For any* external API call:
- If response status is 2xx, task status should become SUCCESS
- If response status is 4xx, task status should become FAILED without retry
- If response status is 5xx or timeout occurs, retry logic should be triggered

**Validates: Requirements 4.3, 4.4, 4.5**

### Property 9: 超时控制

*For any* external API call, if the call duration exceeds callbackTimeoutMs milliseconds, the request should be interrupted and marked as timeout failure.

**Validates: Requirements 4.6**

### Property 10: 重试计数递增

*For any* failed external API call where retry_count < max_retry, the system should increment retry_count by 1 and update task status to RETRYING.

**Validates: Requirements 5.1**

### Property 11: 指数退避延迟计算

*For any* retry_count value, the calculated retry delay should equal min(60 * 2^retry_count, 3600) seconds.

**Validates: Requirements 5.2**

### Property 12: 重试上限终止

*For any* notification task where retry_count >= max_retry, the system should update task status to FAILED, record last_error_code and last_error_message, and not send any more retry messages.

**Validates: Requirements 5.4, 5.5**

### Property 13: 延迟消息设置

*For any* retry message sent to RocketMQ, the message should have the correct delay level mapped from the calculated delay seconds.

**Validates: Requirements 5.3**

### Property 14: 任务状态查询完整性

*For any* existing notificationId, querying GET /notifications/{notificationId} should return a complete JSON response containing all required fields: notificationId, vendorCode, targetUrl, httpMethod, status, retryCount, maxRetry, lastErrorCode, lastErrorMessage, createdAt, updatedAt, lastAttemptAt.

**Validates: Requirements 6.1, 6.3**

### Property 15: 不存在资源返回 404

*For any* non-existent notificationId, querying GET /notifications/{notificationId} should return HTTP 404.

**Validates: Requirements 6.2**

### Property 16: lastAttemptAt 状态不变量

*For any* notification task:
- If status is PENDING, lastAttemptAt should be NULL
- If status is SUCCESS or FAILED, lastAttemptAt should contain a non-null timestamp

**Validates: Requirements 6.4, 6.5**

### Property 17: 供应商配置默认值应用

*For any* notification request with vendorCode but missing optional fields (targetUrl, maxRetry, callbackTimeoutMs), the system should apply the corresponding default values from the vendor configuration (base_url + default_path, default_max_retry, default_timeout_ms).

**Validates: Requirements 7.2, 7.3, 7.4**

### Property 18: 禁用供应商拒绝

*For any* notification request with a vendorCode where enabled=false in vendor_config, the system should reject the request and return HTTP 400.

**Validates: Requirements 7.5**

### Property 19: 外部调用日志记录

*For any* external API call, the system should record both a start log (containing notificationId, targetUrl, httpMethod, attempt_no) and an end log (containing response_status, cost_ms, error_code, error_message).

**Validates: Requirements 8.1, 8.2**

### Property 20: 状态变更更新时间戳

*For any* task status change, the system should update the updated_at field to the current timestamp.

**Validates: Requirements 8.3**

### Property 21: 失败日志级别

*For any* failed external API call, the system should record an ERROR level log, and for tasks that reach max retry limit, the system should record a WARN level alert log.

**Validates: Requirements 8.4, 8.5**

### Property 22: 幂等性键存储

*For any* notification request containing an eventId field, the system should store it in the notification_task table's event_id field.

**Validates: Requirements 9.1**

### Property 23: 幂等性冲突检测

*For any* notification request with an eventId:
- If a task with the same event_id and status in (PENDING, RETRYING) exists, return HTTP 409 with the existing notificationId
- If a task with the same event_id and status in (SUCCESS, FAILED) exists, allow creating a new task
- If no eventId is provided, skip idempotency check

**Validates: Requirements 9.2, 9.3, 9.4, 9.5**

### Property 24: 敏感信息脱敏

*For any* notification task with headers containing sensitive keys (Authorization, X-API-Key, Token), the system should mask these values in all log outputs.

**Validates: Requirements 10.4**

### Property 25: 失败任务详细日志

*For any* task that transitions to FAILED status, the system should log the vendorCode and error reason.

**Validates: Requirements 11.3**

### Property 26: 数据清理策略

*For any* notification task:
- If status=SUCCESS and created_at > 30 days ago, it should be deleted by the cleanup job
- If status=FAILED and created_at > 90 days ago, it should be deleted by the cleanup job

**Validates: Requirements 12.1, 12.2**

### Property 27: 级联删除关联记录

*For any* notification task being deleted, all associated notification_attempt records should also be deleted.

**Validates: Requirements 12.3**

### Property 28: 清理任务日志记录

*For any* execution of the data cleanup job, the system should log the number of records deleted.

**Validates: Requirements 12.4**

## Error Handling

### 异常分类与处理策略

系统将异常分为以下几类，并采用不同的处理策略：

#### 1. 客户端错误（4xx）

**场景**: 
- 请求参数验证失败
- 资源不存在
- 幂等性冲突
- 供应商配置禁用

**处理策略**:
- 立即返回对应的 HTTP 4xx 错误码
- 返回详细的错误信息（包括字段名、错误原因）
- 记录 WARN 级别日志
- 不进行重试

**示例响应**:
```json
{
  "error": "VALIDATION_ERROR",
  "message": "Invalid request parameters",
  "details": [
    {
      "field": "httpMethod",
      "message": "must be one of: GET, POST, PUT, DELETE"
    }
  ],
  "timestamp": "2025-01-14T10:30:00Z"
}
```

#### 2. 服务端错误（5xx）

**场景**:
- 数据库连接失败
- 消息队列连接失败
- 内部服务异常

**处理策略**:
- 返回 HTTP 500 或 503 错误码
- 记录 ERROR 级别日志（包含堆栈信息）
- 对于已持久化的任务，即使 MQ 发送失败也返回成功（后续可通过定时任务补偿）
- 对于未持久化的任务，返回失败并建议客户端重试

#### 3. 外部 API 调用错误

**场景**:
- 网络超时
- 连接被拒绝
- 外部系统返回 5xx
- 响应解析失败

**处理策略**:
- 记录详细的错误信息（URL、状态码、响应体、耗时）
- 根据重试策略决定是否重试
- 更新任务状态和错误信息
- 对于 4xx 错误，直接标记失败不重试
- 对于 5xx 和网络错误，触发重试机制

#### 4. 超时处理

**场景**:
- HTTP 调用超过 callbackTimeoutMs
- 数据库查询超时
- 消息队列操作超时

**处理策略**:
- 中断当前操作
- 记录超时日志（包含超时阈值和实际耗时）
- 对于 HTTP 调用超时，触发重试机制
- 对于数据库超时，返回 503 错误

### 错误码定义

```java
public enum ErrorCode {
    // 客户端错误 (4xx)
    VALIDATION_ERROR("VALIDATION_ERROR", "Request validation failed"),
    RESOURCE_NOT_FOUND("RESOURCE_NOT_FOUND", "Resource not found"),
    IDEMPOTENCY_CONFLICT("IDEMPOTENCY_CONFLICT", "Duplicate eventId detected"),
    VENDOR_DISABLED("VENDOR_DISABLED", "Vendor is disabled"),
    
    // 服务端错误 (5xx)
    DATABASE_ERROR("DATABASE_ERROR", "Database operation failed"),
    MQ_ERROR("MQ_ERROR", "Message queue operation failed"),
    INTERNAL_ERROR("INTERNAL_ERROR", "Internal server error"),
    
    // 外部调用错误
    HTTP_TIMEOUT("HTTP_TIMEOUT", "External API call timeout"),
    HTTP_4XX("HTTP_4XX", "External API returned 4xx error"),
    HTTP_5XX("HTTP_5XX", "External API returned 5xx error"),
    NETWORK_ERROR("NETWORK_ERROR", "Network connection failed");
    
    private final String code;
    private final String message;
}
```

### 日志脱敏规则

为保护敏感信息，系统对以下内容进行脱敏处理：

1. **HTTP Headers**:
   - Authorization: `Bearer ***`
   - X-API-Key: `***`
   - Token: `***`
   - Cookie: `***`

2. **请求体和响应体**:
   - 如果包含 password、secret、token 等关键字的字段，值替换为 `***`
   - 响应体超过 1MB 时截断，只保留前 1KB

3. **URL 参数**:
   - 如果 URL 包含 token、key、secret 等参数，值替换为 `***`

**脱敏实现示例**:
```java
public class SensitiveDataMasker {
    
    private static final Set<String> SENSITIVE_HEADERS = Set.of(
        "authorization", "x-api-key", "token", "cookie"
    );
    
    private static final Pattern SENSITIVE_PATTERN = Pattern.compile(
        "(password|secret|token|key)\\s*[:=]\\s*[\"']?([^\"',\\s}]+)",
        Pattern.CASE_INSENSITIVE
    );
    
    public static String maskHeaders(Map<String, String> headers) {
        return headers.entrySet().stream()
            .map(e -> {
                String key = e.getKey();
                String value = SENSITIVE_HEADERS.contains(key.toLowerCase()) 
                    ? "***" 
                    : e.getValue();
                return key + ": " + value;
            })
            .collect(Collectors.joining(", "));
    }
    
    public static String maskBody(String body) {
        if (body == null) return null;
        if (body.length() > 1024 * 1024) {
            body = body.substring(0, 1024) + "... [truncated]";
        }
        return SENSITIVE_PATTERN.matcher(body)
            .replaceAll("$1: \"***\"");
    }
}
```

## Testing Strategy

### 测试方法论

系统采用**双重测试策略**，结合单元测试和属性测试，确保全面的代码覆盖和正确性验证：

1. **单元测试（Unit Tests）**
   - 验证特定示例和边界条件
   - 测试组件间的集成点
   - 测试错误处理逻辑
   - 使用 JUnit 5 + Mockito

2. **属性测试（Property-Based Tests）**
   - 验证通用属性在所有输入下成立
   - 通过随机生成大量测试用例
   - 发现边界情况和意外行为
   - 使用 jqwik (Java QuickCheck)

### 测试框架选择

**属性测试框架**: jqwik

jqwik 是 Java 生态中最成熟的属性测试框架，提供：
- 与 JUnit 5 无缝集成
- 强大的数据生成器（Arbitraries）
- 自动缩小失败用例（Shrinking）
- 丰富的组合器和约束

**依赖配置**:
```xml
<dependency>
    <groupId>net.jqwik</groupId>
    <artifactId>jqwik</artifactId>
    <version>1.8.2</version>
    <scope>test</scope>
</dependency>
```

### 属性测试配置

每个属性测试必须：
- 运行至少 **100 次迭代**（通过 `@Property(tries = 100)` 配置）
- 使用注释标记对应的设计属性：`// Feature: api-notification-system, Property X: [property text]`
- 使用智能生成器约束输入空间

**示例配置**:
```java
@Property(tries = 100)
// Feature: api-notification-system, Property 1: API 请求接受与响应
void validRequestShouldBeAcceptedWithin200ms(
    @ForAll("validNotificationRequests") CreateNotificationRequest request
) {
    long startTime = System.currentTimeMillis();
    ResponseEntity<CreateNotificationResponse> response = 
        controller.createNotification(request);
    long duration = System.currentTimeMillis() - startTime;
    
    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
    assertThat(response.getBody().getNotificationId()).isNotNull();
    assertThat(duration).isLessThan(200);
}

@Provide
Arbitrary<CreateNotificationRequest> validNotificationRequests() {
    return Combinators.combine(
        Arbitraries.strings().alpha().ofMinLength(1).ofMaxLength(50),
        Arbitraries.of("https://api.example.com/notify", "https://vendor.com/callback"),
        Arbitraries.of(HttpMethod.values()),
        Arbitraries.maps(
            Arbitraries.strings().alpha().ofMinLength(1),
            Arbitraries.strings().ofMinLength(1)
        ),
        Arbitraries.strings().ofMinLength(1)
    ).as((vendorCode, targetUrl, method, headers, body) -> 
        CreateNotificationRequest.builder()
            .vendorCode(vendorCode)
            .targetUrl(targetUrl)
            .httpMethod(method)
            .headers(headers)
            .body(body)
            .build()
    );
}
```

### 测试分层策略

#### 1. 单元测试层

**测试范围**: 单个类或方法的逻辑

**测试示例**:
- `NotificationServiceTest`: 测试业务逻辑
- `RetryPolicyServiceTest`: 测试重试算法
- `SensitiveDataMaskerTest`: 测试脱敏逻辑

**关注点**:
- 边界条件（空值、空列表、极值）
- 异常情况（数据库异常、MQ 异常）
- 特定业务规则

#### 2. 集成测试层

**测试范围**: 多个组件协作

**测试示例**:
- `NotificationControllerIntegrationTest`: 测试 API 端到端流程
- `DispatcherIntegrationTest`: 测试投递流程

**使用工具**:
- `@SpringBootTest`: 启动完整 Spring 上下文
- `TestContainers`: 提供真实的 MySQL 和 RocketMQ 环境
- `WireMock`: 模拟外部 HTTP API

**示例**:
```java
@SpringBootTest
@Testcontainers
class NotificationControllerIntegrationTest {
    
    @Container
    static MySQLContainer<?> mysql = new MySQLContainer<>("mysql:8.0");
    
    @Container
    static GenericContainer<?> rocketmq = new GenericContainer<>("apache/rocketmq:5.1.0");
    
    @Autowired
    private NotificationController controller;
    
    @Autowired
    private NotificationTaskRepository repository;
    
    @Test
    void shouldPersistTaskBeforeReturning() {
        CreateNotificationRequest request = createValidRequest();
        
        ResponseEntity<CreateNotificationResponse> response = 
            controller.createNotification(request);
        
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        
        String notificationId = response.getBody().getNotificationId();
        Optional<NotificationTask> task = repository.findById(notificationId);
        
        assertThat(task).isPresent();
        assertThat(task.get().getStatus()).isEqualTo(TaskStatus.PENDING);
    }
}
```

#### 3. 属性测试层

**测试范围**: 验证通用属性

**测试示例**:
- 所有 28 个设计属性都应该有对应的属性测试
- 每个属性测试运行 100+ 次迭代

**关注点**:
- Round-trip 属性（序列化/反序列化）
- 不变量（状态转换规则）
- 幂等性
- 错误处理的一致性

### 测试数据生成器

为了编写有效的属性测试，需要定义智能的数据生成器：

```java
public class NotificationArbitraries {
    
    // 生成有效的 HTTP 方法
    public static Arbitrary<HttpMethod> httpMethods() {
        return Arbitraries.of(HttpMethod.GET, HttpMethod.POST, 
                              HttpMethod.PUT, HttpMethod.DELETE);
    }
    
    // 生成有效的 URL
    public static Arbitrary<String> validUrls() {
        return Combinators.combine(
            Arbitraries.of("http", "https"),
            Arbitraries.strings().alpha().ofMinLength(3).ofMaxLength(20),
            Arbitraries.strings().alpha().ofMinLength(2).ofMaxLength(10)
        ).as((protocol, domain, path) -> 
            String.format("%s://%s.com/%s", protocol, domain, path)
        );
    }
    
    // 生成无效的 URL
    public static Arbitrary<String> invalidUrls() {
        return Arbitraries.of(
            "not-a-url",
            "ftp://invalid-protocol.com",
            "http://",
            "://missing-protocol.com",
            ""
        );
    }
    
    // 生成任务状态
    public static Arbitrary<TaskStatus> taskStatuses() {
        return Arbitraries.of(TaskStatus.values());
    }
    
    // 生成重试次数（0 到 max_retry 之间）
    public static Arbitrary<Integer> retryCount(int maxRetry) {
        return Arbitraries.integers().between(0, maxRetry);
    }
    
    // 生成 HTTP 状态码
    public static Arbitrary<Integer> httpStatusCodes() {
        return Arbitraries.frequencyOf(
            Tuple.of(5, Arbitraries.integers().between(200, 299)), // 2xx
            Tuple.of(2, Arbitraries.integers().between(400, 499)), // 4xx
            Tuple.of(2, Arbitraries.integers().between(500, 599))  // 5xx
        );
    }
    
    // 生成包含敏感信息的 headers
    public static Arbitrary<Map<String, String>> headersWithSensitiveData() {
        return Arbitraries.maps(
            Arbitraries.of("Authorization", "X-API-Key", "Token", "Content-Type"),
            Arbitraries.strings().ofMinLength(10).ofMaxLength(50)
        ).ofMinSize(1).ofMaxSize(5);
    }
}
```

### 测试覆盖率目标

- **行覆盖率**: ≥ 80%
- **分支覆盖率**: ≥ 75%
- **核心业务逻辑**: ≥ 90%

使用 JaCoCo 进行覆盖率统计：

```xml
<plugin>
    <groupId>org.jacoco</groupId>
    <artifactId>jacoco-maven-plugin</artifactId>
    <version>0.8.11</version>
    <executions>
        <execution>
            <goals>
                <goal>prepare-agent</goal>
            </goals>
        </execution>
        <execution>
            <id>report</id>
            <phase>test</phase>
            <goals>
                <goal>report</goal>
            </goals>
        </execution>
        <execution>
            <id>check</id>
            <goals>
                <goal>check</goal>
            </goals>
            <configuration>
                <rules>
                    <rule>
                        <element>PACKAGE</element>
                        <limits>
                            <limit>
                                <counter>LINE</counter>
                                <value>COVEREDRATIO</value>
                                <minimum>0.80</minimum>
                            </limit>
                        </limits>
                    </rule>
                </rules>
            </configuration>
        </execution>
    </executions>
</plugin>
```

### 持续集成

所有测试应该在 CI/CD 流水线中自动执行：

1. **提交阶段**: 运行单元测试（< 5 分钟）
2. **集成阶段**: 运行集成测试（< 15 分钟）
3. **验收阶段**: 运行属性测试（< 30 分钟）

**失败处理**:
- 任何测试失败都应该阻止合并
- 属性测试失败时，jqwik 会自动缩小失败用例，便于调试
- 记录失败的随机种子，便于重现问题
