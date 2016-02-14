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

import javax.cache.Caching;
import javax.cache.spi.CachingProvider;

import org.bitstrings.test.junit.runner.ClassLoaderPerTestRunner;
import org.junit.Test;
import org.junit.runner.RunWith;

@RunWith(ClassLoaderPerTestRunner.class)
public class GuavaCachingProviderTest
{
    @Test
    public void testGetCachingProvider()
    {
        try (CachingProvider cp1 = Caching.getCachingProvider(GuavaCachingProvider.class.getName());
             CachingProvider cp2 = Caching.getCachingProvider(GuavaCachingProvider.class.getName());)
        {
            assertNotNull(cp1);
            assertNotNull(cp2);

            assertEquals(cp1, cp2);
        }
    }

    @Test
    public void testGetCachingProviders()
    {
        int instances = 0;

        for (CachingProvider cp : Caching.getCachingProviders())
        {
            if (cp.getClass().getName().equals(GuavaCachingProvider.class.getName()))
            {
                instances++;
            }

            cp.close();
        }

        assertEquals(1, instances);
    }

    @Test
    public void testGetCachingProviderWithClassLoader()
    {
        try (CachingProvider cp1 =
                Caching.getCachingProvider(GuavaCachingProvider.class.getName());
             CachingProvider cp2 =
                Caching.getCachingProvider(GuavaCachingProvider.class.getName(), getClass().getClassLoader());)
        {
            assertNotNull(cp1);
            assertNotNull(cp2);

            assertEquals(cp1, cp2);
        }
    }

    @Test
    public void testGetCachingProviderWithSystemClassLoader()
    {
        try (CachingProvider cp1 =
                Caching.getCachingProvider(GuavaCachingProvider.class.getName());
             CachingProvider cp2 =
                Caching.getCachingProvider(GuavaCachingProvider.class.getName(), ClassLoader.getSystemClassLoader());)
        {
            assertNotNull(cp1);
            assertNotNull(cp2);

            assertNotEquals(cp1, cp2);
        }
    }
}
