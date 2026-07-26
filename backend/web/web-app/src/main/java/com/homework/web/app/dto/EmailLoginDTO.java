package com.homework.web.app.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Data
public class EmailLoginDTO {

    @Schema(description = "identifier = email")
    private String email;
    private String password;
    private String turnstileToken;
}
