/**
 * Copyright 2009-2017 the original author or authors.
 * <p>
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * <p>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.ibatis.datasource.pooled;

import java.io.PrintWriter;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Proxy;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Properties;
import java.util.logging.Logger;

import javax.sql.DataSource;

import org.apache.ibatis.datasource.unpooled.UnpooledDataSource;
import org.apache.ibatis.logging.Log;
import org.apache.ibatis.logging.LogFactory;

/**
 * 工厂方法模式：
 * <p>
 * 角色：数据源 实现类
 * <p>
 * This is a simple, synchronous, thread-safe database connection pool.
 * 这是一个简单的、同步的、线程安全的数据库连接池。
 * <p>
 * PooledDataSource并不会直接管理 java.sql.Connection 对象 而是会管理PooledConnection对象。
 * 在PooledConnection对象中封装了真正的数据库连接对象(java.sql.Connection)以及其代理对象
 * <p>
 * PooledDataSource中管理的真正的数据库连接对象是由PooledDataSource中封装的UnpooledDataSource对象创建的
 *
 * @author Clinton Begin
 */
public class PooledDataSource implements DataSource {

    private static final Log log = LogFactory.getLog(PooledDataSource.class);
    /**
     * 通过PoolState管理连接池的状态并记录统计信息
     */
    private final PoolState state = new PoolState(this);
    /**
     * 真正的数据库连接对象
     */
    private final UnpooledDataSource dataSource;

    // OPTIONAL CONFIGURATION FIELDS
    /**
     * 最大活跃连接数
     */
    protected int poolMaximumActiveConnections = 10;
    /**
     * 最大空闲连接数
     */
    protected int poolMaximumIdleConnections = 5;
    /**
     * 最大checkout时长
     */
    protected int poolMaximumCheckoutTime = 20000;
    /**
     * 在无法获取连接时，线程需要等待的时间
     */
    protected int poolTimeToWait = 20000;
    /**
     * 从连接池中,若获取的是坏连接,允许重新获取新连接的次数,默认值3.
     */
    protected int poolMaximumLocalBadConnectionTolerance = 3;
    /**
     * 在测试一个数据库连接是否可用时，会给数据库发送的一个测试SQL语句
     */
    protected String poolPingQuery = "NO PING QUERY SET";
    /**
     * 是否允许发送测试SQL语句
     */
    protected boolean poolPingEnabled;
    /**
     * 当连接超过poolPingConnectionsNotUsedFor毫秒未使用时，会发送一条测试SQL语句，检测连接是否正常
     */
    protected int poolPingConnectionsNotUsedFor;
    /**
     * 根据数据库的URL、用户名、密码生成一个Hash值，该哈希值用于标示着当前的连接池，在构造函数中初始化。
     */
    private int expectedConnectionTypeCode;

    public PooledDataSource() {
        dataSource = new UnpooledDataSource();
    }

    public PooledDataSource(UnpooledDataSource dataSource) {
        this.dataSource = dataSource;
    }

    public PooledDataSource(String driver, String url, String username, String password) {
        dataSource = new UnpooledDataSource(driver, url, username, password);
        expectedConnectionTypeCode = assembleConnectionTypeCode(dataSource.getUrl(), dataSource.getUsername(), dataSource.getPassword());
    }

    public PooledDataSource(String driver, String url, Properties driverProperties) {
        dataSource = new UnpooledDataSource(driver, url, driverProperties);
        expectedConnectionTypeCode = assembleConnectionTypeCode(dataSource.getUrl(), dataSource.getUsername(), dataSource.getPassword());
    }

    public PooledDataSource(ClassLoader driverClassLoader, String driver, String url, String username, String password) {
        dataSource = new UnpooledDataSource(driverClassLoader, driver, url, username, password);
        expectedConnectionTypeCode = assembleConnectionTypeCode(dataSource.getUrl(), dataSource.getUsername(), dataSource.getPassword());
    }

