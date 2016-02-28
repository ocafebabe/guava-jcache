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

import java.net.URI;
import java.util.Collections;
import java.util.Properties;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.atomic.AtomicBoolean;

import javax.cache.Cache;
import javax.cache.CacheException;
import javax.cache.configuration.CompleteConfiguration;
import javax.cache.configuration.Configuration;
import javax.cache.expiry.EternalExpiryPolicy;
import javax.cache.expiry.ExpiryPolicy;
import javax.cache.expiry.ModifiedExpiryPolicy;
import javax.cache.expiry.TouchedExpiryPolicy;
import javax.cache.spi.CachingProvider;

public class GuavaCacheManager
    implements javax.cache.CacheManager
{
    private final URI uri;
    private final ClassLoader classLoader;
    private final Properties properties;
    private final CachingProvider cachingProvider;

    private final ConcurrentMap<String, Cache<?, ?>> caches = new ConcurrentHashMap<>();

    private final AtomicBoolean closed = new AtomicBoolean();

    private final Object lock = new Object();

    public GuavaCacheManager(URI uri, ClassLoader classLoader, Properties properties, CachingProvider cachingProvider)
    {
        this.uri = uri;
        this.classLoader = classLoader;
        this.properties = properties;
        this.cachingProvider = cachingProvider;
    }

    @Override
    public CachingProvider getCachingProvider()
    {
        return cachingProvider;
    }

    @Override
    public URI getURI()
    {
        return uri;
    }

    @Override
    public ClassLoader getClassLoader()
    {
        return classLoader;
    }

    @Override
    public Properties getProperties()
    {
        return properties;
    }

    @Override
    public <K, V, C extends Configuration<K, V>> Cache<K, V> createCache(String cacheName, C configuration)
        throws IllegalArgumentException
    {
        checkState();

        if (cacheName == null || configuration == null)
        {
            throw new NullPointerException();
        }
        else if (!(configuration instanceof CompleteConfiguration))
        {
            throw new IllegalArgumentException("Invalid configuration implementation!");
        }

        CompleteConfiguration<K, V> completeConfiguration = (CompleteConfiguration) configuration;

        validateConfiguration(completeConfiguration);

        synchronized (lock)
        {
            if (caches.containsKey(cacheName))
            {
                throw new CacheException("This cache already exists!");
            }

            Cache<K, V> cache = new GuavaCache<>(cacheName, completeConfiguration, this);

            caches.put(cacheName, cache);

            return cache;
        }
    }

    @Override
    public <K, V> Cache<K, V> getCache(String cacheName, Class<K> keyType, Class<V> valueType)
    {
        checkState();

        if (cacheName != null && caches.containsKey(cacheName))
        {
            Cache<?, ?> cache = caches.get(cacheName);

            CompleteConfiguration<?, ?> configuration = cache.getConfiguration(CompleteConfiguration.class);

            if (configuration.getKeyType().isAssignableFrom(keyType)
                && configuration.getValueType().isAssignableFrom(valueType))
            {
                return (Cache<K, V>) cache;
            }

            throw new IllegalArgumentException("Provided key and/or value types are incompatible with this cache!");
        }

        return null;
    }

    @Override
    public <K, V> Cache<K, V> getCache(String cacheName)
    {
        return (Cache<K, V>) getCache(cacheName, Object.class, Object.class);
    }

    @Override
    public Iterable<String> getCacheNames()
    {
        checkState();

        return Collections.unmodifiableSet(caches.keySet());
    }

    @Override
    public void destroyCache(String cacheName)
    {
        checkState();

        if (cacheName == null)
        {
            throw new NullPointerException();
        }
        
        Cache<?, ?> cache = caches.remove(cacheName);

        if (cache != null)
        {
            cache.close();
        }
    }

    @Override
    public void enableManagement(String cacheName, boolean enabled)
    {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public void enableStatistics(String cacheName, boolean enabled)
    {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public void close()
    {
        if (closed.compareAndSet(false, true))
        {
            for (Cache<?, ?> c : caches.values())
            {
                try
                {
                    c.close();
                }
                catch (Exception e)
                {
                    // no-op
                }
            }

            caches.clear();

            ((GuavaCachingProvider) cachingProvider).close(this);
        }
    }

    @Override
    public boolean isClosed()
    {
        return closed.get();
    }

    @Override
    public <T> T unwrap(Class<T> clazz)
    {
        return clazz.cast(this);
    }

    private void checkState()
    {
        if (isClosed())
        {
            throw new IllegalStateException("This cache manager is closed!");
        }
    }

    private void validateConfiguration(CompleteConfiguration<?, ?> configuration)
    {
        if (configuration.isStoreByValue())
        {
            throw new UnsupportedOperationException("Store by value is not supported in Guava!");
        }

        if (configuration.getExpiryPolicyFactory() == null)
        {
            throw new NullPointerException("Expiry policy factory cannot be null!");
        }

        ExpiryPolicy expiryPolicy = configuration.getExpiryPolicyFactory().create();

        if (!(expiryPolicy instanceof EternalExpiryPolicy)
            && !(expiryPolicy instanceof ModifiedExpiryPolicy)
            && !(expiryPolicy instanceof TouchedExpiryPolicy))
        {
            throw new UnsupportedOperationException("Invalid expiry policy configuration!");
        }

        if (configuration.isReadThrough() && configuration.getCacheLoaderFactory() == null)
        {
            throw new IllegalArgumentException("Invalid read through cache configuration!");
        }

        if (configuration.isWriteThrough() || configuration.getCacheWriterFactory() != null)
        {
            throw new UnsupportedOperationException("Invalid write through cache configuration!");
        }
    }
}
