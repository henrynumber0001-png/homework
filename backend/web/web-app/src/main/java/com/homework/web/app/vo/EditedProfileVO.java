package com.homework.web.app.vo;

import com.homework.model.enums.Gender;
import lombok.Data;

@Data
public class EditedProfileVO {

    private String displayName;

    //允许不填
    private Long subTechDirectionId;

    //允许不填
    private String companyOrSchool;

    //允许不填
    private Gender gender;

    //允许不填
    private String introduction;

    private Integer version;
}
