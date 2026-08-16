package com.homework.web.app.vo;

import com.homework.model.enums.BlockStatus;
import lombok.Data;

@Data
public class BlockResultVO {

    private boolean self;
    private boolean blocked;
    private Long profileUserId;
    private BlockStatus blockStatus;
}
