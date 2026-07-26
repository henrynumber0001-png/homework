package com.homework.web.admin.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.homework.model.entity.CertificateQuestionInfo;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

/** 认证题数据访问接口。 */
public interface CertificateQuestionMapper extends BaseMapper<CertificateQuestionInfo> {

    /** 查询包含逻辑删除记录的认证题。 */
    @Select("SELECT * FROM certificate_question_info WHERE id = #{questionId}")
    CertificateQuestionInfo selectIncludingDeleted(@Param("questionId") Long questionId);

    /** 按版本逻辑删除认证题。 */
    @Update("""
            UPDATE certificate_question_info
            SET is_deleted = 1, is_released = 0, version = version + 1,
                updated_time = CURRENT_TIMESTAMP(3)
            WHERE id = #{questionId} AND is_deleted = 0 AND version = #{version}
            """)
    int logicalDelete(@Param("questionId") Long questionId, @Param("version") Integer version);

    /** 按版本恢复认证题并保持未发布。 */
    @Update("""
            UPDATE certificate_question_info
            SET is_deleted = 0, is_released = 0, version = version + 1,
                updated_time = CURRENT_TIMESTAMP(3)
            WHERE id = #{questionId} AND is_deleted = 1 AND version = #{version}
            """)
    int restore(@Param("questionId") Long questionId, @Param("version") Integer version);
}
