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

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantLock;

import org.apache.ibatis.cache.Cache;
import org.apache.ibatis.cache.CacheException;

/**
 * 装饰着模式
 * <p>
 * 角色：具体装饰器
 * <p>
 * 阻塞版本的缓存装饰器，它会保证只有一个线程到库中查找指定Key对应的数据
 * <p>
 * Simple and inefficient version of EhCache's BlockingCache decorator.
 * It sets a lock over a cache key when the element is not found in cache.
 * This way, other threads will wait until this element is filled instead of hitting the database.
 *
 * @author Eduardo Macarron
 */
public class BlockingCache implements Cache {

    /**
     * 阻塞超时时长
     */
    private long timeout;
    /**
     * 被装饰的底层Cache对象
     */
    private final Cache delegate;
    /**
     * 每一个key都由对应的ReentrantLock对象
     */
    private final ConcurrentHashMap<Object, ReentrantLock> locks;

    public BlockingCache(Cache delegate) {
        this.delegate = delegate;
        this.locks = new ConcurrentHashMap<Object, ReentrantLock>();
    }

    @Override
    public String getId() {
        return delegate.getId();
    }

    @Override
    public int getSize() {
        return delegate.getSize();
    }

    @Override
    public void putObject(Object key, Object value) {
        try {
            // 向缓存中添加缓存项
            delegate.putObject(key, value);
        } finally {
            // 释放锁
            releaseLock(key);
        }
    }

    @Override
    public Object getObject(Object key) {
        // 获取key对应的锁
        acquireLock(key);
        // 查询key
        Object value = delegate.getObject(key);
        // 缓存由key对应的缓存项，释放锁，否则继续持有锁
        if (value != null) {
            // 释放key对应的锁
            releaseLock(key);
        }
        return value;
    }

    @Override
    public Object removeObject(Object key) {
        // despite of its name, this method is called only to release locks
        releaseLock(key);
        return null;
    }

    @Override
    public void clear() {
        delegate.clear();
    }

    @Override
    public ReadWriteLock getReadWriteLock() {
        return null;
    }

    /**
     * 通过key获取锁
     *
     * @param key
     * @return
     */
    private ReentrantLock getLockForKey(Object key) {
        // 创建ReentrantLock对象
        ReentrantLock lock = new ReentrantLock();
        // 尝试添加到locks集合中，如果locks集合已经有了相应的ReentrantLock对象，则使用locks集合中的ReentrantLock对象。
        ReentrantLock previous = locks.putIfAbsent(key, lock);
        return previous == null ? lock : previous;
    }

    /**
     * 获得锁
     * 长是获取指定key对应的锁，如果该key没有对应的锁对象，则为其创建新的ReentrantLock对象，在加锁。
     * 如果获取锁失败，则阻塞一段时间。
     *
     * @param key
     */
    private void acquireLock(Object key) {
        // 获取ReentrantLock对象
        Lock lock = getLockForKey(key);
        // 获取锁，超时时长
        if (timeout > 0) {
            try {
                boolean acquired = lock.tryLock(timeout, TimeUnit.MILLISECONDS);
                if (!acquired) {
                    throw new CacheException("Couldn't get a lock in " + timeout + " for the key " + key + " at the cache " + delegate.getId());
                }
            } catch (InterruptedException e) {
                // 超时则抛出异常
                throw new CacheException("Got interrupted while trying to acquire lock for key " + key, e);
            }
        } else {
            // 获取锁 不带超时时长
            lock.lock();
        }
    }

    /**
     * 释放锁
     *
     * @param key
     */
    private void releaseLock(Object key) {
        ReentrantLock lock = locks.get(key);
        //锁是否被当前线程持有
        if (lock.isHeldByCurrentThread()) {
            // 释放锁
            lock.unlock();
        }
    }

    public long getTimeout() {
        return timeout;
    }

    public void setTimeout(long timeout) {
        this.timeout = timeout;
    }
}