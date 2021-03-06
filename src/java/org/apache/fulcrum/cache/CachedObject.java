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

import java.io.Serializable;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Wrapper for an object you want to store in a cache for a period of time.
 *
 * @author <a href="mailto:mbryson@mont.mindspring.com">Dave Bryson</a>
 * @author <a href="mailto:hps@intermeta.de">Henning P. Schmiedehausen</a>
 * @author <a href="mailto:epugh@upstate.com">Eric Pugh</a>
 * @version $Id$
 */

public class CachedObject<T> implements Serializable
{
    /*
     * TODO: The old Turbine version you could set the default age from Turbine.
     * What we need is a CachedObjectFactory that would generate CachedObject's
     * that could then have their default age set.
     */

    /**
     * Serialization key
     */
    private static final long serialVersionUID = -9107764093769042092L;

    /** Cache the object with the Default TTL */
    public static final int DEFAULT = 0;

    /** Do not expire the object */
    public static final int FOREVER = -1;

    /** The object to be cached. */
    private T contents = null;

    /** Default age (30 minutes). */
    private static final long DEFAULT_AGE = 1_800_000;

    /** When the object is created. */
    protected long created;

    /** When the object should expire. */
    private long expires;

    /** Is this object stale/expired? */
    private final AtomicBoolean stale = new AtomicBoolean();

    /**
     * Constructor; sets the object to expire in the default time (30 minutes).
     *
     * @param object
     *            The object you want to cache.
     */
    public CachedObject(final T object)
    {
        this(object, DEFAULT);
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
    public CachedObject(final T object, final long expires)
    {
        if (expires == DEFAULT)
        {
            this.expires = this.DEFAULT_AGE;
        } else {
            this.expires = expires;
        }

        this.contents = object;

        this.created = System.currentTimeMillis();
    }

    /**
     * Returns the cached object.
     *
     * @return The cached object.
     */
    public T getContents()
    {
        return this.contents;
    }

    /**
     * Returns the creation time for the object.
     *
     * @return When the object was created.
     */
    public long getCreated()
    {
        return this.created;
    }

    /**
     * Returns the expiration time for the object.
     *
     * @return When the object expires.
     */
    public long getExpires()
    {
        return this.expires;
    }

    /**
     * Set the expiration interval for the object.
     *
     * @param expires
     *            Expiration interval in millis ( 1 second = 1000 millis)
     */
    public void setExpires(final long expires)
    {
        if (expires == DEFAULT)
        {
            this.expires = this.DEFAULT_AGE;
        }
        else
        {
            this.expires = expires;
        }
        if (expires == FOREVER)
        {
            setStale(false);
        }
        else
        {
            setStale((System.currentTimeMillis() - this.created) > expires);
        }
    }

    /**
     * Set the stale status for the object.
     *
     * @param stale
     *            Whether the object is stale or not.
     */
    public void setStale(final boolean stale)
    {
        this.stale.set( stale );
    }
    

    /**
     * Is the object stale?
     *
     * @return True if the object is stale.
     */
    public boolean isStale()
    {
    	boolean currentState = false;
        if (this.expires != FOREVER)
        {
        	setStale((System.currentTimeMillis() - this.created) > this.expires);
        	currentState = this.stale.get();
        }
        return currentState;
    }
}
