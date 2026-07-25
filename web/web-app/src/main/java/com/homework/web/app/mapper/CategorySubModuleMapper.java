package com.homework.web.app.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.homework.model.entity.CategorySubModule;
import com.homework.model.enums.GroupType;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface CategorySubModuleMapper extends BaseMapper<CategorySubModule> {



    @Select(
            """
            SELECT csm.id
            FROM category_sub_module csm 
            INNER JOIN category_module cm 
                ON csm.module_id = cm.id AND cm.is_deleted = 0
            INNER JOIN category_group cg 
            ON cg.id = cm.group_id AND cg.is_deleted = 0
            WHERE cg.group_type = #{groupType}
            AND csm.is_deleted = 0
            """
    )
    List<Long> selectByGroupType(GroupType groupType);
}
