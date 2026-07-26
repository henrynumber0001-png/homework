package com.homework.web.app.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.homework.model.entity.HitCommentLike;
import org.apache.ibatis.annotations.*;

@Mapper
public interface HitCommentLikeMapper extends BaseMapper<HitCommentLike> {
    @Select("""
            SELECT * FROM hit_comment_like
            WHERE comment_id = #{commentId} AND action_user_id = #{actionUserId}
            LIMIT 1 FOR UPDATE
            """)
    HitCommentLike selectIncludingDeletedForUpdate(@Param("commentId") Long commentId,
                                                   @Param("actionUserId") Long actionUserId);

    @Update("UPDATE hit_comment_like SET is_deleted = 0, updated_time = CURRENT_TIMESTAMP WHERE id = #{id}")
    int restoreById(@Param("id") Long id);

    @Update("UPDATE hit_comment_like SET is_deleted = 1, updated_time = CURRENT_TIMESTAMP WHERE id = #{id} AND is_deleted = 0")
    int deactivateById(@Param("id") Long id);
}
