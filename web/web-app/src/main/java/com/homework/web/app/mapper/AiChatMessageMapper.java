package com.homework.web.app.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.homework.model.entity.AiChatMessage;
import org.apache.ibatis.annotations.Mapper;

/**
 * AI 追问消息表 mapper。
 * 会话中的用户追问和 AI 回复都会保存到这张表。
 */
@Mapper
public interface AiChatMessageMapper extends BaseMapper<AiChatMessage> {
}
