#!/bin/bash

# API 通知系统测试脚本

BASE_URL="http://localhost:8080/api"

echo "========================================="
echo "API 通知系统功能测试"
echo "========================================="
echo ""

# 测试 1: 创建通知（使用 vendorCode）
echo "测试 1: 创建通知（使用 vendorCode）"
echo "-----------------------------------"
RESPONSE1=$(curl -s -X POST "${BASE_URL}/notifications" \
  -H "Content-Type: application/json" \
  -d '{
    "vendorCode": "AD_SYSTEM_A",
    "httpMethod": "POST",
    "headers": {
      "Content-Type": "application/json",
      "X-Trace-Id": "test-trace-001"
    },
    "body": {
      "userId": "user_12345",
      "eventType": "USER_REGISTERED",
      "eventId": "evt_202501140001"
    },
    "eventId": "test_event_001"
  }')

echo "响应: $RESPONSE1"
NOTIFICATION_ID1=$(echo $RESPONSE1 | grep -o '"notificationId":"[^"]*"' | cut -d'"' -f4)
echo "通知ID: $NOTIFICATION_ID1"
echo ""

# 测试 2: 创建通知（使用 targetUrl）
echo "测试 2: 创建通知（使用 targetUrl）"
echo "-----------------------------------"
RESPONSE2=$(curl -s -X POST "${BASE_URL}/notifications" \
  -H "Content-Type: application/json" \
  -d '{
    "targetUrl": "https://api.example.com/webhook",
    "httpMethod": "POST",
    "body": {
      "message": "Hello World"
    },
    "maxRetry": 3,
    "callbackTimeoutMs": 5000
  }')

echo "响应: $RESPONSE2"
NOTIFICATION_ID2=$(echo $RESPONSE2 | grep -o '"notificationId":"[^"]*"' | cut -d'"' -f4)
echo "通知ID: $NOTIFICATION_ID2"
echo ""

# 测试 3: 查询通知状态
echo "测试 3: 查询通知状态"
echo "-----------------------------------"
if [ ! -z "$NOTIFICATION_ID1" ]; then
  RESPONSE3=$(curl -s -X GET "${BASE_URL}/notifications/${NOTIFICATION_ID1}")
  echo "响应: $RESPONSE3"
else
  echo "跳过：未获取到通知ID"
fi
echo ""

# 测试 4: 幂等性测试（重复提交相同 eventId）
echo "测试 4: 幂等性测试（重复提交相同 eventId）"
echo "-----------------------------------"
RESPONSE4=$(curl -s -X POST "${BASE_URL}/notifications" \
  -H "Content-Type: application/json" \
  -d '{
    "vendorCode": "AD_SYSTEM_A",
    "httpMethod": "POST",
    "body": {
      "test": "duplicate"
    },
    "eventId": "test_event_001"
  }')

echo "响应: $RESPONSE4"
echo "预期: 应该返回 409 冲突错误"
echo ""

# 测试 5: 参数验证测试（缺少必填字段）
echo "测试 5: 参数验证测试（缺少必填字段）"
echo "-----------------------------------"
RESPONSE5=$(curl -s -X POST "${BASE_URL}/notifications" \
  -H "Content-Type: application/json" \
  -d '{
    "body": {
      "test": "validation"
    }
  }')

echo "响应: $RESPONSE5"
echo "预期: 应该返回 400 验证错误"
echo ""

# 测试 6: 查询不存在的通知
echo "测试 6: 查询不存在的通知"
echo "-----------------------------------"
RESPONSE6=$(curl -s -w "\nHTTP_CODE:%{http_code}" -X GET "${BASE_URL}/notifications/not_exist_id")
echo "响应: $RESPONSE6"
echo "预期: 应该返回 404"
echo ""

echo "========================================="
echo "测试完成！"
echo "========================================="
echo ""
echo "提示："
echo "1. 可以访问 http://localhost:8080/api/h2-console 查看数据库"
echo "2. JDBC URL: jdbc:h2:mem:notification_system"
echo "3. 用户名: sa, 密码: (空)"
