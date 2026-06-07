package com.eample.shop.dto;

import lombok.Data;

@Data
public class CategoryRequestDTO {
    private Long id;      // 修改时用
    private String name;
    private Integer sort;
    private Integer status;
}
