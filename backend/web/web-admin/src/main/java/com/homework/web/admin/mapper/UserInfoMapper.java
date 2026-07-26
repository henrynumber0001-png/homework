package com.homework.web.admin.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.homework.model.entity.UserInfo;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Update;

/** App 用户数据访问接口。 */
public interface UserInfoMapper extends BaseMapper<UserInfo> {

    /** 仅递增用户版本，用于社区权限操作的并发控制。 */
    @Update("""
            UPDATE user_info
            SET version = version + 1, updated_time = CURRENT_TIMESTAMP(3)
            WHERE id = #{userId} AND is_deleted = 0 AND version = #{version}
            """)
    int bumpVersion(@Param("userId") Long userId, @Param("version") Integer version);
}
