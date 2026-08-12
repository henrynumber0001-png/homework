package com.homework.web.app.service.impl;

import com.homework.model.enums.UserAuthIdentityProvider;
import com.homework.common.exception.HomeworkException;
import com.homework.common.result.ResultCodeEnum;
import com.homework.web.app.dto.ThirdPartyUser;
import com.homework.web.app.service.ThirdPartyAuthHandler;
import com.homework.web.app.service.ThirdPartyAuthService;
import org.springframework.stereotype.Service;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;

@Service
public class ThirdPartyAuthServiceImpl implements ThirdPartyAuthService {

    private final Map<UserAuthIdentityProvider, ThirdPartyAuthHandler> handlerMap =
            new EnumMap<>(UserAuthIdentityProvider.class);

    public ThirdPartyAuthServiceImpl(List<ThirdPartyAuthHandler> handlers) {
        for (ThirdPartyAuthHandler handler : handlers) {
            handlerMap.put(handler.provider(), handler);
        }
    }

    @Override
    public ThirdPartyUser verifyAndGetUser(UserAuthIdentityProvider provider, String authCode) {
        ThirdPartyAuthHandler handler = handlerMap.get(provider);
        if (handler == null) {
            throw new HomeworkException(ResultCodeEnum.PARAM_ERROR);
        }
        return handler.verifyAndGetUser(authCode);
    }
    /*
    第三方授权账号 这种方式，最重要的就是 ThirdPartyAuthServiceImpl 这个类
    它相当于一个 DispatcherServlet, 居中调度。
    从前端获取provider和authCode信息（传入UserAuthIdentityServiceImpl的registerByOAuth方法），
    然后分发给下游的 ThirdPartyAuthHandler，
    根据provider找到对应的handler，调用 handler 的 verifyAndGetUser 方法，传入 authCode, 获取id_token, 解析并转换成 ThirdPartyUser 对象并返回。
    这个ThirdPartyUser对象中，就包含从 id_token 中获取的 subject(userId), externalUserId
     */

    /*
    authCode: 前端从Google登录流程拿到的一次性授权码
    它是前端完成 Google 授权后拿到的临时凭证。它一般很短命，只能用一次。你的后端用它去换真正的 token。

    id_token：Google 签发的身份令牌。
    它本质上是一个 JWT，里面包含 Google 对“当前登录用户是谁”的声明，比如 subject(userId)、email、name、picture。
    你的代码通过 GoogleIdTokenVerifier 验证它可信。
    authCode/client_id/client_secret/client_uri 都校验完毕了之后，Google会返回这个用户的Google账户的id_token。

    externalUserId：是 id_token 的 userId 字段, 即：idToken.getPayload().getSubject()
    id_token = 一份带签名的 JWT 身份证明
    subject = 这份证明里面的用户唯一 ID
    最终 UserAuthIdentity 将 externalUserId 规范化后存入 account 字段，等效于邮箱登录身份的 account
     */

    /*
    用户在前端完成 Google 授权
            ↓
    Google 给前端 authCode
            ↓
    前端把 authCode 发给 homework 后端
            ↓
    homework 后端用 authCode + client_id + client_secret + redirect_uri 请求 Google (Google会根据 client_id + client_secret 来校验请求方是否是 homework 后端)
            ↓
    Google 校验通过，返回 id_token
            ↓
    homework 后端验证 id_token
            ↓
    从 id_token 里取 sub(userId)
            ↓
    sub(userId) 成为 externalUserId

    client_id + client_secret 可以类比成“homework 这个应用在 Google 那里的应用编号和应用密钥”
    Google就是看：账号密码是否正确，发送的授权码是否属于这个账号
    是，就把包含 请求用户的用户信息的 id_token(JWT) 返回
     */

}
