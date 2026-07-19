package com.homework.web.app.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.homework.model.entity.UserQuestionNote;
import com.homework.model.enums.GroupType;
import com.homework.web.app.vo.NoteBankVO;
import com.homework.web.app.vo.NoteQuestionVO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface UserQuestionNoteMapper extends BaseMapper<UserQuestionNote> {
    IPage<NoteBankVO> getNoteBanks(Page<NoteBankVO> page, @Param("groupType") GroupType groupType,@Param("userId") Long userId);

    IPage<NoteQuestionVO> getNoteQuestions(Page<NoteQuestionVO> page,@Param("userId") Long userId,@Param("bankId") Long bankId);
}
