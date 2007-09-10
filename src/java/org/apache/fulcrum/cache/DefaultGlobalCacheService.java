package org.apache.fulcrum.cache;

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
import java.util.Enumeration;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.List;

import org.apache.avalon.framework.activity.Disposable;
import org.apache.avalon.framework.activity.Initializable;
import org.apache.avalon.framework.configuration.Configurable;
import org.apache.avalon.framework.configuration.Configuration;
import org.apache.avalon.framework.configuration.ConfigurationException;
import org.apache.avalon.framework.logger.AbstractLogEnabled;
import org.apache.avalon.framework.thread.ThreadSafe;
/**
 * This Service functions as a Global Cache.  A global cache is a good
 * place to store items that you may need to access often but don't
 * necessarily need (or want) to fetch from the database everytime.  A
 * good example would be a look up table of States that you store in a
 * database and use throughout your application.  Since information
 * about States doesn't change very often, you could store this
 * information in the Global Cache and decrease the overhead of
 * hitting the database everytime you need State information.
 *
 * @author <a href="mailto:mbryson@mont.mindspring.com">Dave Bryson</a>
 * @author <a href="mailto:jon@clearink.com">Jon S. Stevens</a>
 * @author <a href="mailto:john@zenplex.com">John Thorhauer</a>
 * @author <a href="mailto:hps@intermeta.de">Henning P. Schmiedehausen</a>
 * @author <a href="mailto:epugh@upstate.com">Eric Pugh</a>
 * @author <a href="mailto:peter@courcoux.biz">Peter Courcoux</a>
 * * @version $Id$
 */
