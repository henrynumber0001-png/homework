package com.homework.web.admin.mapper;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.homework.model.entity.QuestionBank;
import com.homework.model.enums.GroupType;
import com.homework.model.enums.QuestionBankStatus;
import com.homework.web.admin.vo.QuestionBankRowVO;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

/** 管理端题库数据访问接口。 */
public interface QuestionBankMapper extends BaseMapper<QuestionBank> {

    /** 根据题库分类链路读取题库所属一级类型。 */
    @Select("""
            SELECT cg.group_type
            FROM question_bank qb
            JOIN category_sub_module csm ON csm.id = qb.sub_module_id AND csm.is_deleted = 0
            JOIN category_module cm ON cm.id = csm.module_id AND cm.is_deleted = 0
            JOIN category_group cg ON cg.id = cm.group_id AND cg.is_deleted = 0
            WHERE qb.id = #{bankId} AND qb.is_deleted = 0
            """)
    GroupType selectGroupType(@Param("bankId") Long bankId);

    /** 根据目标子模块读取其所属的一级分类类型。 */
    @Select("""
            SELECT cg.group_type
            FROM category_sub_module csm
            JOIN category_module cm ON cm.id = csm.module_id AND cm.is_deleted = 0
            JOIN category_group cg ON cg.id = cm.group_id AND cg.is_deleted = 0
            WHERE csm.id = #{subModuleId} AND csm.is_deleted = 0
            """)
    GroupType selectGroupTypeBySubModuleId(@Param("subModuleId") Long subModuleId);

    /** 查询包含逻辑删除记录的题库。 */
    @Select("SELECT * FROM question_bank WHERE id = #{bankId}")
    QuestionBank selectIncludingDeleted(@Param("bankId") Long bankId);

    /** 串行化同一题库内的题目创建、删除和序号调整。 */
    @Select("SELECT * FROM question_bank WHERE id = #{bankId} AND is_deleted = 0 FOR UPDATE")
    QuestionBank selectForUpdate(@Param("bankId") Long bankId);

    /** 按版本逻辑删除已下架题库。 */
    @Update("""
            UPDATE question_bank
            SET is_deleted = 1, status = #{status}, delete_reason = #{reason}, version = version + 1,
                updated_time = CURRENT_TIMESTAMP(3)
            WHERE id = #{bankId} AND is_deleted = 0 AND version = #{version}
            """)
    int logicalDelete(
            @Param("bankId") Long bankId,
            @Param("status") QuestionBankStatus status,
            @Param("reason") String reason,
            @Param("version") Integer version
    );

    /** 按当前版本递增题库版本，用作题目顺序版本。 */
    @Update("""
            UPDATE question_bank
            SET version = version + 1, updated_time = CURRENT_TIMESTAMP(3)
            WHERE id = #{bankId} AND is_deleted = 0 AND version = #{version}
            """)
    int bumpVersion(@Param("bankId") Long bankId, @Param("version") Integer version);

}
