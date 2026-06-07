package com.eample.shop.vo;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
public class SeckillProductVO {
    private Long id;
    private String name;
    private String image;
    private String description;
    private Double originalPrice;
    private BigDecimal flashPrice;
    private Integer stock;
    private Integer flashStock;
    private Integer progress;
    private LocalDateTime flashStartTime;
    private LocalDateTime flashEndTime;
    private Integer status;
    private String statusText;
}
