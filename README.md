guava-jcache
============

This WIP shall be a full implementation of the API and SPI from JSR-107 (aka JCache). It provides a wrapper around a Google Guava cache that allows allows you to use Guava as the caching provider using only JSR-107 APIs.

[![Build Status](https://travis-ci.org/ocafebabe/guava-jcache.svg)](https://travis-ci.org/ocafebabe/guava-jcache)
[![Maven Central](https://maven-badges.herokuapp.com/maven-central/ca.exprofesso/guava-jcache/badge.svg)](https://maven-badges.herokuapp.com/maven-central/ca.exprofesso/guava-jcache/)
[![License](http://img.shields.io/:license-apache-brightgreen.svg)](http://www.apache.org/licenses/LICENSE-2.0.html)

## Usage

Development snapshots are available on Sonatype Nexus repository

```xml
<repositories>
    <repository>
        <id>sonatype-nexus-snapshots</id>
        <name>Sonatype Nexus Snapshots</name>
        <url>https://oss.sonatype.org/content/repositories/snapshots</url>
    </repository>
</repositories>

<dependencies>
    <dependency>
        <groupId>ca.exprofesso</groupId>
        <artifactId>guava-jcache</artifactId>
        <version>1.0.4-SNAPSHOT</version>
    </dependency>
</dependencies>
```

## Example - Simple Cache

```java
MutableConfiguration<String, Integer> configuration = new MutableConfiguration<>();
configuration.setStoreByValue(false);
configuration.setTypes(String.class, Integer.class);

CachingProvider cachingProvider = Caching.getCachingProvider(GuavaCachingProvider.class.getName());
CacheManager cacheManager = cachingProvider.getCacheManager();
Cache<String, Integer> cache = cacheManager.createCache("cache", configuration);

cache.put("key", 1);
Integer value = cache.get("key");
```

## Example - Loading Cache

```java
final CacheLoader<String, Integer> cacheLoader = new CacheLoader<String, Integer>()
{
    @Override
    public Integer load(String key)
        throws CacheLoaderException
    {
        // in a real application the value would probably come from a database...
        return Integer.valueOf(key);
    }

    @Override
    public Map<String, Integer> loadAll(Iterable<? extends String> keys)
        throws CacheLoaderException
    {
        Map<String, Integer> map = new HashMap<>();
        for (String key : keys)
        {
            // in a real application the value would probably come from a database...
            map.put(key, Integer.valueOf(key));
        }
        return map;
    }
};

MutableConfiguration<String, Integer> configuration = new MutableConfiguration<>();
configuration.setStoreByValue(false);
configuration.setTypes(String.class, Integer.class);
custom.setReadThrough(true);
custom.setCacheLoaderFactory
(
    new Factory<CacheLoader<String, Integer>>()
    {
        @Override
        public CacheLoader<String, Integer> create()
        {
            return cacheLoader;
        }
    }
);

CachingProvider cachingProvider = Caching.getCachingProvider(GuavaCachingProvider.class.getName());
CacheManager cacheManager = cachingProvider.getCacheManager();
Cache<String, Integer> cache = cacheManager.createCache("cache", configuration);

Integer value = cache.get("key");
```

## Documentation

[javax.cache (JSR107 API and SPI 1.0.0 API)](http://ignite.apache.org/jcache/1.0.0/javadoc/javax/cache/package-summary.html)

[com.google.common.cache (Guava: Google Core Libraries for Java 22.0 API)](http://google.github.io/guava/releases/22.0/api/docs/com/google/common/cache/package-summary.html)
