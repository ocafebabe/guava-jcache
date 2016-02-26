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

import static java.util.concurrent.TimeUnit.*;

import static org.junit.Assert.*;

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

import org.junit.Test;

public class GuavaCacheEventTest
{
    @Test(timeout = 5000L)
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

        try (CachingProvider cachingProvider = Caching.getCachingProvider(GuavaCachingProvider.class.getName()))
        {
            CacheManager cacheManager = cachingProvider.getCacheManager();

            MutableConfiguration<String, Integer> configuration = new MutableConfiguration<>();

            configuration.setStoreByValue(false);
            configuration.setTypes(String.class, Integer.class);
            configuration.addCacheEntryListenerConfiguration(listener);

            Cache<String, Integer> cache1 = cacheManager.createCache("cache", configuration);

            cache1.put("entry", 100);
            cache1.put("entry", 101);
            cache1.remove("entry");

            assertEquals(1, myCacheEntryListener.getUpdated());
            assertEquals(1, myCacheEntryListener.getRemoved());

            MutableConfiguration<String, Integer> configuration2 = new MutableConfiguration<>();

            configuration2.setStoreByValue(false);
            configuration2.setTypes(String.class, Integer.class);
            configuration2.addCacheEntryListenerConfiguration(listener);
            configuration2.setExpiryPolicyFactory(ModifiedExpiryPolicy.factoryOf(new Duration(MILLISECONDS, 1)));

            Cache<String, Integer> cache2 = cacheManager.createCache("cache2", configuration2);

            cache2.put("entry1", 101);
            cache2.put("entry2", 102);
            cache2.put("entry3", 103);

            while (myCacheEntryListener.getExpired() != 3)
            {
                Thread.sleep(250);

                cache2.unwrap(GuavaCache.class).cleanUp();
            }
        }
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
