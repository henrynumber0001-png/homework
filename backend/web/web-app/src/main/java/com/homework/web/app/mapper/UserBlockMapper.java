package com.homework.web.app.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.homework.model.entity.UserBlock;
import com.homework.model.entity.UserFollow;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;


public interface UserBlockMapper extends BaseMapper<UserBlock> {

    @Select(
            """
                    SELECT id, 
                           blocker_user_id, 
                           blocked_user_id, 
                           created_time, 
                           updated_time, 
                           is_deleted AS deleted
                    FROM user_block
                    WHERE blocker_user_id = #{currentUserId} AND blocked_user_id = #{profileUserId}
                    FOR UPDATE"""
    )
    UserBlock selectIncludingDeletedForUpdate(@Param("profileUserId") Long profileUserId, @Param("currentUserId") Long currentUserId);

    @Update("""
            UPDATE user_block
            SET is_deleted = 0, updated_time = CURRENT_TIMESTAMP(3)
            WHERE blocker_user_id = #{currentUserId} AND blocked_user_id = #{profileUserId} AND is_deleted = 1"""
    )
    int restoreUserBlock(@Param("profileUserId") Long profileUserId, @Param("currentUserId") Long currentUserId);

    @Update("""
            UPDATE user_block
            SET is_deleted = 1, updated_time = CURRENT_TIMESTAMP(3)
            WHERE blocker_user_id = #{currentUserId} AND blocked_user_id = #{profileUserId} AND is_deleted = 0""")
    int blockByCurrentUser(Long profileUserId, Long currentUserId);
}
