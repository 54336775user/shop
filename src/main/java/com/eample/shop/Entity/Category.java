package com.eample.shop.entity;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class Category {
    private Long id;
    private String name;
    private Long sort;
    private Integer status;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}
