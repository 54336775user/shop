package com.eample.shop.controller;

import com.eample.shop.Service.CategoryService;
import com.eample.shop.common.Result;
import com.eample.shop.entity.Category;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/category")
@RequiredArgsConstructor
public class CategoryController {

    private final CategoryService categoryService;

    /** 前台：启用中的分类列表 */
    @GetMapping("/list")
    public Result<List<Category>> listEnabled() {
        return Result.ok(categoryService.listEnabled());
    }
}