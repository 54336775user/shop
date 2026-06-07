package com.eample.shop.controller;

import com.eample.shop.Service.CategoryService;
import com.eample.shop.common.Result;
import com.eample.shop.dto.CategoryRequestDTO;
import com.eample.shop.entity.Category;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/admin/category")
@RequiredArgsConstructor
@Slf4j
public class AdminCategoryController {

    private final CategoryService categoryService;

    /** 分类列表 */
    @GetMapping("/list")
    public Result<List<Category>> list() {
        List<Category> list = categoryService.listAll();
        return Result.ok(list);
    }

    /** 分类详情 */
    @GetMapping("/{id}")
    public Result<Category> detail(@PathVariable Long id) {
        Category category = categoryService.getById(id);
        return Result.ok(category);
    }

    /** 新增分类 */
    @PostMapping("/add")
    public Result<Void> add(@RequestBody CategoryRequestDTO request) {
        log.info("新增分类：{}", request.getName());
        categoryService.add(request);
        return Result.ok("新增成功", null);
    }

    /** 修改分类 */
    @PutMapping("/update")
    public Result<Void> update(@RequestBody CategoryRequestDTO request) {
        log.info("修改分类：id={}, name={}", request.getId(), request.getName());
        categoryService.update(request);
        return Result.ok("修改成功", null);
    }

    /** 删除分类 */
    @DeleteMapping("/delete/{id}")
    public Result<Void> delete(@PathVariable Long id) {
        log.info("删除分类：{}", id);
        categoryService.delete(id);
        return Result.ok("删除成功", null);
    }
}
