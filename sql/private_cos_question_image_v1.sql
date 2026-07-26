-- 将题目图片的永久存储值由公开 URL 调整为私有 COS 对象 Key。
ALTER TABLE question_info
    CHANGE COLUMN image_url image_object_key VARCHAR(512) NULL;

ALTER TABLE certificate_question_info
    CHANGE COLUMN image_url image_object_key VARCHAR(512) NULL;
