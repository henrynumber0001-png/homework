# 枚举与错误处理索引

## 1. 枚举来源

后端业务枚举统一位于：

```text
backend/model/src/main/java/com/homework/model/enums
```

常用分类包括：

- 内容：`GroupType`、`QuestionBankStatus`、`QuestionInfoStatus`、`QuestionInfoQuestionType`。
- 学习与考试：`ExamSessionStatus`、`AiChatSessionStatus`、`AiChatMessageSenderType`。
- 社交：`HitActionType`、`ActionStatus`、`UserNotificationType`、`PrivateChatAccess`、`BlockStatus`。
- 会员：`MembershipType`、`MembershipStatus`、`MembershipPurchaseType`、`MembershipOrderStatus`。
- 后台：`AdminRole`、`AdminStatus`、`AdminSortMode`、`BankDataScope`。

前端不要根据显示文案推断枚举值。接口新增枚举时，应同步更新对应前端的领域常量和类型。

## 2. 统一响应

用户端和管理端使用统一结果包装：

```json
{
  "code": 200,
  "message": "success",
  "data": {}
}
```

具体结构以 `backend/common/.../result/Result.java` 和 `PageResult.java` 为准。

## 3. 错误处理原则

- Controller 负责参数接收，业务校验和状态冲突由 Service 抛出明确异常。
- 全局异常处理转换为稳定的业务错误码和消息。
- 前端根据 HTTP/业务状态区分未登录、无权限、资源不存在、校验失败和并发冲突。
- 高风险动作失败不能通过客户端重试绕过二次认证、权限或幂等校验。

详细接口约定参见[API 总览](api-overview.md)和[管理端 API](admin-api.md)。
