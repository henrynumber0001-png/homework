# Homework 后台管理系统接口文档

> 文档版本：V1.1 Simplified Draft
>
> 目标模块：`web/web-admin`
>
> 设计依据：[Homework 后台管理系统设计文档](./web-admin-system-design.md)
>
> 接口前缀：`/api/admin`

## 1. 精简目标

V1 接口以题库和题目为业务核心：

- 题库、题目、图片、排序和 Excel 导入保留完整能力。
- 数据看板只保留一个概览接口。
- 用户、社区、会员和管理员只保留基础查询和合并动作接口。
- 操作日志只保留查询，不做详情和导出。
- 发布、下架、删除、恢复等低频状态操作合并为统一动作接口。

精简后的 V1 共 46 个接口，其中 18 个直接服务于题库和题目。

### 1.1 V1 暂不提供

- 独立的看板趋势和题库排行接口
- 题目跨题库关联入口
- 题目导入任务取消
- 独立的 Post、Comment 详情接口
- 会员变更流水分页接口
- 订单详情与任何退款接口
- 管理员详情、邀请重发和邀请撤销接口
- 操作日志详情和导出

低频模块需要更多信息时，优先扩展现有列表或详情响应，不提前增加接口。

## 2. 全局约定

### 2.1 认证

除邀请校验、接受邀请和登录外，所有接口都需要独立 Admin Token：

```http
Authorization: Bearer <admin_access_token>
```

Admin Token 与 App Token 不可互换。服务端每次请求校验管理员状态、会话状态和会话版本。

### 2.2 统一响应

继续使用项目现有 `Result<T>`：

```json
{
  "code": 200,
  "message": "success",
  "data": {}
}
```

分页继续使用 `PageResult<T>`：

```json
{
  "records": [],
  "total": 0,
  "pageNum": 1,
  "pageSize": 20
}
```

分页参数统一为 `pageNum`、`pageSize`，默认 `1`、`20`，`pageSize` 最大为 100。

文件下载接口直接返回文件流，不包裹 `Result<T>`。

### 2.3 时间与枚举

- 日期：`yyyy-MM-dd`
- 时间：`yyyy-MM-dd'T'HH:mm:ss`
- 统计时区：`Asia/Singapore`
- 管理端 DTO/VO 使用字符串枚举，不直接暴露数据库数字枚举值。

主要枚举：

| 类型 | 值 |
| --- | --- |
| `GroupType` | `INTERVIEW`、`CERTIFICATION` |
| `QuestionBankStatus` | `DRAFT`、`PUBLISHED`、`OFFLINE` |
| `QuestionType` | `ESSAY`、`SINGLE_CHOICE`、`MULTIPLE` |
| `UserStatus` | `ACTIVE`、`DISABLED`、`BANNED` |
| `MembershipType` | `PREMIUM`、`PREMIUM_PLUS` |
| `AdminStatus` | `INVITED`、`ACTIVE`、`DISABLED`、`ARCHIVED` |

### 2.4 乐观锁

可编辑资源返回 `version`。更新和状态动作必须提交当前版本：

```json
{
  "version": 3,
  "reason": "修改原因"
}
```

版本冲突返回业务码 `1205`，客户端刷新后重新提交。

### 2.5 幂等

V1 不增加通用幂等记录表。状态动作和编辑依靠当前状态与乐观锁版本避免重复生效；邀请 Token 只能接受一次；Excel 导入使用文件 SHA-256 和任务状态防止重复导入。创建类接口由前端在请求完成前禁用重复提交。

### 2.6 高风险二次认证

永久封禁用户、永久回收会员、套餐配置和管理员权限操作需要：

```http
X-Admin-Reauth-Token: <one_time_token>
```

二次认证令牌由 `/auth/reauth` 获取，有效期 5 分钟，仅能使用一次，并与当前管理员、会话和操作域绑定。

### 2.7 通用动作响应

`ActionResultVO`：

```json
{
  "targetId": 101,
  "action": "PUBLISH",
  "status": "PUBLISHED",
  "version": 4,
  "updatedTime": "2026-07-26T16:00:00"
}
```

## 3. 权限码

题库和题目使用细粒度权限，低频模块使用域级权限。

| 权限码 | 用途 |
| --- | --- |
| `dashboard:view` | 查看概览 |
| `bank:view` | 查看题库 |
| `bank:create` | 创建题库 |
| `bank:update` | 编辑题库 |
| `bank:publish` | 发布和下架题库 |
| `bank:delete` | 删除和恢复题库 |
| `question:view` | 查看题目 |
| `question:create` | 创建题目和上传图片 |
| `question:update` | 编辑题目 |
| `question:publish` | 发布和下架题目 |
| `question:delete` | 删除和恢复题目 |
| `question:sort` | 题库内排序 |
| `question:import` | Excel 预检和导入 |
| `user:view` | 查看用户 |
| `user:manage` | 临时禁用和社区限制 |
| `user:ban` | 永久封禁和解封，仅超级管理员 |
| `community:moderate` | 查看和治理 Post、Comment |
| `membership:view` | 查看会员、订单和套餐 |
| `membership:manage` | 发放、暂停和恢复会员 |
| `membership:revoke` | 永久回收会员，仅超级管理员 |
| `membership:plan` | 套餐配置，仅超级管理员 |
| `admin:manage` | 管理普通管理员，仅超级管理员 |
| `audit:view` | 查询操作日志 |

