# 私有 COS 题目图片 URL 鉴权与部署

## 1. 方案

- 腾讯云轻量对象存储保持“私有读写”。
- 数据库只保存对象 Key，例如 `questions/2026-07-26/1785062400000-a.webp`。
- 管理端临时上传目录为 `admin-temp/questions/*`，题目保存后复制到 `questions/*`。
- web-admin 和 web-app 先完成原有登录、权限或会员校验，再在返回 VO 时生成只读签名 URL。
- 题目图片签名 URL 有效期统一为 1 小时，不提供可传入任意对象 Key 的公共签名接口。
- 上传接口返回的 `uploadId` 有效期为 24 小时；预览 URL 失效不影响 `uploadId` 继续绑定。

## 2. 创建两个子账号

在腾讯云访问管理 CAM 中创建两个仅用于编程访问的子账号：

| 子账号建议名称 | 使用服务 | 权限 |
| --- | --- | --- |
| `homework-web-admin-cos` | web-admin | 上传、读取、复制和删除题目图片 |
| `homework-web-app-cos` | web-app | 只读正式题目图片 |

不要给这两个账号控制台登录权限，也不要关联 `QcloudCOSFullAccess` 等全量策略。

## 3. web-admin 自定义策略

在 CAM 的“策略 > 新建自定义策略 > 按策略语法创建”中粘贴以下内容。将示例中的地域、APPID 和完整存储桶名称替换为真实值。

```json
{
  "version": "2.0",
  "statement": [
    {
      "effect": "allow",
      "action": [
        "name/cos:PutObject",
        "name/cos:GetObject",
        "name/cos:DeleteObject"
      ],
      "resource": [
        "qcs::cos:ap-guangzhou:uid/1250000000:homework-1250000000/admin-temp/questions/*",
        "qcs::cos:ap-guangzhou:uid/1250000000:homework-1250000000/questions/*"
      ]
    }
  ]
}
```

同一存储桶内复制对象时，临时源对象需要 `GetObject`，正式目标对象需要 `PutObject`；保存成功后删除临时对象需要 `DeleteObject`。

## 4. web-app 自定义策略

```json
{
  "version": "2.0",
  "statement": [
    {
      "effect": "allow",
      "action": [
        "name/cos:GetObject"
      ],
      "resource": [
        "qcs::cos:ap-guangzhou:uid/1250000000:homework-1250000000/questions/*"
      ]
    }
  ]
}
```

web-app 不应访问 `admin-temp/questions/*`，也不需要上传、删除、列出对象或修改存储桶的权限。

## 5. 创建和保存密钥

分别进入两个子账号的“用户详情 > API 密钥 > 新建密钥”，各创建一组 `SecretId` 和 `SecretKey`。`SecretKey` 只在创建时展示，必须立即保存到密码管理器；不要写入 Git、配置文件模板、前端代码或聊天记录。

## 6. 服务器配置

两个 Java 服务可以使用相同的环境变量名称，但必须通过不同的环境文件注入不同的密钥。

`/etc/homework/web-admin.env`：

```dotenv
TENCENT_COS_REGION=ap-guangzhou
TENCENT_COS_SECRET_ID=<web-admin 子账号 SecretId>
TENCENT_COS_SECRET_KEY=<web-admin 子账号 SecretKey>
TENCENT_COS_BUCKET=homework-1250000000
TENCENT_COS_READ_URL_TTL_SECONDS=3600
```

`/etc/homework/web-app.env`：

```dotenv
TENCENT_COS_REGION=ap-guangzhou
TENCENT_COS_SECRET_ID=<web-app 子账号 SecretId>
TENCENT_COS_SECRET_KEY=<web-app 子账号 SecretKey>
TENCENT_COS_BUCKET=homework-1250000000
TENCENT_COS_READ_URL_TTL_SECONDS=3600
```

环境文件权限设置为仅 root 可读：

```bash
sudo chown root:root /etc/homework/web-admin.env /etc/homework/web-app.env
sudo chmod 600 /etc/homework/web-admin.env /etc/homework/web-app.env
```

在两个 systemd 服务中分别配置：

```ini
# homework-web-admin.service
EnvironmentFile=/etc/homework/web-admin.env
```

```ini
# homework-web-app.service
EnvironmentFile=/etc/homework/web-app.env
```

不要把两组密钥一起放到全局 `/etc/environment`。修改配置后执行：

```bash
sudo systemctl daemon-reload
sudo systemctl restart homework-web-admin homework-web-app
```

## 7. 数据库迁移

首次部署包含本次改造的版本前执行：

```bash
mysql -u DB_USER -p DB_NAME < sql/private_cos_question_image_v1.sql
```

该脚本将 `question_info` 和 `certificate_question_info` 的 `image_url` 字段直接更名为 `image_object_key`。

## 8. 验证

1. 管理端上传图片后，响应应包含 `uploadId`、`previewUrl`、`previewUrlExpiresTime` 和 `uploadExpiresTime`。
2. 保存题目后，数据库应保存 `questions/*` 对象 Key，不应保存 `http://` 或 `https://` 地址。
3. 退出登录后，不能通过后台接口获得新的签名 URL；已有签名 URL 最长还可使用 1 小时。
4. web-app 密钥应能读取 `questions/*`，但上传、删除及读取 `admin-temp/questions/*` 都应被 COS 拒绝。
5. web-admin 密钥应能完成上传、复制、删除临时对象和生成预览 URL，但不能创建、删除或修改存储桶。
