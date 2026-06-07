package com.eample.shop.controller;

import com.eample.shop.Service.UserService;
import com.eample.shop.common.Result;
import com.eample.shop.dto.LoginRequestDTO;
import com.eample.shop.dto.RegisterRequestDTO;
import com.eample.shop.vo.LoginVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;


//
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.GetMapping;

import java.util.Map;

@RestController
@RequestMapping("/api/user")
@RequiredArgsConstructor
@Slf4j
public class UserController {
    private final UserService userService;

    @PostMapping("/register")
    public Result<Void> register(@RequestBody RegisterRequestDTO request) {
        log.info("用户注册：{}", request.getUsername());
        userService.register(request);
        return Result.ok("注册成功",null);
    }

    @PostMapping("/login")
    public Result<LoginVO> login(@RequestBody LoginRequestDTO request) {
        log.info("用户登录：{}", request.getUsername());
        LoginVO login = userService.login(request);
        log.info("JWT验证：{}", login.getToken());
        return Result.ok("登录成功",login);
    }

    @GetMapping("/info")
    public Result<Map<String, Object>> info() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        Long userId = (Long) auth.getPrincipal();
        String username = (String) auth.getDetails();
        Map<String, Object> data = Map.of(
                "userId", userId,
                "username", username
        );
        return Result.ok(data);
    }
}
