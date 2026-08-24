# HomeWork 功能地图

> 本文是产品功能清单和文档导航，不记录详细接口字段。

## 1. 用户端功能

| 一级功能域 | 二级模块 | 主要能力 | 主文档 |
| --- | --- | --- | --- |
| 用户与账号 | 认证 | 邮箱注册、登录、验证码、OAuth、用户会话 | [用户与账号](../domains/account/overview.md) |
| 用户与账号 | 个人资料 | 当前用户信息、资料编辑、头像、Banner | [个人图片](../domains/account/profile-images.md) |
| 用户与账号 | 公开身份 | 用户搜索、公开主页、拉黑 | [用户关系](../domains/social/relationships.md) |
| 首页与发现 | 首页 | 推荐题库、学习摘要、最新内容入口 | [用户与账号](../domains/account/overview.md) |
| 题库内容 | 分类与题库 | 分类组、模块、子模块、面试题库、认证题库 | [题库内容](../domains/question-content/overview.md) |
| 题库内容 | 题目 | 面试题、认证题、选项、图片、顺序和状态 | [内容发布](../domains/question-content/publishing.md) |
| 学习与做题 | 面试练习 | 作答、AI 评价、参考答案、AI 追问 | [做题模式](../domains/learning/assessment-modes.md) |
| 学习与做题 | 认证练习 | 逐题练习、即时判题、解析 | [做题模式](../domains/learning/assessment-modes.md) |
| 学习与做题 | 认证考试 | 考试场次、临时答案、交卷、结果 | [做题模式](../domains/learning/assessment-modes.md) |
| 学习与做题 | 复习 | 完成题库、答题记录、回顾、清除记录 | [学习域概览](../domains/learning/overview.md) |
| 学习资产 | 个人题库 | 错题、收藏题目、题目笔记 | [学习资产](../domains/learning/learning-assets.md) |
| 学习资产 | 学习统计 | 学习心跳、学习日历、完成状态、正确率 | [学习资产](../domains/learning/learning-assets.md) |
| 社区与社交 | Hit | 发布动态、时间线、评论、回复、@ | [Hit](../domains/social/hit.md) |
| 社区与社交 | 内容互动 | 点赞、收藏 Hit、转发、评论点赞 | [内容互动](../domains/social/interactions.md) |
| 社区与社交 | 用户关系 | 关注、粉丝、公开主页、拉黑 | [用户关系](../domains/social/relationships.md) |
| 社区与社交 | 私信 | 会话、纯文本消息、陌生人限制、已读 | [私信](../domains/social/messaging.md) |
| 社区与社交 | 通知 | 评论、@、互动、关注、系统通知、未读数 | [通知](../domains/social/notifications.md) |
| 会员与交易 | 会员 | Premium、Premium Plus、冻结、补差升级 | [会员概览](../domains/membership/overview.md) |
| 会员与交易 | 订单与支付 | 套餐、订单、微信二维码、回调、历史订单 | [微信支付](../domains/membership/wechat-payment.md) |

## 2. 管理端功能

| 二级模块 | 主要能力 | 主文档 |
| --- | --- | --- |
| 数据概览 | 运营统计和待处理事项 | [后台运营](../domains/admin/overview.md) |
| 内容运营 | 分类、题库、题目、排序、图片、发布和下架 | [内容运营](../domains/admin/content-operations.md) |
| 批量导入 | Excel 模板、预检、错误报告和确认导入 | [内容运营](../domains/admin/content-operations.md) |
| 用户治理 | 用户状态、封禁和社区权限 | [运营治理](../domains/admin/governance.md) |
| 社区治理 | Hit 内容查询、隐藏、恢复和删除 | [运营治理](../domains/admin/governance.md) |
| 商业运营 | 会员、订单和套餐配置 | [运营治理](../domains/admin/governance.md) |
| 平台管理 | 管理员邀请、权限、数据范围、二次认证和审计 | [后台运营](../domains/admin/overview.md) |

## 3. 分类边界

- “题库内容”回答系统维护了什么内容；“学习与做题”回答用户如何消费这些内容。
- “Hit”只表示社区内容；关注属于用户关系，私信属于通信，通知属于事件提醒。
- “错题、收藏题目、笔记”是学习资产；“收藏 Hit”是社交互动，两者不可混用。
- 后台管理是运营能力，不与用户端题库浏览、做题流程合并编写。
