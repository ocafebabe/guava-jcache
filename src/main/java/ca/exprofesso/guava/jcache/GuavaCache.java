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

import java.lang.management.ManagementFactory;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;

import javax.cache.CacheException;
import javax.cache.CacheManager;
import javax.cache.configuration.CacheEntryListenerConfiguration;
import javax.cache.configuration.CompleteConfiguration;
import javax.cache.configuration.Configuration;
import javax.cache.configuration.Factory;
import javax.cache.event.CacheEntryCreatedListener;
import javax.cache.event.CacheEntryEvent;
import javax.cache.event.CacheEntryExpiredListener;
import javax.cache.event.CacheEntryListener;
import javax.cache.event.CacheEntryListenerException;
import javax.cache.event.CacheEntryRemovedListener;
import javax.cache.event.CacheEntryUpdatedListener;
import javax.cache.event.EventType;
import javax.cache.expiry.Duration;
import javax.cache.expiry.ExpiryPolicy;
import javax.cache.expiry.ModifiedExpiryPolicy;
import javax.cache.expiry.TouchedExpiryPolicy;
import javax.cache.integration.CacheLoader;
import javax.cache.integration.CompletionListener;
import javax.cache.processor.EntryProcessor;
import javax.cache.processor.EntryProcessorException;
import javax.cache.processor.EntryProcessorResult;
import javax.management.MBeanException;
import javax.management.ObjectName;
import javax.management.OperationsException;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheBuilderSpec;
import com.google.common.cache.CacheStats;
import com.google.common.cache.LoadingCache;
import com.google.common.cache.RemovalListener;
import com.google.common.cache.RemovalNotification;
import com.google.common.collect.Sets;

