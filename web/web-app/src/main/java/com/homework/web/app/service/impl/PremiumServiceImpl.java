package com.homework.web.app.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.homework.common.enums.EnumUtils;
import com.homework.common.enums.PremiumOrderOrderStatus;
import com.homework.common.enums.PremiumOrderPremiumScope;
import com.homework.common.enums.PremiumOrderType;
import com.homework.web.app.dto.PremiumOrderCreateDTO;
import com.homework.model.entity.PremiumOrder;
import com.homework.model.entity.PremiumPlan;
import com.homework.web.app.mapper.PremiumOrderMapper;
import com.homework.web.app.mapper.PremiumPlanMapper;
import com.homework.web.app.service.PremiumService;
import com.homework.web.app.vo.MemberStatusVO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class PremiumServiceImpl implements PremiumService {

    private final PremiumPlanMapper premiumPlanMapper;
    private final PremiumOrderMapper premiumOrderMapper;

    @Override
    public List<Map<String, Object>> listPlans() {
        return premiumPlanMapper.selectMaps(new QueryWrapper<PremiumPlan>()
                .select("id", "plan_name", "premium_scope", "billing_cycle", "price", "duration_days", "benefits")
                .eq("status", 1)
                .eq("is_deleted", 0)
                .orderByAsc("premium_scope", "billing_cycle"));
    }

    @Override
    public MemberStatusVO getMemberStatus(Long userId) {
        PremiumOrder order = premiumOrderMapper.selectOne(new QueryWrapper<PremiumOrder>()
                .eq("user_id", userId)
                .eq("order_status", 2)
                .gt("expired_time", LocalDateTime.now())
                .eq("is_deleted", 0)
                .orderByDesc("expired_time")
                .last("LIMIT 1"));
        MemberStatusVO vo = new MemberStatusVO();
        vo.setPremium(order != null);
        if (order != null) {
            vo.setPremiumScope(order.getPremiumScope() == null ? null : order.getPremiumScope().getValue());
            vo.setType(order.getType() == null ? null : order.getType().getValue());
            vo.setStartTime(order.getStartTime());
            vo.setExpiredTime(order.getExpiredTime());
        }
        return vo;
    }

    @Override
    public Long createOrder(PremiumOrderCreateDTO dto) {
        PremiumOrder order = new PremiumOrder();
        order.setUserId(dto.getUserId());
        order.setPlanId(dto.getPlanId());
        order.setPremiumScope(EnumUtils.fromValue(PremiumOrderPremiumScope.class, dto.getPremiumScope()));
        order.setType(EnumUtils.fromValue(PremiumOrderType.class, dto.getType()));
        order.setOrderNo("HW" + UUID.randomUUID().toString().replace("-", ""));
        order.setStartTime(LocalDateTime.now());
        order.setExpiredTime(LocalDateTime.now().plusDays(dto.getType() != null && dto.getType() == 2 ? 365 : 30));
        order.setOrderStatus(PremiumOrderOrderStatus.PENDING);
        premiumOrderMapper.insert(order);
        return order.getId();
    }
}
