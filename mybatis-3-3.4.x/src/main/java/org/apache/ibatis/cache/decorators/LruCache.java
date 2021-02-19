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
package org.apache.ibatis.cache.decorators;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.locks.ReadWriteLock;

import org.apache.ibatis.cache.Cache;

/**
 * 装饰着模式
 * <p>
 * 角色：具体装饰器
 * <p>
 * Lru (least recently used) cache decorator
 * <p>
 * 最少使用算法进行清楚缓存的装饰器，在需要清理缓存时，会清除最近最少使用的缓存项。
 *
 * @author Clinton Begin
 */
public class LruCache implements Cache {
    /**
     * 被封装的底层Cache对象
     */
    private final Cache delegate;
    /**
     * LinkedHashMap<Object, Object>类型对象 时一个有序的HashMap，用于记录key最近使用的情况
     */
    private Map<Object, Object> keyMap;
    /**
     * 记录最少被使用的缓存项的key
     */
    private Object eldestKey;

    public LruCache(Cache delegate) {
        this.delegate = delegate;
        // 默认缓存大小是1024
        setSize(1024);
    }

    @Override
    public String getId() {
        return delegate.getId();
    }

    @Override
    public int getSize() {
        return delegate.getSize();
    }

    public void setSize(final int size) {
        // LinkedHashMap构造函数的第三个参数，true表示该LinkedHashMap记录的顺序是access-order，
        // 也就是说LinkedHashMap.get()方法会改变其记录的顺序
        keyMap = new LinkedHashMap<Object, Object>(size, .75F, true) {
            private static final long serialVersionUID = 4267176411845948333L;

            // 当调用LinkedHashMap.put()方法时，会调用该方法。
            @Override
            protected boolean removeEldestEntry(Map.Entry<Object, Object> eldest) {
                boolean tooBig = size() > size;
                // 如果已经达到缓存上限，则更新eldestKey字段，后面的会删除该项
                if (tooBig) {
                    eldestKey = eldest.getKey();
                }
                return tooBig;
            }
        };
    }

    @Override
    public void putObject(Object key, Object value) {
        // 添加缓存项
        delegate.putObject(key, value);
        // 删除最久未使用的缓存项
        cycleKeyList(key);
    }

    @Override
    public Object getObject(Object key) {
        // 修改LinkedHashMap中记录的顺序
        keyMap.get(key); //touch
        return delegate.getObject(key);
    }

    @Override
    public Object removeObject(Object key) {
        return delegate.removeObject(key);
    }

    @Override
    public void clear() {
        delegate.clear();
        keyMap.clear();
    }

    @Override
    public ReadWriteLock getReadWriteLock() {
        return null;
    }

    private void cycleKeyList(Object key) {
        keyMap.put(key, key);
        // eldestKey不为空，表示已经达到了缓存上限
        if (eldestKey != null) {
            // 删除最久未使用的缓存项
            delegate.removeObject(eldestKey);
            eldestKey = null;
        }
    }

}
