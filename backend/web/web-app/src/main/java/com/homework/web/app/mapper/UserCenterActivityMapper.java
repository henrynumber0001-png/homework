package com.homework.web.app.mapper;

import com.homework.web.app.vo.UserCenterActivityRowVO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface UserCenterActivityMapper {
    List<UserCenterActivityRowVO> listActivities(@Param("userId") Long userId,
                                                 @Param("tab") String tab,
                                                 @Param("offset") long offset,
                                                 @Param("limit") long limit);
}
