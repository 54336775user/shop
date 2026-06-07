package com.eample.shop.dto;

import lombok.Data;

@Data
public class CartUpdateDTO {
    private Long cartItemId;
    private Integer quantity;
}
