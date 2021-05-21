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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

import java.util.ConcurrentModificationException;
import java.util.Date;
import java.util.List;

import org.apache.avalon.framework.component.ComponentException;
import org.apache.fulcrum.cache.impl.DefaultGlobalCacheService;
import org.apache.fulcrum.testcontainer.BaseUnit5Test;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

/**
 * CacheTest
 *
 * @author <a href="paulsp@apache.org">Paul Spencer</a>
 * @author <a href="epugh@upstate.com">Eric Pugh</a>
 * @author <a href="mailto:peter@courefreshableCachedObjectux.biz">Peter CourefreshableCachedObjectux</a>
 * @version $Id$
 */
public class CacheTest extends BaseUnit5Test
{

    protected GlobalCacheService globalCache = null;

    protected static final String cacheKey = "CacheKey";

    protected static final String cacheKey_2 = "CacheKey_2";

    protected static final Logger log = LogManager.getLogger( CacheTest.class );

    static {
        String logSystem = System.getProperty("jcs.logSystem", null);
        if (logSystem == null) {
            System.setProperty("jcs.logSystem", "log4j2" );
            log.info( "Setting jcs.logSystem to: log4j2");
            logSystem = System.getProperty("jcs.logSystem", null);
        }
        log.warn( "What is the value of the jcs.logSystem: "+ logSystem);
        
    }

    /**
     * Method to configure the role name of the service used
     *
     * @return the role name of the service to lookup
     */
    protected String getCacheRoleName()
    {
        return GlobalCacheService.ROLE;
    }

    @BeforeEach
    protected void setUp() throws Exception
    {
        System.out.println( "Testing service: "+ getClass().getName() + "for "+ getCacheRoleName());
        //if (globalCache == null) {
            try
            {
                globalCache = (GlobalCacheService) this
                        .lookup(getCacheRoleName());
            }
            catch (ComponentException e)
            {
                e.printStackTrace();
                fail(e.getMessage());
            }
        //}
    }
    
    @AfterEach
    protected void cleanup() {
        this.globalCache.removeObject(cacheKey);
    }

    /**
     * Simple test that verify an object can be created and deleted.
     *
     * @throws Exception if unable to add object
     */
    @Test
    public void testSimpleAddGetCacheObject() throws Exception
    {
        String testString = "This is a test";
        Object retrievedObject = null;
        CachedObject<String> cacheObject1 = null;
        // Create object
        cacheObject1 = new CachedObject<String>(testString);
        assertNotNull(cacheObject1, "Failed to create a cachable object 1" );
        // Add object to cache
        this.globalCache.addObject(cacheKey, cacheObject1);
        // Get object from cache
        retrievedObject = this.globalCache.getObject(cacheKey);
        assertNotNull( retrievedObject, "Did not retrieve a cached object 1");
        assertSame(
                retrievedObject, cacheObject1,
                "Did not retrieve a correct, expected cached object 1");
        // Remove object from cache
        this.globalCache.removeObject(cacheKey);
        // Verify object removed from cache
        retrievedObject = null;
        cacheObject1 = null;
        try
        {
            retrievedObject = this.globalCache.getObject(cacheKey);
            assertNull(
                    retrievedObject,
                    "Retrieved the deleted cached object 1 and did not get expected ObjectExpiredException"
                    );
            assertNotNull(
                    retrievedObject,
                    "Did not get expected ObjectExpiredException retrieving a deleted object"
                    );
        }
        catch (ObjectExpiredException e)
        {
            assertNull(
                    retrievedObject,
                    "Retrieved the deleted cached object 1, but caught expected ObjectExpiredException exception"
                    );
        }
        // Remove object from cache that does NOT exist in the cache
        this.globalCache.removeObject(cacheKey);
    }

