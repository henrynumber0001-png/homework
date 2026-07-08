package com.homework.web.app.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.homework.model.entity.AiChatSession;
import org.apache.ibatis.annotations.Mapper;

/**
 * AI 追问会话表 mapper。
 * 一条会话代表“某个用户在某个题库里的答案解析追问窗口”。
 */
@Mapper
public interface AiChatSessionMapper extends BaseMapper<AiChatSession> {
}
