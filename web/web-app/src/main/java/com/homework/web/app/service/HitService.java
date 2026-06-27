package com.homework.web.app.service;

import com.homework.web.app.dto.HitActionDTO;
import com.homework.web.app.dto.HitCommentCreateDTO;
import com.homework.web.app.dto.HitPostCreateDTO;

import java.util.List;
import java.util.Map;

public interface HitService {
    List<Map<String, Object>> listHits(Integer pageNum, Integer pageSize);
    Long publish(HitPostCreateDTO dto);
    Long comment(Long postId, HitCommentCreateDTO dto);
    Map<String, Object> action(Long postId, HitActionDTO dto);
}