    /**
     * Simple test that adds, retrieves, and deletes 2 object.
     *
     * @throws Exception if unable to add and retrieve objects
     */
    @Test
    public void test2ObjectAddGetCachedObject() throws Exception
    {
        String testString = "This is a test";
        Object retrievedObject = null;
        CachedObject<String> cacheObject1 = null;
        CachedObject<String> cacheObject2 = null;
        // Create and add Object #1
        cacheObject1 = new CachedObject<String>(testString);
        assertNotNull(cacheObject1, "Failed to create a cachable object 1" );
        this.globalCache.addObject(cacheKey, cacheObject1);
        retrievedObject = this.globalCache.getObject(cacheKey);
        assertNotNull( retrievedObject, "Did not retrieve a cached object 1");
        assertEquals(cacheObject1,
                retrievedObject, "Did not retrieve correct cached object");
        // Create and add Object #2
        cacheObject2 = new CachedObject<String>(testString);
        assertNotNull(cacheObject2, "Failed to create a cachable object 2");
        this.globalCache.addObject(cacheKey_2, cacheObject2);
        retrievedObject = this.globalCache.getObject(cacheKey_2);
        assertNotNull(retrievedObject, "Did not retrieve a cached object 2");
        assertEquals( cacheObject2,
                retrievedObject,
                "Did not retrieve correct cached object 2");
        // Get object #1
        retrievedObject = this.globalCache.getObject(cacheKey);
        assertNotNull(
                retrievedObject,
                "Did not retrieve a cached object 1. Attempt #2");
        assertEquals(
                cacheObject1, retrievedObject,
                "Did not retrieve correct cached object 1. Attempt #2");
        // Get object #1
        retrievedObject = this.globalCache.getObject(cacheKey);
        assertNotNull(
                retrievedObject,
                "Did not retrieve a cached object 1. Attempt #3");
        assertEquals(
                cacheObject1, retrievedObject,
                "Did not retrieve correct cached object 1. Attempt #3");
        // Get object #2
        retrievedObject = this.globalCache.getObject(cacheKey_2);
        assertNotNull(
                retrievedObject,
                "Did not retrieve a cached object 2. Attempt #2");
        assertEquals(
                cacheObject2, retrievedObject,
                "Did not retrieve correct cached object 2 Attempt #2");
        // Remove objects
        this.globalCache.removeObject(cacheKey);
        this.globalCache.removeObject(cacheKey_2);
    }

    /**
     * Verify that an object will throw the ObjectExpiredException when it now
     * longer exists in cache.
     *
     * @throws Exception if object is not expired
     */
    @Test
    public void testObjectExpiration() throws Exception
    {
        String testString = "This is a test";
        Object retrievedObject = null;
        CachedObject<String> cacheObject = null;
        // Create and add Object that expires in 1000 millis (1 second)
        cacheObject = new CachedObject<String>(testString, 1000);
        assertNotNull(cacheObject, "Failed to create a cachable object");
        long addTime = System.currentTimeMillis();
        this.globalCache.addObject(cacheKey, cacheObject);
        // Try to get un-expired object
        try
        {
            retrievedObject = this.globalCache.getObject(cacheKey);
            assertNotNull( retrievedObject, "Did not retrieve a cached object");
            assertEquals(
                    cacheObject, retrievedObject,
                    "Did not retrieve correct cached object");
        }
        catch (ObjectExpiredException e)
        {
            assertTrue(false, "Object expired early ( "
                    + (System.currentTimeMillis() - addTime) + " millis)");
        }
        // Sleep 1500 Millis (1.5 seconds)
        Thread.sleep(1500);
        // Try to get expired object
        try
        {
            retrievedObject = null;
            retrievedObject = this.globalCache.getObject(cacheKey);
            assertNull(retrievedObject,
                    "Retrieved the expired cached object  and did not get expected ObjectExpiredException"
                    );
            assertNotNull(retrievedObject,
                    "Did not get expected ObjectExpiredException retrieving an expired object"
                    );
        }
        catch (ObjectExpiredException e)
        {
            assertNull(retrievedObject,
                    "Retrieved the expired cached object, but caught expected ObjectExpiredException exception"
                    );
        }
        // Remove objects
        this.globalCache.removeObject(cacheKey);
    }

