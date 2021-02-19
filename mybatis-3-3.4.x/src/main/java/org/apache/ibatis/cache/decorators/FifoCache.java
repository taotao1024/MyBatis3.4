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
package org.apache.ibatis.cache.decorators;

import java.util.Deque;
import java.util.LinkedList;
import java.util.concurrent.locks.ReadWriteLock;

import org.apache.ibatis.cache.Cache;

/**
 * 装饰着模式
 * <p>
 * 角色：具体装饰器
 * <p>
 * 为了控制缓存的大小，需要按照一定规则清理缓存。FifoCache是先入先出版本的装饰器
 * 当缓存项的个数已经到达上限，则会将缓存中最老的缓存项删除。
 * <p>
 * FIFO (first in, first out) cache decorator
 *
 * @author Clinton Begin
 */
public class FifoCache implements Cache {
    /**
     * 被装饰的底层Cache对象
     */
    private final Cache delegate;
    /**
     * 用于记录key进入缓存的先后顺序，使用的是LinkedList类型的集合对象
     */
    private final Deque<Object> keyList;
    /**
     * 记录了缓存项的上限，超过该值则需要清理最老的缓存项
     */
    private int size;

    public FifoCache(Cache delegate) {
        this.delegate = delegate;
        this.keyList = new LinkedList<Object>();
        this.size = 1024;
    }

    @Override
    public String getId() {
        return delegate.getId();
    }

    @Override
    public int getSize() {
        return delegate.getSize();
    }

    public void setSize(int size) {
        this.size = size;
    }

    @Override
    public void putObject(Object key, Object value) {
        // 检查并清除缓存
        cycleKeyList(key);
        // 添加缓存项
        delegate.putObject(key, value);
    }

    @Override
    public Object getObject(Object key) {
        return delegate.getObject(key);
    }

    @Override
    public Object removeObject(Object key) {
        return delegate.removeObject(key);
    }

    @Override
    public void clear() {
        delegate.clear();
        keyList.clear();
    }

    @Override
    public ReadWriteLock getReadWriteLock() {
        return null;
    }

    /**
     * 检查并清除缓存
     *
     * @param key
     */
    private void cycleKeyList(Object key) {
        // 记录key
        keyList.addLast(key);
        // 如果达到缓存上线，则清理最老的缓存项
        if (keyList.size() > size) {
            Object oldestKey = keyList.removeFirst();
            delegate.removeObject(oldestKey);
        }
    }

}
