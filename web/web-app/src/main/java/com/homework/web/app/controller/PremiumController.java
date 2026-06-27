package com.homework.web.app.controller;

import com.homework.common.result.Result;
import com.homework.web.app.dto.PremiumOrderCreateDTO;
import com.homework.web.app.service.PremiumService;
import com.homework.web.app.vo.MemberStatusVO;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/app/premium")
@RequiredArgsConstructor
public class PremiumController {

    private final PremiumService premiumService;

    @GetMapping("/plans")
    public Result<List<Map<String, Object>>> plans() {
        return Result.success(premiumService.listPlans());
    }

    @GetMapping("/status")
    public Result<MemberStatusVO> status(@RequestParam Long userId) {
        return Result.success(premiumService.getMemberStatus(userId));
    }

    @PostMapping("/orders")
    public Result<Long> createOrder(@RequestBody PremiumOrderCreateDTO dto) {
        return Result.success(premiumService.createOrder(dto));
    }
}
