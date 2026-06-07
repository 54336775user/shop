package com.eample.shop.entity;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
public class ShopOrder {
    /** 主键 */
    private Long id;
    /** 业务订单号 */
    private String orderNo;
    private Long userId;
    /** 订单总金额 */
    private BigDecimal totalAmount;
    /**
     * 0待支付 1已支付/待发货 2已发货 3已完成 4已取消
     * MVP 创建时可先设为 1
     */
    private Integer status;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}
