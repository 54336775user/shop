package com.eample.shop.controller;


import com.eample.shop.Service.OrderService;
import com.eample.shop.common.Result;
import com.eample.shop.dto.OrderCreateDTO;
import com.eample.shop.vo.OrderDetailVO;
import com.eample.shop.vo.OrderVO;
import lombok.RequiredArgsConstructor;
import org.jspecify.annotations.Nullable;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/order")
@RequiredArgsConstructor
public class OrderController {

    private final OrderService orderService;

    /**
     * 获取订单token
     * @return
     */
    @GetMapping("/token")
    public Result<Map<String, String>> token() {
        Long userId = currentUserId();
        String token = orderService.createToken(userId);
        return Result.ok(Map.of("token", token));
    }

    /**
     * 从购物车结算下单
     */
    @PostMapping("/create")
    public Result<Map<String, Long>> create(@RequestBody OrderCreateDTO request) {
        Long userId = currentUserId();
        Long orderId = orderService.create(userId, request);
        return Result.ok("下单成功", Map.of("orderId", orderId));
    }
    /**
     * 我的订单列表
     */
    @GetMapping("/list")
    public Result<List<OrderVO>> list() {
        Long userId = currentUserId();
        return Result.ok(orderService.list(userId));
    }
    /**
     * 订单详情
     */
    @GetMapping("/{orderId}")
    public Result<OrderDetailVO> detail(@PathVariable Long orderId,
                                      @RequestParam(required = false) Integer orderType) {
        Long userId = currentUserId();
        return Result.ok(orderService.detail(userId, orderId, orderType));
    }
    /**
     * 取消订单
     */
    @PutMapping("/{orderId}/cancel")
    public Result<Void> cancel(@PathVariable Long orderId,
                               @RequestParam(required = false) Integer orderType) {
        Long userId = currentUserId();
        orderService.cancel(userId, orderId, orderType);
        return Result.ok("订单已取消", null);
    }

    /**
     * 支付超时取消（倒计时结束时前端调用）
     */
    @PutMapping("/{orderId}/expire")
    public Result<Void> expire(@PathVariable Long orderId,
                               @RequestParam(required = false) Integer orderType) {
        Long userId = currentUserId();
        orderService.expireUnpaidOrder(userId, orderId, orderType);
        return Result.ok("订单已超时取消", null);
    }

    /**
     * 支付模块
     * @param orderId
     * @return
     */
    @PutMapping("/pay/{orderId}")
    public Result<Void> pay(@PathVariable Long orderId,
                            @RequestParam(required = false) Integer orderType) {
        Long userId = currentUserId();
        orderService.pay(userId, orderId, orderType);
        return Result.ok("支付成功", null);
    }
    private Long currentUserId() {
        @Nullable Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        return (Long) auth.getPrincipal();
    }
}
