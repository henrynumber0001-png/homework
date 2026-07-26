package com.homework.model.entity;

import com.baomidou.mybatisplus.annotation.FieldStrategy;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.annotation.Version;
import com.homework.common.entity.BaseEntity;
import com.homework.model.enums.QuestionBankStatus;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("question_bank")
public class QuestionBank extends BaseEntity {

    private String bankName;

    private Long subModuleId;

    @Schema(description = "完成题库的人数")
    private Integer completeCount;

    private BigDecimal avgCorrectRate;

    private Integer viewCount;

    @TableField(insertStrategy = FieldStrategy.NEVER, updateStrategy = FieldStrategy.NEVER)
    private Integer hotScore;

    private Integer priority;

    private Long createUserId;

    /** 创建该题库的后台管理员 ID；历史 App 数据可为空。 */
    private Long createAdminId;

    private LocalDateTime publishedTime;

    /** 后台维护的草稿、已发布或已下架状态。 */
    private QuestionBankStatus status;

    /** 最近一次逻辑删除原因。 */
    private String deleteReason;

    /** 乐观锁版本。 */
    @Version
    private Integer version;
}
