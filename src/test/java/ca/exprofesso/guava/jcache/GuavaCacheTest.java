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
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;

import javax.cache.Cache;
import javax.cache.CacheManager;
import javax.cache.Caching;
import javax.cache.configuration.CacheEntryListenerConfiguration;
import javax.cache.configuration.Factory;
import javax.cache.configuration.MutableConfiguration;
import javax.cache.event.CacheEntryEvent;
import javax.cache.event.CacheEntryEventFilter;
import javax.cache.event.CacheEntryExpiredListener;
import javax.cache.event.CacheEntryListener;
import javax.cache.event.CacheEntryListenerException;
import javax.cache.event.CacheEntryRemovedListener;
import javax.cache.event.CacheEntryUpdatedListener;
import javax.cache.expiry.Duration;
import javax.cache.expiry.ModifiedExpiryPolicy;
import javax.cache.spi.CachingProvider;
import javax.management.MBeanServer;
import javax.management.ObjectName;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

public class GuavaCacheTest
{
    private CachingProvider cachingProvider;

    private CacheManager cacheManager;

    private Cache<String, Integer> cache;

    private final MutableConfiguration<String, Integer> configuration = new MutableConfiguration<>();

    @Before
    public void init()
    {
        cachingProvider = Caching.getCachingProvider(GuavaCachingProvider.class.getName());
        assertNotNull(cachingProvider);

        cacheManager = cachingProvider.getCacheManager();
        assertNotNull(cacheManager);

        configuration.setStoreByValue(false);
        configuration.setTypes(String.class, Integer.class);

        cache = cacheManager.createCache("cache", configuration);
        assertNotNull(cache);
        assertEquals("cache", cache.getName());
        assertEquals(cacheManager, cache.getCacheManager());
        assertEquals(configuration, cache.getConfiguration(MutableConfiguration.class));
    }

    @After
    public void close()
    {
        cachingProvider.close();

        assertTrue(cacheManager.isClosed());
        assertTrue(cache.isClosed());

        cachingProvider = null;
    }

    @Test
    public void testPutGet()
    {
        cache.put("1", 1);

        assertEquals(Integer.valueOf(1), cache.get("1"));
        assertEquals(1, cache.unwrap(GuavaCache.class).size());
    }

    @Test
    public void testPutAllGetAll()
    {
        Map<String, Integer> map = new HashMap<>();

        map.put("key1", 1);
        map.put("key2", 2);
        map.put("key3", 3);

        cache.putAll(map);

        Map<String, Integer> map2 = cache.getAll(map.keySet());

        assertEquals(map, map2);
    }

    @Test
    public void testContainsKey()
    {
        cache.put("1", 1);

        assertTrue(cache.containsKey("1"));
        assertFalse(cache.containsKey("2"));
    }

    @Test
    public void testPutIfAbsent()
    {
        assertTrue(cache.putIfAbsent("key", Integer.MIN_VALUE));
        assertFalse(cache.putIfAbsent("key", Integer.MIN_VALUE));
    }

    @Test
    public void testGetAndPut()
    {
        assertNull(cache.getAndPut("key", Integer.MIN_VALUE));
        assertEquals(Integer.valueOf(Integer.MIN_VALUE), cache.getAndPut("key", Integer.MAX_VALUE));
    }

    @Test
    public void testClear()
    {
        cache.put("1", 1);

        cache.clear();

        assertNull(cache.get("1"));
        assertEquals(0, cache.unwrap(GuavaCache.class).size());
    }

    @Test
    public void testRemove()
    {
        cache.put("1", 1);
        cache.put("2", 2);
        cache.put("3", 3);

        cache.remove("2");

        assertNotNull(cache.get("1"));
        assertNull(cache.get("2"));
        assertNotNull(cache.get("3"));
        assertEquals(2, cache.unwrap(GuavaCache.class).size());
    }

    @Test
    public void testRemoveWithValue()
    {
        cache.put("1", 1);

        assertFalse(cache.remove("1", 0));
        assertTrue(cache.remove("1", 1));
        assertEquals(0, cache.unwrap(GuavaCache.class).size());
    }

