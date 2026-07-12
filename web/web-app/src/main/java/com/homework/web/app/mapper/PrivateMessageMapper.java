package com.homework.web.app.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.homework.model.entity.PrivateMessage;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

@Mapper
public interface PrivateMessageMapper extends BaseMapper<PrivateMessage> {

    /** 查询历史记录时不受逻辑删除过滤影响，防止删除后绕过“仅第一条”的规则。 */
    @Select("""
            SELECT COUNT(*)
            FROM private_message
            WHERE sender_user_id = #{senderUserId}
              AND receiver_user_id = #{receiverUserId}
              AND allow_reason = 2
            """)
    long countFirstNonMutualMessages(@Param("senderUserId") Long senderUserId,
                                     @Param("receiverUserId") Long receiverUserId);

    /** 只有接收者可以把私信标记为已读。 */
    @Update("""
            UPDATE private_message
            SET message_status = 2, updated_time = CURRENT_TIMESTAMP
            WHERE id = #{messageId} AND receiver_user_id = #{receiverUserId}
              AND message_status = 1 AND is_deleted = 0
            """)
    int markRead(@Param("messageId") Long messageId, @Param("receiverUserId") Long receiverUserId);
}
