package com.homework.web.app.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.homework.model.entity.PrivateChatbox;
import org.apache.ibatis.annotations.*;

@Mapper
public interface PrivateChatboxMapper extends BaseMapper<PrivateChatbox> {
    @Insert("""
            INSERT IGNORE INTO private_chatbox
                (user_a_id, user_b_id, initiator_user_id, chat_access,
                 created_time, updated_time, is_deleted)
            VALUES
                (#{userAId}, #{userBId}, #{initiatorUserId}, #{chatAccess},
                 CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 0)
            """)
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insertIfAbsent(PrivateChatbox chatbox);

    @Select("""
            SELECT * FROM private_chatbox
            WHERE user_a_id = #{userAId} AND user_b_id = #{userBId} AND is_deleted = 0
            LIMIT 1 FOR UPDATE
            """)
    PrivateChatbox selectForUpdate(@Param("userAId") Long userAId, @Param("userBId") Long userBId);
}
