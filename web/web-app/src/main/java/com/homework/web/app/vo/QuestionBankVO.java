package com.homework.web.app.vo;

import com.baomidou.mybatisplus.annotation.TableField;
import com.homework.model.entity.BankTag;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

@Data
public class QuestionBankVO {
    private Long id;

    private String bankName;

    private Long subModuleId;

    @Schema(description = "完成题库的人数")
    private Integer completeUserCount;

    private BigDecimal avgCorrectRate;

    private Integer favoriteCount;

    private Integer viewCount;

    private Integer priority;

    @TableField("is_premium")
    private Boolean isPremium;

    private Integer questionCount;

    private List<String> tagNames;

}
