package com.homework.common.utils;


import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestClient;

import java.util.HashMap;
import java.util.Map;

@Component
public class TurnstileService {

    private final RestClient restClient;

    @Value("${cloudflare.turnstile.secret-key}")
    private String secretKey;

    @Value("${cloudflare.turnstile.siteverify-url}")
    private String siteverifyUrl;

    public TurnstileService(RestClient.Builder restClientBuilder) {
        this.restClient = restClientBuilder.build();
    }

    public boolean verify(String turnstileToken, String remoteIp) {
        if(!StringUtils.hasText(turnstileToken)) {
            return false;
        }

        Map<String,String> requestBody = new HashMap<>();
        requestBody.put("secret", secretKey);
        requestBody.put("response", turnstileToken);

        if(StringUtils.hasText(remoteIp)) {
            requestBody.put("remoteip", remoteIp);
        }

        TurnstileVerifyResponse response = restClient.post()
                .uri(siteverifyUrl)
                .contentType(MediaType.APPLICATION_JSON)
                .body(requestBody)
                .retrieve()
                .body(TurnstileVerifyResponse.class);

        return response != null && Boolean.TRUE.equals(response.getSuccess());
    }
}
