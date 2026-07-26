package com.homework.web.app.service.impl;

import com.homework.model.enums.UserAuthIdentityProvider;
import com.homework.common.exception.HomeworkException;
import com.homework.common.result.ResultCodeEnum;
import com.homework.web.app.config.WechatOAuthProperties;
import com.homework.web.app.dto.ThirdPartyUser;
import com.homework.web.app.dto.WechatTokenResponse;
import com.homework.web.app.dto.WechatUserInfoResponse;
import com.homework.web.app.service.ThirdPartyAuthHandler;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestClient;
import org.springframework.web.util.UriComponentsBuilder;

@Service
@ConditionalOnProperty(prefix = "oauth.wechat", name = "enabled", havingValue = "true")
public class WechatAuthHandler implements ThirdPartyAuthHandler {

    private final WechatOAuthProperties properties;

    private final RestClient restClient;

    public WechatAuthHandler(WechatOAuthProperties properties, RestClient.Builder restClientBuilder) {
        this.properties = properties;
        this.restClient = restClientBuilder.build();
    }

    @Override
    public UserAuthIdentityProvider provider() {
        return UserAuthIdentityProvider.WECHAT;
    }

    @Override
    public ThirdPartyUser verifyAndGetUser(String authCode) {
        if(!StringUtils.hasText(authCode)) {
            throw new HomeworkException(ResultCodeEnum.PARAM_ERROR);
        }

        //第一步：通过 authCode 和 环境变量校验 换取 accessToken 和 unionId & openId
        WechatTokenResponse tokenResponse = exchangeAuthCode(authCode);

        if (tokenResponse == null || hasWechatError(tokenResponse.getErrcode())) {
            throw new HomeworkException(ResultCodeEnum.PARAM_ERROR);
        }

        if (!StringUtils.hasText(tokenResponse.getAccessToken())
                || !StringUtils.hasText(tokenResponse.getOpenid())) {
            throw new HomeworkException(ResultCodeEnum.PARAM_ERROR);
        }

        //第二步：通过 accessToken 和 openId 换取 微信userinfo接口中的用户信息(头像信息、昵称等)
        //微信userinfo接口要求的参数就是openId，而非unionId
        WechatUserInfoResponse userInfoResponse = fetchUserInfo(
                tokenResponse.getAccessToken(),
                tokenResponse.getOpenid()
        );

        if (userInfoResponse == null || hasWechatError(userInfoResponse.getErrcode())) {
            throw new HomeworkException(ResultCodeEnum.PARAM_ERROR);
        }

        String externalUserId = chooseExternalUserId(tokenResponse, userInfoResponse); //优先unionId, 其次openId

        ThirdPartyUser thirdPartyUser = new ThirdPartyUser();
        thirdPartyUser.setProvider(UserAuthIdentityProvider.WECHAT);
        thirdPartyUser.setExternalUserId(externalUserId);
        thirdPartyUser.setDisplayName(userInfoResponse.getNickname());
        thirdPartyUser.setAvatar(userInfoResponse.getHeadImgUrl());

        return thirdPartyUser;
    }



    private WechatTokenResponse exchangeAuthCode(String authCode) {
        String url = UriComponentsBuilder
                .fromUriString(properties.getAccessTokenUrl())
                .queryParam("appid", properties.getAppId())
                .queryParam("secret", properties.getAppSecret())
                .queryParam("code", authCode)
                .queryParam("grant_type", "authorization_code")
                .toUriString();

        try {
            return restClient.get()
                    .uri(url)
                    .retrieve()
                    .body(WechatTokenResponse.class);
        } catch (Exception e) {
            throw new HomeworkException(ResultCodeEnum.PARAM_ERROR);
        }
        /*
        微信这个接口通常是 GET query param 的写法，和 Goole 还有 Apple 的 POST form body 写法不一样
         */

    }

    private WechatUserInfoResponse fetchUserInfo(String accessToken, String openid) {
        String url = UriComponentsBuilder
                .fromUriString(properties.getUserInfoUrl())
                .queryParam("access_token", accessToken)
                .queryParam("openid", openid)
                .queryParam("lang", properties.getLang())
                .toUriString();

        try {
            return restClient.get()
                    .uri(url)
                    .retrieve()
                    .body(WechatUserInfoResponse.class);
        } catch (Exception e) {
            throw new HomeworkException(ResultCodeEnum.PARAM_ERROR);
        }
    }

    private String chooseExternalUserId(
            WechatTokenResponse tokenResponse,
            WechatUserInfoResponse userInfoResponse
    ) {
        if (StringUtils.hasText(tokenResponse.getUnionid())) {
            return tokenResponse.getUnionid();
        }

        if (StringUtils.hasText(userInfoResponse.getUnionid())) {
            return userInfoResponse.getUnionid();
        }

        if (StringUtils.hasText(tokenResponse.getOpenid())) {
            return tokenResponse.getOpenid();
        }

        throw new HomeworkException(ResultCodeEnum.PARAM_ERROR);
    }

    private boolean hasWechatError(Integer errcode) {
        return errcode != null && errcode != 0;
    }


}
