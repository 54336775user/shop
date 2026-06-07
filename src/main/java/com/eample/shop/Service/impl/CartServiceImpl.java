package com.eample.shop.Service.impl;

import com.eample.shop.Service.CartService;
import com.eample.shop.Service.ProductService;
import com.eample.shop.dto.CartAddDTO;
import com.eample.shop.dto.CartUpdateDTO;
import com.eample.shop.entity.CartItem;
import com.eample.shop.entity.Product;
import com.eample.shop.mapper.CartItemMapper;
import com.eample.shop.vo.CartItemVO;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;


@Service
@RequiredArgsConstructor
public class CartServiceImpl implements CartService {

    private final ProductService productService;
    private final CartItemMapper cartItemMapper;

    @Override
    public List<CartItemVO> list(Long userId) {
        List<CartItemVO> list =cartItemMapper.listByUserId(userId);
        for(CartItemVO cartItemVO:list){
            fillCartItemVO(cartItemVO);
        }
        return list;
    }

    @Override
    public void add(Long userId, CartAddDTO request) {
        if(request.getProductId() == null){
            throw new RuntimeException("商品不能为空");
        }
        Integer quantity = request.getQuantity();
        if(quantity == null || quantity <= 0){
            quantity = 1;
        }
        Product product =productService.getById(request.getProductId());
        if(product.getStatus() == null ||product.getStatus() != 1){
            throw new RuntimeException("商品已下架");
        }
        CartItem exit = cartItemMapper.findByUserIdAndProductId(userId, request.getProductId());
        if(exit != null){
            Integer newQuantity = quantity + exit.getQuantity();
            if(newQuantity >product.getStock()){
                throw new RuntimeException("库存不足");
            }
            int row=cartItemMapper.updateQuantity(userId, request.getProductId(), newQuantity);
            if(row == 0){
                throw new RuntimeException("购物车添加失败");
            }
            return;
        }
        LocalDateTime now = LocalDateTime.now();
        if(quantity>product.getStock()){
            throw new RuntimeException("库存不足");
        }
        CartItem cartItem = new CartItem();
        cartItem.setUserId(userId);
        cartItem.setProductId(request.getProductId());
        cartItem.setQuantity(quantity);
        cartItem.setCreateTime(now);
        cartItem.setUpdateTime(now);
        cartItemMapper.insert(cartItem);
    }

    @Override
    public void updateQuantity(Long userId, CartUpdateDTO request) {
        if (request.getCartItemId() == null) {
            throw new RuntimeException("购物车项不能为空");
        }
        if (request.getQuantity() == null || request.getQuantity() < 1) {
            throw new RuntimeException("数量必须大于 0");
        }
        CartItem cartItem = cartItemMapper.findById(request.getCartItemId());
        if (cartItem == null || !userId.equals(cartItem.getUserId())) {
            throw new RuntimeException("购物车项不存在");
        }
        Product product = productService.getById(cartItem.getProductId());
        if (product.getStatus() == null || product.getStatus() != 1) {
            throw new RuntimeException("商品已下架");
        }
        if (request.getQuantity() > product.getStock()) {
            throw new RuntimeException("库存不足");
        }
        int rows = cartItemMapper.updateQuantity(request.getCartItemId(), userId, request.getQuantity());
        if (rows == 0) {
            throw new RuntimeException("更新购物车失败");
        }
    }

    @Override
    public void remove(Long userId, Long cartItemId) {
        int rows = cartItemMapper.deleteById(cartItemId, userId);
        if (rows == 0) {
            throw new RuntimeException("购物车项不存在");
        }
    }

    private void fillCartItemVO(CartItemVO vo) {
        BigDecimal price = vo.getPrice() == null ? BigDecimal.ZERO : vo.getPrice();
        int quantity = vo.getQuantity() == null ? 0 : vo.getQuantity();
        int stock = vo.getStock() == null ? 0 : vo.getStock();
        int status = vo.getStatus() == null ? 0 : vo.getStatus();
        vo.setSubtotal(price.multiply(BigDecimal.valueOf(quantity)));
        vo.setSelectable(status == 1 && stock > 0 && quantity <= stock);
    }
}
