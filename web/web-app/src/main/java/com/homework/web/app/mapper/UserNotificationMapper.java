package com.homework.web.app.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.homework.model.entity.UserNotification;
import com.homework.model.enums.UserNotificationType;
import org.apache.ibatis.annotations.*;

import java.time.LocalDateTime;
import java.util.Collection;

@Mapper
public interface UserNotificationMapper extends BaseMapper<UserNotification> {
    @Update("""
            <script>
            UPDATE user_notification
            SET read_status = 2, updated_time = CURRENT_TIMESTAMP(3)
            WHERE receiver_user_id = #{receiverUserId}
              AND read_status = 1 AND is_deleted = 0
              AND notification_type IN
              <foreach collection="types" item="type" open="(" separator="," close=")">#{type}</foreach>
            </script>
            """)
    int markTypesRead(@Param("receiverUserId") Long receiverUserId,
                      @Param("types") Collection<UserNotificationType> types);

    /**
     * 每次批量已读都会给同一批通知写入相同的 updated_time。
     * 最大的 updated_time 就代表这个 Tab 最近一次已读批次。
     */
    @Select("""
            <script>
            SELECT MAX(updated_time)
            FROM user_notification
            WHERE receiver_user_id = #{receiverUserId}
              AND read_status = 2
              AND is_deleted = 0
              AND notification_type IN
              <foreach collection="types" item="type"
                       open="(" separator="," close=")">
                  #{type}
              </foreach>
            </script>
            """)
    LocalDateTime selectLatestReadTime(
            @Param("receiverUserId") Long receiverUserId,
            @Param("types") Collection<UserNotificationType> types);
}
