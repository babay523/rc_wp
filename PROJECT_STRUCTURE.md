# 项目结构说明

本文档说明 API 通知系统的项目结构和文件组织。

## 📁 目录结构

```
api-notification-system/
├── .kiro/                     # Kiro 配置和规格文档
│   ├── settings/              # Kiro 设置
│   └── specs/                 # 功能规格文档
│       └── api-notification-system/
│           ├── requirements.md  # 需求文档
│           ├── design.md       # 设计文档
│           └── tasks.md        # 任务列表
│
├── docs/                      # 项目文档
│   ├── README.md             # 文档目录说明
│   ├── AI使用说明.md          # AI 辅助开发说明
│   ├── CHANGELOG.md          # 更新日志
│   ├── IMPLEMENTATION.md     # 实现指南
│   └── TESTING_FULL_MODE.md  # 完整模式测试指南
│
├── scripts/                   # 脚本工具
│   ├── README.md             # 脚本说明
│   ├── test-api.sh           # API 测试
│   ├── test-full-mode.sh     # 完整模式测试
│   ├── test-dispatcher-direct.sh  # 直接测试投递器
│   └── start-rocketmq-local.sh    # 启动本地 RocketMQ
│
├── src/                       # 源代码
│   ├── main/
│   │   ├── java/com/notification/
│   │   │   ├── config/       # 配置类
│   │   │   ├── controller/   # REST 控制器
│   │   │   ├── dto/          # 数据传输对象
│   │   │   ├── entity/       # 实体类
│   │   │   ├── mapper/       # MyBatis Mapper
│   │   │   ├── mq/           # 消息队列组件
│   │   │   ├── service/      # 业务服务
│   │   │   ├── util/         # 工具类
│   │   │   └── NotificationSystemApplication.java
│   │   └── resources/
│   │       ├── application.yml           # 主配置
│   │       ├── application-local.yml     # 本地模式配置
│   │       ├── application-dev.yml       # 开发模式配置
│   │       ├── application-prod.yml      # 生产模式配置
│   │       ├── schema-h2.sql            # H2 数据库脚本
│   │       └── db/
│   │           └── schema.sql           # MySQL 数据库脚本
│   └── test/                 # 测试代码
│       └── java/com/notification/
│           └── ManualDispatcherTest.java
│
├── target/                    # Maven 构建输出（忽略）
│
├── .gitignore                # Git 忽略文件
├── pom.xml                   # Maven 项目配置
├── PROJECT_STRUCTURE.md      # 本文件
└── README.md                 # 项目主文档
```

## 📄 核心文件说明

### 根目录文件

| 文件 | 说明 |
|------|------|
| `README.md` | 项目主文档，包含项目介绍、架构设计、快速开始 |
| `PROJECT_STRUCTURE.md` | 项目结构说明（本文件） |
| `pom.xml` | Maven 项目配置，定义依赖和构建配置 |
| `.gitignore` | Git 版本控制忽略文件 |

### 文档目录 (docs/)

| 文件 | 说明 |
|------|------|
| `IMPLEMENTATION.md` | 实现指南和技术细节 |
| `TESTING_FULL_MODE.md` | 完整模式测试指南 |
| `CHANGELOG.md` | 版本更新日志 |
| `AI使用说明.md` | AI 辅助开发说明 |

### 脚本目录 (scripts/)

| 脚本 | 说明 |
|------|------|
| `test-api.sh` | 测试 API 接口 |
| `test-full-mode.sh` | 测试完整模式 |
| `test-dispatcher-direct.sh` | 直接测试投递器 |
| `start-rocketmq-local.sh` | 启动本地 RocketMQ |

### 源代码目录 (src/)

#### Java 包结构

```
com.notification/
├── config/                    # 配置类
│   ├── MyBatisPlusConfig.java
│   ├── NotificationProperties.java
│   └── RocketMQConfig.java
│
├── controller/                # REST 控制器
│   └── NotificationController.java
│
├── dto/                       # 数据传输对象
│   ├── CreateNotificationRequest.java
│   ├── CreateNotificationResponse.java
│   ├── NotificationStatusResponse.java
│   ├── ErrorResponse.java
│   └── NotificationMessage.java
│
├── entity/                    # 实体类
│   ├── NotificationTask.java
│   ├── VendorConfig.java
│   ├── NotificationAttempt.java
│   └── enums/
│       ├── TaskStatus.java
│       ├── HttpMethod.java
│       ├── AuthType.java
│       └── ErrorCode.java
│
├── mapper/                    # MyBatis Mapper
│   ├── NotificationTaskMapper.java
│   └── VendorConfigMapper.java
│
├── mq/                        # 消息队列组件
│   ├── RocketMQProducer.java
│   ├── RocketMQConsumer.java
│   └── MockRocketMQProducer.java
│
├── service/                   # 业务服务
│   ├── NotificationService.java
│   ├── VendorConfigService.java
│   ├── NotificationDispatcher.java
│   ├── HttpClientService.java
│   └── RetryPolicyService.java
│
├── util/                      # 工具类
│   ├── IdGenerator.java
│   └── JsonUtil.java
│
└── NotificationSystemApplication.java  # 主启动类
```

## 🎯 文件组织原则

### 1. 按功能分类
- **docs/** - 所有文档集中管理
- **scripts/** - 所有脚本集中管理
- **docker/** - Docker 相关配置
- **src/** - 源代码按包结构组织

### 2. 清晰的命名
- 文档使用大写字母开头（如 `DEPLOYMENT.md`）
- 脚本使用小写字母和连字符（如 `docker-build.sh`）
- Java 类使用驼峰命名（如 `NotificationService.java`）

### 3. README 文件
- 每个主要目录都有 README.md 说明
- 根目录 README.md 是项目入口
- 子目录 README.md 说明该目录内容

### 4. 配置文件分离
- 开发、测试、生产环境配置分离
- Docker 配置独立目录
- 敏感信息通过环境变量传递

## 📚 相关文档

- [项目主文档](README.md)
- [文档目录](docs/README.md)
- [脚本说明](scripts/README.md)
- [部署指南](docs/DEPLOYMENT.md)

## 🔄 文件组织变更历史

### 2026-01-14
- 创建 `docs/` 目录，集中管理所有文档
- 创建 `scripts/` 目录，集中管理所有脚本
- 更新所有文档中的路径引用
- 添加各目录的 README.md 说明
- 创建本项目结构说明文档

## 💡 最佳实践

1. **添加新文档**：放入 `docs/` 目录，并更新 `docs/README.md`
2. **添加新脚本**：放入 `scripts/` 目录，添加执行权限，更新 `scripts/README.md`
3. **修改配置**：根据环境选择对应的配置文件
4. **更新代码**：遵循现有的包结构和命名规范
5. **版本更新**：更新 `docs/CHANGELOG.md` 记录变更

## 🆘 获取帮助

- 查看 [README.md](README.md) 了解项目概况
- 查看 [docs/README.md](docs/README.md) 浏览所有文档
- 查看 [scripts/README.md](scripts/README.md) 了解脚本用法
- 查看 [docs/DEPLOYMENT.md](docs/DEPLOYMENT.md) 获取部署帮助
