package com.eample.shop.mapper;

import com.eample.shop.entity.User;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface AdminMapper {

    /**
     * 查询用户名字
     * @param username
     * @return
     */
    User findByUsername(@Param("username") String username);

    /**
     * 查看该名字用户数量，防止名字重复
     * @param username
     * @return
     */
    Integer countByUsername(@Param("username") String username);

    /**
     * 插入用户数据
     * @param user
     * @return
     */
    Integer insert(User user);
}
