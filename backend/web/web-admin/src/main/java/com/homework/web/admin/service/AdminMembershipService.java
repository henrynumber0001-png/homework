package com.homework.web.admin.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.homework.common.exception.HomeworkException;
import com.homework.common.result.PageResult;
import com.homework.common.result.ResultCodeEnum;
import com.homework.model.entity.BaseVipRecord;
import com.homework.model.entity.MembershipAccessSuspension;
import com.homework.model.entity.MembershipChangeRecord;
import com.homework.model.entity.MembershipOrder;
import com.homework.model.entity.MembershipPlan;
import com.homework.model.entity.SvipRecord;
import com.homework.model.entity.UserInfo;
import com.homework.model.enums.BillingType;
import com.homework.model.enums.MembershipChangeType;
import com.homework.model.enums.MembershipOrderStatus;
import com.homework.model.enums.MembershipPurchaseType;
import com.homework.model.enums.MembershipStatus;
import com.homework.model.enums.MembershipType;
import com.homework.web.admin.context.AdminContext;
import com.homework.web.admin.dto.MembershipActionDTO;
import com.homework.model.enums.MembershipAction;
import com.homework.web.admin.dto.MembershipPlanCreateDTO;
import com.homework.web.admin.dto.MembershipPlanUpdateDTO;
import com.homework.web.admin.mapper.BaseVipRecordMapper;
import com.homework.web.admin.mapper.MembershipAccessSuspensionMapper;
import com.homework.web.admin.mapper.MembershipChangeRecordMapper;
import com.homework.web.admin.mapper.MembershipOrderMapper;
import com.homework.web.admin.mapper.MembershipPlanMapper;
import com.homework.web.admin.mapper.SvipRecordMapper;
import com.homework.web.admin.mapper.UserInfoMapper;
import com.homework.web.admin.vo.MembershipDetailVO;
import com.homework.web.admin.vo.MembershipOrderVO;
import com.homework.web.admin.vo.MembershipPlanVO;
import com.homework.web.admin.vo.MembershipRowVO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;

/** 后台会员发放、暂停、回收、订单查询和套餐配置。 */
@Service
@RequiredArgsConstructor
public class AdminMembershipService {

    private final UserInfoMapper userMapper;
    private final BaseVipRecordMapper baseVipMapper;
    private final SvipRecordMapper svipMapper;
    private final MembershipAccessSuspensionMapper suspensionMapper;
    private final MembershipChangeRecordMapper changeMapper;
    private final MembershipOrderMapper orderMapper;
    private final MembershipPlanMapper planMapper;
    private final MembershipAssembler assembler;
    private final MembershipSnapshotService snapshotService;
    private final AdminAuditService auditService;

    public PageResult<MembershipRowVO> list(
            String keyword,
            MembershipStatus membershipType,
            Integer pageNum,
            Integer pageSize
    ) {
        List<MembershipRowVO> memberships = userMapper.selectList(
                        new LambdaQueryWrapper<UserInfo>().orderByDesc(UserInfo::getCreatedTime))
                .stream()
                .map(user -> assembler.toRow(user.getId()))
                .filter(row -> keyword == null || keyword.isBlank()
                        || row.getAccountNo().toLowerCase(Locale.ROOT)
                        .contains(keyword.trim().toLowerCase(Locale.ROOT))
                        || row.getDisplayName().toLowerCase(Locale.ROOT)
                        .contains(keyword.trim().toLowerCase(Locale.ROOT)))
                .filter(row -> membershipType == null || membershipType == row.getCurrentType())
                .sorted(Comparator.comparing(MembershipRowVO::getUserId).reversed())
                .toList();
        int normalizedPage = pageNum == null || pageNum < 1 ? 1 : pageNum;
        int normalizedSize = pageSize == null ? 20 : Math.min(Math.max(pageSize, 1), 100);
        int from = Math.min((normalizedPage - 1) * normalizedSize, memberships.size());
        int to = Math.min(from + normalizedSize, memberships.size());
        PageResult<MembershipRowVO> result = new PageResult<>();
        result.setRecords(memberships.subList(from, to));
        result.setTotal(memberships.size());
        result.setPageNum(normalizedPage);
        result.setPageSize(normalizedSize);
        return result;
    }

