package com.homework.web.app.mapper;

import com.homework.web.app.vo.UserCenterCountsVO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

@Mapper
public interface UserCenterMapper {

    @Select(
            """
SELECT 
    """
    )
    UserCenterCountsVO selectCounts(@Param("userId") Long userId);
}
