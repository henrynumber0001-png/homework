package com.homework.model.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

/**
 * 用户开始某个认证题库考试时使用的数据库行锁。
 *
 * <p>同一个用户和题库只保留一行，业务事务通过 {@code SELECT ... FOR UPDATE}
 * 串行化同一用户、同一题库的开考操作。</p>
 */
@Data
@TableName("certificate_exam_lock")
public class CertificateExamLock {

    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    private Long userId;

    private Long bankId;
}
