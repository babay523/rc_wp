# 脚本目录

本目录包含 API 通知系统的测试和工具脚本。

## 🛠️ 脚本列表

### 测试脚本

#### test-api.sh
API 测试脚本

```bash
./scripts/test-api.sh
```

功能：
- 测试创建通知 API
- 测试查询通知状态 API
- 测试参数验证
- 测试幂等性

#### test-full-mode.sh
完整模式测试

```bash
./scripts/test-full-mode.sh
```

功能：
- 测试完整的通知流程
- 使用 webhook.site 接收通知
- 测试重试机制

#### test-dispatcher-direct.sh
直接测试投递器

```bash
./scripts/test-dispatcher-direct.sh
```

功能：
- 直接测试 NotificationDispatcher
- 使用 httpbin.org 作为目标
- 测试成功和失败场景

### RocketMQ 脚本

#### start-rocketmq-local.sh
启动本地 RocketMQ

```bash
./scripts/start-rocketmq-local.sh
```

功能：
- 启动本地 RocketMQ NameServer
- 启动本地 RocketMQ Broker
- 不依赖 Docker

## 📋 使用建议

### 快速开始流程

1. **启动应用**
   ```bash
   mvn spring-boot:run -Dspring-boot.run.profiles=local
   ```

2. **测试 API**
   ```bash
   ./scripts/test-api.sh
   ```

3. **测试完整流程**
   ```bash
   ./scripts/test-full-mode.sh
   ```

### 开发流程

1. **启动应用（开发模式）**
   ```bash
   # 如果需要 RocketMQ，先启动
   ./scripts/start-rocketmq-local.sh
   
   # 启动应用
   mvn spring-boot:run -Dspring-boot.run.profiles=dev
   ```

2. **测试功能**
   ```bash
   ./scripts/test-api.sh
   ./scripts/test-full-mode.sh
   ```

## 🔧 脚本维护

### 添加新脚本

1. 创建脚本文件
2. 添加执行权限：`chmod +x scripts/your-script.sh`
3. 更新本 README
4. 添加脚本说明和使用示例

### 脚本规范

- 使用 `#!/bin/bash` 作为 shebang
- 使用 `set -e` 确保错误时退出
- 添加颜色输出提升可读性
- 添加详细的注释
- 提供清晰的错误信息

### 颜色定义

脚本中使用的标准颜色：

```bash
GREEN='\033[0;32m'   # 成功信息
YELLOW='\033[1;33m'  # 警告信息
RED='\033[0;31m'     # 错误信息
BLUE='\033[0;34m'    # 提示信息
NC='\033[0m'         # 无颜色
```

## 📚 相关文档

- [实现指南](../docs/IMPLEMENTATION.md)
- [测试指南](../docs/TESTING_FULL_MODE.md)
- [项目主文档](../README.md)

## 🆘 问题反馈

如果脚本执行遇到问题：

1. 检查脚本是否有执行权限
2. 查看脚本输出的错误信息
3. 参考相关文档的故障排查章节
4. 在项目仓库提交 Issue
