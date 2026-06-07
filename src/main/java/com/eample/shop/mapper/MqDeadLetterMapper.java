package com.eample.shop.mapper;

import com.eample.shop.entity.MqDeadLetter;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface MqDeadLetterMapper {

    int insert(MqDeadLetter record);

    MqDeadLetter findById(@Param("id") Long id);

    MqDeadLetter findByRequestId(@Param("requestId") String requestId);

    long countByStatus(@Param("status") Integer status);

    List<MqDeadLetter> listByStatus(@Param("status") Integer status,
                                    @Param("offset") int offset,
                                    @Param("pageSize") int pageSize);

    int markProcessing(@Param("id") Long id);

    int markDone(@Param("id") Long id,
                 @Param("handlerAdminId") Long handlerAdminId,
                 @Param("handleRemark") String handleRemark);
}