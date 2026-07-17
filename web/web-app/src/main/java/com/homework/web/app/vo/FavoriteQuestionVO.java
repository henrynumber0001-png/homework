package com.homework.web.app.vo;

import com.homework.model.entity.UserFavoriteQuestion;
import com.homework.model.enums.QuestionInfoQuestionType;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.util.List;

@Data
public class FavoriteQuestionVO {

    private long groupId;
    private long moduleId;
    private long subModuleId;
    private long bankId;
    //“我的收藏”功能，也是按照 group-module-subModule-bank-QuestionInfoVO进行展示的
    private List<QuestionInfoVO> questionInfoVOList;

}
