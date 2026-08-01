package com.homework.web.admin.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.homework.model.entity.CertificateQuestionInfo;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

/** 认证题数据访问接口。 */
public interface CertificateQuestionMapper extends BaseMapper<CertificateQuestionInfo> {

    /** 变更：关系表已移除，现在用 bank_id + id 校验认证题归属。 */
    @Select("""
            SELECT *
            FROM certificate_question_info
            WHERE bank_id = #{bankId} AND id = #{questionId}
            """)
    CertificateQuestionInfo selectIncludingDeleted(
            @Param("bankId") Long bankId,
            @Param("questionId") Long questionId
    );

    /** 变更：排序值不再从关系表取得，直接查询当前题库认证题的最大顺序。 */
    @Select("""
            SELECT COALESCE(MAX(sort_order), 0)
            FROM certificate_question_info
            WHERE bank_id = #{bankId} AND is_deleted = 0
            """)
    Integer selectMaxSortOrder(@Param("bankId") Long bankId);

    /** 变更：拖拽排序现在直接更新认证题表，并同时校验题库归属。 */
    @Update("""
            UPDATE certificate_question_info
            SET sort_order = #{sortOrder}, updated_time = CURRENT_TIMESTAMP(3)
            WHERE bank_id = #{bankId} AND id = #{questionId} AND is_deleted = 0
            """)
    int updateSortOrder(
            @Param("bankId") Long bankId,
            @Param("questionId") Long questionId,
            @Param("sortOrder") Integer sortOrder
    );

    /** 变更：逻辑删除同时校验 bank_id，防止跨题库操作题目。 */
    @Update("""
            UPDATE certificate_question_info
            SET is_deleted = 1, is_released = 0, version = version + 1,
                updated_time = CURRENT_TIMESTAMP(3)
            WHERE bank_id = #{bankId} AND id = #{questionId}
              AND is_deleted = 0 AND version = #{version}
            """)
    int logicalDelete(
            @Param("bankId") Long bankId,
            @Param("questionId") Long questionId,
            @Param("version") Integer version
    );

}
