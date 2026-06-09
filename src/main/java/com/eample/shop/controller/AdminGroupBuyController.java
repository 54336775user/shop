package com.eample.shop.controller;

import com.eample.shop.Service.GroupBuyService;
import com.eample.shop.common.PageResult;
import com.eample.shop.common.Result;
import com.eample.shop.dto.GroupBuyActivityRequestDTO;
import com.eample.shop.vo.GroupBuyActivityAdminVO;
import com.eample.shop.vo.GroupBuyActivityVO;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/admin/group-buy")
@RequiredArgsConstructor
public class AdminGroupBuyController {

    private final GroupBuyService groupBuyService;

    /**
     * 拼团活动列表
     */
    @GetMapping("/list")
    public Result<PageResult<GroupBuyActivityAdminVO>> list(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) Long productId,
            @RequestParam(required = false) Integer status,
            @RequestParam(required = false, defaultValue = "1") Integer page,
            @RequestParam(required = false, defaultValue = "10") Integer size) {
        return Result.ok(groupBuyService.adminList(keyword, productId, status, page, size));
    }

    /**
     * 新增拼团活动
     */
    @PostMapping("/add")
    public Result<Void> add(@RequestBody GroupBuyActivityRequestDTO request) {
        groupBuyService.addActivity(request);
        return Result.ok("新增成功", null);
    }

    /**
     * 修改拼团活动
     */
    @PutMapping("/update")
    public Result<Void> update(@RequestBody GroupBuyActivityRequestDTO request) {
        groupBuyService.updateActivity(request);
        return Result.ok("修改成功", null);
    }

    /**
     * 上架拼团活动
     */
    @PutMapping("/enable/{id}")
    public Result<Void> enable(@PathVariable Long id) {
        groupBuyService.enableActivity(id);
        return Result.ok("上架成功", null);
    }

    /**
     * 下架拼团活动
     */
    @PutMapping("/disable/{id}")
    public Result<Void> disable(@PathVariable Long id) {
        groupBuyService.disableActivity(id);
        return Result.ok("下架成功", null);
    }

    /**
     * 查询单个拼团活动
     */
    @GetMapping("/{id}")
    public Result<GroupBuyActivityVO> detail(@PathVariable Long id) {
        return Result.ok(groupBuyService.getActivityById(id));
    }
}
