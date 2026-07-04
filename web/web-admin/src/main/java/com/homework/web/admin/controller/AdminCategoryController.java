package com.homework.web.admin.controller;

import com.homework.common.result.Result;
import com.homework.web.app.dto.CategorySubModuleSaveDTO;
import com.homework.web.app.service.CategoryService;
import com.homework.web.app.vo.CategoryModuleVO;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/admin/categories")
@RequiredArgsConstructor
public class AdminCategoryController {

    private final CategoryService categoryService;

    @GetMapping("/modules")
    public Result<List<CategoryModuleVO>> modules(@RequestParam(required = false) Long groupId) {
        return Result.success(categoryService.listModules(groupId));
    }

    @GetMapping("/modules/{moduleId}/sub-modules")
    public Result<List<CategorySubModuleVO>> subModules(@PathVariable Long moduleId) {
        return Result.success(categoryService.listSubModules(moduleId));
    }

    @PostMapping("/sub-modules")
    public Result<Long> saveSubModule(@RequestBody CategorySubModuleSaveDTO dto) {
        return Result.success(categoryService.saveSubModule(dto));
    }

    @DeleteMapping("/sub-modules/{id}")
    public Result<Void> deleteSubModule(@PathVariable Long id) {
        categoryService.deleteSubModule(id);
        return Result.success();
    }
}
