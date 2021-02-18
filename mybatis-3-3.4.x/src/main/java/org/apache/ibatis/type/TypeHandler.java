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
package org.apache.ibatis.type;

import java.sql.CallableStatement;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

/**
 * 类型转换接口<p>
 * 主要分为两类<p>
 * setParameter：主要负责将数据由Java类型转换成jdbcType类型<p>
 * getResult：主要负责将数据由jdbcType类型转换为Java类型<p>
 *
 * @author Clinton Begin
 */
public interface TypeHandler<T> {

    /**
     * 使用PreparedStatement为SQL语句绑定参数时，会将数据由Java类型转换成jdbcType类型
     *
     * @param ps        PreparedStatement
     * @param i
     * @param parameter
     * @param jdbcType
     * @throws SQLException
     */
    void setParameter(PreparedStatement ps, int i, T parameter, JdbcType jdbcType) throws SQLException;

    /**
     * 从ResultSet获取数据时，有限调用此方法，会将数据由jdbcType类型转换为Java类型
     *
     * @param rs         ResultSet
     * @param columnName
     * @return
     * @throws SQLException
     */
    T getResult(ResultSet rs, String columnName) throws SQLException;

    /**
     * 从ResultSet获取数据时，有限调用此方法，会将数据由jdbcType类型转换为Java类型
     *
     * @param rs
     * @param columnIndex
     * @return
     * @throws SQLException
     */
    T getResult(ResultSet rs, int columnIndex) throws SQLException;

    /**
     * 从ResultSet获取数据时，有限调用此方法，会将数据由jdbcType类型转换为Java类型
     *
     * @param cs
     * @param columnIndex
     * @return
     * @throws SQLException
     */
    T getResult(CallableStatement cs, int columnIndex) throws SQLException;

}
