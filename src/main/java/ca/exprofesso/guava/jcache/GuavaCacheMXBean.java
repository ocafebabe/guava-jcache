/*
 * Copyright 2016 ExProfesso.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package ca.exprofesso.guava.jcache;

import javax.cache.Cache;
import javax.cache.configuration.CompleteConfiguration;

public class GuavaCacheMXBean
    implements javax.cache.management.CacheMXBean
{
    private final Cache<?, ?> cache;

    public GuavaCacheMXBean(Cache<?, ?> cache)
    {
        this.cache = cache;
    }

    @Override
    public String getKeyType()
    {
        return cache.getConfiguration(CompleteConfiguration.class).getKeyType().getName();
    }

    @Override
    public String getValueType()
    {
        return cache.getConfiguration(CompleteConfiguration.class).getValueType().getName();
    }

    @Override
    public boolean isReadThrough()
    {
        return cache.getConfiguration(CompleteConfiguration.class).isReadThrough();
    }

    @Override
    public boolean isWriteThrough()
    {
        return cache.getConfiguration(CompleteConfiguration.class).isWriteThrough();
    }

    @Override
    public boolean isStoreByValue()
    {
        return cache.getConfiguration(CompleteConfiguration.class).isStoreByValue();
    }

    @Override
    public boolean isStatisticsEnabled()
    {
        return cache.getConfiguration(CompleteConfiguration.class).isStatisticsEnabled();
    }

    @Override
    public boolean isManagementEnabled()
    {
        return cache.getConfiguration(CompleteConfiguration.class).isManagementEnabled();
    }

    protected String getObjectName()
    {
        return getObjectName(cache);
    }

    protected static String getObjectName(Cache<?, ?> c)
    {
        StringBuilder builder = new StringBuilder("javax.cache:type=Cache");

        builder.append(",CacheManager=").append(c.getCacheManager().getURI().toString().replaceAll(":", "//"));
        builder.append(",Cache=").append(c.getName());

        return builder.toString();
    }
}
