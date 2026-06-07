package com.eample.shop.dto;

import lombok.Data;

@Data
public class SeckillMessage {
    private Long userId;
    private Long productId;
    private Integer quantity;
    private String requestId;
}