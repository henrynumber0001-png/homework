package com.homework.model.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.homework.common.entity.BaseEntity;
import com.homework.model.enums.ItemType;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Data
@TableName(value = "graph_info")
public class GraphInfo extends BaseEntity {

    @Schema(description = "图片名称")
    @TableField(value = "name")
    private String name;

    @Schema(description = "图片所属枚举常量类型")
    @TableField(value = "item_type")
    private ItemType itemType;

    private Long itemId;

    @Schema(description = "图片地址")
    @TableField(value = "url")
    private String url;


}
