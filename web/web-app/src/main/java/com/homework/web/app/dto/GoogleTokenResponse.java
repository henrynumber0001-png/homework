package com.homework.web.app.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

@Data
@JsonIgnoreProperties(ignoreUnknown = true) //反序列化 JSON 时，如果 JSON 中存在 Java 类没有定义的字段，就直接忽略，不要报错。
public class GoogleTokenResponse {

    @JsonProperty("access_token")
    private String accessToken;

    @JsonProperty("id_token")
    private String idToken;

    @JsonProperty("expires_in")
    private Long expiresIn;

    @JsonProperty("token_type")
    private String tokenType;

    private String scope;
}

/*
注意：@JsonIgnoreProperties(ignoreUnknown = true) 生效条件
当 Google OAuth 返回的 JSON 比 TokenResponse 中定义的属性多时，@JsonIgnoreProperties(ignoreUnknown = true) 生效。
当 Google OAuth 返回的 JSON 比 TokenResponse 中定义的属性少时，@JsonIgnoreProperties(ignoreUnknown = true) 不生效，因为没有被JSON赋值的属性，直接就是 null。
 */
