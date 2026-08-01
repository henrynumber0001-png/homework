package com.homework.web.admin.controller;

import com.homework.common.result.Result;
import com.homework.web.admin.auth.AdminPermission;
import com.homework.web.admin.service.AdminCategoryService;
import com.homework.web.admin.vo.CategoryGroupTreeVO;
import io.swagger.v3.oas.annotations.Operation;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;


@RestController
@RequiredArgsConstructor
@RequestMapping("/api/admin/categories")
public class AdminCategoryController { //Group,Module,SubModule的全部信息，都从这个接口返回给前端

    private final AdminCategoryService categoryService;

    // 一次性返回全部查询用的分类树明细给前端，用于制作筛选用的下拉菜单
    @Operation(summary = "查询三级分类树")
    @AdminPermission("bank:view")
    @GetMapping("/tree")
    public Result<List<CategoryGroupTreeVO>> getTree() {
        return Result.success(categoryService.getTree());
    }
}
