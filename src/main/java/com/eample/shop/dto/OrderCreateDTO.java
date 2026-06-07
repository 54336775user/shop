package com.eample.shop.dto;

import lombok.Data;

import java.util.List;

@Data
public class OrderCreateDTO {
    /**
     * 从购物车结算：传勾选的 cart_item.id 列表
     */
    private List<Long> cartItemIds;

    /**
     * 可选：直接购买（不做购物车时再用）
     * 与 cartItemIds 二选一，Service 里校验
     */
    private Long productId;
    private Integer quantity;
    /**
     * 前端带的验证token
     */
    private String token;
}
