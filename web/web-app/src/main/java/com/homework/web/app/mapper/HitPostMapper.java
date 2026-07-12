package com.homework.web.app.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.homework.model.entity.HitPost;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Update;

@Mapper
public interface HitPostMapper extends BaseMapper<HitPost> {

    /**
     * 原子修改互动计数，避免并发请求同时读取旧值后互相覆盖。
     * GREATEST 确保异常重试或历史脏数据也不会产生负数。
     */
    @Update("""
            <script>
            UPDATE hit_post
            SET
              like_count = GREATEST(COALESCE(like_count, 0) + #{likeDelta}, 0),
              favorite_count = GREATEST(COALESCE(favorite_count, 0) + #{favoriteDelta}, 0),
              repost_count = GREATEST(COALESCE(repost_count, 0) + #{repostDelta}, 0),
              updated_time = CURRENT_TIMESTAMP
            WHERE id = #{postId} AND is_deleted = 0
            </script>
            """)
    int changeActionCounters(@Param("postId") Long postId,
                             @Param("likeDelta") int likeDelta,
                             @Param("favoriteDelta") int favoriteDelta,
                             @Param("repostDelta") int repostDelta);

    /** 评论计数同样使用原子 SQL 更新。 */
    @Update("""
            UPDATE hit_post
            SET comment_count = GREATEST(COALESCE(comment_count, 0) + #{delta}, 0),
                updated_time = CURRENT_TIMESTAMP
            WHERE id = #{postId} AND is_deleted = 0
            """)
    int changeCommentCount(@Param("postId") Long postId, @Param("delta") int delta);
}
