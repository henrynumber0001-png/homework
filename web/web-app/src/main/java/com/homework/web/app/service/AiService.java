package com.homework.web.app.service;

import com.homework.web.app.dto.AiChatMessageDTO;
import com.homework.web.app.dto.CorrectionFeedbackDTO;

import java.util.Map;

public interface AiService {
    Map<String, Object> ask(AiChatMessageDTO dto);
    Long submitCorrection(CorrectionFeedbackDTO dto);
}
