package com.homework.model.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.homework.common.entity.BaseEntity;
import com.homework.model.enums.GroupType;
import lombok.Data;

import java.math.BigDecimal;

@Data
@TableName("user_bank_correct_rate")
public class UserBankCorrectRate extends BaseEntity {
    private Long userId;
    private Long bankId;
    private GroupType groupType;
    private BigDecimal correctRate;
}
