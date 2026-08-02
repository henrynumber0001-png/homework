# HomeWork 主要 API 接口文档

> 本文是当前接口的简明总览，重点说明主业务接口。详细字段、错误码和边界规则以
> Controller、DTO/VO 以及文末专题文档为准。

## 1. 基础约定

### 1.1 服务与前缀

| 服务 | 默认地址 | API 前缀 |
| --- | --- | --- |
| 用户端 API | `http://127.0.0.1:8080` | `/api/app` |
| 管理端 API | `http://localhost:8081` | `/api/admin` |
| 微信支付回调 | 用户端 API 所在服务 | `/api/payment` |

下面用户端表格中的路径默认省略 `/api/app`，管理端表格默认省略 `/api/admin`。

### 1.2 鉴权

除用户注册/登录、管理员登录/邀请和微信回调外，接口需要对应 Token：

```http
Authorization: Bearer <access_token>
```

用户 Token 和 Admin Token 不可互换。管理端高风险操作还可能需要：

```http
X-Admin-Reauth-Token: <one_time_token>
```

### 1.3 统一响应

普通接口使用：

```json
{
  "code": 200,
  "message": "success",
  "data": {}
}
```

分页数据通常位于 `data` 中：

```json
{
  "records": [],
  "total": 0,
  "pageNum": 1,
  "pageSize": 20
}
```

分页参数统一使用 `pageNum` 和 `pageSize`。文件下载和微信回调不使用统一响应包装。

### 1.4 常用枚举

| 类型 | 常用值 |
| --- | --- |
| 用户端 `GroupType` | `1=INTERVIEW`、`2=CERTIFICATION` |
| 用户端题型 | `1=SINGLE_CHOICE`、`2=MULTIPLE`、`3=ESSAY` |
| `ActionStatus` | `ACTIVATE`、`DEACTIVATE` |
| 用户端会员等级 | `0=FREE`、`1=PREMIUM`、`2=PREMIUM_PLUS` |
| 管理端题库状态 | `DRAFT`、`PUBLISHED`、`OFFLINE` |
| 管理端题型 | `ESSAY`、`SINGLE_CHOICE`、`MULTIPLE` |

## 2. 用户端 API

### 2.1 认证、首页与当前用户

| 方法 | 路径 | 主要参数 | 用途 |
| --- | --- | --- | --- |
| `POST` | `/auth/register/email` | `email`、`password`、`passwordConfirm`、`displayName`、`turnstileToken` | 邮箱注册并返回 JWT |
| `POST` | `/auth/login/email` | `email`、`password`、`turnstileToken` | 邮箱登录并返回 JWT |
| `POST` | `/auth/register/oauth` | OAuth 注册信息 | 第三方账号注册 |
| `POST` | `/auth/login/oauth` | OAuth 登录信息 | 第三方账号登录 |
| `GET` | `/user/info` | 无 | 查询当前登录用户 |
| `GET` | `/home-page` | 无 | 查询首页题库推荐和最新 Hit |

OAuth 能力由服务端配置控制，未启用的平台不会在前端显示。

### 2.2 题库目录

| 方法 | 路径 | 主要参数 | 用途 |
| --- | --- | --- | --- |
| `GET` | `/question-banks/group-page` | `groupId` | 获取面试或认证题库首页 |
| `GET` | `/question-banks/group-page/module-page` | `currentGroupId`、`moduleId`、`currentModuleId` | 切换题库模块 |
| `GET` | `/question-banks/group-page/module-page/sub-module-page` | 当前分类和子模块 ID | 切换子模块并查询题库 |
| `GET` | `/question-banks/group-page/sort-type` | `sortType`、`currentSubModuleId` | 按指定规则排序题库 |

### 2.3 练习、考试与复习

