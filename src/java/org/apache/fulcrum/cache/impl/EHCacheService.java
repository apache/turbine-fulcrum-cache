package org.apache.fulcrum.cache.impl;

/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.apache.avalon.framework.activity.Disposable;
import org.apache.avalon.framework.activity.Initializable;
import org.apache.avalon.framework.configuration.Configurable;
import org.apache.avalon.framework.configuration.Configuration;
import org.apache.avalon.framework.configuration.ConfigurationException;
import org.apache.avalon.framework.logger.AbstractLogEnabled;
import org.apache.avalon.framework.thread.ThreadSafe;
import org.apache.fulcrum.cache.CachedObject;
import org.apache.fulcrum.cache.GlobalCacheService;
import org.apache.fulcrum.cache.ObjectExpiredException;
import org.apache.fulcrum.cache.RefreshableCachedObject;

import net.sf.ehcache.Cache;
import net.sf.ehcache.CacheManager;
import net.sf.ehcache.Element;

/**
 * Default implementation of EHCacheService (Ehcache 2)
 *
 * @author <a href="mailto:epughNOSPAM@opensourceconnections.com">Eric Pugh</a>
 * @author <a href="mailto:tv@apache.org">Thomas Vandahl</a>
 *
 */
public class EHCacheService extends AbstractLogEnabled implements
        GlobalCacheService, Runnable, Configurable, Disposable, Initializable, ThreadSafe
{
    /**
     * Cache check frequency in Millis (1000 Millis = 1 second). Value must be >
     * 0. Default = 5 seconds
     */
    public static final long DEFAULT_CACHE_CHECK_FREQUENCY = 5000; // 5 seconds

    /**
     * cacheCheckFrequency (default - 5 seconds)
     */
    private long cacheCheckFrequency;

    /**
     * Path name of the Ehcache configuration file
     */
    private String configFile;

    /**
     * Constant value which provides a cache name
     */
    private static final String DEFAULT_CACHE_NAME = "fulcrum";

    /**
     * A cache name
     */
    private String cacheName;

    /** thread for refreshing stale items in the cache */
    private Thread refreshing;

    /** flag to stop the housekeeping thread when the component is disposed. */
    private boolean continueThread;

    /** The EHCache manager instance */
    private CacheManager cacheManager;

    /** A cache instance */
    private Cache cache;

    // ---------------- Avalon Lifecycle Methods ---------------------

    /**
     * @see org.apache.avalon.framework.configuration.Configurable#configure(org.apache.avalon.framework.configuration.Configuration)
     */
    @Override
    public void configure(Configuration config) throws ConfigurationException
    {
        this.cacheCheckFrequency = config.getChild("cacheCheckFrequency")
                .getValueAsLong(DEFAULT_CACHE_CHECK_FREQUENCY);
        this.cacheName = config.getChild("cacheName").getValue(DEFAULT_CACHE_NAME);
        this.configFile = config.getChild("configurationFile").getValue(null);
    }

    /**
     * @see org.apache.avalon.framework.activity.Initializable#initialize()
     */
    @Override
    public void initialize() throws Exception
    {
        if (this.configFile == null)
        {
            this.cacheManager = new CacheManager();
            this.cacheManager.addCache(this.cacheName);
        }
        else
        {
            this.cacheManager = new CacheManager(this.configFile);
        }

        this.cache = this.cacheManager.getCache(this.cacheName);

        // Start housekeeping thread.
        this.continueThread = true;
        this.refreshing = new Thread(this);

        // Indicate that this is a system thread. JVM will quit only when
        // there are no more active user threads. Settings threads spawned
        // internally by Turbine as daemons allows commandline applications
        // using Turbine to terminate in an orderly manner.
        this.refreshing.setDaemon(true);
        this.refreshing.setName("EHCacheService Refreshing");
        this.refreshing.start();

        getLogger().debug("EHCacheService started!");
    }

    /**
     * @see org.apache.avalon.framework.activity.Disposable#dispose()
     */
    @Override
    public void dispose()
    {
        this.continueThread = false;
        this.refreshing.interrupt();

        this.cacheManager.shutdown();
        this.cacheManager = null;
        this.cache = null;
        getLogger().debug("EHCacheService stopped!");
    }

    /**
     * @see org.apache.fulcrum.cache.GlobalCacheService#addObject(java.lang.String, org.apache.fulcrum.cache.CachedObject)
     */
    @Override
    public <T> void addObject(String id, CachedObject<T> o)
    {
        Element cacheElement = new Element(id, o);

        if (o instanceof RefreshableCachedObject)
        {
            cacheElement.setEternal(true);
        }
        else
        {
            cacheElement.setEternal(false);
            cacheElement.setTimeToLive((int)(o.getExpires() + 500) / 1000);
        }

        this.cache.put(cacheElement);
    }

    /**
     * @see org.apache.fulcrum.cache.GlobalCacheService#flushCache()
     */
    @Override
    public void flushCache()
    {
        this.cache.removeAll();
    }

    /**
     * @see org.apache.fulcrum.cache.GlobalCacheService#getCachedObjects()
     */
    @Override
    public List<CachedObject<?>> getCachedObjects()
    {
        ArrayList<CachedObject<?>> values = new ArrayList<CachedObject<?>>();

        for (String key : getKeys())
        {
            Element cachedElement = this.cache.get(key);

            if (cachedElement != null)
            {
                values.add((CachedObject<?>)cachedElement.getObjectValue());
            }
        }

        return values;
    }

    /**
     * @see org.apache.fulcrum.cache.GlobalCacheService#getCacheSize()
     */
    @Override
    public int getCacheSize() throws IOException
    {
        return (int)this.cache.calculateInMemorySize();
    }

    /**
     * @see org.apache.fulcrum.cache.GlobalCacheService#getKeys()
     */
    @Override
    public List<String> getKeys()
    {
        @SuppressWarnings("unchecked")
        List<String> keysWithExpiryCheck = this.cache.getKeysWithExpiryCheck();
        return keysWithExpiryCheck;
    }

    /**
     * @see org.apache.fulcrum.cache.GlobalCacheService#getNumberOfObjects()
     */
    @Override
    public int getNumberOfObjects()
    {
        return getKeys().size();
    }

    /**
     * @see org.apache.fulcrum.cache.GlobalCacheService#getObject(java.lang.String)
     */
    @Override
    public <T> CachedObject<T> getObject(String id) throws ObjectExpiredException
    {
        Element cachedElement = this.cache.get(id);

        if (cachedElement == null)
        {
            // Not in the cache.
            throw new ObjectExpiredException();
        }

        @SuppressWarnings("unchecked")
        CachedObject<T> obj = (CachedObject<T>)cachedElement.getObjectValue();

        if (obj.isStale())
        {
            if (obj instanceof RefreshableCachedObject)
            {
                RefreshableCachedObject<?> rco = (RefreshableCachedObject<?>) obj;
                if (rco.isUntouched())
                {
                    // Do not refresh an object that has exceeded TimeToLive
                    removeObject(id);
                    throw new ObjectExpiredException();
                }

                // Refresh Object
                rco.refresh();
                if (rco.isStale())
                {
                    // Object is Expired, remove it from cache.
                    removeObject(id);
                    throw new ObjectExpiredException();
                }
            }
            else
            {
                // Expired.
                removeObject(id);
                throw new ObjectExpiredException();
            }
        }

        if (obj instanceof RefreshableCachedObject)
        {
            // notify it that it's being accessed.
            RefreshableCachedObject<?> rco = (RefreshableCachedObject<?>) obj;
            rco.touch();
        }

        return obj;
    }

    /**
     * @see org.apache.fulcrum.cache.GlobalCacheService#removeObject(java.lang.String)
     */
    @Override
    public void removeObject(String id)
    {
        this.cache.remove(id);
    }

    /**
     * Circle through the cache and refresh stale objects. Frequency is
     * determined by the cacheCheckFrequency property.
     */
    @Override
    public void run()
    {
        while (this.continueThread)
        {
            // Sleep for amount of time set in cacheCheckFrequency -
            // default = 5 seconds.
            try
            {
                Thread.sleep(this.cacheCheckFrequency);
            }
            catch (InterruptedException exc)
            {
                if (!this.continueThread)
                {
                    return;
                }
            }

            for (String key : getKeys())
            {
                Element cachedElement = this.cache.get(key);

                if (cachedElement == null)
                {
                    this.cache.remove(key);
                    continue;
                }

                Object o = cachedElement.getObjectValue();

                if (o instanceof RefreshableCachedObject)
                {
                    RefreshableCachedObject<?> rco = (RefreshableCachedObject<?>) o;
                    if (rco.isUntouched())
                    {
                        this.cache.remove(key);
                    }
                    else if (rco.isStale())
                    {
                        rco.refresh();
                    }
                }
            }
        }
    }
}
