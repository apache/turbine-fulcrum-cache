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

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import org.apache.avalon.framework.activity.Disposable;
import org.apache.avalon.framework.activity.Initializable;
import org.apache.avalon.framework.configuration.Configurable;
import org.apache.avalon.framework.configuration.Configuration;
import org.apache.avalon.framework.configuration.ConfigurationException;
import org.apache.avalon.framework.logger.AbstractLogEnabled;
import org.apache.avalon.framework.thread.ThreadSafe;
import org.apache.commons.jcs3.JCS;
import org.apache.commons.jcs3.access.GroupCacheAccess;
import org.apache.commons.jcs3.access.exception.CacheException;
import org.apache.commons.jcs3.engine.ElementAttributes;
import org.apache.commons.jcs3.engine.control.CompositeCacheManager;
import org.apache.fulcrum.cache.CachedObject;
import org.apache.fulcrum.cache.GlobalCacheService;
import org.apache.fulcrum.cache.ObjectExpiredException;
import org.apache.fulcrum.cache.RefreshableCachedObject;

/**
 * Default implementation of JCSCacheService
 *
 * @author <a href="mailto:tv@apache.org">Thomas Vandahl</a>
 * @version $Id$
 */
public class JCSCacheService extends AbstractLogEnabled implements
        GlobalCacheService, Runnable, Configurable, Disposable, Initializable,
        ThreadSafe
{
    /**
     * Cache check frequency in Millis (1000 Millis = 1 second). Value must be &gt;
     * 0. Default = 5 seconds
     */
    public static final long DEFAULT_CACHE_CHECK_FREQUENCY = 5000; // 5 seconds

    /**
     * cacheCheckFrequency (default - 5 seconds)
     */
    private long cacheCheckFrequency;

    /**
     * Instance of the JCS cache
     */
    private GroupCacheAccess<String, CachedObject<?>> cacheManager;

    /**
     * JCS region to use
     */
    private String region;

    /**
     * Path name of the JCS configuration file
     */
    private String configFile;

    /**
     * Constant value which provides a group name
     */
    private static String group = "default_group";

    /** thread for refreshing stale items in the cache */
    private Thread refreshing;

    /** flag to stop the housekeeping thread when the component is disposed. */
    private boolean continueThread;

    // ---------------- Avalon Lifecycle Methods ---------------------

    /**
     * @see org.apache.avalon.framework.configuration.Configurable#configure(org.apache.avalon.framework.configuration.Configuration)
     */
    @Override
	public void configure(Configuration config) throws ConfigurationException
    {
        this.cacheCheckFrequency = config.getChild("cacheCheckFrequency")
                .getValueAsLong(DEFAULT_CACHE_CHECK_FREQUENCY);
        this.region = config.getChild("region").getValue("fulcrum");
        this.configFile = config.getChild("configurationFile").getValue(
                "/cache.ccf");
    }

    /**
     * @see org.apache.avalon.framework.activity.Initializable#initialize()
     */
    @Override
	public void initialize() throws Exception
    {
        JCS.setConfigFilename(this.configFile);
        this.cacheManager = JCS.getGroupCacheInstance(this.region);

        // Start housekeeping thread.
        this.continueThread = true;
        this.refreshing = new Thread(this);

        // Indicate that this is a system thread. JVM will quit only when
        // there are no more active user threads. Settings threads spawned
        // internally by Turbine as daemons allows commandline applications
        // using Turbine to terminate in an orderly manner.
        this.refreshing.setDaemon(true);
        this.refreshing.setName("JCSCacheService Refreshing");
        this.refreshing.start();

        getLogger().debug("JCSCacheService started.");
    }

    /**
     * @see org.apache.avalon.framework.activity.Disposable#dispose()
     */
    @Override
	public void dispose()
    {
        this.continueThread = false;
        this.refreshing.interrupt();

        this.cacheManager.dispose();
        this.cacheManager = null;
        CompositeCacheManager.getInstance().shutDown();

        getLogger().debug("JCSCacheService stopped.");
    }

    /**
     * @see org.apache.fulcrum.cache.GlobalCacheService#getObject(java.lang.String)
     */
    @Override
	public <T> CachedObject<T> getObject(String objectId) throws ObjectExpiredException
    {
        @SuppressWarnings("unchecked")
        CachedObject<T> cachedObject = (CachedObject<T>)this.cacheManager.getFromGroup(objectId, group);

        if (cachedObject == null)
        {
            // Not in the cache.
            throw new ObjectExpiredException();
        }

        if (cachedObject.isStale())
        {
            if (cachedObject instanceof RefreshableCachedObject)
            {
                RefreshableCachedObject<?> refreshableObject = (RefreshableCachedObject<?>) cachedObject;
                if (refreshableObject.isUntouched())
                {
                    // Do not refresh an object that has exceeded TimeToLive
                    removeObject(objectId);
                    throw new ObjectExpiredException();
                }

                // Refresh Object
                refreshableObject.refresh();
                if (refreshableObject.isStale())
                {
                    // Object is Expired, remove it from cache.
                    removeObject(objectId);
                    throw new ObjectExpiredException();
                }
            }
            else
            {
                // Expired.
                removeObject(objectId);
                throw new ObjectExpiredException();
            }
        }

        if (cachedObject instanceof RefreshableCachedObject)
        {
            // notify it that it's being accessed.
            RefreshableCachedObject<?> refreshableCachedObject = (RefreshableCachedObject<?>) cachedObject;
            refreshableCachedObject.touch();
        }

        return cachedObject;
    }

    /**
     * @see org.apache.fulcrum.cache.GlobalCacheService#addObject(java.lang.String,
     *      org.apache.fulcrum.cache.CachedObject)
     */
    @Override
	public <T> void addObject(String objectId, CachedObject<T> cachedObject)
    {
        try
        {
            if (!(cachedObject.getContents() instanceof Serializable))
            {
                getLogger()
                        .warn(
                                "Object with id ["
                                        + objectId
                                        + "] is not serializable. Expect problems with auxiliary caches.");
            }

            ElementAttributes attrib = (ElementAttributes) this.cacheManager.getDefaultElementAttributes();

            if (cachedObject instanceof RefreshableCachedObject)
            {
                attrib.setIsEternal(true);
            }
            else
            {
                attrib.setIsEternal(false);
                // expires in millis, maxlife in seconds
                double tmp0 = ((double) (cachedObject.getExpires() + 500)) / 1000;
                getLogger().debug( "setting maxlife seconds (minimum 1sec) from expiry + 0.5s: " + (int)tmp0 );
                attrib.setMaxLife( tmp0 > 0 ? (int) Math.floor( tmp0 ) : 1 );
            }

            attrib.setLastAccessTimeNow();
            attrib.setCreateTime();

            this.cacheManager.putInGroup(objectId, group, cachedObject, attrib);
        }
        catch (CacheException e)
        {
            getLogger().error("Could not add object " + objectId + " to cache", e);
        }
    }

    /**
     * @see org.apache.fulcrum.cache.GlobalCacheService#removeObject(java.lang.String)
     */
    @Override
	public void removeObject(String objectId)
    {
        this.cacheManager.removeFromGroup(objectId, group);
    }

    /**
     * @see org.apache.fulcrum.cache.GlobalCacheService#getKeys()
     */
    @Override
	public List<String> getKeys()
    {
        ArrayList<String> keys = new ArrayList<>();
        keys.addAll(this.cacheManager.getGroupKeys(group));
        return keys;
    }

    /**
     * @see org.apache.fulcrum.cache.GlobalCacheService#getCachedObjects()
     */
    @Override
	public List<CachedObject<?>> getCachedObjects()
    {
        ArrayList<CachedObject<?>> values = new ArrayList<>();
        for (String key : this.cacheManager.getGroupKeys(group))
        {
            CachedObject<?> cachedObject = this.cacheManager.getFromGroup(key, group);
            if (cachedObject != null)
            {
                values.add(cachedObject);
            }
        }

        return values;
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

            for (String key : this.cacheManager.getGroupKeys(group))
            {
                CachedObject<?> cachedObject = this.cacheManager.getFromGroup(key, group);
                if (cachedObject == null)
                {
                    removeObject(key);
                }
                else
                {
                    if (cachedObject instanceof RefreshableCachedObject)
                    {
                        RefreshableCachedObject<?> refreshableObject = (RefreshableCachedObject<?>) cachedObject;
                        if (refreshableObject.isUntouched())
                        {
                            this.cacheManager.removeFromGroup(key, group);
                        }
                        else if (refreshableObject.isStale())
                        {
                            refreshableObject.refresh();
                        }
                    }
                }
            }
        }
    }

    /**
     * @see org.apache.fulcrum.cache.GlobalCacheService#getCacheSize()
     */
    @Override
	public int getCacheSize() throws IOException
    {
        // This is evil!
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        ObjectOutputStream out = new ObjectOutputStream(baos);
        Set<String> keys = this.cacheManager.getGroupKeys(group);

        for (String key : keys)
        {
            out.writeObject(this.cacheManager.getFromGroup(key, group));
        }

        out.flush();

        //
        // Subtract 4 bytes from the length, because the serialization
        // magic number (2 bytes) and version number (2 bytes) are
        // both written to the stream before the object
        //
        return baos.toByteArray().length - 4 * keys.size();
    }

    /**
     * @see org.apache.fulcrum.cache.GlobalCacheService#getNumberOfObjects()
     */
    @Override
	public int getNumberOfObjects()
    {
        int count = 0;

        for (String key : this.cacheManager.getGroupKeys(group))
        {
            if (this.cacheManager.getFromGroup(key, group) != null)
            {
                count++;
            }
        }

        return count;
    }

    /**
     * @see org.apache.fulcrum.cache.GlobalCacheService#flushCache()
     */
    @Override
	public void flushCache()
    {
        this.cacheManager.invalidateGroup(group);
    }
}
