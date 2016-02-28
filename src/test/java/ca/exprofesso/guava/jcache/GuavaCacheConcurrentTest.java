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
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Properties;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicInteger;

import javax.cache.Cache;
import javax.cache.CacheManager;
import javax.cache.Caching;
import javax.cache.configuration.Factory;
import javax.cache.configuration.MutableConfiguration;
import javax.cache.expiry.EternalExpiryPolicy;
import javax.cache.integration.CacheLoader;
import javax.cache.integration.CacheLoaderException;
import javax.cache.spi.CachingProvider;
import javax.management.MBeanServer;
import javax.management.ObjectName;

import org.bitstrings.test.junit.runner.ClassLoaderPerTestRunner;
import org.junit.Test;
import org.junit.runner.RunWith;

@RunWith(ClassLoaderPerTestRunner.class)
public class GuavaCacheConcurrentTest
{
    private static final int THREADS = 8;
    private static final int TEST_CACHE_SIZE = 100_000;
    private static final int MAXIMUM_CACHE_SIZE = 50_000;

    @Test(timeout = 60000L)
    public void main()
        throws Exception
    {
        final AtomicInteger loads = new AtomicInteger();

        Properties properties = new Properties()
        {
            {
                setProperty("maximumSize", String.valueOf(MAXIMUM_CACHE_SIZE));
            }
        };

        MutableConfiguration<KeyObject, ValueObject> configuration = new MutableConfiguration<>();

        configuration.setStoreByValue(false);
        configuration.setTypes(KeyObject.class, ValueObject.class);
        configuration.setExpiryPolicyFactory(EternalExpiryPolicy.factoryOf());
        configuration.setStatisticsEnabled(true);
        configuration.setManagementEnabled(true);
        configuration.setReadThrough(true);
        configuration.setCacheLoaderFactory
        (
            new Factory<CacheLoader<KeyObject, ValueObject>>()
            {
                @Override
                public CacheLoader<KeyObject, ValueObject> create()
                {
                    return new CacheLoader<KeyObject, ValueObject>()
                    {
                        @Override
                        public ValueObject load(KeyObject key)
                            throws CacheLoaderException
                        {
                            loads.incrementAndGet();

                            return new ValueObject(key.getId(), "CODE_" + key.getId(), new Date());
                        }

                        @Override
                        public Map<KeyObject, ValueObject> loadAll(Iterable<? extends KeyObject> keys)
                            throws CacheLoaderException
                        {
                            throw new UnsupportedOperationException("Not supported yet.");
                        }
                    };
                }
            }
        );

        CachingProvider cachingProvider = Caching.getCachingProvider(GuavaCachingProvider.class.getName());

        CacheManager cacheManager = cachingProvider.getCacheManager(null, null, properties);

        Cache<KeyObject, ValueObject> workerCache = cacheManager.createCache("workerCache", configuration);

        List<Worker> workers = new ArrayList<>();

        for (int i = 0; i < THREADS; i++)
        {
            workers.add(new Worker());
        }

        Set<ValueObject> uniques = new HashSet<>();

        ExecutorService executorService = Executors.newFixedThreadPool(THREADS);

        for (Future<Collection<ValueObject>> f : executorService.invokeAll(workers))
        {
            Collection<ValueObject> values = f.get();

            assertEquals(TEST_CACHE_SIZE, values.size());

            uniques.addAll(values);
        }

        assertEquals(TEST_CACHE_SIZE, loads.get());
        assertEquals(TEST_CACHE_SIZE, uniques.size());

        executorService.shutdown();

        MBeanServer beanServer = ManagementFactory.getPlatformMBeanServer();

        ObjectName name = new ObjectName(GuavaCacheStatisticsMXBean.getObjectName(workerCache));

        long cacheGets = (long) beanServer.getAttribute(name, "CacheGets");
        long cacheHits = (long) beanServer.getAttribute(name, "CacheHits");
        long cacheMisses = (long) beanServer.getAttribute(name, "CacheMisses");
        long cacheEvictions = (long) beanServer.getAttribute(name, "CacheEvictions");
        float averageGetTime = (float) beanServer.getAttribute(name, "AverageGetTime");

        assertEquals(TEST_CACHE_SIZE, cacheGets);
        assertEquals((THREADS * TEST_CACHE_SIZE), (cacheHits + cacheMisses));
        assertEquals((TEST_CACHE_SIZE - MAXIMUM_CACHE_SIZE), cacheEvictions);
        assertNotEquals(0, averageGetTime);
    }

    private static class Worker
        implements Callable<Collection<ValueObject>>
    {
        @Override
        public Collection<ValueObject> call()
            throws Exception
        {
            CachingProvider cachingProvider = Caching.getCachingProvider(GuavaCachingProvider.class.getName());

            CacheManager cacheManager = cachingProvider.getCacheManager();

            Cache<KeyObject, ValueObject> cache =
                cacheManager.getCache("workerCache", KeyObject.class, ValueObject.class);

            Set<ValueObject> values = new HashSet<>();

            for (long id = 0; id < TEST_CACHE_SIZE; id++)
            {
                values.add(cache.get(new KeyObject(id)));
            }

            return values;
        }
    }

    private static class KeyObject
    {
        private Long id;

        public KeyObject(Long id)
        {
            this.id = id;
        }

        public Long getId()
        {
            return id;
        }

        public void setId(Long id)
        {
            this.id = id;
        }

        @Override
        public int hashCode()
        {
            int hash = 7;
            hash = 61 * hash + Objects.hashCode(this.id);
            return hash;
        }

        @Override
        public boolean equals(Object obj)
        {
            if (this == obj)
            {
                return true;
            }

            if (obj == null)
            {
                return false;
            }

            if (getClass() != obj.getClass())
            {
                return false;
            }

            final KeyObject other = (KeyObject) obj;

            if (!Objects.equals(this.id, other.id))
            {
                return false;
            }

            return true;
        }
    }

    private static class ValueObject
    {
        private Long id;
        private String code;
        private Date timestamp;

        public ValueObject(Long id, String code, Date timestamp)
        {
            this.id = id;
            this.code = code;
            this.timestamp = timestamp;
        }

        public Long getId()
        {
            return id;
        }

        public void setId(Long id)
        {
            this.id = id;
        }

        public String getCode()
        {
            return code;
        }

        public void setCode(String code)
        {
            this.code = code;
        }

        public Date getTimestamp()
        {
            return timestamp;
        }

        public void setTimestamp(Date timestamp)
        {
            this.timestamp = timestamp;
        }

        @Override
        public int hashCode()
        {
            int hash = 5;

            hash = 61 * hash + Objects.hashCode(this.id);

            return hash;
        }

        @Override
        public boolean equals(Object obj)
        {
            if (this == obj)
            {
                return true;
            }

            if (obj == null)
            {
                return false;
            }

            if (getClass() != obj.getClass())
            {
                return false;
            }

            final ValueObject other = (ValueObject) obj;

            if (!Objects.equals(this.id, other.id))
            {
                return false;
            }

            return true;
        }
    }
}
