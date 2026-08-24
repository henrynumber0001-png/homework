# 用户关系与公开主页

## 1. 功能范围

- 搜索可被 @ 的用户。
- 查看其他用户的公开主页和公开 Hit。
- 关注、取消关注。
- 查看自己的关注列表和粉丝列表。
- 拉黑、取消拉黑。
- 从公开主页发起私信。

## 2. 关注规则

- 关注关系由 `follower_user_id` 和 `followee_user_id` 表达。
- 关注接口提交显式目标状态，重复请求保持幂等。
- 用户不能关注自己。
- 新增关注产生关注通知；取消关注不产生取消通知。
- 双方互相关注会影响陌生人首次私信的访问状态。

## 3. 拉黑规则

- 用户不能拉黑自己。
- 拉黑状态会限制双方社交访问，服务端是最终判定者。
- 解除拉黑不应自动恢复此前取消或失效的关系状态。

## 4. 公开与私有边界

公开主页可以展示基本资料、公开统计和已发布 Hit。错题、收藏题目、笔记等个人学习资产不得通过公开主页泄露。

当前实现提供：

- `GET /api/app/users/{userId}/profile`
- `GET /api/app/users/{userId}/profile/posts`
- `PUT /api/app/users/{targetUserId}/follow?status=...`
- `PUT /api/app/users/{userId}/block`
- `GET /api/app/users/search`

私信访问规则参见[私信](messaging.md)。
