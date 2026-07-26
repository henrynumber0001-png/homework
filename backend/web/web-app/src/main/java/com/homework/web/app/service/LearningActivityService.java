package com.homework.web.app.service;

import com.homework.web.app.vo.LearningCalendarItemVO;

import java.util.List;

public interface LearningActivityService {
    void heartbeat(Long userId);

    List<LearningCalendarItemVO> getCalendar(Long userId, Integer year);
}
