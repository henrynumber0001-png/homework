package com.homework.web.app.controller;

import com.homework.common.result.Result;
import com.homework.web.app.service.CategoryService;
import com.homework.web.app.vo.CategoryDetailVO;
import com.homework.web.app.vo.CategoryModuleVO;
import com.homework.web.app.vo.CategorySubModuleVO;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/app/categories")
@RequiredArgsConstructor
public class CategoryController {

    private final CategoryService categoryService;

    @GetMapping("/modules")
    public Result<List<CategoryModuleVO>> modules(@RequestParam(required = false) Long groupId) {
        return Result.success(categoryService.listModules(groupId));
    }

    @GetMapping("/modules/{moduleId}/sub-modules")
    public Result<List<CategorySubModuleVO>> subModules(@PathVariable Long moduleId) {
        return Result.success(categoryService.listSubModules(moduleId));
    }

    @GetMapping("/sub-modules/{subModuleId}/details")
    public Result<List<CategoryDetailVO>> details(@PathVariable Long subModuleId) {
        return Result.success(categoryService.listDetails(subModuleId));
    }
}