| 方法 | 路径 | 主要参数 | 用途 |
| --- | --- | --- | --- |
| `GET` | `/bank/questions/interview/question` | `bankId` | 获取面试题 |
| `POST` | `/bank/questions/interview/answer` | `bankId`、`questionId`、`content` | 提交面试答案并获取反馈 |
| `GET` | `/bank/questions/certificate/question` | `bankId` | 获取认证练习题 |
| `POST` | `/bank/questions/certificate/practice/answer` | 题库、题目、题型、所选选项 | 提交认证练习答案 |
| `POST` | `/bank/certificate/exams/start` | `bankId` | 创建或恢复考试场次 |
| `GET` | `/bank/certificate/exams/{sessionId}` | 考试场次 ID | 恢复考试 |
| `POST` | `/bank/certificate/exams/answer` | `sessionId`、`questionId`、`chosenOptions` | 保存考试临时答案 |
| `POST` | `/bank/certificate/exams/{sessionId}/submit` | 考试场次 ID | 交卷并返回结果 |
| `POST` | `/bank/questions/finish` | `bankId`、`groupType` | 完成一次题库练习 |
| `GET` | `/bank/questions/interview/review` | `bankId` | 面试题结果回顾 |
| `GET` | `/bank/questions/certificate/review` | `bankId` | 认证题结果回顾 |

答题记录查询和清理属于辅助接口：

- `GET /bank/questions/interview/record`
- `GET /bank/questions/certificate/record`
- `DELETE /bank/questions/clear/record`

### 2.4 笔记、收藏与 AI 追问

| 方法 | 路径 | 用途 |
| --- | --- | --- |
| `POST` | `/bank/questions/answer/note` | 保存或更新题目笔记 |
| `POST` | `/bank/questions/collect` | 收藏或取消收藏题目 |
| `GET` | `/bank/questions/ai/chat` | 获取题库已有 AI 会话 |
| `POST` | `/bank/questions/ai/chat` | 提交追问并返回完整会话 |
| `POST` | `/bank/questions/ai/chat/close` | 主动关闭当前 AI 会话 |

AI 追问 Body 的主要字段是 `bankId`、`questionId`、`groupType` 和 `message`。

### 2.5 个人中心

| 方法 | 路径 | 用途 |
| --- | --- | --- |
| `GET` | `/user-center` | 查询个人中心汇总 |
| `GET` | `/user-center/wrong-question-banks` | 分页查询存在错题的题库 |
| `GET` | `/user-center/wrong-question-list` | 查询指定题库的错题 |
| `GET` | `/user-center/favorite-question-banks` | 分页查询收藏题目所在题库 |
| `GET` | `/user-center/favorite-question-list` | 查询指定题库的收藏题 |
| `GET` | `/user-center/note-banks` | 分页查询存在笔记的题库 |
| `GET` | `/user-center/note-list` | 查询指定题库的笔记题目 |

单题详情分别使用 `/wrong-question`、`/favorite-question` 和 `/note-question`；
`GET /user-center/membership-info` 返回个人中心所需的简要会员信息。

### 2.6 Hit 社区

| 方法 | 路径 | 用途 |
| --- | --- | --- |
| `GET` | `/hits` | 分页获取 Hit 时间线 |
| `POST` | `/hits` | 发布 Hit，可带标签和 @ 用户 |
| `GET` | `/hits/{postId}/comments` | 查询评论 |
| `POST` | `/hits/{postId}/comments` | 评论或回复 |
| `POST` | `/hits/{postId}/actions` | 点赞、收藏、转发或取消 |
| `PUT` | `/hits/{postId}/comments/{commentId}/like` | 点赞或取消点赞评论 |

发布内容主要包含 `content`、`tags` 和 `mentionedUserIds`。Post 互动的
`actionType` 为 `1=点赞、2=收藏、3=转发`。

### 2.7 消息、主页与关注

| 方法 | 路径 | 用途 |
| --- | --- | --- |
| `GET` | `/messages/unread-summary` | 获取各消息模块未读数 |
| `PUT` | `/messages/notifications/open-tab` | 打开通知页签、标记已读并返回最近通知 |
| `GET` | `/messages/notifications/history` | 查询历史通知 |
| `GET` | `/messages/chatboxes` | 查询私信会话 |
| `GET` | `/messages/chatboxes/{id}/messages` | 查询聊天记录或轮询新消息 |
| `POST` | `/messages/private` | 发送私信 |
| `PUT` | `/messages/private/{messageId}/read` | 标记一条私信已读 |
| `GET` | `/users/search` | 搜索可 @ 的用户 |
| `GET` | `/users/{userId}/profile` | 查询公开主页 |
| `GET` | `/users/{userId}/profile/activities` | 查询用户社区活动 |
| `PUT` | `/users/{targetUserId}/follow` | 关注或取消关注 |

