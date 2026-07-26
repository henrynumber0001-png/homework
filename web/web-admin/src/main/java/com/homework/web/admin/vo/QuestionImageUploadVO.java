package com.homework.web.admin.vo;

import lombok.Data;

import java.time.LocalDateTime;

/** 腾讯云 COS 题目图片上传结果。 */
@Data
public class QuestionImageUploadVO {

    /** 后续创建或编辑题目时提交的上传标识。 */
    private String uploadId;

    /** 一小时内可访问私有临时图片的签名预览地址。 */
    private String previewUrl;

    /** 签名预览地址的失效时间。 */
    private LocalDateTime previewUrlExpiresTime;

    /** 上传标识的失效时间，失效后不可再绑定到题目。 */
    private LocalDateTime uploadExpiresTime;
}
