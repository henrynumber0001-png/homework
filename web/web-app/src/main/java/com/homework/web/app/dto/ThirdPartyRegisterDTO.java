package com.homework.web.app.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Data
public class ThirdPartyRegisterDTO {
    @Schema(description = "注册类型对应的注册id,这里是谷歌/Apple/微信 授权码")
    private String identifier;

    private String authCode;

    @Schema(description = "nickName")
    private String displayName;


}
