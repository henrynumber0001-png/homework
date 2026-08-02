package com.homework.web.admin.dto;

import com.homework.model.enums.CommunityContentAction;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

/** 隐藏、恢复或删除社区动态和评论的请求。 */
@Data
public class CommunityContentActionDTO {

    /** 本次要对社区内容执行的固定治理动作。 */
    @NotNull
    private CommunityContentAction action;

    /** 管理员执行本次治理动作的原因。 */
    @NotBlank
    @Size(max = 500)
    private String reason;

    /** 前端最近一次读取到的社区内容乐观锁版本。 */
    @NotNull
    private Integer version;
}
