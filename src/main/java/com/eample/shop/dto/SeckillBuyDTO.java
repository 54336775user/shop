package com.eample.shop.dto;

import lombok.Data;

@Data
public class SeckillBuyDTO {
    /**
     * 前端防重复提交 token
     */
    private String token;

    /**
     * 购买数量，第一版建议只允许 1
     */
    private Integer quantity;
}