    public PooledDataSource(ClassLoader driverClassLoader, String driver, String url, Properties driverProperties) {
        dataSource = new UnpooledDataSource(driverClassLoader, driver, url, driverProperties);
        expectedConnectionTypeCode = assembleConnectionTypeCode(dataSource.getUrl(), dataSource.getUsername(), dataSource.getPassword());
    }

    @Override
    public Connection getConnection() throws SQLException {
        return popConnection(dataSource.getUsername(), dataSource.getPassword()).getProxyConnection();
    }

    @Override
    public Connection getConnection(String username, String password) throws SQLException {
        return popConnection(username, password).getProxyConnection();
    }

    @Override
    public void setLoginTimeout(int loginTimeout) throws SQLException {
        DriverManager.setLoginTimeout(loginTimeout);
    }

    @Override
    public int getLoginTimeout() throws SQLException {
        return DriverManager.getLoginTimeout();
    }

    @Override
    public void setLogWriter(PrintWriter logWriter) throws SQLException {
        DriverManager.setLogWriter(logWriter);
    }

    @Override
    public PrintWriter getLogWriter() throws SQLException {
        return DriverManager.getLogWriter();
    }

    public void setDriver(String driver) {
        dataSource.setDriver(driver);
        forceCloseAll();
    }

    public void setUrl(String url) {
        dataSource.setUrl(url);
        forceCloseAll();
    }

    public void setUsername(String username) {
        dataSource.setUsername(username);
        forceCloseAll();
    }

    public void setPassword(String password) {
        dataSource.setPassword(password);
        forceCloseAll();
    }

    public void setDefaultAutoCommit(boolean defaultAutoCommit) {
        dataSource.setAutoCommit(defaultAutoCommit);
        forceCloseAll();
    }

    public void setDefaultTransactionIsolationLevel(Integer defaultTransactionIsolationLevel) {
        dataSource.setDefaultTransactionIsolationLevel(defaultTransactionIsolationLevel);
        forceCloseAll();
    }

    public void setDriverProperties(Properties driverProps) {
        dataSource.setDriverProperties(driverProps);
        forceCloseAll();
    }

    /**
     * The maximum number of active connections
     *
     * @param poolMaximumActiveConnections The maximum number of active connections
     */
    public void setPoolMaximumActiveConnections(int poolMaximumActiveConnections) {
        this.poolMaximumActiveConnections = poolMaximumActiveConnections;
        forceCloseAll();
    }

    /**
     * The maximum number of idle connections
     *
     * @param poolMaximumIdleConnections The maximum number of idle connections
     */
    public void setPoolMaximumIdleConnections(int poolMaximumIdleConnections) {
        this.poolMaximumIdleConnections = poolMaximumIdleConnections;
        forceCloseAll();
    }

    /**
     * The maximum number of tolerance for bad connection happens in one thread
     * which are applying for new {@link PooledConnection}
     *
     * @param poolMaximumLocalBadConnectionTolerance max tolerance for bad connection happens in one thread
     * @since 3.4.5
     */
    public void setPoolMaximumLocalBadConnectionTolerance(
            int poolMaximumLocalBadConnectionTolerance) {
        this.poolMaximumLocalBadConnectionTolerance = poolMaximumLocalBadConnectionTolerance;
    }

    /**
     * The maximum time a connection can be used before it *may* be
     * given away again.
     *
     * @param poolMaximumCheckoutTime The maximum time
     */
    public void setPoolMaximumCheckoutTime(int poolMaximumCheckoutTime) {
        this.poolMaximumCheckoutTime = poolMaximumCheckoutTime;
        forceCloseAll();
    }

    /**
     * The time to wait before retrying to get a connection
     *
     * @param poolTimeToWait The time to wait
     */
    public void setPoolTimeToWait(int poolTimeToWait) {
        this.poolTimeToWait = poolTimeToWait;
        forceCloseAll();
    }

    /**
     * The query to be used to check a connection
     *
     * @param poolPingQuery The query
     */
    public void setPoolPingQuery(String poolPingQuery) {
        this.poolPingQuery = poolPingQuery;
        forceCloseAll();
    }

