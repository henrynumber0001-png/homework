package com.homework.web.app.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class QqUserInfoResponse {
    private Integer ret;

    private String msg;

    private String nickname;

    private String gender;

    private String province;

    private String city;

    private String year;

    @JsonProperty("figureurl")
    private String figureUrl;

    @JsonProperty("figureurl_1")
    private String figureUrl1;

    @JsonProperty("figureurl_2")
    private String figureUrl2;

    @JsonProperty("figureurl_qq_1")
    private String figureUrlQq1;

    @JsonProperty("figureurl_qq_2")
    private String figureUrlQq2;
}
