# Premium / Premium Plus 会员接口

## 会员规则

- Premium 是基础会员，Premium Plus 包含 Premium 的全部权益。
- Premium Plus 有效时优先生效，Premium 时长冻结。
- 全款购买 Premium Plus 会新增总会员时长；补差升级只把 Premium 时长转换为 Premium Plus 时长。
- 同等级重复购买从当前台账末尾继续累加。
- 补差每月 30 元、每月转换 31 天，支持 1-11 个月。

## 获取会员页面

```http
GET /api/app/membership
Authorization: Bearer ...
```

响应包含：

- `memberStatus`：`0=FREE`、`1=PREMIUM`、`2=PREMIUM_PLUS`；
- `currentExpireTime`：当前生效等级的到期时间；
- `baseFreezeExpireTime`：Premium Plus 生效时，冻结的 Premium 最终到期时间；
- `fullPurchaseCards`：Premium 与 Premium Plus 的月、季、年全款套餐；
- 每张卡片的 `fullPurchaseOptions`：该等级可全款购买的套餐；
- `diffUpgradeAvailable`、`maxDiffUpgradeMonths`、`diffUpgradeOptions`：当前补差资格和可购买档位。

前端只能使用接口返回的 `planId` 创建订单，不自行计算价格或可补差月份。

## 创建订单

```http
POST /api/app/membership/orders
Authorization: Bearer ...
Idempotency-Key: 本次购买意图的唯一值
Content-Type: application/json

{
  "planId": 1
}
```

同一个购买意图重试时必须复用相同的 `Idempotency-Key`。后端会再次校验补差资格，隐藏前端入口不能绕过该校验。

订单创建后使用响应中的 `codeUrl` 展示微信支付二维码，并轮询：

```http
GET /api/app/membership/orders/{orderNo}
```

## 订单历史

```http
GET /api/app/membership/orders
```

订单保存会员等级、购买类型、档位月数和成交金额快照，后续套餐改价不会修改历史订单。

## 数据库脚本

- 新环境执行 `sql/membership_v2.sql`。
- 已部署旧单订阅模型的环境执行一次 `sql/membership_dual_ledger_migration.sql`。
- 迁移脚本保留旧 `membership_subscription` 表用于审计，新代码不再读取该表。
