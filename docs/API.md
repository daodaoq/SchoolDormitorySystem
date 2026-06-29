# API 接口文档

Base URL: `http://localhost:8080/api`

## 认证

除登录、支付回调、文件访问外，所有接口需在请求头携带 JWT：

```
Authorization: Bearer <token>
```

Token 由登录接口返回，包含用户身份、角色、权限列表。过期后需重新登录。

---

## 通用响应格式

```json
{
  "code": 200,
  "message": "success",
  "data": { ... }
}
```

| code | 含义 |
|------|------|
| 200 | 成功 |
| 400 | 参数校验失败 |
| 401 | 未登录 / Token 过期 / 用户名密码错误 |
| 403 | 无权限 / 账号被禁用 |
| 404 | 资源不存在 |
| 409 | 冲突（重复操作） |
| 429 | 请求过于频繁 |
| 500 | 服务器内部错误 |

分页响应统一使用：

```json
{
  "code": 200,
  "data": {
    "records": [ ... ],
    "total": 1005,
    "page": 1,
    "pageSize": 10
  }
}
```

---

## 1. 认证模块 `/api/auth`

### POST `/auth/login` — 登录

限流：同一用户名或 IP 每分钟最多 10 次，超频返回 429。Redis 不可用时自动跳过限流。

```json
// Request
{
  "username": "admin",
  "password": "123456"
}

// Response 200
{
  "code": 200,
  "data": {
    "token": "eyJhbGciOiJIUzI1NiJ9...",
    "username": "admin",
    "realName": "系统管理员",
    "role": "ADMIN",
    "roleName": "管理员",
    "permissions": ["dashboard:view", "student:view", "fee:view", "..."],
    "menus": [
      { "id": 1, "parentId": 0, "menuName": "首页", "path": "/dashboard", "icon": "DashboardOutlined", "children": [] },
      { "id": 2, "parentId": 0, "menuName": "学生管理", "path": "/students", "icon": "UserOutlined", "children": [] },
      ...
    ],
    "studentInfo": {
      "id": 1,
      "studentNo": "2025001",
      "studentName": "张三",
      "dormitoryNo": "A-101",
      "phone": "13800138001",
      "checkInDate": "2026-09-01",
      "paymentStatus": "UNPAID"
    }
  }
}
```

> `studentInfo` 仅当登录用户角色为 `STUDENT` 且有关联学生记录时返回。

### GET `/auth/me` — 获取当前用户信息

```json
// Response 200
{
  "code": 200,
  "data": {
    "id": 1,
    "username": "admin",
    "realName": "系统管理员",
    "role": "ADMIN",
    "status": "ACTIVE",
    "avatar": null
  }
}
```

---

## 2. 学生管理 `/api/students`

### GET `/students` — 分页查询

| 参数 | 类型 | 必填 | 说明 |
|------|------|------|------|
| page | int | 否 | 页码，默认 1 |
| pageSize | int | 否 | 每页条数，默认 10 |
| studentName | string | 否 | 学生姓名（模糊匹配） |
| studentNo | string | 否 | 学号（模糊匹配） |
| dormitoryNo | string | 否 | 宿舍号（模糊匹配） |
| paymentStatus | string | 否 | 缴费状态（精确匹配：UNPAID / PAID / OVERDUE） |

```json
// Response 200
{
  "code": 200,
  "data": {
    "records": [
      {
        "id": 1,
        "studentName": "张三",
        "studentNo": "2025001",
        "dormitoryNo": "A-101",
        "phone": "13800138001",
        "photo": "/api/files/student-photos/abc123.jpg",
        "checkInDate": "2026-09-01",
        "paymentStatus": "UNPAID",
        "userId": 3,
        "deleted": 0,
        "createTime": "2026-06-17T12:00:00"
      }
    ],
    "total": 1005,
    "page": 1,
    "pageSize": 10
  }
}
```

### GET `/students/{id}` — 查询学生详情

### POST `/students` — 新增学生

新增时自动创建对应系统账号（用户名 = 学号，密码 = 123456，角色 = STUDENT）。

```json
// Request
{
  "studentName": "张三",
  "studentNo": "2025001",
  "dormitoryNo": "A-101",
  "phone": "13800138001",
  "checkInDate": "2026-09-01"
}

// Response 200 — 返回完整学生对象（含自动生成的 userId）
```

### PUT `/students/{id}` — 更新学生信息