题库数据范围：

- `ALL_BANKS`
- `ASSIGNED_BANKS`

所有携带 `bankId` 的接口必须在 Service 层再次校验数据范围。

## 4. 管理员认证

### 4.1 接口清单

| 方法 | 路径 | 用途 |
| --- | --- | --- |
| `GET` | `/auth/invitations/{token}` | 校验邀请 |
| `POST` | `/auth/invitations/{token}/accept` | 接受邀请并设置密码 |
| `POST` | `/auth/login` | 登录 |
| `POST` | `/auth/logout` | 退出当前会话 |
| `GET` | `/auth/me` | 当前管理员、权限和题库范围 |
| `PUT` | `/auth/password` | 修改密码 |
| `POST` | `/auth/reauth` | 高风险二次认证 |

### 4.2 登录

```http
POST /api/admin/auth/login

{
  "email": "admin@example.com",
  "password": "StrongPassword",
  "turnstileToken": null
}
```

响应：

```json
{
  "accessToken": "eyJ...",
  "tokenType": "Bearer",
  "expiresInSeconds": 7200,
  "admin": {
    "id": 2,
    "email": "admin@example.com",
    "displayName": "Content Admin",
    "role": "STANDARD_ADMIN",
    "status": "ACTIVE"
  },
  "permissions": ["bank:view", "question:view"],
  "bankDataScope": "ASSIGNED_BANKS"
}
```

登录失败不区分邮箱不存在和密码错误。

### 4.3 邀请

校验邀请：

```http
GET /api/admin/auth/invitations/{token}
```

返回脱敏邮箱、显示名称、过期时间和是否有效。

接受邀请：

```http
POST /api/admin/auth/invitations/{token}/accept
{
  "password": "StrongPassword",
  "confirmPassword": "StrongPassword"
}
```

密码为 12～72 字，邀请 24 小时有效且只能使用一次。成功后不自动登录。

### 4.4 当前管理员

```http
GET /api/admin/auth/me
```

响应：

```json
{
  "admin": {
    "id": 2,
    "email": "admin@example.com",
    "displayName": "Content Admin",
    "role": "STANDARD_ADMIN",
    "status": "ACTIVE"
  },
  "permissions": [],
  "bankDataScope": "ASSIGNED_BANKS",
  "assignedBankIds": [101, 102],
  "sessionExpiresTime": "2026-07-26T18:00:00"
}
```

### 4.5 修改密码与二次认证

修改密码：

```http
PUT /api/admin/auth/password

{
  "currentPassword": "OldPassword",
  "newPassword": "NewStrongPassword",
  "confirmPassword": "NewStrongPassword"
}
```

成功后撤销其他会话。

二次认证：

```http
POST /api/admin/auth/reauth

{
  "password": "CurrentPassword",
  "actionScope": "membership:plan"
}
```

响应 `reauthToken` 和 `expiresTime`。

## 5. 分类树

分类树 V1 只读。

| 方法 | 路径 | 权限 |
| --- | --- | --- |
| `GET` | `/categories/tree` | `bank:view` 或 `bank:create` |

```http
GET /api/admin/categories/tree?groupType=INTERVIEW
```

响应：

```json
[
  {
    "id": 1,
    "groupName": "面试",
    "groupType": "INTERVIEW",
    "modules": [
      {
        "id": 11,
        "moduleName": "Java",
        "sortOrder": 10,
        "subModules": [
          {
            "id": 111,
            "subModuleName": "Spring Boot",
            "sortOrder": 10
          }
        ]
      }
    ]
  }
]
```

## 6. 数据看板

只保留一个概览接口。

| 方法 | 路径 | 权限 |
| --- | --- | --- |
| `GET` | `/dashboard` | `dashboard:view` |

```http
GET /api/admin/dashboard?date=2026-07-26
```

响应：

```json
{
  "statDate": "2026-07-26",
  "bankViews": {
    "daily": 1260,
    "total": 98231
  },
  "bankCompletedUsers": {
    "daily": 219,
    "total": 12590
  },
  "loginUsers": {
    "daily": 830,
    "total": 45210
  },
  "registeredUsers": {
    "daily": 91,
    "total": 50120
  },
  "postingUsers": {
    "daily": 130,
    "total": 9280
  },
  "paidUsers": {
    "premiumDaily": 12,
    "premiumTotal": 1920,
    "premiumPlusDaily": 4,
    "premiumPlusTotal": 610
  },
  "updatedTime": "2026-07-26T16:00:00"
}
```

