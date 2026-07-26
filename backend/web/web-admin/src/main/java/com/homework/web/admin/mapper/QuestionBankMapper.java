package com.homework.web.admin.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.homework.model.entity.QuestionBank;
import com.homework.model.enums.GroupType;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.util.List;

/** 管理端题库数据访问接口。 */
public interface QuestionBankMapper extends BaseMapper<QuestionBank> {

    /** 根据题库分类链路读取题库所属一级类型。 */
    @Select("""
            SELECT cg.group_type
            FROM question_bank qb
            JOIN category_sub_module csm ON csm.id = qb.sub_module_id AND csm.is_deleted = 0
            JOIN category_module cm ON cm.id = csm.module_id AND cm.is_deleted = 0
            JOIN category_group cg ON cg.id = cm.group_id AND cg.is_deleted = 0
            WHERE qb.id = #{bankId}
            """)
    GroupType selectGroupType(@Param("bankId") Long bankId);

    /** 查询包含逻辑删除记录的题库。 */
    @Select("SELECT * FROM question_bank WHERE id = #{bankId}")
    QuestionBank selectIncludingDeleted(@Param("bankId") Long bankId);

    /** 查询题库回收站记录。 */
    @Select("SELECT * FROM question_bank WHERE is_deleted = 1 ORDER BY updated_time DESC, id DESC")
    List<QuestionBank> selectDeletedList();

    /** 按版本逻辑删除已下架题库。 */
    @Update("""
            UPDATE question_bank
            SET is_deleted = 1, delete_reason = #{reason}, version = version + 1,
                updated_time = CURRENT_TIMESTAMP(3)
            WHERE id = #{bankId} AND is_deleted = 0 AND version = #{version}
            """)
    int logicalDelete(
            @Param("bankId") Long bankId,
            @Param("reason") String reason,
            @Param("version") Integer version
    );

    /** 按版本恢复题库并保持下架状态。 */
    @Update("""
            UPDATE question_bank
            SET is_deleted = 0, status = 3, delete_reason = NULL, version = version + 1,
                updated_time = CURRENT_TIMESTAMP(3)
            WHERE id = #{bankId} AND is_deleted = 1 AND version = #{version}
            """)
    int restore(
            @Param("bankId") Long bankId,
            @Param("version") Integer version
    );

    /** 按当前版本递增题库版本，用作题目顺序版本。 */
    @Update("""
            UPDATE question_bank
            SET version = version + 1, updated_time = CURRENT_TIMESTAMP(3)
            WHERE id = #{bankId} AND is_deleted = 0 AND version = #{version}
            """)
    int bumpVersion(
            @Param("bankId") Long bankId,
            @Param("version") Integer version
    );
}