`GET /messages/chatboxes/with/{userId}` 用于查询与指定用户已有的私信会话。

### 2.8 会员与支付

| 方法 | 路径 | 主要参数 | 用途 |
| --- | --- | --- | --- |
| `GET` | `/membership/center` | 无 | 查询会员中心宣传和当前状态 |
| `GET` | `/membership` | 无 | 查询可购买套餐和补差资格 |
| `POST` | `/membership/orders` | Header `Idempotency-Key`；Body `planId` | 创建待支付订单 |
| `GET` | `/membership/orders/{orderNo}` | 订单号 | 轮询订单状态 |
| `GET` | `/membership/orders` | 无 | 查询会员订单历史 |

微信支付平台回调：

```http
POST /api/payment/wechat/native/notify
```

该接口由微信调用，不接收用户 JWT。只有验签、解密和订单校验全部成功后才发放权益。

### 2.9 学习活动

| 方法 | 路径 | 用途 |
| --- | --- | --- |
| `POST` | `/learning-activity/heartbeat` | 记录有效学习活动 |
| `GET` | `/learning-activity/calendar` | 查询指定年份的学习日历 |

## 3. 管理端 API

### 3.1 管理员认证

| 方法 | 路径 | 用途 |
| --- | --- | --- |
| `GET` | `/auth/invitations/{token}` | 预览管理员邀请 |
| `POST` | `/auth/invitations/{token}/accept` | 接受邀请并设置账号 |
| `POST` | `/auth/login` | 管理员登录 |
| `POST` | `/auth/logout` | 注销当前会话 |
| `GET` | `/auth/me` | 查询当前管理员、权限和数据范围 |
| `PUT` | `/auth/password` | 修改管理员密码 |
| `POST` | `/auth/reauth` | 获取一次性二次认证 Token |

### 3.2 数据概览与分类

| 方法 | 路径 | 用途 |
| --- | --- | --- |
| `GET` | `/dashboard` | 查询运营数据概览 |
| `GET` | `/categories/tree` | 查询题库分类树 |

### 3.3 题库管理

| 方法 | 路径 | 用途 |
| --- | --- | --- |
| `GET` | `/question-banks` | 筛选并分页查询题库 |
| `GET` | `/question-banks/{bankId}` | 查询题库详情 |
| `POST` | `/question-banks` | 创建题库 |
| `PUT` | `/question-banks/{bankId}` | 编辑题库 |
| `POST` | `/question-banks/{bankId}/actions` | 发布、下架或删除题库 |

创建题库的主要字段为 `subModuleId`、`bankName`、`tags` 和 `sortOrder`。其中
`sortOrder` 是题库的人工曝光权重，默认 10，数值越大越优先。编辑与状态动作
需要提交当前 `version`，状态动作同时提交 `action` 和 `reason`。同一 SubModule
中的未删除题库不能重名。

### 3.4 题目管理

| 方法 | 路径 | 用途 |
| --- | --- | --- |
| `GET` | `/question-banks/{bankId}/questions` | 筛选并分页查询题目 |
| `GET` | `/question-banks/{bankId}/questions/{questionId}` | 查询题目详情 |
| `POST` | `/question-banks/{bankId}/questions` | 创建题目 |
| `PUT` | `/question-banks/{bankId}/questions/{questionId}` | 编辑题目 |
| `POST` | `/question-banks/{bankId}/questions/{questionId}/actions` | 发布、下架或删除题目 |
| `PUT` | `/question-banks/{bankId}/questions/{questionId}/question-no` | 修改题目序号，中间题目自动顺移 |
| `POST` | `/uploads/question-images` | 上传题目图片 |

