#!/bin/bash

# API 通知系统 - Full Mode 测试脚本
# 使用 webhook.site 作为外部 API 接收器

BASE_URL="http://localhost:8080"
WEBHOOK_URL="https://webhook.site/unique-id-here"  # 请替换为你的 webhook.site URL

echo "=========================================="
echo "API 通知系统 - Full Mode 测试"
echo "=========================================="
echo ""

# 颜色定义
GREEN='\033[0;32m'
RED='\033[0;31m'
YELLOW='\033[1;33m'
NC='\033[0m' # No Color

# 检查服务是否运行
echo "1. 检查服务状态..."
if curl -s -f "${BASE_URL}/actuator/health" > /dev/null 2>&1; then
    echo -e "${GREEN}✓ 服务正在运行${NC}"
else
    echo -e "${RED}✗ 服务未运行，请先启动服务${NC}"
    echo "启动命令: mvn spring-boot:run -Dspring-boot.run.profiles=dev"
    exit 1
fi
echo ""

# 提示用户设置 webhook
echo -e "${YELLOW}注意: 请访问 https://webhook.site 获取一个唯一的 webhook URL${NC}"
echo -e "${YELLOW}然后修改此脚本中的 WEBHOOK_URL 变量${NC}"
echo ""
read -p "按 Enter 继续测试..."
echo ""

# 测试 1: 创建通知任务（使用 webhook.site）
echo "2. 测试创建通知任务（使用外部 webhook）..."
RESPONSE=$(curl -s -X POST "${BASE_URL}/notifications" \
  -H "Content-Type: application/json" \
  -d "{
    \"targetUrl\": \"${WEBHOOK_URL}\",
    \"httpMethod\": \"POST\",
    \"headers\": {
      \"Content-Type\": \"application/json\",
      \"X-Test-Header\": \"full-mode-test\"
    },
    \"body\": {
      \"testId\": \"full-mode-001\",
      \"message\": \"Testing full notification delivery\",
      \"timestamp\": \"$(date -u +%Y-%m-%dT%H:%M:%SZ)\"
    },
    \"maxRetry\": 3,
    \"callbackTimeoutMs\": 5000,
    \"eventId\": \"evt-full-$(date +%s)\"
  }")

NOTIFICATION_ID=$(echo $RESPONSE | grep -o '"notificationId":"[^"]*"' | cut -d'"' -f4)

if [ -n "$NOTIFICATION_ID" ]; then
    echo -e "${GREEN}✓ 通知任务已创建${NC}"
    echo "  Notification ID: $NOTIFICATION_ID"
else
    echo -e "${RED}✗ 创建失败${NC}"
    echo "  Response: $RESPONSE"
    exit 1
fi
echo ""

# 等待投递
echo "3. 等待通知投递..."
echo "  (如果 RocketMQ 正在运行，通知将被投递到 webhook.site)"
echo "  请在浏览器中打开 ${WEBHOOK_URL} 查看接收到的请求"
echo ""

# 轮询查询状态
for i in {1..10}; do
    sleep 2
    STATUS_RESPONSE=$(curl -s "${BASE_URL}/notifications/${NOTIFICATION_ID}")
    STATUS=$(echo $STATUS_RESPONSE | grep -o '"status":"[^"]*"' | cut -d'"' -f4)
    RETRY_COUNT=$(echo $STATUS_RESPONSE | grep -o '"retryCount":[0-9]*' | cut -d':' -f2)
    
    echo "  [$i/10] 当前状态: $STATUS, 重试次数: $RETRY_COUNT"
    
    if [ "$STATUS" = "SUCCESS" ]; then
        echo -e "${GREEN}✓ 通知投递成功！${NC}"
        break
    elif [ "$STATUS" = "FAILED" ]; then
        echo -e "${RED}✗ 通知投递失败${NC}"
        ERROR_CODE=$(echo $STATUS_RESPONSE | grep -o '"lastErrorCode":"[^"]*"' | cut -d'"' -f4)
        ERROR_MSG=$(echo $STATUS_RESPONSE | grep -o '"lastErrorMessage":"[^"]*"' | cut -d'"' -f4)
        echo "  错误代码: $ERROR_CODE"
        echo "  错误信息: $ERROR_MSG"
        break
    fi
done
echo ""

