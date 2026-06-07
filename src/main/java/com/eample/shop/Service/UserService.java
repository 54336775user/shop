package com.eample.shop.Service;

import com.eample.shop.dto.LoginRequestDTO;
import com.eample.shop.dto.RegisterRequestDTO;
import com.eample.shop.vo.LoginVO;

public interface UserService {
    /**
     * 用户注册
     * @param request
     */
    void register(RegisterRequestDTO request);

    /**
     * 用户登录
     * @param request
     * @return
     */
    LoginVO login(LoginRequestDTO request);
}
