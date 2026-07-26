package com.homework.web.admin.service;

import com.homework.model.enums.AdminRole;
import com.homework.model.enums.BankDataScope;
import com.homework.web.admin.auth.AdminAccessService;
import com.homework.web.admin.context.AdminContext;
import com.homework.web.admin.mapper.DashboardQueryMapper;
import com.homework.web.admin.vo.DashboardMetricVO;
import com.homework.web.admin.vo.DashboardPaidUsersVO;
import com.homework.web.admin.vo.DashboardVO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

/** 汇总单页后台概览并按权限隐藏低频域指标。 */
@Service
@RequiredArgsConstructor
public class AdminDashboardService {

    private final DashboardQueryMapper dashboardMapper;
    private final AdminAccessService accessService;

    public DashboardVO get(LocalDate date) {
        LocalDate statDate = date == null ? LocalDate.now() : date;
        boolean assignedOnly = AdminContext.get().getRole() != AdminRole.SUPER_ADMIN
                && AdminContext.get().getBankDataScope() == BankDataScope.ASSIGNED_BANKS;
        List<Long> bankIds = assignedOnly
                ? accessService.listAssignedBankIds(AdminContext.getAdminId())
                : List.of();
        long dailyViews;
        long totalViews;
        long dailyCompleted;
        long totalCompleted;
        if (assignedOnly) {
            dailyViews = bankIds.stream()
                    .mapToLong(bankId -> dashboardMapper.sumDailyBankViewsByBank(statDate, bankId))
                    .sum();
            totalViews = bankIds.stream()
                    .map(dashboardMapper::selectTotalBankViewsByBank)
                    .filter(java.util.Objects::nonNull)
                    .mapToLong(Long::longValue)
                    .sum();
            dailyCompleted = bankIds.stream()
                    .mapToLong(bankId -> dashboardMapper.sumDailyBankCompletedByBank(statDate, bankId))
                    .sum();
            totalCompleted = bankIds.stream()
                    .map(dashboardMapper::selectTotalBankCompletedByBank)
                    .filter(java.util.Objects::nonNull)
                    .mapToLong(Long::longValue)
                    .sum();
        } else {
            dailyViews = dashboardMapper.sumDailyBankViews(statDate);
            totalViews = dashboardMapper.sumTotalBankViews();
            dailyCompleted = dashboardMapper.sumDailyBankCompleted(statDate);
            totalCompleted = dashboardMapper.sumTotalBankCompleted();
        }
        DashboardVO vo = new DashboardVO();
        vo.setStatDate(statDate);
        DashboardMetricVO bankViews = new DashboardMetricVO();
        bankViews.setDaily(dailyViews);
        bankViews.setTotal(totalViews);
        vo.setBankViews(bankViews);
        DashboardMetricVO completed = new DashboardMetricVO();
        completed.setDaily(dailyCompleted);
        completed.setTotal(totalCompleted);
        vo.setBankCompletedUsers(completed);
        if (accessService.hasPermission("user:view")) {
            DashboardMetricVO login = new DashboardMetricVO();
            login.setDaily(dashboardMapper.selectDailyMetric(statDate, "login_user_count"));
            login.setTotal(dashboardMapper.selectTotalMetric("login_user_count"));
            vo.setLoginUsers(login);
            DashboardMetricVO registered = new DashboardMetricVO();
            registered.setDaily(dashboardMapper.selectDailyMetric(statDate, "register_user_count"));
            registered.setTotal(dashboardMapper.selectTotalMetric("register_user_count"));
            vo.setRegisteredUsers(registered);
        }
        if (accessService.hasPermission("community:moderate")) {
            DashboardMetricVO posting = new DashboardMetricVO();
            posting.setDaily(dashboardMapper.selectDailyMetric(statDate, "posting_user_count"));
            posting.setTotal(dashboardMapper.selectTotalMetric("posting_user_count"));
            vo.setPostingUsers(posting);
        }
        if (accessService.hasPermission("membership:view")) {
            DashboardPaidUsersVO paid = new DashboardPaidUsersVO();
            paid.setPremiumDaily(dashboardMapper.selectDailyMetric(statDate, "premium_paid_user_count"));
            paid.setPremiumTotal(dashboardMapper.selectTotalMetric("premium_paid_user_count"));
            paid.setPremiumPlusDaily(dashboardMapper.selectDailyMetric(statDate, "premium_plus_paid_user_count"));
            paid.setPremiumPlusTotal(dashboardMapper.selectTotalMetric("premium_plus_paid_user_count"));
            vo.setPaidUsers(paid);
        }
        LocalDateTime updatedTime = dashboardMapper.selectUpdatedTime(statDate);
        vo.setUpdatedTime(updatedTime == null ? LocalDateTime.now() : updatedTime);
        return vo;
    }
}
