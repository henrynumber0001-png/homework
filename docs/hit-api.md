# Hit、我的消息与公开主页接口

所有接口都在 `/api/app` 下，并需要 `Authorization: Bearer <JWT>`。发布者、评论者、互动者、关注者和私信发送者均从 JWT 获取。

## Hit

| 方法 | 路径 | 用途 |
| --- | --- | --- |
| `GET` | `/hits?pageNum=1&pageSize=20` | 公共时间线 |
| `POST` | `/hits` | 发布最多 140 字的 Post，可同时 @ 用户 |
| `GET` | `/hits/{postId}/comments` | 分页读取 Comment |
| `POST` | `/hits/{postId}/comments` | 评论、回复或 @ 用户 |
| `POST` | `/hits/{postId}/actions` | Post 点赞、收藏、转发或取消 |
| `PUT` | `/hits/{postId}/comments/{commentId}/like` | Comment 点赞或取消点赞 |

Post 和 Comment 中的 `mentionedUserIds` 必须来自用户搜索接口；Comment 只能点赞，不能收藏或转发。

```json
{
  "content": "今天刷了 30 道 React Hooks 题。",
  "mentionedUserIds": [42]
}
```

```json
{
  "parentCommentId": 88,
  "comment": "useMemo 那道题我也踩坑了。",
  "mentionedUserIds": [42]
}
```

Post 互动的 `actionType` 为 `1=点赞、2=收藏、3=转发`：

```json
{
  "actionType": 1,
  "actionStatus": "ACTIVATE"
}
```

Comment 点赞只提交最终目标状态：

```json
{
  "actionStatus": "DEACTIVATE"
}
```

## 我的消息

页面默认打开私信。前三个通知页签只返回当前登录用户收到的通知：

| 方法 | 路径 | 用途 |
| --- | --- | --- |
| `GET` | `/messages/unread-summary` | 四模块未读数及总数 |
| `PUT` | `/messages/notifications/open-tab?tab=comments` | 评论和@全部已读并返回最近批次 |
| `PUT` | `/messages/notifications/open-tab?tab=interactions` | 点赞、收藏、转发全部已读并返回最近批次 |
| `PUT` | `/messages/notifications/open-tab?tab=system` | 系统消息全部已读并返回最近批次 |
| `GET` | `/messages/notifications/history?tab=comments` | 查询最近批次之前的历史通知 |

通知目标包含稳定的 `postId`。Comment 或 parentComment 删除后，`content` 返回“原评论已删除”，但仍可通过 `postId` 打开所属 Post；若 Post 不可访问，`postAvailable=false`。

取消点赞、收藏或转发会撤销原通知，不创建“取消操作”通知。互动通知中的 `actionUserId` 用于打开动作发出者的公开主页。

## 私信

| 方法 | 路径 | 用途 |
| --- | --- | --- |
| `GET` | `/messages/chatboxes` | Chatbox 列表 |
| `GET` | `/messages/chatboxes/with/{userId}` | 查询双方已有 Chatbox，可为空 |
| `GET` | `/messages/chatboxes/{id}/messages?beforeId=&limit=50` | 向前读取聊天历史 |
| `GET` | `/messages/chatboxes/{id}/messages?afterId=&limit=50` | 短轮询读取新消息 |
| `POST` | `/messages/private` | 发送纯文本私信并按需创建 Chatbox |
| `PUT` | `/messages/private/{messageId}/read` | 将当前用户收到的一条私信设为已读 |

`beforeId` 与 `afterId` 不能同时提交。私信角标统计未读消息条数。

```json
{
  "receiverUserId": 42,
  "content": "你好，可以交流一下备考经验吗？"
}
```

陌生人第一条消息后，Chatbox 为 `PENDING_REPLY`，发起者不能继续发送；对方回复后变为永久开放的 `OPEN`。双方已互相关注时首次发送直接为 `OPEN`。

## 用户搜索、公开主页与关注

| 方法 | 路径 | 用途 |
| --- | --- | --- |
| `GET` | `/users/search?keyword=henry&limit=10` | Post/Comment 的 @ 用户选择器 |
| `GET` | `/users/{userId}/profile` | PublicUserProfile 上方个人信息卡 |
| `GET` | `/users/{userId}/profile/activities?tab=posts` | 下方活动列表 |
| `PUT` | `/users/{userId}/follow` | 显式关注或取消关注 |

活动页签为 `posts、commented、liked、favorite`，默认使用 `posts`，列表不查询总数。`postCount` 等于原创 Post 数与有效转发数之和。

查看自己的公开主页时：

- `self=true`
- `followedByCurrentUser=null`
- `canFollow=false`
- `canSendPrivateMessage=false`
- `chatboxId=null`

关注请求：

```json
{
  "active": true
}
```

## 数据库脚本

- 新数据库首次部署执行 [`sql/hit_feature.sql`](../sql/hit_feature.sql)。
- 已有旧版 Hit/私信表时执行 [`sql/my_messages_v1.sql`](../sql/my_messages_v1.sql)；脚本会回填 Chatbox 和 `chatbox_id` 后移除旧 `allow_reason` 设计。
