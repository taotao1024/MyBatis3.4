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
 * <p>
 * Java提供的4中引用类型：强引用(Strong Reference)、软引用(Soft Reference)、弱引用(Weak Reference)、幽灵引用(Phantom Reference)
 * <p>
 * 强引用：
 * 强引用时Java编程中最普通的引用，例如Object obj = new Object()中，如果一个对象被强引用，即使时java虚拟机内存空间不足时，GC
 * 也绝不会回收该对象。当Java虚拟机内存不足时，会导致内存溢出，也就是常见的OOM异常。
 * <p>
 * 软引用：
 * 引用强度仅弱于强引用的一种引用。当Java虚拟机内存不足时，GC会回收那些只被软引用指向的对象，从而避免内存溢出。在GC试方了那些只被
 * 软引用指向的对象后，虚拟机内存依然不足，才会抛出OOM异常。软引用适合引用那些可以通过其他方式恢复的对象，例如数据库缓存中的对象就
 * 可以从数据库中恢复，所以使用软引用可以实现缓存。由于程序在使用软引用之前的某个时刻，其所指的对象可能已经被GC回收了，所以通过
 * Reference.get()方法来获取软引用所指的对象时，总是要通过检查该对象返回值是否为null，来判断软引用对象是否存活。
 * <p>
 * 弱引用：
 * 引用强度比软引用还要弱，它可以引用一个对象，但并不阻止被引用的对象被GC回收。在虚拟机进行垃圾回收时，如果指向一个对象的所有引用
 * 都是弱引用，那么该对象回被回收。由此可见、被弱引用指向的对象生存周期时两次GC之间的这段时间。而只被软引用指向的对象可以经历多
 * 次GC,直到出现内存紧张的情况才被回收。弱引用典型场景时JDK提供的java.util.WeakHashMap。WeakHashMap.Entry实现继承了
 * WeakReference，Entry弱引用key、强引用Value。当不在由强引用指向Key时，则Key可以被垃圾回收，当Key被回收后，对应的Entry对象
 * 会被Java虚拟机加入到其关联的队列中，当程序下一次操作WeakHashMap时，例如对WeakHashMap进行扩容操作，就会遍历关联的引用队列，
 * 将其中的Entry对象，从WeakHashMap中删除。
 * <p>
 * 幽灵引用：
 * Java提供的对象终止化机制，每个对象的Finalize()方法至多由GC执行一次，由于使用finalize()方法还会导致严重的内存消耗和性能损失，
 * 现在被弃用，取而代之的是幽灵引用。幽灵引用又被称为虚引用，幽灵引用可以实现一些比较精细的内粗能使用控制。
 * <p>
 * 引用队列：
 * 很多场景下，我们的程序需要在一个对象的可达性发生变化时得到通知，引用队列就是用于收集这些信息的队列。在创建SoftReference对象时，
 * 可以为其关联一个引用队列，当SoftReference所引用的对象被GC回收时，Java虚拟机就会将SoftReference对象添加到与之关联的引用队列
 * 中。当需要检测这些通知信息时，就可以从引用队列中获取这些SoftReference对象。
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
        // 清除已经被GC回收的缓存项
        removeGarbageCollectedItems();
        // 向缓存中添加缓存项
        delegate.putObject(key, new SoftEntry(key, value, queueOfGarbageCollectedEntries));
    }

    /**
     * 除了从缓存中查找对应的value，处理被GC回收的value对应的缓存项，还会更新hardLinksToAvoidGarbageCollection集合
     *
     * @param key The key
     * @return
     */
    @Override
    public Object getObject(Object key) {
        Object result = null;
        @SuppressWarnings("unchecked") // assumed delegate cache is totally managed by this cache
                // 从缓存中查找对应的缓存项
                SoftReference<Object> softReference = (SoftReference<Object>) delegate.getObject(key);
        // 检测缓存中是否有对应的缓存项
        if (softReference != null) {
            // 获取SoftReference引用的value
            result = softReference.get();
            // 已经被GC回收
            if (result == null) {
                // 从缓存中清除对应的缓存项
                delegate.removeObject(key);
            } else {
                // See #586 (and #335) modifications need more than a read lock
                // 未被GC回收
                synchronized (hardLinksToAvoidGarbageCollection) {
                    // 缓存向的value添加到hardLinksToAvoidGarbageCollection集合中保存
                    hardLinksToAvoidGarbageCollection.addFirst(result);
                    // 超过numberOfHardLinks，则将最老的缓存项从hardLinksToAvoidGarbageCollection集合中移除，类似于FIFO队列
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
            // 清理强引用集合
            hardLinksToAvoidGarbageCollection.clear();
        }
        // 清理被GC回收的缓存项
        removeGarbageCollectedItems();
        // 清空缓存中的缓存项
        delegate.clear();
    }

    @Override
    public ReadWriteLock getReadWriteLock() {
        return null;
    }

    /**
     * 清除已经被GC回收的缓存项
     */
    private void removeGarbageCollectedItems() {
        SoftEntry sv;
        // 遍历queueOfGarbageCollectedEntries集合
        while ((sv = (SoftEntry) queueOfGarbageCollectedEntries.poll()) != null) {
            // 将已经被GC回收的value对象对应的缓存项清除
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