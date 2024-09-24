package com.blog4java.jdbc;

import com.blog4java.common.DbUtils;
import com.blog4java.common.IOUtils;
import org.apache.ibatis.datasource.DataSourceFactory;
import org.apache.ibatis.datasource.unpooled.UnpooledDataSourceFactory;
import org.junit.Test;

import javax.sql.DataSource;
import java.io.InputStream;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.Statement;
import java.util.Properties;

public class Example03 {
    @Test
    public void testJdbc() {
        // 初始化数据
        DbUtils.initData();
        try {
            // 创建DataSource实例
            DataSourceFactory dsf = new UnpooledDataSourceFactory();
            Properties properties = new Properties();
            InputStream configStream = Thread.currentThread().getContextClassLoader().getResourceAsStream("database.properties");
            properties.load(configStream);
            dsf.setProperties(properties);
            DataSource dataSource = dsf.getDataSource();
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
