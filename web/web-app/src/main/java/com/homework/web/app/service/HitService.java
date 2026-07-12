package com.homework.web.app.service;

import com.homework.web.app.dto.HitActionDTO;
import com.homework.web.app.dto.HitCommentCreateDTO;
import com.homework.web.app.dto.HitPostCreateDTO;
import com.homework.web.app.vo.HitCommentVO;
import com.homework.web.app.vo.HitPostVO;

import java.util.List;
import java.util.Map;

public interface HitService {

    List<HitPostVO> listHits(Integer pageNum, Integer pageSize);

    List<HitCommentVO> listComments(Long postId, Integer pageNum, Integer pageSize);

    Long publish(Long currentUserId, HitPostCreateDTO dto);

    Long comment(Long currentUserId, Long postId, HitCommentCreateDTO dto);

    Map<String, Object> action(Long currentUserId, Long postId, HitActionDTO dto);
}