无对应用户、社区或会员查看权限的指标返回 `null`。`ASSIGNED_BANKS` 管理员的题库指标只统计已分配题库。

## 7. 题库管理

### 7.1 接口清单

| 方法 | 路径 | 权限 |
| --- | --- | --- |
| `GET` | `/question-banks` | `bank:view` |
| `GET` | `/question-banks/{bankId}` | `bank:view` + 题库范围 |
| `POST` | `/question-banks` | `bank:create` |
| `PUT` | `/question-banks/{bankId}` | `bank:update` + 题库范围 |
| `POST` | `/question-banks/{bankId}/actions` | 按动作校验 + 题库范围 |

统一动作：

| 动作 | 权限 | 可用状态 |
| --- | --- | --- |
| `PUBLISH` | `bank:publish` | `DRAFT`、`OFFLINE` |
| `OFFLINE` | `bank:publish` | `PUBLISHED` |
| `DELETE` | `bank:delete` | `DRAFT`、`OFFLINE` |
| `RESTORE` | `bank:delete` | 已逻辑删除 |

### 7.2 题库列表

```http
GET /api/admin/question-banks?keyword=spring&groupType=INTERVIEW&moduleId=11&subModuleId=111&status=PUBLISHED&deleted=false&pageNum=1&pageSize=20&sortBy=UPDATED_TIME&sortDirection=DESC
```

允许的 `sortBy`：

- `CREATED_TIME`
- `UPDATED_TIME`
- `PUBLISHED_TIME`
- `PRIORITY`
- `VIEW_COUNT`
- `COMPLETE_COUNT`

记录：

```json
{
  "id": 101,
  "bankName": "Spring Boot 高频面试题",
  "group": {
    "id": 1,
    "name": "面试",
    "type": "INTERVIEW"
  },
  "module": {
    "id": 11,
    "name": "Java"
  },
  "subModule": {
    "id": 111,
    "name": "Spring Boot"
  },
  "status": "PUBLISHED",
  "tags": ["Spring", "Backend"],
  "priority": 10,
  "questionCount": 100,
  "releasedQuestionCount": 98,
  "viewCount": 5000,
  "completeCount": 930,
  "publishedTime": "2026-07-01T10:00:00",
  "updatedTime": "2026-07-25T18:00:00",
  "version": 3
}
```

`deleted=true` 只允许拥有 `bank:delete` 权限的管理员使用。

### 7.3 题库详情

```http
GET /api/admin/question-banks/101
```

在列表字段基础上返回：

```json
{
  "createAdmin": {
    "id": 2,
    "displayName": "Content Admin"
  },
  "createdTime": "2026-06-30T12:00:00",
  "deleted": false,
  "deleteReason": null
}
```

### 7.4 创建题库

```http
POST /api/admin/question-banks
{
  "subModuleId": 111,
  "bankName": "Spring Boot 高频面试题",
  "tags": ["Spring", "Backend"],
  "priority": 10
}
```

规则：

- 题库只能单条创建。
- `bankName` 去除首尾空格后全局唯一，最大 100 字。
- 标签最多 10 个，每个最大 30 字。
- `priority` 为 `0～9999`，默认 0。
- 初始状态固定为 `DRAFT`。
- `ASSIGNED_BANKS` 管理员自动获得新题库权限。

返回 HTTP `201` 和题库详情。

### 7.5 编辑题库

```http
PUT /api/admin/question-banks/101

{
  "subModuleId": 112,
  "bankName": "Spring Boot 核心面试题",
  "tags": ["Spring"],
  "priority": 20,
  "reason": "调整题库定位",
  "version": 3
}
```

有题目的题库只能在相同 `GroupType` 内调整 Module 或 SubModule。计数、状态、创建人和发布时间不能通过编辑接口修改。

### 7.6 题库动作

```http
POST /api/admin/question-banks/101/actions

{
  "action": "PUBLISH",
  "reason": "题库审核完成",
  "version": 3
}
```

规则：

- 发布前至少有一条已发布题目。
- 首次发布写入 `published_time`，重新发布不覆盖。
- 发布中的题库必须先下架才能删除。
- 删除为逻辑删除，不级联删除题目和用户数据。
- 恢复后的题库状态固定为 `OFFLINE`。

返回 `ActionResultVO`。

## 8. 题目管理

所有接口使用 `bankId + questionId` 定位题目，避免面试题表和认证题表出现相同 ID 时产生歧义。

### 8.1 接口清单

| 方法 | 路径 | 权限 |
| --- | --- | --- |
| `GET` | `/question-banks/{bankId}/questions` | `question:view` + 题库范围 |
| `GET` | `/question-banks/{bankId}/questions/{questionId}` | `question:view` + 题库范围 |
| `POST` | `/question-banks/{bankId}/questions` | `question:create` + 题库范围 |
| `PUT` | `/question-banks/{bankId}/questions/{questionId}` | `question:update` + 全部关联题库范围 |
| `POST` | `/question-banks/{bankId}/questions/{questionId}/actions` | 按动作校验 + 全部关联题库范围 |
| `PUT` | `/question-banks/{bankId}/questions/order` | `question:sort` + 题库范围 |
| `POST` | `/uploads/question-images` | `question:create` 或 `question:update` |

