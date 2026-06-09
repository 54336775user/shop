package com.eample.shop.mapper;

import com.eample.shop.entity.GroupBuyActivity;
import com.eample.shop.vo.GroupBuyActivityAdminVO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface GroupBuyActivityMapper {

    int insert(GroupBuyActivity activity);

    int update(GroupBuyActivity activity);

    GroupBuyActivity findById(@Param("id") Long id);

    GroupBuyActivity findActiveByProductId(@Param("productId") Long productId);

    int decreaseStock(@Param("id") Long id, @Param("quantity") Integer quantity);

    int increaseStock(@Param("id") Long id, @Param("quantity") Integer quantity);

    List<GroupBuyActivity> findAll();

    List<GroupBuyActivity> findEnabled();

    long countAdminSearch(@Param("keyword") String keyword,
                          @Param("productId") Long productId,
                          @Param("status") Integer status);

    List<GroupBuyActivityAdminVO> adminSearch(@Param("keyword") String keyword,
                                              @Param("productId") Long productId,
                                              @Param("status") Integer status,
                                              @Param("offset") Integer offset,
                                              @Param("size") Integer size);
}
