package com.homework.web.app.service;

import com.homework.web.app.dto.HitActionDTO;
import com.homework.web.app.dto.HitCommentCreateDTO;
import com.homework.web.app.dto.HitCommentLikeDTO;
import com.homework.web.app.dto.HitPostCreateDTO;
import com.homework.web.app.vo.HitCommentVO;
import com.homework.web.app.vo.HitCommentLikeResultVO;
import com.homework.web.app.vo.HitActionResultVO;
import com.homework.web.app.vo.HitPostVO;

import java.util.List;

public interface HitService {

    List<HitPostVO> listHits(Integer pageNum, Integer pageSize);

    List<HitCommentVO> listComments(Long postId, Integer pageNum, Integer pageSize);

    Long publish(HitPostCreateDTO dto);

    Long comment(Long postId, HitCommentCreateDTO dto);

    HitActionResultVO action(Long postId, HitActionDTO dto);

    HitCommentLikeResultVO commentLike(Long postId, Long commentId, HitCommentLikeDTO dto);
}
