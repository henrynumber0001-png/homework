package com.homework.web.admin.vo;

import lombok.Data;

/** 当前管理员可见的题目关联题库。 */
@Data
public class ReferencedBankVO {

    /** 题库 ID。 */
    private Long bankId;

    /** 题库名称。 */
    private String bankName;
}
