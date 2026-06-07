package com.eample.shop.mq;


import com.eample.shop.config.RabbitMQConfig;
import com.eample.shop.dto.SeckillMessage;
import com.eample.shop.entity.MqDeadLetter;
import com.eample.shop.mapper.MqDeadLetterMapper;
import com.eample.shop.mapper.SeckillOrderMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.amqp.support.converter.JacksonJsonMessageConverter;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

@Component
@Slf4j
@RequiredArgsConstructor
public class SeckillOrderDlqConsumer {

    private static final String BIZ_TYPE = "SECKILL_ORDER";
    private static final String SECKILL_REQUEST_PREFIX = "shop:seckill:request:";

    private final MqDeadLetterMapper mqDeadLetterMapper;
    private final SeckillOrderMapper seckillOrderMapper;
    private final RedisTemplate<String, Object> redisTemplate;
    private final JacksonJsonMessageConverter messageConverter;

    @RabbitListener(queues = RabbitMQConfig.SECKILL_DLQ)
    public void handleDeadLetter(Message message) {
        String rawBody = new String(message.getBody(), StandardCharsets.UTF_8);
        log.warn("收到秒杀死信消息 body={}", rawBody);
        SeckillMessage seckillMessage = parseMessage(message);
        if (seckillMessage == null) {
            saveUnknownDeadLetter(rawBody, "消息体解析失败");
            return;
        }

        // 幂等：同一个 requestId 只落一条死信记录
        if (StringUtils.hasText(seckillMessage.getRequestId())) {
            MqDeadLetter existed = mqDeadLetterMapper.findByRequestId(seckillMessage.getRequestId());
            if (existed != null) {
                log.warn("死信消息已存在，跳过 requestId={}", seckillMessage.getRequestId());
                return;
            }
        }
        int retryCount = extractRetryCount(message);

        // 1. 落库
        MqDeadLetter record = new MqDeadLetter();
        record.setBizType(BIZ_TYPE);
        record.setRequestId(seckillMessage.getRequestId());
        record.setUserId(seckillMessage.getUserId());
        record.setProductId(seckillMessage.getProductId());
        record.setMessageBody(rawBody);
        record.setFailReason(extractFailReason(message));
        record.setRetryCount(retryCount);
        record.setStatus(0);   // 0 = 待人工处理
        record.setCreateTime(LocalDateTime.now());
        record.setUpdateTime(LocalDateTime.now());
        mqDeadLetterMapper.insert(record);

        // 2. 给用户侧 request 写失败态
        if (StringUtils.hasText(seckillMessage.getRequestId())) {
            updateRequestFail(seckillMessage.getRequestId(), "系统异常，已转人工处理");
        }

        // 3. 补偿：如果还没落单，回滚 buy() 里 Redis 预扣库存
        compensateRedisStock(seckillMessage);

        log.warn("秒杀死信已落库 id={}, requestId={}", record.getId(), seckillMessage.getRequestId());
    }

    private SeckillMessage parseMessage(Message message) {
        try {
            Object obj = messageConverter.fromMessage(message);
            if (obj instanceof SeckillMessage seckillMessage) {
                return seckillMessage;
            }
            return null;
        } catch (Exception e) {
            log.error("解析死信消息失败", e);
            return null;
        }
    }

    private void saveUnknownDeadLetter(String rawBody, String reason) {
        MqDeadLetter record = new MqDeadLetter();
        record.setBizType(BIZ_TYPE);
        record.setMessageBody(rawBody);
        record.setFailReason(reason);
        record.setRetryCount(0);
        record.setStatus(0);
        record.setCreateTime(LocalDateTime.now());
        record.setUpdateTime(LocalDateTime.now());
        mqDeadLetterMapper.insert(record);
    }

    private int extractRetryCount(Message message) {
        Object xDeath = message.getMessageProperties().getHeaders().get("x-death");
        if (!(xDeath instanceof List<?> deathList) || deathList.isEmpty()) {
            return 0;
        }
        Object first = deathList.get(0);
        if (first instanceof Map<?, ?> map && map.get("count") != null) {
            return Integer.parseInt(String.valueOf(map.get("count")));
        }
        return 0;
    }

    private String extractFailReason(Message message) {
        Object reason = message.getMessageProperties().getHeaders().get("x-exception-message");
        if (reason != null) {
            return String.valueOf(reason);
        }
        log.info("消费重试耗尽，进入死信队列");
        return "消费重试耗尽，进入死信队列";
    }

    private void compensateRedisStock(SeckillMessage message) {
        if (message.getUserId() == null || message.getProductId() == null) {
            return;
        }
        int bought = seckillOrderMapper.countUserBought(message.getUserId(), message.getProductId());
        if (bought > 0) {
            log.info("已存在秒杀订单，不回滚 Redis 库存 userId={}, productId={}",
                    message.getUserId(), message.getProductId());
            return;
        }

        Integer quantity = message.getQuantity() == null ? 1 : message.getQuantity();
        String stockKey = "shop:seckill:stock:" + message.getProductId();
        redisTemplate.opsForValue().increment(stockKey, quantity);
        log.info("已回滚 Redis 预扣库存 key={}, quantity={}", stockKey, quantity);
    }

    private void updateRequestFail(String requestId, String reason) {
        String key = SECKILL_REQUEST_PREFIX + requestId;
        redisTemplate.opsForValue().set(key, "FAIL:" + reason, 10, TimeUnit.MINUTES);
    }
}