题目动作：

| 动作 | 权限 |
| --- | --- |
| `PUBLISH`、`OFFLINE` | `question:publish` |
| `DELETE`、`RESTORE` | `question:delete` |

### 8.2 题型规则

| 题库 Group | 题目表 | 允许题型 |
| --- | --- | --- |
| `INTERVIEW` | `question_info` | `ESSAY` |
| `CERTIFICATION` | `certificate_question_info` | `SINGLE_CHOICE`、`MULTIPLE` |

V1 不支持判断题。

### 8.3 题目列表

```http
GET /api/admin/question-banks/101/questions?keyword=事务&questionType=ESSAY&released=true&deleted=false&pageNum=1&pageSize=20&sortBy=BANK_ORDER&sortDirection=ASC
```

记录：

```json
{
  "id": 10001,
  "bankId": 101,
  "questionType": "ESSAY",
  "title": "Spring 事务失效的常见原因有哪些？",
  "imageUrl": null,
  "released": true,
  "deleted": false,
  "bankSortOrder": 10,
  "referencedBankCount": 1,
  "updatedTime": "2026-07-20T10:00:00",
  "version": 3
}
```

允许的 `sortBy`：`BANK_ORDER`、`CREATED_TIME`、`UPDATED_TIME`、`QUESTION_ID`。

### 8.4 题目详情

```json
{
  "id": 10001,
  "bankId": 201,
  "groupType": "CERTIFICATION",
  "questionType": "SINGLE_CHOICE",
  "title": "以下哪一项是正确的？",
  "imageUrl": "https://cdn.example.com/question/a.webp",
  "analysis": "答案说明",
  "options": [
    {
      "key": "A",
      "content": "选项 A"
    },
    {
      "key": "B",
      "content": "选项 B"
    }
  ],
  "correctAnswers": ["A"],
  "released": true,
  "deleted": false,
  "bankSortOrder": 20,
  "referencedBankCount": 1,
  "visibleReferencedBanks": [
    {
      "bankId": 201,
      "bankName": "认证题库"
    }
  ],
  "hasHiddenReferences": false,
  "version": 3
}
```

面试题的 `options`、`correctAnswers` 返回空数组。超出管理员数据范围的关联题库不返回 ID 和名称，只通过 `hasHiddenReferences=true` 表示，并禁止该管理员编辑题目主体。

### 8.5 创建题目

面试题：

```http
POST /api/admin/question-banks/101/questions
{
  "questionType": "ESSAY",
  "title": "Spring 事务失效的常见原因有哪些？",
  "analysis": "参考答案",
  "imageObjectKey": null
}
```

认证题：

```json
{
  "questionType": "MULTIPLE",
  "title": "以下哪些说法正确？",
  "analysis": "答案说明",
  "imageObjectKey": "admin-temp/questions/2026-07-26/1785062400000-a.webp",
  "options": [
    {
      "key": "A",
      "content": "选项 A"
    },
    {
      "key": "B",
      "content": "选项 B"
    },
    {
      "key": "C",
      "content": "选项 C"
    }
  ],
  "correctAnswers": ["A", "C"]
}
```

规则：

- 标题最大 5,000 字，解析最大 20,000 字。
- 选择题包含 2～26 个选项，Key 从 A 连续生成。
- 单选题只能有一个正确答案。
- 多选题至少有两个正确答案。
- 题型必须与题库 Group 匹配。
- 创建后默认未发布。
- 题目主体和 `question_bank_question` 关系在同一事务创建。

### 8.6 编辑题目

请求字段与创建 DTO 相同，增加：

```json
{
  "removeImage": false,
  "reason": "修正标准答案",
  "version": 3
}
```

规则：

- `imageObjectKey` 未传或为 `null` 时保留原图片，传入新的临时对象 Key 时替换原图片。
- 只有显式传入 `removeImage=true` 时才删除原图片；此时不能同时传 `imageObjectKey`。
- `ESSAY` 不能转换为选择题，选择题不能转换为 `ESSAY`。
- `SINGLE_CHOICE` 与 `MULTIPLE` 可以互转，但必须重新校验正确答案。
- 已发布题目修改标题、选项或正确答案时必须填写原因。
- 被多个题库引用时，管理员必须拥有全部关联题库数据权限。
- 编辑题目主体不修改任何题库中的排序。

### 8.7 题目动作

```http
POST /api/admin/question-banks/101/questions/10001/actions

{
  "action": "OFFLINE",
  "reason": "答案需要修订",
  "version": 3
}
```

规则：

- 发布只允许未发布、未删除题目。
- 删除前自动下架，恢复后保持未发布。
- 被多个题库引用的题目不能通过单个题库删除主体。
- 已开始的认证考试 Session 不受下架和排序变化影响。

### 8.8 题目排序

