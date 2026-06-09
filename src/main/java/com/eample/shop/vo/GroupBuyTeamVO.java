package com.eample.shop.vo;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class GroupBuyTeamVO {
    private Long id;
    private String teamNo;
    private Long activityId;
    private Long productId;
    private Long leaderUserId;
    private String leaderNickname;
    private String leaderAvatar;
    private Integer requiredSize;
    private Integer paidSize;
    private Integer remainSize;
    private Integer status;
    private String statusText;
    private LocalDateTime expireTime;
    private Long remainSeconds;
}
