package com.homework.web.app.mapper;

import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

@Mapper
public interface CertificateExamLockMapper {

    /**
     * 确保当前用户和题库对应的锁记录存在。
     *
     * INSERT IGNORE 是 MySQL 语法：
     * 如果记录已经存在，则忽略唯一键冲突。
     */
    @Insert("""
            INSERT IGNORE INTO certificate_exam_lock (
                user_id,
                bank_id
            )
            VALUES (
                #{userId},
                #{bankId}
            )
            """)
    int ensureLockRow(@Param("userId") Long userId, @Param("bankId") Long bankId);

    /**
     * 锁住当前用户和题库对应的锁记录。
     *
     * 锁会保持到外层事务提交或回滚。
     */
    @Select("""
            SELECT id
            FROM certificate_exam_lock
            WHERE user_id = #{userId}
              AND bank_id = #{bankId}
            FOR UPDATE
            """)
    Long lockUserBank(@Param("userId") Long userId, @Param("bankId") Long bankId);
}
