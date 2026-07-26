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
@TableName("question_info")
public class InterviewQuestionInfo extends BaseEntity {

    private String title;

    @Schema(description = "参考答案")
    private String analysis;

    private QuestionInfoQuestionType questionType;

    private Boolean isReleased;

    private Long createUserId;

    /** 创建该题目的后台管理员 ID；历史 App 数据可为空。 */
    private Long createAdminId;

    private Integer sortOrder;

    /** 题干图片在私有对象存储中的对象 Key。 */
    private String imageObjectKey;

    /** 乐观锁版本。 */
    @Version
    private Integer version;
}
