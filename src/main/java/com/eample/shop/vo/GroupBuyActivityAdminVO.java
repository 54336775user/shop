package com.eample.shop.vo;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
public class GroupBuyActivityAdminVO {
    private Long id;
    private Long productId;
    private String productName;
    private String categoryName;
    private BigDecimal productPrice;
    private BigDecimal groupPrice;
    private Integer requiredSize;
    private Integer durationHours;
    private Integer stock;
    private Integer status;
    private String statusText;
    private LocalDateTime startTime;
    private LocalDateTime endTime;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}