```http
PUT /api/admin/question-banks/101/questions/order

{
  "questionIds": [10003, 10001, 10002],
  "bankQuestionOrderVersion": 8,
  "reason": "按难度重新排序"
}
```

规则：

- 必须提交题库全部未删除题目 ID。
- 不能缺失、重复或包含其他题库题目。
- 数组顺序映射为 `10、20、30...`。
- `question_bank_question.sort_order` 是唯一排序来源。
- 全部关系在同一事务更新。
- 认证考试仍在创建 Session 时随机题序。

响应：

```json
{
  "bankId": 101,
  "questionCount": 3,
  "bankQuestionOrderVersion": 9,
  "updatedTime": "2026-07-26T16:00:00"
}
```

### 8.9 上传题目图片

```http
POST /api/admin/uploads/question-images
Content-Type: multipart/form-data

file=<binary>
```

响应：

```json
{
  "objectKey": "admin-temp/questions/2026-07-26/1785062400000-a.webp",
  "previewUrl": "https://example-1250000000.cos.ap-guangzhou.myqcloud.com/admin-temp/questions/2026-07-26/a.webp?q-sign-algorithm=sha1&...",
  "previewUrlExpiresTime": "2026-07-26T17:00:00",
  "uploadExpiresTime": "2026-07-27T16:00:00"
}
```

支持 JPG、PNG、WebP，单文件最大 5 MB。`previewUrl` 是私有存储桶的 1 小时只读签名地址，只用于即时预览；`objectKey` 是临时 COS 对象 Key，24 小时内可用于创建或编辑题目。题目保存时对象从临时目录复制到永久目录并删除临时对象，数据库只保存永久对象 Key，不保存签名 URL。未绑定的临时对象需要通过腾讯云生命周期规则、控制台或后续后台清理任务删除。

## 9. Excel 批量导入

### 9.1 接口清单

| 方法 | 路径 | 权限 |
| --- | --- | --- |
| `GET` | `/question-banks/{bankId}/question-import-template` | `question:import` + 题库范围 |
| `POST` | `/question-imports` | `question:import` + 题库范围 |
| `GET` | `/question-imports/{taskId}` | 创建人或超级管理员 |
| `GET` | `/question-imports/{taskId}/errors` | 创建人或超级管理员 |
| `POST` | `/question-imports/{taskId}/commit` | `question:import` + 题库范围 |

### 9.2 下载模板

```http
GET /api/admin/question-banks/101/question-import-template
```

根据题库 Group 返回面试题或认证题 `.xlsx` 模板。

### 9.3 上传与预检

```http
POST /api/admin/question-imports
Content-Type: multipart/form-data

bankId=101
file=<xlsx binary>
```

响应 HTTP `202`：

```json
{
  "taskId": "QIMPORT-20260726-000001",
  "bankId": 101,
  "fileName": "questions.xlsx",
  "status": "VALIDATING",
  "totalRows": null,
  "validRows": null,
  "errorRows": null,
  "expiresTime": "2026-07-27T16:00:00"
}
```

限制：

- 只支持 `.xlsx`。
- 文件最大 10 MB。
- 单次最多 1,000 行。
- 新导入题目默认未发布。
- 使用文件 SHA-256 防止相同文件重复导入。

### 9.4 查询结果与错误报告

```http
GET /api/admin/question-imports/{taskId}
GET /api/admin/question-imports/{taskId}/errors
```

任务状态：

- `VALIDATING`
- `READY`
- `INVALID`
- `IMPORTING`
- `SUCCEEDED`
- `FAILED`
- `EXPIRED`

有错误行时状态为 `INVALID`，错误报告返回原始行号、字段和错误原因。

### 9.5 确认导入

```http
POST /api/admin/question-imports/{taskId}/commit
{
  "confirmTotalRows": 200
}
```

只有 `READY` 且未过期的任务可以导入。存在任一错误行时不写入任何题目。

成功响应：

```json
{
  "taskId": "QIMPORT-20260726-000001",
  "status": "SUCCEEDED",
  "importedRows": 200,
  "finishedTime": "2026-07-26T16:02:00"
}
```

## 10. 用户管理

只保留查询、账号状态和社区权限。

### 10.1 接口清单

| 方法 | 路径 | 权限 |
| --- | --- | --- |
| `GET` | `/users` | `user:view` |
| `GET` | `/users/{userId}` | `user:view` |
| `POST` | `/users/{userId}/actions` | 按动作校验 |
| `PUT` | `/users/{userId}/community-access` | `user:manage` |

### 10.2 用户查询

```http
GET /api/admin/users?keyword=henry&status=ACTIVE&pageNum=1&pageSize=20
```

列表记录：

```json
{
  "id": 9001,
  "accountNo": "HW00009001",
  "displayName": "Henry",
  "avatar": "https://cdn.example.com/avatar.webp",
  "status": "ACTIVE",
  "membershipType": "PREMIUM",
  "registeredTime": "2026-01-01T12:00:00",
  "version": 2
}
```

