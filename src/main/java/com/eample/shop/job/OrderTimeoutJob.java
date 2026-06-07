package com.eample.shop.job;

import com.eample.shop.Service.OrderExpireService;
import com.eample.shop.entity.SeckillOrder;
import com.eample.shop.entity.ShopOrder;
import com.eample.shop.mapper.OrderMapper;
import com.eample.shop.mapper.SeckillOrderMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;

@Component
@RequiredArgsConstructor
@Slf4j
public class OrderTimeoutJob {

    private static final int PAYMENT_TIMEOUT_MINUTES = 15;
    private static final int STATUS_UNPAID = 0;
    private static final int SECKILL_STATUS_UNPAID = 0;

    private final OrderMapper orderMapper;
    private final SeckillOrderMapper seckillOrderMapper;
    private final OrderExpireService orderExpireService;

    @Scheduled(fixedDelay = 60000)
    public void cancelExpiredOrders() {
        log.info("定时检测超时订单");
        LocalDateTime cutoff = LocalDateTime.now().minusMinutes(PAYMENT_TIMEOUT_MINUTES);
        List<ShopOrder> orders = orderMapper.findExpiredUnpaidOrders(STATUS_UNPAID, cutoff);
        for (ShopOrder order : orders) {
            try {
                orderExpireService.cancelExpiredOrder(order.getId());
                log.info("普通订单超时取消 id={}", order.getId());
            } catch (Exception e) {
                log.warn("普通订单超时取消失败 id={}", order.getId(), e);
            }
        }
        List<SeckillOrder> seckillOrders = seckillOrderMapper.findExpiredUnpaidOrders(SECKILL_STATUS_UNPAID, cutoff);
        for (SeckillOrder order : seckillOrders) {
            try {
                orderExpireService.cancelExpiredSeckillOrder(order.getId());
                log.info("秒杀订单超时取消 id={}", order.getId());
            } catch (Exception e) {
                log.warn("秒杀订单超时取消失败 id={}", order.getId(), e);
            }
        }
    }
}
