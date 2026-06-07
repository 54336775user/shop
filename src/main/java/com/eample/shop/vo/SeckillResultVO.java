package com.eample.shop.vo;

import lombok.Data;

@Data
public class SeckillResultVO {
    private Integer status;
    private String statusText;
    private Long orderId;
    private String message;
}