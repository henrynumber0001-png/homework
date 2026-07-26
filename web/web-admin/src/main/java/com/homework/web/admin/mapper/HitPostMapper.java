package com.homework.web.admin.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.homework.model.entity.HitPost;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Update;

/** 后台 Post 治理数据访问接口。 */
public interface HitPostMapper extends BaseMapper<HitPost> {

    /** 在不低于零的前提下原子调整有效评论数。 */
    @Update("""
            UPDATE hit_post
            SET comment_count = GREATEST(0, comment_count + #{delta}),
                updated_time = CURRENT_TIMESTAMP(3)
            WHERE id = #{postId} AND is_deleted = 0
            """)
    int changeCommentCount(@Param("postId") Long postId, @Param("delta") int delta);
}