    /**
     * Verify the all object will be flushed from the cache.
     *
     * This test can take server minutes.
     *
     * @throws Exception if flushing the cache fails
     */
    @Test
    public void testCacheFlush() throws Exception
    {
        String testString = "This is a test";
        CachedObject<String> cacheObject = null;
        // Create and add Object that expires in 1 turbine Refresh + 1 millis
        cacheObject = new CachedObject<String>(testString, (getCacheRefresh() * 5) + 1);
        assertNotNull( cacheObject, "Failed to create a cachable object");
        this.globalCache.addObject(cacheKey, cacheObject);
        // 1 Refresh
        Thread.sleep(getCacheRefresh() + 1);
        assertTrue( (0 < this.globalCache
                .getNumberOfObjects()),
                "No object in cache before flush");
        // Flush Cache
        this.globalCache.flushCache();
        // Wait 15 seconds, 3 Refresh
        Thread.sleep((getCacheRefresh() * 2) + 1);
        assertEquals( 0, this.globalCache.getNumberOfObjects(),
                "After refresh");
        // Remove objects
        this.globalCache.removeObject(cacheKey);
    }

    /**
     * Verify the Cache count is correct.
     *
     * @throws Exception if the cache count does not match expected value
     */
    @Test
    public void testObjectCount() throws Exception
    {
        assertNotNull(this.globalCache, "Could not retrieve cache service.");

        long cacheRefresh = getCacheRefresh();

        // Create and add Object that expires in 1.5 turbine Refresh
        long expireTime = cacheRefresh + cacheRefresh / 2;
        log.info( "set expireTime in ms: {}", expireTime );

        CachedObject<String> cacheObject = new CachedObject<String>("This is a test",
                expireTime);
        assertNotNull( cacheObject, "Failed to create a cachable object");

        this.globalCache.addObject(cacheKey, cacheObject);
        assertEquals( 1, this.globalCache
                .getNumberOfObjects(),
                "After adding 1 Object");

        // Wait until we're passed 1 refresh, but not half way.
        Thread.sleep(cacheRefresh + cacheRefresh / 3);
        assertEquals( 1, this.globalCache
                .getNumberOfObjects(),
                "After one refresh");

        // Wait until we're passed 2 more refreshes
        Thread.sleep((cacheRefresh * 2) + cacheRefresh / 3);
        
        assertEquals(0, this.globalCache
                .getNumberOfObjects(),
                "After three refreshes");
    }

    /**
     * Verify a refreshable object will refreshed in the following cases: o The
     * object is retrieved via getObject an it is stale. o The object is
     * determined to be stale during a cache refresh
     *
     * This test can take several minutes.
     *
     * @throws Exception if object is not a refreshable object
     */
    @Tag("LongRunning")
    @Test
    public void testRefreshableObject() throws Exception
    {
        CachedObject<RefreshableObject> retrievedObject = null;
        RefreshableCachedObject<RefreshableObject> cacheObject = null;
        // Create and add Object that expires in TEST_EXPIRETIME millis.
        cacheObject = new RefreshableCachedObject<RefreshableObject>(new RefreshableObject(),
                getTestExpireTime());
        assertNotNull( cacheObject, "Failed to create a cachable object");
        long addTime = System.currentTimeMillis();
        this.globalCache.addObject(cacheKey, cacheObject);
        // Try to get un-expired object
        try
        {
            retrievedObject = this.globalCache.getObject(cacheKey);
            assertNotNull(retrievedObject, "Did not retrieve a cached object");
            assertEquals(
                    cacheObject, retrievedObject,
                    "Did not retrieve correct cached object");
        }
        catch (ObjectExpiredException e)
        {
            assertTrue(false, "Object expired early ( "
                    + (System.currentTimeMillis() - addTime) + " millis)"
                    );
        }
        // Wait 1 Turbine cache refresh + 1 second.
        Thread.sleep(getTestExpireTime() + 1000);
        // Try to get expired object
        try
        {
            retrievedObject = null;
            retrievedObject = this.globalCache.getObject(cacheKey);
            assertNotNull(
                    retrievedObject, "Did not retrieve a cached object, after sleep");
            assertNotNull(
                    ((RefreshableCachedObject<?>) retrievedObject).getContents(),
                    "Cached object has no contents, after sleep.");
            assertTrue(
                   
                    (((RefreshableCachedObject<RefreshableObject>) retrievedObject)
                            .getContents().getRefreshCount() > 0),
                    "Object did not refresh.");
        }
        catch (ObjectExpiredException e)
        {
            assertTrue(false, "Received unexpected ObjectExpiredException exception "
                    + "when retrieving refreshable object after ( "
                    + (System.currentTimeMillis() - addTime) + " millis)"
                    );
        }
        // See if object will expires (testing every second for 100 seconds. It
        // should not!
        for (int i = 0; i < 100; i++)
        {
            Thread.sleep(1000); // Sleep 0.5 seconds
            // Try to get expired object
            try
            {
                retrievedObject = null;
                retrievedObject = this.globalCache.getObject(cacheKey);
                assertNotNull(retrievedObject,
                        "Did not retrieve a cached object, after sleep");
                assertNotNull(
                        ((RefreshableCachedObject<?>) retrievedObject)
                                .getContents(),
                                "Cached object has no contents, after sleep.");
                assertTrue(
                        
                        (((RefreshableCachedObject<RefreshableObject>) retrievedObject)
                                .getContents().getRefreshCount() > 0),
                        "Object did not refresh.");
            }
            catch (ObjectExpiredException e)
            {
                assertTrue(false,
                        "Received unexpected ObjectExpiredException exception "
                                + "when retrieving refreshable object after ( "
                                + (System.currentTimeMillis() - addTime)
                                + " millis)");
            }
        }
        // Remove objects
        this.globalCache.removeObject(cacheKey);
    }

