package com.eample.shop.Service.impl;

import com.eample.shop.Service.OrderExpireService;
import com.eample.shop.Service.GroupBuyService;
import com.eample.shop.Service.OrderService;
import com.eample.shop.Service.ProductService;
import com.eample.shop.cache.ProductCacheService;
import com.eample.shop.dto.OrderCreateDTO;
import com.eample.shop.entity.*;
import com.eample.shop.mapper.*;
import com.eample.shop.vo.OrderDetailVO;
import com.eample.shop.vo.OrderItemVO;
import com.eample.shop.vo.OrderVO;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ThreadLocalRandom;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class OrderServiceimpl implements OrderService {
    private static final int STATUS_UNPAID = 0;
    private static final int STATUS_PAID = 1;
    private static final int STATUS_CANCELLED = 4;
    private static final int ORDER_TYPE_NORMAL = 1;
    private static final int ORDER_TYPE_SECKILL = 2;
    private static final int ORDER_TYPE_GROUP = 3;
    private static final int SECKILL_STATUS_UNPAID = 0;
    private static final int SECKILL_STATUS_PAID = 1;
    private static final int SECKILL_STATUS_CANCELLED = 2;
    private final OrderMapper orderMapper;
    private final OrderItemMapper orderItemMapper;
    private final CartItemMapper cartItemMapper;
    private final ProductMapper productMapper;
    private final ProductService productService;
    private final ProductCacheService productCacheService;
    private final RedisTemplate<String, Object> redisTemplate;
    private static final String ORDER_TOKEN_PREFIX = "shop:order:token:";
    private final DefaultRedisScript<Long> orderTokenScript;
    private final SeckillOrderMapper seckillOrderMapper;
    private final GroupBuyOrderMapper groupBuyOrderMapper;
    private final GroupBuyService groupBuyService;
    private final OrderExpireService orderExpireService;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long create(Long userId, OrderCreateDTO request) {
        //调用Lua获取token，消费token，删除token
        checkAndConsumeToken(userId,request.getToken());

        List<CartItem> cartItems = resolveCartItems(userId, request);
        if (cartItems.isEmpty()) {
            throw new RuntimeException("请选择要结算的商品");
        }
        List<OrderItem> orderItems = new ArrayList<>();
        BigDecimal totalAmount = BigDecimal.ZERO;

        List<ProductByQuantity> products = productService.getByIds(cartItems);
        for(ProductByQuantity pdByQua : products) {
            Product product =pdByQua.getProduct();
            Integer quantity=pdByQua.getQuantity();
            validateProductForOrder(product,quantity);
            BigDecimal price = BigDecimal.valueOf(product.getPrice());
            BigDecimal amount = price.multiply(BigDecimal.valueOf(quantity));
            totalAmount = totalAmount.add(amount);
            OrderItem orderItem = new OrderItem();
            orderItem.setProductId(product.getId());
            orderItem.setProductName(product.getName());
            orderItem.setProductImage(product.getImage());
            orderItem.setPrice(price);
            orderItem.setQuantity(quantity);
            orderItem.setAmount(amount);
            orderItems.add(orderItem);
        }
        for (CartItem cartItem : cartItems) {
            int rows = productMapper.decreaseStock(cartItem.getProductId(), cartItem.getQuantity());
            if (rows == 0) {
                throw new RuntimeException("商品库存不足或已下架");
            }
        }
        LocalDateTime now = LocalDateTime.now();
        ShopOrder order = new ShopOrder();
        order.setOrderNo(generateOrderNo(userId));
        order.setUserId(userId);
        order.setTotalAmount(totalAmount);
        order.setStatus(STATUS_UNPAID);
        order.setCreateTime(now);
        order.setUpdateTime(now);
        orderMapper.insert(order);
        for (OrderItem item : orderItems) {
            item.setOrderId(order.getId());
        }
        orderItemMapper.batchInsert(orderItems);
        List<Long> cartItemIds = cartItems.stream()
                .map(CartItem::getId)
                .toList();
        cartItemMapper.deleteByIds(cartItemIds, userId);
        productCacheService.clearAllProductListCache();

        return order.getId();
    }

    @Override
    public List<OrderVO> list(Long userId) {
        orderExpireService.sweepExpiredOrdersForUser(userId);

        List<ShopOrder> orders = orderMapper.listByUserId(userId);
        List<SeckillOrder> seckillOrders = seckillOrderMapper.listByUserId(userId);
        List<GroupBuyOrder> groupBuyOrders = groupBuyOrderMapper.listByUserId(userId);

        List<OrderVO> result = new ArrayList<>();

        // 普通订单
        if (!orders.isEmpty()) {
            List<Long> orderIds = orders.stream().map(ShopOrder::getId).toList();
            Map<Long, List<OrderItem>> itemMap = orderItemMapper.findByOrderIds(orderIds)
                    .stream().collect(Collectors.groupingBy(OrderItem::getOrderId));

            for (ShopOrder order : orders) {
                OrderVO vo = toOrderVO(order);
                vo.setOrderType(1);
                vo.setItems(toOrderItemVOList(itemMap.get(order.getId())));
                result.add(vo);
            }
        }

        // 秒杀订单
        for (SeckillOrder seckillOrder : seckillOrders) {
            result.add(secToOrderVO(seckillOrder));
        }

        for (GroupBuyOrder groupBuyOrder : groupBuyOrders) {
            result.add(groupBuyToOrderVO(groupBuyOrder));
        }

        //拼单订单

        // 按下单时间倒序
        result.sort((a, b) -> b.getCreateTime().compareTo(a.getCreateTime()));
        return result;
    }
    @Override
    public OrderDetailVO detail(Long userId, Long orderId, Integer orderType) {
        try {
            orderExpireService.expireUnpaidOrder(userId, orderId, orderType);
        } catch (RuntimeException ignored) {
            // 订单不存在或已处理时继续走详情查询
        }

        if (Integer.valueOf(ORDER_TYPE_SECKILL).equals(orderType)) {
            SeckillOrder seckillOrder = seckillOrderMapper.findByIdAndUserId(orderId, userId);
            if (seckillOrder != null) {
                return toSeckillDetailVO(seckillOrder);
            }
            throw new RuntimeException("订单不存在");
        }

        if (Integer.valueOf(ORDER_TYPE_GROUP).equals(orderType)) {
            GroupBuyOrder groupBuyOrder = groupBuyOrderMapper.findByIdAndUserId(orderId, userId);
            if (groupBuyOrder != null) {
                return toGroupBuyDetailVO(groupBuyOrder);
            }
            throw new RuntimeException("订单不存在");
        }

        if (!Integer.valueOf(ORDER_TYPE_SECKILL).equals(orderType)) {
            ShopOrder order = orderMapper.findByIdAndUserId(orderId, userId);
            if (order != null) {
                OrderDetailVO vo = new OrderDetailVO();
                vo.setId(order.getId());
                vo.setOrderNo(order.getOrderNo());
                vo.setUserId(order.getUserId());
                vo.setTotalAmount(order.getTotalAmount());
                vo.setStatus(order.getStatus());
                vo.setStatusText(statusText(order.getStatus()));
                vo.setCreateTime(order.getCreateTime());
                vo.setUpdateTime(order.getUpdateTime());
                vo.setOrderType(ORDER_TYPE_NORMAL);
                vo.setItems(toOrderItemVOList(orderItemMapper.findByOrderId(orderId)));
                return vo;
            }
        }

        SeckillOrder seckillOrder = seckillOrderMapper.findByIdAndUserId(orderId, userId);
        if (seckillOrder != null) {
            return toSeckillDetailVO(seckillOrder);
        }

        throw new RuntimeException("订单不存在");
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void cancel(Long userId, Long orderId, Integer orderType) {
        if (Integer.valueOf(ORDER_TYPE_SECKILL).equals(orderType)) {
            cancelSeckillOrder(userId, orderId);
            return;
        }

        if (Integer.valueOf(ORDER_TYPE_GROUP).equals(orderType)) {
            groupBuyService.cancelUnpaidOrder(userId, orderId);
            return;
        }

        ShopOrder order = orderMapper.findByIdAndUserId(orderId, userId);
        if (order != null) {
            if (!Integer.valueOf(STATUS_PAID).equals(order.getStatus())) {
                throw new RuntimeException("当前订单不可取消");
            }
            List<OrderItem> items = orderItemMapper.findByOrderId(orderId);
            for (OrderItem item : items) {
                productMapper.increaseStock(item.getProductId(), item.getQuantity());
            }
            int rows = orderMapper.updateStatus(orderId, userId, STATUS_CANCELLED);
            if (rows == 0) {
                throw new RuntimeException("取消订单失败");
            }
            productCacheService.clearAllProductListCache();
            return;
        }

        if (orderType == null) {
            cancelSeckillOrder(userId, orderId);
            return;
        }

        throw new RuntimeException("订单不存在");
    }

    @Override
    public String createToken(Long userId) {
        String token = java.util.UUID.randomUUID().toString().replace("-", "");
        String key = ORDER_TOKEN_PREFIX + userId + ":" + token;
        redisTemplate.opsForValue().set(key, "1", java.time.Duration.ofMinutes(5));
        return token;
    }

    @Override
    public void expireUnpaidOrder(Long userId, Long orderId, Integer orderType) {
        orderExpireService.expireUnpaidOrder(userId, orderId, orderType);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void pay(Long userId, Long orderId, Integer orderType) {
        if (Integer.valueOf(ORDER_TYPE_SECKILL).equals(orderType)) {
            paySeckillOrder(userId, orderId);
            return;
        }

        if (Integer.valueOf(ORDER_TYPE_GROUP).equals(orderType)) {
            groupBuyService.payOrder(userId, orderId);
            return;
        }

        ShopOrder order = orderMapper.findByIdAndUserId(orderId, userId);
        if (order != null) {
            if (!Integer.valueOf(STATUS_UNPAID).equals(order.getStatus())) {
                throw new RuntimeException("当前订单不可支付");
            }
            if (orderExpireService.isPaymentExpired(order.getCreateTime())) {
                orderExpireService.cancelExpiredOrder(orderId);
                throw new RuntimeException("订单已超时，请重新下单");
            }
            int rows = orderMapper.updateStatus(orderId, userId, STATUS_PAID);
            if (rows == 0) {
                throw new RuntimeException("支付失败");
            }
            return;
        }

        if (orderType == null) {
            paySeckillOrder(userId, orderId);
            return;
        }

        throw new RuntimeException("订单不存在");
    }

    private OrderVO groupBuyToOrderVO(GroupBuyOrder order) {
        OrderVO vo = new OrderVO();
        vo.setId(order.getId());
        vo.setOrderNo(order.getOrderNo());
        vo.setTotalAmount(order.getTotalAmount());
        vo.setStatus(order.getStatus());
        vo.setStatusText(groupBuyStatusText(order.getStatus()));
        vo.setCreateTime(order.getCreateTime());
        vo.setOrderType(ORDER_TYPE_GROUP);
        vo.setItems(List.of(toGroupBuyOrderItemVO(order)));
        return vo;
    }

    private OrderDetailVO toGroupBuyDetailVO(GroupBuyOrder order) {
        OrderDetailVO vo = new OrderDetailVO();
        vo.setId(order.getId());
        vo.setOrderNo(order.getOrderNo());
        vo.setUserId(order.getUserId());
        vo.setTotalAmount(order.getTotalAmount());
        vo.setStatus(order.getStatus());
        vo.setStatusText(groupBuyStatusText(order.getStatus()));
        vo.setCreateTime(order.getCreateTime());
        vo.setUpdateTime(order.getUpdateTime());
        vo.setOrderType(ORDER_TYPE_GROUP);
        vo.setItems(List.of(toGroupBuyOrderItemVO(order)));
        return vo;
    }

    private OrderItemVO toGroupBuyOrderItemVO(GroupBuyOrder order) {
        OrderItemVO item = new OrderItemVO();
        item.setId(order.getId());
        item.setProductId(order.getProductId());
        item.setProductName(order.getProductName());
        item.setProductImage(order.getProductImage());
        item.setPrice(order.getGroupPrice());
        item.setQuantity(order.getQuantity());
        item.setAmount(order.getTotalAmount());
        return item;
    }

    private void checkAndConsumeToken(Long userId, String token) {
        if (!StringUtils.hasText(token)) {
            throw new RuntimeException("请勿重复提交");
        }
        String key = ORDER_TOKEN_PREFIX + userId + ":" + token;
        Long result = redisTemplate.execute(orderTokenScript, List.of(key));
        if (result == null || result == 0L) {
            throw new RuntimeException("请勿重复提交");
        }
    }

    private List<CartItem> resolveCartItems(Long userId, OrderCreateDTO request) {
        if (!CollectionUtils.isEmpty(request.getCartItemIds())) {
            List<CartItem> list = cartItemMapper.findByIdsAndUserId(request.getCartItemIds(), userId);
            if (list.size() != request.getCartItemIds().size()) {
                throw new RuntimeException("存在无效的购物车项");
            }
            return list;
        }
        if (request.getProductId() != null) {
            Integer requestedQuantity = request.getQuantity();
            int quantity = requestedQuantity == null || requestedQuantity < 1 ? 1 : requestedQuantity;
            CartItem temp = new CartItem();
            temp.setProductId(request.getProductId());
            temp.setQuantity(quantity);
            return List.of(temp);
        }
        throw new RuntimeException("请选择要结算的商品");
    }
    private void validateProductForOrder(Product product, int quantity) {
        if (product.getStatus() == null || product.getStatus() != 1) {
            throw new RuntimeException("商品已下架：" + product.getName());
        }
        if (product.getStock() == null || product.getStock() < quantity) {
            throw new RuntimeException("库存不足：" + product.getName());
        }
        if (product.getPrice() == null || product.getPrice() <= 0) {
            throw new RuntimeException("商品价格异常：" + product.getName());
        }
    }
    private String generateOrderNo(Long userId) {
        String time = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMddHHmmss"));
        int random = ThreadLocalRandom.current().nextInt(1000, 9999);
        return time + userId + random;
    }
    private OrderVO toOrderVO(ShopOrder order) {
        OrderVO vo = new OrderVO();
        vo.setId(order.getId());
        vo.setOrderNo(order.getOrderNo());
        vo.setTotalAmount(order.getTotalAmount());
        vo.setStatus(order.getStatus());
        vo.setStatusText(statusText(order.getStatus()));
        vo.setCreateTime(order.getCreateTime());
        vo.setOrderType(1);
        return vo;
    }
    private OrderVO secToOrderVO(SeckillOrder seckillOrder) {
        OrderVO vo = new OrderVO();
        vo.setId(seckillOrder.getId());
        vo.setOrderNo(seckillOrder.getOrderNo());
        vo.setTotalAmount(seckillOrder.getTotalAmount());
        vo.setStatus(toDisplayStatus(ORDER_TYPE_SECKILL, seckillOrder.getStatus()));
        vo.setStatusText(seckillStatusText(seckillOrder.getStatus()));
        vo.setCreateTime(seckillOrder.getCreateTime());
        vo.setOrderType(ORDER_TYPE_SECKILL);
        vo.setItems(List.of(toSeckillOrderItemVO(seckillOrder)));
        return vo;
    }

    private OrderDetailVO toSeckillDetailVO(SeckillOrder seckillOrder) {
        OrderDetailVO vo = new OrderDetailVO();
        vo.setId(seckillOrder.getId());
        vo.setOrderNo(seckillOrder.getOrderNo());
        vo.setUserId(seckillOrder.getUserId());
        vo.setTotalAmount(seckillOrder.getTotalAmount());
        vo.setStatus(toDisplayStatus(ORDER_TYPE_SECKILL, seckillOrder.getStatus()));
        vo.setStatusText(seckillStatusText(seckillOrder.getStatus()));
        vo.setCreateTime(seckillOrder.getCreateTime());
        vo.setUpdateTime(seckillOrder.getUpdateTime());
        vo.setOrderType(ORDER_TYPE_SECKILL);
        vo.setItems(List.of(toSeckillOrderItemVO(seckillOrder)));
        return vo;
    }

    private OrderItemVO toSeckillOrderItemVO(SeckillOrder seckillOrder) {
        OrderItemVO item = new OrderItemVO();
        item.setId(seckillOrder.getId());
        item.setProductId(seckillOrder.getProductId());
        item.setProductName(seckillOrder.getProductName());
        item.setProductImage(seckillOrder.getProductImage());
        item.setPrice(seckillOrder.getSeckillPrice());
        item.setQuantity(seckillOrder.getQuantity());
        item.setAmount(seckillOrder.getTotalAmount());
        return item;
    }

    private void paySeckillOrder(Long userId, Long orderId) {
        SeckillOrder seckillOrder = seckillOrderMapper.findByIdAndUserId(orderId, userId);
        if (seckillOrder == null) {
            throw new RuntimeException("订单不存在");
        }
        if (!Integer.valueOf(SECKILL_STATUS_UNPAID).equals(seckillOrder.getStatus())) {
            throw new RuntimeException("当前订单不可支付");
        }
        if (orderExpireService.isPaymentExpired(seckillOrder.getCreateTime())) {
            orderExpireService.cancelExpiredSeckillOrder(seckillOrder.getId());
            throw new RuntimeException("订单已超时，请重新下单");
        }
        int rows = seckillOrderMapper.updateStatus(orderId, userId, SECKILL_STATUS_PAID);
        if (rows == 0) {
            throw new RuntimeException("支付失败");
        }
    }

    private void cancelSeckillOrder(Long userId, Long orderId) {
        SeckillOrder seckillOrder = seckillOrderMapper.findByIdAndUserId(orderId, userId);
        if (seckillOrder == null) {
            throw new RuntimeException("订单不存在");
        }
        Integer status = seckillOrder.getStatus();
        if (!Integer.valueOf(SECKILL_STATUS_UNPAID).equals(status)
                && !Integer.valueOf(SECKILL_STATUS_PAID).equals(status)) {
            throw new RuntimeException("当前订单不可取消");
        }
        restoreSeckillStock(seckillOrder.getProductId(), seckillOrder.getQuantity());
        int rows = seckillOrderMapper.updateStatus(orderId, userId, SECKILL_STATUS_CANCELLED);
        if (rows == 0) {
            throw new RuntimeException("取消订单失败");
        }
        productCacheService.clearAllProductListCache();
    }

    private void restoreSeckillStock(Long productId, Integer quantity) {
        if (productId == null || quantity == null || quantity <= 0) {
            return;
        }
        productMapper.increaseFlashStock(productId, quantity);
        redisTemplate.opsForValue().increment(seckillStockKey(productId), quantity);
    }

    private String seckillStockKey(Long productId) {
        return "shop:seckill:stock:" + productId;
    }

    private Integer toDisplayStatus(Integer orderType, Integer status) {
        if (Integer.valueOf(ORDER_TYPE_SECKILL).equals(orderType)
                && Integer.valueOf(SECKILL_STATUS_CANCELLED).equals(status)) {
            return STATUS_CANCELLED;
        }
        return status;
    }

    private String seckillStatusText(Integer status) {
        if (status == null) {
            return "未知";
        }
        return switch (status) {
            case 0 -> "待支付";
            case 1 -> "待发货";
            case 2 -> "已取消";
            case 3 -> "已完成";
            default -> "未知";
        };
    }

    private String groupBuyStatusText(Integer status) {
        if (status == null) {
            return "未知";
        }
        return switch (status) {
            case 0 -> "待支付";
            case 1 -> "已支付待成团";
            case 2 -> "已成团待发货";
            case 3 -> "已完成";
            case 4 -> "已取消";
            case 5 -> "未成团已退款";
            default -> "未知";
        };
    }
    private List<OrderItemVO> toOrderItemVOList(List<OrderItem> items) {
        if (items == null || items.isEmpty()) {
            return List.of();
        }
        List<OrderItemVO> list = new ArrayList<>();
        for (OrderItem item : items) {
            OrderItemVO vo = new OrderItemVO();
            vo.setId(item.getId());
            vo.setProductId(item.getProductId());
            vo.setProductName(item.getProductName());
            vo.setProductImage(item.getProductImage());
            vo.setPrice(item.getPrice());
            vo.setQuantity(item.getQuantity());
            vo.setAmount(item.getAmount());
            list.add(vo);
        }
        return list;
    }
    private String statusText(Integer status) {
        if (status == null) {
            return "未知";
        }
        return switch (status) {
            case 0 -> "待支付";
            case 1 -> "待发货";
            case 2 -> "已发货";
            case 3 -> "已完成";
            case 4 -> "已取消";
            default -> "未知";
        };
    }

}
