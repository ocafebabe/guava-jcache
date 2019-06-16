
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

public class GuavaMutableEntry<K, V>
    implements javax.cache.processor.MutableEntry<K, V>
{
    private volatile K key;
    private volatile V value;
    private volatile boolean exists;

    private volatile boolean removed;
    private volatile boolean updated;

    public GuavaMutableEntry(K key, V value, boolean exists)
    {
        this.key = key;
        this.value = value;
        this.exists = exists;
    }

    @Override
    public boolean exists()
    {
        return exists;
    }

    @Override
    public void remove()
    {
        exists = false;
        removed = true;
    }

    @Override
    public void setValue(V value)
    {
        this.value = value;

        exists = true;
        updated = true;
        removed = false;
    }

    @Override
    public K getKey()
    {
        return key;
    }

    @Override
    public V getValue()
    {
        return value;
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

    protected boolean isRemoved()
    {
        return removed;
    }

    protected boolean isUpdated()
    {
        return updated;
    }
}