```json
// Request（全字段更新，只需传要改的字段 + 必填项）
{
  "studentName": "张三",
  "studentNo": "2025001",
  "dormitoryNo": "B-201",
  "phone": "13900139001"
}
```

### DELETE `/students/{id}` — 删除学生

逻辑删除（`deleted = 1`），不会清除关联的系统账号。

### POST `/students/batch` — 批量导入 Excel

`Content-Type: multipart/form-data`，字段名 `file`。

Excel 模板列：学生姓名、学号、宿舍号、联系电话、入住日期。
导入时自动创建系统账号。

```json
// Response 200
{ "code": 200, "message": "成功导入 150 条学生数据", "data": 150 }
```

### GET `/students/export` — 导出 Excel

| 参数 | 类型 | 必填 | 说明 |
|------|------|------|------|
| studentName | string | 否 | 学生姓名筛选 |
| studentNo | string | 否 | 学号筛选 |
| dormitoryNo | string | 否 | 宿舍号筛选 |
| paymentStatus | string | 否 | 缴费状态筛选 |

返回 `application/vnd.openxmlformats-officedocument.spreadsheetml.sheet` 二进制流。

### POST `/students/{id}/photo` — 上传学生照片

`Content-Type: multipart/form-data`，字段名 `file`。照片存入 MinIO `student-photos` bucket。

### GET `/students/personnel` — 人员管理查询

关联查询学生记录与系统用户信息。

| 参数 | 类型 | 必填 | 说明 |
|------|------|------|------|
| page / pageSize | int | 否 | 分页参数 |
| studentName | string | 否 | 学生姓名（模糊） |
| studentNo | string | 否 | 学号（精确） |
| dormitoryNo | string | 否 | 宿舍号（模糊） |
| linked | bool | 否 | true=已关联 / false=未关联 |

```json
// Response 200 — 每条记录额外包含 username、userStatus 字段
{
  "code": 200,
  "data": {
    "records": [
      {
        "id": 1,
        "studentName": "张三",
        "studentNo": "2025001",
        "dormitoryNo": "A-101",
        "userId": 3,
        "username": "2025001",
        "userStatus": "ACTIVE"
      }
    ],
    "total": 1005
  }
}
```

### PUT `/students/{id}/link-user` — 关联系统用户

```json
// Request
{ "userId": 3 }
```

约束：目标用户角色必须为 `STUDENT`，且未被其他学生关联。

### PUT `/students/{id}/unlink-user` — 解除用户关联

无需请求体，直接解除该学生记录与系统用户的关联。

---

## 3. 宿舍管理 `/api/dormitories`

### GET `/dormitories` — 分页查询

| 参数 | 类型 | 必填 | 说明 |
|------|------|------|------|
| page / pageSize | int | 否 | 分页参数 |
| dormitoryNo | string | 否 | 宿舍编号（模糊） |
| building | string | 否 | 楼栋（模糊） |

### GET `/dormitories/active` — 获取所有启用宿舍

无需分页参数，返回 `List<Dormitory>`。用于下拉选择组件。

### POST `/dormitories` — 新增宿舍

```json
// Request
{
  "dormitoryNo": "D-401",
  "building": "D栋",
  "floor": "4层",
  "roomType": "双人间",
  "capacity": 2,
  "status": "ACTIVE"
}
```

### PUT `/dormitories/{id}` — 更新宿舍

### DELETE `/dormitories/{id}` — 删除宿舍

---

## 4. 收费项目管理 `/api/fee-items`

### GET `/fee-items` — 分页查询

| 参数 | 类型 | 必填 | 说明 |
|------|------|------|------|
| page / pageSize | int | 否 | 分页参数 |
| itemName | string | 否 | 项目名称（模糊） |
| feeType | string | 否 | 收费类型（模糊） |

### GET `/fee-items/active` — 获取启用项目列表

### GET `/fee-items/{id}` — 查询项目详情

### POST `/fee-items` — 新增收费项目

```json
// Request
{
  "itemName": "住宿费",
  "feeType": "住宿费",
  "unitPrice": 1200.00,
  "billingCycle": "SEMESTER",
  "applicableDormType": "ALL",
  "description": "标准四人间住宿费"
}
```

> `feeType` 为自由文本输入，不再限制枚举值。

### PUT `/fee-items/{id}` — 更新收费项目

使用 Redis 分布式锁防并发修改。

### DELETE `/fee-items/{id}` — 删除收费项目

---

## 5. 账单管理 `/api/bills`

### GET `/bills` — 分页查询

