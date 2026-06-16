# 学生宿舍收费管理系统 - API接口文档

## 基础信息
- **Base URL**: `http://localhost:8080/api`
- **Content-Type**: `application/json`
- **字符编码**: UTF-8
- **统一响应格式**:
```json
{
  "code": 200,
  "message": "success",
  "data": {}
}
```
- **分页响应格式**:
```json
{
  "code": 200,
  "message": "success",
  "data": {
    "records": [],
    "total": 100,
    "page": 1,
    "pageSize": 10
  }
}
```

---

## 一、学生宿舍信息管理模块

### 1.1 分页查询学生列表
- **URL**: `GET /api/students`
- **描述**: 多条件组合筛选+分页查询学生宿舍信息
- **参数**:
  - `page` (int, 默认1): 页码
  - `pageSize` (int, 默认10): 每页条数
  - `studentName` (String, 可选): 学生姓名(模糊)
  - `studentNo` (String, 可选): 学号(精确)
  - `dormitoryNo` (String, 可选): 宿舍号(模糊)
  - `paymentStatus` (String, 可选): 缴费状态(PAID/UNPAID/OVERDUE)
- **响应示例**:
```json
{
  "code": 200,
  "message": "success",
  "data": {
    "records": [{
      "id": 1, "studentName": "张三", "studentNo": "2024001",
      "dormitoryNo": "A-101", "phone": "13800138000",
      "checkInDate": "2024-09-01", "paymentStatus": "UNPAID",
      "createTime": "2024-09-01T10:00:00"
    }],
    "total": 50, "page": 1, "pageSize": 10
  }
}
```

### 1.2 根据ID查询学生
- **URL**: `GET /api/students/{id}`
- **描述**: 查询单个学生详细信息

### 1.3 新增学生
- **URL**: `POST /api/students`
- **描述**: 单条手动录入学生信息
- **请求体**:
```json
{
  "studentName": "张三",
  "studentNo": "2024001",
  "dormitoryNo": "A-101",
  "phone": "13800138000",
  "checkInDate": "2024-09-01",
  "paymentStatus": "UNPAID"
}
```

### 1.4 批量导入学生(Excel)
- **URL**: `POST /api/students/batch`
- **描述**: 上传Excel文件批量导入学生信息
- **Content-Type**: `multipart/form-data`
- **参数**: `file` (Excel文件, .xls/.xlsx)

### 1.5 更新学生信息
- **URL**: `PUT /api/students/{id}`
- **描述**: 修改学生基本信息、宿舍号、联系方式等
- **请求体**: 同新增，但字段均可选

### 1.6 删除学生
- **URL**: `DELETE /api/students/{id}`
- **描述**: 删除学生记录(逻辑删除)

### 1.7 导出学生数据
- **URL**: `GET /api/students/export`
- **描述**: 异步导出学生数据为Excel文件
- **参数**: 同查询参数(用于筛选导出范围)

---

## 二、宿舍收费项目管理模块

### 2.1 分页查询收费项目
- **URL**: `GET /api/fee-items`
- **描述**: 查询所有收费项目列表
- **参数**:
  - `page` / `pageSize`: 分页参数
  - `feeType` (String, 可选): 收费类型(WATER/ELECTRICITY/ACCOMMODATION/AC/NETWORK)
  - `status` (String, 可选): 状态(ACTIVE/INACTIVE)

### 2.2 根据ID查询收费项目
- **URL**: `GET /api/fee-items/{id}`

### 2.3 新增收费项目
- **URL**: `POST /api/fee-items`
- **请求体**:
```json
{
  "itemName": "住宿费",
  "feeType": "ACCOMMODATION",
  "unitPrice": 1200.00,
  "billingCycle": "SEMESTER",
  "applicableDormType": "STANDARD",
  "status": "ACTIVE"
}
```

### 2.4 更新收费项目
- **URL**: `PUT /api/fee-items/{id}`
- **描述**: 修改收费项目（需分布式锁防止并发修改）

### 2.5 删除收费项目(软删除)
- **URL**: `DELETE /api/fee-items/{id}`

---

## 三、缴费记录/账单管理模块

### 3.1 分页查询账单
- **URL**: `GET /api/bills`
- **参数**:
  - `page` / `pageSize`: 分页参数
  - `studentNo` (String, 可选): 学号
  - `dormitoryNo` (String, 可选): 宿舍号
  - `semester` (String, 可选): 学期
  - `status` (String, 可选): 状态(UNPAID/PAID/OVERDUE/CANCELLED)
  - `feeType` (String, 可选): 收费类型
- **响应**:
```json
{
  "code": 200,
  "message": "success",
  "data": {
    "records": [{
      "id": 1, "billNo": "BILL20240901001", "studentName": "张三",
      "studentNo": "2024001", "dormitoryNo": "A-101",
      "feeItemName": "住宿费", "semester": "2024-1",
      "amount": 1200.00, "paidAmount": 0.00,
      "dueDate": "2024-10-01", "status": "UNPAID",
      "createTime": "2024-09-01T10:00:00"
    }],
    "total": 100, "page": 1, "pageSize": 10
  }
}
```

### 3.2 查询账单详情
- **URL**: `GET /api/bills/{id}`

### 3.3 手动触发账单生成
- **URL**: `POST /api/bills/generate`
- **描述**: 根据收费项目+学生信息批量生成账单（MQ异步处理）
- **请求体**:
```json
{
  "semester": "2024-1",
  "feeItemIds": [1, 2, 3]
}
```

