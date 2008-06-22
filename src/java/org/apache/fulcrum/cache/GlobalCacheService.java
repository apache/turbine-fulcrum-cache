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

import java.io.IOException;
import java.util.List;

import org.apache.avalon.framework.component.Component;

/**
 * GlobalCacheService interface.
 * 
 * @author <a href="mailto:mbryson@mont.mindspring.com">Dave Bryson</a>
 * @author <a href="mailto:peter@courcoux.biz">Peter Courcoux</a>
 * @version $Id$
 */
public interface GlobalCacheService extends Component
{
    /*
     * TODO: This GlobalCacheService should be usable by both the
     * DefaultEHCacheService and the DefaultGlobalCache. The CacheObject class
     * from GCS and the Element class from EHCS are very similar.
     */

    /** Avalon role - used to id the component within the manager */
    String ROLE = GlobalCacheService.class.getName();

    /**
     * Gets a cached object given its id (a String).
     * 
     * @param id
     *            The String id for the object.
     * @return A CachedObject.
     * @exception ObjectExpiredException,
     *                if the object has expired in the cache.
     */
    CachedObject getObject(String id) throws ObjectExpiredException;

    /**
     * Adds an object to the cache.
     * 
     * @param id
     *            The String id for the object.
     * @param o
     *            The object to add to the cache.
     */
    void addObject(String id, CachedObject o);

    /**
     * Removes an object from the cache.
     * 
     * @param id
     *            The String id for the object.
     */
    void removeObject(String id);

    /**
     * Returns a copy of keys to objects in the cache as a list.
     * 
     * Note that keys to expired objects are not returned.
     * 
     * @return A List of <code>String</code>'s representing the keys to
     *         objects in the cache.
     */
    public List getKeys();

    /**
     * Returns a copy of the non-expired CachedObjects in the cache as a list.
     * 
     * @return A List of <code>CachedObject</code> objects held in the cache
     */
    public List getCachedObjects();

    /**
     * Returns the current size of the cache.
     * 
     * @return int representing current cache size in number of bytes
     */
    int getCacheSize() throws IOException;

    /**
     * Returns the number of objects in the cache.
     * 
     * @return int The current number of objects in the cache.
     */
    int getNumberOfObjects();

    /**
     * Flush the cache of all objects.
     */
    void flushCache();
}