详情增加脱敏登录身份、当前社区限制、会员摘要和内容数量。没有会员或社区权限时，相应字段返回 `null`。

### 10.3 用户状态动作

```http
POST /api/admin/users/9001/actions

{
  "action": "DISABLE",
  "reason": "账号异常，需要核查",
  "version": 2
}
```

| 动作 | 权限 | 状态转换 |
| --- | --- | --- |
| `DISABLE` | `user:manage` | `ACTIVE → DISABLED` |
| `ACTIVATE` | `user:manage` | `DISABLED → ACTIVE` |
| `BAN` | `user:ban` + 二次认证 | `ACTIVE/DISABLED → BANNED` |
| `UNBAN` | `user:ban` + 二次认证 | `BANNED → ACTIVE` |

### 10.4 社区权限

```http
PUT /api/admin/users/9001/community-access

{
  "restricted": true,
  "scope": "BOTH",
  "endTime": "2026-08-02T16:00:00",
  "reason": "短期限制社区发言",
  "version": 2
}
```

`scope` 为 `POST`、`COMMENT` 或 `BOTH`。`restricted=false` 表示提前恢复。社区限制不影响登录、刷题和会员时钟。

## 11. 社区内容治理

只保留列表和统一动作接口。

### 11.1 接口清单

| 方法 | 路径 | 权限 |
| --- | --- | --- |
| `GET` | `/community/posts` | `community:moderate` |
| `POST` | `/community/posts/{postId}/actions` | `community:moderate` |
| `GET` | `/community/comments` | `community:moderate` |
| `POST` | `/community/comments/{commentId}/actions` | `community:moderate` |

### 11.2 列表

```http
GET /api/admin/community/posts?keyword=hooks&userId=9001&status=PUBLISHED&pageNum=1&pageSize=20
GET /api/admin/community/comments?postId=701&userId=9001&status=PUBLISHED&pageNum=1&pageSize=20
```

列表直接返回治理所需的完整正文、作者、状态、发布时间和互动计数，不再提供独立详情接口。

### 11.3 内容动作

```http
POST /api/admin/community/posts/701/actions

{
  "action": "HIDE",
  "reason": "内容需要审核",
  "version": 1
}
```

Post 和 Comment 均支持：

- `HIDE`
- `RESTORE`
- `DELETE`

规则：

- 用户主动删除的内容不能由管理员恢复。
- 删除父 Comment 不删除子回复。
- 删除或恢复 Comment 必须幂等地更新 Post 有效评论数。
- 隐藏或删除 Post 不级联修改 Comment 状态。

## 12. 会员与订单

保留基础查询、合并会员动作、只读订单列表和套餐配置。V1 不提供退款。

### 12.1 接口清单

| 方法 | 路径 | 权限 |
| --- | --- | --- |
| `GET` | `/memberships` | `membership:view` |
| `GET` | `/memberships/users/{userId}` | `membership:view` |
| `POST` | `/memberships/users/{userId}/actions` | 按动作校验 |
| `GET` | `/membership-orders` | `membership:view` |
| `GET` | `/membership-plans` | `membership:view` |
| `POST` | `/membership-plans` | `membership:plan` + 二次认证 |
| `PUT` | `/membership-plans/{planId}` | `membership:plan` + 二次认证 |

### 12.2 会员查询

```http
GET /api/admin/memberships?keyword=HW00009001&membershipType=PREMIUM&pageNum=1&pageSize=20
GET /api/admin/memberships/users/9001
```

详情：

```json
{
  "userId": 9001,
  "accountNo": "HW00009001",
  "displayName": "Henry",
  "currentType": "PREMIUM_PLUS",
  "accessStatus": "ACTIVE",
  "premiumExpireTime": "2026-12-01T00:00:00",
  "premiumPlusExpireTime": "2026-09-01T00:00:00",
  "suspended": false,
  "recentChanges": [],
  "ledgerVersion": 5
}
```

`recentChanges` 最多返回最近 20 条会员变更，V1 不单独提供流水分页接口。

### 12.3 会员动作

```http
POST /api/admin/memberships/users/9001/actions
{
  "action": "GRANT",
  "membershipType": "PREMIUM",
  "durationMonths": 3,
  "reason": "活动奖励",
  "ledgerVersion": 5
}
```

| 动作 | 权限 | 额外字段 |
| --- | --- | --- |
| `GRANT` | `membership:manage` | `membershipType`、`durationMonths` |
| `SUSPEND` | `membership:manage` | 无 |
| `RESUME` | `membership:manage` | 无 |
| `REVOKE` | `membership:revoke` + 二次认证 | 无 |

暂停期间到期时间继续流逝，恢复后不补时长。所有动作写入不可变会员变更流水。

管理员或超级管理员需要浏览 App 时，仍然使用自己的普通 App 账号，并通过本接口给对应的 `user_info.id` 正常发放会员。后台管理员身份不提供免会员或自动会员能力。

### 12.4 订单查询

