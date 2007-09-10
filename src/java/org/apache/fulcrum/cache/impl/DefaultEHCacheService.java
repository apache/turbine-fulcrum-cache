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

import net.sf.ehcache.Cache;
import net.sf.ehcache.CacheManager;

import org.apache.avalon.framework.activity.Disposable;
import org.apache.avalon.framework.activity.Initializable;
import org.apache.avalon.framework.service.ServiceException;
import org.apache.avalon.framework.service.ServiceManager;
import org.apache.avalon.framework.service.Serviceable;
import org.apache.avalon.framework.thread.ThreadSafe;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.fulcrum.cache.EHCacheService;

/**
 * Default implementation of EHCacheService
 *
 * @author <a href="mailto:epughNOSPAM@opensourceconnections.com">Eric Pugh </a>
 *
 */
public class DefaultEHCacheService implements EHCacheService, Serviceable, Disposable, Initializable,
        ThreadSafe {

    protected Log logger = LogFactory.getLog(DefaultEHCacheService.class.getName());

    private CacheManager cacheManager;

    public CacheManager getCacheManager(){
    		return cacheManager;
    }

    public Cache getCache(String cacheName){
    		return cacheManager.getCache(cacheName);
    }


    /**
     * @see org.apache.avalon.framework.component.Composable#compose(org.apache.avalon.framework.component.ComponentManager)
     */
    public void service(ServiceManager manager) throws ServiceException {


    }

    /**
     * @see org.apache.avalon.framework.activity.Initializable#initialize()
     */
    public void initialize() throws Exception {

        cacheManager = CacheManager.create();
        logger.debug("EHCacheService started!");
    }

    /**
     * @see org.apache.avalon.framework.activity.Disposable#dispose()
     */
    public void dispose() {
    		cacheManager.shutdown();
    		cacheManager = null;
    	    logger.debug("EHCacheService stopped!");

    }


}
