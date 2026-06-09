package com.eample.shop.controller;

import com.eample.shop.Service.AdminOrderService;
import com.eample.shop.common.PageResult;
import com.eample.shop.common.Result;
import com.eample.shop.vo.OrderDetailVO;
import com.eample.shop.vo.OrderVO;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/admin/order")
@RequiredArgsConstructor
public class AdminOrderController {

    private final AdminOrderService adminOrderService;

    /**
     * 后台订单列表
     */
    @GetMapping("/list")
    public Result<PageResult<OrderVO>> list(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) Integer status,
            @RequestParam(required = false, defaultValue = "1") Integer page,
            @RequestParam(required = false, defaultValue = "10") Integer size) {
        return Result.ok(adminOrderService.list(keyword, status, page, size));
    }

    /**
     * 后台订单详情
     */
    @GetMapping("/{id}")
    public Result<OrderDetailVO> detail(@PathVariable Long id,
                                        @RequestParam(required = false) Integer orderType) {
        return Result.ok(adminOrderService.detail(id, orderType));
    }

    /**
     * 修改订单状态
     */
    @PutMapping("/status")
    public Result<Void> updateStatus(@RequestParam Long id, @RequestParam Integer status) {
        adminOrderService.updateStatus(id, status);
        return Result.ok("状态修改成功", null);
    }

    /**
     * 后台取消订单
     */
    @PutMapping("/cancel/{id}")
    public Result<Void> cancel(@PathVariable Long id) {
        adminOrderService.cancel(id);
        return Result.ok("订单已取消", null);
    }
}
