package com.eample.shop.Service.impl;

import com.eample.shop.Service.AdminOrderService;
import com.eample.shop.cache.ProductCacheService;
import com.eample.shop.common.PageResult;
import com.eample.shop.entity.OrderItem;
import com.eample.shop.entity.Product;
import com.eample.shop.entity.ShopOrder;
import com.eample.shop.mapper.OrderItemMapper;
import com.eample.shop.mapper.OrderMapper;
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

    private final OrderMapper orderMapper;
    private final OrderItemMapper orderItemMapper;
    private final ProductMapper productMapper;
    private final ProductCacheService productCacheService;

    @Override
    public PageResult<OrderVO> list(String keyword, Integer status, Integer page, Integer size) {
        int currentPage = (page == null || page < 1) ? 1 : page;
        int pageSize = (size == null || size < 1) ? 10 : size;
        int offset = (currentPage - 1) * pageSize;

        String realKeyword = null;
        if (StringUtils.hasText(keyword)) {
            realKeyword = keyword.trim();
        }

        long total = orderMapper.countAdminSearch(realKeyword, status);
        List<ShopOrder> orders = orderMapper.adminSearch(realKeyword, status, offset, pageSize);

        if (orders.isEmpty()) {
            return PageResult.of(List.of(), total, currentPage, pageSize);
        }

        List<Long> orderIds = orders.stream()
                .map(ShopOrder::getId)
                .toList();

        List<OrderItem> allItems = orderItemMapper.findByOrderIds(orderIds);
        Map<Long, List<OrderItem>> itemMap = allItems.stream()
                .collect(Collectors.groupingBy(OrderItem::getOrderId));

        List<OrderVO> result = new ArrayList<>();
        for (ShopOrder order : orders) {
            OrderVO vo = toOrderVO(order);
            vo.setItems(toOrderItemVOList(itemMap.get(order.getId())));
            result.add(vo);
        }

        return PageResult.of(result, total, currentPage, pageSize);
    }

    @Override
    public OrderDetailVO detail(Long id) {
        ShopOrder order = orderMapper.findById(id);
        if (order == null) {
            throw new RuntimeException("订单不存在");
        }

        OrderDetailVO vo = new OrderDetailVO();
        vo.setId(order.getId());
        vo.setOrderNo(order.getOrderNo());
        vo.setUserId(order.getUserId());
        vo.setTotalAmount(order.getTotalAmount());
        vo.setStatus(order.getStatus());
        vo.setStatusText(statusText(order.getStatus()));
        vo.setCreateTime(order.getCreateTime());
        vo.setUpdateTime(order.getUpdateTime());
        vo.setItems(toOrderItemVOList(orderItemMapper.findByOrderId(id)));
        return vo;
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

    private OrderVO toOrderVO(ShopOrder order) {
        OrderVO vo = new OrderVO();
        vo.setId(order.getId());
        vo.setOrderNo(order.getOrderNo());
        vo.setTotalAmount(order.getTotalAmount());
        vo.setStatus(order.getStatus());
        vo.setStatusText(statusText(order.getStatus()));
        vo.setCreateTime(order.getCreateTime());
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
}
