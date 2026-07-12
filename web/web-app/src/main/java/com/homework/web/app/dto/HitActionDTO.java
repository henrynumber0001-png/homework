package com.homework.web.app.dto;

import lombok.Data;

@Data
public class HitActionDTO {
    /** 操作者从 JWT 登录上下文获取，防止替其他用户点赞。 */
    /** 1.like;2.favorite;3.repost */
    private Integer actionType;
    /** true 表示强制选中，false 表示强制取消；不传时按原型交互做切换。 */
    private Boolean active;
}
