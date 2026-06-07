package com.eample.shop.mapper;
import com.eample.shop.entity.ShopOrder;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.time.LocalDateTime;
import java.util.List;
@Mapper
public interface OrderMapper {
    /**
     * 插入订单
     * @param order
     * @return
     */
    int insert(ShopOrder order);

    /**
     * 根据id查询订单
     * @param id
     * @return
     */
    ShopOrder findById(@Param("id") Long id);

    /**
     * 根据id和用户id查询订单
     * @param id
     * @param userId
     * @return
     */
    ShopOrder findByIdAndUserId(@Param("id") Long id, @Param("userId") Long userId);

    /**
     * 获取订单列表-我的订单
     * @param userId
     * @return
     */
    List<ShopOrder> listByUserId(@Param("userId") Long userId);

    /**
     * 修改订单状态
     * @param id
     * @param userId
     * @param status
     * @return
     */
    int updateStatus(@Param("id") Long id,
                     @Param("userId") Long userId,
                     @Param("status") Integer status);

    /**
     * 后台计算订单数
     * @param realKeyword
     * @param status
     * @return
     */
    long countAdminSearch(String realKeyword, Integer status);

    /**
     * 后台查询订单
     * @param realKeyword
     * @param status
     * @param offset
     * @param pageSize
     * @return
     */
    List<ShopOrder> adminSearch(String realKeyword, Integer status, int offset, int pageSize);

    /**
     * 定时任务，更新15未支付的订单
     * @param status
     * @param cutoff
     * @return
     */
    List<ShopOrder> findExpiredUnpaidOrders(@Param("status") Integer status,
                                            @Param("cutoff") LocalDateTime cutoff);

    List<ShopOrder> findExpiredUnpaidOrdersByUserId(@Param("userId") Long userId,
                                                      @Param("status") Integer status,
                                                      @Param("cutoff") LocalDateTime cutoff);

    int updateStatusById(@Param("id") Long id, @Param("status") Integer status);
}