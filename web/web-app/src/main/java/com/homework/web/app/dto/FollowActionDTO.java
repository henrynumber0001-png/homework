package com.homework.web.app.dto;

import lombok.Data;

/** 关注按钮参数；不传 active 时切换，传入后接口保持幂等。 */
@Data
public class FollowActionDTO {
    private Boolean active;
}
