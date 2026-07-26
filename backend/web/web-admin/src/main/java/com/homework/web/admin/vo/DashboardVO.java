package com.homework.web.admin.vo;

import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;

/** 精简后台数据看板。 */
@Data
public class DashboardVO {

    /** 查询统计日期。 */
    private LocalDate statDate;

    /** 题库浏览量。 */
    private DashboardMetricVO bankViews;

    /** 题库完成人数。 */
    private DashboardMetricVO bankCompletedUsers;

    /** 登录用户数，无用户权限时为空。 */
    private DashboardMetricVO loginUsers;

    /** 注册用户数，无用户权限时为空。 */
    private DashboardMetricVO registeredUsers;

    /** 发帖用户数，无社区权限时为空。 */
    private DashboardMetricVO postingUsers;

    /** 付费用户数，无会员权限时为空。 */
    private DashboardPaidUsersVO paidUsers;

    /** 统计最近更新时间。 */
    private LocalDateTime updatedTime;
}
