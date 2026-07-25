package com.homework.web.app.dto;

import com.homework.model.enums.ActionStatus;
import lombok.Data;

@Data
public class HitCommentLikeDTO {
    private ActionStatus actionStatus;
}
