package com.eample.shop.mq;

import com.eample.shop.config.RabbitMQConfig;
import com.eample.shop.dto.SeckillMessage;
import com.eample.shop.entity.Product;
import com.eample.shop.entity.SeckillOrder;
import com.eample.shop.mapper.ProductMapper;
import com.eample.shop.mapper.SeckillOrderMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import org.springframework.data.redis.core.script.DefaultRedisScript;
import java.time.Duration;
import java.util.Collections;

@Component
@Slf4j
@RequiredArgsConstructor
public class SeckillOrderConsumer {

    private static final String SECKILL_REQUEST_PREFIX = "shop:seckill:request:";

    private final ProductMapper productMapper;
    private final SeckillOrderMapper seckillOrderMapper;
    private final RedisTemplate<String, Object> redisTemplate;

    private final DefaultRedisScript<Long> seckillLockReleaseScript;

    @RabbitListener(queues = RabbitMQConfig.SECKILL_QUEUE)
    @Transactional(rollbackFor = Exception.class)
    public void handleSeckillOrder(SeckillMessage message) {
        //检测锁
        String lockKey = "shop:seckill:lock:order:" + message.getUserId() + ":" + message.getProductId();
        String lockValue = UUID.randomUUID().toString();
        Boolean locked = redisTemplate.opsForValue()
                .setIfAbsent(lockKey, lockValue, Duration.ofSeconds(5));
        if (Boolean.FALSE.equals(locked)) {
            log.warn("重复秒杀请求被锁拦截 userId={}, productId={}", message.getUserId(), message.getProductId());
            return;
        }
        try {
            Product product = productMapper.findById(message.getProductId());
            if (product == null) {
                updateRequestFail(message.getRequestId(), "商品不存在");
                return;
            }

            if (product.getFlashStock() == null || product.getFlashStock() < message.getQuantity()) {
                updateRequestFail(message.getRequestId(), "秒杀库存不足");
                return;
            }
            int bought = seckillOrderMapper.countUserBought(message.getUserId(), message.getProductId());
            if (bought > 0) {
                updateRequestFail(message.getRequestId(), "每人限购一件");
                return;
            }

            //DB扣库存
            int rows = productMapper.decreaseFlashStock(message.getProductId(), message.getQuantity());
            if (rows == 0) {
                updateRequestFail(message.getRequestId(), "秒杀库存不足");
                return;
            }

            SeckillOrder order = new SeckillOrder();
            order.setOrderNo(generateOrderNo(message.getUserId()));
            order.setUserId(message.getUserId());
            order.setProductId(product.getId());
            order.setProductName(product.getName());
            order.setProductImage(product.getImage());
            order.setSeckillPrice(BigDecimal.valueOf(product.getFlashPrice()));
            order.setQuantity(message.getQuantity());
            order.setTotalAmount(BigDecimal.valueOf(product.getFlashPrice())
                    .multiply(BigDecimal.valueOf(message.getQuantity())));
            order.setStatus(0);
            order.setCreateTime(LocalDateTime.now());
            order.setUpdateTime(LocalDateTime.now());
            seckillOrderMapper.insert(order);

            updateRequestSuccess(message.getRequestId(), order.getId());

        } catch (Exception e) {
            log.error("处理秒杀订单失败", e);
            updateRequestFail(message.getRequestId(), "系统繁忙，请稍后重试");
            throw e;
        }finally {
            //消费结束，Lua解锁（释放锁）
            try {
                redisTemplate.execute(
                        seckillLockReleaseScript,
                        Collections.singletonList(lockKey),
                        lockValue
                );
                log.info("释放锁成功");
            } catch (Exception e) {
                log.warn("释放秒杀锁失败 key={}", lockKey, e);
            }
        }
    }

    private void updateRequestSuccess(String requestId, Long orderId) {
        String key = SECKILL_REQUEST_PREFIX + requestId;
        redisTemplate.opsForValue().set(key, "SUCCESS:" + orderId, 10, TimeUnit.MINUTES);
        log.info(" 成功创建秒杀： {} ", orderId);
    }

    private void updateRequestFail(String requestId, String reason) {
        String key = SECKILL_REQUEST_PREFIX + requestId;
        redisTemplate.opsForValue().set(key, "FAIL:" + reason, 10, TimeUnit.MINUTES);
        log.info(" 秒杀创建失败，失败原因： {} ", reason);
    }

    private String generateOrderNo(Long userId) {
        String time = java.time.LocalDateTime.now()
                .format(java.time.format.DateTimeFormatter.ofPattern("yyyyMMddHHmmss"));
        String random = UUID.randomUUID().toString().replace("-", "").substring(0, 8);
        return "SK" + time + userId + random;
    }
}
