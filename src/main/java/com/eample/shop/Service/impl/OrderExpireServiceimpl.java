package com.eample.shop.Service.impl;

import com.eample.shop.Service.OrderExpireService;
import com.eample.shop.cache.ProductCacheService;
import com.eample.shop.entity.OrderItem;
import com.eample.shop.entity.SeckillOrder;
import com.eample.shop.entity.ShopOrder;
import com.eample.shop.mapper.OrderItemMapper;
import com.eample.shop.mapper.OrderMapper;
import com.eample.shop.mapper.ProductMapper;
import com.eample.shop.mapper.SeckillOrderMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class OrderExpireServiceimpl implements OrderExpireService {

    private static final int STATUS_UNPAID = 0;
    private static final int STATUS_CANCELLED = 4;
    private static final int ORDER_TYPE_SECKILL = 2;
    private static final int SECKILL_STATUS_UNPAID = 0;
    private static final int SECKILL_STATUS_CANCELLED = 2;
    private static final int PAYMENT_TIMEOUT_MINUTES = 15;

    private final OrderMapper orderMapper;
    private final OrderItemMapper orderItemMapper;
    private final ProductMapper productMapper;
    private final SeckillOrderMapper seckillOrderMapper;
    private final ProductCacheService productCacheService;
    private final RedisTemplate<String, Object> redisTemplate;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void cancelExpiredOrder(Long orderId) {
        ShopOrder order = orderMapper.findById(orderId);
        if (order == null) {
            return;
        }
        if (!Integer.valueOf(STATUS_UNPAID).equals(order.getStatus())) {
            return;
        }
        if (!isPaymentExpired(order.getCreateTime())) {
            return;
        }
        doCancelExpiredNormalOrder(orderId);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void cancelExpiredSeckillOrder(Long orderId) {
        SeckillOrder order = seckillOrderMapper.findById(orderId);
        if (order == null) {
            return;
        }
        if (!Integer.valueOf(SECKILL_STATUS_UNPAID).equals(order.getStatus())) {
            return;
        }
        if (!isPaymentExpired(order.getCreateTime())) {
            return;
        }
        doCancelExpiredSeckillOrder(orderId);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void expireUnpaidOrder(Long userId, Long orderId, Integer orderType) {
        if (Integer.valueOf(ORDER_TYPE_SECKILL).equals(orderType)) {
            expireSeckillUnpaidOrder(userId, orderId);
            return;
        }

        ShopOrder order = orderMapper.findByIdAndUserId(orderId, userId);
        if (order != null) {
            expireNormalUnpaidOrder(order);
            return;
        }

        if (orderType == null) {
            expireSeckillUnpaidOrder(userId, orderId);
            return;
        }

        throw new RuntimeException("订单不存在");
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void sweepExpiredOrdersForUser(Long userId) {
        LocalDateTime cutoff = paymentCutoffTime();
        List<ShopOrder> expiredNormal = orderMapper.findExpiredUnpaidOrdersByUserId(
                userId, STATUS_UNPAID, cutoff);
        for (ShopOrder order : expiredNormal) {
            try {
                doCancelExpiredNormalOrder(order.getId());
            } catch (Exception ignored) {
                // 单笔失败不影响列表
            }
        }
        List<SeckillOrder> expiredSeckill = seckillOrderMapper.findExpiredUnpaidOrdersByUserId(
                userId, SECKILL_STATUS_UNPAID, cutoff);
        for (SeckillOrder order : expiredSeckill) {
            try {
                doCancelExpiredSeckillOrder(order.getId());
            } catch (Exception ignored) {
                // 单笔失败不影响列表
            }
        }
    }

    @Override
    public boolean isPaymentExpired(LocalDateTime createTime) {
        return createTime != null && createTime.isBefore(paymentCutoffTime());
    }

    private LocalDateTime paymentCutoffTime() {
        return LocalDateTime.now().minusMinutes(PAYMENT_TIMEOUT_MINUTES);
    }

    private void expireNormalUnpaidOrder(ShopOrder order) {
        if (!Integer.valueOf(STATUS_UNPAID).equals(order.getStatus())) {
            return;
        }
        if (!isPaymentExpired(order.getCreateTime())) {
            return;
        }
        doCancelExpiredNormalOrder(order.getId());
    }

    private void expireSeckillUnpaidOrder(Long userId, Long orderId) {
        SeckillOrder seckillOrder = seckillOrderMapper.findByIdAndUserId(orderId, userId);
        if (seckillOrder == null) {
            throw new RuntimeException("订单不存在");
        }
        if (!Integer.valueOf(SECKILL_STATUS_UNPAID).equals(seckillOrder.getStatus())) {
            return;
        }
        if (!isPaymentExpired(seckillOrder.getCreateTime())) {
            return;
        }
        doCancelExpiredSeckillOrder(seckillOrder.getId());
    }

    private void doCancelExpiredNormalOrder(Long orderId) {
        ShopOrder order = orderMapper.findById(orderId);
        if (order == null || !Integer.valueOf(STATUS_UNPAID).equals(order.getStatus())) {
            return;
        }
        List<OrderItem> items = orderItemMapper.findByOrderId(orderId);
        for (OrderItem item : items) {
            productMapper.increaseStock(item.getProductId(), item.getQuantity());
        }
        int rows = orderMapper.updateStatusById(orderId, STATUS_CANCELLED);
        if (rows == 0) {
            throw new RuntimeException("自动取消订单失败");
        }
        productCacheService.clearAllProductListCache();
    }

    private void doCancelExpiredSeckillOrder(Long orderId) {
        SeckillOrder seckillOrder = seckillOrderMapper.findById(orderId);
        if (seckillOrder == null || !Integer.valueOf(SECKILL_STATUS_UNPAID).equals(seckillOrder.getStatus())) {
            return;
        }
        restoreSeckillStock(seckillOrder.getProductId(), seckillOrder.getQuantity());
        int rows = seckillOrderMapper.updateStatusById(orderId, SECKILL_STATUS_CANCELLED);
        if (rows == 0) {
            throw new RuntimeException("自动取消秒杀订单失败");
        }
        productCacheService.clearAllProductListCache();
    }

    private void restoreSeckillStock(Long productId, Integer quantity) {
        if (productId == null || quantity == null || quantity <= 0) {
            return;
        }
        productMapper.increaseFlashStock(productId, quantity);
        redisTemplate.opsForValue().increment("shop:seckill:stock:" + productId, quantity);
    }
}
