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

/**
 * The idea of the RefreshableCachedObject is that, rather than removing items
 * from the cache when they become stale, we'll tell them to refresh themselves
 * instead. That way they'll always be in the cache, and the code to refresh
 * them will be run by the background thread rather than by a user request
 * thread. You can also set a TTL (Time To Live) for the object. This way, if
 * the object hasn't been touched for the TTL period, then it will be removed
 * from the cache.
 *
 * This extends CachedObject and provides a method for refreshing the cached
 * object, and resetting its expire time.
 *
 * @author <a href="mailto:nissim@nksystems.com">Nissim Karpenstein</a>
 * @author <a href="mailto:hps@intermeta.de">Henning P. Schmiedehausen</a>
 * @version $Id$
 */
public class RefreshableCachedObject<T extends Refreshable> extends CachedObject<T>
{

    /**
     * Serialization key
     */
    private static final long serialVersionUID = 4072572956381768087L;

    /**
     * How long to wait before removing an untouched object from the cache.
     * Negative numbers mean never remove (the default).
     * 
     * time to live in millisec, getter/setter ({@link #getTTL()}
     * 
     */
    private long timeToLive = -1;

    /**
     * The last time the Object was accessed from the cache.
     */
    private long lastAccess;

    /**
     * Constructor; sets the object to expire in the default time (30 minutes).
     *
     * @param object
     *            The object you want to cache.
     */
    public RefreshableCachedObject(final T object)
    {
        super(object);
        setLastAccess(System.currentTimeMillis());
    }

    /**
     * Constructor.
     *
     * @param object
     *            The object to cache.
     * @param expires
     *            How long before the object expires, in ms, e.g. 1000 = 1
     *            second.
     */
    public RefreshableCachedObject(final T object, final long expires)
    {
        super(object, expires);
        setLastAccess(System.currentTimeMillis());
    }

    /**
     * Sets the timeToLive value
     *
     * @param timeToLive
     *            the new Value in milliseconds
     */
    public void setTTL(final long timeToLive)
    {
        synchronized(this) {
            this.timeToLive = timeToLive;
        }
    }

    /**
     * Gets the timeToLive value.
     *
     * @return The current timeToLive value (in milliseconds)
     */
    public long getTTL()
    {
        synchronized(this) {
            return this.timeToLive;
        }
    }

    /**
     * Sets the last access time to the current time.
     */
    public void touch()
    {
        synchronized(this) {
            this.lastAccess = System.currentTimeMillis();
        }
    }

    /**
     * Returns true if the object hasn't been touched in the previous TTL
     * period.
     * 
     * @return boolean status of object
     */
    public boolean isUntouched()
    {
        boolean untouched = false;
        synchronized(this) {
            if (this.lastAccess + this.timeToLive < System.currentTimeMillis())
            {
            	untouched = true;
            }
            return untouched;
        }
    }

    /**
     * Refresh the object and the created time.
     */
    public void refresh()
    {
        final Refreshable refreshable = getContents();
        synchronized (this)
        {
            this.created = System.currentTimeMillis();
            refreshable.refresh();
        }
    }

    public long getLastAccess()
    {
        return lastAccess;
    }

    public void setLastAccess(long lastAccess)
    {
        this.lastAccess = lastAccess;
    }
}