    @Test
    public void testGetAndRemove()
    {
        cache.put("1", 1);

        assertEquals(Integer.valueOf(1), cache.getAndRemove("1"));
        assertFalse(cache.containsKey("1"));
        assertEquals(0, cache.unwrap(GuavaCache.class).size());
    }

    @Test
    public void testGetAndReplace()
    {
        assertNull(cache.getAndReplace("1", 1));

        cache.put("1", 1);

        assertEquals(Integer.valueOf(1), cache.getAndReplace("1", 2));
        assertEquals(Integer.valueOf(2), cache.get("1"));
    }

    @Test
    public void testReplace()
    {
        assertFalse(cache.replace("1", 1));

        cache.put("1", 1);

        assertTrue(cache.replace("1", 2));
        assertEquals(Integer.valueOf(2), cache.get("1"));
    }

    @Test
    public void testReplaceWithValue()
    {
        assertFalse(cache.replace("1", 1, 2));

        cache.put("1", 1);

        assertFalse(cache.replace("1", 2, 1));
        assertTrue(cache.replace("1", 1, 2));
        assertEquals(Integer.valueOf(2), cache.get("1"));
    }

    @Test
    public void testRemoveAll()
    {
        cache.put("1", 1);

        cache.removeAll();

        assertNull(cache.get("1"));
        assertEquals(0, cache.unwrap(GuavaCache.class).size());
    }

    @Test
    public void testRemoveAllWithKeys()
    {
        cache.put("1", 1);
        cache.put("2", 2);
        cache.put("3", 3);

        Set<String> keys = new HashSet<>();

        keys.add("1");
        keys.add("3");

        cache.removeAll(keys);

        assertNull(cache.get("1"));
        assertNotNull(cache.get("2"));
        assertNull(cache.get("3"));
        assertEquals(1, cache.unwrap(GuavaCache.class).size());
    }

    @Test
    public void testIterator()
    {
        Map<String, Integer> map = new HashMap<>();

        map.put("1", 1);
        map.put("2", 2);
        map.put("3", 3);

        cache.putAll(map);

        Iterator<Cache.Entry<String, Integer>> i = cache.iterator();

        while (i.hasNext())
        {
            Cache.Entry<String, Integer> entry = i.next();

            String key = entry.getKey();
            Integer value = entry.getValue();

            assertNotNull(key);
            assertNotNull(value);

            assertEquals(value, map.remove(key));
        }

        assertTrue(map.isEmpty());
    }

    @Test(expected = IllegalStateException.class)
    public void testClosedCache()
    {
        cache.close();

        cache.get("test");
    }

    @Test
    public void testCacheManagementBean()
        throws Exception
    {
        MutableConfiguration<String, Integer> custom = new MutableConfiguration<>(configuration);

        custom.setManagementEnabled(true);

        Cache<String, Integer> managementCache = cacheManager.createCache("managementCache", custom);

        MBeanServer beanServer = ManagementFactory.getPlatformMBeanServer();

        assertNotNull(beanServer);

        ObjectName name = new ObjectName(GuavaCacheMXBean.getObjectName(managementCache));

        Object keyType = beanServer.getAttribute(name, "KeyType");
        Object valueType = beanServer.getAttribute(name, "ValueType");
        Object readThrough = beanServer.getAttribute(name, "ReadThrough");
        Object writeThrough = beanServer.getAttribute(name, "WriteThrough");
        Object storeByValue = beanServer.getAttribute(name, "StoreByValue");
        Object statisticsEnabled = beanServer.getAttribute(name, "StatisticsEnabled");
        Object managementEnabled = beanServer.getAttribute(name, "ManagementEnabled");

        assertNotNull(keyType);
        assertNotNull(valueType);
        assertNotNull(readThrough);
        assertNotNull(writeThrough);
        assertNotNull(storeByValue);
        assertNotNull(statisticsEnabled);
        assertNotNull(managementEnabled);

        assertEquals("java.lang.String", keyType);
        assertEquals("java.lang.Integer", valueType);
        assertFalse((boolean) readThrough);
        assertFalse((boolean) writeThrough);
        assertFalse((boolean) storeByValue);
        assertFalse((boolean) statisticsEnabled);
        assertTrue((boolean) managementEnabled);
    }

