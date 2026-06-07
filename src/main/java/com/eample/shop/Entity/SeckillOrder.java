package com.eample.shop.entity;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
public class SeckillOrder {
    private Long id;
    private String orderNo;
    private Long userId;
    private Long productId;
    private String productName;
    private String productImage;
    private BigDecimal seckillPrice;
    private Integer quantity;
    private BigDecimal totalAmount;
    /**
     * 0待支付 1已支付 2已取消 3已完成
     */
    private Integer status;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}