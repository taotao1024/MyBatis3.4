/**
 * Copyright 2015-2018 the original author or authors.
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
package org.mybatis.caches.redis;

import org.apache.ibatis.cache.Cache;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;

import java.util.Map;
import java.util.concurrent.locks.ReadWriteLock;

/**
 * Cache adapter for Redis.
 * 实现Cache接口 使用Jedis客户端操作Redis
 *
 * @author Eduardo Macarron
 */
public final class RedisCache implements Cache {

    private final ReadWriteLock readWriteLock = new DummyReadWriteLock();

    private String id;

    private static JedisPool pool;

    private final RedisConfig redisConfig;

    private Integer timeout;

    public RedisCache(final String id) {
        if (id == null) {
            throw new IllegalArgumentException("Cache instances require an ID");
        }
        this.id = id;
        // 通过RedisConfigurationBuilder对象，获取Redis配置信息
        redisConfig = RedisConfigurationBuilder.getInstance().parseConfiguration();
        // 实例化JedisPool，与Redis服务器建立连接
        pool = new JedisPool(redisConfig, redisConfig.getHost(), redisConfig.getPort(), redisConfig.getConnectionTimeout(),
                redisConfig.getSoTimeout(), redisConfig.getPassword(), redisConfig.getDatabase(), redisConfig.getClientName(),
                redisConfig.isSsl(), redisConfig.getSslSocketFactory(), redisConfig.getSslParameters(),
                redisConfig.getHostnameVerifier());
    }

    // TODO Review this is UNUSED
    private Object execute(RedisCallback callback) {
        Jedis jedis = pool.getResource();
        try {
            return callback.doWithRedis(jedis);
        } finally {
            jedis.close();
        }
    }

    @Override
    public String getId() {
        return this.id;
    }

    @Override
    public int getSize() {
        return (Integer) execute(new RedisCallback() {
            @Override
            public Object doWithRedis(Jedis jedis) {
                Map<byte[], byte[]> result = jedis.hgetAll(id.getBytes());
                return result.size();
            }
        });
    }

    /**
     * 写缓存操作
     *
     * @param key
     * @param value
     */
    @Override
    public void putObject(final Object key, final Object value) {
        execute(new RedisCallback() {
            @Override
            public Object doWithRedis(Jedis jedis) {
                final byte[] idBytes = id.getBytes();
                // 对数据进行实例化 提供两种实例化策略
                // JDK内置实例化机制、第三方序列化框架Kryo
                jedis.hset(idBytes, key.toString().getBytes(), redisConfig.getSerializer().serialize(value));
                if (timeout != null && jedis.ttl(idBytes) == -1) {
                    jedis.expire(idBytes, timeout);
                }
                return null;
            }
        });
    }

    /**
     * 都缓存操作
     *
     * @param key
     * @return
     */
    @Override
    public Object getObject(final Object key) {
        return execute(new RedisCallback() {
            @Override
            public Object doWithRedis(Jedis jedis) {
                // 先根据Key获取序列化对象信息，在进行反序列化操作
                return redisConfig.getSerializer().unserialize(
                        jedis.hget(id.getBytes(), key.toString().getBytes())
                );
            }
        });
    }

    @Override
    public Object removeObject(final Object key) {
        return execute(new RedisCallback() {
            @Override
            public Object doWithRedis(Jedis jedis) {
                return jedis.hdel(id, key.toString());
            }
        });
    }

    @Override
    public void clear() {
        execute(new RedisCallback() {
            @Override
            public Object doWithRedis(Jedis jedis) {
                jedis.del(id);
                return null;
            }
        });

    }

    @Override
    public ReadWriteLock getReadWriteLock() {
        return readWriteLock;
    }

    @Override
    public String toString() {
        return "Redis {" + id + "}";
    }

    public void setTimeout(Integer timeout) {
        this.timeout = timeout;
    }

}
