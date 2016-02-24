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

import com.google.common.cache.CacheStats;

public class GuavaCacheStatisticsMXBean
    implements javax.cache.management.CacheStatisticsMXBean
{
    private final Cache<?, ?> cache;

    private CacheStats snapshot = new CacheStats(0, 0, 0, 0, 0, 0);

    public GuavaCacheStatisticsMXBean(Cache<?, ?> cache)
    {
        this.cache = cache;
    }

    @Override
    public void clear()
    {
        CacheStats cacheStats = cache.unwrap(GuavaCache.class).stats();

        snapshot =
            new CacheStats(cacheStats.hitCount(),
                           cacheStats.missCount(),
                           cacheStats.loadSuccessCount(),
                           cacheStats.loadExceptionCount(),
                           cacheStats.totalLoadTime(),
                           cacheStats.evictionCount());
    }

    @Override
    public long getCacheHits()
    {
        return cache.unwrap(GuavaCache.class).stats().minus(snapshot).hitCount();
    }

    @Override
    public float getCacheHitPercentage()
    {
        return (float) cache.unwrap(GuavaCache.class).stats().minus(snapshot).hitRate();
    }

    @Override
    public long getCacheMisses()
    {
        return cache.unwrap(GuavaCache.class).stats().minus(snapshot).missCount();
    }

    @Override
    public float getCacheMissPercentage()
    {
        return (float) cache.unwrap(GuavaCache.class).stats().minus(snapshot).missRate();
    }

    @Override
    public long getCacheGets()
    {
        return cache.unwrap(GuavaCache.class).stats().minus(snapshot).loadCount();
    }

    @Override
    public long getCachePuts()
    {
        return -1; // unsupported
    }

    @Override
    public long getCacheRemovals()
    {
        return -1; // unsupported
    }

    @Override
    public long getCacheEvictions()
    {
        return cache.unwrap(GuavaCache.class).stats().minus(snapshot).evictionCount();
    }

    @Override
    public float getAverageGetTime()
    {
        return (float) (cache.unwrap(GuavaCache.class).stats().minus(snapshot).averageLoadPenalty() / 1000);
    }

    @Override
    public float getAveragePutTime()
    {
        return -1; // unsupported
    }

    @Override
    public float getAverageRemoveTime()
    {
        return -1; // unsupported
    }

    @Override
    public String toString()
    {
        return cache.unwrap(GuavaCache.class).stats().minus(snapshot).toString();
    }

    protected String getObjectName()
    {
        return getObjectName(cache);
    }

    protected static String getObjectName(Cache<?, ?> c)
    {
        StringBuilder builder = new StringBuilder("javax.cache:type=CacheStatistics");

        builder.append(",CacheManager=").append(c.getCacheManager().getURI().toString().replaceAll(":", "//"));
        builder.append(",Cache=").append(c.getName());

        return builder.toString();
    }
}
