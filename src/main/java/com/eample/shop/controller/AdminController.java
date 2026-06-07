package com.eample.shop.controller;


import com.eample.shop.Service.AdminService;
import com.eample.shop.common.Result;
import com.eample.shop.dto.LoginRequestDTO;
import com.eample.shop.dto.RegisterRequestDTO;
import com.eample.shop.vo.LoginVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jspecify.annotations.Nullable;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/admin")
@RequiredArgsConstructor
@Slf4j
public class AdminController {
    private final AdminService adminService;

    @PostMapping("/register")
    public Result<Void> register(@RequestBody RegisterRequestDTO request) {
        log.info("管理员注册：{}", request.getUsername());
        adminService.register(request);
        return Result.ok("注册成功",null);
    }

    @PostMapping("/login")
    public Result<LoginVO> login(@RequestBody LoginRequestDTO request) {
        log.info("管理员登录：{}", request.getUsername());
        LoginVO login = adminService.login(request);
        log.info("JWT验证：{}", login.getToken());
        return Result.ok("登录成功",login);
    }

    @GetMapping("/info")
    public Result<Map<String, Object>> info() {
        @Nullable Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        return Result.ok(Map.of(
                "userId", auth.getPrincipal(),
                "username", auth.getDetails(),
                "authorities", auth.getAuthorities()
        ));
    }
}