题目主要包含 `questionType`、`title`、`analysis`、`options`、`correctAnswers` 和可选的
`imageObjectKey`。同一题库的未删除题目不能使用相同 `title`；正确答案由后端校验，
用户端接口不会提前返回考试正确答案。

### 3.5 Excel 导入

| 方法 | 路径 | 用途 |
| --- | --- | --- |
| `GET` | `/question-banks/{bankId}/question-import-template` | 下载 Excel 模板 |
| `POST` | `/question-imports` | 上传文件并预检 |
| `GET` | `/question-imports/{taskId}` | 查询预检任务 |
| `GET` | `/question-imports/{taskId}/errors` | 分页查询错误行 |
| `POST` | `/question-imports/{taskId}/commit` | 确认导入通过预检的数据 |

上传和预检不会立即写入正式题目；只有确认导入接口会提交数据。

### 3.6 辅助运营接口

| 模块 | 接口 | 用途 |
| --- | --- | --- |
| 用户 | `GET /users`、`GET /users/{userId}` | 用户列表与详情 |
| 用户动作 | `POST /users/{userId}/actions` | 禁用、恢复或封禁用户 |
| 社区权限 | `PUT /users/{userId}/community-access` | 限制发布或评论能力 |
| 社区治理 | `GET /community/posts`、`GET /community/comments` | 查询 Hit 和评论 |
| 社区动作 | `POST /community/posts/{postId}/actions`、`POST /community/comments/{commentId}/actions` | 下架或恢复内容 |
| 会员 | `GET /memberships`、`GET /memberships/users/{userId}` | 查询会员列表与详情 |
| 会员动作 | `POST /memberships/users/{userId}/actions` | 暂停、恢复或调整会员 |
| 订单 | `GET /membership-orders` | 查询会员订单 |
| 套餐 | `GET/POST /membership-plans`、`PUT /membership-plans/{planId}` | 查询和维护套餐 |
| 管理员 | `GET /admins`、`POST /admin-invitations` | 管理员列表和邀请 |
| 权限 | `PUT /admins/{adminId}/access`、`POST /admins/{adminId}/actions` | 修改权限或账号状态 |
| 审计 | `GET /audit-logs` | 查询后台操作日志 |

永久封禁、会员永久回收、套餐配置和管理员权限变更等高风险操作需要二次认证。

## 4. 典型请求

### 4.1 提交面试答案

```http
POST /api/app/bank/questions/interview/answer
Authorization: Bearer <user_token>
Content-Type: application/json

{
  "bankId": 10,
  "questionId": 101,
  "content": "我的回答内容"
}
```

### 4.2 创建会员订单

```http
POST /api/app/membership/orders
Authorization: Bearer <user_token>
Idempotency-Key: 6e8e7c22-9f0f-4de0-9fe2-0f0d7a9cbf2f
Content-Type: application/json

{
  "planId": 1
}
```

### 4.3 发布题库

```http
POST /api/admin/question-banks/10/actions
Authorization: Bearer <admin_token>
Content-Type: application/json

{
  "action": "PUBLISH",
  "reason": "题目已完成审核",
  "version": 3
}
```

## 5. 错误处理

HTTP 请求成功不代表业务一定成功，前端还需要检查统一响应的 `code`：

| 范围 | 含义 |
| --- | --- |
| `200` | 成功 |
| `201-206` | 通用参数、数据或服务错误 |
| `501-602` | 用户认证、账号和 Token 错误 |
| `701-711` | Hit、评论和社区权限错误 |
| `801-812` | 会员、订单和支付错误 |
| `1001-1502` | 管理员认证、权限、题库、题目、用户和会员运营错误 |

管理端资源版本冲突使用业务码 `1205`，客户端应刷新数据后重新提交。

## 6. 详细专题文档

- [后台管理 API 详细文档](web-admin-api.md)
- [会员接口与业务规则](membership-api.md)
- [Hit、消息、公开主页和关注](hit-api.md)
- [微信 Native 支付接入](wechat-native-membership-payment.md)
- [私有 COS 图片访问](private-cos-url-auth.md)
