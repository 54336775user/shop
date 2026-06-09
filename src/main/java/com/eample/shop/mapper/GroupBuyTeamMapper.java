package com.eample.shop.mapper;

import com.eample.shop.entity.GroupBuyTeam;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.time.LocalDateTime;
import java.util.List;

@Mapper
public interface GroupBuyTeamMapper {

    int insert(GroupBuyTeam team);

    GroupBuyTeam findById(@Param("id") Long id);

    GroupBuyTeam findByIdForUpdate(@Param("id") Long id);

    GroupBuyTeam findByTeamNo(@Param("teamNo") String teamNo);

    List<GroupBuyTeam> listActiveByProductId(@Param("productId") Long productId,
                                             @Param("now") LocalDateTime now,
                                             @Param("limit") Integer limit);

    int updateStatus(@Param("id") Long id, @Param("status") Integer status);

    int markSuccess(@Param("id") Long id, @Param("successTime") LocalDateTime successTime);

    int updatePaidSize(@Param("id") Long id, @Param("paidSize") Integer paidSize);

    int increasePaidSize(@Param("id") Long id);

    List<GroupBuyTeam> findExpiredTeams(@Param("now") LocalDateTime now);
}
