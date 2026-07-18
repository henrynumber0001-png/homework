package com.homework.web.app.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.homework.common.result.PageResult;
import com.homework.model.entity.UserQuestionAnswer;
import com.homework.model.enums.GroupType;
import com.homework.web.app.vo.WrongQuestionBankVO;
import com.homework.web.app.vo.WrongQuestionVO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

@Mapper
public interface UserQuestionAnswerMapper extends BaseMapper<UserQuestionAnswer> {
    IPage<WrongQuestionBankVO> getWrongQuestionBanks(Page<WrongQuestionBankVO> page, @Param("groupType") GroupType groupType, @Param("userId") Long userId);



    IPage<WrongQuestionVO> getWrongQuestions(Page<WrongQuestionVO> page,@Param("userId") Long userId,@Param("bankId") Long bankId);

    GroupType getGroupType(@Param("bankId") Long bankId);
}
