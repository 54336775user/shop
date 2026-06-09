package com.eample.shop.entity;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class GroupBuyTeam {
    private Long id;
    private String teamNo;
    private Long activityId;
    private Long productId;
    private Long leaderUserId;
    private Integer requiredSize;
    private Integer paidSize;
    private Integer status;
    private LocalDateTime expireTime;
    private LocalDateTime successTime;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}