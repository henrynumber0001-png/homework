package com.homework.model.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.annotation.Version;
import com.homework.common.entity.BaseEntity;
import com.homework.model.enums.QuestionInfoQuestionType;
import com.homework.model.enums.QuestionInfoStatus;
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

    /** 后台维护的草稿、已发布、已下架或已删除状态。 */
    private QuestionInfoStatus status;

    private Long createUserId;

    /** 创建该题目的后台管理员 ID；历史 App 数据可为空。 */
    private Long createAdminId;

    // 题库内连续且唯一的题目编号，从 1 开始。
    // 题目编号 不是 用户页面看到的题目的序号，题目序号在用户页面显示的是数组下标 index + 1
    private Integer questionNo;

    /** 题干图片在私有对象存储中的对象 Key。 */
    private String imageObjectKey;

    /** 乐观锁版本。 */
    @Version
    private Integer version;
}
