/*
 * Copyright 2019 ExProfesso.
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

import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import javax.cache.Cache;
import javax.cache.CacheManager;
import javax.cache.Caching;
import javax.cache.configuration.MutableConfiguration;
import javax.cache.processor.EntryProcessor;
import javax.cache.processor.EntryProcessorException;
import javax.cache.processor.MutableEntry;
import javax.cache.spi.CachingProvider;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

/**
 *
 * @author zfarkas
 */
public class GuavaCacheAtomicEntryProcessorTest
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
    public void testAtomic()
        throws InterruptedException, ExecutionException
    {
        EntryProcessor<String, Integer, Boolean> entryProcessor = new EntryProcessor<String, Integer, Boolean>()
        {
            @Override
            public Boolean process(MutableEntry<String, Integer> entry, Object... arguments)
                throws EntryProcessorException
            {
                if (!entry.exists())
                {
                    entry.setValue(Integer.parseInt(entry.getKey()));

                    return Boolean.TRUE;
                }
                else
                {
                    entry.setValue(entry.getValue() + 1);
                }

                return Boolean.FALSE;
            }
        };

        MutableConfiguration<String, Integer> custom = new MutableConfiguration<>(configuration);
        Cache<String, Integer> atoTestCache = cacheManager.createCache("atoTestCache", custom);
        ExecutorService tp = Executors.newFixedThreadPool(8);
        Future<Boolean>[] futures = new Future[100];

        for (int i = 0; i < 100; i++)
        {
            futures[i] = tp.submit(() -> atoTestCache.invoke("1", entryProcessor));
        }

        for (Future<Boolean> future : futures)
        {
            future.get();
        }

        assertEquals(100, atoTestCache.get("1").intValue());

        tp.shutdown();
    }
}
