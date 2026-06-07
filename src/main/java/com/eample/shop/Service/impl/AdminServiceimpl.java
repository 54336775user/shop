package com.eample.shop.Service.impl;

import com.eample.shop.Service.AdminService;
import com.eample.shop.dto.LoginRequestDTO;
import com.eample.shop.dto.RegisterRequestDTO;
import com.eample.shop.entity.User;
import com.eample.shop.mapper.AdminMapper;
import com.eample.shop.utils.JwtUtil;
import com.eample.shop.vo.LoginVO;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class AdminServiceimpl implements AdminService {

    private final AdminMapper adminMapper;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;

    @Override
    public void register(RegisterRequestDTO request) {
        String adminname=request.getUsername().trim();
        if (!StringUtils.hasText(adminname) || !StringUtils.hasText(request.getPassword())) {
            throw new RuntimeException("用户名或密码不能为空");
        }
        if(adminMapper.countByUsername(adminname)>0){
            throw new RuntimeException("用户名重复");
        }
        User admin = new User();
        admin.setUsername(adminname);
        admin.setPassword(passwordEncoder.encode(request.getPassword())); // 密文入库
        admin.setEmail(request.getEmail());
        admin.setNickname(adminname);
        admin.setRole("ADMIN");
        admin.setStatus(1);
        admin.setCreateTime(LocalDateTime.now());
        admin.setUpdateTime(LocalDateTime.now());
        adminMapper.insert(admin);
    }

    @Override
    public LoginVO login(LoginRequestDTO request) {
        User admin = adminMapper.findByUsername(request.getUsername());
        if(admin == null) {
            throw new RuntimeException("用户不存在");
        }
        if (admin.getStatus() != null && admin.getStatus() == 0) {
            throw new RuntimeException("账号已被禁用");
        }
        if(!passwordEncoder.matches(request.getPassword(),admin.getPassword())) {
            throw new RuntimeException("密码错误");
        }

        if (!"ADMIN".equals(admin.getRole())) {
            throw new RuntimeException("无管理员权限");
        }
        String token= jwtUtil.generateToken(admin.getId(),admin.getUsername(),admin.getRole());
        LoginVO loginVO = new LoginVO();
        loginVO.setToken(token);
        loginVO.setUsername(admin.getUsername());
        loginVO.setRole(admin.getRole());
        loginVO.setNickname(admin.getNickname());
        return loginVO;
    }


}
