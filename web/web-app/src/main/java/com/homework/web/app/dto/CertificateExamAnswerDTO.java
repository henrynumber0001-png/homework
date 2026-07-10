package com.homework.web.app.dto;

import lombok.Data;

import java.util.List;

/**
 * 接收考试过程中某一道题的临时选择。
 * 这里故意不接收正确答案、题型或用户 id，这些信息必须由后端查询和判断。
 */
@Data
public class CertificateExamAnswerDTO {

    /**
     * 用户本次选择的选项。
     * 空列表表示用户清空了这道题的选择，null 表示请求参数不完整。
     */
    private List<String> chosenOptions;
}
