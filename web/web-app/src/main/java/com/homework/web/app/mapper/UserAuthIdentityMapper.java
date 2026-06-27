package com.homework.web.app.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.homework.model.entity.UserAuthIdentity;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface UserAuthIdentityMapper extends BaseMapper<UserAuthIdentity> {
}
