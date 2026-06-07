package com.eample.shop.Service;

import com.eample.shop.dto.CartAddDTO;
import com.eample.shop.dto.CartUpdateDTO;
import com.eample.shop.vo.CartItemVO;

import java.util.List;

public interface CartService {

    List<CartItemVO> list(Long userId);

    void add(Long userId, CartAddDTO request);

    void updateQuantity(Long userId, CartUpdateDTO request);

    void remove(Long userId, Long cartItemId);
}