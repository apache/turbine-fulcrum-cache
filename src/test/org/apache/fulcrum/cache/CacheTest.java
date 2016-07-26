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

// Cactus and Junit imports

import java.util.ConcurrentModificationException;
import java.util.Date;
import java.util.List;

import org.apache.avalon.framework.component.ComponentException;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.fulcrum.cache.impl.DefaultGlobalCacheService;
import org.apache.fulcrum.testcontainer.BaseUnitTest;

/**
 * CacheTest
 *
 * @author <a href="paulsp@apache.org">Paul Spencer</a>
 * @author <a href="epugh@upstate.com">Eric Pugh</a>
 * @author <a href="mailto:peter@courcoux.biz">Peter Courcoux</a>
 * @version $Id$
 */
public class CacheTest extends BaseUnitTest
{

    protected GlobalCacheService globalCache = null;

    protected static final String cacheKey = "CacheKey";

    protected static final String cacheKey_2 = "CacheKey_2";

    public static final String SKIP_TESTS_KEY = "fulcrum.cache.skip.long.tests";

    protected static final Log LOG = LogFactory.getLog(CacheTest.class);

    /**
     * Defines the testcase name for JUnit.
     *
     * @param name
     *            the testcase's name.
     */
    public CacheTest(String name)
    {
        super(name);
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

    @Override
    protected void setUp() throws Exception
    {
        super.setUp();

        try
        {
            this.globalCache = (GlobalCacheService) this
                    .lookup(getCacheRoleName());
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
        String testString = "This is a test";
        Object retrievedObject = null;
        CachedObject<String> cacheObject1 = null;
        // Create object
        cacheObject1 = new CachedObject<String>(testString);
        assertNotNull("Failed to create a cachable object 1", cacheObject1);
        // Add object to cache
        this.globalCache.addObject(cacheKey, cacheObject1);
        // Get object from cache
        retrievedObject = this.globalCache.getObject(cacheKey);
        assertNotNull("Did not retrieve a cached object 1", retrievedObject);
        assertSame("Did not retrieve a correct, expected cached object 1",
                retrievedObject, cacheObject1);
        // Remove object from cache
        this.globalCache.removeObject(cacheKey);
        // Verify object removed from cache
        retrievedObject = null;
        cacheObject1 = null;
        try
        {
            retrievedObject = this.globalCache.getObject(cacheKey);
            assertNull(
                    "Retrieved the deleted cached object 1 and did not get expected ObjectExpiredException",
                    retrievedObject);
            assertNotNull(
                    "Did not get expected ObjectExpiredException retrieving a deleted object",
                    retrievedObject);
        }
        catch (ObjectExpiredException e)
        {
            assertNull(
                    "Retrieved the deleted cached object 1, but caught expected ObjectExpiredException exception",
                    retrievedObject);
        }
        // Remove object from cache that does NOT exist in the cache
        this.globalCache.removeObject(cacheKey);
    }

    /**
     * Simple test that adds, retrieves, and deletes 2 object.
     *
     * @throws Exception
     */
    public void test2ObjectAddGetCachedObject() throws Exception
    {
        String testString = "This is a test";
        Object retrievedObject = null;
        CachedObject<String> cacheObject1 = null;
        CachedObject<String> cacheObject2 = null;
        // Create and add Object #1
        cacheObject1 = new CachedObject<String>(testString);
        assertNotNull("Failed to create a cachable object 1", cacheObject1);
        this.globalCache.addObject(cacheKey, cacheObject1);
        retrievedObject = this.globalCache.getObject(cacheKey);
        assertNotNull("Did not retrieve a cached object 1", retrievedObject);
        assertEquals("Did not retrieve correct cached object", cacheObject1,
                retrievedObject);
        // Create and add Object #2
        cacheObject2 = new CachedObject<String>(testString);
        assertNotNull("Failed to create a cachable object 2", cacheObject2);
        this.globalCache.addObject(cacheKey_2, cacheObject2);
        retrievedObject = this.globalCache.getObject(cacheKey_2);
        assertNotNull("Did not retrieve a cached object 2", retrievedObject);
        assertEquals("Did not retrieve correct cached object 2", cacheObject2,
                retrievedObject);
        // Get object #1
        retrievedObject = this.globalCache.getObject(cacheKey);
        assertNotNull("Did not retrieve a cached object 1. Attempt #2",
                retrievedObject);
        assertEquals("Did not retrieve correct cached object 1. Attempt #2",
                cacheObject1, retrievedObject);
        // Get object #1
        retrievedObject = this.globalCache.getObject(cacheKey);
        assertNotNull("Did not retrieve a cached object 1. Attempt #3",
                retrievedObject);
        assertEquals("Did not retrieve correct cached object 1. Attempt #3",
                cacheObject1, retrievedObject);
        // Get object #2
        retrievedObject = this.globalCache.getObject(cacheKey_2);
        assertNotNull("Did not retrieve a cached object 2. Attempt #2",
                retrievedObject);
        assertEquals("Did not retrieve correct cached object 2 Attempt #2",
                cacheObject2, retrievedObject);
        // Remove objects
        this.globalCache.removeObject(cacheKey);
        this.globalCache.removeObject(cacheKey_2);
    }

    /**
     * Verify that an object will throw the ObjectExpiredException when it now
     * longer exists in cache.
     *
     * @throws Exception
     */
    public void testObjectExpiration() throws Exception
    {
        String testString = "This is a test";
        Object retrievedObject = null;
        CachedObject<String> cacheObject = null;
        // Create and add Object that expires in 1000 millis (1 second)
        cacheObject = new CachedObject<String>(testString, 1000);
        assertNotNull("Failed to create a cachable object", cacheObject);
        long addTime = System.currentTimeMillis();
        this.globalCache.addObject(cacheKey, cacheObject);
        // Try to get un-expired object
        try
        {
            retrievedObject = this.globalCache.getObject(cacheKey);
            assertNotNull("Did not retrieve a cached object", retrievedObject);
            assertEquals("Did not retrieve correct cached object",
                    cacheObject, retrievedObject);
        }
        catch (ObjectExpiredException e)
        {
            assertTrue("Object expired early ( "
                    + (System.currentTimeMillis() - addTime) + " millis)",
                    false);
        }
        // Sleep 1500 Millis (1.5 seconds)
        Thread.sleep(1500);
        // Try to get expired object
        try
        {
            retrievedObject = null;
            retrievedObject = this.globalCache.getObject(cacheKey);
            assertNull(
                    "Retrieved the expired cached object  and did not get expected ObjectExpiredException",
                    retrievedObject);
            assertNotNull(
                    "Did not get expected ObjectExpiredException retrieving an expired object",
                    retrievedObject);
        }
        catch (ObjectExpiredException e)
        {
            assertNull(
                    "Retrieved the expired cached object, but caught expected ObjectExpiredException exception",
                    retrievedObject);
        }
        // Remove objects
        this.globalCache.removeObject(cacheKey);
    }

    /**
     * Verify the all object will be flushed from the cache.
     *
     * This test can take server minutes.
     *
     * @throws Exception
     */
    public void testCacheFlush() throws Exception
    {
        String testString = "This is a test";
        CachedObject<String> cacheObject = null;
        // Create and add Object that expires in 1 turbine Refresh + 1 millis
        cacheObject = new CachedObject<String>(testString, (getCacheRefresh() * 5) + 1);
        assertNotNull("Failed to create a cachable object", cacheObject);
        this.globalCache.addObject(cacheKey, cacheObject);
        // 1 Refresh
        Thread.sleep(getCacheRefresh() + 1);
        assertTrue("No object in cache before flush", (0 < this.globalCache
                .getNumberOfObjects()));
        // Flush Cache
        this.globalCache.flushCache();
        // Wait 15 seconds, 3 Refresh
        Thread.sleep((getCacheRefresh() * 2) + 1);
        assertEquals("After refresh", 0, this.globalCache.getNumberOfObjects());
        // Remove objects
        this.globalCache.removeObject(cacheKey);
    }

    /**
     * Verify the Cache count is correct.
     *
     * @throws Exception
     */
    public void testObjectCount() throws Exception
    {
        assertNotNull("Could not retrieve cache service.", this.globalCache);

        long cacheRefresh = getCacheRefresh();

        // Create and add Object that expires in 1.5 turbine Refresh
        long expireTime = cacheRefresh + cacheRefresh / 2;

        CachedObject<String> cacheObject = new CachedObject<String>("This is a test",
                expireTime);
        assertNotNull("Failed to create a cachable object", cacheObject);

        this.globalCache.addObject(cacheKey, cacheObject);
        assertEquals("After adding 1 Object", 1, this.globalCache
                .getNumberOfObjects());

        // Wait until we're passed 1 refresh, but not half way.
        Thread.sleep(cacheRefresh + cacheRefresh / 3);
        assertEquals("After one refresh", 1, this.globalCache
                .getNumberOfObjects());

        // Wait until we're passed 2 more refreshes
        Thread.sleep((cacheRefresh * 2) + cacheRefresh / 3);
        assertEquals("After three refreshes", 0, this.globalCache
                .getNumberOfObjects());
    }

    /**
     * Verify a refreshable object will refreshed in the following cases: o The
     * object is retrieved via getObject an it is stale. o The object is
     * determined to be stale during a cache refresh
     *
     * This test can take several minutes.
     *
     * @throws Exception
     */
    public void testRefreshableObject() throws Exception
    {
        CachedObject<RefreshableObject> retrievedObject = null;
        RefreshableCachedObject<RefreshableObject> cacheObject = null;
        // Create and add Object that expires in TEST_EXPIRETIME millis.
        cacheObject = new RefreshableCachedObject<RefreshableObject>(new RefreshableObject(),
                getTestExpireTime());
        assertNotNull("Failed to create a cachable object", cacheObject);
        long addTime = System.currentTimeMillis();
        this.globalCache.addObject(cacheKey, cacheObject);
        // Try to get un-expired object
        try
        {
            retrievedObject = this.globalCache.getObject(cacheKey);
            assertNotNull("Did not retrieve a cached object", retrievedObject);
            assertEquals("Did not retrieve correct cached object",
                    cacheObject, retrievedObject);
        }
        catch (ObjectExpiredException e)
        {
            assertTrue("Object expired early ( "
                    + (System.currentTimeMillis() - addTime) + " millis)",
                    false);
        }
        // Wait 1 Turbine cache refresh + 1 second.
        Thread.sleep(getTestExpireTime() + 1000);
        // Try to get expired object
        try
        {
            retrievedObject = null;
            retrievedObject = this.globalCache.getObject(cacheKey);
            assertNotNull("Did not retrieve a cached object, after sleep",
                    retrievedObject);
            assertNotNull("Cached object has no contents, after sleep.",
                    ((RefreshableCachedObject<?>) retrievedObject).getContents());
            assertTrue(
                    "Object did not refresh.",
                    (((RefreshableCachedObject<RefreshableObject>) retrievedObject)
                            .getContents().getRefreshCount() > 0));
        }
        catch (ObjectExpiredException e)
        {
            assertTrue("Received unexpected ObjectExpiredException exception "
                    + "when retrieving refreshable object after ( "
                    + (System.currentTimeMillis() - addTime) + " millis)",
                    false);
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
                assertNotNull("Did not retrieve a cached object, after sleep",
                        retrievedObject);
                assertNotNull("Cached object has no contents, after sleep.",
                        ((RefreshableCachedObject<?>) retrievedObject)
                                .getContents());
                assertTrue(
                        "Object did not refresh.",
                        (((RefreshableCachedObject<RefreshableObject>) retrievedObject)
                                .getContents().getRefreshCount() > 0));
            }
            catch (ObjectExpiredException e)
            {
                assertTrue(
                        "Received unexpected ObjectExpiredException exception "
                                + "when retrieving refreshable object after ( "
                                + (System.currentTimeMillis() - addTime)
                                + " millis)", false);
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
     * @throws Exception
     */
    public void testRefreshableTimeToLive() throws Exception
    {
        String skipTestsProperty = System.getProperty(SKIP_TESTS_KEY, "false");
        LOG.info("What is the value of the skipTestsProperty:"
                + skipTestsProperty);
        if (Boolean.valueOf(skipTestsProperty).booleanValue())
        {
            LOG.warn("Skipping testRefreshableTimeToLive test due to property "
                    + SKIP_TESTS_KEY + " being true.");
            return;
        }
        else
        {
            LOG.warn("Running testRefreshableTimeToLive test due to property "
                    + SKIP_TESTS_KEY + " being false.");
        }

        CachedObject<RefreshableObject> retrievedObject = null;
        RefreshableCachedObject<RefreshableObject>  cacheObject = null;
        // Create and add Object that expires in TEST_EXPIRETIME millis.
        cacheObject = new RefreshableCachedObject<RefreshableObject>(new RefreshableObject(),
                getTestExpireTime());
        assertNotNull("Failed to create a cachable object", cacheObject);
        cacheObject.setTTL(getTestExpireTime());
        // Verify TimeToLive was set
        assertEquals("Returned TimeToLive", getTestExpireTime(), cacheObject
                .getTTL());
        // Add object to Cache
        this.globalCache.addObject(cacheKey, cacheObject);
        long addTime = System.currentTimeMillis();
        // Try to get un-expired object
        try
        {
            retrievedObject = this.globalCache.getObject(cacheKey);
            assertNotNull("Did not retrieve a cached object", retrievedObject);
            assertEquals("Did not retrieve correct cached object",
                    cacheObject, retrievedObject);
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
            assertNotNull("Did not retrieve a cached object, after sleep",
                    retrievedObject);
            assertNotNull("Cached object has no contents, after sleep.",
                    ((RefreshableCachedObject<?>) retrievedObject).getContents());
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
            assertTrue("Received unexpected ObjectExpiredException exception "
                    + "when retrieving refreshable object after ( "
                    + (System.currentTimeMillis() - addTime) + " millis)",
                    false);
        }
        // Wait long enough to allow object to expire and exceed TTL
        Thread.sleep(getTestExpireTime() + 5000);
        // Try to get expired object
        try
        {
            retrievedObject = null;
            retrievedObject = this.globalCache.getObject(cacheKey);
            assertNull("Retrieved a cached object, after exceeding TimeToLive",
                    retrievedObject);
        }
        catch (ObjectExpiredException e)
        {
            assertNull(
                    "Retrieved the expired cached object, but caught expected ObjectExpiredException exception",
                    retrievedObject);
        }
    }

    /**
     * Test that we can get a list of the keys in the cache
     *
     * @return
     */
    public void testCacheGetKeyList()
    {
        this.globalCache.flushCache();
        this.globalCache.addObject("date1", new CachedObject<Date>(new Date()));
        this.globalCache.addObject("date2", new CachedObject<Date>(new Date()));
        this.globalCache.addObject("date3", new CachedObject<Date>(new Date()));
        assertTrue("Did not get key list back.",
                (this.globalCache.getKeys() != null));
        List<String> keys = this.globalCache.getKeys();
        for (String key : keys)
        {
            assertTrue("Key was not an instance of String.",
                    (key instanceof String));
        }

    }

    /**
     * Test that we can get a list of the keys in the cache
     *
     * @return
     */
    public void testCacheGetCachedObjects()
    {
        this.globalCache.flushCache();
        this.globalCache.addObject("date1", new CachedObject<Date>(new Date()));
        this.globalCache.addObject("date2", new CachedObject<Date>(new Date()));
        this.globalCache.addObject("date3", new CachedObject<Date>(new Date()));
        assertTrue("Did not get object list back.", (this.globalCache
                .getCachedObjects() != null));
        List<CachedObject<?>> objects = this.globalCache.getCachedObjects();
        for (CachedObject<?> obj : objects)
        {
            assertNotNull("Object was null.", obj);
            assertTrue("Object was not an instance of CachedObject",
                    (obj instanceof CachedObject));
        }

    }

    /**
     * Test that the retrieved list is safe from
     * ConcurrentModificationException's being thrown if the cache is updated
     * while we are iterating over the List.
     *
     * @return
     */
    public void testCacheModification()
    {
        this.globalCache.flushCache();
        this.globalCache.addObject("date1", new CachedObject<Date>(new Date()));
        this.globalCache.addObject("date2", new CachedObject<Date>(new Date()));
        this.globalCache.addObject("date3", new CachedObject<Date>(new Date()));
        assertTrue("Did not get key list back.",
                (this.globalCache.getKeys() != null));
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
