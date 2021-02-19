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
package org.apache.ibatis.binding;

import org.apache.ibatis.annotations.Flush;
import org.apache.ibatis.annotations.MapKey;
import org.apache.ibatis.cursor.Cursor;
import org.apache.ibatis.mapping.MappedStatement;
import org.apache.ibatis.mapping.SqlCommandType;
import org.apache.ibatis.mapping.StatementType;
import org.apache.ibatis.reflection.MetaObject;
import org.apache.ibatis.reflection.ParamNameResolver;
import org.apache.ibatis.reflection.TypeParameterResolver;
import org.apache.ibatis.session.Configuration;
import org.apache.ibatis.session.ResultHandler;
import org.apache.ibatis.session.RowBounds;
import org.apache.ibatis.session.SqlSession;

import java.lang.reflect.Array;
import java.lang.reflect.Method;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.*;

/**
 * 封装了Mapper接口中对应方法的信息，以及对应SQL语句的信息
 * <p>
 * 可以理解为连接Mapper接口以及映射配置文件中定义SQL语句的桥梁
 *
 * @author Clinton Begin
 * @author Eduardo Macarron
 * @author Lasse Voss
 */
public class MapperMethod {
    //记录了SQL语句的名字和类型
    private final SqlCommand command;
    // Mapper接口中对应方法的相关信息
    private final MethodSignature method;

    public MapperMethod(Class<?> mapperInterface, Method method, Configuration config) {
        this.command = new SqlCommand(config, mapperInterface, method);
        this.method = new MethodSignature(config, mapperInterface, method);
    }

    /**
     * 根据SQL语句类的类型调用SqlSession对应的方法，完成数据库操作
     *
     * @param sqlSession
     * @param args
     * @return
     */
    public Object execute(SqlSession sqlSession, Object[] args) {
        Object result;
        // 根据SQL语句类型，调用SqlSession对应的方法
        switch (command.getType()) {
            case INSERT: {
                // 使用ParamNameResolver处理args[]数组 将用户传入的实际参数与指定该参数名称关联起来
                Object param = method.convertArgsToSqlCommandParam(args);
                // 调用SqlSession.insert()方法，rowCountResult()方法会根据method字段中记录的方法的返回值对结果进行类型转换
                result = rowCountResult(sqlSession.insert(command.getName(), param));
                break;
            }
            case UPDATE: {
                // 使用ParamNameResolver处理args[]数组 将用户传入的实际参数与指定该参数名称关联起来
                Object param = method.convertArgsToSqlCommandParam(args);
                // 调用SqlSession.insert()方法，rowCountResult()方法会根据method字段中记录的方法的返回值对结果进行类型转换
                result = rowCountResult(sqlSession.update(command.getName(), param));
                break;
            }
            case DELETE: {
                // 使用ParamNameResolver处理args[]数组 将用户传入的实际参数与指定该参数名称关联起来
                Object param = method.convertArgsToSqlCommandParam(args);
                // 调用SqlSession.insert()方法，rowCountResult()方法会根据method字段中记录的方法的返回值对结果进行类型转换
                result = rowCountResult(sqlSession.delete(command.getName(), param));
                break;
            }
            case SELECT:
                if (method.returnsVoid() && method.hasResultHandler()) {
                    // 处理返回值为Void且ResultSet通过ResultHandler处理的方法
                    executeWithResultHandler(sqlSession, args);
                    result = null;
                } else if (method.returnsMany()) {
                    // 处理返回值为集合或数组的方法
                    result = executeForMany(sqlSession, args);
                } else if (method.returnsMap()) {
                    // 处理返回值为Map的方法
                    result = executeForMap(sqlSession, args);
                } else if (method.returnsCursor()) {
                    // 处理返回值为Cursor的方法
                    result = executeForCursor(sqlSession, args);
                } else {
                    // 处理返回值为单一对象的方法
                    Object param = method.convertArgsToSqlCommandParam(args);
                    result = sqlSession.selectOne(command.getName(), param);
                }
                break;
            case FLUSH:
                result = sqlSession.flushStatements();
                break;
            default:
                throw new BindingException("Unknown execution method for: " + command.getName());
        }
        // 边界检查
        if (result == null && method.getReturnType().isPrimitive() && !method.returnsVoid()) {
            throw new BindingException("Mapper method '" + command.getName()
                    + " attempted to return null from a method with a primitive return type (" + method.getReturnType() + ").");
        }
        return result;
    }

