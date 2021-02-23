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
package org.apache.ibatis.scripting.xmltags;

import java.util.HashMap;
import java.util.Map;

import ognl.OgnlContext;
import ognl.OgnlException;
import ognl.OgnlRuntime;
import ognl.PropertyAccessor;

import org.apache.ibatis.reflection.MetaObject;
import org.apache.ibatis.session.Configuration;

/**
 * 主要用于记录解析动态SQL语句之后产生的SQL语句片段，可以认为是一个用于记录动态SQL语句解析结果的容器
 *
 * @author Clinton Begin
 */
public class DynamicContext {

    /**
     * 参数对象key
     */
    public static final String PARAMETER_OBJECT_KEY = "_parameter";
    /**
     * 数据库IDkey
     */
    public static final String DATABASE_ID_KEY = "_databaseId";

    static {
        OgnlRuntime.setPropertyAccessor(ContextMap.class, new ContextAccessor());
    }

    /**
     * 参数上下文
     */
    private final ContextMap bindings;
    /**
     * 在SQLNode解析动态SQL时，会将解析后的SQL语句片段添加到该属性中保存，最终拼凑出一条完整的SQL语句
     */
    private final StringBuilder sqlBuilder = new StringBuilder();
    private int uniqueNumber = 0;

    /**
     * 初始化bindings集合
     *
     * @param configuration
     * @param parameterObject 运行时用户传入的实参 包含了后续用于替换#{}占位符的实参
     */
    public DynamicContext(Configuration configuration, Object parameterObject) {
        if (parameterObject != null && !(parameterObject instanceof Map)) {
            // 对于非Map类型的参数，会创建对应的MetaObject对象，并封装成ContextMap对象
            MetaObject metaObject = configuration.newMetaObject(parameterObject);
            // 初始化Bindings集合
            bindings = new ContextMap(metaObject);
        } else {
            bindings = new ContextMap(null);
        }
        bindings.put(PARAMETER_OBJECT_KEY, parameterObject);
        bindings.put(DATABASE_ID_KEY, configuration.getDatabaseId());
    }

    public Map<String, Object> getBindings() {
        return bindings;
    }

    public void bind(String name, Object value) {
        bindings.put(name, value);
    }

    /**
     * 追加SQL片段
     *
     * @param sql SQL片段
     */
    public void appendSql(String sql) {
        sqlBuilder.append(sql);
        sqlBuilder.append(" ");
    }

    /**
     * 获取解析后的完整的SQL语句
     *
     * @return 完整的SQL语句
     */
    public String getSql() {
        return sqlBuilder.toString().trim();
    }

    public int getUniqueNumber() {
        return uniqueNumber++;
    }

    /**
     * 内部类 重写了HashMap的get()方法
     */
    static class ContextMap extends HashMap<String, Object> {
        private static final long serialVersionUID = 2977601501966151582L;

        /**
         * 将用户传入的参数封装成了MetaObject对象
         */
        private MetaObject parameterMetaObject;

        public ContextMap(MetaObject parameterMetaObject) {
            this.parameterMetaObject = parameterMetaObject;
        }

        /**
         * 重写了get()方法
         *
         * @param key
         * @return
         */
        @Override
        public Object get(Object key) {
            String strKey = (String) key;
            // 如果ContextMap中已经包含了该Key，则直接返回
            if (super.containsKey(strKey)) {
                return super.get(strKey);
            }
            // 从运行时参数中查找对应的属性
            if (parameterMetaObject != null) {
                // issue #61 do not modify the context when reading
                return parameterMetaObject.getValue(strKey);
            }

            return null;
        }
    }

    static class ContextAccessor implements PropertyAccessor {

        @Override
        public Object getProperty(Map context, Object target, Object name)
                throws OgnlException {
            Map map = (Map) target;

            Object result = map.get(name);
            if (map.containsKey(name) || result != null) {
                return result;
            }

            Object parameterObject = map.get(PARAMETER_OBJECT_KEY);
            if (parameterObject instanceof Map) {
                return ((Map) parameterObject).get(name);
            }

            return null;
        }

        @Override
        public void setProperty(Map context, Object target, Object name, Object value)
                throws OgnlException {
            Map<Object, Object> map = (Map<Object, Object>) target;
            map.put(name, value);
        }

        @Override
        public String getSourceAccessor(OgnlContext arg0, Object arg1, Object arg2) {
            return null;
        }

        @Override
        public String getSourceSetter(OgnlContext arg0, Object arg1, Object arg2) {
            return null;
        }
    }
}