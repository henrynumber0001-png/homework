# 核心数据模型索引

本文只提供跨功能域的数据导航。字段定义以 `backend/model` 实体和数据库迁移为准，业务含义以对应功能域文档为准。

| 功能域 | 核心实体 | 说明 |
| --- | --- | --- |
| 用户与账号 | `UserInfo`、`UserAuthIdentity`、`OtpCode` | 用户资料、认证身份和验证码 |
| 题库内容 | `CategoryGroup`、`CategoryModule`、`CategorySubModule`、`QuestionBank` | 分类层级和题库 |
| 题库内容 | `InterviewQuestionInfo`、`CertificateQuestionInfo`、`QuestionOption` | 面试题、认证题和选项 |
| 学习与做题 | `UserQuestionAnswer`、`QuestionAiEvaluation`、`AiChatSession`、`AiChatMessage` | 答题、AI 评价和追问 |
| 认证考试 | `CertificateExamSession`、`CertificateExamAnswer`、`CertificateExamLock` | 考试场次、答案和并发保护 |
| 学习资产 | `UserFavoriteQuestion`、`UserQuestionNote`、`UserLearningStatDaily` | 收藏题目、笔记和学习统计 |
| Hit | `HitPost`、`HitComment` | 社区动态和评论 |
| 社交互动 | `HitAction`、`HitCommentLike` | Hit 互动和评论点赞 |
| 用户关系 | `UserFollow`、`UserBlock` | 关注和拉黑 |
| 通知与私信 | `UserNotification`、`PrivateChatbox`、`PrivateMessage` | 通知、会话和消息 |
| 会员与交易 | `MembershipPlan`、`MembershipOrder`、`MembershipChangeRecord` | 套餐、订单和权益变更 |
| 后台运营 | `AdminAccount`、`AdminInvitation`、`AdminOperationLog` | 管理员、邀请和审计 |

## 数据边界

- 公开社区数据、私信数据和私有学习资产分开存储和授权。
- 私信未读状态保存在 `PrivateMessage`，不复制到 `UserNotification`。
- 历史订单保存成交快照，不随套餐后续改价变化。
- 数据库只保存 COS object key，不保存短时签名 URL。

数据库基线和迁移位于 `backend/common/src/main/resources/db/migration`；`sql/` 中还保留专题初始化和历史迁移脚本。