    /**
     * Verify a cached object will be delete after it has been untouched beyond
     * it's TimeToLive.
     *
     * This test can take several minutes.
     *
     * @throws Exception if object is not deleted
     */
    @Tag("LongRunning")
    @Test
    public void testRefreshableTimeToLive() throws Exception
    {
        CachedObject<RefreshableObject> retrievedObject = null;
        RefreshableCachedObject<RefreshableObject>  cacheObject = null;
        // Create and add Object that expires in TEST_EXPIRETIME millis.
        cacheObject = new RefreshableCachedObject<RefreshableObject>(new RefreshableObject(),
                getTestExpireTime());
        assertNotNull(cacheObject, "Failed to create a cachable object");
        cacheObject.setTTL(getTestExpireTime());
        // Verify TimeToLive was set
        assertEquals(getTestExpireTime(), cacheObject
                .getTTL(),
                "Returned TimeToLive");
        // Add object to Cache
        this.globalCache.addObject(cacheKey, cacheObject);
        long addTime = System.currentTimeMillis();
        // Try to get un-expired object
        try
        {
            retrievedObject = this.globalCache.getObject(cacheKey);
            assertNotNull(retrievedObject, "Did not retrieve a cached object");
            assertEquals(
                    cacheObject, retrievedObject,
                    "Did not retrieve correct cached object");
        }
        catch (ObjectExpiredException e)
        {
            fail("Object expired early ( "
                    + (System.currentTimeMillis() - addTime) + " millis)");
        }
        // Wait long enough to allow object to expire, but do not exceed TTL
        long timeout = getTestExpireTime() - 0000;
        Thread.sleep(timeout);
        // Try to get expired object
        try
        {
            retrievedObject = null;
            retrievedObject = this.globalCache.getObject(cacheKey);
            assertNotNull(retrievedObject,
                    "Did not retrieve a cached object, after sleep");
            assertNotNull(
                    ((RefreshableCachedObject<?>) retrievedObject).getContents(),
                    "Cached object has no contents, after sleep.");
            /*
             * @todo this is not working for some reason
             *
             * assertTrue( "Object did not refresh.", (((RefreshableObject)
             * ((RefreshableCachedObject)
             * retrievedObject).getContents()).getRefreshCount() > 0));
             */
        }
        catch (ObjectExpiredException e)
        {
            assertTrue(false, "Received unexpected ObjectExpiredException exception "
                    + "when retrieving refreshable object after ( "
                    + (System.currentTimeMillis() - addTime) + " millis)");
        }
        // Wait long enough to allow object to expire and exceed TTL
        Thread.sleep(getTestExpireTime() + 5000);
        // Try to get expired object
        try
        {
            retrievedObject = null;
            retrievedObject = this.globalCache.getObject(cacheKey);
            assertNull(retrievedObject, "Retrieved a cached object, after exceeding TimeToLive"
                    );
        }
        catch (ObjectExpiredException e)
        {
            assertNull(retrievedObject,
                    "Retrieved the expired cached object, but caught expected ObjectExpiredException exception");
        }
        // Remove objects
        this.globalCache.removeObject(cacheKey);
    }

