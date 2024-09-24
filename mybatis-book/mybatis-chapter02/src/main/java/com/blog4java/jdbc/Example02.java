package com.blog4java.jdbc;

import com.blog4java.common.DbUtils;
import com.blog4java.common.IOUtils;
import org.apache.ibatis.datasource.unpooled.UnpooledDataSource;
import org.junit.Test;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.Statement;

public class Example02 {
    @Test
    public void testJdbc() {
        // 初始化数据
        DbUtils.initData();
        try {
            // 创建DataSource实例
            // DataSource 是JDBC 2.0中定义的驱动管理类 由于JDBC API 只定义接口，不提供实现类。
            // 因此 UnpooledDataSource 是有Mybatis框架提供的DataSource实现类
            DataSource dataSource = new UnpooledDataSource("org.hsqldb.jdbcDriver",
                    "jdbc:hsqldb:mem:mybatis", "sa", "");
            // 获取Connection对象
            Connection connection = dataSource.getConnection();
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
