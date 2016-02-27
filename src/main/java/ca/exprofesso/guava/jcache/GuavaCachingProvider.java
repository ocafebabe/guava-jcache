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

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.Iterator;
import java.util.Map;
import java.util.Objects;
import java.util.Properties;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import javax.cache.CacheException;
import javax.cache.CacheManager;
import javax.cache.configuration.OptionalFeature;

public final class GuavaCachingProvider
    implements javax.cache.spi.CachingProvider
{
    private static final URI defaultUri;
    private static final Properties defaultProperties;

    static
    {
        URI uri = null;
        Properties properties = new Properties();

        try
        {
            URL url = GuavaCachingProvider.class.getResource("/cachebuilderspec.properties");

            if (url != null)
            {
                uri = url.toURI();

                try (InputStream is = url.openStream())
                {
                    properties.load(is);
                }
                catch (IOException e)
                {
                    throw new CacheException(e);
                }
            }
        }
        catch (URISyntaxException e)
        {
            throw new CacheException(e);
        }

        defaultUri = uri;
        defaultProperties = properties;
    }

    private final
        ConcurrentMap<Triple<URI, ClassLoader, Properties>, CacheManager> cacheManagers = new ConcurrentHashMap<>();

    public GuavaCachingProvider()
    {
    }

    @Override
    public CacheManager getCacheManager(URI uri, ClassLoader classLoader, Properties properties)
    {
        URI _uri = (uri != null) ? uri : getDefaultURI();
        ClassLoader _classLoader = (classLoader != null) ? classLoader : getDefaultClassLoader();
        Properties _properties = (properties != null) ? properties : getDefaultProperties();

        CacheManager newCacheManager = new GuavaCacheManager(_uri, _classLoader, _properties, this);

        CacheManager oldCacheManager =
            cacheManagers.putIfAbsent(new Triple<>(_uri, _classLoader, _properties), newCacheManager);

        if (oldCacheManager != null)
        {
            return oldCacheManager;
        }

        return newCacheManager;
    }

    @Override
    public ClassLoader getDefaultClassLoader()
    {
        return getClass().getClassLoader();
    }

    @Override
    public URI getDefaultURI()
    {
        return defaultUri;
    }

    @Override
    public Properties getDefaultProperties()
    {
        return defaultProperties;
    }

    @Override
    public CacheManager getCacheManager(URI uri, ClassLoader classLoader)
    {
        return getCacheManager(uri, classLoader, getDefaultProperties());
    }

    @Override
    public CacheManager getCacheManager()
    {
        return getCacheManager(getDefaultURI(), getDefaultClassLoader());
    }

    @Override
    public void close()
    {
        Iterator<Map.Entry<Triple<URI, ClassLoader, Properties>, CacheManager>> i = cacheManagers.entrySet().iterator();

        while (i.hasNext())
        {
            Map.Entry<Triple<URI, ClassLoader, Properties>, CacheManager> entry = i.next();

            CacheManager cm = entry.getValue();

            if (cm != null && !cm.isClosed())
            {
                cm.close();
            }

            i.remove();
        }
    }

    @Override
    public void close(ClassLoader classLoader)
    {
        close(getDefaultURI(), classLoader);
    }

    @Override
    public void close(URI uri, ClassLoader classLoader)
    {
        CacheManager cm = cacheManagers.remove(new Triple<>(uri, classLoader, getDefaultProperties()));

        if (cm != null && !cm.isClosed())
        {
            cm.close();
        }
    }

    @Override
    public boolean isSupported(OptionalFeature optionalFeature)
    {
        return optionalFeature.equals(OptionalFeature.STORE_BY_REFERENCE);
    }

    private static class Triple<L, M, R>
    {
        private final L left;
        private final M middle;
        private final R right;

        public Triple(L left, M middle, R right)
        {
            this.left = left;
            this.middle = middle;
            this.right = right;
        }

        public L getLeft()
        {
            return left;
        }

        public M getMiddle()
        {
            return middle;
        }

        public R getRight()
        {
            return right;
        }

        @Override
        public int hashCode()
        {
            int hash = 7;

            hash = 71 * hash + Objects.hashCode(this.left);
            hash = 71 * hash + Objects.hashCode(this.middle);
            hash = 71 * hash + Objects.hashCode(this.right);

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

            final Triple<?, ?, ?> other = (Triple<?, ?, ?>) obj;

            if (!Objects.equals(this.left, other.left))
            {
                return false;
            }

            if (!Objects.equals(this.middle, other.middle))
            {
                return false;
            }

            if (!Objects.equals(this.right, other.right))
            {
                return false;
            }

            return true;
        }
    }
}