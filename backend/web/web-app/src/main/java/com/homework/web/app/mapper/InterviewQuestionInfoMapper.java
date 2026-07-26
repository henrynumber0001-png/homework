package com.homework.web.app.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.homework.model.entity.InterviewQuestionInfo;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Update;

@Mapper
public interface InterviewQuestionInfoMapper extends BaseMapper<InterviewQuestionInfo> {

    @Update("""
                    UPDATE question_bank
                    SET view_count = view_count + 1
                    WHERE id = #{bankId} AND is_deleted = 0
            """)
    int incrementViewCount(Long bankId);


    @Update("""
        UPDATE question_bank
        SET complete_count = COALESCE(complete_count,0) + 1
        WHERE id = #{bankId}
          AND is_deleted = 0
        """)
    int bankCompletionCount(Long bankId);

}
