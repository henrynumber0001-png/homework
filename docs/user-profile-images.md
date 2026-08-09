# 用户头像与个人中心 Banner 存储

## 数据模型

- `user_info.avatar_object_key`：用户主动上传头像的私有 COS object key。
- `user_info.banner_object_key`：用户个人中心 Banner 的私有 COS object key。

前端只能接收签名 URL，数据库只保存 object key，不能保存预签名 URL。
用户上传头像优先；没有上传头像时，使用 OAuth 平台头像作为兜底。

App 后端使用的 COS 子账号不能再是纯只读权限。请按对象前缀授予最小权限：允许在
`temp/user/image/` 上传、复制和删除对象，允许在 `user/image/` 写入、读取和删除对象；
不要把 SecretId/SecretKey 下发给浏览器。

## 对象 key

临时对象：

```text
temp/user/image/{avatar|banner}/{userId}/{uuid}.{ext}
```

正式对象：

```text
user/image/{avatar|banner}/{userId}/{uuid}.{ext}
```

上传时，服务端通过文件头校验真实图片格式和大小。确认绑定时，通过固定 key 前缀校验
图片种类和当前用户 ID，并只接受没有子目录的 JPG、PNG、WebP 文件名。

## 临时对象清理

不要为常规临时图清理新增应用定时任务。请在腾讯云 COS 存储桶中配置生命周期规则：

- 作用前缀：`temp/user/image/`
- 管理当前版本文件：开启
- 过期删除：对象最后修改 1 天后
- 如果存储桶开启过版本控制，同时配置非当前版本和删除标记清理

COS 生命周期删除是异步执行；对象被删除后，确认接口复制图片时会直接失败。规则配置说明：
<https://cloud.tencent.com/document/product/436/17031>

只有在无法使用 COS 生命周期、或需要分钟级清理时，才考虑应用定时任务；多实例部署时还必须增加分布式锁。
