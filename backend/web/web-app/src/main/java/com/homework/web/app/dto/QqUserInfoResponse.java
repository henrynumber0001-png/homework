package com.homework.web.app.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
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

}
