package com.eample.shop.runer;

import com.eample.shop.Service.SeckillService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;
@Component
@RequiredArgsConstructor
@Slf4j
public class SeckillStockWarmupRunner implements ApplicationRunner {
    private final SeckillService seckillService;
    @Override
    public void run(ApplicationArguments args) {
        try {
            seckillService.warmupStock();
            log.info("秒杀库存与商品列表缓存预热完成");
        } catch (Exception e) {
            log.error("秒杀库存预热失败", e);
        }
    }
}

