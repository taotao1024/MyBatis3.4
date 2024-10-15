package com.blog4java.example.test;

import com.blog4java.example.ApplicationTest;
import com.blog4java.example.entity.User;
import com.blog4java.example.service.UserService;
import org.junit.Test;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.TransactionCallback;
import org.springframework.transaction.support.TransactionTemplate;

import javax.annotation.Resource;
import java.util.ArrayList;
import java.util.List;

public class TransactionTest extends ApplicationTest {

    @Resource
    private TransactionTemplate transactionTemplate;

    @Resource
    private UserService userService;

    /**
     * 声明式事务
     */
    @Test
    public void testTrans() {
        transactionTemplate.execute(new TransactionCallback<Integer>() {
            @Override
            public Integer doInTransaction(TransactionStatus status) {
                User user = buildUser();
                userService.userRegister(user);
                return 0;
            }
        });
    }

    private User buildUser() {
        User user = new User();
        user.setName("aaa");
        user.setNickName("aaa");
        user.setPassword("***");
        user.setPhone("111");
        return user;
    }

    /**
     * 测试{{}}表达式
     */
    @Test
    public void testOther() {
        List<String> list = new ArrayList<String>() {{
            this.add("111");
            this.add("222");
        }};
        System.out.println(list);
    }
}