public class DefaultGlobalCacheService
    extends AbstractLogEnabled
    implements GlobalCacheService, Runnable, Configurable, Initializable, Disposable, ThreadSafe
{
    /**
    	 * Initial size of hash table
    	 * Value must be > 0.
    	 * Default = 20
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
	 * Cache check frequency in Millis (1000 Millis = 1 second).
	 * Value must be > 0.
	 * Default = 5 seconds
	 */
    public static final long DEFAULT_CACHE_CHECK_FREQUENCY = 5000; // 5 seconds
    /** The cache. **/
    protected Hashtable cache = null;
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
     * @return
     */
    public long getCacheCheckFrequency()
    {
        return cacheCheckFrequency;
    }

    public DefaultGlobalCacheService()
    {
    }
    /**
     * Returns an item from the cache.
    /**
     * Returns an item from the cache.  RefreshableCachedObject will be
     * refreshed if it is expired and not untouched.
     *
     * @param id The key of the stored object.
     * @return The object from the cache.
     * @exception ObjectExpiredException, when either the object is
     * not in the cache or it has expired.
     */
    public CachedObject getObject(String id) throws ObjectExpiredException
    {
        CachedObject obj = null;
        obj = (CachedObject) cache.get(id);
        if (obj == null)
        {
            // Not in the cache.
            throw new ObjectExpiredException();
        }
        if (obj.isStale())
        {
            if (obj instanceof RefreshableCachedObject)
            {
                RefreshableCachedObject rco = (RefreshableCachedObject) obj;
                if (rco.isUntouched()) // Do not refresh an object that has exceeded TimeToLive
                    throw new ObjectExpiredException();
                // Refresh Object
                rco.refresh();
                if (rco.isStale()) // Object is Expired.
                    throw new ObjectExpiredException();
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
            RefreshableCachedObject rco = (RefreshableCachedObject) obj;
            rco.touch();
        }
        return obj;
    }
    /**
     * Adds an object to the cache.
     *
     * @param id The key to store the object by.
     * @param o The object to cache.
     */
    public void addObject(String id, CachedObject o)
    {
        // If the cache already contains the key, remove it and add
        // the fresh one.
        if (cache.containsKey(id))
        {
            cache.remove(id);
        }
        cache.put(id, o);
    }
    /**
     * Removes an object from the cache.
     *
     * @param id The String id for the object.
     */
    public void removeObject(String id)
    {
        cache.remove(id);
    }

    /**
     * Returns a copy of keys to objects in the cache as a list.
     *
     * Note that keys to expired objects are not returned.
     *
     * @return A List of <code>String</code>'s representing the keys to objects
     * in the cache.
     */
    public List getKeys() {
        ArrayList keys = new ArrayList(cache.size());
        synchronized (this) {
            for (Iterator itr = cache.keySet().iterator(); itr.hasNext();)
            {
                String key = (String) itr.next();
                try {
                    /* CachedObject obj = */ getObject(key);
                } catch (ObjectExpiredException oee) {
                    // this is OK we just do not want this key
                    continue;
                }
                keys.add(new String(key));
            }
        }
        return (List)keys;
    }

    /**
     * Returns a copy of the non-expired CachedObjects
     * in the cache as a list.
     *
     * @return A List of <code>CachedObject</code> objects
     * held in the cache
     */
    public List getCachedObjects() {
        ArrayList objects = new ArrayList(cache.size());
        synchronized (this) {
            for (Iterator itr = cache.keySet().iterator(); itr.hasNext();)
            {
                String key = (String) itr.next();
                CachedObject obj = null;
                try {
                    obj = getObject(key);
                } catch (ObjectExpiredException oee) {
                    // this is OK we just do not want this object
                    continue;
                }
                objects.add(obj);
            }
        }
        return (List)objects;
    }


    /**
     * Circle through the cache and remove stale objects.  Frequency
     * is determined by the cacheCheckFrequency property.
     */
    public void run()
    {
        while (continueThread)
        {
            // Sleep for amount of time set in cacheCheckFrequency -
            // default = 5 seconds.
            try
            {
                Thread.sleep(cacheCheckFrequency);
            }
            catch (InterruptedException exc)
            {
                if (!continueThread) return;
            }

            clearCache();
        }
    }
    /**
     * Iterate through the cache and remove or refresh stale objects.
     */
    public void clearCache()
    {
        List refreshThese = new ArrayList(20);
        // Sync on this object so that other threads do not
        // change the Hashtable while enumerating over it.
        synchronized (this)
        {
            for (Enumeration e = cache.keys(); e.hasMoreElements();)
            {
                String key = (String) e.nextElement();
                CachedObject co = (CachedObject) cache.get(key);
                if (co instanceof RefreshableCachedObject)
                {
                    RefreshableCachedObject rco = (RefreshableCachedObject) co;
                    if (rco.isUntouched())
                        cache.remove(key);
                    else if (rco.isStale()) // Do refreshing outside of sync block so as not
                        // to prolong holding the lock on this object
                        refreshThese.add(key);
                }
                else if (co.isStale())
                {
                    cache.remove(key);
                }
            }
        }
        for (Iterator i = refreshThese.iterator(); i.hasNext();)
        {
            String key = (String) i.next();
            CachedObject co = (CachedObject) cache.get(key);
            RefreshableCachedObject rco = (RefreshableCachedObject) co;
            rco.refresh();
        }
    }
    /**
     * Returns the number of objects currently stored in the cache
     *
     * @return int number of object in the cache
     */
    public int getNumberOfObjects()
    {
        return cache.size();
    }
    /**
     * Returns the current size of the cache.
     *
     * @return int representing current cache size in number of bytes
     */
    public int getCacheSize() throws IOException
    {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        ObjectOutputStream out = new ObjectOutputStream(baos);
        out.writeObject(cache);
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
    public void flushCache()
    {
        synchronized (this)
        {
            for (Enumeration e = cache.keys(); e.hasMoreElements();)
            {
                String key = (String) e.nextElement();
                cache.remove(key);
            }
        }
    }

    // ---------------- Avalon Lifecycle Methods ---------------------
    /**
     * Avalon component lifecycle method
     */
    public void configure(Configuration conf) throws ConfigurationException
    {
		cacheCheckFrequency = conf.getAttributeAsLong(CACHE_CHECK_FREQUENCY, DEFAULT_CACHE_CHECK_FREQUENCY);
		cacheInitialSize = conf.getAttributeAsInteger(INITIAL_CACHE_SIZE, DEFAULT_INITIAL_CACHE_SIZE);
    }

    /**
     * Avalon component lifecycle method
     */
    public void initialize() throws Exception
    {
        try
        {
            cache = new Hashtable(cacheInitialSize);
            // Start housekeeping thread.
            continueThread = true;
            housekeeping = new Thread(this);
            // Indicate that this is a system thread. JVM will quit only when
            // there are no more active user threads. Settings threads spawned
            // internally by Turbine as daemons allows commandline applications
            // using Turbine to terminate in an orderly manner.
            housekeeping.setDaemon(true);
            housekeeping.start();
        }
        catch (Exception e)
        {
            throw new Exception("DefaultGlobalCacheService failed to initialize", e);
        }
    }

    /**
     * Avalon component lifecycle method
     */
    public void dispose()
    {
        continueThread = false;
        housekeeping.interrupt();
    }

    /**
     * The name used to specify this component in TurbineResources.properties
     * @deprecated part of the pre-avalon compatibility layer
     */
    protected String getName()
    {
        return "GlobalCacheService";
    }
}
