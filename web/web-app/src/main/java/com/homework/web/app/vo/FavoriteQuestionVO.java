package com.homework.web.app.vo;

import com.homework.model.entity.UserFavoriteQuestion;
import lombok.Data;

import java.util.List;

@Data
public class FavoriteQuestionVO {

    private long groupId;
    private long moduleId;
    private long subModuleId;
    private long bankId;
    private List<Long> FavoriteQuestionIds;

}
