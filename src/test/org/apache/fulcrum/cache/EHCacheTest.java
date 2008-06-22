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

package org.apache.fulcrum.cache;

import net.sf.ehcache.Cache;
import net.sf.ehcache.Element;

import org.apache.avalon.framework.component.ComponentException;
import org.apache.fulcrum.testcontainer.BaseUnitTest;

/**
 * EHCacheTest
 * 
 * @author <a href="epugh@opensourceconnections.com">Eric Pugh</a>
 * @version $Id: CacheTest.java 223198 2004-11-09 08:30:41Z epugh $
 */
public class EHCacheTest extends BaseUnitTest
{

    private EHCacheService ehCacheService = null;

    private static final String cacheKey = "CacheKey";

    private static final String cacheKey_2 = "CacheKey_2";

    /**
     * Defines the testcase name for JUnit.
     * 
     * @param name
     *            the testcase's name.
     */
    public EHCacheTest(String name)
    {
        super(name);
    }

    protected void setUp() throws Exception
    {
        super.setUp();
        try
        {
            this.ehCacheService = (EHCacheService) this
                    .lookup(EHCacheService.ROLE);
        }
        catch (ComponentException e)
        {
            e.printStackTrace();
            fail(e.getMessage());
        }
    }

    /**
     * Simple test that verify an object can be created and deleted.
     * 
     * @throws Exception
     */
    public void testSimpleAddGetCacheObject() throws Exception
    {
        Cache ehCache = new Cache("test", 1, true, false, 5, 2);
        this.ehCacheService.getCacheManager().addCache(ehCache);
        String testString = "This is a test";
        Object retrievedObject = null;
        Element cacheObject1 = null;
        // Create object
        cacheObject1 = new Element(cacheKey, testString);
        assertNotNull("Failed to create a cachable object 1", cacheObject1);
        // Add object to cache
        ehCache.put(cacheObject1);
        // Get object from cache
        retrievedObject = ehCache.get(cacheKey);
        assertNotNull("Did not retrieve a cached object 1", retrievedObject);
        assertSame("Did not retrieve a correct, expected cached object 1",
                retrievedObject, cacheObject1);
        // Remove object from cache
        ehCache.remove(cacheKey);
        // Verify object removed from cache
        retrievedObject = null;
        cacheObject1 = null;

        retrievedObject = ehCache.get(cacheKey);
        assertNull(
                "Retrieved the deleted cached object 1 and did not get expected ObjectExpiredException",
                retrievedObject);

        // Remove object from cache that does NOT exist in the cache
        ehCache.remove(cacheKey);
    }

    /**
     * Simple test that adds, retrieves, and deletes 2 object.
     * 
     * @throws Exception
     */
    public void test2ObjectAddGetCachedObject() throws Exception
    {
        Cache ehCache = new Cache("test2", 1, true, false, 5, 2);
        this.ehCacheService.getCacheManager().addCache(ehCache);
        String testString = "This is a test";
        Object retrievedObject = null;
        Element cacheObject1 = null;
        Element cacheObject2 = null;
        // Create and add Object #1
        cacheObject1 = new Element(cacheKey, testString);
        assertNotNull("Failed to create a cachable object 1", cacheObject1);
        ehCache.put(cacheObject1);
        retrievedObject = ehCache.get(cacheKey);
        assertNotNull("Did not retrieved a cached object 1", retrievedObject);
        assertEquals("Did not retrieved correct cached object", cacheObject1,
                retrievedObject);
        // Create and add Object #2
        cacheObject2 = new Element(cacheKey_2, testString);
        assertNotNull("Failed to create a cachable object 2", cacheObject2);
        ehCache.put(cacheObject2);
        retrievedObject = ehCache.get(cacheKey_2);
        assertNotNull("Did not retrieved a cached object 2", retrievedObject);
        assertEquals("Did not retrieved correct cached object 2", cacheObject2,
                retrievedObject);
        // Get object #1
        retrievedObject = ehCache.get(cacheKey);
        assertNotNull("Did not retrieved a cached object 1. Attempt #2",
                retrievedObject);
        assertEquals("Did not retrieved correct cached object 1. Attempt #2",
                cacheObject1, retrievedObject);
        // Get object #1
        retrievedObject = ehCache.get(cacheKey);
        assertNotNull("Did not retrieved a cached object 1. Attempt #3",
                retrievedObject);
        assertEquals("Did not retrieved correct cached object 1. Attempt #3",
                cacheObject1, retrievedObject);
        // Get object #2
        retrievedObject = ehCache.get(cacheKey_2);
        assertNotNull("Did not retrieved a cached object 2. Attempt #2",
                retrievedObject);
        assertEquals("Did not retrieved correct cached object 2 Attempt #2",
                cacheObject2, retrievedObject);
        // Remove objects
        ehCache.remove(cacheKey);
        ehCache.remove(cacheKey_2);
    }

    /**
     * Verify that an object expiration when it now longer exists in cache.
     * 
     * @throws Exception
     */
    public void testObjectExpiration() throws Exception
    {
        Cache ehCache = new Cache("expire", 1, true, false, 2, 2);
        this.ehCacheService.getCacheManager().addCache(ehCache);
        String testString = "This is a test";
        Object retrievedObject = null;
        Element cacheObject = null;
        // Create and add Object that expires in 1000 millis (1 second)
        cacheObject = new Element(cacheKey, testString);
        assertNotNull("Failed to create a cachable object", cacheObject);
        ehCache.put(cacheObject);

        // Try to get un-expired object
        retrievedObject = ehCache.get(cacheKey);
        assertNotNull("Did not retrieved a cached object", retrievedObject);
        assertEquals("Did not retrieved correct cached object", cacheObject,
                retrievedObject);

        // Sleep 3000 Millis (3 seconds)
        Thread.sleep(3000);
        // Try to get expired object

        retrievedObject = null;
        retrievedObject = ehCache.get(cacheKey);
        assertNull(
                "Retrieved the expired cached object  and did not get expected ObjectExpiredException",
                retrievedObject);

        // Remove objects
        ehCache.remove(cacheKey);
    }
}
