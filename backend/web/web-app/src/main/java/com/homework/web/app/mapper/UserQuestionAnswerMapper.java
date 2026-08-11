package com.homework.web.app.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.homework.common.result.PageResult;
import com.homework.model.entity.UserQuestionAnswer;
import com.homework.model.enums.GroupType;
import com.homework.web.app.vo.WrongQuestionBankVO;
import com.homework.web.app.vo.WrongQuestionVO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

@Mapper
public interface UserQuestionAnswerMapper extends BaseMapper<UserQuestionAnswer> {

    /**
     * 唯一索引覆盖 user_id、bank_id、question_id，因此重新作答时必须同时查询逻辑删除记录。
     * FOR UPDATE 避免同一题目的并发提交同时执行恢复或插入。
     */
    @Select("""
            SELECT id, is_deleted AS deleted
            FROM user_question_answer
            WHERE user_id = #{userId}
              AND bank_id = #{bankId}
              AND question_id = #{questionId}
            LIMIT 1
            FOR UPDATE
            """)
    UserQuestionAnswer selectIncludingDeletedForUpdate(@Param("userId") Long userId,
                                                        @Param("bankId") Long bankId,
                                                        @Param("questionId") Long questionId);

    @Update("""
            UPDATE user_question_answer
            SET is_deleted = 0,
                updated_time = CURRENT_TIMESTAMP(3)
            WHERE id = #{id} AND is_deleted = 1
            """)
    int restoreById(@Param("id") Long id);

    IPage<WrongQuestionBankVO> getWrongQuestionBanks(Page<WrongQuestionBankVO> page, @Param("groupType") GroupType groupType, @Param("userId") Long userId);

    IPage<WrongQuestionVO> getWrongQuestions(Page<WrongQuestionVO> page,@Param("userId") Long userId,@Param("bankId") Long bankId);

    int overwriteAllUpdate(@Param("userQuestionAnswer") UserQuestionAnswer userQuestionAnswer);
}