    public MembershipDetailVO get(Long userId) {
        return assembler.toDetail(userId);
    }

    @Transactional
    public MembershipDetailVO action(Long userId, MembershipActionDTO dto) {
        UserInfo user = userMapper.selectById(userId);
        if (user == null) {
            throw new HomeworkException(ResultCodeEnum.ADMIN_USER_STATE_INVALID);
        }
        BaseVipRecord baseVip = assembler.findBaseVip(userId);
        SvipRecord svip = assembler.findSvip(userId);
        MembershipAccessSuspension suspension = assembler.findActiveSuspension(userId);
        int currentLedgerVersion = assembler.getLedgerVersion(userId, baseVip, svip);
        if (currentLedgerVersion != dto.getLedgerVersion()) {
            throw new HomeworkException(ResultCodeEnum.ADMIN_MEMBERSHIP_LEDGER_CONFLICT);
        }
        String beforeSnapshot = snapshotService.create(baseVip, svip, suspension != null);
        MembershipAction action = dto.getAction();
        MembershipChangeType changeType;
        MembershipType membershipType = null;
        Integer durationMonths = null;
        LocalDateTime now = LocalDateTime.now();

        if (action == MembershipAction.GRANT) {
            membershipType = dto.getMembershipType();
            if (membershipType == null) {
                throw new HomeworkException(ResultCodeEnum.PARAM_ERROR);
            }
            if (dto.getDurationMonths() == null
                    || dto.getDurationMonths() < 1 || dto.getDurationMonths() > 120) {
                throw new HomeworkException(ResultCodeEnum.PARAM_ERROR);
            }
            durationMonths = dto.getDurationMonths();
            if (membershipType == MembershipType.PREMIUM) {
                LocalDateTime start = now;
                if (baseVip != null && baseVip.getExpireTime() != null && baseVip.getExpireTime().isAfter(start)) {
                    start = baseVip.getExpireTime();
                }
                if (svip != null && svip.getExpireTime() != null && svip.getExpireTime().isAfter(start)) {
                    start = svip.getExpireTime();
                }
                if (baseVip == null) {
                    baseVip = new BaseVipRecord();
                    baseVip.setUserId(userId);
                    baseVip.setExpireTime(start.plusMonths(durationMonths));
                    baseVip.setVersion(0);
                    baseVipMapper.insert(baseVip);
                } else {
                    baseVip.setExpireTime(start.plusMonths(durationMonths));
                    if (baseVipMapper.updateById(baseVip) == 0) {
                        throw new HomeworkException(ResultCodeEnum.ADMIN_MEMBERSHIP_LEDGER_CONFLICT);
                    }
                }
            } else {
                LocalDateTime plusStart = now;
                if (svip != null && svip.getExpireTime() != null && svip.getExpireTime().isAfter(plusStart)) {
                    plusStart = svip.getExpireTime();
                }
                Duration remainingPremium = null;
                if (baseVip != null && baseVip.getExpireTime() != null
                        && baseVip.getExpireTime().isAfter(plusStart)) {
                    remainingPremium = Duration.between(plusStart, baseVip.getExpireTime());
                }
                LocalDateTime newPlusExpireTime = plusStart.plusMonths(durationMonths);
                if (svip == null) {
                    svip = new SvipRecord();
                    svip.setUserId(userId);
                    svip.setExpireTime(newPlusExpireTime);
                    svip.setVersion(0);
                    svipMapper.insert(svip);
                } else {
                    svip.setExpireTime(newPlusExpireTime);
                    if (svipMapper.updateById(svip) == 0) {
                        throw new HomeworkException(ResultCodeEnum.ADMIN_MEMBERSHIP_LEDGER_CONFLICT);
                    }
                }
                if (remainingPremium != null) {
                    baseVip.setExpireTime(newPlusExpireTime.plus(remainingPremium));
                    if (baseVipMapper.updateById(baseVip) == 0) {
                        throw new HomeworkException(ResultCodeEnum.ADMIN_MEMBERSHIP_LEDGER_CONFLICT);
                    }
                }
            }
            changeType = MembershipChangeType.ADMIN_GRANT;
        } else if (action == MembershipAction.SUSPEND) {
            boolean hasActiveMembership = svip != null && svip.getExpireTime() != null && svip.getExpireTime().isAfter(now)
                    || baseVip != null && baseVip.getExpireTime() != null && baseVip.getExpireTime().isAfter(now);
            if (!hasActiveMembership || suspension != null) {
                throw new HomeworkException(ResultCodeEnum.ADMIN_MEMBERSHIP_STATE_INVALID);
            }
            suspension = new MembershipAccessSuspension();
            suspension.setUserId(userId);
            suspension.setReason(dto.getReason());
            suspension.setAdminId(AdminContext.getAdminId());
            suspension.setSuspendedTime(now);
            suspensionMapper.insert(suspension);
            changeType = MembershipChangeType.ADMIN_SUSPEND;
        } else if (action == MembershipAction.RESUME) {
            if (suspension == null) {
                throw new HomeworkException(ResultCodeEnum.ADMIN_MEMBERSHIP_STATE_INVALID);
            }
            suspension.setResumedTime(now);
            suspension.setResumedByAdminId(AdminContext.getAdminId());
            suspensionMapper.updateById(suspension);
            changeType = MembershipChangeType.ADMIN_RESUME;
        } else if (action == MembershipAction.REVOKE) {
            boolean changed = false;
            if (baseVip != null && baseVip.getExpireTime() != null && baseVip.getExpireTime().isAfter(now)) {
                baseVip.setExpireTime(now);
                changed = baseVipMapper.updateById(baseVip) > 0;
            }
            if (svip != null && svip.getExpireTime() != null && svip.getExpireTime().isAfter(now)) {
                svip.setExpireTime(now);
                changed = svipMapper.updateById(svip) > 0 || changed;
            }
            if (!changed) {
                throw new HomeworkException(ResultCodeEnum.ADMIN_MEMBERSHIP_STATE_INVALID);
            }
            if (suspension != null) {
                suspension.setResumedTime(now);
                suspension.setResumedByAdminId(AdminContext.getAdminId());
                suspensionMapper.updateById(suspension);
            }
            changeType = MembershipChangeType.ADMIN_REVOKE;
        } else {
            throw new HomeworkException(ResultCodeEnum.PARAM_ERROR);
        }

        BaseVipRecord updatedBaseVip = assembler.findBaseVip(userId);
        SvipRecord updatedSvip = assembler.findSvip(userId);
        MembershipAccessSuspension updatedSuspension = assembler.findActiveSuspension(userId);
        MembershipChangeRecord change = new MembershipChangeRecord();
        change.setUserId(userId);
        change.setChangeType(changeType);
        change.setMembershipType(membershipType);
        change.setDurationMonths(durationMonths);
        change.setBeforeSnapshot(beforeSnapshot);
        change.setAfterSnapshot(snapshotService.create(
                updatedBaseVip,
                updatedSvip,
                updatedSuspension != null
        ));
        change.setReason(dto.getReason().trim());
        change.setAdminId(AdminContext.getAdminId());
        changeMapper.insert(change);
        auditService.record("MEMBERSHIP", action.name(), "USER_MEMBERSHIP", userId, dto.getReason(), beforeSnapshot, change);
        return assembler.toDetail(userId);
    }