| 参数 | 类型 | 必填 | 说明 |
|------|------|------|------|
| page / pageSize | int | 否 | 分页参数 |
| studentNo | string | 否 | 学号（模糊） |
| dormitoryNo | string | 否 | 宿舍号（模糊） |
| semester | string | 否 | 学期，如 `2026-1`（精确） |
| status | string | 否 | 状态：UNPAID / PAID / OVERDUE / CANCELLED |
| feeType | string | 否 | 收费类型（模糊） |

### GET `/bills/{id}` — 查询账单详情

### POST `/bills` — 新建单条账单

```json
// Request
{
  "studentId": 1,
  "feeItemId": 1,
  "semester": "2026-1",
  "amount": 1200.00,
  "dueDate": "2026-06-30"
}
```

系统自动生成 `billNo`（雪花 ID），默认状态 `UNPAID`。

### POST `/bills/generate` — 批量生成账单

```json
// Request
{ "semester": "2026-1" }
```

为所有学生按所有启用收费项目生成账单，已存在的不重复生成。

```json
// Response 200
{ "code": 200, "message": "成功生成 6025 条账单", "data": 6025 }
```

### POST `/bills/{id}/pay` — 直接确认缴费（测试用）

跳过支付宝支付流程，直接标记账单为已缴，创建支付流水（`payMethod = "SYSTEM"`）。

### PUT `/bills/{id}/status` — 修正账单状态

```json
// Request
{ "status": "PAID", "remark": "学生已线下补缴" }
```

### GET `/bills/export` — 导出账单 Excel

| 参数 | 类型 | 必填 | 说明 |
|------|------|------|------|
| studentNo | string | 否 | 筛选 |
| dormitoryNo | string | 否 | 筛选 |
| semester | string | 否 | 筛选 |
| status | string | 否 | 筛选 |

---

## 6. 支付模块 `/api/payment`

### POST `/payment/create-order` — 创建支付宝订单

```json
// Request
{ "billId": 1 }

// Response 200
{
  "code": 200,
  "data": {
    "orderNo": "ORDER20260629123456001",
    "payUrl": "/api/payment/pay-page/ORDER20260629123456001"
  }
}
```

### GET `/payment/pay-page/{orderNo}` — 获取支付宝支付页面

返回 HTML 页面，包含自动提交的支付宝表单。前端可在新窗口/iframe 中打开。
**无需 Token。**

### GET `/payment/callback` — 支付宝同步回调

支付完成后支付宝跳转回此地址，展示支付结果。**无需 Token。**

### POST `/payment/notify` — 支付宝异步通知

支付宝服务器端通知，验证签名后更新账单和支付记录状态。**无需 Token。**

### GET `/payment/records` — 查询支付记录

| 参数 | 类型 | 必填 | 说明 |
|------|------|------|------|
| page / pageSize | int | 否 | 分页参数 |
| studentNo | string | 否 | 学号（模糊） |

### GET `/payment/records/{orderNo}` — 查询单条支付记录

---

## 7. 统计报表 `/api/statistics`

### GET `/statistics/overview` — 首页概览

```json
// Response 200
{
  "code": 200,
  "data": {
    "totalStudents": 1005,
    "totalBillsThisSemester": 7035,
    "totalAmount": 1200000.00,
    "paidAmount": 800000.00,
    "collectionRate": 66.67,
    "overdueCount": 835,
    "overdueAmount": 400000.00,
    "unpaidCount": 3165,
    "feeTypeDistribution": [
      { "feeType": "住宿费", "count": 1005, "totalAmount": 1206000.00 },
      { "feeType": "水费", "count": 1005, "totalAmount": 210000.00 },
      ...
    ]
  }
}
```

### GET `/statistics/student/{studentId}` — 学生个人缴费概览

```json
// Response 200
{
  "code": 200,
  "data": {
    "totalPaid": 2400.00,
    "totalDue": 3600.00,
    "unpaidCount": 2,
    "bills": [...]
  }
}
```

### GET `/statistics/collection-rate` — 各类型收缴率

| 参数 | 类型 | 必填 | 说明 |
|------|------|------|------|
| semester | string | 否 | 学期筛选 |

```json
// Response 200
{
  "code": 200,
  "data": [
    { "feeType": "住宿费", "totalAmount": 1206000.00, "paidAmount": 900000.00, "rate": 74.63 },
    ...
  ]
}
```

### GET `/statistics/arrears` — 欠费统计

