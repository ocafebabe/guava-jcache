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
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

import javax.cache.Cache;
import javax.cache.CacheManager;
import javax.cache.Caching;
import javax.cache.configuration.Factory;
import javax.cache.configuration.MutableConfiguration;
import javax.cache.integration.CacheLoader;
import javax.cache.integration.CacheLoaderException;
import javax.cache.processor.EntryProcessor;
import javax.cache.processor.EntryProcessorException;
import javax.cache.processor.MutableEntry;
import javax.cache.spi.CachingProvider;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import com.google.common.collect.Sets;

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
        cacheManager.destroyCache(cache.getName());

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

        Set<String> keys = Sets.newHashSet("1", "3");

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
    public void testInvoke()
    {
        final CacheLoader<String, Integer> cacheLoader = new CacheLoader<String, Integer>()
        {
            @Override
            public Integer load(String key)
                throws CacheLoaderException
            {
                return Integer.valueOf(key);
            }

            @Override
            public Map<String, Integer> loadAll(Iterable<? extends String> keys)
                throws CacheLoaderException
            {
                throw new UnsupportedOperationException("Not supported yet.");
            }
        };

        final EntryProcessor<String, Integer, Boolean> entryProcessor = new EntryProcessor<String, Integer, Boolean>()
        {
            @Override
            public Boolean process(MutableEntry<String, Integer> entry, Object... arguments)
                throws EntryProcessorException
            {
                assertTrue(entry.exists());
                assertEquals(Integer.valueOf(1), entry.getValue());
                entry.setValue(2);
                assertEquals(Integer.valueOf(2), entry.getValue());
                entry.remove();
                assertFalse(entry.exists());

                return Boolean.TRUE;
            }
        };

        MutableConfiguration<String, Integer> custom = new MutableConfiguration<>(configuration);

        custom.setReadThrough(true);
        custom.setCacheLoaderFactory
        (
            new Factory<CacheLoader<String, Integer>>()
            {
                @Override
                public CacheLoader<String, Integer> create()
                {
                    return cacheLoader;
                }
            }
        );

        Cache<String, Integer> invokingCache = cacheManager.createCache("invokingCache", custom);

        assertTrue(invokingCache.invoke("1", entryProcessor));
        assertFalse(invokingCache.containsKey("1"));
    }

    @Test
    public void testInvokeAll()
    {
        final CacheLoader<String, Integer> cacheLoader = new CacheLoader<String, Integer>()
        {
            @Override
            public Integer load(String key)
                throws CacheLoaderException
            {
                return Integer.valueOf(key);
            }

            @Override
            public Map<String, Integer> loadAll(Iterable<? extends String> keys)
                throws CacheLoaderException
            {
                throw new UnsupportedOperationException("Not supported yet.");
            }
        };

        final EntryProcessor<String, Integer, Void> entryProcessor1 = new EntryProcessor<String, Integer, Void>()
        {
            @Override
            public Void process(MutableEntry<String, Integer> entry, Object... arguments)
                throws EntryProcessorException
            {
                assertTrue(entry.exists());
                assertEquals(Integer.valueOf(1), entry.getValue());
                entry.setValue(2);
                assertEquals(Integer.valueOf(2), entry.getValue());

                return null;
            }
        };

        final EntryProcessor<String, Integer, Void> entryProcessor2 = new EntryProcessor<String, Integer, Void>()
        {
            @Override
            public Void process(MutableEntry<String, Integer> entry, Object... arguments)
                throws EntryProcessorException
            {
                entry.remove();

                return null;
            }
        };

        MutableConfiguration<String, Integer> custom = new MutableConfiguration<>(configuration);

        custom.setReadThrough(true);
        custom.setCacheLoaderFactory
        (
            new Factory<CacheLoader<String, Integer>>()
            {
                @Override
                public CacheLoader<String, Integer> create()
                {
                    return cacheLoader;
                }
            }
        );

        Cache<String, Integer> invokingCache = cacheManager.createCache("invokingCache", custom);

        assertNull(invokingCache.invoke("1", entryProcessor1));
        assertEquals(Integer.valueOf(2), invokingCache.get("1"));
        assertNull(invokingCache.invoke("1", entryProcessor2));
        assertFalse(invokingCache.containsKey("1"));
    }

    @Test // org.jsr107.tck.GetTest.getAll_NullKey
    public void getAllWithNullKey()
    {
        Set<String> keys = Sets.newHashSet("1", null, "2");

        try
        {
            cache.getAll(keys);
            fail("should have thrown an exception - null key in keys not allowed");
        }
        catch (NullPointerException e)
        {
            //expected
        }
    }

    @Test // org.jsr107.tck.RemoveTest.getAndRemove_NullKey
    public void getAndRemoveWithNullKey()
    {
        try
        {
            assertNull(cache.getAndRemove(null));
            fail("should have thrown an exception - null key not allowed");
        }
        catch (NullPointerException e)
        {
            //expected
        }
    }

    @Test // org.jsr107.tck.RemoveTest.removeAll_1arg_NullKey
    public void removeAllWithNullKey()
    {
        Set<String> keys = Sets.newHashSet((String) null);

        try
        {
            cache.removeAll(keys);
            fail("should have thrown an exception - null key not allowed");
        }
        catch (NullPointerException e)
        {
            //expected
        }
    }

    @Test // org.jsr107.tck.RemoveTest.remove_2arg_NullValue
    public void removeWithNullValue()
    {
        try
        {
            cache.remove("1", null);
            fail("should have thrown an exception - null value");
        }
        catch (NullPointerException e)
        {
            //good
        }
    }

    @Test // org.jsr107.tck.PutTest.remove_2arg_NullValue
    public void putAllWithNullKey()
    {
        Map<String, Integer> data = new LinkedHashMap<>();

        data.put("1", 1);
        data.put("2", 2);
        data.put("3", 3);
        data.put(null, Integer.MIN_VALUE);

        try
        {
            cache.putAll(data);
            fail("should have thrown an exception - null key not allowed");
        }
        catch (NullPointerException e)
        {
            //expected
        }
        
        for (Map.Entry<String, Integer> entry : data.entrySet())
        {
            if (entry.getKey() != null)
            {
                assertNull(cache.get(entry.getKey()));
            }
        }
    }
}
