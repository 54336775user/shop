package com.eample.shop.mapper;

import com.eample.shop.entity.CartItem;
import com.eample.shop.vo.CartItemVO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface CartItemMapper {

    CartItem findById(@Param("id") Long id);

    CartItem findByUserIdAndProductId(@Param("userId") Long userId,
                                      @Param("productId") Long productId);

    List<CartItemVO> listByUserId(@Param("userId") Long userId);

    List<CartItem> findByIdsAndUserId(@Param("ids") List<Long> ids,
                                      @Param("userId") Long userId);

    int insert(CartItem cartItem);

    int updateQuantity(@Param("id") Long id,
                       @Param("userId") Long userId,
                       @Param("quantity") Integer quantity);

    int deleteById(@Param("id") Long id, @Param("userId") Long userId);

    int deleteByIds(@Param("ids") List<Long> ids, @Param("userId") Long userId);
}