| 参数 | 类型 | 必填 | 说明 |
|------|------|------|------|
| page / pageSize | int | 否 | 分页 |
| dormitoryNo | string | 否 | 宿舍号筛选 |

### GET `/statistics/semester-report` — 学期报表

按学期汇总收费、缴费、逾期数据。

### GET `/statistics/monthly-report` — 月度报表

按月份汇总。

---

## 8. 用户管理 `/api/users`

### GET `/users` — 分页查询

| 参数 | 类型 | 必填 | 说明 |
|------|------|------|------|
| page / pageSize | int | 否 | 分页参数 |
| username | string | 否 | 用户名（模糊） |
| role | string | 否 | 角色（精确：ADMIN / TEACHER / STUDENT） |

### GET `/users/{id}` — 查询用户详情

### POST `/users` — 新增用户

```json
// Request
{
  "username": "2025001",
  "password": "123456",
  "realName": "张三",
  "role": "STUDENT"
}
```

密码自动 BCrypt 加密存储。

### PUT `/users/{id}` — 更新用户

可修改 `realName`、`role`、`status`、`avatar` 等字段。

### DELETE `/users/{id}` — 删除用户

### PUT `/users/{id}/reset-password` — 重置密码

重置为 `123456`，无需传参。

### POST `/users/{id}/avatar` — 上传头像

`Content-Type: multipart/form-data`，字段名 `file`。存入 MinIO `avatars` bucket。

---

## 9. 角色管理 `/api/roles`

### GET `/roles` — 获取所有角色列表

```json
// Response 200
{
  "code": 200,
  "data": [
    { "id": 1, "roleCode": "ADMIN", "roleName": "管理员", "description": "系统管理员", "status": "ACTIVE" },
    { "id": 2, "roleCode": "TEACHER", "roleName": "宿管老师", "status": "ACTIVE" },
    { "id": 3, "roleCode": "STUDENT", "roleName": "学生", "status": "ACTIVE" }
  ]
}
```

### POST `/roles` — 新增角色

```json
// Request
{
  "roleCode": "CUSTOM",
  "roleName": "自定义角色",
  "description": "描述"
}
```

### PUT `/roles/{id}` — 更新角色

### DELETE `/roles/{id}` — 删除角色

### GET `/roles/{id}/menus` — 获取角色菜单权限

```json
// Response 200
{
  "code": 200,
  "data": {
    "menus": [...],
    "checkedKeys": [1, 2, 3, 4, 5]
  }
}
```

### POST `/roles/{id}/assign` — 分配菜单权限

```json
// Request
{ "menuIds": [1, 2, 3, 4, 5, 6, 7, 8, 9, 10] }
```

---

## 10. 菜单管理（内部）`/api/menus`

### GET `/menus/tree` — 获取菜单树

### POST `/menus` — 新增菜单

### PUT `/menus/{id}` — 更新菜单

### DELETE `/menus/{id}` — 删除菜单

> 菜单为代码驱动管理，通常无需在前端暴露。

---

## 11. 操作日志 `/api/logs`

### GET `/logs` — 分页查询

| 参数 | 类型 | 必填 | 说明 |
|------|------|------|------|
| page | int | 否 | 页码，默认 1 |
| pageSize | int | 否 | 每页条数，默认 20 |
| username | string | 否 | 操作人（模糊匹配） |
| module | string | 否 | 模块（精确匹配） |
| action | string | 否 | 操作类型（精确匹配） |
| status | string | 否 | SUCCESS / FAIL（精确匹配） |
| startDate | string | 否 | 开始日期，格式 `YYYY-MM-DD` |
| endDate | string | 否 | 结束日期，格式 `YYYY-MM-DD` |

```json
// Response 200
{
  "code": 200,
  "data": {
    "records": [
      {
        "id": 1,
        "userId": 1,
        "username": "admin",
        "realName": "系统管理员",
        "module": "认证",
        "action": "登录",
        "description": "用户登录",
        "method": "AuthController.login",
        "requestParams": "{\"username\":\"admin\"}",
        "duration": 125,
        "status": "SUCCESS",
        "errorMsg": null,
        "ipAddress": "127.0.0.1",
        "userAgent": "Mozilla/5.0 ...",
        "createTime": "2026-06-29T22:30:00"
      }
    ],
    "total": 1
  }
}
```

### GET `/logs/modules` — 已注册的操作模块列表

```json
// Response 200
{ "code": 200, "data": ["学生管理", "宿舍管理", "收费项目", "账单管理", "支付管理", "用户管理", "角色管理", "系统管理", "认证", "知识库"] }
```

