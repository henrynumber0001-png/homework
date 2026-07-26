package com.homework.web.admin.controller;

import com.homework.common.result.Result;
import com.homework.web.admin.auth.AdminPermission;
import com.homework.web.admin.service.AdminDashboardService;
import com.homework.web.admin.vo.DashboardVO;
import io.swagger.v3.oas.annotations.Operation;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;

/** 后台单页数据概览接口。 */
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/admin/dashboard")
public class AdminDashboardController {

    private final AdminDashboardService dashboardService;

    /** 查询指定日期的题库、用户、社区和会员概览。 */
    @Operation(summary = "查询后台数据概览")
    @AdminPermission("dashboard:view")
    @GetMapping
    public Result<DashboardVO> get(
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date
    ) {
        return Result.success(dashboardService.get(date));
    }
}
