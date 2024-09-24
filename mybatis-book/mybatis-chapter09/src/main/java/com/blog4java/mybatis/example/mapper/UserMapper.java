package com.blog4java.mybatis.example.mapper;

import com.blog4java.mybatis.example.entity.UserEntity09;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

public interface UserMapper {

    List<UserEntity09> getUserByEntity(UserEntity09 user);

    List<UserEntity09> getUserInfo(UserEntity09 user);

    List<UserEntity09> getUserByPhones(@Param("phones") List<String> phones);

    @Select("<script>" +
            "select * from user\n" +
            "<where>\n" +
            "    <if test=\"name != null\">\n" +
            "        AND name = #{name}\n" +
            "    </if>\n" +
            "    <if test=\"phone != null\">\n" +
            "        AND phone = #{phone}\n" +
            "    </if>\n" +
            "</where>" +
            "</script>")
    UserEntity09 getUserByPhoneAndName(@Param("phone") String phone, @Param("name") String name);


    List<UserEntity09> getUserByNames(@Param("names") List<String> names);

    UserEntity09 getUserByName(@Param("userName") String userName);
}