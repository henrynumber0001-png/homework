package com.homework.web.app.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.homework.model.entity.UserFavoriteQuestion;
import org.apache.ibatis.annotations.*;

@Mapper
public interface UserFavoriteQuestionMapper extends BaseMapper<UserFavoriteQuestion> {

    //因为有唯一索引：UNIQUE (user_id, bank_id, question_id)
    //注意：之所以这三个组合的唯一索引，能保证查询结果最多一条，是因为 这三种组合在一起的唯一性，只能产生一条行数据，所以即便is_deleted是可以重复的字段，也没办法重复
    //所以这组条件最多只能查到一条记录，包括逻辑删除记录
    //因此 LIMIT 1 只要是防御性语句，正常最多只能查到1条
    //FOR UPDATE 不必依赖 LIMIT 1，它会锁住所有符合条件的记录，直到当前事务提交或回滚。

    @Select("""
            SELECT id, user_id, bank_id, question_id, collected_time,
                   created_time, updated_time, is_deleted
            FROM user_favorite_question
            WHERE user_id = #{userId}
              AND bank_id = #{bankId}
              AND question_id = #{questionId}
            LIMIT 1
            FOR UPDATE
            """)
    UserFavoriteQuestion selectIncludingDeletedForUpdate(@Param("userId") Long userId,
                                                          @Param("bankId") Long bankId,
                                                          @Param("questionId") Long questionId);

    /** 恢复历史收藏，并把收藏时间更新为本次操作时间。 */
    @Update("""
            UPDATE user_favorite_question
            SET is_deleted = 0,
                collected_time = CURRENT_TIMESTAMP(3),
                updated_time = CURRENT_TIMESTAMP(3)
            WHERE id = #{id} AND is_deleted = 1
            """)
    int restoreById(@Param("id") Long id);

    /** 只取消当前仍有效的收藏，使重复取消保持幂等。 */
    @Update("""
            UPDATE user_favorite_question
            SET is_deleted = 1,
                updated_time = CURRENT_TIMESTAMP(3)
            WHERE id = #{id} AND is_deleted = 0
            """)
    int deactivateById(@Param("id") Long id);
}
