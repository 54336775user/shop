package com.eample.shop.controller;

import com.eample.shop.Service.SeckillService;
import com.eample.shop.common.Result;
import com.eample.shop.dto.SeckillBuyDTO;
import com.eample.shop.vo.SeckillOrderVO;
import com.eample.shop.vo.SeckillProductVO;
import lombok.RequiredArgsConstructor;
import org.jspecify.annotations.Nullable;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/seckill")
@RequiredArgsConstructor
public class SeckillController {

    private final SeckillService seckillService;

    /**
     * 秒杀商品列表
     */
    @GetMapping("/list")
    public Result<List<SeckillProductVO>> list() {
        return Result.ok(seckillService.list());
    }

    /**
     * 秒杀商品详情
     */
    @GetMapping("/{productId}")
    public Result<SeckillProductVO> detail(@PathVariable Long productId) {
        return Result.ok(seckillService.detail(productId));
    }

    /**
     * 获取秒杀token
     */
    @GetMapping("/{productId}/token")
    public Result<Map<String, String>> token(@PathVariable Long productId) {
        Long userId = currentUserId();
        String token = seckillService.createToken(userId, productId);
        return Result.ok(Map.of("token", token));
    }

    /**
     * 发起秒杀
     */
    @PostMapping("/{productId}/buy")
    public Result<Map<String, String>> buy(@PathVariable Long productId,
                                           @RequestBody SeckillBuyDTO request) {
        Long userId = currentUserId();
        String requestId = seckillService.buy(userId, productId, request);
        return Result.ok("抢购请求已提交", Map.of("requestId", requestId));
    }

    /**
     * 查询 buy 是否处于降级（用户端只读）
     */
    @GetMapping("/degrade/buy")
    public Result<Map<String, Object>> buyDegradeStatus() {
        boolean degraded = seckillService.isBuyDegraded();
        return Result.ok(Map.of(
                "degraded", degraded,
                "statusText", degraded ? "已降级" : "正常",
                "message", degraded ? "当前抢购人数过多，请稍后再试" : "正常"
        ));
    }

    /**
     * 查询秒杀结果
     */
    @GetMapping("/result/{requestId}")
    public Result<SeckillOrderVO> result(@PathVariable String requestId) {
        Long userId = currentUserId();
        return Result.ok(seckillService.result(userId, requestId));
    }

    private Long currentUserId() {
        @Nullable Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        return (Long) auth.getPrincipal();
    }
}