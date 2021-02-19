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

import java.lang.ref.ReferenceQueue;
import java.lang.ref.SoftReference;
import java.util.Deque;
import java.util.LinkedList;
import java.util.concurrent.locks.ReadWriteLock;

import org.apache.ibatis.cache.Cache;

/**
 * 装饰着模式
 * <p>
 * 角色：具体装饰器
 * <p>
 * Soft Reference cache decorator
 * Thanks to Dr. Heinz Kabutz for his guidance here.
 * <p>
 * 软引用缓存
 *
 * @author Clinton Begin
 */
public class SoftCache implements Cache {
    /**
     * 在SoftCache中，最近使用的一部分缓存项不会被GC回收，这就是通过value添加到hardLinksToAvoidGarbageCollection集合中实现的
     * (即有强引用指向其value) hardLinksToAvoidGarbageCollection集合是LinkedList类型
     */
    private final Deque<Object> hardLinksToAvoidGarbageCollection;
    /**
     * 弱引用队列 用于记录已经被GC回收的缓存项所对应的SoftEntry对象
     */
    private final ReferenceQueue<Object> queueOfGarbageCollectedEntries;
    /**
     * 底层被装饰的底层Cache对象
     */
    private final Cache delegate;
    /**
     * 强连接的个数 默认是256
     */
    private int numberOfHardLinks;

    public SoftCache(Cache delegate) {
        this.delegate = delegate;
        this.numberOfHardLinks = 256;
        this.hardLinksToAvoidGarbageCollection = new LinkedList<Object>();
        this.queueOfGarbageCollectedEntries = new ReferenceQueue<Object>();
    }

    @Override
    public String getId() {
        return delegate.getId();
    }

    @Override
    public int getSize() {
        removeGarbageCollectedItems();
        return delegate.getSize();
    }


    public void setSize(int size) {
        this.numberOfHardLinks = size;
    }

    @Override
    public void putObject(Object key, Object value) {
        removeGarbageCollectedItems();
        delegate.putObject(key, new SoftEntry(key, value, queueOfGarbageCollectedEntries));
    }

    @Override
    public Object getObject(Object key) {
        Object result = null;
        @SuppressWarnings("unchecked") // assumed delegate cache is totally managed by this cache
                SoftReference<Object> softReference = (SoftReference<Object>) delegate.getObject(key);
        if (softReference != null) {
            result = softReference.get();
            if (result == null) {
                delegate.removeObject(key);
            } else {
                // See #586 (and #335) modifications need more than a read lock
                synchronized (hardLinksToAvoidGarbageCollection) {
                    hardLinksToAvoidGarbageCollection.addFirst(result);
                    if (hardLinksToAvoidGarbageCollection.size() > numberOfHardLinks) {
                        hardLinksToAvoidGarbageCollection.removeLast();
                    }
                }
            }
        }
        return result;
    }

    @Override
    public Object removeObject(Object key) {
        removeGarbageCollectedItems();
        return delegate.removeObject(key);
    }

    @Override
    public void clear() {
        synchronized (hardLinksToAvoidGarbageCollection) {
            hardLinksToAvoidGarbageCollection.clear();
        }
        removeGarbageCollectedItems();
        delegate.clear();
    }

    @Override
    public ReadWriteLock getReadWriteLock() {
        return null;
    }

    private void removeGarbageCollectedItems() {
        SoftEntry sv;
        while ((sv = (SoftEntry) queueOfGarbageCollectedEntries.poll()) != null) {
            delegate.removeObject(sv.key);
        }
    }

    /**
     * SoftCache中缓存项的value是SoftEntry对象，SoftEntry继承了SoftReference，其中指向key的引用时强引用，而指向value的引用
     * 时软引用
     */
    private static class SoftEntry extends SoftReference<Object> {
        private final Object key;

        SoftEntry(Object key, Object value, ReferenceQueue<Object> garbageCollectionQueue) {
            // 指向value的引用时软引用，且关联了引用队列
            super(value, garbageCollectionQueue);
            // 强引用
            this.key = key;
        }
    }

}