### GET `/logs/actions` — 已注册的操作类型列表

```json
// Response 200
{ "code": 200, "data": ["新增", "修改", "删除", "登录", "支付", "导入", "导出", "上传"] }
```

### GET `/logs/stats?days=7` — 最近 N 天操作量统计

---

## 12. AI 问答 `/api/ai`

### POST `/ai/ask` — 非流式问答

```json
// Request
{ "question": "住宿费多少钱？", "userId": "student" }

// Response 200
{
  "code": 200,
  "data": {
    "answer": "根据宿舍类型不同，住宿费标准如下：...",
    "source": "AI",
    "citations": [...]
  }
}
```

### POST `/ai/ask/stream` — SSE 流式问答

```json
// Request
{ "question": "住宿费多少钱？", "userId": "student" }
```

`Content-Type: text/event-stream`。事件类型：

| 事件 | 说明 |
|------|------|
| `event: content` | 增量文本片段 |
| `event: done` | 流结束，data 为 `{ "status": "completed", "source": "AI", "citations": [...], "totalLength": 256 }` |
| `event: error` | 错误信息文本 |
| `: heartbeat` | 心跳注释（每 15 秒），防止连接超时 |

### GET `/ai/history` — 问答历史

| 参数 | 类型 | 必填 | 说明 |
|------|------|------|------|
| page / pageSize | int | 否 | 分页 |
| userId | string | 否 | 用户标识筛选 |

### GET `/ai/health` — AI 服务健康检查

```json
// Response 200
{ "code": 200, "data": { "deepseek": "ok", "bailian": "ok", "milvus": "ok" } }
```

---

## 13. 知识库 `/api/kb`

### GET `/kb/documents` — 文档列表

| 参数 | 类型 | 必填 | 说明 |
|------|------|------|------|
| page / pageSize | int | 否 | 分页 |

### GET `/kb/documents/{id}` — 文档详情

### POST `/kb/documents/upload` — 上传文档

`Content-Type: multipart/form-data`：

| 字段 | 类型 | 必填 | 说明 |
|------|------|------|------|
| file | File | 是 | 文档文件（PDF / DOCX / XLSX / TXT / MD） |
| title | string | 否 | 自定义标题 |
| description | string | 否 | 描述 |

上传后自动触发：MinIO 存储 → 文本提取 → 分块 → 向量嵌入 → 存入 Milvus。

### DELETE `/kb/documents/{id}` — 删除文档

同时清理 MinIO 文件、Milvus 向量和分块记录。已删除的文档不会出现在 AI 引用中。

### GET `/kb/documents/{id}/chunks` — 获取文档分块列表

### POST `/kb/documents/{id}/reprocess` — 重新处理文档

重新提取文本、分块、嵌入向量，覆盖旧数据。

### POST `/kb/search` — 语义搜索

```json
// Request
{ "query": "住宿费收费标准", "topK": 5 }

// Response 200
{
  "code": 200,
  "data": [
    {
      "chunkId": "abc123",
      "docId": 10,
      "docTitle": "宿舍收费标准",
      "content": "标准四人间：1200元/学期...",
      "score": 0.92,
      "chunkIndex": 0
    }
  ]
}
```

---

## 14. 文件访问 `/api/files`

### POST `/files/upload` — 上传文件到指定 bucket

`Content-Type: multipart/form-data`。

| 字段 | 类型 | 必填 | 说明 |
|------|------|------|------|
| file | File | 是 | 文件 |
| bucket | string | 是 | 存储桶名称 |

### GET `/files/{bucket}/{objectName}` — 访问文件

**无需 Token。** 从 MinIO 流式返回文件。

### DELETE `/files/{bucket}/{objectName}` — 删除文件

---

## 日志体系

所有涉及数据变更的 Controller 方法通过 `@OpLog` 注解自动记录审计日志：

| 模块 | 操作 | 接口 |
|------|------|------|
| 认证 | 登录 | POST /api/auth/login |
| 学生管理 | 新增/修改/删除/导入 | POST/PUT/DELETE /api/students |
| 账单管理 | 新增/生成/支付/状态修正 | POST /api/bills 等 |
| 用户管理 | 新增/修改/删除 | POST/PUT/DELETE /api/users |
| 角色管理 | 新增/修改/删除 | POST/PUT/DELETE /api/roles |

日志通过 AOP 切面自动采集（用户、IP、参数脱敏、耗时），经 RabbitMQ 异步写入数据库，不影响业务响应。
