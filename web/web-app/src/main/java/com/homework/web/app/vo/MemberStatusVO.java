package com.homework.web.app.vo;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class MemberStatusVO {
    private Boolean premium;
    private Integer premiumScope;
    private Integer type;
    private LocalDateTime startTime;
    private LocalDateTime expiredTime;
}
