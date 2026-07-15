package com.homework.web.app.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.homework.model.entity.UserFavoriteQuestion;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Update;

@Mapper
public interface UserFavoriteQuestionMapper extends BaseMapper<UserFavoriteQuestion> {


    @Update(
            """
            UPDATE user_favorite_question ufq
            SET ufq.is_deleted = 0
            WHERE ufq.question_id = #{questionId} AND ufq.user_id = #{userId} AND ufq.is_deleted = 1
            """
    )
    int restoreById(Long questionId,Long userId);
}
