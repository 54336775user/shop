package com.eample.shop.vo;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
public class CartItemVO {
    private Long cartItemId;
    private Long productId;
    private String productName;
    private String productImage;
    /** 当前商品售价（普通价，非秒杀价） */
    private BigDecimal price;
    private Integer quantity;
    /** 当前商品库存 */
    private Integer stock;
    /** 是否上架：1上架 0下架 */
    private Integer status;
    /** 小计 */
    private BigDecimal subtotal;
    /** 是否可下单（上架且有库存） */
    private Boolean selectable;
    private LocalDateTime createTime;
}
