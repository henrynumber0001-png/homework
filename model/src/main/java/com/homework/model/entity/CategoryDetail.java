package com.homework.model.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.homework.common.entity.BaseEntity;
import java.math.BigDecimal;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("category_detail")
public class CategoryDetail extends BaseEntity {

    private Long subModuleId;

    private String detailName;

    private Integer participantCount;

    private BigDecimal avgCorrectRate;

    private String tagsJson;

    private Integer sortOrder;
}
