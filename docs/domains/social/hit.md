# Hit 内容

## 1. 功能目标

Hit 是轻量学习动态，用于分享刷题进度、经验和讨论。它只负责公开内容的创建与阅读，互动、用户关系和私信分别由其他社交模块负责。

## 2. 功能范围

- 浏览公共时间线。
- 发布最多 140 字的 Hit，可携带标签并 @ 用户。
- 分页读取评论。
- 评论、回复评论并 @ 用户。

## 3. 核心规则

- 发布者和评论者从 JWT 获取，客户端不能指定。
- `mentionedUserIds` 必须使用用户搜索得到的稳定用户 ID。
- 回复通过 `parentCommentId` 表达，服务端校验父评论属于当前 Post。
- 自己对自己的内容进行 @、评论或互动时，不产生给自己的通知。
- 不可见或已删除内容不能继续被普通用户访问或互动。

## 4. 数据与实现

- 数据实体：`HitPost`、`HitComment`
- 前端：`frontend/web-app/src/features/hit`
- 后端：`HitController`、`HitService`
- API：`GET/POST /api/app/hits`、`GET/POST /api/app/hits/{postId}/comments`

互动规则参见[内容互动](interactions.md)，接口示例参见[社交域 API](../../reference/social-api.md)。
