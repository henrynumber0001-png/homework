package com.homework.web.app.service.impl;

import com.google.api.client.googleapis.auth.oauth2.GoogleIdToken;
import com.google.api.client.googleapis.auth.oauth2.GoogleIdTokenVerifier;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.gson.GsonFactory;
import com.homework.model.enums.UserAuthIdentityProvider;
import com.homework.common.exception.HomeworkException;
import com.homework.common.result.ResultCodeEnum;
import com.homework.web.app.config.GoogleOAuthProperties;
import com.homework.web.app.dto.GoogleTokenResponse;
import com.homework.web.app.dto.ThirdPartyUser;
import com.homework.web.app.service.ThirdPartyAuthHandler;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestClient;

import java.util.List;

@Service
@ConditionalOnProperty(prefix = "oauth.google", name = "enabled", havingValue = "true")
public class GoogleAuthHandler implements ThirdPartyAuthHandler {

    private final GoogleOAuthProperties properties;

    private final RestClient restClient;

    private final GoogleIdTokenVerifier verifier;


    public GoogleAuthHandler(GoogleOAuthProperties properties, RestClient.Builder restClientBuilder) {
        this.properties = properties;
        this.restClient = restClientBuilder.build();
        this.verifier = new GoogleIdTokenVerifier.Builder(
                new NetHttpTransport(), GsonFactory.getDefaultInstance()
        )
                .setAudience(List.of(properties.getClientId()))
                .build();
    }

    @Override
    public UserAuthIdentityProvider provider() {
        return UserAuthIdentityProvider.GOOGLE;
    }

    @Override
    public ThirdPartyUser verifyAndGetUser(String authCode) {

        if (!StringUtils.hasText(authCode)) {
            throw new HomeworkException(ResultCodeEnum.PARAM_ERROR);
        }

        GoogleTokenResponse tokenResponse = exchangeAuthCode(authCode);

        if (tokenResponse == null || !StringUtils.hasText(tokenResponse.getIdToken())) {
            throw new HomeworkException(ResultCodeEnum.PARAM_ERROR);
        }

        GoogleIdToken idToken;
        try {
            idToken = verifier.verify(tokenResponse.getIdToken());
        } catch (Exception e) {
            throw new HomeworkException(ResultCodeEnum.PARAM_ERROR);
        }

        if (idToken == null) {
            throw new HomeworkException(ResultCodeEnum.PARAM_ERROR);
        }

        GoogleIdToken.Payload payload = idToken.getPayload();

        ThirdPartyUser user = new ThirdPartyUser();
        user.setProvider(UserAuthIdentityProvider.GOOGLE);
        user.setExternalUserId(payload.getSubject());
        user.setEmail(payload.getEmail());
        user.setDisplayName((String) payload.get("name"));
        user.setAvatar((String) payload.get("picture"));

        return user;
    }

    private GoogleTokenResponse exchangeAuthCode(String authCode) {
        LinkedMultiValueMap<String, String> body = new LinkedMultiValueMap<>();
        body.add("code", authCode);
        body.add("client_id", properties.getClientId());
        body.add("client_secret", properties.getClientSecret());
        body.add("redirect_uri", properties.getRedirectUri());
        body.add("grant_type", "authorization_code"); // grant_type 是固定值，不属于环境差异配置,因此不需要写在配置文件中，直接在代码中写死即可

        /*
        homework 后端对 Google 说：
        我是 client_id 对应的这个应用；
        这是我的 client_secret，用来证明我是这个应用的后端；
        这是刚刚用户授权后给我的 authCode；
        这是当初申请授权时使用的 redirect_uri；
        我现在要用 authorization_code 模式换 token。
         */

        /*
        Google 校验的是：
        authCode 是否真实、没过期、没被用过。
        authCode 是否属于这个 client_id。
        client_secret 是否能证明请求方确实是 homework 后端。
        redirect_uri 是否和前面授权时的一致。
        用户是否确实授权过这个应用。

        client_id + client_secret 可以类比成“homework 这个应用在 Google 那里的应用编号和应用密钥”
         */

        try {
            return restClient.post()
                    .uri(properties.getTokenUri())
                    .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                    .body(body)
                    .retrieve()
                    .body(GoogleTokenResponse.class);
        } catch (Exception e) {
            throw new HomeworkException(ResultCodeEnum.PARAM_ERROR);
        }
    }
}