    public PageResult<MembershipOrderVO> listOrders(
            String keyword,
            Long userId,
            MembershipOrderStatus orderStatus,
            Integer pageNum,
            Integer pageSize
    ) {
        int normalizedPage = pageNum == null || pageNum < 1 ? 1 : pageNum;
        int normalizedSize = pageSize == null ? 20 : Math.min(Math.max(pageSize, 1), 100);
        LambdaQueryWrapper<MembershipOrder> query = new LambdaQueryWrapper<>();
        query.like(keyword != null && !keyword.isBlank(),
                        MembershipOrder::getOrderNo,
                        keyword == null ? null : keyword.trim())
                .eq(userId != null, MembershipOrder::getUserId, userId);
        query.eq(orderStatus != null, MembershipOrder::getOrderStatus, orderStatus);
        query.orderByDesc(MembershipOrder::getCreatedTime).orderByDesc(MembershipOrder::getId);
        Page<MembershipOrder> page = orderMapper.selectPage(new Page<>(normalizedPage, normalizedSize), query);
        PageResult<MembershipOrderVO> result = new PageResult<>();
        result.setRecords(page.getRecords().stream().map(order -> {
            MembershipOrderVO vo = new MembershipOrderVO();
            vo.setOrderNo(order.getOrderNo());
            vo.setUserId(order.getUserId());
            vo.setMembershipType(order.getMembershipType());
            vo.setDurationMonths(order.getDurationMonths());
            vo.setPayAmount(order.getPayAmount());
            vo.setCurrency(order.getCurrency());
            vo.setOrderStatus(order.getOrderStatus());
            vo.setPayTime(order.getPayTime());
            vo.setRefundable(false);
            return vo;
        }).toList());
        result.setTotal(page.getTotal());
        result.setPageNum(page.getCurrent());
        result.setPageSize(page.getSize());
        return result;
    }