```http
GET /api/admin/membership-orders?keyword=MO202607260001&userId=9001&orderStatus=PAID&pageNum=1&pageSize=20
```

订单列表直接返回套餐快照、金额、支付状态和支付时间，不再提供独立详情接口。当前版本 `refundable` 固定返回 `false`，服务端不存在退款路由，也不会调用支付渠道退款能力。

### 12.5 套餐配置

创建：

```http
POST /api/admin/membership-plans
X-Admin-Reauth-Token: <token>

{
  "membershipType": "PREMIUM",
  "purchaseType": "FULL",
  "durationMonths": 3,
  "billingType": "QUARTERLY",
  "price": 199.00,
  "currency": "CNY",
  "enabled": false,
  "reason": "新增季度套餐"
}
```

更新：

```http
PUT /api/admin/membership-plans/10
X-Admin-Reauth-Token: <token>

{
  "price": 209.00,
  "enabled": true,
  "reason": "调整价格并上架",
  "version": 2
}
```

已产生订单的套餐只能修改价格和启停状态，不能修改会员等级、购买类型、时长和计费类型。

## 13. 管理员管理

只保留列表、邀请、权限配置和状态动作。

### 13.1 接口清单

| 方法 | 路径 | 权限 |
| --- | --- | --- |
| `GET` | `/admins` | `admin:manage` |
| `POST` | `/admin-invitations` | `admin:manage` + 二次认证 |
| `PUT` | `/admins/{adminId}/access` | `admin:manage` + 二次认证 |
| `POST` | `/admins/{adminId}/actions` | `admin:manage` + 二次认证 |

### 13.2 管理员列表

```http
GET /api/admin/admins?keyword=henry&status=ACTIVE&pageNum=1&pageSize=20
```

列表直接返回管理员基础信息、完整权限码、题库范围、已分配题库 ID、最近登录时间和版本，不再提供独立详情接口。

### 13.3 邀请管理员

```http
POST /api/admin/admin-invitations
X-Admin-Reauth-Token: <token>

{
  "email": "new-admin@example.com",
  "displayName": "New Admin",
  "permissions": ["bank:view", "question:view"],
  "bankDataScope": "ASSIGNED_BANKS",
  "assignedBankIds": [101, 102],
  "reason": "新增内容运营管理员"
}
```

只能邀请普通管理员，不能授予超级管理员专属权限。接口直接返回可复制的邀请链接，不发送邮件。相同邮箱存在未过期邀请时，该接口重新生成链接并刷新邀请有效期，不额外设计重发接口。

### 13.4 权限与状态

修改权限：

```http
PUT /api/admin/admins/2/access
X-Admin-Reauth-Token: <token>

{
  "permissions": ["bank:view", "question:view", "question:update"],
  "bankDataScope": "ASSIGNED_BANKS",
  "assignedBankIds": [101, 102, 103],
  "reason": "增加题目编辑职责",
  "version": 2
}
```

状态动作：

```http
POST /api/admin/admins/2/actions
X-Admin-Reauth-Token: <token>

{
  "action": "DISABLE",
  "reason": "管理员暂时离岗",
  "version": 3
}
```

支持 `DISABLE`、`ACTIVATE`、`ARCHIVE`。超级管理员不能成为目标。权限或状态变化后立即撤销目标管理员旧会话。

## 14. 操作日志

只保留一个分页查询接口。

| 方法 | 路径 | 权限 |
| --- | --- | --- |
| `GET` | `/audit-logs` | `audit:view` |

```http
GET /api/admin/audit-logs?operatorAdminId=2&module=QUESTION&action=UPDATE&targetId=10001&startTime=2026-07-01T00:00:00&endTime=2026-07-31T23:59:59&pageNum=1&pageSize=20
```

列表记录直接包含：

- 请求 ID
- 操作者
- 模块和动作
- 目标类型和 ID
- 操作原因
- 变更前后摘要
- 成功状态和失败原因
- IP、User-Agent 和操作时间

普通管理员只能查询自己的日志，超级管理员可以查询全部日志。V1 不提供日志导出。

## 15. 精简错误码

通用参数、系统和 Token 错误继续复用现有错误码；管理端新增：

