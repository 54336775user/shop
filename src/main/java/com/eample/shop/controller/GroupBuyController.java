package com.eample.shop.controller;

import com.eample.shop.Service.GroupBuyService;
import com.eample.shop.common.Result;
import com.eample.shop.vo.GroupBuyActivityVO;
import com.eample.shop.vo.GroupBuyTeamVO;
import lombok.RequiredArgsConstructor;
import org.jspecify.annotations.Nullable;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/group-buy")
@RequiredArgsConstructor
public class GroupBuyController {

    private final GroupBuyService groupBuyService;

    @GetMapping("/activity/{productId}")
    public Result<GroupBuyActivityVO> activity(@PathVariable Long productId) {
        return Result.ok(groupBuyService.getActivityByProductId(productId));
    }

    @GetMapping("/product/{productId}/teams")
    public Result<List<GroupBuyTeamVO>> teams(@PathVariable Long productId) {
        return Result.ok(groupBuyService.listTeamsByProductId(productId));
    }

    @GetMapping("/team/{teamId}")
    public Result<GroupBuyTeamVO> team(@PathVariable Long teamId) {
        return Result.ok(groupBuyService.getTeamDetail(teamId));
    }

    @PostMapping("/activity/{activityId}/open")
    public Result<Map<String, Long>> open(@PathVariable Long activityId) {
        Long userId = currentUserId();
        Long orderId = groupBuyService.openTeam(userId, activityId);
        return Result.ok("拼团已创建", Map.of("orderId", orderId));
    }

    @PostMapping("/team/{teamId}/join")
    public Result<Map<String, Long>> join(@PathVariable Long teamId) {
        Long userId = currentUserId();
        Long orderId = groupBuyService.joinTeam(userId, teamId);
        return Result.ok("已加入拼团", Map.of("orderId", orderId));
    }

    private Long currentUserId() {
        @Nullable Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        return (Long) auth.getPrincipal();
    }
}
