package com.eample.shop.vo;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class MqDeadLetterVO {
    private Long id;
    private String bizType;
    private String requestId;
    private Long userId;
    private Long productId;
    private String messageBody;
    private String failReason;
    private Integer retryCount;
    private Integer status;
    private String statusText;
    private Long handlerAdminId;
    private String handleRemark;
    private LocalDateTime createTime;
    private LocalDateTime handleTime;
}
