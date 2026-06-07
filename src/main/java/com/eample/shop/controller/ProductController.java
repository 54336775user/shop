package com.eample.shop.controller;

import com.eample.shop.Service.ProductService;
import com.eample.shop.common.Result;
import com.eample.shop.entity.Product;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/product")
@RequiredArgsConstructor
@Slf4j
public class ProductController {

    private final ProductService productService;

    @GetMapping("/list")
    public Result<List<Product>> list(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) Long categoryId,
            @RequestParam(required = false, defaultValue = "false") Boolean flashSale,
            @RequestParam(required = false, defaultValue = "1") Integer page,
            @RequestParam(required = false, defaultValue = "8") Integer size) {
        return Result.ok(productService.search(keyword, categoryId, flashSale, page, size));
    }


    @GetMapping("/flash-sale")
    public Result<List<Product>> listFlashSale() {
        log.info("查询秒杀商品");
        return Result.ok(productService.listFlashSale());
    }

    @GetMapping("/category/{categoryId}")
    public Result<List<Product>> listByCategory(@PathVariable Long categoryId) {
        log.info("根据id查询商品：{}", categoryId);
        return Result.ok(productService.listByCategory(categoryId));
    }

    @GetMapping("/{id}")
    public Result<Product> detail(@PathVariable Long id) {
        log.info("删除购物车商品：{}", id);
        return Result.ok(productService.getById(id));
    }
}