    private Object rowCountResult(int rowCount) {
        final Object result;
        if (method.returnsVoid()) {
            // mapper接口中相应的返回值为void
            result = null;
        } else if (Integer.class.equals(method.getReturnType()) || Integer.TYPE.equals(method.getReturnType())) {
            // mapper接口中相应的返回值为int或Integer
            result = rowCount;
        } else if (Long.class.equals(method.getReturnType()) || Long.TYPE.equals(method.getReturnType())) {
            // mapper接口中相应的返回值为long或Long
            result = (long) rowCount;
        } else if (Boolean.class.equals(method.getReturnType()) || Boolean.TYPE.equals(method.getReturnType())) {
            // mapper接口中相应的返回值为boolean或Boolean
            result = rowCount > 0;
        } else {
            // 以上条件都不成立 抛出异常
            throw new BindingException("Mapper method '" + command.getName() + "' has an unsupported return type: " + method.getReturnType());
        }
        return result;
    }

    /**
     * Mapper接口中定义的方法使用ResultHandler处理结果集
     *
     * @param sqlSession
     * @param args
     */
    private void executeWithResultHandler(SqlSession sqlSession, Object[] args) {
        // 获取SQL语句对应的MappedStatement对象，MappedStatement对象中记录了SQL语句的相关信息
        MappedStatement ms = sqlSession.getConfiguration().getMappedStatement(command.getName());
        // 当使用ResultHandler处理结果集时，必须指定ResultMap或ResultType
        if (!StatementType.CALLABLE.equals(ms.getStatementType())
                && void.class.equals(ms.getResultMaps().get(0).getType())) {
            throw new BindingException("method " + command.getName()
                    + " needs either a @ResultMap annotation, a @ResultType annotation,"
                    + " or a resultType attribute in XML so a ResultHandler can be used as a parameter.");
        }
        // 转换实参列表
        Object param = method.convertArgsToSqlCommandParam(args);
        // 检测参数列表中是否有RowBounds参数类型 RowBounds逻辑分页
        if (method.hasRowBounds()) {
            // 获取RowBounds对象，根据MethodSignature.rowBoundsIndex字段指定位置，从args数组中查找
            // 获取ResultHandler对象同理
            RowBounds rowBounds = method.extractRowBounds(args);
            // 调用SqlSession.select()方法执行查询，并由指定的ResultHandler处理结果对象
            sqlSession.select(command.getName(), param, rowBounds, method.extractResultHandler(args));
        } else {
            // 调用SqlSession.select()方法执行查询，并由指定的ResultHandler处理结果对象
            sqlSession.select(command.getName(), param, method.extractResultHandler(args));
        }
    }

    /**
     * Mapper接口中对应方法的返回值为Collection集合或数组
     *
     * @param sqlSession
     * @param args
     * @param <E>
     * @return
     */
    private <E> Object executeForMany(SqlSession sqlSession, Object[] args) {
        List<E> result;
        // 参数列表转换
        Object param = method.convertArgsToSqlCommandParam(args);
        // 检测到由逻辑分页参数
        if (method.hasRowBounds()) {
            RowBounds rowBounds = method.extractRowBounds(args);
            // 调用SqlSession.selectList()方法执行查询，
            result = sqlSession.<E>selectList(command.getName(), param, rowBounds);
        } else {
            // 调用SqlSession.selectList()方法执行查询，
            result = sqlSession.<E>selectList(command.getName(), param);
        }
        // issue #510 Collections & arrays support
        // 将结果集转换为数组或Collection集合
        if (!method.getReturnType().isAssignableFrom(result.getClass())) {
            if (method.getReturnType().isArray()) {
                // 数组
                return convertToArray(result);
            } else {
                // 集合
                return convertToDeclaredCollection(sqlSession.getConfiguration(), result);
            }
        }
        return result;
    }

    /**
     * 结果集转换为Cursor
     *
     * @param sqlSession
     * @param args
     * @param <T>
     * @return
     */
    private <T> Cursor<T> executeForCursor(SqlSession sqlSession, Object[] args) {
        Cursor<T> result;
        Object param = method.convertArgsToSqlCommandParam(args);
        if (method.hasRowBounds()) {
            RowBounds rowBounds = method.extractRowBounds(args);
            result = sqlSession.<T>selectCursor(command.getName(), param, rowBounds);
        } else {
            result = sqlSession.<T>selectCursor(command.getName(), param);
        }
        return result;
    }

    /**
     * 结果集转换为Collection集合
     *
     * @param config
     * @param list
     * @param <E>
     * @return
     */
    private <E> Object convertToDeclaredCollection(Configuration config, List<E> list) {
        // ObjectFactory通过反射创建集合对象
        Object collection = config.getObjectFactory().create(method.getReturnType());
        // 创建MataObject对象
        MetaObject metaObject = config.newMetaObject(collection);
        // 调用Collection.addAll()方法
        metaObject.addAll(list);
        return collection;
    }

