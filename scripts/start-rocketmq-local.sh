#!/bin/bash

# 本地启动 RocketMQ（不使用 Docker）
# 需要先下载 RocketMQ: https://rocketmq.apache.org/download

echo "=========================================="
echo "本地启动 RocketMQ"
echo "=========================================="
echo ""

# 检查 ROCKETMQ_HOME 环境变量
if [ -z "$ROCKETMQ_HOME" ]; then
    echo "错误: 请设置 ROCKETMQ_HOME 环境变量"
    echo ""
    echo "下载 RocketMQ:"
    echo "  wget https://dist.apache.org/repos/dist/release/rocketmq/5.1.0/rocketmq-all-5.1.0-bin-release.zip"
    echo "  unzip rocketmq-all-5.1.0-bin-release.zip"
    echo "  export ROCKETMQ_HOME=/path/to/rocketmq-all-5.1.0-bin-release"
    echo ""
    exit 1
fi

echo "ROCKETMQ_HOME: $ROCKETMQ_HOME"
echo ""

# 启动 NameServer
echo "1. 启动 NameServer..."
nohup sh $ROCKETMQ_HOME/bin/mqnamesrv > /tmp/rocketmq-namesrv.log 2>&1 &
NAMESRV_PID=$!
echo "  NameServer PID: $NAMESRV_PID"
echo "  日志: /tmp/rocketmq-namesrv.log"
sleep 3

# 启动 Broker
echo ""
echo "2. 启动 Broker..."
nohup sh $ROCKETMQ_HOME/bin/mqbroker -n localhost:9876 > /tmp/rocketmq-broker.log 2>&1 &
BROKER_PID=$!
echo "  Broker PID: $BROKER_PID"
echo "  日志: /tmp/rocketmq-broker.log"
sleep 5

# 检查状态
echo ""
echo "3. 检查 RocketMQ 状态..."
if ps -p $NAMESRV_PID > /dev/null; then
    echo "  ✓ NameServer 正在运行"
else
    echo "  ✗ NameServer 启动失败"
fi

if ps -p $BROKER_PID > /dev/null; then
    echo "  ✓ Broker 正在运行"
else
    echo "  ✗ Broker 启动失败"
fi

echo ""
echo "=========================================="
echo "RocketMQ 已启动"
echo "=========================================="
echo ""
echo "NameServer: localhost:9876"
echo ""
echo "停止 RocketMQ:"
echo "  sh $ROCKETMQ_HOME/bin/mqshutdown broker"
echo "  sh $ROCKETMQ_HOME/bin/mqshutdown namesrv"
echo ""
echo "查看日志:"
echo "  tail -f /tmp/rocketmq-namesrv.log"
echo "  tail -f /tmp/rocketmq-broker.log"
