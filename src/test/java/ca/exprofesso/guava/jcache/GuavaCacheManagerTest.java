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

import java.net.URI;
import java.util.ArrayList;
import java.util.Properties;

import javax.cache.Cache;
import javax.cache.CacheException;
import javax.cache.CacheManager;
import javax.cache.Caching;
import javax.cache.configuration.CompleteConfiguration;
import javax.cache.configuration.MutableConfiguration;
import javax.cache.expiry.AccessedExpiryPolicy;
import javax.cache.expiry.CreatedExpiryPolicy;
import javax.cache.expiry.Duration;
import javax.cache.expiry.ModifiedExpiryPolicy;
import javax.cache.expiry.TouchedExpiryPolicy;
import javax.cache.spi.CachingProvider;

import org.bitstrings.test.junit.runner.ClassLoaderPerTestRunner;
import org.junit.After;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;

@RunWith(ClassLoaderPerTestRunner.class)
public class GuavaCacheManagerTest
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
    public void testCacheManager()
        throws Exception
    {
        CacheManager cacheManager = cachingProvider.getCacheManager();

        assertEquals(cacheManager, cachingProvider.getCacheManager());

        cacheManager.close();

        assertTrue(cacheManager.isClosed());
    }

    @Test
    public void testCacheManagerWithCustomClassLoader()
        throws Exception
    {
        CacheManager cacheManager1 = cachingProvider.getCacheManager();
        CacheManager cacheManager2 = cachingProvider.getCacheManager(null, ClassLoader.getSystemClassLoader());

        assertNotNull(cacheManager1);
        assertNotNull(cacheManager2);

        assertNotEquals(cacheManager1, cacheManager2);

        cacheManager1.close();
        cacheManager2.close();

        assertTrue(cacheManager1.isClosed());
        assertTrue(cacheManager2.isClosed());
    }

    @Test
    public void testCacheManagerWithCustomProperties()
        throws Exception
    {
        Properties properties = new Properties();

        properties.setProperty("concurrencyLevel", "1");
        properties.setProperty("initialCapacity", "16");

        CacheManager cacheManager1 = cachingProvider.getCacheManager();
        CacheManager cacheManager2 = cachingProvider.getCacheManager(null, null, properties);

        assertNotNull(cacheManager1);
        assertNotNull(cacheManager2);

        // properties do not form part of the identity of the CacheManager
        assertEquals(cacheManager1, cacheManager2);

        cacheManager1.close();
        cacheManager2.close();

        assertTrue(cacheManager1.isClosed());
        assertTrue(cacheManager2.isClosed());
    }

    @Test
    public void testCacheManagerWithDefaultProperties()
        throws Exception
    {
        CacheManager cacheManager = cachingProvider.getCacheManager();

        assertNotNull(cacheManager);

        MutableConfiguration<Long, String> configuration =
            new MutableConfiguration<Long, String>().setStoreByValue(false);

        Cache<Long, String> cache = cacheManager.createCache("maximumSizeCacheTest", configuration);

        long maximumSize = Long.valueOf(cacheManager.getProperties().getProperty("maximumSize"));

        assertNotEquals(0, maximumSize);

        for (long l = 1; l <= (maximumSize * 10); l++)
        {
            cache.put(l, "VALUE-" + l);

            if ((l % maximumSize) == 0)
            {
                assertTrue(cache.unwrap(GuavaCache.class).size() <= maximumSize);
            }
        }

        assertTrue(cache.unwrap(GuavaCache.class).size() <= maximumSize);

        cacheManager.close();
    }

    @Test(expected = IllegalArgumentException.class)
    public void testCacheManagerWithCustomInvalidProperties()
        throws Exception
    {
        Properties properties = new Properties();

        properties.setProperty("unknownProperty", "true");

        try (CacheManager cacheManager = cachingProvider.getCacheManager(null, null, properties);)
        {
            assertNotNull(cacheManager);

            MutableConfiguration configuration = new MutableConfiguration();

            configuration.setStoreByValue(false);

            cacheManager.createCache("test", configuration);
        }
    }

    @Test
    public void testCacheManagerWithCustomClassLoaderAndProperties()
        throws Exception
    {
        Properties properties = new Properties();

        properties.setProperty("concurrencyLevel", "1");
        properties.setProperty("initialCapacity", "16");

        CacheManager cacheManager1 = cachingProvider.getCacheManager();
        CacheManager cacheManager2 =
            cachingProvider.getCacheManager(null, ClassLoader.getSystemClassLoader(), properties);

        assertNotNull(cacheManager1);
        assertNotNull(cacheManager2);

        assertNotEquals(cacheManager1, cacheManager2);

        cacheManager1.close();
        cacheManager2.close();

        assertTrue(cacheManager1.isClosed());
        assertTrue(cacheManager2.isClosed());
    }

    @Test(expected = IllegalStateException.class)
    public void testClosedCacheManager()
        throws Exception
    {
        CacheManager cacheManager = cachingProvider.getCacheManager();

        assertNotNull(cacheManager);

        cacheManager.close();

        cacheManager.getCacheNames();
    }

    @Test
    public void testGetCacheWithTypes()
    {
        CacheManager cacheManager = cachingProvider.getCacheManager();

        MutableConfiguration<Number, Number> configuration = new MutableConfiguration<>();

        configuration.setStoreByValue(false);
        configuration.setTypes(Number.class, Number.class);

        cacheManager.createCache("cache", configuration);

        Cache<Integer, Long> cache = cacheManager.getCache("cache", Integer.class, Long.class);

        assertNotNull(cache);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testGetCacheWithInvalidTypes()
    {
        CacheManager cacheManager = cachingProvider.getCacheManager();

        MutableConfiguration<Number, Number> configuration = new MutableConfiguration<>();

        configuration.setStoreByValue(false);
        configuration.setTypes(Number.class, Number.class);

        cacheManager.createCache("cache", configuration);

        Cache<String, String> cache = cacheManager.getCache("cache", String.class, String.class);

        assertNotNull(cache);
    }

    @Test(expected = UnsupportedOperationException.class)
    public void testCreateStoryByValueCache()
    {
        CacheManager cacheManager = cachingProvider.getCacheManager();

        MutableConfiguration<Number, Number> configuration = new MutableConfiguration<>();

        configuration.setStoreByValue(true);

        cacheManager.createCache("cache", configuration);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testCreateReadThroughCacheWithInvalidConfiguration()
    {
        CacheManager cacheManager = cachingProvider.getCacheManager();

        MutableConfiguration<Number, Number> configuration = new MutableConfiguration<>();

        configuration.setStoreByValue(false);
        configuration.setReadThrough(true);

        cacheManager.createCache("cache", configuration);
    }

    @Test(expected = UnsupportedOperationException.class)
    public void testCreateWriteThroughCacheWithInvalidConfiguration()
    {
        CacheManager cacheManager = cachingProvider.getCacheManager();

        MutableConfiguration<Number, Number> configuration = new MutableConfiguration<>();

        configuration.setStoreByValue(false);
        configuration.setWriteThrough(true);

        cacheManager.createCache("cache", configuration);
    }

    @Test(expected = UnsupportedOperationException.class)
    public void testCreateCacheWithUnsupportedAccessedExpiryPolicy()
    {
        CacheManager cacheManager = cachingProvider.getCacheManager();

        MutableConfiguration<Number, Number> configuration = new MutableConfiguration<>();

        configuration.setStoreByValue(false);
        configuration.setExpiryPolicyFactory(AccessedExpiryPolicy.factoryOf(Duration.ZERO));

        cacheManager.createCache("cache", configuration);
    }

    @Test(expected = UnsupportedOperationException.class)
    public void testCreateCacheWithUnsupportedCreatedExpiryPolicy()
    {
        CacheManager cacheManager = cachingProvider.getCacheManager();

        MutableConfiguration<Number, Number> configuration = new MutableConfiguration<>();

        configuration.setStoreByValue(false);
        configuration.setExpiryPolicyFactory(CreatedExpiryPolicy.factoryOf(Duration.ZERO));

        cacheManager.createCache("cache", configuration);
    }

    @Test
    public void testCreateCacheWithModifiedExpiryPolicy()
    {
        CacheManager cacheManager = cachingProvider.getCacheManager();

        MutableConfiguration<Number, Number> configuration = new MutableConfiguration<>();

        configuration.setStoreByValue(false);
        configuration.setExpiryPolicyFactory(ModifiedExpiryPolicy.factoryOf(Duration.ONE_DAY));

        Cache cache = cacheManager.createCache("cache", configuration);

        CompleteConfiguration actualConfiguration =
            (CompleteConfiguration) cache.getConfiguration(CompleteConfiguration.class);

        assertEquals(ModifiedExpiryPolicy.factoryOf(Duration.ONE_DAY), actualConfiguration.getExpiryPolicyFactory());
    }

    @Test
    public void testCreateCacheWithTouchedExpiryPolicy()
    {
        CacheManager cacheManager = cachingProvider.getCacheManager();

        MutableConfiguration<Number, Number> configuration = new MutableConfiguration<>();

        configuration.setStoreByValue(false);
        configuration.setExpiryPolicyFactory(TouchedExpiryPolicy.factoryOf(Duration.ONE_MINUTE));

        Cache cache = cacheManager.createCache("cache", configuration);

        CompleteConfiguration actualConfiguration =
            (CompleteConfiguration) cache.getConfiguration(CompleteConfiguration.class);

        assertEquals(TouchedExpiryPolicy.factoryOf(Duration.ONE_MINUTE), actualConfiguration.getExpiryPolicyFactory());
    }

    @Test // org.jsr107.tck.CacheManagerTest.testReuseCacheManager
    public void testReuseCacheManager()
        throws Exception
    {
        CachingProvider provider = Caching.getCachingProvider();
        URI uri = provider.getDefaultURI();

        CacheManager cacheManager = provider.getCacheManager(uri, provider.getDefaultClassLoader());
        assertFalse(cacheManager.isClosed());
        cacheManager.close();
        assertTrue(cacheManager.isClosed());

        try
        {
            cacheManager.createCache("Dog", null);
            fail();
        }
        catch (IllegalStateException e)
        {
            //expected
        }

        CacheManager otherCacheManager = provider.getCacheManager(uri, provider.getDefaultClassLoader());
        assertFalse(otherCacheManager.isClosed());

        assertNotSame(cacheManager, otherCacheManager);
    }

    @Test // org.jsr107.tck.CacheManagerTest.getCacheManager_nonNullProperties
    public void getCacheManagerWithNonNullProperties()
    {
        // see https://github.com/jsr107/jsr107tck/issues/102

        // make sure existing cache managers are closed and the non empty properties get picked up
        try
        {
            Caching.getCachingProvider().close();
        }
        catch (CacheException ignore)
        {
            // ignore exception which may happen if the provider is not active
        }

        CachingProvider provider = Caching.getCachingProvider();

        Properties properties = new Properties();
        properties.put("dummy.com", "goofy");

        provider.getCacheManager(provider.getDefaultURI(), provider.getDefaultClassLoader(), properties);
        CacheManager manager = provider.getCacheManager();

        assertEquals(properties, manager.getProperties());
    }

    @Test // org.jsr107.tck.CacheManagerTest.getCaches_MutateCacheManager
    public void getCachesAndMutateCacheManager()
    {
        CacheManager cacheManager = cachingProvider.getCacheManager();

        MutableConfiguration configuration = new MutableConfiguration();
        configuration.setStoreByValue(false);

        String removeName = "c2";
        ArrayList<String> cacheNames1 = new ArrayList<>();
        cacheManager.createCache("c1", configuration);
        Cache c1 = cacheManager.getCache("c1");
        cacheNames1.add(c1.getName());
        cacheManager.createCache(removeName, configuration);
        cacheManager.createCache("c3", configuration);
        Cache c3 = cacheManager.getCache("c3");
        cacheNames1.add(c3.getName());

        Iterable<String> cacheNames;
        int size;

        cacheNames = cacheManager.getCacheNames();
        size = 0;

        for (String cacheName : cacheNames)
        {
            size++;
        }

        assertEquals(3, size);

        cacheManager.destroyCache(removeName);
        size = 0;

        for (String cacheName : cacheNames)
        {
            size++;
        }

        assertEquals(3, size);

        cacheNames = cacheManager.getCacheNames();
        size = 0;

        for (String cacheName : cacheNames)
        {
            size++;
        }

        assertEquals(2, size);

        ArrayList<String> collection2 = new ArrayList<>();

        for (String element : cacheNames)
        {
            assertTrue(cacheNames1.contains(element));
            collection2.add(element);
        }

        assertEquals(cacheNames1.size(), collection2.size());

        for (String element : cacheNames1)
        {
            assertTrue(collection2.contains(element));
        }
    }

    @Test(expected = IllegalArgumentException.class) // org.jsr107.tck.CacheManagerTest.unwrapThrowsInvalidArgument
    public void unwrapThrowsInvalidArgument()
    {
        final Class ALWAYS_INVALID_UNWRAP_CLASS = Exception.class;

        cachingProvider.getCacheManager().unwrap(Exception.class);

        fail();
    }
}
