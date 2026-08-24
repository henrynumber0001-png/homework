# HomeWork 文档中心

本目录按“业务功能域”组织。阅读功能时从业务域进入，查询接口时进入 `reference`，了解代码和部署时进入 `engineering`。不要按 Controller、页面或前后端目录重复编写同一份业务规则。

## 快速入口

- [产品概览](overview/product-overview.md)
- [功能地图](overview/feature-map.md)
- [系统架构](overview/system-architecture.md)
- [API 总览](reference/api-overview.md)
- [管理端 API](reference/admin-api.md)

## 业务功能域

| 功能域 | 负责回答的问题 | 文档入口 |
| --- | --- | --- |
| 用户与账号 | 用户如何注册、登录、维护资料和访问公开主页 | [account](domains/account/overview.md) |
| 题库内容 | 系统有什么题，题目如何分类、归属和发布 | [question-content](domains/question-content/overview.md) |
| 学习与做题 | 用户如何练习、考试、复习并沉淀学习资产 | [learning](domains/learning/overview.md) |
| 社区与社交 | 用户如何发布 Hit、互动、关注、私信和接收通知 | [social](domains/social/overview.md) |
| 会员与交易 | 会员权益、套餐、订单和微信支付如何工作 | [membership](domains/membership/overview.md) |
| 后台运营 | 管理员如何维护内容、治理用户和审计操作 | [admin](domains/admin/overview.md) |

## 参考手册

- [用户端与全局 API 总览](reference/api-overview.md)
- [社交域 API](reference/social-api.md)
- [管理端 API](reference/admin-api.md)
- [核心数据模型](reference/data-model.md)
- [枚举与错误处理](reference/enums-and-errors.md)

## 工程与运维

- [用户端前端工程设计](engineering/web-app-frontend.md)
- [管理端前端工程设计](engineering/web-admin-frontend.md)
- [私有 COS 访问与部署](engineering/storage/private-cos.md)
- [项目部署手册](../deploy/README.md)

## 变更记录

- `changes/decisions/`：仍然影响当前设计的架构或产品决策。
- `changes/archive/`：已完成的实施计划和历史改造说明，仅用于追溯，不作为当前规则来源。

## 文档维护约定

1. 一个业务规则只在一个“主文档”中维护，其他文档使用链接引用。
2. 功能文档描述目标、范围、场景、规则和验收；接口字段放在 `reference`。
3. 工程文档描述技术栈、代码结构、部署、安全和存储，不承担产品规则说明。
4. 历史实施计划完成后移入 `changes/archive/`，避免与现状文档混淆。
5. 新增功能时，先更新[功能地图](overview/feature-map.md)，再创建或修改对应业务域文档。

## 功能文档模板

新增功能文档时按需使用以下章节：

```markdown
# 功能名称

## 1. 功能目标
## 2. 范围与非目标
## 3. 用户场景
## 4. 核心规则
## 5. 状态与流程
## 6. 数据模型
## 7. 接口与实现位置
## 8. 权限与安全
## 9. 测试与验收
## 10. 相关文档
```
