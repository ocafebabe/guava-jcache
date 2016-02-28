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

import javax.cache.Cache;
import javax.cache.event.EventType;

import com.google.common.cache.RemovalNotification;

public class GuavaCacheEntryEvent<K, V>
    extends javax.cache.event.CacheEntryEvent<K, V>
{
    private final RemovalNotification<K, V> notification;

    public GuavaCacheEntryEvent(Cache<K, V> source, EventType eventType, RemovalNotification<K, V> notification)
    {
        super(source, eventType);

        this.notification = notification;
    }

    @Override
    public V getOldValue()
    {
        return null;
    }

    @Override
    public boolean isOldValueAvailable()
    {
        return false;
    }

    @Override
    public K getKey()
    {
        return notification.getKey();
    }

    @Override
    public V getValue()
    {
        return notification.getValue();
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
}
