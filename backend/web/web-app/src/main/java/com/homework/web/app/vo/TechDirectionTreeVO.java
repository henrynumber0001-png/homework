package com.homework.web.app.vo;

import com.homework.model.entity.SubTechDirection;
import lombok.Data;

import java.util.List;

@Data
public class TechDirectionTreeVO {

    private Long directionId;
    private String directionName;
    List<SubTechDirectionTreeVO> subTechDirectionTreeVOList;
}
