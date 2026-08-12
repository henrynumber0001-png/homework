package com.homework.web.app.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class EmailSendDTO{
    @NotBlank @Email String email;
    @NotBlank String turnstileToken;


}
