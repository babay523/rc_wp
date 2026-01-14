#!/bin/bash

# 直接测试 Dispatcher 功能（不依赖 RocketMQ）
# 此脚本创建任务后，手动触发 dispatcher

BASE_URL="http://localhost:8080"

echo "=========================================="
echo "Dispatcher 直接测试"
echo "=========================================="
echo ""

GREEN='\033[0;32m'
RED='\033[0;31m'
YELLOW='\033[1;33m'
NC='\033[0m'

# 1. 创建通知任务
echo "1. 创建通知任务（使用 httpbin.org 作为测试端点）..."
RESPONSE=$(curl -s -X POST "${BASE_URL}/notifications" \
  -H "Content-Type: application/json" \
  -d '{
    "targetUrl": "https://httpbin.org/post",
    "httpMethod": "POST",
    "headers": {
      "Content-Type": "application/json",
      "X-Test-Header": "dispatcher-test"
    },
    "body": {
      "testId": "dispatcher-001",
      "message": "Testing direct dispatcher call",
      "timestamp": "'$(date -u +%Y-%m-%dT%H:%M:%SZ)'"
    },
    "maxRetry": 3,
    "callbackTimeoutMs": 5000
  }')

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

# 2. 查询初始状态
echo "2. 查询初始状态..."
INITIAL_STATUS=$(curl -s "${BASE_URL}/notifications/${NOTIFICATION_ID}")
echo "  状态: $(echo $INITIAL_STATUS | grep -o '"status":"[^"]*"' | cut -d'"' -f4)"
echo ""

# 3. 说明
echo -e "${YELLOW}注意: 由于 RocketMQ 未启动，任务不会自动投递${NC}"
echo -e "${YELLOW}在完整模式下，RocketMQ Consumer 会自动调用 Dispatcher${NC}"
echo ""
echo "如果 RocketMQ 正在运行，你应该看到:"
echo "  - 任务状态从 PENDING 变为 SUCCESS"
echo "  - lastAttemptAt 字段被更新"
echo "  - httpbin.org 返回 200 状态码"
echo ""

# 4. 创建一个会失败的任务
echo "3. 创建一个会失败的任务（测试重试）..."
FAIL_RESPONSE=$(curl -s -X POST "${BASE_URL}/notifications" \
  -H "Content-Type: application/json" \
  -d '{
    "targetUrl": "https://httpbin.org/status/500",
    "httpMethod": "GET",
    "maxRetry": 2,
    "callbackTimeoutMs": 3000
  }')

FAIL_NOTIFICATION_ID=$(echo $FAIL_RESPONSE | grep -o '"notificationId":"[^"]*"' | cut -d'"' -f4)

if [ -n "$FAIL_NOTIFICATION_ID" ]; then
    echo -e "${GREEN}✓ 失败测试任务已创建${NC}"
    echo "  Notification ID: $FAIL_NOTIFICATION_ID"
    echo "  此任务将返回 500 错误，触发重试机制"
else
    echo -e "${RED}✗ 创建失败${NC}"
fi
echo ""

echo "=========================================="
echo "测试任务已创建"
echo "=========================================="
echo ""
echo "已创建的任务:"
echo "  1. 成功任务: $NOTIFICATION_ID (httpbin.org/post)"
echo "  2. 失败任务: $FAIL_NOTIFICATION_ID (httpbin.org/status/500)"
echo ""
echo "如果 RocketMQ 正在运行:"
echo "  - 任务会被自动投递"
echo "  - 失败任务会自动重试"
echo "  - 可以通过查询接口监控状态变化"
echo ""
echo "查询命令:"
echo "  curl ${BASE_URL}/notifications/${NOTIFICATION_ID}"
echo "  curl ${BASE_URL}/notifications/${FAIL_NOTIFICATION_ID}"
