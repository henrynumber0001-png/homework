package com.homework.web.app.service;

import com.homework.web.app.vo.ProfileOverviewVO;

import java.util.List;
import java.util.Map;

public interface ProfileService {
    ProfileOverviewVO getOverview(Long userId);
    List<Map<String, Object>> listWrongQuestions(Long userId);
    List<Map<String, Object>> listFavoriteBanks(Long userId);
    List<Map<String, Object>> listFavoriteQuestions(Long userId);
    List<Map<String, Object>> listNotes(Long userId);
}
