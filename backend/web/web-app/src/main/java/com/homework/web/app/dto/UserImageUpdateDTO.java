package com.homework.web.app.dto;

import com.homework.model.enums.UserImageType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class UserImageUpdateDTO {

    @NotBlank
    @Size(max = 512)
    private String imageObjectKey;

    @NotNull
    private UserImageType userImageType;
}
