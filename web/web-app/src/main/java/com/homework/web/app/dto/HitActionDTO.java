package com.homework.web.app.dto;

import lombok.Data;

@Data
public class HitActionDTO {
    private Long userId;
    /** 1.like;2.favorite;3.repost */
    private Integer actionType;
    /** true 表示强制选中，false 表示强制取消；不传时按原型交互做切换。 */
    private Boolean active;
}
