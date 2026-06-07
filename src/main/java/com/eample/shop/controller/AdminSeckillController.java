package com.eample.shop.controller;

import com.eample.shop.Service.SeckillService;
import com.eample.shop.common.Result;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/admin/seckill")
@RequiredArgsConstructor
public class AdminSeckillController {

    private final SeckillService seckillService;

    /** 查询 buy 降级状态 */
    @GetMapping("/degrade/buy")
    public Result<Map<String, Object>> getBuyDegradeStatus() {
        boolean degraded = seckillService.isBuyDegraded();
        return Result.ok(Map.of(
                "degraded", degraded,
                "statusText", degraded ? "已降级" : "正常"
        ));
    }

    /** 开启 buy 降级 */
    @PutMapping("/degrade/buy/enable")
    public Result<Void> enableBuyDegrade() {
        seckillService.enableBuyDegrade();
        return Result.ok("已开启秒杀抢购降级", null);
    }

    /** 关闭 buy 降级 */
    @PutMapping("/degrade/buy/disable")
    public Result<Void> disableBuyDegrade() {
        seckillService.disableBuyDegrade();
        return Result.ok("已关闭秒杀抢购降级", null);
    }

    /** 刷新秒杀商品列表 Redis 缓存 */
    @PutMapping("/cache/list/refresh")
    public Result<Void> refreshProductListCache() {
        seckillService.refreshProductListCache();
        return Result.ok("秒杀列表缓存已刷新", null);
    }
}
