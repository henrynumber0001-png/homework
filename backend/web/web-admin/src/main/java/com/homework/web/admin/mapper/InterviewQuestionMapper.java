package com.homework.web.admin.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.homework.model.entity.InterviewQuestionInfo;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

/** 面试题数据访问接口。 */
public interface InterviewQuestionMapper extends BaseMapper<InterviewQuestionInfo> {

    /** 变更：表名改为 interview_question_info，并用 bank_id + id 校验题目归属。 */
    @Select("""
            SELECT *
            FROM interview_question_info
            WHERE bank_id = #{bankId} AND id = #{questionId}
            """)
    InterviewQuestionInfo selectIncludingDeleted(
            @Param("bankId") Long bankId,
            @Param("questionId") Long questionId
    );

    /** 变更：排序值不再从关系表取得，直接查询当前题库面试题的最大顺序。 */
    @Select("""
            SELECT COALESCE(MAX(sort_order), 0)
            FROM interview_question_info
            WHERE bank_id = #{bankId} AND is_deleted = 0
            """)
    Integer selectMaxSortOrder(@Param("bankId") Long bankId);

    /** 变更：拖拽排序现在直接更新面试题表，并同时校验题库归属。 */
    @Update("""
            UPDATE interview_question_info
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
            UPDATE interview_question_info
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
