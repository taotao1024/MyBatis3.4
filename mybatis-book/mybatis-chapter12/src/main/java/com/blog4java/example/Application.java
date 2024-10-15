package com.blog4java.example;

import com.blog4java.example.controller.UserController;
import com.blog4java.example.entity.User;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;

import java.util.List;

@SpringBootApplication
@EnableAutoConfiguration(exclude = {DataSourceAutoConfiguration.class})
public class Application {
    public static void main(String[] args) {
        SpringApplication.run(Application.class, args);

        /*AnnotationConfigApplicationContext acac = new AnnotationConfigApplicationContext(Application.class);
        List<User> allUserInfo = acac.getBean(UserController.class).getAllUserInfo();
        System.out.println(allUserInfo);*/
    }
}
