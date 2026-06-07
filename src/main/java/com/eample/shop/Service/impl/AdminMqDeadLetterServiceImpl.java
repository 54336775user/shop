package com.eample.shop.Service.impl;

import com.eample.shop.Service.AdminMqDeadLetterService;
import com.eample.shop.common.PageResult;
import com.eample.shop.entity.MqDeadLetter;
import com.eample.shop.mapper.MqDeadLetterMapper;
import com.eample.shop.vo.MqDeadLetterVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class AdminMqDeadLetterServiceImpl implements AdminMqDeadLetterService {

    private final MqDeadLetterMapper mqDeadLetterMapper;

    @Override
    public long pendingCount() {
        return mqDeadLetterMapper.countByStatus(0);
    }

    @Override
    public PageResult<MqDeadLetterVO> list(Integer status, Integer page, Integer size) {
        int currentPage = (page == null || page < 1) ? 1 : page;
        int pageSize = (size == null || size < 1) ? 10 : size;
        int offset = (currentPage - 1) * pageSize;

        Integer queryStatus = status == null ? 0 : status;
        long total = mqDeadLetterMapper.countByStatus(queryStatus);
        List<MqDeadLetter> records = mqDeadLetterMapper.listByStatus(queryStatus, offset, pageSize);

        List<MqDeadLetterVO> voList = records.stream().map(this::toVO).toList();
        return PageResult.of(voList, total, currentPage, pageSize);
    }

    @Override
    public MqDeadLetterVO detail(Long id) {
        MqDeadLetter record = mqDeadLetterMapper.findById(id);
        if (record == null) {
            log.info("死信队列不存在");
            throw new RuntimeException("死信记录不存在");
        }
        return toVO(record);
    }

    @Override
    public void markDone(Long id, Long adminId, String remark) {
        MqDeadLetter record = mqDeadLetterMapper.findById(id);
        if (record == null) {
            log.info("死信队列不存在");
            throw new RuntimeException("死信记录不存在");
        }

        String finalRemark = StringUtils.hasText(remark) ? remark.trim() : "已人工处理";
        log.info("已人工处理");
        int rows = mqDeadLetterMapper.markDone(id, adminId, finalRemark);
        if (rows == 0) {
            throw new RuntimeException("记录状态不可处理");
        }
    }

    private MqDeadLetterVO toVO(MqDeadLetter record) {
        MqDeadLetterVO vo = new MqDeadLetterVO();
        vo.setId(record.getId());
        vo.setBizType(record.getBizType());
        vo.setRequestId(record.getRequestId());
        vo.setUserId(record.getUserId());
        vo.setProductId(record.getProductId());
        vo.setMessageBody(record.getMessageBody());
        vo.setFailReason(record.getFailReason());
        vo.setRetryCount(record.getRetryCount());
        vo.setStatus(record.getStatus());
        vo.setStatusText(toStatusText(record.getStatus()));
        vo.setHandlerAdminId(record.getHandlerAdminId());
        vo.setHandleRemark(record.getHandleRemark());
        vo.setCreateTime(record.getCreateTime());
        vo.setHandleTime(record.getHandleTime());
        return vo;
    }

    private String toStatusText(Integer status) {
        if (status == null) {
            return "未知";
        }
        return switch (status) {
            case 0 -> "待处理";
            case 1 -> "处理中";
            case 2 -> "已处理";
            default -> "未知";
        };
    }
}
