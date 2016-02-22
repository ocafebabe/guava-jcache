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

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentMap;

import javax.cache.CacheManager;
import javax.cache.configuration.CacheEntryListenerConfiguration;
import javax.cache.configuration.CompleteConfiguration;
import javax.cache.configuration.Configuration;
import javax.cache.expiry.Duration;
import javax.cache.expiry.ExpiryPolicy;
import javax.cache.expiry.ModifiedExpiryPolicy;
import javax.cache.expiry.TouchedExpiryPolicy;
import javax.cache.integration.CompletionListener;
import javax.cache.processor.EntryProcessor;
import javax.cache.processor.EntryProcessorException;
import javax.cache.processor.EntryProcessorResult;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheBuilderSpec;

public class GuavaCache<K, V>
    implements javax.cache.Cache<K, V>
{
    private final String cacheName;
    private final CompleteConfiguration<K, V> configuration;
    private final CacheManager cacheManager;

    private final Cache<K, V> cache;
    private final ConcurrentMap<K, V> view;

    private boolean closed = false;

    public GuavaCache(String cacheName, CompleteConfiguration<K, V> configuration, CacheManager cacheManager)
    {
        this.cacheName = cacheName;
        this.configuration = configuration;
        this.cacheManager = cacheManager;

        String properties = cacheManager.getProperties().toString();

        CacheBuilderSpec cacheBuilderSpec = CacheBuilderSpec.parse(properties.substring(1, properties.length() - 1));

        CacheBuilder cacheBuilder = CacheBuilder.from(cacheBuilderSpec);

        ExpiryPolicy expiryPolicy = configuration.getExpiryPolicyFactory().create();

        if (expiryPolicy instanceof ModifiedExpiryPolicy) // == Guava expire after write
        {
            Duration d = expiryPolicy.getExpiryForUpdate();

            cacheBuilder.expireAfterWrite(d.getDurationAmount(), d.getTimeUnit());
        }
        else if (expiryPolicy instanceof TouchedExpiryPolicy) // == Guava expire after access
        {
            Duration d = expiryPolicy.getExpiryForAccess();

            cacheBuilder.expireAfterAccess(d.getDurationAmount(), d.getTimeUnit());
        }

        this.cache = (Cache<K, V>) cacheBuilder.build();

        this.view = cache.asMap();
    }

    @Override
    public V get(K key)
    {
        checkState();

        if (key == null)
        {
            throw new NullPointerException();
        }

        return cache.getIfPresent(key);
    }

    @Override
    public Map<K, V> getAll(Set<? extends K> keys)
    {
        checkState();

        if (keys == null)
        {
            throw new NullPointerException();
        }

        return cache.getAllPresent(keys);
    }

    @Override
    public boolean containsKey(K key)
    {
        checkState();

        if (key == null)
        {
            throw new NullPointerException();
        }

        return view.containsKey(key);
    }

    @Override
    public void loadAll(Set<? extends K> keys, boolean replaceExistingValues, CompletionListener completionListener)
    {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public void put(K key, V value)
    {
        checkState();

        if (key == null || value == null)
        {
            throw new NullPointerException();
        }

        cache.put(key, value);
    }

    @Override
    public V getAndPut(K key, V value)
    {
        checkState();

        if (key == null || value == null)
        {
            throw new NullPointerException();
        }

        return view.put(key, value);
    }

    @Override
    public void putAll(Map<? extends K, ? extends V> map)
    {
        checkState();

        if (map == null)
        {
            throw new NullPointerException();
        }

        view.putAll(map);
    }

    @Override
    public boolean putIfAbsent(K key, V value)
    {
        checkState();

        if (key == null || value == null)
        {
            throw new NullPointerException();
        }

        return (view.putIfAbsent(key, value) == null);
    }

    @Override
    public boolean remove(K key)
    {
        checkState();

        if (key == null)
        {
            throw new NullPointerException();
        }

        return (view.remove(key) != null);
    }

    @Override
    public boolean remove(K key, V oldValue)
    {
        checkState();

        if (key == null)
        {
            throw new NullPointerException();
        }

        return view.remove(key, oldValue);
    }

    @Override
    public V getAndRemove(K key)
    {
        checkState();

        return view.remove(key);
    }

    @Override
    public boolean replace(K key, V oldValue, V newValue)
    {
        checkState();

        if (key == null || oldValue == null || newValue == null)
        {
            throw new NullPointerException();
        }

        return view.replace(key, oldValue, newValue);
    }

    @Override
    public boolean replace(K key, V value)
    {
        checkState();

        if (key == null || value == null)
        {
            throw new NullPointerException();
        }

        return (view.replace(key, value) != null);
    }

    @Override
    public V getAndReplace(K key, V value)
    {
        checkState();

        if (key == null || value == null)
        {
            throw new NullPointerException();
        }

        return view.replace(key, value);
    }

    @Override
    public void removeAll(Set<? extends K> keys)
    {
        checkState();

        cache.invalidateAll(keys);
    }

    @Override
    public void removeAll()
    {
        checkState();

        cache.invalidateAll();
    }

    @Override
    public void clear()
    {
        checkState();

        view.clear();
    }

    @Override
    public <C extends Configuration<K, V>> C getConfiguration(Class<C> clazz)
    {
        return (C) configuration;
    }

    @Override
    public <T> T invoke(K key, EntryProcessor<K, V, T> entryProcessor, Object... arguments)
        throws EntryProcessorException
    {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public <T> Map<K, EntryProcessorResult<T>>
        invokeAll(Set<? extends K> keys, EntryProcessor<K, V, T> entryProcessor, Object... arguments)
    {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public String getName()
    {
        return cacheName;
    }

    @Override
    public CacheManager getCacheManager()
    {
        return cacheManager;
    }

    @Override
    public void close()
    {
        checkState();

        cache.invalidateAll();
        cache.cleanUp();

        closed = true;
    }

    @Override
    public boolean isClosed()
    {
        return closed;
    }

    @Override
    public <T> T unwrap(Class<T> clazz)
    {
        return (T) this;
    }

    @Override
    public void registerCacheEntryListener(CacheEntryListenerConfiguration<K, V> cacheEntryListenerConfiguration)
    {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public void deregisterCacheEntryListener(CacheEntryListenerConfiguration<K, V> cacheEntryListenerConfiguration)
    {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public Iterator<Entry<K, V>> iterator()
    {
        checkState();

        List<javax.cache.Cache.Entry<K, V>> list = new ArrayList<>();

        for (final Map.Entry<K, V> entry : view.entrySet())
        {
            list.add(new javax.cache.Cache.Entry<K, V>()
            {
                @Override
                public K getKey()
                {
                    return entry.getKey();
                }

                @Override
                public V getValue()
                {
                    return entry.getValue();
                }

                @Override
                public <T> T unwrap(Class<T> clazz)
                {
                    return (T) entry;
                }
            });
        }

        return list.iterator();
    }

    public long size()
    {
        return cache.size();
    }

    private void checkState()
    {
        if (isClosed())
        {
            throw new IllegalStateException("This cache is closed!");
        }
    }
}
