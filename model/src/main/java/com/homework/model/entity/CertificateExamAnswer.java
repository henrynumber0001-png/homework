package com.homework.model.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.extension.handlers.JacksonTypeHandler;
import com.homework.common.entity.BaseEntity;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

//临时答案
@Data
@TableName(value = "certificate_exam_answer", autoResultMap = true)
public class CertificateExamAnswer extends BaseEntity {
    private Long sessionId; //场次ID
    private Long userId;
    private Long questionId;

    @TableField(typeHandler = JacksonTypeHandler.class)
    private List<String> chosenOptions;
    private LocalDateTime answeredAt; //答题时间
}
