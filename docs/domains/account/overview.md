# 用户与账号

## 1. 功能目标

用户与账号域负责识别“谁在使用系统”，并维护用户自己的身份资料。公开主页、关注和拉黑虽然依赖用户信息，但其核心是用户之间的关系，因此记录在[社区与社交](../social/overview.md)。

## 2. 功能范围

- 邮箱验证码注册和邮箱登录。
- OAuth 注册、登录和回调扩展能力。
- JWT 用户会话及受保护路由。
- 当前用户基础信息和个人中心。
- 昵称、简介、技术方向等资料编辑。
- 头像和个人中心 Banner 上传与绑定。

## 3. 核心规则

- 用户端 Token 与管理员 Token 相互隔离。
- 当前用户身份从服务端验证后的 JWT 获取，客户端不能指定操作者。
- 私有资料只通过个人中心接口返回；公开主页只返回明确允许公开的字段。
- 用户上传图片时，数据库保存 COS object key，前端只接收短时签名 URL。

## 4. 接口与实现

- 认证接口：`/api/app/auth/**`
- 当前用户：`/api/app/user/info`
- 个人中心：`/api/app/user-center/**`
- 前端：`frontend/web-app/src/features/auth`、`features/user-center`
- 后端：`controller/login`、`UserCenterController`

接口字段参见[API 总览](../../reference/api-overview.md)，图片规则参见[用户头像与 Banner](profile-images.md)。
