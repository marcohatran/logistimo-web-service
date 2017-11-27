package com.logistimo.mock;
/*
 * Copyright © 2017 Logistimo.
 *
 * This file is part of Logistimo.
 *
 * Logistimo software is a mobile & web platform for supply chain management and remote temperature monitoring in
 * low-resource settings, made available under the terms of the GNU Affero General Public License (AGPL).
 *
 * This program is free software: you can redistribute it and/or modify it under the terms of the GNU Affero General
 * Public License as published by the Free Software Foundation, either version 3 of the License, or (at your option) any
 * later version.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied
 * warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU Affero General Public License
 * for more details.
 *
 * You should have received a copy of the GNU Affero General Public License along with this program.  If not, see
 * <http://www.gnu.org/licenses/>.
 *
 * You can be released from the requirements of the license by purchasing a commercial license. To know more about
 * the commercial license, please contact us at opensource@logistimo.com
 */

import com.logistimo.AppFactory;
import com.logistimo.auth.service.AuthenticationService;
import com.logistimo.auth.service.AuthorizationService;
import com.logistimo.dao.DaoException;
import com.logistimo.dao.IDaoUtil;
import com.logistimo.logger.ILogger;
import com.logistimo.models.ICounter;
import com.logistimo.reports.dao.IReportsDao;
import com.logistimo.services.IBackendService;
import com.logistimo.services.blobstore.BlobstoreService;
import com.logistimo.services.cache.MemcacheService;
import com.logistimo.services.http.URLFetchService;
import com.logistimo.services.mapred.IMapredService;
import com.logistimo.services.storage.StorageUtil;
import com.logistimo.services.taskqueue.ITaskService;

import static org.mockito.Mockito.mock;

/**
 * Created by charan on 14/11/17.
 */
public class TestLogiAppFactory extends AppFactory {
  @Override
  public ILogger getLogger(String name) {
    return null;
  }

  @Override
  public ITaskService getTaskService() {
    return null;
  }

  @Override
  public StorageUtil getStorageUtil() {
    return null;
  }

  @Override
  public IMapredService getMapredService() {
    return null;
  }

  @Override
  public MemcacheService getMemcacheService() {
    return mock(MemcacheService.class);
  }

  @Override
  public BlobstoreService getBlobstoreService() {
    return null;
  }

  @Override
  public URLFetchService getURLFetchService() {
    return null;
  }

  @Override
  public IBackendService getBackendService() {
    return null;
  }

  @Override
  public ICounter getCounter() {
    return null;
  }

  @Override
  public AuthorizationService getAuthorizationService() {
    return null;
  }

  @Override
  public AuthenticationService getAuthenticationService() {
    return null;
  }

  @Override
  public IDaoUtil getDaoUtil() throws DaoException {
    return null;
  }

  @Override
  public IReportsDao getReportsDao() throws DaoException {
    return null;
  }
}
