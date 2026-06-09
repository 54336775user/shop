package com.eample.shop.mapper;

import com.eample.shop.entity.GroupBuyOrder;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.time.LocalDateTime;
import java.util.List;

@Mapper
public interface GroupBuyOrderMapper {

    int insert(GroupBuyOrder order);

    GroupBuyOrder findById(@Param("id") Long id);

    GroupBuyOrder findByOrderNo(@Param("orderNo") String orderNo);

    GroupBuyOrder findByIdAndUserId(@Param("id") Long id, @Param("userId") Long userId);

    List<GroupBuyOrder> listByUserId(@Param("userId") Long userId);

    List<GroupBuyOrder> listByTeamId(@Param("teamId") Long teamId);

    long countAdminSearch(@Param("keyword") String keyword,
                          @Param("status") Integer status);

    List<GroupBuyOrder> adminSearch(@Param("keyword") String keyword,
                                    @Param("status") Integer status,
                                    @Param("offset") int offset,
                                    @Param("pageSize") int pageSize);

    List<GroupBuyOrder> findExpiredUnpaidOrdersByUserId(@Param("userId") Long userId,
                                                        @Param("status") Integer status,
                                                        @Param("cutoff") LocalDateTime cutoff);

    int updateStatus(@Param("id") Long id, @Param("status") Integer status);

    int updateStatusById(@Param("id") Long id, @Param("status") Integer status);

    List<GroupBuyOrder> findExpiredUnpaidOrders(@Param("status") Integer status,
                                                @Param("cutoff") LocalDateTime cutoff);


}
