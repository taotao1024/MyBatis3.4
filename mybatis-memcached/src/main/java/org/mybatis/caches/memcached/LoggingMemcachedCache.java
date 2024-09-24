/*
 *    Copyright 2012-2022 the original author or authors.
 *
 *    Licensed under the Apache License, Version 2.0 (the "License");
 *    you may not use this file except in compliance with the License.
 *    You may obtain a copy of the License at
 *
 *       https://www.apache.org/licenses/LICENSE-2.0
 *
 *    Unless required by applicable law or agreed to in writing, software
 *    distributed under the License is distributed on an "AS IS" BASIS,
 *    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *    See the License for the specific language governing permissions and
 *    limitations under the License.
 */
package org.mybatis.caches.memcached;

import org.apache.ibatis.cache.decorators.LoggingCache;

/**
 * {@code LoggingCache} adapter for Memcached.
 *
 * @author Simone Tripodi
 */
public final class LoggingMemcachedCache extends LoggingCache {

  public LoggingMemcachedCache(final String id) {
    super(new MemcachedCache(id));
  }

}
