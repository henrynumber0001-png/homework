package com.homework.web.app.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.homework.model.entity.UserCommunityRestriction;
import org.apache.ibatis.annotations.Mapper;

/** App 端读取管理员设置的社区权限限制。 */
@Mapper
public interface UserCommunityRestrictionMapper extends BaseMapper<UserCommunityRestriction> {
}