    @Test
    public void testCacheStatisticsBean()
        throws Exception
    {
        MutableConfiguration<String, Integer> custom = new MutableConfiguration<>(configuration);

        custom.setStatisticsEnabled(true);

        Cache<String, Integer> statisticsCache = cacheManager.createCache("statisticsCache", custom);

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

    @Test
    public void testCacheEntryListener()
        throws InterruptedException
    {
        final MyCacheEntryListener myCacheEntryListener = new MyCacheEntryListener();
        final MyCacheEntryEventFilter myCacheEntryEventFilter = new MyCacheEntryEventFilter();

        CacheEntryListenerConfiguration<String, Integer> listener =
            new CacheEntryListenerConfiguration<String, Integer>()
        {
            @Override
            public Factory<CacheEntryListener<? super String, ? super Integer>> getCacheEntryListenerFactory()
            {
                return new Factory<CacheEntryListener<? super String, ? super Integer>>()
                {
                    @Override
                    public CacheEntryListener<? super String, ? super Integer> create()
                    {
                        return myCacheEntryListener;
                    }
                };
            }

            @Override
            public boolean isOldValueRequired()
            {
                return false;
            }

            @Override
            public Factory<CacheEntryEventFilter<? super String, ? super Integer>> getCacheEntryEventFilterFactory()
            {
                return new Factory<CacheEntryEventFilter<? super String, ? super Integer>>()
                {
                    @Override
                    public CacheEntryEventFilter<? super String, ? super Integer> create()
                    {
                        return myCacheEntryEventFilter;
                    }
                };
            }

            @Override
            public boolean isSynchronous()
            {
                return true;
            }
        };

        MutableConfiguration<String, Integer> configuration2 = new MutableConfiguration<>(configuration);

        configuration2.addCacheEntryListenerConfiguration(listener);

        Cache<String, Integer> cache2 = cacheManager.createCache("cache2", configuration2);

        cache2.put("entry", 100);
        cache2.put("entry", 101);
        cache2.remove("entry");

        assertEquals(1, myCacheEntryListener.getUpdated());
        assertEquals(1, myCacheEntryListener.getRemoved());

        MutableConfiguration<String, Integer> configuration3 = new MutableConfiguration<>(configuration);

        configuration3.addCacheEntryListenerConfiguration(listener);
        configuration3.setExpiryPolicyFactory(ModifiedExpiryPolicy.factoryOf(new Duration(TimeUnit.MILLISECONDS, 1)));

        Cache<String, Integer> cache3 = cacheManager.createCache("cache3", configuration3);

        cache3.put("entry1", 101);
        cache3.put("entry2", 102);
        cache3.put("entry3", 103);

        Thread.sleep(1000); // REVIEW: there's got to be a better way to test this use case?

        cache3.unwrap(GuavaCache.class).cleanUp();

        assertEquals(3, myCacheEntryListener.getExpired());
    }

    private static class MyCacheEntryListener
        implements CacheEntryExpiredListener<String, Integer>,
                   CacheEntryRemovedListener<String, Integer>,
                   CacheEntryUpdatedListener<String, Integer>
    {
        int expired = 0;
        int removed = 0;
        int updated = 0;

        public int getExpired()
        {
            return expired;
        }

        public int getRemoved()
        {
            return removed;
        }

        public int getUpdated()
        {
            return updated;
        }

        @Override
        public void onExpired(Iterable<CacheEntryEvent<? extends String, ? extends Integer>> events)
            throws CacheEntryListenerException
        {
            expired++;
        }

        @Override
        public void onRemoved(Iterable<CacheEntryEvent<? extends String, ? extends Integer>> events)
            throws CacheEntryListenerException
        {
            removed++;
        }

        @Override
        public void onUpdated(Iterable<CacheEntryEvent<? extends String, ? extends Integer>> events)
            throws CacheEntryListenerException
        {
            updated++;
        }
    }

    private static class MyCacheEntryEventFilter
        implements CacheEntryEventFilter<String, Integer>
    {
        @Override
        public boolean evaluate(CacheEntryEvent<? extends String, ? extends Integer> event)
            throws CacheEntryListenerException
        {
            switch (event.getEventType())
            {
                case EXPIRED:
                    return true;

                case REMOVED:
                    return true;

                case UPDATED:
                    return true;
            }

            return false;
        }
    }
}
