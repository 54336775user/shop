package com.eample.shop.entity;

import lombok.Data;

@Data
public class ProductByQuantity {
    private Product product;
    private int quantity;
}
