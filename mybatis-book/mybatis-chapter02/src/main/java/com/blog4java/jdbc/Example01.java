package com.blog4java.jdbc;

import com.blog4java.common.DbUtils;
import com.blog4java.common.IOUtils;
import org.junit.Test;

import java.sql.*;

public class Example01 {
    @Test
    public void testJdbc() {
        // 初始化数据
        DbUtils.initData();
        try {
            // 加载驱动
            Class.forName("org.hsqldb.jdbcDriver");
            // 获取Connection对象
            // DriverManager 是JDBC 1.0规范中定义的驱动管理类
            Connection connection = DriverManager.getConnection("jdbc:hsqldb:mem:mybatis", "sa", "");
            Statement statement = connection.createStatement();
            ResultSet resultSet = statement.executeQuery("select * from user");
            // 遍历ResultSet
            DbUtils.dumpRS(resultSet);
            // 关闭连接
            IOUtils.closeQuietly(resultSet);
            IOUtils.closeQuietly(statement);
            IOUtils.closeQuietly(connection);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