public class GuavaCache<K, V>
    implements javax.cache.Cache<K, V>, RemovalListener<K, V>
{
    private final String cacheName;
    private final CompleteConfiguration<K, V> configuration;
    private final CacheManager cacheManager;

    private final Cache<K, V> cache;
    private final ConcurrentMap<K, V> view;

    private final Set<CacheEntryListenerConfiguration<K, V>> cacheEntryListenerConfigurations;

    private final AtomicBoolean closed = new AtomicBoolean();

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

        this.cacheEntryListenerConfigurations = Sets.newHashSet(configuration.getCacheEntryListenerConfigurations());

        if (!this.cacheEntryListenerConfigurations.isEmpty())
        {
            cacheBuilder = cacheBuilder.removalListener(this);
        }

        if (configuration.isManagementEnabled())
        {
            GuavaCacheMXBean bean = new GuavaCacheMXBean(this);

            try
            {
                ManagementFactory.getPlatformMBeanServer().registerMBean(bean, new ObjectName(bean.getObjectName()));
            }
            catch (OperationsException | MBeanException e)
            {
                throw new CacheException(e);
            }
        }

        if (configuration.isStatisticsEnabled())
        {
            GuavaCacheStatisticsMXBean bean = new GuavaCacheStatisticsMXBean(this);

            try
            {
                ManagementFactory.getPlatformMBeanServer().registerMBean(bean, new ObjectName(bean.getObjectName()));
            }
            catch (OperationsException | MBeanException e)
            {
                throw new CacheException(e);
            }

            cacheBuilder.recordStats();
        }

        if (configuration.isReadThrough())
        {
            Factory<CacheLoader<K, V>> factory = configuration.getCacheLoaderFactory();

            this.cache = (Cache<K, V>) cacheBuilder.build(new GuavaCacheLoader<>(factory.create()));
        }
        else
        {
            this.cache = (Cache<K, V>) cacheBuilder.build();
        }

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

        if (configuration.isReadThrough())
        {
            try
            {
                return ((LoadingCache<K, V>) cache).get(key);
            }
            catch (ExecutionException e)
            {
                throw new CacheException(e);
            }
        }

        return cache.getIfPresent(key);
    }

    @Override
    public Map<K, V> getAll(Set<? extends K> keys)
    {
        checkState();

        if (keys == null || keys.contains(null))
        {
            throw new NullPointerException();
        }

        if (configuration.isReadThrough())
        {
            try
            {
                return ((LoadingCache<K, V>) cache).getAll(keys);
            }
            catch (ExecutionException e)
            {
                throw new CacheException(e);
            }
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
    public void loadAll(final Set<? extends K> keys, final boolean replaceExistingValues, final CompletionListener cl)
    {
        checkState();

        if (keys == null || cl == null)
        {
            throw new NullPointerException();
        }

        ExecutorService executorService = Executors.newSingleThreadExecutor();

        executorService.submit
        (
            new Runnable()
            {
                @Override
                public void run()
                {
                    try
                    {
                        if (cache instanceof LoadingCache)
                        {
                            LoadingCache<K, V> loadingCache = (LoadingCache<K, V>) cache;

                            for (K key : keys)
                            {
                                if (!view.containsKey(key))
                                {
                                    loadingCache.get(key);
                                }
                                else if (replaceExistingValues)
                                {
                                    loadingCache.refresh(key);
                                }
                            }
                        }
                    }
                    catch (ExecutionException e)
                    {
                        cl.onException(e);
                    }
                    finally
                    {
                        cl.onCompletion();
                    }
                }
            }
        );
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

        if (map == null || map.containsKey(null) || map.containsValue(null))
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

        if (key == null || oldValue == null)
        {
            throw new NullPointerException();
        }

        return view.remove(key, oldValue);
    }

    @Override
    public V getAndRemove(K key)
    {
        checkState();

        if (key == null)
        {
            throw new NullPointerException();
        }

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

        if (keys == null || keys.contains(null))
        {
            throw new NullPointerException();
        }

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
        checkState();

        if (key == null || entryProcessor == null)
        {
            throw new NullPointerException();
        }

        V value = get(key);

        GuavaMutableEntry<K, V> entry = new GuavaMutableEntry<>(key, value, containsKey(key));

        T t = entryProcessor.process(entry, arguments);

        if (entry.isUpdated())
        {
            put(key, entry.getValue());
        }

        if (entry.isRemoved())
        {
            remove(key);
        }

        return t;
    }

    @Override
    public <T> Map<K, EntryProcessorResult<T>>
        invokeAll(Set<? extends K> keys, EntryProcessor<K, V, T> entryProcessor, Object... arguments)
    {
        Map<K, EntryProcessorResult<T>> results = new HashMap<>();

        for (K key : keys)
        {
            results.put(key, new GuavaEntryProcessorResult<>(invoke(key, entryProcessor, arguments)));
        }

        return results;
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
        if (closed.compareAndSet(false, true))
        {
            cache.invalidateAll();
            cache.cleanUp();

            ((GuavaCacheManager) cacheManager).close(this);

            if (configuration.isManagementEnabled())
            {
                String name = GuavaCacheMXBean.getObjectName(this);

                try
                {
                    ManagementFactory.getPlatformMBeanServer().unregisterMBean(new ObjectName(name));
                }
                catch (OperationsException | MBeanException e)
                {
                    throw new CacheException(e);
                }
            }

            if (configuration.isStatisticsEnabled())
            {
                String name = GuavaCacheStatisticsMXBean.getObjectName(this);

                try
                {
                    ManagementFactory.getPlatformMBeanServer().unregisterMBean(new ObjectName(name));
                }
                catch (OperationsException | MBeanException e)
                {
                    throw new CacheException(e);
                }
            }
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
        if (!clazz.isAssignableFrom(getClass()))
        {
            throw new IllegalArgumentException();
        }

        return clazz.cast(this);
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
                    return clazz.cast(entry);
                }
            });
        }

        return Collections.unmodifiableList(list).iterator();
    }

    @Override
    public void onRemoval(RemovalNotification<K, V> notification)
    {
        switch (notification.getCause())
        {
            case EXPIRED:
                notifyListeners(new GuavaCacheEntryEvent<>(this, EventType.EXPIRED, notification));
                break;

            case EXPLICIT:
                notifyListeners(new GuavaCacheEntryEvent<>(this, EventType.REMOVED, notification));
                break;

            case REPLACED:
                notifyListeners(new GuavaCacheEntryEvent<>(this, EventType.UPDATED, notification));
                break;
        }
    }

    public void cleanUp()
    {
        cache.cleanUp();
    }

    public long size()
    {
        return cache.size();
    }

    public CacheStats stats()
    {
        return cache.stats();
    }

    private void checkState()
    {
        if (isClosed())
        {
            throw new IllegalStateException("This cache is closed!");
        }
    }

    private void notifyListeners(CacheEntryEvent<K, V> event)
    {
        for (CacheEntryListenerConfiguration<K, V> listenerConfiguration : cacheEntryListenerConfigurations)
        {
            boolean invokeListener = true;

            if (listenerConfiguration.getCacheEntryEventFilterFactory() != null)
            {
                invokeListener = listenerConfiguration.getCacheEntryEventFilterFactory().create().evaluate(event);
            }

            if (invokeListener)
            {
                CacheEntryListener<?, ?> cel = listenerConfiguration.getCacheEntryListenerFactory().create();

                switch (event.getEventType())
                {
                    case CREATED:
                        if (cel instanceof CacheEntryCreatedListener)
                        {
                            throw new CacheEntryListenerException("Not supported!");
                        }
                        break;

                    case EXPIRED:
                        if (cel instanceof CacheEntryExpiredListener)
                        {
                            ((CacheEntryExpiredListener) cel).onExpired(Sets.newHashSet(event));
                        }
                        break;

                    case REMOVED:
                        if (cel instanceof CacheEntryRemovedListener)
                        {
                            ((CacheEntryRemovedListener) cel).onRemoved(Sets.newHashSet(event));
                        }
                        break;

                    case UPDATED:
                        if (cel instanceof CacheEntryUpdatedListener)
                        {
                            ((CacheEntryUpdatedListener) cel).onUpdated(Sets.newHashSet(event));
                        }
                        break;
                }
            }
        }
    }
}
