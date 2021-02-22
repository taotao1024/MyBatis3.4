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
package org.apache.ibatis.mapping;

/**
 * Represents the content of a mapped statement read from an XML file or an annotation.
 * 从xml文件或注解映射的sql内容，主要就是用于创建BoundSql
 * It creates the SQL that will be passed to the database out of the input parameter received from the user.
 * 它创建将从用户接收的输入参数传递到数据库的SQL。
 *
 * @author Clinton Begin
 */
public interface SqlSource {

    /**
     * getBoundSql() 方法会根据映射文件或注解描述的SQL语句，以及传入的参数，返回可执行的SQL
     *
     * @param parameterObject
     * @return
     */
    BoundSql getBoundSql(Object parameterObject);

}
