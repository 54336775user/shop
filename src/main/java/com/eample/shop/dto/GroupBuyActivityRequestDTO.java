package com.eample.shop.dto;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
public class GroupBuyActivityRequestDTO {
    private Long id;
    private Long productId;
    private BigDecimal groupPrice;
    private Integer requiredSize;
    private Integer durationHours;
    private Integer stock;
    private Integer status;
    private LocalDateTime startTime;
    private LocalDateTime endTime;
}
