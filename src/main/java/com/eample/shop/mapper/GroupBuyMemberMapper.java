package com.eample.shop.mapper;

import com.eample.shop.entity.GroupBuyMember;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface GroupBuyMemberMapper {

    int insert(GroupBuyMember member);

    GroupBuyMember findById(@Param("id") Long id);

    GroupBuyMember findByTeamIdAndUserId(@Param("teamId") Long teamId,
                                         @Param("userId") Long userId);

    List<GroupBuyMember> listByTeamId(@Param("teamId") Long teamId);

    int updateStatus(@Param("id") Long id, @Param("status") Integer status);

    int updatePayTime(@Param("id") Long id);

    int countPaidByTeamId(@Param("teamId") Long teamId);
}