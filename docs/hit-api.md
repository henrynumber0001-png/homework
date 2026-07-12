# Hit 学习打卡接口

所有接口都在 `/api/app` 下，并需要 `Authorization: Bearer <JWT>`。发布者、评论者、互动者和私信发送者均从 JWT 获取，前端不要提交用户 ID。

## Hit

| 方法 | 路径 | 用途 |
| --- | --- | --- |
| `GET` | `/hits?pageNum=1&pageSize=20` | 公共时间线，严格 newest → oldest |
| `POST` | `/hits` | 发布最多 140 字的 Hit |
| `GET` | `/hits/{postId}/comments` | 分页读取评论 |
| `POST` | `/hits/{postId}/comments` | 评论或回复评论 |
| `POST` | `/hits/{postId}/actions` | 点赞、收藏、转发或取消 |

发布示例：

```json
{
  "content": "今天刷了 30 道 React Hooks 题。",
  "tags": ["React", "Hooks"]
}
```

评论示例；顶级评论不传 `parentId`：

```json
{
  "parentId": 88,
  "content": "useMemo 那道题我也踩坑了。"
}
```

互动示例。`actionType`：1 点赞、2 收藏、3 转发；`active` 不传时切换当前状态，显式传值时接口幂等：

```json
{
  "actionType": 1,
  "active": true
}
```

## 我的消息

| 方法 | 路径 | 用途 |
| --- | --- | --- |
| `GET` | `/messages/notifications?tab=replies` | 回复我的 |
| `GET` | `/messages/notifications?tab=likes` | 收到的赞（含收藏、转发） |
| `GET` | `/messages/notifications?tab=system` | 系统消息（含新增关注） |
| `GET` | `/messages/private` | 私信 |
| `GET` | `/messages/unread-summary` | 四模块未读数及总数 |
| `PUT` | `/messages/notifications/{id}/read` | 单条通知已读 |
| `PUT` | `/messages/notifications/read-all?tab=likes` | 某模块全部已读 |
| `POST` | `/messages/private` | 发送纯文本私信 |
| `PUT` | `/messages/private/{id}/read` | 私信已读 |
| `POST` | `/users/{targetUserId}/follow` | 关注/取消关注，并产生新增关注系统通知 |

发送私信只提交接收者和纯文本。非互相关注时，同一发送者对同一接收者只允许第一条：

```json
{
  "receiverUserId": 42,
  "content": "你好，可以交流一下备考经验吗？"
}
```

首次部署请执行 [`sql/hit_feature.sql`](../sql/hit_feature.sql)。表结构通过唯一键保证互动幂等，并在数据库层并发保护“非互关仅第一条私信”。
