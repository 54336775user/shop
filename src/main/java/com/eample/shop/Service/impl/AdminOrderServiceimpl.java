package com.eample.shop.Service.impl;

import com.eample.shop.Service.AdminOrderService;
import com.eample.shop.cache.ProductCacheService;
import com.eample.shop.common.PageResult;
import com.eample.shop.entity.OrderItem;
import com.eample.shop.entity.GroupBuyOrder;
import com.eample.shop.entity.Product;
import com.eample.shop.entity.SeckillOrder;
import com.eample.shop.entity.ShopOrder;
import com.eample.shop.mapper.OrderItemMapper;
import com.eample.shop.mapper.OrderMapper;
import com.eample.shop.mapper.GroupBuyOrderMapper;
import com.eample.shop.mapper.SeckillOrderMapper;
import com.eample.shop.mapper.ProductMapper;
import com.eample.shop.vo.OrderDetailVO;
import com.eample.shop.vo.OrderItemVO;
import com.eample.shop.vo.OrderVO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class AdminOrderServiceimpl implements AdminOrderService {

    private static final int STATUS_CANCELLED = 4;
    private static final int ORDER_TYPE_NORMAL = 1;
    private static final int ORDER_TYPE_SECKILL = 2;
    private static final int ORDER_TYPE_GROUP = 3;

    private final OrderMapper orderMapper;
    private final OrderItemMapper orderItemMapper;
    private final SeckillOrderMapper seckillOrderMapper;
    private final GroupBuyOrderMapper groupBuyOrderMapper;
    private final ProductMapper productMapper;
    private final ProductCacheService productCacheService;

    @Override
    public PageResult<OrderVO> list(String keyword, Integer status, Integer page, Integer size) {
        int currentPage = (page == null || page < 1) ? 1 : page;
        int pageSize = (size == null || size < 1) ? 10 : size;

        String realKeyword = null;
        if (StringUtils.hasText(keyword)) {
            realKeyword = keyword.trim();
        }

        List<OrderVO> result = new ArrayList<>();
        result.addAll(loadNormalOrders(realKeyword, status));
        result.addAll(loadSeckillOrders(realKeyword, status));
        result.addAll(loadGroupBuyOrders(realKeyword, status));
        result.sort((a, b) -> b.getCreateTime().compareTo(a.getCreateTime()));

        long total = result.size();
        int fromIndex = Math.min((currentPage - 1) * pageSize, result.size());
        int toIndex = Math.min(fromIndex + pageSize, result.size());
        List<OrderVO> pageList = fromIndex >= toIndex ? List.of() : new ArrayList<>(result.subList(fromIndex, toIndex));
        return PageResult.of(pageList, total, currentPage, pageSize);
    }

    @Override
    public OrderDetailVO detail(Long id, Integer orderType) {
        if (Integer.valueOf(ORDER_TYPE_SECKILL).equals(orderType)) {
            SeckillOrder seckillOrder = seckillOrderMapper.findById(id);
            if (seckillOrder == null) {
                throw new RuntimeException("订单不存在");
            }
            return toSeckillDetailVO(seckillOrder);
        }

        if (Integer.valueOf(ORDER_TYPE_GROUP).equals(orderType)) {
            GroupBuyOrder groupBuyOrder = groupBuyOrderMapper.findById(id);
            if (groupBuyOrder == null) {
                throw new RuntimeException("订单不存在");
            }
            return toGroupBuyDetailVO(groupBuyOrder);
        }

        ShopOrder order = orderMapper.findById(id);
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
            vo.setItems(toOrderItemVOList(orderItemMapper.findByOrderId(id)));
            return vo;
        }

        SeckillOrder seckillOrder = seckillOrderMapper.findById(id);
        if (seckillOrder != null) {
            return toSeckillDetailVO(seckillOrder);
        }

        GroupBuyOrder groupBuyOrder = groupBuyOrderMapper.findById(id);
        if (groupBuyOrder != null) {
            return toGroupBuyDetailVO(groupBuyOrder);
        }

        throw new RuntimeException("订单不存在");
    }

    @Override
    public void updateStatus(Long id, Integer status) {
        if (id == null) {
            throw new RuntimeException("订单 id 不能为空");
        }
        if (status == null) {
            throw new RuntimeException("订单状态不能为空");
        }

        ShopOrder order = orderMapper.findById(id);
        if (order == null) {
            throw new RuntimeException("订单不存在");
        }

        int rows = orderMapper.updateStatus(id, order.getUserId(), status);
        if (rows == 0) {
            throw new RuntimeException("修改订单状态失败");
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void cancel(Long id) {
        ShopOrder order = orderMapper.findById(id);
        if (order == null) {
            throw new RuntimeException("订单不存在");
        }
        if (order.getStatus() != null && order.getStatus() == STATUS_CANCELLED) {
            throw new RuntimeException("订单已取消");
        }

        List<OrderItem> items = orderItemMapper.findByOrderId(id);
        for (OrderItem item : items) {
            productMapper.increaseStock(item.getProductId(), item.getQuantity());
        }

        int rows = orderMapper.updateStatus(id, order.getUserId(), STATUS_CANCELLED);
        if (rows == 0) {
            throw new RuntimeException("取消订单失败");
        }

        productCacheService.clearAllProductListCache();
    }

    private List<OrderVO> loadNormalOrders(String realKeyword, Integer status) {
        long total = orderMapper.countAdminSearch(realKeyword, status);
        if (total <= 0) {
            return List.of();
        }
        int fetchSize = (int) Math.min(total, Integer.MAX_VALUE);
        List<ShopOrder> orders = orderMapper.adminSearch(realKeyword, status, 0, fetchSize);
        if (orders.isEmpty()) {
            return List.of();
        }

        List<Long> orderIds = orders.stream().map(ShopOrder::getId).toList();
        Map<Long, List<OrderItem>> itemMap = orderItemMapper.findByOrderIds(orderIds)
                .stream().collect(Collectors.groupingBy(OrderItem::getOrderId));

        List<OrderVO> result = new ArrayList<>();
        for (ShopOrder order : orders) {
            OrderVO vo = toOrderVO(order);
            vo.setItems(toOrderItemVOList(itemMap.get(order.getId())));
            result.add(vo);
        }
        return result;
    }

    private List<OrderVO> loadSeckillOrders(String realKeyword, Integer status) {
        long total = seckillOrderMapper.countAdminSearch(realKeyword, status);
        if (total <= 0) {
            return List.of();
        }
        int fetchSize = (int) Math.min(total, Integer.MAX_VALUE);
        List<SeckillOrder> orders = seckillOrderMapper.adminSearch(realKeyword, status, 0, fetchSize);
        if (orders.isEmpty()) {
            return List.of();
        }

        List<OrderVO> result = new ArrayList<>();
        for (SeckillOrder order : orders) {
            result.add(toSeckillOrderVO(order));
        }
        return result;
    }

    private List<OrderVO> loadGroupBuyOrders(String realKeyword, Integer status) {
        long total = groupBuyOrderMapper.countAdminSearch(realKeyword, status);
        if (total <= 0) {
            return List.of();
        }
        int fetchSize = (int) Math.min(total, Integer.MAX_VALUE);
        List<GroupBuyOrder> orders = groupBuyOrderMapper.adminSearch(realKeyword, status, 0, fetchSize);
        if (orders.isEmpty()) {
            return List.of();
        }

        List<OrderVO> result = new ArrayList<>();
        for (GroupBuyOrder order : orders) {
            result.add(toGroupBuyOrderVO(order));
        }
        return result;
    }

    private OrderVO toOrderVO(ShopOrder order) {
        OrderVO vo = new OrderVO();
        vo.setId(order.getId());
        vo.setOrderNo(order.getOrderNo());
        vo.setTotalAmount(order.getTotalAmount());
        vo.setStatus(order.getStatus());
        vo.setStatusText(statusText(order.getStatus()));
        vo.setCreateTime(order.getCreateTime());
        vo.setOrderType(ORDER_TYPE_NORMAL);
        return vo;
    }

    private OrderVO toSeckillOrderVO(SeckillOrder order) {
        OrderVO vo = new OrderVO();
        vo.setId(order.getId());
        vo.setOrderNo(order.getOrderNo());
        vo.setTotalAmount(order.getTotalAmount());
        vo.setStatus(order.getStatus());
        vo.setStatusText(seckillStatusText(order.getStatus()));
        vo.setCreateTime(order.getCreateTime());
        vo.setOrderType(ORDER_TYPE_SECKILL);
        vo.setItems(List.of(toSeckillOrderItemVO(order)));
        return vo;
    }

    private OrderVO toGroupBuyOrderVO(GroupBuyOrder order) {
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

    private OrderDetailVO toSeckillDetailVO(SeckillOrder order) {
        OrderDetailVO vo = new OrderDetailVO();
        vo.setId(order.getId());
        vo.setOrderNo(order.getOrderNo());
        vo.setUserId(order.getUserId());
        vo.setTotalAmount(order.getTotalAmount());
        vo.setStatus(order.getStatus());
        vo.setStatusText(seckillStatusText(order.getStatus()));
        vo.setCreateTime(order.getCreateTime());
        vo.setUpdateTime(order.getUpdateTime());
        vo.setOrderType(ORDER_TYPE_SECKILL);
        vo.setItems(List.of(toSeckillOrderItemVO(order)));
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

    private OrderItemVO toSeckillOrderItemVO(SeckillOrder order) {
        OrderItemVO vo = new OrderItemVO();
        vo.setId(order.getId());
        vo.setProductId(order.getProductId());
        vo.setProductName(order.getProductName());
        vo.setProductImage(order.getProductImage());
        vo.setPrice(order.getSeckillPrice());
        vo.setQuantity(order.getQuantity());
        vo.setAmount(order.getTotalAmount());
        return vo;
    }

    private OrderItemVO toGroupBuyOrderItemVO(GroupBuyOrder order) {
        OrderItemVO vo = new OrderItemVO();
        vo.setId(order.getId());
        vo.setProductId(order.getProductId());
        vo.setProductName(order.getProductName());
        vo.setProductImage(order.getProductImage());
        vo.setPrice(order.getGroupPrice());
        vo.setQuantity(order.getQuantity());
        vo.setAmount(order.getTotalAmount());
        return vo;
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
}
