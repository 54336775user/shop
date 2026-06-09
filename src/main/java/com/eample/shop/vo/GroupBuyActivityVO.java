package com.eample.shop.vo;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
public class GroupBuyActivityVO {
    private Long id;
    private Long productId;
    private BigDecimal groupPrice;
    private Integer requiredSize;
    private Integer durationHours;
    private Integer stock;
    private Integer status;
    private LocalDateTime startTime;
    private LocalDateTime endTime;
    private String statusText;
}
