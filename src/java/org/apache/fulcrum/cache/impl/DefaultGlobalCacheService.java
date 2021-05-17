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
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

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

/**
 * This Service functions as a Global Cache. A global cache is a good place to
 * store items that you may need to access often but don't necessarily need (or
 * want) to fetch from the database everytime. A good example would be a look up
 * table of States that you store in a database and use throughout your
 * application. Since information about States doesn't change very often, you
 * could store this information in the Global Cache and decrease the overhead of
 * hitting the database everytime you need State information.
 *
 * @author <a href="mailto:mbryson@mont.mindspring.com">Dave Bryson</a>
 * @author <a href="mailto:jon@clearink.com">Jon S. Stevens</a>
 * @author <a href="mailto:john@zenplex.com">John Thorhauer</a>
 * @author <a href="mailto:hps@intermeta.de">Henning P. Schmiedehausen</a>
 * @author <a href="mailto:epugh@upstate.com">Eric Pugh</a>
 * @author <a href="mailto:peter@courcoux.biz">Peter Courcoux</a>
 * @version $Id$
 */
public class DefaultGlobalCacheService extends AbstractLogEnabled implements
        GlobalCacheService, Runnable, Configurable, Initializable, Disposable,
        ThreadSafe
{
    /**
     * Initial size of hash table Value must be &gt; 0. Default = 20
     */
    public static final int DEFAULT_INITIAL_CACHE_SIZE = 20;

    /**
     * The property for the InitalCacheSize
     */
    public static final String INITIAL_CACHE_SIZE = "cacheInitialSize";

    /**
     * The property for the Cache check frequency
     */
    public static final String CACHE_CHECK_FREQUENCY = "cacheCheckFrequency";

    /**
     * Cache check frequency in Millis (1000 Millis = 1 second). Value must be &gt;
     * 0. Default = 5 seconds
     */
    public static final long DEFAULT_CACHE_CHECK_FREQUENCY = 5000; // 5 seconds

    /** The cache. * */
    protected ConcurrentHashMap<String, CachedObject<?>> cache = null;

    /**
     * cacheCheckFrequency (default - 5 seconds)
     */
    private long cacheCheckFrequency;

    /**
     * cacheInitialSize (default - 20)
     */
    private int cacheInitialSize;

    /** thread for removing stale items from the cache */
    private Thread housekeeping;

    /** flag to stop the housekeeping thread when the component is disposed. */
    private boolean continueThread;

    /**
     * Get the Cache Check Frequency in milliseconds
     *
     * @return the time between two cache check runs in milliseconds
     */
    public long getCacheCheckFrequency()
    {
        return this.cacheCheckFrequency;
    }

    /**
     * Returns an item from the cache. /** Returns an item from the cache.
     * RefreshableCachedObject will be refreshed if it is expired and not
     * untouched.
     *
     * @param id
     *            The key of the stored object.
     * @return The object from the cache.
     * @throws ObjectExpiredException
     *                when either the object is not in the cache or it has
     *                expired.
     */
    @Override
    public <T> CachedObject<T> getObject(String id) throws ObjectExpiredException
    {
        @SuppressWarnings("unchecked")
        CachedObject<T> obj = (CachedObject<T>) this.cache.get(id);
        if (obj == null)
        {
            // Not in the cache.
            throw new ObjectExpiredException();
        }
        if (obj.isStale())
        {
            if (obj instanceof RefreshableCachedObject)
            {
                RefreshableCachedObject<?> rco = (RefreshableCachedObject<?>) obj;
                if (rco.isUntouched())
                {
                    throw new ObjectExpiredException();
                }
                // Refresh Object
                rco.refresh();
                if (rco.isStale())
                {
                    throw new ObjectExpiredException();
                }
            }
            else
            {
                // Expired.
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
     * Adds an object to the cache.
     *
     * @param id
     *            The key to store the object by.
     * @param o
     *            The object to cache.
     */
    @Override
    public <T> void addObject(String id, CachedObject<T> o)
    {
        // If the cache already contains the key, remove it and add
        // the fresh one.
        if (this.cache.containsKey(id))
        {
            this.cache.remove(id);
        }
        this.cache.put(id, o);
    }

    /**
     * Removes an object from the cache.
     *
     * @param id
     *            The String id for the object.
     */
    @Override
    public void removeObject(String id)
    {
        this.cache.remove(id);
    }

    /**
     * Returns a copy of keys to objects in the cache as a list.
     *
     * Note that keys to expired objects are not returned.
     *
     * @return A List of <code>String</code>'s representing the keys to
     *         objects in the cache.
     */
    @Override
    public List<String> getKeys()
    {
        ArrayList<String> keys = new ArrayList<String>(this.cache.size());
        for (String key : this.cache.keySet())
        {
            try
            {
                /* CachedObject obj = */getObject(key);
            }
            catch (ObjectExpiredException oee)
            {
                // this is OK we just do not want this key
                continue;
            }
            keys.add(key);
        }
        return keys;
    }

    /**
     * Returns a copy of the non-expired CachedObjects in the cache as a list.
     *
     * @return A List of <code>CachedObject</code> objects held in the cache
     */
    @Override
    public List<CachedObject<?>> getCachedObjects()
    {
        ArrayList<CachedObject<?>> objects = new ArrayList<CachedObject<?>>(this.cache.size());
        for (String key : this.cache.keySet())
        {
            CachedObject<?> obj = null;
            try
            {
                obj = getObject(key);
            }
            catch (ObjectExpiredException oee)
            {
                // this is OK we just do not want this object
                continue;
            }
            objects.add(obj);
        }
        return objects;
    }

    /**
     * Circle through the cache and remove stale objects. Frequency is
     * determined by the cacheCheckFrequency property.
     */
    @Override
    public void run()
    {
        while (this.continueThread)
        {
            // Sleep for amount of time set in cacheCheckFrequency -
            // default = 5 seconds.
            synchronized (this)
            {
                try
                {
                    wait(this.cacheCheckFrequency);
                }
                catch (InterruptedException exc)
                {
                    // to be expected
                }
            }

            clearCache();
        }
    }

    /**
     * Iterate through the cache and remove or refresh stale objects.
     */
    public void clearCache()
    {
        List<String> refreshThese = new ArrayList<String>(20);
        // Sync on this object so that other threads do not
        // change the Hashtable while enumerating over it.
        for (String key : this.cache.keySet())
        {
            CachedObject<?> co = this.cache.get(key);
            if (co instanceof RefreshableCachedObject)
            {
                RefreshableCachedObject<?> rco = (RefreshableCachedObject<?>) co;
                if (rco.isUntouched())
                {
                    this.cache.remove(key);
                }
                else if (rco.isStale())
                {
                    // to prolong holding the lock on this object
                    refreshThese.add(key);
                }
            }
            else if (co.isStale())
            {
                this.cache.remove(key);
            }
        }

        for (String key : refreshThese)
        {
            CachedObject<?> co = this.cache.get(key);
            RefreshableCachedObject<?> rco = (RefreshableCachedObject<?>) co;
            rco.refresh();
        }
    }

    /**
     * Returns the number of objects currently stored in the cache
     *
     * @return int number of object in the cache
     */
    @Override
    public int getNumberOfObjects()
    {
        return this.cache.size();
    }

    /**
     * Returns the current size of the cache.
     *
     * @return int representing current cache size in number of bytes
     */
    @Override
    public int getCacheSize() throws IOException
    {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        ObjectOutputStream out = new ObjectOutputStream(baos);
        out.writeObject(this.cache);
        out.flush();
        //
        // Subtract 4 bytes from the length, because the serialization
        // magic number (2 bytes) and version number (2 bytes) are
        // both written to the stream before the object
        //
        int objectsize = baos.toByteArray().length - 4;
        return objectsize;
    }

    /**
     * Flush the cache of all objects.
     */
    @Override
    public void flushCache()
    {
        this.cache.clear();
    }

    // ---------------- Avalon Lifecycle Methods ---------------------
    /**
     * Avalon component lifecycle method
     */
    @Override
    public void configure(Configuration conf) throws ConfigurationException
    {
        this.cacheCheckFrequency = conf.getAttributeAsLong(
                CACHE_CHECK_FREQUENCY, DEFAULT_CACHE_CHECK_FREQUENCY);
        this.cacheInitialSize = conf.getAttributeAsInteger(INITIAL_CACHE_SIZE,
                DEFAULT_INITIAL_CACHE_SIZE);
    }

    /**
     * Avalon component lifecycle method
     */
    @Override
    public void initialize() throws Exception
    {
        this.cache = new ConcurrentHashMap<String, CachedObject<?>>(this.cacheInitialSize);
        // Start housekeeping thread.
        this.continueThread = true;
        this.housekeeping = new Thread(this);
        // Indicate that this is a system thread. JVM will quit only when
        // there are no more active user threads. Settings threads spawned
        // internally by Turbine as daemons allows commandline applications
        // using Turbine to terminate in an orderly manner.
        this.housekeeping.setDaemon(true);
        this.housekeeping.start();
    }

    /**
     * Avalon component lifecycle method
     */
    @Override
    public void dispose()
    {
        synchronized (this)
        {
            this.continueThread = false;
            notify();
        }
    }
}
