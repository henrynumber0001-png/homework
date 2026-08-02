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

    /** 查询当前题库有效面试题的最大序号。 */
    @Select("""
            SELECT COALESCE(MAX(question_no), 0)
            FROM interview_question_info
            WHERE bank_id = #{bankId} AND is_deleted = 0
            """)
    Integer selectMaxQuestionNo(@Param("bankId") Long bankId);

    @Select("""
            SELECT COUNT(*)
            FROM interview_question_info
            WHERE bank_id = #{bankId} AND is_deleted = 0
            """)
    Integer selectActiveQuestionCount(@Param("bankId") Long bankId);

    @Select("""
            SELECT *
            FROM interview_question_info
            WHERE bank_id = #{bankId} AND id = #{questionId} AND is_deleted = 0
            FOR UPDATE
            """)
    InterviewQuestionInfo selectActiveForUpdate(
            @Param("bankId") Long bankId,
            @Param("questionId") Long questionId
    );

    /** 把待移动题目暂存到 0，给连续区间腾出目标序号。 */
    @Update("""
            UPDATE interview_question_info
            SET question_no = 0
            WHERE bank_id = #{bankId} AND id = #{questionId}
              AND is_deleted = 0 AND question_no = #{questionNo}
            """)
    int parkQuestionNo(
            @Param("bankId") Long bankId,
            @Param("questionId") Long questionId,
            @Param("questionNo") Integer questionNo
    );

    /** 先把受影响的正序号转成负数，避免唯一索引在顺移过程中发生瞬时冲突。 */
    @Update("""
            UPDATE interview_question_info
            SET question_no = -question_no
            WHERE bank_id = #{bankId} AND is_deleted = 0
              AND question_no BETWEEN #{lowerQuestionNo} AND #{upperQuestionNo}
            """)
    int negateQuestionNoRange(
            @Param("bankId") Long bankId,
            @Param("lowerQuestionNo") Integer lowerQuestionNo,
            @Param("upperQuestionNo") Integer upperQuestionNo
    );

    /** 恢复暂存区间，并按 delta（1 或 -1）完成顺移。 */
    @Update("""
            UPDATE interview_question_info
            SET question_no = -question_no + #{delta}, updated_time = CURRENT_TIMESTAMP(3)
            WHERE bank_id = #{bankId} AND is_deleted = 0
              AND question_no BETWEEN 0 - #{upperQuestionNo} AND 0 - #{lowerQuestionNo}
            """)
    int restoreQuestionNoRange(
            @Param("bankId") Long bankId,
            @Param("lowerQuestionNo") Integer lowerQuestionNo,
            @Param("upperQuestionNo") Integer upperQuestionNo,
            @Param("delta") Integer delta
    );

    @Update("""
            UPDATE interview_question_info
            SET question_no = #{questionNo}, updated_time = CURRENT_TIMESTAMP(3)
            WHERE bank_id = #{bankId} AND id = #{questionId}
              AND is_deleted = 0 AND question_no = 0
            """)
    int placeQuestionNo(
            @Param("bankId") Long bankId,
            @Param("questionId") Long questionId,
            @Param("questionNo") Integer questionNo
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
