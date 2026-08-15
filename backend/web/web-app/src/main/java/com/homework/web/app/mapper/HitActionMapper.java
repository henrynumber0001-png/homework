package com.homework.web.app.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.homework.model.entity.HitAction;
import org.apache.ibatis.annotations.*;

@Mapper
public interface HitActionMapper extends BaseMapper<HitAction> {

    /** MyBatis-Plus 会自动过滤逻辑删除记录，因此这里显式查询历史互动。 */
    @Select("""
            SELECT id, 
                   post_id, 
                   action_user_id, 
                   action_type, 
                   created_time, 
                   updated_time, 
                   is_deleted AS deleted
            FROM hit_action
            WHERE post_id = #{postId} AND action_user_id = #{actionUserId} AND action_type = #{actionType}
            LIMIT 1
            FOR UPDATE
            """)
    HitAction selectIncludingDeletedForUpdate(@Param("postId") Long postId,
                                              @Param("actionUserId") Long actionUserId,
                                              @Param("actionType") Integer actionType);

    /** 恢复曾取消的互动，复用唯一键对应的历史记录。 */
    @Update("""
            UPDATE hit_action
            SET is_deleted = 0, updated_time = CURRENT_TIMESTAMP
            WHERE id = #{id} AND is_deleted = 1
            """)
    int restoreById(@Param("id") Long id);

    /** 只删除当前仍有效的互动，使重复取消请求保持幂等。 */
    @Update("""
            UPDATE hit_action
            SET is_deleted = 1, updated_time = CURRENT_TIMESTAMP
            WHERE id = #{id} AND is_deleted = 0
            """)
    int deactivateById(@Param("id") Long id);
}
