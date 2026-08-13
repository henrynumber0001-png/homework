package com.homework.web.app.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class EmailRegisterDTO {


    @NotBlank
    @Email
    @Size(max = 50)
    private String email;

    @NotBlank
    @Size(min = 8, max = 24)
    private String password;

    @NotBlank
    @Size(min = 8, max = 24)
    private String passwordConfirm;

    @NotBlank
    private String secureTicket;

    @NotBlank
    @Size(max = 20)
    private String displayName;
}