    /**
     * Test that we can get a list of the keys in the cache
     *
     */
    @Test
    public void testCacheGetKeyList()
    {
        this.globalCache.flushCache();
        this.globalCache.addObject("date1", new CachedObject<Date>(new Date()));
        this.globalCache.addObject("date2", new CachedObject<Date>(new Date()));
        this.globalCache.addObject("date3", new CachedObject<Date>(new Date()));
        assertTrue(
                (this.globalCache.getKeys() != null),
                "Did not get key list back.");
        List<String> keys = this.globalCache.getKeys();
        for (String key : keys)
        {
            assertTrue(
                    (key instanceof String),
                    "Key was not an instance of String.");
        }

    }

    /**
     * Test that we can get a list of the keys in the cache
     */
    @Test
    public void testCacheGetCachedObjects()
    {
        this.globalCache.flushCache();
        this.globalCache.addObject("date1", new CachedObject<Date>(new Date()));
        this.globalCache.addObject("date2", new CachedObject<Date>(new Date()));
        this.globalCache.addObject("date3", new CachedObject<Date>(new Date()));
        assertTrue((this.globalCache
                .getCachedObjects() != null),
                "Did not get object list back.");
        List<CachedObject<?>> objects = this.globalCache.getCachedObjects();
        for (CachedObject<?> obj : objects)
        {
            assertNotNull(obj, "Object was null.");
            assertTrue(
                    (obj instanceof CachedObject),
                    "Object was not an instance of CachedObject");
        }

    }

    /**
     * Test that the retrieved list is safe from
     * ConcurrentModificationException's being thrown if the cache is updated
     * while we are iterating over the List.
     *
     */
    @Test
    public void testCacheModification()
    {
        this.globalCache.flushCache();
        this.globalCache.addObject("date1", new CachedObject<Date>(new Date()));
        this.globalCache.addObject("date2", new CachedObject<Date>(new Date()));
        this.globalCache.addObject("date3", new CachedObject<Date>(new Date()));
        assertTrue(
                (this.globalCache.getKeys() != null),
                "Did not get key list back.");
        List<String> keys = this.globalCache.getKeys();
        try
        {
            for (@SuppressWarnings("unused") String key : keys)
            {
                this.globalCache.addObject("date4",
                        new CachedObject<Date>(new Date()));
            }
        }
        catch (ConcurrentModificationException cme)
        {
            fail("Caught ConcurrentModificationException adding to cache.");
        }
        List<CachedObject<?>> objects = this.globalCache.getCachedObjects();
        try
        {
            for (@SuppressWarnings("unused") CachedObject<?> obj : objects)
            {
                this.globalCache.addObject("date4",
                        new CachedObject<Date>(new Date()));
            }
        }
        catch (ConcurrentModificationException cme)
        {
            fail("Caught ConcurrentModificationException adding to cache.");
        }
    }

    /**
     * Down cast the interface to the concrete object in order to grab the
     * cache check frequency.
     *
     * @return the refresh frequency in milliseconds
     */
    private long getCacheRefresh()
    {
        try
        {
            DefaultGlobalCacheService cache =
                (DefaultGlobalCacheService)this.lookup(GlobalCacheService.ROLE);
            return cache.getCacheCheckFrequency() * 1000L;
        }
        catch (ComponentException e)
        {
            return 5000;
        }
    }

    /**
     * How long until it expires
     *
     * @return the cache refresh plus 1000.
     */
    private long getTestExpireTime()
    {
        return getCacheRefresh() + 1000;
    }

    /**
     * Simple object that can be refreshed
     */
    class RefreshableObject implements Refreshable
    {
        private int refreshCount = 0;

        /**
         * Increment the refresh counter
         */
        @Override
        public void refresh()
        {
            this.refreshCount++;
        }

        /**
         * Return the number of time this object has been refreshed
         *
         * @return Number of times refresh() has been called
         */
        public int getRefreshCount()
        {
            return this.refreshCount;
        }
    }
}
