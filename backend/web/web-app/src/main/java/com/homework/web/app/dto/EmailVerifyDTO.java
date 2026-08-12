package com.homework.web.app.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.Data;

@Data
public class EmailVerifyDTO {

    @NotBlank @Email String  email;
    @Pattern(regexp ="\\d{6}") String code;
}
