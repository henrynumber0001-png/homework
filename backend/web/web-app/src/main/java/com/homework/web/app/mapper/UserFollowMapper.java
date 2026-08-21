package com.homework.web.app.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.homework.model.entity.UserFollow;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

@Mapper
public interface UserFollowMapper extends BaseMapper<UserFollow> {


    @Select(
            """
            SELECT COUNT(*) 
            FROM user_follow 
            WHERE followee_user_id = #{targetUserId} AND is_deleted = 0
            """
    )
    //查看当前用户的关注者数量（因此是 当前用户 = 被关注者）
    long countFollowers(@Param("targetUserId") Long targetUserId);

    @Select(
            """
            SELECT COUNT(*) 
            FROM user_follow 
            WHERE follower_user_id = #{userId} AND is_deleted = 0
            """
    )
    //查询当前用户关注了多少人 （因此是 当前用户 = 发起关注的人）
    long countFollowing(@Param("userId") Long userId);



    @Select(
            """
                    SELECT * 
                    FROM user_follow 
                    WHERE follower_user_id = #{currentUserId} 
                      AND followee_user_id = #{targetUserId}
                    LIMIT 1
                    FOR UPDATE 
                    """
    )
    UserFollow selectIncludingDeletedForUpdate(@Param("currentUserId") Long currentUserId, @Param("targetUserId") Long targetUserId);

    @Update("""
            UPDATE user_follow 
            SET is_deleted = 0,
                updated_time = CURRENT_TIMESTAMP 
                WHERE id = #{followId}
                AND is_deleted = 1""")
    int restoreById(@Param("followId") Long followId);

    @Update("""
            UPDATE user_follow 
            SET is_deleted = 1,
                updated_time = CURRENT_TIMESTAMP 
                WHERE id = #{followId} 
                  AND is_deleted = 0""")
    int deactivateById(@Param("followId") Long followId);
}
