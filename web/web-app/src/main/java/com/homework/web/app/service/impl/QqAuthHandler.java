package com.homework.web.app.service.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.homework.model.enums.UserAuthIdentityProvider;
import com.homework.common.exception.HomeworkException;
import com.homework.common.result.ResultCodeEnum;
import com.homework.web.app.config.QqOAuthProperties;
import com.homework.web.app.dto.QqOpenIdResponse;
import com.homework.web.app.dto.QqTokenResponse;
import com.homework.web.app.dto.QqUserInfoResponse;
import com.homework.web.app.dto.ThirdPartyUser;
import com.homework.web.app.service.ThirdPartyAuthHandler;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestClient;
import org.springframework.web.util.UriComponentsBuilder;

@Service
public class QqAuthHandler implements ThirdPartyAuthHandler {

    private final RestClient restClient;

    private final QqOAuthProperties properties;

    private final ObjectMapper objectMapper;

    public QqAuthHandler(QqOAuthProperties properties, RestClient.Builder restClientBuilder, ObjectMapper objectMapper) {
        this.properties = properties;
        this.restClient = restClientBuilder.build();
        this.objectMapper = objectMapper;
    }

    @Override
    public UserAuthIdentityProvider provider() {
        return UserAuthIdentityProvider.QQ;
    }

    @Override
    public ThirdPartyUser verifyAndGetUser(String authCode) {
        if(!StringUtils.hasText(authCode)) {
            throw new HomeworkException(ResultCodeEnum.PARAM_ERROR);
        }

        //第一步：先通过 authCode 和 环境变量，获得 accessToken (QqTokenResponse)
        QqTokenResponse tokenResponse = exchangeAccessToken(authCode);
        if(tokenResponse == null || StringUtils.hasText(tokenResponse.getError()) ||!StringUtils.hasText(tokenResponse.getAccessToken())){
            throw new HomeworkException(ResultCodeEnum.PARAM_ERROR);
        }

        //第二步：通过 accessToken 获取 openId
        QqOpenIdResponse openIdResponse = fetchOpenId(tokenResponse.getAccessToken());
        if(openIdResponse == null || StringUtils.hasText(openIdResponse.getError()) || !StringUtils.hasText(openIdResponse.getOpenId())){
            throw new HomeworkException(ResultCodeEnum.PARAM_ERROR);
        }

        if(StringUtils.hasText(openIdResponse.getClientId()) && !properties.getAppId().equals(openIdResponse.getClientId())){
            throw new HomeworkException(ResultCodeEnum.PARAM_ERROR);
        }

        //第三步：通过accessToken 和 openId 获取 userInfo
        QqUserInfoResponse userInfoResponse = fetchUserInfo(tokenResponse.getAccessToken(), openIdResponse.getOpenId());
        if(userInfoResponse == null || userInfoResponse.getRet() == null || userInfoResponse.getRet() != 0){
            throw new HomeworkException(ResultCodeEnum.PARAM_ERROR);
        }

        ThirdPartyUser thirdPartyUser = new ThirdPartyUser();
        thirdPartyUser.setProvider(UserAuthIdentityProvider.QQ);
        thirdPartyUser.setExternalUserId(openIdResponse.getOpenId()); //本地账号唯一标识用 openid
        thirdPartyUser.setDisplayName(userInfoResponse.getNickname());
        thirdPartyUser.setAvatar(chooseAvatar(userInfoResponse));

        return thirdPartyUser;


    }

    private QqTokenResponse exchangeAccessToken(String authCode) {
        String url = UriComponentsBuilder
                .fromUriString(properties.getTokenUrl())
                .queryParam("grant_type", "authorization_code")
                .queryParam("client_id", properties.getAppId())
                .queryParam("client_secret", properties.getAppKey())
                .queryParam("code", authCode)
                .queryParam("redirect_uri", properties.getRedirectUri())
                .queryParam("fmt", "json")
                .toUriString();

        try {
            return restClient.get()
                    .uri(url)
                    .retrieve()
                    .body(QqTokenResponse.class);
        } catch (Exception e) {
            throw new HomeworkException(ResultCodeEnum.PARAM_ERROR);
        }
    }

    private QqOpenIdResponse fetchOpenId(String accessToken) {
        String url = UriComponentsBuilder
                .fromUriString(properties.getOpenidUrl())
                .queryParam("access_token", accessToken)
                .toUriString();

        try {
            String response = restClient.get()
                    .uri(url)
                    .retrieve()
                    .body(String.class);

            return parseJsonpResponse(response, QqOpenIdResponse.class);
        } catch (Exception e) {
            throw new HomeworkException(ResultCodeEnum.PARAM_ERROR);
        }
    }

    private QqUserInfoResponse fetchUserInfo(String accessToken, String openid) {
        String url = UriComponentsBuilder
                .fromUriString(properties.getUserInfoUrl())
                .queryParam("access_token", accessToken)
                .queryParam("oauth_consumer_key", properties.getAppId())
                .queryParam("openid", openid)
                .toUriString();

        try {
            return restClient.get()
                    .uri(url)
                    .retrieve()
                    .body(QqUserInfoResponse.class);
        } catch (Exception e) {
            throw new HomeworkException(ResultCodeEnum.PARAM_ERROR);
        }
    }
    //openid 接口返回的是 callback( {"client_id":"...","openid":"..."} );
    //这种 JSONP，所以需要 parseJsonpResponse 截出 {...} 再用 Jackson 解析。


    private <T> T parseJsonpResponse(String response, Class<T> clazz) {
        if (!StringUtils.hasText(response)) {
            throw new HomeworkException(ResultCodeEnum.PARAM_ERROR);
        }

        int start = response.indexOf('{');
        int end = response.lastIndexOf('}');
        if (start < 0 || end < start) {
            throw new HomeworkException(ResultCodeEnum.PARAM_ERROR);
        }

        String json = response.substring(start, end + 1);

        try {
            return objectMapper.readValue(json, clazz);
        } catch (Exception e) {
            throw new HomeworkException(ResultCodeEnum.PARAM_ERROR);
        }
    }

    private String chooseAvatar(QqUserInfoResponse response) {
        if (StringUtils.hasText(response.getFigureUrlQq2())) {
            return response.getFigureUrlQq2();
        }
        if (StringUtils.hasText(response.getFigureUrlQq1())) {
            return response.getFigureUrlQq1();
        }
        if (StringUtils.hasText(response.getFigureUrl2())) {
            return response.getFigureUrl2();
        }
        if (StringUtils.hasText(response.getFigureUrl1())) {
            return response.getFigureUrl1();
        }
        return response.getFigureUrl();
    }
}
