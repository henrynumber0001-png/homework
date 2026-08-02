package com.homework.web.admin.dto;

import com.homework.model.enums.QuestionAction;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

/** 发布、下架或删除题目的请求。 */
@Data
public class QuestionActionDTO {

    /** 本次要对题目执行的固定状态动作。 */
    @NotNull
    private QuestionAction action;

    /** 管理员执行本次状态动作的原因。 */
    @NotBlank
    @Size(max = 500)
    private String reason;

    /** 前端最近一次读取到的题目乐观锁版本。 */
    @NotNull
    private Integer version;
}