| 代码 | 常量 | 消息 |
| --- | --- | --- |
| `1001` | `ADMIN_NOT_AUTHENTICATED` | 管理员未登录 |
| `1002` | `ADMIN_CREDENTIALS_INVALID` | 邮箱或密码错误 |
| `1003` | `ADMIN_ACCOUNT_UNAVAILABLE` | 管理员账号不可用 |
| `1004` | `ADMIN_PERMISSION_DENIED` | 无权执行该管理操作 |
| `1005` | `ADMIN_BANK_SCOPE_DENIED` | 无权访问该题库 |
| `1006` | `ADMIN_SESSION_REVOKED` | 管理员会话已失效 |
| `1007` | `ADMIN_INVITATION_INVALID` | 管理员邀请无效或已过期 |
| `1008` | `ADMIN_REAUTH_INVALID` | 二次认证无效或已过期 |
| `1101` | `ADMIN_ACCOUNT_NOT_FOUND` | 管理员不存在 |
| `1102` | `ADMIN_ACCOUNT_CONFLICT` | 管理员邮箱或状态冲突 |
| `1201` | `ADMIN_BANK_NOT_FOUND` | 题库不存在 |
| `1202` | `ADMIN_BANK_NAME_CONFLICT` | 题库名称已存在 |
| `1203` | `ADMIN_BANK_STATE_INVALID` | 题库状态不允许当前操作 |
| `1204` | `ADMIN_BANK_CATEGORY_INVALID` | 题库分类不合法 |
| `1205` | `ADMIN_RESOURCE_VERSION_CONFLICT` | 数据已变化，请刷新后重试 |
| `1206` | `ADMIN_BANK_NO_RELEASED_QUESTION` | 题库没有可发布题目 |
| `1301` | `ADMIN_QUESTION_NOT_FOUND` | 题目不存在 |
| `1302` | `ADMIN_QUESTION_TYPE_INVALID` | 题型与题库不匹配 |
| `1303` | `ADMIN_QUESTION_OPTION_INVALID` | 选项或正确答案不合法 |
| `1304` | `ADMIN_QUESTION_STATE_INVALID` | 题目状态不允许当前操作 |
| `1305` | `ADMIN_SHARED_QUESTION_FORBIDDEN` | 无权修改共享题目 |
| `1306` | `ADMIN_QUESTION_ORDER_INVALID` | 题目排序数据不合法 |
| `1310` | `ADMIN_IMPORT_FILE_INVALID` | 导入文件不合法 |
| `1311` | `ADMIN_IMPORT_ROW_INVALID` | 导入文件存在错误行 |
| `1312` | `ADMIN_IMPORT_TASK_INVALID` | 导入任务不存在、过期或状态错误 |
| `1401` | `ADMIN_USER_STATE_INVALID` | 用户不存在或状态不允许当前操作 |
| `1411` | `ADMIN_CONTENT_STATE_INVALID` | 社区内容不存在或状态不允许当前操作 |
| `1501` | `ADMIN_MEMBERSHIP_STATE_INVALID` | 会员状态不允许当前操作 |
| `1502` | `ADMIN_MEMBERSHIP_LEDGER_CONFLICT` | 会员台账已变化 |

## 16. 核心数据约束

### 16.1 一题多库

- `question_bank_question` 保持多对多关系。
- 建立 `(bank_id, question_id)` 唯一索引。
- 不建立 `question_id` 单列唯一索引。
- V1 新建题目只创建一条目标题库关系。
- V1 不提供关联已有题目的接口。
- 关系表 `sort_order` 是题库内顺序的唯一来源。
- 题目被多个题库引用时，编辑需要全部关联题库权限。
- 题目仍被其他题库引用时，不能删除题目主体。

### 16.2 必要数据能力

| 能力 | 用途 |
| --- | --- |
| 管理员账号、会话、邀请、权限和题库范围 | 独立后台认证 |
| 操作日志 | 所有写操作审计 |
| 题库业务状态和版本 | 发布、下架和并发控制 |
| 题库与题目的 `create_admin_id` | 后台创建人，不复用带 App 用户外键的 `create_user_id` |
| 面试题 `image_object_key` | 保存私有 COS 对象 Key，用于图文题目 |
| 关系表排序版本 | 原子拖拽排序 |
| 题目导入任务和错误明细 | 两阶段 Excel 导入 |
| Comment 业务状态 | 隐藏、删除和恢复 |
| 用户社区限制 | 控制发帖和评论 |
| 会员暂停和变更流水 | 基础会员管理 |
| 每日统计表 | 单接口看板 |

## 17. 验收重点

### 17.1 题库与题目

- 题库只能单条创建，名称全局唯一。
- 有题目的题库不能跨 `GroupType` 迁移。
- 发布题库前至少有一条已发布题目。
- 面试题和认证题不能交叉创建。
- 单选、多选的选项和正确答案校验完整。
- Excel 有错误行时不写入任何题目。
- 排序失败时不产生部分更新。
- 普通练习读取新题序，进行中的认证考试不受影响。
- 数据库不存在限制一道题只能关联一个题库的约束。

### 17.2 权限与审计

- App Token 不能访问管理接口。
- 普通管理员不能构造请求执行超级管理员动作。
- `ASSIGNED_BANKS` 管理员不能读取或修改未分配题库。
- 共享题目不会泄露无权限题库信息。
- 每个写操作都记录请求 ID、操作者、目标、原因和结果。

### 17.3 低频模块

- 用户状态和社区限制使用合并接口即可完成基础治理。
- Post 和 Comment 无需详情接口即可完成查询和治理。
- 会员详情返回最近变更，不依赖独立流水页。
- 订单列表只承担基础支付记录查询，不开放退款操作。
- 管理员列表包含权限和范围，不依赖独立详情页。
