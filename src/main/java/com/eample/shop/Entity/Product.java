package com.eample.shop.entity;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class Product {
    private Long id;
    private String name;
    private Integer categoryId;
    private Double price;
    private Integer stock;
    private String image;
    private String description;
    private Integer status;      //1上架，0下架
    private Integer isFlashSale;    // 1是 0否
    private Double flashPrice;
    private Integer flashStock;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
    private LocalDateTime flashStartTime;
    private LocalDateTime flashEndTime;
}
