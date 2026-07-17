package com.homework.web.app.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.homework.model.entity.UserFollow;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

@Mapper
public interface UserFollowMapper extends BaseMapper<UserFollow> {


    @Select(
            """
            SELECT COUNT(*) 
            FROM user_follow 
            WHERE followee_user_id = #{userId} AND is_deleted = 0
            """
    )
    //查看当前用户的关注者数量（因此是 当前用户 = 被关注者）
    long countFollowers(@Param("userId") Long userId);

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
            WHERE follower_user_id = #{followerUserId} AND followee_user_id = #{followeeUserId}
            LIMIT 1
            FOR UPDATE 
            """
    )
    UserFollow selectIncludingDeletedForUpdate(@Param("followerUserId") Long followerUserId, @Param("followeeUserId") Long followeeUserId);
}
