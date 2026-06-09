package com.eample.shop.entity;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
public class GroupBuyOrder {
    private Long id;
    private String orderNo;
    private Long userId;
    private Long teamId;
    private Long activityId;
    private Long productId;
    private String productName;
    private String productImage;
    private BigDecimal groupPrice;
    private Integer quantity;
    private BigDecimal totalAmount;
    private Integer status;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}