    /**
     * Determines if the ping query should be used.
     *
     * @param poolPingEnabled True if we need to check a connection before using it
     */
    public void setPoolPingEnabled(boolean poolPingEnabled) {
        this.poolPingEnabled = poolPingEnabled;
        forceCloseAll();
    }

    /**
     * If a connection has not been used in this many milliseconds, ping the
     * database to make sure the connection is still good.
     *
     * @param milliseconds the number of milliseconds of inactivity that will trigger a ping
     */
    public void setPoolPingConnectionsNotUsedFor(int milliseconds) {
        this.poolPingConnectionsNotUsedFor = milliseconds;
        forceCloseAll();
    }

    public String getDriver() {
        return dataSource.getDriver();
    }

    public String getUrl() {
        return dataSource.getUrl();
    }

    public String getUsername() {
        return dataSource.getUsername();
    }

    public String getPassword() {
        return dataSource.getPassword();
    }

    public boolean isAutoCommit() {
        return dataSource.isAutoCommit();
    }

    public Integer getDefaultTransactionIsolationLevel() {
        return dataSource.getDefaultTransactionIsolationLevel();
    }

    public Properties getDriverProperties() {
        return dataSource.getDriverProperties();
    }

    public int getPoolMaximumActiveConnections() {
        return poolMaximumActiveConnections;
    }

    public int getPoolMaximumIdleConnections() {
        return poolMaximumIdleConnections;
    }

    public int getPoolMaximumLocalBadConnectionTolerance() {
        return poolMaximumLocalBadConnectionTolerance;
    }

    public int getPoolMaximumCheckoutTime() {
        return poolMaximumCheckoutTime;
    }

    public int getPoolTimeToWait() {
        return poolTimeToWait;
    }

    public String getPoolPingQuery() {
        return poolPingQuery;
    }

    public boolean isPoolPingEnabled() {
        return poolPingEnabled;
    }

    public int getPoolPingConnectionsNotUsedFor() {
        return poolPingConnectionsNotUsedFor;
    }

    /**
     * 当修改PooledDataSource的字段时，例如数据库URL、用户名、密码、autoCommit设置等，都会调用forceCloseAll()方法将所有连接关闭，
     * 同事将所有相应的PooledConnection对象都设置未无效，并清空activeConnections活跃连接集合和idleConnections空闲连接集合。系统
     * 之后通过PooledDataSource.getConnection()获取连接时，会按照新的配置重新创建数据库连接及其相应的PooledConnection对象。
     * <p>
     * 关闭池中的所有活动连接和空闲连接
     */
    public void forceCloseAll() {
        // 同步
        synchronized (state) {
            // 更新当前连接池的标识
            expectedConnectionTypeCode = assembleConnectionTypeCode(dataSource.getUrl(), dataSource.getUsername(), dataSource.getPassword());
            // 处理全部的活跃链接
            for (int i = state.activeConnections.size(); i > 0; i--) {
                try {
                    // 从activeConnections集合中获取PooledConnection对象
                    PooledConnection conn = state.activeConnections.remove(i - 1);
                    // 将PooledConnection对象设置为无效
                    conn.invalidate();
                    // 获取真正的数据库连接对象
                    Connection realConn = conn.getRealConnection();
                    if (!realConn.getAutoCommit()) {
                        // 回滚未提交的事务
                        realConn.rollback();
                    }
                    // 关闭真正的数据库连接对象
                    realConn.close();
                } catch (Exception e) {
                    // ignore
                }
            }
            // 处理全部的空闲连接
            for (int i = state.idleConnections.size(); i > 0; i--) {
                try {
                    // 从activeConnections集合中获取PooledConnection对象
                    PooledConnection conn = state.idleConnections.remove(i - 1);
                    // 将PooledConnection对象设置为无效
                    conn.invalidate();
                    // 获取真正的数据库连接对象
                    Connection realConn = conn.getRealConnection();
                    if (!realConn.getAutoCommit()) {
                        // 回滚未提交的事务
                        realConn.rollback();
                    }
                    // 关闭真正的数据库连接对象
                    realConn.close();
                } catch (Exception e) {
                    // ignore
                }
            }
        }
        if (log.isDebugEnabled()) {
            log.debug("PooledDataSource forcefully closed/removed all connections.");
        }
    }