# 测试 2: 测试重试机制（使用无效 URL）
echo "4. 测试重试机制（使用无效 URL）..."
RETRY_RESPONSE=$(curl -s -X POST "${BASE_URL}/notifications" \
  -H "Content-Type: application/json" \
  -d '{
    "targetUrl": "http://invalid-domain-that-does-not-exist.com/api",
    "httpMethod": "POST",
    "body": {"test": "retry"},
    "maxRetry": 2,
    "callbackTimeoutMs": 3000,
    "eventId": "evt-retry-'$(date +%s)'"
  }')

RETRY_NOTIFICATION_ID=$(echo $RETRY_RESPONSE | grep -o '"notificationId":"[^"]*"' | cut -d'"' -f4)

if [ -n "$RETRY_NOTIFICATION_ID" ]; then
    echo -e "${GREEN}✓ 重试测试任务已创建${NC}"
    echo "  Notification ID: $RETRY_NOTIFICATION_ID"
    echo "  此任务将会失败并触发重试机制"
else
    echo -e "${RED}✗ 创建失败${NC}"
fi
echo ""

# 等待重试
echo "5. 监控重试过程..."
for i in {1..15}; do
    sleep 2
    RETRY_STATUS_RESPONSE=$(curl -s "${BASE_URL}/notifications/${RETRY_NOTIFICATION_ID}")
    RETRY_STATUS=$(echo $RETRY_STATUS_RESPONSE | grep -o '"status":"[^"]*"' | cut -d'"' -f4)
    RETRY_COUNT=$(echo $RETRY_STATUS_RESPONSE | grep -o '"retryCount":[0-9]*' | cut -d':' -f2)
    
    echo "  [$i/15] 状态: $RETRY_STATUS, 重试次数: $RETRY_COUNT"
    
    if [ "$RETRY_STATUS" = "FAILED" ] && [ "$RETRY_COUNT" -ge 2 ]; then
        echo -e "${GREEN}✓ 重试机制正常工作（达到最大重试次数后标记为失败）${NC}"
        break
    fi
done
echo ""

# 测试 3: 测试幂等性
echo "6. 测试幂等性..."
EVENT_ID="evt-idempotent-$(date +%s)"

# 第一次请求
FIRST_RESPONSE=$(curl -s -X POST "${BASE_URL}/notifications" \
  -H "Content-Type: application/json" \
  -d "{
    \"targetUrl\": \"${WEBHOOK_URL}\",
    \"httpMethod\": \"POST\",
    \"body\": {\"test\": \"idempotency\"},
    \"eventId\": \"${EVENT_ID}\"
  }")

FIRST_ID=$(echo $FIRST_RESPONSE | grep -o '"notificationId":"[^"]*"' | cut -d'"' -f4)
echo "  第一次请求 ID: $FIRST_ID"

# 第二次请求（相同 eventId）
sleep 1
SECOND_RESPONSE=$(curl -s -w "\nHTTP_CODE:%{http_code}" -X POST "${BASE_URL}/notifications" \
  -H "Content-Type: application/json" \
  -d "{
    \"targetUrl\": \"${WEBHOOK_URL}\",
    \"httpMethod\": \"POST\",
    \"body\": {\"test\": \"idempotency\"},
    \"eventId\": \"${EVENT_ID}\"
  }")

HTTP_CODE=$(echo "$SECOND_RESPONSE" | grep "HTTP_CODE" | cut -d':' -f2)

if [ "$HTTP_CODE" = "409" ]; then
    echo -e "${GREEN}✓ 幂等性检查正常工作（返回 409 Conflict）${NC}"
else
    echo -e "${RED}✗ 幂等性检查失败（应该返回 409）${NC}"
    echo "  HTTP Code: $HTTP_CODE"
fi
echo ""

echo "=========================================="
echo "测试完成！"
echo "=========================================="
echo ""
echo "总结:"
echo "1. 通知创建和投递: 检查 webhook.site 是否收到请求"
echo "2. 重试机制: 查看日志确认重试逻辑"
echo "3. 幂等性: 已验证"
echo ""
echo "查看详细日志:"
echo "  tail -f target/spring-boot.log"
echo ""
echo "查看数据库:"
echo "  访问 http://localhost:8080/h2-console"
echo "  JDBC URL: jdbc:h2:mem:notification_system"
echo "  Username: sa"
echo "  Password: (留空)"
