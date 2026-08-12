package com.homework.web.app.dto;

import com.homework.model.enums.UserAuthIdentityProvider;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Data
public class ThirdPartyRegisterDTO {

    @Schema(description = "注册的第三方渠道类型")
    private UserAuthIdentityProvider identityProvider; //保留这个属性，因为 ThirdPartAuthServiceImpl 的 verifyAndGetUser方法 需要凭借 传入identityProvider属性，搭配authCode才能返回 thirdPartyUser 对象

    // account 不从前端接收；服务端验证 authCode 后从 id_token 的 sub/unionid/openid 中取得。

    @Schema(description = "注册类型对应的注册id,这里是谷歌/Apple/微信 授权码")
    private String authCode;

    @Schema(description = "turnstile token")
    private String turnstileToken;


}
