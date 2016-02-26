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

import static org.junit.Assert.*;

import java.lang.management.ManagementFactory;

import javax.cache.Cache;
import javax.cache.CacheManager;
import javax.cache.Caching;
import javax.cache.configuration.MutableConfiguration;
import javax.cache.spi.CachingProvider;
import javax.management.MBeanServer;
import javax.management.ObjectName;

import org.junit.Test;

public class GuavaCacheStatisticsMXBeanTest
{
    @Test
    public void testCacheStatisticsBean()
        throws Exception
    {
        try (CachingProvider cachingProvider = Caching.getCachingProvider(GuavaCachingProvider.class.getName()))
        {
            CacheManager cacheManager = cachingProvider.getCacheManager();

            MutableConfiguration<String, Integer> configuration = new MutableConfiguration<>();

            configuration.setStoreByValue(false);
            configuration.setTypes(String.class, Integer.class);
            configuration.setStatisticsEnabled(true);

            Cache<String, Integer> statisticsCache = cacheManager.createCache("statisticsCache", configuration);

            statisticsCache.put("entry1", 1);
            statisticsCache.put("entry2", 2);
            statisticsCache.put("entry3", 3);

            statisticsCache.get("entry1");
            statisticsCache.get("entry2");
            statisticsCache.get("entry3");
            statisticsCache.get("entry4");

            MBeanServer beanServer = ManagementFactory.getPlatformMBeanServer();

            assertNotNull(beanServer);

            ObjectName name = new ObjectName(GuavaCacheStatisticsMXBean.getObjectName(statisticsCache));

            Object cacheHits = beanServer.getAttribute(name, "CacheHits");
            Object cacheMisses = beanServer.getAttribute(name, "CacheMisses");
            Object cacheHitPercentage = beanServer.getAttribute(name, "CacheHitPercentage");
            Object cacheMissPercentage = beanServer.getAttribute(name, "CacheMissPercentage");

            assertNotNull(cacheHits);
            assertNotNull(cacheMisses);
            assertNotNull(cacheHitPercentage);
            assertNotNull(cacheMissPercentage);

            assertEquals("cache hits", 3L, cacheHits);
            assertEquals("cache misses", 1L, cacheMisses);
            assertEquals("cache hit percentage", 0.75F, cacheHitPercentage);
            assertEquals("cache miss percentage", 0.25F, cacheMissPercentage);

            beanServer.invoke(name, "clear", null, null);

            cacheHits = beanServer.getAttribute(name, "CacheHits");
            cacheMisses = beanServer.getAttribute(name, "CacheMisses");
            cacheHitPercentage = beanServer.getAttribute(name, "CacheHitPercentage");
            cacheMissPercentage = beanServer.getAttribute(name, "CacheMissPercentage");

            assertEquals("cache hits", 0L, cacheHits);
            assertEquals("cache misses", 0L, cacheMisses);
            assertEquals("cache hit percentage", 1F, cacheHitPercentage);
            assertEquals("cache miss percentage", 0F, cacheMissPercentage);
        }
    }
}
