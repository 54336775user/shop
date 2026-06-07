package com.eample.shop.vo;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
public class SeckillOrderVO {
    private Long id;
    private String orderNo;
    private Long productId;
    private String productName;
    private String productImage;
    private BigDecimal seckillPrice;
    private Integer quantity;
    private BigDecimal totalAmount;
    private Integer status;
    private String statusText;
    private String message;
    private Long orderId;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}
