package com.eample.shop.entity;

import lombok.Data;

import java.math.BigDecimal;

@Data
public class OrderItem {
    private Long id;
    private Long orderId;
    private Long productId;
    /** 下单时商品名称快照 */
    private String productName;
    private String productImage;
    /** 下单时单价快照 */
    private BigDecimal price;
    private Integer quantity;
    /** 小计 = price * quantity */
    private BigDecimal amount;
}
