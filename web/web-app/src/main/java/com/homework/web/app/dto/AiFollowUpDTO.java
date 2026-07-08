package com.homework.web.app.dto;

import com.homework.model.enums.GroupType;
import lombok.Data;

@Data
public class AiFollowUpDTO {

    /** 当前答案解析所在题库 id。AI 会话按 userId + bankId 复用。 */
    private Long bankId;

    /** 当前答案解析所在题目 id。用于构造本轮追问的题目上下文。 */
    private Long questionId;

    /** 题库类型：1.面试题库；2.认证题库。决定从哪张题表查询题目解析。 */
    private GroupType bankType;

    /** 用户在“追问AI”输入框中提交的问题。 */
    private String message;
}