    /**
     * 结果集转换为数组
     *
     * @param list
     * @param <E>
     * @return
     */
    @SuppressWarnings("unchecked")
    private <E> Object convertToArray(List<E> list) {
        // 获取数组元素类型
        Class<?> arrayComponentType = method.getReturnType().getComponentType();
        // 创建对象数组
        Object array = Array.newInstance(arrayComponentType, list.size());
        if (arrayComponentType.isPrimitive()) {
            // 遍历数组 将list中每一项都添加到数组中
            for (int i = 0; i < list.size(); i++) {
                Array.set(array, i, list.get(i));
            }
            return array;
        } else {
            return list.toArray((E[]) array);
        }
    }

    /**
     * 结果集转换为Map
     *
     * @param sqlSession
     * @param args
     * @param <K>
     * @param <V>
     * @return
     */
    private <K, V> Map<K, V> executeForMap(SqlSession sqlSession, Object[] args) {
        Map<K, V> result;
        // 转换实参列表
        Object param = method.convertArgsToSqlCommandParam(args);
        if (method.hasRowBounds()) {
            RowBounds rowBounds = method.extractRowBounds(args);
            // 调用SqlSession.selectMap()方法完成查询操作
            result = sqlSession.<K, V>selectMap(command.getName(), param, method.getMapKey(), rowBounds);
        } else {
            result = sqlSession.<K, V>selectMap(command.getName(), param, method.getMapKey());
        }
        return result;
    }

    /**
     * 内部类
     *
     * @param <V>
     */
    public static class ParamMap<V> extends HashMap<String, V> {

        private static final long serialVersionUID = -2212268410512043556L;

        @Override
        public V get(Object key) {
            if (!super.containsKey(key)) {
                throw new BindingException("Parameter '" + key + "' not found. Available parameters are " + keySet());
            }
            return super.get(key);
        }

    }

    /**
     * 内部类，使用name字段记录了SQL语句的名称、使用type字段积聚了SQL语句的类型
     */
    public static class SqlCommand {

        private final String name;
        private final SqlCommandType type;

        /**
         * 构造方法
         *
         * @param configuration
         * @param mapperInterface
         * @param method
         */
        public SqlCommand(Configuration configuration, Class<?> mapperInterface, Method method) {
            final String methodName = method.getName();
            // 声明类
            final Class<?> declaringClass = method.getDeclaringClass();
            // 解析MappedStatement
            MappedStatement ms = resolveMappedStatement(mapperInterface, methodName, declaringClass,
                    configuration);

            if (ms == null) {
                // 处理@Flush注解
                if (method.getAnnotation(Flush.class) != null) {
                    name = null;
                    type = SqlCommandType.FLUSH;
                } else {
                    throw new BindingException("Invalid bound statement (not found): "
                            + mapperInterface.getName() + "." + methodName);
                }
            } else {
                // 初始化name和type
                name = ms.getId();
                type = ms.getSqlCommandType();
                if (type == SqlCommandType.UNKNOWN) {
                    throw new BindingException("Unknown execution method for: " + name);
                }
            }
        }

        public String getName() {
            return name;
        }

        public SqlCommandType getType() {
            return type;
        }

        private MappedStatement resolveMappedStatement(Class<?> mapperInterface, String methodName,
                                                       Class<?> declaringClass, Configuration configuration) {
            // SQL语句的名称是由Mapper接口的名称和对应的方法名称组成的
            String statementId = mapperInterface.getName() + "." + methodName;
            // 检测是否有该名称的SQL语句
            if (configuration.hasStatement(statementId)) {
                // 从Configuration.mappedStatements集合中查找对应的MappedStatement对象
                // MappedStatement封装了Mapper中的全部信息，包括SQL语句相关的信息等
                return configuration.getMappedStatement(statementId);
            } else if (mapperInterface.equals(declaringClass)) {
                // 跳出递归
                return null;
            }
            for (Class<?> superInterface : mapperInterface.getInterfaces()) {
                // 指定方法是在父类接口中定义
                if (declaringClass.isAssignableFrom(superInterface)) {
                    // 递归
                    MappedStatement ms = resolveMappedStatement(superInterface, methodName,
                            declaringClass, configuration);
                    if (ms != null) {
                        return ms;
                    }
                }
            }
            return null;
        }
    }

