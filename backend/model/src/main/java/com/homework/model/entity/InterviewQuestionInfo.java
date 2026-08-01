package com.homework.model.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.annotation.Version;
import com.homework.common.entity.BaseEntity;
import com.homework.model.enums.QuestionInfoQuestionType;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("interview_question_info")
public class InterviewQuestionInfo extends BaseEntity {

    /** 变更：题目原来通过 question_bank_question 关联题库，现在直接保存所属题库 ID。 */
    private Long bankId;

    private String title;

    @Schema(description = "参考答案")
    private String analysis;

    private QuestionInfoQuestionType questionType;

    private Boolean isReleased;

    private Long createUserId;

    /** 创建该题目的后台管理员 ID；历史 App 数据可为空。 */
    private Long createAdminId;

    /** 变更：排序值现在属于题目本身，数值越小，在题库中的手动顺序越靠前。 */
    private Integer sortOrder;

    /** 题干图片在私有对象存储中的对象 Key。 */
    private String imageObjectKey;

    /** 乐观锁版本。 */
    @Version
    private Integer version;
}
