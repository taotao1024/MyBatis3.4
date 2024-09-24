package com.blog4java.mybatis.sqlsession;

import org.apache.ibatis.io.Resources;
import org.apache.ibatis.session.SqlSession;
import org.apache.ibatis.session.SqlSessionFactory;
import org.apache.ibatis.session.SqlSessionFactoryBuilder;
import org.junit.Test;

import java.io.IOException;
import java.io.Reader;

public class SqlSessionExample {

    @Test
    public void testSqlSession() throws IOException {
        // 获取Mybatis配置文件输入流
        Reader reader = Resources.getResourceAsReader("mybatis-config.xml");
        // 通过SqlSessionFactoryBuilder创建SqlSessionFactory实例
        // SqlSessionFactory 在构建前会创建MyBatis Configuration对象
        SqlSessionFactory sqlSessionFactory = new SqlSessionFactoryBuilder().build(reader);
        // 调用SqlSessionFactory的openSession（）方法，创建SqlSession实例
        SqlSession openSession = sqlSessionFactory.openSession();
        // sqlSession 表示MyBatis框架与数据库建立的会话，通过sqlSession实例完成对数据库的增删改查操作。
    }
}
