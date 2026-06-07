package com.eample.shop.Service.impl;

import com.eample.shop.Service.SeckillService;
import com.eample.shop.config.RabbitMQConfig;
import com.eample.shop.dto.SeckillMessage;
import com.eample.shop.entity.Product;
import com.eample.shop.mapper.ProductMapper;
import com.eample.shop.dto.SeckillBuyDTO;
import com.eample.shop.mapper.SeckillOrderMapper;
import com.eample.shop.vo.SeckillOrderVO;
import com.eample.shop.vo.SeckillProductVO;
import lombok.RequiredArgsConstructor;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class SeckillServiceimpl implements SeckillService {

    //降级处理模块
    private static final String SECKILL_DEGRADE_BUY_KEY = "shop:seckill:degrade:buy";
    private static final String SECKILL_PRODUCT_LIST_KEY = "shop:seckill:product:list";

    //
    private static final String SECKILL_TOKEN_PREFIX = "shop:seckill:token:";
    private static final String SECKILL_REQUEST_PREFIX = "shop:seckill:request:";
    private static final int SECKILL_TOKEN_EXPIRE_MINUTES = 5;

    private final ProductMapper productMapper;
    private final RedisTemplate<String, Object> redisTemplate;
    private final DefaultRedisScript<Long> orderTokenScript;
    private final RabbitTemplate rabbitTemplate;
    private final SeckillOrderMapper seckillOrderMapper;

    @Override
    public List<SeckillProductVO> list() {
        List<SeckillProductVO> cached = getCachedProductList();
        if (cached != null) {
            return cached;
        }
        List<SeckillProductVO> list = loadVisibleProductsFromDb();
        cacheProductList(list);
        return list;
    }

    @Override
    public void refreshProductListCache() {
        List<SeckillProductVO> list = loadVisibleProductsFromDb();
        cacheProductList(list);
    }

    @Override
    public SeckillProductVO detail(Long productId) {
        Product product = productMapper.findById(productId);
        if (product == null) {
            throw new RuntimeException("商品不存在");
        }
        if (!isActiveSeckillProduct(product)) {
            throw new RuntimeException("秒杀商品未开始或已结束");
        }
        return toSeckillProductVO(product);
    }

    @Override
    public String createToken(Long userId, Long productId) {
        String token = UUID.randomUUID().toString().replace("-", "");
        String key = buildTokenKey(userId, productId, token);
        redisTemplate.opsForValue().set(key, "1", SECKILL_TOKEN_EXPIRE_MINUTES, TimeUnit.MINUTES);
        return token;
    }

    @Override
    public String buy(Long userId, Long productId, SeckillBuyDTO request) {
        //降级处理
        if (request == null) {
            throw new RuntimeException("请求参数不能为空");
        }
        // ... 和现有的 quantity、token 校验 ...
        // 降级开关：buy 入口拦截
        if (isBuyDegraded()) {
            throw new RuntimeException("当前抢购人数过多，请稍后再试");
        }
        //
        if (request == null) {
            throw new RuntimeException("请求参数不能为空");
        }
        Integer quantity = request.getQuantity();
        if (quantity == null || quantity != 1) {
            throw new RuntimeException("秒杀商品暂时只支持购买1件");
        }
        if (!StringUtils.hasText(request.getToken())) {
            throw new RuntimeException("请勿重复提交");
        }
        Product product = productMapper.findById(productId);
        if (product == null) {
            throw new RuntimeException("商品不存在");
        }
        if (!isActiveSeckillProduct(product)) {
            throw new RuntimeException("秒杀活动未开始或已结束");
        }
        // 1. 消费 token
        consumeToken(userId, productId, request.getToken());


        String stockKey = buildStockKey(productId);
        Boolean exists = redisTemplate.hasKey(stockKey);
        if (Boolean.FALSE.equals(exists)) {
            throw new RuntimeException("秒杀库存初始化中，请稍后重试");
        }
        // 2. Redis预扣库存
        Long remain = redisTemplate.opsForValue().decrement(stockKey, quantity);
        if (remain == null) {
            throw new RuntimeException("秒杀库存不足");
        }
        if (remain < 0) {
            redisTemplate.opsForValue().increment(stockKey, quantity);
            throw new RuntimeException("秒杀库存不足");
        }
        // 3. 生成 requestId
        String requestId = UUID.randomUUID().toString().replace("-", "");
        // 4. 先记录排队中
        String requestKey = SECKILL_REQUEST_PREFIX + requestId;
        redisTemplate.opsForValue().set(requestKey, "PENDING", 10, TimeUnit.MINUTES);
        // 5. 发送 MQ
        SeckillMessage message = new SeckillMessage();
        message.setUserId(userId);
        message.setProductId(productId);
        message.setQuantity(quantity);
        message.setRequestId(requestId);

        try {
            rabbitTemplate.convertAndSend(
                    RabbitMQConfig.SECKILL_EXCHANGE,
                    RabbitMQConfig.SECKILL_ROUTING_KEY,
                    message
            );
        } catch (Exception e) {
            // MQ 发不出去，库存要回滚
            redisTemplate.opsForValue().increment(stockKey, quantity);
            // 请求状态改失败
            redisTemplate.opsForValue().set(requestKey, "FAIL:系统繁忙，请稍后重试", 10, TimeUnit.MINUTES);
            throw new RuntimeException("系统繁忙，请稍后重试");
        }

        return requestId;
    }


    public SeckillOrderVO result(Long userId, String requestId) {
        if (!StringUtils.hasText(requestId)) {
            throw new RuntimeException("requestId不能为空");
        }

        String key = SECKILL_REQUEST_PREFIX + requestId;
        Object value = redisTemplate.opsForValue().get(key);

        SeckillOrderVO vo = new SeckillOrderVO();

        if (value == null) {
            vo.setStatus(2);
            vo.setStatusText("请求不存在或已过期");
            vo.setMessage("请求不存在或已过期");
            return vo;
        }

        String status = String.valueOf(value);
        if ("PENDING".equals(status)) {
            vo.setStatus(0);
            vo.setStatusText("排队中");
            vo.setMessage("秒杀请求正在处理中");
            return vo;
        }

        if (status.startsWith("SUCCESS:")) {
            Long orderId = Long.valueOf(status.substring("SUCCESS:".length()));
            vo.setStatus(1);
            vo.setStatusText("抢购成功");
            vo.setOrderId(orderId);
            vo.setMessage("抢购成功");
            return vo;
        }

        if (status.startsWith("FAIL:")) {
            vo.setStatus(2);
            vo.setStatusText("抢购失败");
            vo.setMessage(status.substring("FAIL:".length()));
            return vo;
        }

        vo.setStatus(0);
        vo.setStatusText("排队中");
        vo.setMessage("请求处理中");
        return vo;
    }

    @Override
    public void warmupStock() {
        List<Product> products = productMapper.findFlashSale();
        for (Product product : products) {
            warmupSingleProductStock(product);
        }
        refreshProductListCache();
    }

    @Override
    public void warmupStockByProductId(Long productId) {
        if (productId == null) {
            return;
        }
        Product product = productMapper.findById(productId);
        if (product == null) {
            return;
        }
        if (isActiveSeckillProduct(product)) {
            warmupSingleProductStock(product);
        }
        refreshProductListCache();
    }

    //降级处理部分
    @Override
    public boolean isBuyDegraded() {
        Object value = redisTemplate.opsForValue().get(SECKILL_DEGRADE_BUY_KEY);
        return "1".equals(String.valueOf(value));
    }

    @Override
    public void enableBuyDegrade() {
        redisTemplate.opsForValue().set(SECKILL_DEGRADE_BUY_KEY, "1");
    }

    @Override
    public void disableBuyDegrade() {
        redisTemplate.opsForValue().set(SECKILL_DEGRADE_BUY_KEY, "0");
    }

    private void warmupSingleProductStock(Product product) {
        String stockKey = buildStockKey(product.getId());
        int flashStock = product.getFlashStock() == null ? 0 : product.getFlashStock();

        // B 方案：预热时直接覆盖，保证 Redis 与 DB 对齐
        redisTemplate.opsForValue().set(stockKey, flashStock);

        // 活动结束后再保留 30 分钟，避免脏 key 长期残留
        LocalDateTime end = product.getFlashEndTime();
        if (end != null) {
            long seconds = Duration.between(LocalDateTime.now(), end.plusMinutes(30)).getSeconds();
            if (seconds > 0) {
                redisTemplate.expire(stockKey, seconds, TimeUnit.SECONDS);
            }
        }
    }
    private boolean isActiveSeckillProduct(Product product) {
        if (product == null) {
            return false;
        }
        if (product.getStatus() == null || product.getStatus() != 1) {
            return false;
        }
        if (product.getIsFlashSale() == null || product.getIsFlashSale() != 1) {
            return false;
        }
        if (product.getFlashPrice() == null || product.getFlashPrice() <= 0) {
            return false;
        }
        if (product.getFlashStock() == null || product.getFlashStock() <= 0) {
            return false;
        }

        LocalDateTime now = LocalDateTime.now();
        if (product.getFlashStartTime() != null && now.isBefore(product.getFlashStartTime())) {
            return false;
        }
        if (product.getFlashEndTime() != null && now.isAfter(product.getFlashEndTime())) {
            return false;
        }
        return true;
    }

    /**
     * 前台列表可见：进行中 + 10 分钟内即将开始
     */
    private boolean isVisibleSeckillProduct(Product product) {
        if (product == null) {
            return false;
        }
        if (product.getStatus() == null || product.getStatus() != 1) {
            return false;
        }
        if (product.getIsFlashSale() == null || product.getIsFlashSale() != 1) {
            return false;
        }
        if (product.getFlashPrice() == null || product.getFlashPrice() <= 0) {
            return false;
        }
        if (product.getFlashStock() == null || product.getFlashStock() <= 0) {
            return false;
        }
        if (product.getFlashStartTime() == null || product.getFlashEndTime() == null) {
            return false;
        }

        LocalDateTime now = LocalDateTime.now();
        if (now.isAfter(product.getFlashEndTime())) {
            return false;
        }
        if (product.getFlashStartTime().isAfter(now.plusMinutes(10))) {
            return false;
        }
        return true;
    }

    private SeckillProductVO toSeckillProductVO(Product product) {
        SeckillProductVO vo = new SeckillProductVO();
        vo.setId(product.getId());
        vo.setName(product.getName());
        vo.setImage(product.getImage());
        vo.setDescription(product.getDescription());
        vo.setOriginalPrice(product.getPrice());
        vo.setFlashPrice(BigDecimal.valueOf(product.getFlashPrice()));
        vo.setStock(product.getStock());
        vo.setFlashStock(product.getFlashStock());
        vo.setFlashStartTime(product.getFlashStartTime());
        vo.setFlashEndTime(product.getFlashEndTime());

        int stock = product.getFlashStock() == null ? 0 : product.getFlashStock();
        int sold = Math.max(0, product.getStock() == null ? 0 : product.getStock() - stock);
        int progress = (stock + sold) == 0 ? 0 : (int) Math.min(100, Math.round(sold * 100.0 / (stock + sold)));
        vo.setProgress(progress);

        vo.setStatus(product.getStatus());
        vo.setStatusText(product.getStatus() != null && product.getStatus() == 1 ? "上架" : "下架");
        return vo;
    }

    private String buildTokenKey(Long userId, Long productId, String token) {
        return SECKILL_TOKEN_PREFIX + userId + ":" + productId + ":" + token;
    }

    private void consumeToken(Long userId, Long productId, String token) {
        String key = buildTokenKey(userId, productId, token);
        Long result = redisTemplate.execute(orderTokenScript, List.of(key));
        if (result == null || result == 0L) {
            throw new RuntimeException("请勿重复提交");
        }
    }

    private String buildStockKey(Long productId) {
        return "shop:seckill:stock:" + productId;
    }

    private List<SeckillProductVO> loadVisibleProductsFromDb() {
        return productMapper.findVisibleFlashSale().stream()
                .filter(this::isVisibleSeckillProduct)
                .map(this::toSeckillProductVO)
                .collect(Collectors.toList());
    }

    @SuppressWarnings("unchecked")
    private List<SeckillProductVO> getCachedProductList() {
        Object value = redisTemplate.opsForValue().get(SECKILL_PRODUCT_LIST_KEY);
        if (value == null) {
            return null;
        }
        return (List<SeckillProductVO>) value;
    }

    private void cacheProductList(List<SeckillProductVO> list) {
        redisTemplate.opsForValue().set(SECKILL_PRODUCT_LIST_KEY, list);
        long ttlSeconds = calculateListCacheTtlSeconds(list);
        if (ttlSeconds > 0) {
            redisTemplate.expire(SECKILL_PRODUCT_LIST_KEY, ttlSeconds, TimeUnit.SECONDS);
        }
    }

    private long calculateListCacheTtlSeconds(List<SeckillProductVO> list) {
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime latestEnd = list.stream()
                .map(SeckillProductVO::getFlashEndTime)
                .filter(end -> end != null)
                .max(LocalDateTime::compareTo)
                .orElse(now.plusMinutes(30));
        long seconds = Duration.between(now, latestEnd.plusMinutes(30)).getSeconds();
        return Math.max(seconds, 60);
    }
}
