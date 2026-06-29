# 智慧宿舍收费管理系统

基于 Spring Boot + React + AI 的全栈宿舍收费管理系统，支持账单管理、支付宝缴费、知识库智能问答。

## 技术栈

| 层级 | 技术 |
|------|------|
| 后端 | Spring Boot 3.2.5、MyBatis-Plus 3.5.6、Spring Security + JWT |
| 数据库 | MySQL 8.0 |
| 缓存 | Redis 7 |
| 消息队列 | RabbitMQ 3.13 |
| 对象存储 | MinIO |
| 向量数据库 | Milvus 2.4 |
| 前端 | React 19、TypeScript、Vite 8 |
| UI | Ant Design 5、Tailwind CSS 4 |
| 图表 | ECharts |
| 支付 | 支付宝沙箱 |
| AI | SSE 流式输出、知识库 RAG |

## 功能模块

### 后台管理（管理员/教师）

| 模块 | 功能 |
|------|------|
| 数据概览 | 仪表盘统计卡片、收缴率环形图、收费类型分布饼图 |
| 学生管理 | 学生 CRUD、照片上传、批量导入导出 Excel、模糊搜索 |
| 宿舍管理 | 宿舍楼栋楼层 CRUD |
| 收费项目 | 收费项目 CRUD、自定义收费类型、模糊搜索 |
| 账单管理 | 单条新建、批量生成、状态修正、Excel 导出 |
| 支付管理 | 支付记录查询、学号筛选 |
| 统计报表 | 各类型收缴率柱状图、欠费明细表、动态学期筛选 |
| 用户管理 | 系统用户 CRUD、角色筛选、头像上传 |
| 角色管理 | 角色 CRUD、菜单权限分配 |
| AI 问答 | 流式输出、知识库检索、文档溯源、多轮对话 |
| 知识库 | 文档上传、自动分块向量化、语义搜索 |

### 学生端

| 模块 | 功能 |
|------|------|
| 我的宿舍 | 宿舍信息、个人缴费进度 |
| 我的账单 | 待缴账单、支付宝支付、确认缴费、支付记录 |
| AI 助手 | 基于知识库的智能问答 |
| 个人信息 | 账户信息查看 |

## 快速启动

### 环境要求

- JDK 17+
- Node.js 18+
- Docker Desktop
- MySQL 8.0

### 1. 启动中间件

```bash
docker-compose up -d
```

启动 Redis、RabbitMQ、MinIO、Milvus。

### 2. 初始化数据库

```bash
# 一键导入（含建表 + 种子数据 + 1000 测试学生 + 10000 账单）
mysql -u root -p < backed/src/main/resources/db/schema.sql
```

数据库完全由 SQL 脚本管理，启动后端**不会**自动修改数据库。

### 3. 配置后端

复制 `backed/src/main/resources/application-example.yaml` 为 `application.yaml`，修改数据库密码等配置。

### 4. 启动后端

```bash
cd backed
mvn spring-boot:run
```

后端运行在 `http://localhost:8080`。

### 5. 启动前端

```bash
cd fronted
npm install
npm run dev
```

前端运行在 `http://localhost:3001`。

### 6. 登录

| 角色 | 用户名 | 密码 |
|------|--------|------|
| 管理员 | admin | 123456 |
| 教师 | teacher | 123456 |
| 学生 | student | 123456 |

## 项目结构

```
SchoolDormitorySystem/
├── backed/                          # Spring Boot 后端
│   └── src/main/java/org/java/backed/
│       ├── ai/                      # AI 问答（SSE流式、知识库RAG）
│       ├── common/                  # 公共类（Result、雪花ID、分布式锁）
│       ├── config/                  # Spring 配置（Cache、Security、MinIO）
│       ├── consumer/                # RabbitMQ 消费者
│       ├── controller/              # REST 控制器
│       ├── entity/                  # 实体类
│       ├── mapper/                  # MyBatis 映射器
│       ├── security/                # JWT 认证 + 角色鉴权
│       ├── service/                 # 业务逻辑
│       └── util/                    # 工具（Excel、Email、JWT）
├── fronted/                         # React 前端
│   └── src/
│       ├── components/              # 公共组件（DocumentViewer等）
│       ├── layouts/                 # 布局（BasicLayout、StudentLayout）
│       ├── pages/                   # 页面组件（16个页面）
│       ├── services/                # API 调用
│       ├── stores/                  # Zustand 状态管理
│       ├── types/                   # TypeScript 类型定义
│       └── utils/                   # Axios 封装
├── docs/                            # 文档
│   ├── API.md                       # 接口文档
│   └── DATABASE.md                  # 数据库设计文档
└── docker-compose.yml
```

## 文档

- [接口文档](docs/API.md) — 全部 REST API 接口说明
- [数据库设计文档](docs/DATABASE.md) — 表结构 DDL 与种子数据