    public PoolState getPoolState() {
        return state;
    }

    /**
     * 更新当前连接池的标识
     *
     * @param url
     * @param username
     * @param password
     * @return
     */
    private int assembleConnectionTypeCode(String url, String username, String password) {
        return ("" + url + username + password).hashCode();
    }

    /**
     * 将连接归还连接池
     *
     * @param conn 连接
     * @throws SQLException
     */
    protected void pushConnection(PooledConnection conn) throws SQLException {
        // 同步
        synchronized (state) {
            // 从activeConnections集合中移除PooledConnection对象
            state.activeConnections.remove(conn);
            // 检测连接是否可用
            if (conn.isValid()) {
                // 检测空闲连接数是否已经达到上限，以及PooledConnection是否未该连接池的连接
                if (state.idleConnections.size() < poolMaximumIdleConnections && conn.getConnectionTypeCode() == expectedConnectionTypeCode) {
                    // 累计checkout时长
                    state.accumulatedCheckoutTime += conn.getCheckoutTime();
                    // 回滚未提交的事务
                    if (!conn.getRealConnection().getAutoCommit()) {
                        conn.getRealConnection().rollback();
                    }
                    // 未返还连接创建新的PooledConnection对象
                    PooledConnection newConn = new PooledConnection(conn.getRealConnection(), this);
                    // 添加到idleConnections集合中
                    state.idleConnections.add(newConn);
                    newConn.setCreatedTimestamp(conn.getCreatedTimestamp());
                    newConn.setLastUsedTimestamp(conn.getLastUsedTimestamp());
                    // 将原PooledConnection对象设置为无效
                    conn.invalidate();
                    if (log.isDebugEnabled()) {
                        log.debug("Returned connection " + newConn.getRealHashCode() + " to pool.");
                    }
                    // 唤醒阻塞等待的全部线程
                    state.notifyAll();
                } else {
                    // 空闲连接数已达到上线或PooledConnection对象并不属于该连接池
                    // 累计checkout时长
                    state.accumulatedCheckoutTime += conn.getCheckoutTime();
                    if (!conn.getRealConnection().getAutoCommit()) {
                        conn.getRealConnection().rollback();
                    }
                    // 关闭真正的数据库连接
                    conn.getRealConnection().close();
                    if (log.isDebugEnabled()) {
                        log.debug("Closed connection " + conn.getRealHashCode() + ".");
                    }
                    // 将PooledConnection对象设置未无效
                    conn.invalidate();
                }
            } else {
                if (log.isDebugEnabled()) {
                    log.debug("A bad connection (" + conn.getRealHashCode() + ") attempted to return to the pool, discarding connection.");
                }
                // 统计无效PooledConnection对象个数
                state.badConnectionCount++;
            }
        }
    }

