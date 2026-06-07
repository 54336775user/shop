package com.eample.shop.controller;

import com.eample.shop.Service.AdminMqDeadLetterService;
import com.eample.shop.common.PageResult;
import com.eample.shop.common.Result;
import com.eample.shop.vo.MqDeadLetterVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/admin/mq/dead-letter")
@RequiredArgsConstructor
@Slf4j
public class AdminMqDeadLetterController {

    private final AdminMqDeadLetterService adminMqDeadLetterService;

    /**
     * 待处理数量（红点）
     */
    @GetMapping("/pending-count")
    public Result<Map<String, Long>> pendingCount() {
        long count = adminMqDeadLetterService.pendingCount();
        return Result.ok(Map.of("count", count));
    }

    /**
     * 死信列表
     * status: 0待处理 1处理中 2已处理
     */
    @GetMapping("/list")
    public Result<PageResult<MqDeadLetterVO>> list(
            @RequestParam(required = false, defaultValue = "0") Integer status,
            @RequestParam(required = false, defaultValue = "1") Integer page,
            @RequestParam(required = false, defaultValue = "10") Integer size) {
        return Result.ok(adminMqDeadLetterService.list(status, page, size));
    }

    /**
     * 死信详情
     */
    @GetMapping("/{id}")
    public Result<MqDeadLetterVO> detail(@PathVariable Long id) {
        return Result.ok(adminMqDeadLetterService.detail(id));
    }

    /**
     * 标记已处理
     */
    @PutMapping("/{id}/done")
    public Result<Void> markDone(@PathVariable Long id,
                                 @RequestParam(required = false) String remark) {
        log.info("标记已处理死信队列");
        Long adminId = currentAdminId();
        adminMqDeadLetterService.markDone(id, adminId, remark);
        return Result.ok("已标记处理完成", null);
    }

    private Long currentAdminId() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        return (Long) auth.getPrincipal();
    }
}