    public List<MembershipPlanVO> listPlans() {
        return planMapper.selectList(new LambdaQueryWrapper<MembershipPlan>()
                        .orderByAsc(MembershipPlan::getMembershipType)
                        .orderByAsc(MembershipPlan::getDurationMonths)
                        .orderByAsc(MembershipPlan::getId))
                .stream()
                .map(this::toPlanVO)
                .toList();
    }

    @Transactional
    public MembershipPlanVO createPlan(MembershipPlanCreateDTO dto) {
        MembershipType membershipType = dto.getMembershipType();
        MembershipPurchaseType purchaseType = dto.getPurchaseType();
        BillingType billingType = dto.getBillingType();
        if (dto.getDurationMonths() < 1 || dto.getDurationMonths() > 12
                || purchaseType == MembershipPurchaseType.FULL && billingType == null
                || purchaseType == MembershipPurchaseType.DIFF && billingType != null) {
            throw new HomeworkException(ResultCodeEnum.PARAM_ERROR);
        }
        MembershipPlan plan = new MembershipPlan();
        plan.setMembershipType(membershipType);
        plan.setPurchaseType(purchaseType);
        plan.setDurationMonths(dto.getDurationMonths());
        plan.setBillingType(billingType);
        plan.setPrice(dto.getPrice());
        plan.setCurrency(dto.getCurrency().trim().toUpperCase(Locale.ROOT));
        plan.setEnabled(dto.getEnabled());
        plan.setVersion(0);
        planMapper.insert(plan);
        auditService.record("MEMBERSHIP", "CREATE_PLAN", "MEMBERSHIP_PLAN", plan.getId(), dto.getReason(), null, plan);
        return toPlanVO(planMapper.selectById(plan.getId()));
    }

    @Transactional
    public MembershipPlanVO updatePlan(Long planId, MembershipPlanUpdateDTO dto) {
        MembershipPlan plan = planMapper.selectById(planId);
        if (plan == null) {
            throw new HomeworkException(ResultCodeEnum.ADMIN_MEMBERSHIP_STATE_INVALID);
        }
        if (!plan.getVersion().equals(dto.getVersion())) {
            throw new HomeworkException(ResultCodeEnum.ADMIN_RESOURCE_VERSION_CONFLICT);
        }
        MembershipPlan before = new MembershipPlan();
        org.springframework.beans.BeanUtils.copyProperties(plan, before);
        plan.setPrice(dto.getPrice());
        plan.setEnabled(dto.getEnabled());
        if (planMapper.updateById(plan) == 0) {
            throw new HomeworkException(ResultCodeEnum.ADMIN_RESOURCE_VERSION_CONFLICT);
        }
        MembershipPlan updated = planMapper.selectById(planId);
        auditService.record("MEMBERSHIP", "UPDATE_PLAN", "MEMBERSHIP_PLAN", planId, dto.getReason(), before, updated);
        return toPlanVO(updated);
    }

    public MembershipPlanVO toPlanVO(MembershipPlan plan) {
        MembershipPlanVO vo = new MembershipPlanVO();
        vo.setId(plan.getId());
        vo.setMembershipType(plan.getMembershipType());
        vo.setPurchaseType(plan.getPurchaseType());
        vo.setDurationMonths(plan.getDurationMonths());
        vo.setBillingType(plan.getBillingType());
        vo.setPrice(plan.getPrice());
        vo.setCurrency(plan.getCurrency());
        vo.setEnabled(plan.getEnabled());
        vo.setVersion(plan.getVersion());
        return vo;
    }
}
