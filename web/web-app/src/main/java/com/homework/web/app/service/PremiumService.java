package com.homework.web.app.service;

import com.homework.web.app.dto.PremiumOrderCreateDTO;
import com.homework.web.app.vo.MemberStatusVO;

import java.util.List;
import java.util.Map;

public interface PremiumService {
    List<Map<String, Object>> listPlans();
    MemberStatusVO getMemberStatus(Long userId);
    Long createOrder(PremiumOrderCreateDTO dto);
}
