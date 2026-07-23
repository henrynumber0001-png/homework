# 微信 Native 会员支付接入

本项目使用微信支付 API v3 官方 Java SDK，通过 Native 模式为电脑网页返回扫码支付
`code_url`。后端不接受前端提交“支付成功”；会员权益只会在微信回调验签通过后发放。

## 1. 数据库迁移

新环境执行：

```sql
source sql/membership_v2.sql;
```

已有 `membership_order` 表的环境只执行：

```sql
source sql/membership_dual_ledger_migration.sql;
```

## 2. 微信商户平台准备

需要准备：

- 与商户号绑定的应用 AppID；
- 微信支付商户号；
- 商户 API 证书序列号；
- 商户 API 私钥 `apiclient_key.pem`；
- 微信支付公钥 ID 和微信支付公钥文件；
- 32 字节 APIv3 密钥；
- 可被微信公网访问的 HTTPS 回调地址。

回调地址必须是：

```text
https://你的域名/api/payment/wechat/native/notify
```

## 3. 环境变量

```bash
WECHAT_PAY_ENABLED=true
WECHAT_PAY_APP_ID=wx...
WECHAT_PAY_MERCHANT_ID=19...
WECHAT_PAY_MERCHANT_SERIAL_NUMBER=...
WECHAT_PAY_MERCHANT_PRIVATE_KEY_PATH=/secure/path/apiclient_key.pem
WECHAT_PAY_PUBLIC_KEY_ID=PUB_KEY_ID_...
WECHAT_PAY_PUBLIC_KEY_PATH=/secure/path/wechatpay_public_key.pem
WECHAT_PAY_API_V3_KEY=32字节APIv3密钥
WECHAT_PAY_NOTIFY_URL=https://你的域名/api/payment/wechat/native/notify
```

私钥、公钥和 APIv3 密钥不得提交到 Git。生产服务器上的私钥文件应仅允许应用运行
用户读取。

## 4. 前端创建订单

```http
POST /api/app/membership/orders
Authorization: Bearer ...
Idempotency-Key: 前端为本次购买生成的UUID
Content-Type: application/json

{
  "planId": 会员页面接口返回的套餐ID
}
```

成功响应中的关键字段：

```json
{
  "orderNo": "订单号",
  "orderStatus": 1,
  "amountDue": 129.00,
  "currency": "CNY",
  "paymentExpiredTime": "2026-07-21T12:15:00",
  "codeUrl": "weixin://wxpay/bizpayurl?..."
}
```

前端使用二维码库把 `codeUrl` 原样生成二维码，不要修改或 URL encode
该字符串。随后每隔 2—3 秒查询：

```http
GET /api/app/membership/orders/{orderNo}
```

看到 `PAID` 后显示“支付成功”；看到 `EXPIRED` 或 `PAY_FAILED` 后停止轮询。

## 5. 服务端安全边界

- 套餐金额以数据库中的 `membership_plan` 为准，前端不能传入价格；
- 微信回调使用原始请求体及 `Wechatpay-*` 请求头验签；
- 回调解密后再次核对 AppID、商户号、订单号、金额、币种和支付渠道；
- 重复回调由 `confirmPayment()` 幂等处理；
- 只有完成验签的微信支付通知能够调用 `confirmPayment()` 发放会员时长；
- 重复支付通知不会重复增加 Premium 或 Premium Plus 台账。
