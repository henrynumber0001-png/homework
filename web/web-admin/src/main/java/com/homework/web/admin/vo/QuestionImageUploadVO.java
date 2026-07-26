package com.homework.web.admin.vo;

import lombok.Data;

import java.time.LocalDateTime;

/** MinIO 题目图片上传结果。 */
@Data
public class QuestionImageUploadVO {

    /** 后续创建或编辑题目时提交的上传标识。 */
    private String uploadId;

    /** 图片公开访问地址。 */
    private String url;

    /** 临时图片建议清理时间。 */
    private LocalDateTime expiresTime;
}
