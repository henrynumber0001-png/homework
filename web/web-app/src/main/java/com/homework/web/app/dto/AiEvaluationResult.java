package com.homework.web.app.dto;

import lombok.Data;

import java.math.BigDecimal;

@Data
public class AiEvaluationResult {
    private BigDecimal scoreRate;
    private String accurateComment;
    private String innovativeComment;
    private String missingComment;
    private String wrongComment;
    private String summary;
    private String modelName;
}
