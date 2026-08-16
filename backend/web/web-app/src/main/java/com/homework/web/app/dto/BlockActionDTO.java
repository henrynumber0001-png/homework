package com.homework.web.app.dto;

import com.homework.model.enums.BlockStatus;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class BlockActionDTO {

    @NotNull
    private BlockStatus blockStatus;
}