### 3.4 修正账单状态(管理员)
- **URL**: `PUT /api/bills/{id}/status`
- **描述**: 管理员手动修正异常账单状态，记录操作日志
- **请求体**:
```json
{
  "status": "PAID",
  "remark": "线下已缴，手动修正"
}
```

### 3.5 批量导入缴费记录(Excel)
- **URL**: `POST /api/bills/import`
- **描述**: 上传Excel批量补录缴费记录
- **Content-Type**: `multipart/form-data`
- **参数**: `file`

### 3.6 导出账单(Excel)
- **URL**: `GET /api/bills/export`
- **描述**: 异步导出账单数据，生成后通知下载
- **参数**: 同查询参数

---

## 四、在线缴费模块（核心）

### 4.1 创建支付订单
- **URL**: `POST /api/payment/create-order`
- **描述**: 为指定账单创建支付宝支付订单（需分布式锁防重）
- **请求体**:
```json
{
  "billId": 1
}
```
- **响应**:
```json
{
  "code": 200,
  "message": "success",
  "data": {
    "orderNo": "PAY20240901001",
    "payUrl": "https://openapi.alipaydev.com/gateway.do?...",
    "amount": 1200.00,
    "qrCode": "..."
  }
}
```

### 4.2 支付宝支付回调(同步)
- **URL**: `GET /api/payment/callback`
- **描述**: 支付完成后支付宝同步回调，幂等处理
- **参数**: 支付宝回调参数（由支付宝SDK解析）

### 4.3 支付宝异步通知
- **URL**: `POST /api/payment/notify`
- **描述**: 支付宝异步通知，更新账单状态
- **Content-Type**: `application/x-www-form-urlencoded`
- **参数**: 支付宝异步通知参数

### 4.4 查询用户支付记录
- **URL**: `GET /api/payment/records`
- **描述**: 学生查询自己的缴费记录
- **参数**:
  - `studentNo` (String): 学号
  - `page` / `pageSize`: 分页

### 4.5 查询支付详情
- **URL**: `GET /api/payment/records/{orderNo}`

### 4.6 下载缴费凭证
- **URL**: `GET /api/payment/receipt/{orderNo}`
- **描述**: 生成/下载电子缴费凭证PDF
- **响应**: PDF文件流

---

## 五、收费统计与报表模块

### 5.1 首页仪表盘概览
- **URL**: `GET /api/statistics/overview`
- **描述**: 获取Dashboard核心统计数据
- **响应**:
```json
{
  "code": 200,
  "message": "success",
  "data": {
    "totalStudents": 500,
    "totalBillsThisSemester": 450,
    "totalAmount": 560000.00,
    "paidAmount": 380000.00,
    "collectionRate": 67.86,
    "overdueCount": 85,
    "overdueAmount": 102000.00,
    "unpaidCount": 70
  }
}
```

### 5.2 收缴率统计(按收费类型)
- **URL**: `GET /api/statistics/collection-rate`
- **参数**: `semester` (String, 可选): 学期
- **响应**: 按收费类型分组的收缴率数据数组

### 5.3 欠费统计
- **URL**: `GET /api/statistics/arrears`
- **参数**:
  - `page` / `pageSize`: 分页
  - `dormitoryNo` (String, 可选): 按宿舍楼筛选
- **响应**: 欠费学生列表及金额

### 5.4 月度报表
- **URL**: `GET /api/statistics/monthly-report`
- **参数**: `year` (int), `month` (int)

### 5.5 学期报表
- **URL**: `GET /api/statistics/semester-report`
- **参数**: `semester` (String): 学期标识

### 5.6 导出统计报表
- **URL**: `GET /api/statistics/export`
- **参数**: `reportType` (String): 报表类型(MONTHLY/SEMESTER/ARREARS)
- **描述**: 异步生成Excel报表文件

### 5.7 手动触发逾期通知
- **URL**: `POST /api/statistics/send-overdue-notice`
- **描述**: 管理员手动触发逾期通知发送
- **请求体**:
```json
{
  "channel": "EMAIL",
  "studentIds": [1, 2, 3]
}
```

---

## 六、SpringAI智能问答模块

### 6.1 AI问答
- **URL**: `POST /api/ai/ask`
- **描述**: 向AI提问，优先匹配本地知识库
- **限流**: 单用户每分钟最多5次
- **请求体**:
```json
{
  "question": "为什么我的住宿费一直显示未缴?"
}
```
- **响应**:
```json
{
  "code": 200,
  "message": "success",
  "data": {
    "question": "为什么我的住宿费一直显示未缴？",
    "answer": "您的住宿费显示未缴可能是因为...",
    "source": "LOCAL_KB",
    "confidence": 0.95
  }
}
```

### 6.2 查询问答历史
- **URL**: `GET /api/ai/history`
- **参数**:
  - `userId` (String): 用户标识
  - `page` / `pageSize`: 分页

---

## 通用错误码

| 错误码 | 说明 |
|-------|------|
| 200 | 成功 |
| 400 | 请求参数错误 |
| 401 | 未授权 |
| 403 | 无权限 |
| 404 | 资源不存在 |
| 409 | 数据冲突（重复支付等） |
| 429 | 请求频率超限 |
| 500 | 服务器内部错误 |
| 503 | 服务暂不可用（降级） |