    /**
     * 内部类 使用ParamNameResolver处理Mapper接口中定义的方法的参数列表
     */
    public static class MethodSignature {
        /**
         * 返回值类型是否为Collection类型或数组类型
         */
        private final boolean returnsMany;
        /**
         * 返回值类型是否为Map类型
         */
        private final boolean returnsMap;
        /**
         * 返回值类型是否Void类型
         */
        private final boolean returnsVoid;
        /**
         * 返回值类型是否Cursor类型
         */
        private final boolean returnsCursor;
        /**
         * 返回值类型
         */
        private final Class<?> returnType;
        /**
         * 返回值类型如果是Map,则该字段记录了作为Key的列名
         */
        private final String mapKey;
        /**
         * 用来标记该方法参数列表中ResultHandler类型参数的位置
         */
        private final Integer resultHandlerIndex;
        /**
         * 用来标记该方法参数列表中RowBounds类型参数的位置
         */
        private final Integer rowBoundsIndex;
        /**
         * 该方法对一个的ParamNameResolver对象
         */
        private final ParamNameResolver paramNameResolver;

        /**
         * 构造函数会解析相应的Method对象
         *
         * @param configuration
         * @param mapperInterface
         * @param method
         */
        public MethodSignature(Configuration configuration, Class<?> mapperInterface, Method method) {
            // 解析方法的返回值类型
            Type resolvedReturnType = TypeParameterResolver.resolveReturnType(method, mapperInterface);
            if (resolvedReturnType instanceof Class<?>) {
                this.returnType = (Class<?>) resolvedReturnType;
            } else if (resolvedReturnType instanceof ParameterizedType) {
                this.returnType = (Class<?>) ((ParameterizedType) resolvedReturnType).getRawType();
            } else {
                this.returnType = method.getReturnType();
            }
            // 初始化
            this.returnsVoid = void.class.equals(this.returnType);
            this.returnsMany = configuration.getObjectFactory().isCollection(this.returnType) || this.returnType.isArray();
            this.returnsCursor = Cursor.class.equals(this.returnType);
            // 若对应方法返回值是Map且使用了@MapKey注解
            this.mapKey = getMapKey(method);
            this.returnsMap = this.mapKey != null;
            // 初始化RowBounds、ResultHandler字段
            this.rowBoundsIndex = getUniqueParamIndex(method, RowBounds.class);
            this.resultHandlerIndex = getUniqueParamIndex(method, ResultHandler.class);
            // 创建ParamNameResolver对象
            this.paramNameResolver = new ParamNameResolver(configuration, method);
        }

        /**
         * 负责将args数组转换为SQL语句对应的参数烈
         *
         * @param args
         * @return
         */
        public Object convertArgsToSqlCommandParam(Object[] args) {
            return paramNameResolver.getNamedParams(args);
        }

        public boolean hasRowBounds() {
            return rowBoundsIndex != null;
        }

        public RowBounds extractRowBounds(Object[] args) {
            return hasRowBounds() ? (RowBounds) args[rowBoundsIndex] : null;
        }

        public boolean hasResultHandler() {
            return resultHandlerIndex != null;
        }

        public ResultHandler extractResultHandler(Object[] args) {
            return hasResultHandler() ? (ResultHandler) args[resultHandlerIndex] : null;
        }

        public String getMapKey() {
            return mapKey;
        }

        public Class<?> getReturnType() {
            return returnType;
        }

        public boolean returnsMany() {
            return returnsMany;
        }

        public boolean returnsMap() {
            return returnsMap;
        }

        public boolean returnsVoid() {
            return returnsVoid;
        }

        public boolean returnsCursor() {
            return returnsCursor;
        }

        /**
         * 查找指定类型的参数在参数列表中的位置
         *
         * @param method
         * @param paramType
         * @return
         */
        private Integer getUniqueParamIndex(Method method, Class<?> paramType) {
            Integer index = null;
            final Class<?>[] argTypes = method.getParameterTypes();
            // 遍历MethodSignature对应方法的参数列表
            for (int i = 0; i < argTypes.length; i++) {
                if (paramType.isAssignableFrom(argTypes[i])) {
                    // 记录ParamType类型参数在参数列表中的位置索引
                    if (index == null) {
                        index = i;
                    } else {
                        // RowBounds、ResultHandler类型的参数 只能有一个
                        throw new BindingException(method.getName() + " cannot have multiple " + paramType.getSimpleName() + " parameters");
                    }
                }
            }
            return index;
        }

        /**
         * 若对应方法返回值是Map且使用了@MapKey注解
         *
         * @param method
         * @return
         */
        private String getMapKey(Method method) {
            String mapKey = null;
            if (Map.class.isAssignableFrom(method.getReturnType())) {
                final MapKey mapKeyAnnotation = method.getAnnotation(MapKey.class);
                if (mapKeyAnnotation != null) {
                    mapKey = mapKeyAnnotation.value();
                }
            }
            return mapKey;
        }
    }

}
