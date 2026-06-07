package com.eample.shop.mapper;

import com.eample.shop.entity.OrderItem;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface OrderItemMapper {

    int batchInsert(@Param("list") List<OrderItem> list);

    List<OrderItem> findByOrderId(@Param("orderId") Long orderId);

    List<OrderItem> findByOrderIds(@Param("orderIds") List<Long> orderIds);
}
