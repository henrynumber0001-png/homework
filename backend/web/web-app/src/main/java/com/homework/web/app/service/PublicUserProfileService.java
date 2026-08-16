package com.homework.web.app.service;

import com.homework.model.enums.BlockStatus;
import com.homework.web.app.dto.BlockActionDTO;
import com.homework.web.app.vo.BlockResultVO;
import com.homework.web.app.vo.HitPostVO;
import com.homework.web.app.vo.PublicUserProfileVO;

import java.util.List;

public interface PublicUserProfileService {
    PublicUserProfileVO getProfile(Long currentUserId, Long profileUserId);
    List<HitPostVO> listPosts(Long currentUserId, Long profileUserId, Integer pageNum, Integer pageSize);

    BlockResultVO blockByCurrentUser(Long currentUserId, Long profileUserId, BlockActionDTO dto);
}
