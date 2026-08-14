package com.homework.web.app.dto;

import com.homework.model.enums.Gender;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class EditProfileDTO {

    @NotBlank
    @Size(max = 50)
    private String displayName;

    //允许不填
    private Long subTechDirectionId;

    //允许不填
    @Size(max = 50)
    private String companyOrSchool;

    //允许不填
    private Gender gender;

    //允许不填
    @Size(max = 100)
    private String introduction;

    @NotNull
    private Integer version;


}
