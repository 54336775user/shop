package com.eample.shop.mapper;

import com.eample.shop.entity.SeckillOrder;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.time.LocalDateTime;
import java.util.List;

@Mapper
public interface SeckillOrderMapper {

    int insert(SeckillOrder order);

    SeckillOrder findById(@Param("id") Long id);

    SeckillOrder findByOrderNo(@Param("orderNo") String orderNo);

    SeckillOrder findByIdAndUserId(@Param("id") Long id, @Param("userId") Long userId);

    List<SeckillOrder> listByUserId(@Param("userId") Long userId);

    long countAdminSearch(@Param("keyword") String keyword,
                          @Param("status") Integer status);

    List<SeckillOrder> adminSearch(@Param("keyword") String keyword,
                                   @Param("status") Integer status,
                                   @Param("offset") int offset,
                                   @Param("pageSize") int pageSize);

    int updateStatus(@Param("id") Long id,
                     @Param("userId") Long userId,
                     @Param("status") Integer status);

    int updateStatusById(@Param("id") Long id,
                         @Param("status") Integer status);

    int countUserBought(@Param("userId") Long userId,
                        @Param("productId") Long productId);

    List<SeckillOrder> findExpiredUnpaidOrders(@Param("status") Integer status,
                                               @Param("cutoff") LocalDateTime cutoff);

    List<SeckillOrder> findExpiredUnpaidOrdersByUserId(@Param("userId") Long userId,
                                                         @Param("status") Integer status,
                                                         @Param("cutoff") LocalDateTime cutoff);
}