# 学生宿舍收费管理系统

基于 SpringBoot 3.2 + React 18 的全栈高并发宿舍收费管理系统。

## 技术栈

| 层级 | 技术 |
|------|------|
| **后端** | Spring Boot 3.2.5, MyBatis-Plus 3.5.6, Redis, RabbitMQ, Spring AI |
| **前端** | React 18, Ant Design 5, ECharts, Vite |
| **数据库** | MySQL 8.0 |
| **工具** | Apache POI (Excel), JavaMail, Alipay SDK (Stub), Lombok, Hutool |

## 项目结构

```
SchoolDormitorySystem/
├── backed/                          # 后端 Spring Boot 项目
│   ├── src/main/java/org/java/backed/
│   │   ├── BackedApplication.java   # 启动类
│   │   ├── common/                  # 通用工具 (Result, RedisLock, SnowflakeID...)
│   │   ├── config/                  # 配置类 (Redis, RabbitMQ, Alipay, CORS...)
│   │   ├── entity/                  # 实体类 (7张表)
│   │   ├── mapper/                  # MyBatis-Plus Mapper
│   │   ├── service/                 # 业务服务层
│   │   ├── controller/              # REST API 控制器
│   │   ├── consumer/                # RabbitMQ 消费者
│   │   ├── task/                    # 定时任务
│   │   └── util/                    # 工具类 (Excel, Email, Alipay)
│   ├── src/main/resources/
│   │   ├── application.yaml         # 主配置文件
│   │   └── db/schema.sql            # 数据库初始化脚本
│   └── pom.xml                      # Maven 依赖
├── fronted/                         # 前端 React 项目
│   └── src/
│       ├── layouts/BasicLayout.jsx  # 主布局 (侧边栏+顶栏)
│       ├── pages/
│       │   ├── Dashboard/           # 首页仪表盘 (ECharts图表)
│       │   ├── Student/             # 学生管理 (CRUD+Excel导入导出)
│       │   ├── FeeItem/             # 收费项目管理
│       │   ├── Bill/                # 账单管理 (生成+导出)
│       │   ├── Payment/             # 在线缴费 (支付宝沙箱)
│       │   ├── Statistics/          # 统计报表
│       │   └── AiQa/                # AI智能问答
│       ├── services/api.js          # API 请求封装
│       └── utils/request.js         # Axios 拦截器
├── API接口文档.md                   # 完整 REST API 文档
├── 数据库设计文档.md                 # 数据库表结构设计
└── 设计要求.txt                     # 原始设计文档
```

## 6大核心模块

1. **学生宿舍信息管理** - 录入、查询、Excel批量导入导出
2. **宿舍收费项目管理** - 收费类型配置、分布式锁防并发修改
3. **缴费记录管理** - 自动账单生成、状态管理、异步导出
4. **在线缴费** - 支付宝沙箱集成、防重复支付、幂等回调
5. **收费统计与报表** - 多维统计、ECharts可视化、逾期预警通知
6. **SpringAI智能问答** - 本地知识库匹配、缓存、限流、降级

## 高并发特性

- Redis缓存热点数据（收费配置、学生信息等）
- RabbitMQ异步解耦（账单生成、通知发送、支付回调）
- Redis分布式锁防并发支付、重复账单
- 接口速率限制（AI问答、支付接口）
- 数据库索引优化（学号、宿舍号、账单状态、支付时间）
- 定时任务（逾期检查、订单超时关闭、统计预计算）

## 快速开始

### 前置要求

- Java 17+
- MySQL 8.0+
- Node.js 18+
- (可选) Redis, RabbitMQ

### 1. 初始化数据库

```bash
# 修改 application.yaml 中的数据库密码
# 然后执行:
mysql -u root -p < backed/src/main/resources/db/schema.sql
```

### 2. 配置 application.yaml

编辑 `backed/src/main/resources/application.yaml`:
- MySQL 连接密码
- Redis 连接 (如未安装可注释)
- RabbitMQ 连接 (如未安装可注释)
- 支付宝沙箱配置 (使用Stub实现，无需配置即可运行)
- OpenAI API Key (AI问答功能需要)

### 3. 启动后端

```bash
cd backed
./mvnw spring-boot:run
```

后端启动在 http://localhost:8080

### 4. 启动前端

```bash
cd fronted
npm install
npm run dev
```

前端启动在 http://localhost:3000，自动代理API到后端

### 5. 访问系统

打开 http://localhost:3000 即可访问完整系统。

## 数据库表

| 表名 | 说明 |
|------|------|
| student_dormitory | 学生宿舍信息表 |
| fee_item | 收费项目表 |
| payment_bill | 缴费账单表 |
| payment_record | 支付流水表 |
| notification_record | 通知记录表 |
| ai_qa_log | AI问答日志表 |
| operation_log | 操作日志表 |

## 编译验证

```bash
# 后端编译 (已验证 BUILD SUCCESS)
cd backed && ./mvnw compile

# 前端编译 (已验证)
cd fronted && npm run build
```

## 注意事项

1. **支付宝SDK**: 当前使用Stub实现（模拟支付）。集成真实支付宝需要：
   - 下载 alipay-sdk-java 并安装到本地Maven仓库
   - 配置真实的沙箱APPID/密钥
   - 替换 `AlipayUtil.java` 中的 Stub 实现

2. **Spring AI**: 需要配置有效的 OpenAI API Key，否则使用本地知识库+降级回复

3. **Redis/RabbitMQ**: 系统设计支持，但基础CRUD功能不依赖这两个中间件

4. **邮件通知**: 需要配置真实的SMTP服务器参数
