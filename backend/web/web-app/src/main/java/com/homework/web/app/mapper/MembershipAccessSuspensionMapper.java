package com.homework.web.app.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.homework.model.entity.MembershipAccessSuspension;
import org.apache.ibatis.annotations.Mapper;

/** App 端读取管理员会员暂停记录。 */
@Mapper
public interface MembershipAccessSuspensionMapper extends BaseMapper<MembershipAccessSuspension> {
}