    /**
     * 获取连接
     *
     * @param username 用户名
     * @param password 密码
     * @return 连接
     * @throws SQLException
     */
    private PooledConnection popConnection(String username, String password) throws SQLException {
        boolean countedWait = false;
        PooledConnection conn = null;
        long t = System.currentTimeMillis();
        int localBadConnectionCount = 0;

        while (conn == null) {
            // 同步
            synchronized (state) {
                // 检测空闲连接
                if (!state.idleConnections.isEmpty()) {
                    // 池有可用连接 获取连接
                    conn = state.idleConnections.remove(0);
                    if (log.isDebugEnabled()) {
                        log.debug("Checked out connection " + conn.getRealHashCode() + " from pool.");
                    }
                } else {
                    // 池中没有可用空闲连接
                    // 活跃连接数没有到达最大值，可以创建新连接
                    if (state.activeConnections.size() < poolMaximumActiveConnections) {
                        // 创建新数据库连接，并封装成PooledConnection对象
                        conn = new PooledConnection(dataSource.getConnection(), this);
                        if (log.isDebugEnabled()) {
                            log.debug("Created connection " + conn.getRealHashCode() + ".");
                        }
                    } else {
                        // 活跃连接已到达最大数，不能创建新连接
                        // 获取最先创建的活跃链接
                        PooledConnection oldestActiveConnection = state.activeConnections.get(0);
                        long longestCheckoutTime = oldestActiveConnection.getCheckoutTime();
                        // 检测该连接是否超时
                        if (longestCheckoutTime > poolMaximumCheckoutTime) {
                            // 对超时连接的信息进行统计
                            state.claimedOverdueConnectionCount++;
                            state.accumulatedCheckoutTimeOfOverdueConnections += longestCheckoutTime;
                            state.accumulatedCheckoutTime += longestCheckoutTime;
                            // 将超时连接移除activeConnections集合
                            state.activeConnections.remove(oldestActiveConnection);
                            // 如果超时连接未提交，则自动回滚。
                            if (!oldestActiveConnection.getRealConnection().getAutoCommit()) {
                                try {
                                    oldestActiveConnection.getRealConnection().rollback();
                                } catch (SQLException e) {
                  /*
                     Just log a message for debug and continue to execute the following
                     statement like nothing happend.
                     Wrap the bad connection with a new PooledConnection, this will help
                     to not intterupt current executing thread and give current thread a
                     chance to join the next competion for another valid/good database
                     connection. At the end of this loop, bad {@link @conn} will be set as null.
                   */
                                    log.debug("Bad connection. Could not roll back");
                                }
                            }
                            // 创建新PooledConnection对象，但是真正的数据库连接并未创建新的
                            conn = new PooledConnection(oldestActiveConnection.getRealConnection(), this);
                            conn.setCreatedTimestamp(oldestActiveConnection.getCreatedTimestamp());
                            conn.setLastUsedTimestamp(oldestActiveConnection.getLastUsedTimestamp());
                            // 将超时的PooledConnection设置未无效
                            oldestActiveConnection.invalidate();
                            if (log.isDebugEnabled()) {
                                log.debug("Claimed overdue connection " + conn.getRealHashCode() + ".");
                            }
                        } else {
                            // 无空闲连接、无法创建新连接且无超时连接，则只能阻塞等待
                            try {
                                if (!countedWait) {
                                    // 统计等待次数
                                    state.hadToWaitCount++;
                                    countedWait = true;
                                }
                                if (log.isDebugEnabled()) {
                                    log.debug("Waiting as long as " + poolTimeToWait + " milliseconds for connection.");
                                }
                                long wt = System.currentTimeMillis();
                                // 阻塞等待
                                state.wait(poolTimeToWait);
                                // 统计累积的等待时间
                                state.accumulatedWaitTime += System.currentTimeMillis() - wt;
                            } catch (InterruptedException e) {
                                break;
                            }
                        }
                    }
                }
                if (conn != null) {
                    // ping到服务器，检查连接是否有效
                    if (conn.isValid()) {
                        if (!conn.getRealConnection().getAutoCommit()) {
                            conn.getRealConnection().rollback();
                        }
                        // 设置PooledConnection的相关属性
                        conn.setConnectionTypeCode(assembleConnectionTypeCode(dataSource.getUrl(), username, password));
                        conn.setCheckoutTimestamp(System.currentTimeMillis());
                        conn.setLastUsedTimestamp(System.currentTimeMillis());
                        // 进行相关统计
                        state.activeConnections.add(conn);
                        state.requestCount++;
                        state.accumulatedRequestTime += System.currentTimeMillis() - t;
                    } else {
                        if (log.isDebugEnabled()) {
                            log.debug("A bad connection (" + conn.getRealHashCode() + ") was returned from the pool, getting another connection.");
                        }
                        state.badConnectionCount++;
                        localBadConnectionCount++;
                        conn = null;
                        if (localBadConnectionCount > (poolMaximumIdleConnections + poolMaximumLocalBadConnectionTolerance)) {
                            if (log.isDebugEnabled()) {
                                log.debug("PooledDataSource: Could not get a good connection to the database.");
                            }
                            throw new SQLException("PooledDataSource: Could not get a good connection to the database.");
                        }
                    }
                }
            }

        }

        if (conn == null) {
            if (log.isDebugEnabled()) {
                log.debug("PooledDataSource: Unknown severe error condition.  The connection pool returned a null connection.");
            }
            throw new SQLException("PooledDataSource: Unknown severe error condition.  The connection pool returned a null connection.");
        }

        return conn;
    }

