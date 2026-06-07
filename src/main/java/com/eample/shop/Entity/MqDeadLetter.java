package com.eample.shop.entity;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class MqDeadLetter {
    private Long id;
    private String bizType;
    private String requestId;
    private Long userId;
    private Long productId;
    private String messageBody;
    private String failReason;
    private Integer retryCount;
    /**
     * 0待处理 1处理中 2已处理
     */
    private Integer status;
    private Long handlerAdminId;
    private String handleRemark;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
    private LocalDateTime handleTime;
}
