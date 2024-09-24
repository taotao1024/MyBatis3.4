/**
 * Copyright 2009-2015 the original author or authors.
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
package org.apache.ibatis.executor.resultset;

import org.apache.ibatis.cursor.Cursor;

import java.sql.CallableStatement;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.List;

/**
 * @author Clinton Begin
 */
public interface ResultSetHandler {
    /**
     * 获取statement 对象中的resultSet对象，对resultSet对象进行处理
     * @param stmt
     * @return
     * @param <E>
     * @throws SQLException
     */
    <E> List<E> handleResultSets(Statement stmt) throws SQLException;

    /**
     * 将ResultSet对象包装成Cursor对象，对Cursor进行遍历时，能够动态的从数据库查询数据，避免一次性将所以数据加载到内存中。
     * @param stmt
     * @return
     * @param <E>
     * @throws SQLException
     */
    <E> Cursor<E> handleCursorResultSets(Statement stmt) throws SQLException;

    /**
     * 处理存储过程调用结果
     * @param cs
     * @throws SQLException
     */
    void handleOutputParameters(CallableStatement cs) throws SQLException;

}
