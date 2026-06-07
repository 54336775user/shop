package com.eample.shop.controller;

import com.eample.shop.Service.CartService;
import com.eample.shop.common.Result;
import com.eample.shop.dto.CartAddDTO;
import com.eample.shop.dto.CartUpdateDTO;
import com.eample.shop.vo.CartItemVO;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/cart")
@RequiredArgsConstructor
public class CartController {

    private final CartService cartService;

    @GetMapping("/list")
    public Result<List<CartItemVO>> list() {
        Long userId = currentUserId();
        return Result.ok(cartService.list(userId));
    }

    @PostMapping("/add")
    public Result<Void> add(@RequestBody CartAddDTO request) {
        Long userId = currentUserId();
        cartService.add(userId, request);
        return Result.ok("加入购物车成功", null);
    }

    @PutMapping("/quantity")
    public Result<Void> updateQuantity(@RequestBody CartUpdateDTO request) {
        Long userId = currentUserId();
        cartService.updateQuantity(userId, request);
        return Result.ok("更新成功", null);
    }

    @DeleteMapping("/{cartItemId}")
    public Result<Void> remove(@PathVariable Long cartItemId) {
        Long userId = currentUserId();
        cartService.remove(userId, cartItemId);
        return Result.ok("删除成功", null);
    }

    private Long currentUserId() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        return (Long) auth.getPrincipal();
    }
}