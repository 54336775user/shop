package com.eample.shop.entity;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class GroupBuyMember {
    private Long id;
    private Long teamId;
    private Long userId;
    private Long orderId;
    private Integer role;
    private Integer status;
    private LocalDateTime joinTime;
    private LocalDateTime payTime;
}
