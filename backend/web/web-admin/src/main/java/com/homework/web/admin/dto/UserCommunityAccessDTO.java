package com.homework.web.admin.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.time.LocalDateTime;

/** 设置或提前恢复用户社区权限的请求。 */
@Data
public class UserCommunityAccessDTO {

    @NotNull
    private Boolean restricted;

    private String scope;

    private LocalDateTime endTime;

    @Size(max = 500)
    private String reason;

    @NotNull
    private Integer version;
}
