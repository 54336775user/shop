package com.eample.shop.dto;

import lombok.Data;

@Data
public class CartAddDTO {
    private Long productId;
    /** 默认 1，Service 里做 null/<=0 校验 */
    private Integer quantity;
}