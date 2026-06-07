package com.eample.shop.controller;

import com.eample.shop.Service.ProductService;
import com.eample.shop.common.PageResult;
import com.eample.shop.common.Result;
import com.eample.shop.dto.ProductRequestDTO;
import com.eample.shop.entity.Product;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/admin/product")
@RequiredArgsConstructor
@Slf4j
public class AdminProductController {

    private final ProductService productService;

    @GetMapping("/list")
    public Result<PageResult<Product>> list(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) Long categoryId,
            @RequestParam(required = false) Integer status,
            @RequestParam(required = false) Boolean flashSale,
            @RequestParam(required = false, defaultValue = "1") Integer page,
            @RequestParam(required = false, defaultValue = "10") Integer size) {
        return Result.ok(productService.adminSearch(keyword, categoryId, status, flashSale, page, size));
    }

    @GetMapping("/{id}")
    public Result<Product> detail(@PathVariable Long id) {
        return Result.ok(productService.getById(id));
    }

    @PostMapping("/add")
    public Result<Void> add(@RequestBody ProductRequestDTO request) {
        log.info("新增商品：{}", request.getName());
        productService.add(request);
        return Result.ok("新增成功", null);
    }

    @PutMapping("/update")
    public Result<Void> update(@RequestBody ProductRequestDTO request) {
        log.info("修改商品：id={}, name={}", request.getId(), request.getName());
        productService.update(request);
        return Result.ok("修改成功", null);
    }

    @PutMapping("/on-shelf/{id}")
    public Result<Void> onShelf(@PathVariable Long id) {
        log.info("商品上架：{}", id);
        productService.onShelf(id);
        return Result.ok("上架成功", null);
    }

    @PutMapping("/off-shelf/{id}")
    public Result<Void> offShelf(@PathVariable Long id) {
        log.info("商品下架：{}", id);
        productService.offShelf(id);
        return Result.ok("下架成功", null);
    }

    @DeleteMapping("/delete/{id}")
    public Result<Void> delete(@PathVariable Long id) {
        log.info("删除商品：{}", id);
        productService.delete(id);
        return Result.ok("删除成功", null);
    }
}
