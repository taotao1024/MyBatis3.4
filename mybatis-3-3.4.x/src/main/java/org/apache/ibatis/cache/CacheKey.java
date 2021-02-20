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
package org.apache.ibatis.cache;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

import org.apache.ibatis.reflection.ArrayUtil;

/**
 * Cache中唯一确定一个缓存项需要使用缓存项的key
 * MyBatis中因为设计到动态SQL等多方面因素，其缓存项的key不仅仅通过一个String表示，所以MyBatis提供了CacheKey来来表示缓存项的key
 *
 * @author Clinton Begin
 */
public class CacheKey implements Cloneable, Serializable {

    private static final long serialVersionUID = 1146682552656046210L;

    public static final CacheKey NULL_CACHE_KEY = new NullCacheKey();

    private static final int DEFAULT_MULTIPLYER = 37;
    private static final int DEFAULT_HASHCODE = 17;
    /**
     * 参与计算hashcode，默认值是37
     */
    private final int multiplier;
    /**
     * CacheKey对象的hashcode，初始值是17
     */
    private int hashcode;
    /**
     * 校验和
     */
    private long checksum;
    /**
     * updateList集合的个数
     */
    private int count;
    // 8/21/2017 - Sonarlint flags this as needing to be marked transient.  While true if content is not serializable, this is not always true and thus should not be marked transient.
    /**
     * 由该集合中的所有对象共同决定两个CacheKey是否相同
     */
    private List<Object> updateList;

    public CacheKey() {
        this.hashcode = DEFAULT_HASHCODE;
        this.multiplier = DEFAULT_MULTIPLYER;
        this.count = 0;
        this.updateList = new ArrayList<Object>();
    }

    public CacheKey(Object[] objects) {
        this();
        updateAll(objects);
    }

    public int getUpdateCount() {
        return updateList.size();
    }

    public void update(Object object) {
        int baseHashCode = object == null ? 1 : ArrayUtil.hashCode(object);
        // 重新己算count、checksum、hashcode的值
        count++;
        checksum += baseHashCode;
        baseHashCode *= count;

        hashcode = multiplier * hashcode + baseHashCode;
        // 将object添加到updateList集合中
        updateList.add(object);
    }

    public void updateAll(Object[] objects) {
        for (Object o : objects) {
            update(o);
        }
    }

    @Override
    public boolean equals(Object object) {
        // 同一对象
        if (this == object) {
            return true;
        }
        // 相同类型
        if (!(object instanceof CacheKey)) {
            return false;
        }

        final CacheKey cacheKey = (CacheKey) object;
        // 比较hashcode
        if (hashcode != cacheKey.hashcode) {
            return false;
        }
        // 比较checksum
        if (checksum != cacheKey.checksum) {
            return false;
        }
        // 比较count
        if (count != cacheKey.count) {
            return false;
        }
        // 比较updateList中的每一项
        for (int i = 0; i < updateList.size(); i++) {
            Object thisObject = updateList.get(i);
            Object thatObject = cacheKey.updateList.get(i);
            if (!ArrayUtil.equals(thisObject, thatObject)) {
                return false;
            }
        }
        return true;
    }

    @Override
    public int hashCode() {
        return hashcode;
    }

    @Override
    public String toString() {
        StringBuilder returnValue = new StringBuilder().append(hashcode).append(':').append(checksum);
        for (Object object : updateList) {
            returnValue.append(':').append(ArrayUtil.toString(object));
        }
        return returnValue.toString();
    }

    @Override
    public CacheKey clone() throws CloneNotSupportedException {
        CacheKey clonedCacheKey = (CacheKey) super.clone();
        clonedCacheKey.updateList = new ArrayList<Object>(updateList);
        return clonedCacheKey;
    }

}
