package com.blog4java.mybatis.example.mapper;

import com.blog4java.mybatis.example.entity.UserEntity04;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

public interface UserMapper {

    List<UserEntity04> listAllUser();

    @Select("select * from user where id=#{userId,jdbcType=INTEGER}")
    UserEntity04 getUserById(@Param("userId") String userId);

    List<UserEntity04> getUserByEntity(UserEntity04 user);

    UserEntity04 getUserByPhone(@Param("phone") String phone);

}