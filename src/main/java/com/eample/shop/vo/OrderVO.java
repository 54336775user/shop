package com.eample.shop.vo;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Data
public class OrderVO {
    private Long id;
    private String orderNo;
    private BigDecimal totalAmount;
    private Integer status;
    /** 状态文案，如：已支付、已取消 */
    private String statusText;
    private LocalDateTime createTime;
    /** 列表页可只返回前几条明细，详情页再查全量 */
    private List<OrderItemVO> items;
    private Integer orderType;
}
