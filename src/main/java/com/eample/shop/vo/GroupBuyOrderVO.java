package com.eample.shop.vo;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Data
public class GroupBuyOrderVO {
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
    private String statusText;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
    private Integer orderType;
    private List<OrderItemVO> items;
}
