package com.eample.shop.Service.impl;

import com.eample.shop.Service.UserService;
import com.eample.shop.dto.LoginRequestDTO;
import com.eample.shop.dto.RegisterRequestDTO;
import com.eample.shop.entity.User;
import com.eample.shop.mapper.UserMapper;
import com.eample.shop.utils.JwtUtil;
import com.eample.shop.vo.LoginVO;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class UserServiceimpl implements UserService {

    private final UserMapper userMapper;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;

    @Override
    public void register(RegisterRequestDTO request) {
        String username = request.getUsername().trim();
        if (!StringUtils.hasText(username) || !StringUtils.hasText(request.getPassword())) {
            throw new RuntimeException("用户名或密码不能为空");
        }
        if(userMapper.countByUsername(username) > 0) {
            throw new RuntimeException("用户名已存在");
        }
        User user = new User();
        user.setUsername(username);
        user.setPassword(passwordEncoder.encode(request.getPassword())); // 密文入库
        user.setEmail(request.getEmail());
        user.setNickname(username);
        user.setRole("USER");
        user.setStatus(1);
        user.setCreateTime(LocalDateTime.now());
        user.setUpdateTime(LocalDateTime.now());
        userMapper.insert(user);
    }

    @Override
    public LoginVO login(LoginRequestDTO request) {
        User user = userMapper.findByUsername(request.getUsername());
        if(user == null) {
            throw new RuntimeException("用户不存在");
        }
        if (user.getStatus() != null && user.getStatus() == 0) {
            throw new RuntimeException("账号已被禁用");
        }
        if(!passwordEncoder.matches(request.getPassword(),user.getPassword())) {
            throw new RuntimeException("密码错误");
        }

        String token= jwtUtil.generateToken(user.getId(),user.getUsername(),user.getRole());
        LoginVO loginVO = new LoginVO();
        loginVO.setToken(token);
        loginVO.setUsername(user.getUsername());
        loginVO.setRole(user.getRole());
        loginVO.setNickname(user.getNickname());
        return loginVO;
    }
}
