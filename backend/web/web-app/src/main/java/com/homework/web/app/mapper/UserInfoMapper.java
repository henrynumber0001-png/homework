package com.homework.web.app.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.homework.model.entity.UserInfo;
import com.homework.web.app.dto.EditProfileDTO;
import com.homework.web.app.vo.EditedProfileVO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Update;

@Mapper
public interface UserInfoMapper extends BaseMapper<UserInfo> {


    @Update("""
             UPDATE user_info
             SET display_name = #{vo.displayName},
                 sub_tech_direction_id = #{vo.subTechDirectionId},
                 company_or_school = #{vo.companyOrSchool},
                 gender = #{vo.gender},
                 introduction = #{vo.introduction},
                 version = version + 1,
                 updated_time = CURRENT_TIMESTAMP(3)
             WHERE id = #{userId}
             AND is_deleted = 0
             AND status = 1
             AND version = #{version}""")
    int updateProfile(@Param("vo") EditedProfileVO vo, @Param("userId") Long userId, @Param("version") Integer version);
}


//注意，#{version}一定要出现在筛选条件里，而不是直接放在更新条件里，否则乐观锁失去它作为校验工具的意义了
//你现在还只是在mapper层，所以传入vo.gender 是对的，不需要你自己手动传入gender.getCode()，那是 MybatisPlus 传入到 数据库时候 根据 @EnumValue 操作的