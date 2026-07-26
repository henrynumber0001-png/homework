package com.homework.web.app.dto;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.homework.model.enums.ActionStatus;
import com.homework.model.enums.HitActionType;
import com.homework.web.app.converter.HitActionTypeJsonDeserializer;
import lombok.Data;

@Data
public class HitActionDTO {

    /** 1=点赞，2=收藏，3=转发。 */
    @JsonDeserialize(using = HitActionTypeJsonDeserializer.class)
    private HitActionType actionType;

    /** ACTIVATE=执行互动，DEACTIVATE=取消互动。 */
    private ActionStatus actionStatus;
}
