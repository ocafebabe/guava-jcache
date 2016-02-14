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

    @Before
    public void init()
    {
        cachingProvider = Caching.getCachingProvider(GuavaCachingProvider.class.getName());

        assertNotNull(cachingProvider);
    }

    @After
    public void close()
    {
        cachingProvider.close();

        cachingProvider = null;
    }

    @Test
    public void testPutGet()
    {
        CacheManager cm = cachingProvider.getCacheManager();

        MutableConfiguration<String, Integer> mc = new MutableConfiguration<>();

        mc.setStoreByValue(false);
        mc.setTypes(String.class, Integer.class);

        Cache<String, Integer> c = cm.createCache("cache", mc);

        assertEquals("cache", c.getName());

        c.put("1", 1);

        assertEquals(Integer.valueOf(1), c.get("1"));
        assertEquals(1, c.unwrap(GuavaCache.class).size());
    }

    @Test
    public void testPutIfAbsent()
    {
        CacheManager cm = cachingProvider.getCacheManager();

        MutableConfiguration<String, Integer> mc = new MutableConfiguration<>();

        mc.setStoreByValue(false);
        mc.setTypes(String.class, Integer.class);

        Cache<String, Integer> c = cm.createCache("cache", mc);

        assertEquals("cache", c.getName());

        assertTrue(c.putIfAbsent("key", Integer.MIN_VALUE));
        assertFalse(c.putIfAbsent("key", Integer.MIN_VALUE));
    }

    @Test
    public void testClear()
    {
        CacheManager cm = cachingProvider.getCacheManager();

        MutableConfiguration<String, Integer> mc = new MutableConfiguration<>();

        mc.setStoreByValue(false);
        mc.setTypes(String.class, Integer.class);

        Cache<String, Integer> c = cm.createCache("cache", mc);

        c.put("1", 1);

        c.clear();

        assertNull(c.get("1"));
        assertEquals(0, c.unwrap(GuavaCache.class).size());
    }

    @Test
    public void testRemoveAll()
    {
        CacheManager cm = cachingProvider.getCacheManager();

        MutableConfiguration<String, Integer> mc = new MutableConfiguration<>();

        mc.setStoreByValue(false);
        mc.setTypes(String.class, Integer.class);

        Cache<String, Integer> c = cm.createCache("cache", mc);

        c.put("1", 1);

        c.removeAll();

        assertNull(c.get("1"));
        assertEquals(0, c.unwrap(GuavaCache.class).size());
    }

    @Test(expected = IllegalStateException.class)
    public void testClosedCache()
    {
        CacheManager cm = cachingProvider.getCacheManager();

        MutableConfiguration<String, Integer> mc = new MutableConfiguration<>();

        mc.setStoreByValue(false);
        mc.setTypes(String.class, Integer.class);

        Cache<String, Integer> c = cm.createCache("cache", mc);

        c.close();

        c.get("test");
    }
}
