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

import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

import javax.cache.Cache;
import javax.cache.CacheManager;
import javax.cache.Caching;
import javax.cache.configuration.MutableConfiguration;
import javax.cache.spi.CachingProvider;

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
}
