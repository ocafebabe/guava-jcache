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

import javax.cache.Cache;
import javax.cache.CacheManager;
import javax.cache.Caching;
import javax.cache.configuration.MutableConfiguration;
import javax.cache.spi.CachingProvider;
import javax.management.MBeanServer;
import javax.management.ObjectName;

import org.junit.Test;

public class GuavaCacheMXBeanTest
{
    @Test
    public void testCacheManagementBean()
        throws Exception
    {
        try (CachingProvider cachingProvider = Caching.getCachingProvider(GuavaCachingProvider.class.getName()))
        {
            CacheManager cacheManager = cachingProvider.getCacheManager();

            MutableConfiguration<String, Integer> configuration = new MutableConfiguration<>();

            configuration.setStoreByValue(false);
            configuration.setTypes(String.class, Integer.class);
            configuration.setManagementEnabled(true);

            Cache<String, Integer> managementCache = cacheManager.createCache("managementCache", configuration);

            MBeanServer beanServer = ManagementFactory.getPlatformMBeanServer();

            assertNotNull(beanServer);

            ObjectName name = new ObjectName(GuavaCacheMXBean.getObjectName(managementCache));

            Object keyType = beanServer.getAttribute(name, "KeyType");
            Object valueType = beanServer.getAttribute(name, "ValueType");
            Object readThrough = beanServer.getAttribute(name, "ReadThrough");
            Object writeThrough = beanServer.getAttribute(name, "WriteThrough");
            Object storeByValue = beanServer.getAttribute(name, "StoreByValue");
            Object statisticsEnabled = beanServer.getAttribute(name, "StatisticsEnabled");
            Object managementEnabled = beanServer.getAttribute(name, "ManagementEnabled");

            assertNotNull(keyType);
            assertNotNull(valueType);
            assertNotNull(readThrough);
            assertNotNull(writeThrough);
            assertNotNull(storeByValue);
            assertNotNull(statisticsEnabled);
            assertNotNull(managementEnabled);

            assertEquals("java.lang.String", keyType);
            assertEquals("java.lang.Integer", valueType);
            assertFalse((boolean) readThrough);
            assertFalse((boolean) writeThrough);
            assertFalse((boolean) storeByValue);
            assertFalse((boolean) statisticsEnabled);
            assertTrue((boolean) managementEnabled);
        }
    }
}
