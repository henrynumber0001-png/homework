package com.homework.web.admin.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.homework.model.entity.QuestionBankQuestion;
import com.homework.model.enums.GroupType;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

/** 题库与题目关系数据访问接口。 */
public interface QuestionBankQuestionMapper extends BaseMapper<QuestionBankQuestion> {

    /** 按题库一级类型查询题目的全部关联题库，避免两张题目表的自增 ID 冲突。 */
    @Select("""
            SELECT qbq.*
            FROM question_bank_question qbq
            JOIN question_bank qb ON qb.id = qbq.bank_id
            JOIN category_sub_module csm ON csm.id = qb.sub_module_id
            JOIN category_module cm ON cm.id = csm.module_id
            JOIN category_group cg ON cg.id = cm.group_id
            WHERE qbq.question_id = #{questionId}
              AND cg.group_type = #{groupType}
              AND qbq.is_deleted = 0
            ORDER BY qbq.bank_id
            """)
    java.util.List<QuestionBankQuestion> selectQuestionRelations(
            @Param("questionId") Long questionId,
            @Param("groupType") GroupType groupType
    );

    /** 查询题库当前最大的题目排序值。 */
    @Select("""
            SELECT COALESCE(MAX(sort_order), 0)
            FROM question_bank_question
            WHERE bank_id = #{bankId} AND is_deleted = 0
            """)
    Integer selectMaxSortOrder(@Param("bankId") Long bankId);

    /** 更新单条题库题目关系的排序值。 */
    @Update("""
            UPDATE question_bank_question
            SET sort_order = #{sortOrder}, updated_time = CURRENT_TIMESTAMP(3)
            WHERE bank_id = #{bankId} AND question_id = #{questionId} AND is_deleted = 0
            """)
    int updateSortOrder(
            @Param("bankId") Long bankId,
            @Param("questionId") Long questionId,
            @Param("sortOrder") Integer sortOrder
    );
}
