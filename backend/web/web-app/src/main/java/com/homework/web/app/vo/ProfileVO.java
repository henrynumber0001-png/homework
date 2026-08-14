package com.homework.web.app.vo;

import com.homework.model.enums.Gender;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.util.List;

@Data
public class ProfileVO {

    @NotBlank
    @Size(max = 50)
    private String displayName;

    private String avatarUrl;


    @Size(max = 50)
    private String companyOrSchool;

    private Long subTechDirectionId;

    //允许不填
    private Gender gender;

    //允许不填
    @Size(max = 100)
    private String introduction;

    private Integer version;


}
