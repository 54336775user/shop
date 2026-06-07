package com.eample.shop.dto;

import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
public class ProductRequestDTO {
    private Long id;
    private String name;
    private Long categoryId;
    private Double price;
    private Integer stock;
    private String image;
    private String description;
    private Integer status;
    private Integer isFlashSale;
    private Double flashPrice;
    private Integer flashStock;
    private LocalDateTime flashStartTime;
    private LocalDateTime flashEndTime;
}
