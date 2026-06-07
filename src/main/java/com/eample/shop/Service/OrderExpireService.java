package com.eample.shop.Service;

import java.time.LocalDateTime;

public interface OrderExpireService {

    void cancelExpiredOrder(Long orderId);

    void cancelExpiredSeckillOrder(Long orderId);

    void expireUnpaidOrder(Long userId, Long orderId, Integer orderType);

    void sweepExpiredOrdersForUser(Long userId);

    boolean isPaymentExpired(LocalDateTime createTime);
}
