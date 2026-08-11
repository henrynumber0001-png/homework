package com.homework.web.admin.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.homework.model.entity.BankTag;
import org.apache.ibatis.annotations.Delete;

/**
 * 题库标签数据访问接口。
 */
public interface BankTagMapper extends BaseMapper<BankTag> {

    @Delete(
            """
                    DELETE FROM bank_tag 
                    WHERE bank_id = #{bankId}"""
    )
    void deleteAllTags(Long bankId);
}
