package com.homework.web.app.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.homework.model.entity.UserFollower;
import org.apache.ibatis.annotations.*;

@Mapper
public interface UserFollowMapper extends BaseMapper<UserFollower> {

    /** 包含已取消的历史关系并加行锁，供关注按钮做幂等切换。 */
    @Select("""
            SELECT id, follower_user_id, following_user_id, created_time, updated_time, is_deleted
            FROM user_follow
            WHERE follower_user_id = #{followerUserId} AND following_user_id = #{followingUserId}
            LIMIT 1
            FOR UPDATE
            """)
    UserFollower selectIncludingDeletedForUpdate(@Param("followerUserId") Long followerUserId,
                                                 @Param("followingUserId") Long followingUserId);

    @Update("""
            UPDATE user_follow SET is_deleted = 0, updated_time = CURRENT_TIMESTAMP
            WHERE id = #{id} AND is_deleted = 1
            """)
    int restoreById(@Param("id") Long id);

    @Update("""
            UPDATE user_follow SET is_deleted = 1, updated_time = CURRENT_TIMESTAMP
            WHERE id = #{id} AND is_deleted = 0
            """)
    int deactivateById(@Param("id") Long id);
}
