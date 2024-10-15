package com.blog4java.example.controller;

import com.blog4java.example.ApplicationTest;
import org.junit.Test;
import org.springframework.http.MediaType;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

public class UserControllerTest extends ApplicationTest {

    @Test
    public void testUserRegister() throws Exception {
        // Spring 通过代理对象 操作Mybatis的SqlSession对象。
        // SqlSession使用后会关闭sqlSession对象，导致Mybatis的一级缓存失效
        // TODO 经过Debug 发现一级缓存失效原因存在争议
        String response = mockMvc.perform(
                        get("/user/register")
                                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                                .param("name", "jack")
                                .param("password", "12323")
                                .param("phone", "189000000")
                                .param("gender", "male")
                                .param("nickName", "mack")
                ).andExpect(status().isOk())
                .andDo(print())
                .andReturn().getResponse().getContentAsString();
        // System.out.println("返回数据 = " + response);
    }

    @Test
    public void testGetAllUser() throws Exception {
        String response = mockMvc.perform(
                        get("/user/getAllUser")
                                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                ).andExpect(status().isOk())
                .andDo(print())
                .andReturn().getResponse().getContentAsString();

        String response2 = mockMvc.perform(
                        get("/user/getAllUser")
                                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                ).andExpect(status().isOk())
                .andDo(print())
                .andReturn().getResponse().getContentAsString();
        //  System.out.println("返回数据 = " + response);
    }
}

