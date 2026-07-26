package com.homework.web.admin.controller;

import com.homework.common.result.Result;
import com.homework.model.enums.GroupType;
import com.homework.web.admin.auth.AdminPermission;
import com.homework.web.admin.service.AdminCategoryService;
import com.homework.web.admin.vo.CategoryGroupTreeVO;
import io.swagger.v3.oas.annotations.Operation;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/** 后台只读分类树接口。 */
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/admin/categories")
public class AdminCategoryController {

    private final AdminCategoryService categoryService;

    /** 查询题库创建和筛选所需的三级分类树。 */
    @Operation(summary = "查询三级分类树")
    @AdminPermission("bank:view")
    @GetMapping("/tree")
    public Result<List<CategoryGroupTreeVO>> getTree(
            @RequestParam(required = false) GroupType groupType
    ) {
        return Result.success(categoryService.getTree(groupType));
    }
}
