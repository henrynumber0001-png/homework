package com.homework.web.admin.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

/** 发布、下架、删除或恢复资源的统一请求。 */
@Data
public class ResourceActionDTO {

    @NotBlank
    private String action;

    @NotBlank
    @Size(max = 500)
    private String reason;

    @NotNull
    private Integer version;
}