    /**
     * 方法来检查连接是否仍然可用
     *
     * @param conn - 连接检查
     * @return True if the connection is still usable
     */
    protected boolean pingConnection(PooledConnection conn) {
        // 记录ping操作是否成功
        boolean result = true;

        try {
            // 检测真正的数据库连接时否已经关闭
            result = !conn.getRealConnection().isClosed();
        } catch (SQLException e) {
            if (log.isDebugEnabled()) {
                log.debug("Connection " + conn.getRealHashCode() + " is BAD: " + e.getMessage());
            }
            result = false;
        }

        if (result) {
            // 检测poolPingEnabled设置，是否运行执行测试SQL语句
            if (poolPingEnabled) {
                // 长时间未使用(超过poolPingConnectionsNotUsedFor时长)的连接才需要ping操作来检测数据库是否正常
                if (poolPingConnectionsNotUsedFor >= 0 && conn.getTimeElapsedSinceLastUse() > poolPingConnectionsNotUsedFor) {
                    try {
                        if (log.isDebugEnabled()) {
                            log.debug("Testing connection " + conn.getRealHashCode() + " ...");
                        }
                        // Java执行SQL语句的JDBC操作
                        Connection realConn = conn.getRealConnection();
                        Statement statement = realConn.createStatement();
                        ResultSet rs = statement.executeQuery(poolPingQuery);
                        rs.close();
                        statement.close();
                        if (!realConn.getAutoCommit()) {
                            realConn.rollback();
                        }
                        result = true;
                        if (log.isDebugEnabled()) {
                            log.debug("Connection " + conn.getRealHashCode() + " is GOOD!");
                        }
                    } catch (Exception e) {
                        log.warn("Execution of ping query '" + poolPingQuery + "' failed: " + e.getMessage());
                        try {
                            conn.getRealConnection().close();
                        } catch (Exception e2) {
                            //ignore
                        }
                        result = false;
                        if (log.isDebugEnabled()) {
                            log.debug("Connection " + conn.getRealHashCode() + " is BAD: " + e.getMessage());
                        }
                    }
                }
            }
        }
        return result;
    }

    /*
     * Unwraps a pooled connection to get to the 'real' connection
     *
     * @param conn - the pooled connection to unwrap
     * @return The 'real' connection
     */
    public static Connection unwrapConnection(Connection conn) {
        if (Proxy.isProxyClass(conn.getClass())) {
            InvocationHandler handler = Proxy.getInvocationHandler(conn);
            if (handler instanceof PooledConnection) {
                return ((PooledConnection) handler).getRealConnection();
            }
        }
        return conn;
    }

    protected void finalize() throws Throwable {
        forceCloseAll();
        super.finalize();
    }

    public <T> T unwrap(Class<T> iface) throws SQLException {
        throw new SQLException(getClass().getName() + " is not a wrapper.");
    }

    public boolean isWrapperFor(Class<?> iface) throws SQLException {
        return false;
    }

    public Logger getParentLogger() {
        return Logger.getLogger(Logger.GLOBAL_LOGGER_NAME); // requires JDK version 1.6
    }

}
