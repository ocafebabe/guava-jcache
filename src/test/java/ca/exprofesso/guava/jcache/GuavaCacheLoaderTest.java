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
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

import javax.cache.Cache;
import javax.cache.CacheManager;
import javax.cache.Caching;
import javax.cache.configuration.Factory;
import javax.cache.configuration.MutableConfiguration;
import javax.cache.integration.CacheLoader;
import javax.cache.integration.CacheLoaderException;
import javax.cache.integration.CompletionListener;
import javax.cache.spi.CachingProvider;

import org.junit.Test;

import com.google.common.collect.Sets;

public class GuavaCacheLoaderTest
{
    @Test
    public void testCacheLoader()
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
                Map<String, Integer> map = new HashMap<>();

                for (String key : keys)
                {
                    map.put(key, Integer.valueOf(key));
                }

                return map;
            }
        };

        try (CachingProvider cachingProvider = Caching.getCachingProvider(GuavaCachingProvider.class.getName()))
        {
            CacheManager cacheManager = cachingProvider.getCacheManager();

            MutableConfiguration<String, Integer> custom = new MutableConfiguration<>();

            custom.setStoreByValue(false);
            custom.setTypes(String.class, Integer.class);
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

            Cache<String, Integer> loadingCache = cacheManager.createCache("loadingCache", custom);

            assertEquals(Integer.valueOf(1), loadingCache.get("1"));
            assertEquals(Integer.valueOf(2), loadingCache.get("2"));
            assertEquals(Integer.valueOf(3), loadingCache.get("3"));

            Set<String> keys = Sets.newHashSet("4", "5", "6");

            Map<String, Integer> map = loadingCache.getAll(keys);

            assertEquals(3, map.size());
            assertEquals(Integer.valueOf(4), map.get("4"));
            assertEquals(Integer.valueOf(5), map.get("5"));
            assertEquals(Integer.valueOf(6), map.get("6"));
        }
    }

    @Test(timeout = 5000L)
    public void testCacheLoaderAsyncLoadAll()
        throws InterruptedException
    {
        final AtomicInteger loads = new AtomicInteger();

        final CacheLoader<String, Integer> cacheLoader = new CacheLoader<String, Integer>()
        {
            @Override
            public Integer load(String key)
                throws CacheLoaderException
            {
                loads.incrementAndGet();

                return Integer.valueOf(key);
            }

            @Override
            public Map<String, Integer> loadAll(Iterable<? extends String> keys)
                throws CacheLoaderException
            {
                throw new UnsupportedOperationException("Not supported yet.");
            }
        };

        final AtomicBoolean completed = new AtomicBoolean(false);

        final CompletionListener completionListener = new CompletionListener()
        {
            @Override
            public void onCompletion()
            {
                completed.set(true);
            }

            @Override
            public void onException(Exception e)
            {
                System.err.println(e);
            }
        };

        try (CachingProvider cachingProvider = Caching.getCachingProvider(GuavaCachingProvider.class.getName()))
        {
            CacheManager cacheManager = cachingProvider.getCacheManager();

            MutableConfiguration<String, Integer> custom = new MutableConfiguration<>();

            custom.setStoreByValue(false);
            custom.setTypes(String.class, Integer.class);
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

            Cache<String, Integer> loadingCache = cacheManager.createCache("loadingCache", custom);

            loadingCache.put("1", 1);
            loadingCache.put("2", 2);
            loadingCache.put("3", 3);

            Set<String> keys = Sets.newHashSet("1", "2", "3", "4", "5", "6");

            loadingCache.loadAll(keys, false, completionListener);

            while (!completed.get())
            {
                Thread.sleep(250);
            }

            assertEquals(3, loads.getAndSet(0));

            completed.set(false);

            loadingCache.loadAll(keys, true, completionListener);

            while (!completed.get())
            {
                Thread.sleep(250);
            }

            assertEquals(6, loads.get());
            assertEquals(Integer.valueOf(1), loadingCache.getAndRemove("1"));
            assertEquals(Integer.valueOf(2), loadingCache.getAndRemove("2"));
            assertEquals(Integer.valueOf(3), loadingCache.getAndRemove("3"));
            assertEquals(Integer.valueOf(4), loadingCache.getAndRemove("4"));
            assertEquals(Integer.valueOf(5), loadingCache.getAndRemove("5"));
            assertEquals(Integer.valueOf(6), loadingCache.getAndRemove("6"));
        }
    }
}
