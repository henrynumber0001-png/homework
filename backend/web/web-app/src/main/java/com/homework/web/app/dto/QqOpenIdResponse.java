package com.homework.web.app.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class QqOpenIdResponse {
    @JsonProperty("client_id")
    private String clientId;

    private String openId;

    private String error;

    @JsonProperty("error_description")
    private String errorDescription;
}
