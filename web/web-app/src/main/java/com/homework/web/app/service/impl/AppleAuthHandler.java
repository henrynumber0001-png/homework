package com.homework.web.app.service.impl;

import com.homework.model.enums.UserAuthIdentityProvider;
import com.homework.common.exception.HomeworkException;
import com.homework.common.result.ResultCodeEnum;
import com.homework.web.app.config.AppleOAuthProperties;
import com.homework.web.app.dto.AppleTokenResponse;
import com.homework.web.app.dto.ThirdPartyUser;
import com.homework.web.app.service.ThirdPartyAuthHandler;
import com.nimbusds.jose.JOSEObjectType;
import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.JWSHeader;
import com.nimbusds.jose.crypto.ECDSASigner;
import com.nimbusds.jose.crypto.RSASSAVerifier;
import com.nimbusds.jose.jwk.JWK;
import com.nimbusds.jose.jwk.JWKSet;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestClient;

import java.security.KeyFactory;
import java.security.interfaces.ECPrivateKey;
import com.nimbusds.jose.jwk.RSAKey;
import java.security.interfaces.RSAPublicKey;
import java.security.spec.PKCS8EncodedKeySpec;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Base64;
import java.util.Date;

@Service
public class AppleAuthHandler implements ThirdPartyAuthHandler {

    private final AppleOAuthProperties properties;

    private final RestClient restClient;

    private final String APPLE_ISSUER = "https://appleid.apple.com";

    public AppleAuthHandler(AppleOAuthProperties properties, RestClient.Builder restClientBuilder) {
        this.properties = properties;
        this.restClient = restClientBuilder.build();
    }


    @Override
    public UserAuthIdentityProvider provider() {
        return UserAuthIdentityProvider.APPLE;
    }

    @Override
    public ThirdPartyUser verifyAndGetUser(String authCode) {

        if (!StringUtils.hasText(authCode)) {
            throw new HomeworkException(ResultCodeEnum.PARAM_ERROR);
        }

        AppleTokenResponse tokenResponse = exchangeAuthCode(authCode);
        if(tokenResponse == null || !StringUtils.hasText(tokenResponse.getIdToken())){
            throw new HomeworkException(ResultCodeEnum.PARAM_ERROR);
        }
        JWTClaimsSet claims = verifyIdToken(tokenResponse.getIdToken());
        ThirdPartyUser user = new ThirdPartyUser();
        user.setProvider(UserAuthIdentityProvider.APPLE);
        user.setExternalUserId(claims.getSubject());
        user.setEmail((String)claims.getClaim("email"));
        return user;
    }

    private AppleTokenResponse exchangeAuthCode(String authCode) {
        LinkedMultiValueMap<String, String> body = new LinkedMultiValueMap<>();
        body.add("client_id", properties.getClientId());
        body.add("client_secret", createClientSecret());
        body.add("code", authCode);
        body.add("grant_type", "authorization_code");

        if (StringUtils.hasText(properties.getRedirectUri())) {
            body.add("redirect_uri", properties.getRedirectUri());
        }

        try {
            return restClient.post()
                    .uri(properties.getTokenUri())
                    .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                    .body(body)
                    .retrieve()
                    .body(AppleTokenResponse.class);
        } catch (Exception e) {
            throw new HomeworkException(ResultCodeEnum.PARAM_ERROR);
        }
    }

    private String createClientSecret() {
        try {
            Instant now = Instant.now();

            JWSHeader header = new JWSHeader.Builder(JWSAlgorithm.ES256)
                    .keyID(properties.getKeyId())
                    .type(JOSEObjectType.JWT)
                    .build();

            JWTClaimsSet claims = new JWTClaimsSet.Builder()
                    .issuer(properties.getTeamId())
                    .issueTime(Date.from(now))
                    .expirationTime(Date.from(now.plus(180, ChronoUnit.DAYS)))
                    .audience(APPLE_ISSUER)
                    .subject(properties.getClientId())
                    .build();

            SignedJWT jwt = new SignedJWT(header, claims);
            jwt.sign(new ECDSASigner(loadPrivateKey()));

            return jwt.serialize();
        } catch (Exception e) {
            throw new HomeworkException(ResultCodeEnum.PARAM_ERROR);
        }
    }

    private ECPrivateKey loadPrivateKey() throws Exception {
        String privateKey = properties.getPrivateKey()
                .replace("\\n", "\n")
                .replace("-----BEGIN PRIVATE KEY-----", "")
                .replace("-----END PRIVATE KEY-----", "")
                .replaceAll("\\s+", "");

        byte[] keyBytes = Base64.getDecoder().decode(privateKey);
        PKCS8EncodedKeySpec keySpec = new PKCS8EncodedKeySpec(keyBytes);

        return (ECPrivateKey) KeyFactory.getInstance("EC").generatePrivate(keySpec);
    }

    private JWTClaimsSet verifyIdToken(String idToken) {
        try {
            SignedJWT signedJWT = SignedJWT.parse(idToken);

            JWKSet jwkSet = JWKSet.load(new java.net.URL(properties.getJwksUri()));
            JWK jwk = jwkSet.getKeyByKeyId(signedJWT.getHeader().getKeyID());

            if (!(jwk instanceof RSAKey rsaKey)) {
                throw new HomeworkException(ResultCodeEnum.PARAM_ERROR);
            }

            RSAPublicKey publicKey = rsaKey.toRSAPublicKey();
            boolean verified = signedJWT.verify(new RSASSAVerifier(publicKey));

            if (!verified) {
                throw new HomeworkException(ResultCodeEnum.PARAM_ERROR);
            }

            JWTClaimsSet claims = signedJWT.getJWTClaimsSet();

            if (!APPLE_ISSUER.equals(claims.getIssuer())) {
                throw new HomeworkException(ResultCodeEnum.PARAM_ERROR);
            }

            if (!claims.getAudience().contains(properties.getClientId())) {
                throw new HomeworkException(ResultCodeEnum.PARAM_ERROR);
            }

            if (claims.getExpirationTime() == null || claims.getExpirationTime().before(new Date())) {
                throw new HomeworkException(ResultCodeEnum.PARAM_ERROR);
            }

            if (!StringUtils.hasText(claims.getSubject())) {
                throw new HomeworkException(ResultCodeEnum.PARAM_ERROR);
            }

            return claims;
        } catch (HomeworkException e) {
            throw e;
        } catch (Exception e) {
            throw new HomeworkException(ResultCodeEnum.PARAM_ERROR);
        }
    }
}
