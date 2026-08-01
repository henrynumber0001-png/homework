package com.homework.web.admin.vo;

import lombok.Data;

import java.time.LocalDateTime;

/** 腾讯云 COS 题目图片上传结果。 */
@Data
public class QuestionImageUploadVO {

    /** 后续创建或编辑题目时提交的临时 COS 对象 Key。 */
    private String objectKey;

    //让管理员立即预览刚上传的图片。
    private String previewUrl;

    //告诉前端预览地址什么时候过期。
    private LocalDateTime previewUrlExpiresTime;

    /** 临时对象 Key 的失效时间，失效后不可再绑定到题目。 */
    //告诉前端这个临时对象 Key 什么时候不能再绑定。
    private LocalDateTime uploadExpiresTime;
}
