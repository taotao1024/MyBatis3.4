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

import java.util.concurrent.TimeUnit;

/**
 * Setter from String to TimeUnit representation.
 *
 * @author Simone Tripodi
 */
final class TimeUnitSetter extends AbstractPropertySetter<TimeUnit> {

  /**
   * Instantiates a String to TimeUnit setter.
   */
  public TimeUnitSetter() {
    super("org.mybatis.caches.memcached.timeoutunit", "timeUnit", TimeUnit.SECONDS);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  protected TimeUnit convert(String property) throws Exception {
    return TimeUnit.valueOf(property.toUpperCase());
  }

}
