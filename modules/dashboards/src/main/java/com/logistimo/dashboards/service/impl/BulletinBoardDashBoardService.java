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

package com.logistimo.dashboards.service.impl;

import com.logistimo.dao.JDOUtils;
import com.logistimo.dashboards.entity.IDashboard;
import com.logistimo.dashboards.service.IBulletinBoardDashBoardService;
import com.logistimo.logger.XLog;
import com.logistimo.services.ServiceException;
import com.logistimo.services.impl.PMF;

import org.springframework.stereotype.Service;

import java.util.List;

import javax.jdo.PersistenceManager;

/**
 * Created by naveensnair on 13/11/17.
 */
@Service
public class BulletinBoardDashBoardService implements IBulletinBoardDashBoardService {

  private static final XLog xLogger = XLog.getLog(IBulletinBoardDashBoardService.class);

  @Override
  public void createDashboard(IDashboard ds) throws ServiceException {
    try {
      create(ds);
    } catch (Exception e) {
      xLogger.severe("Error in creating Dashboard:", e);
      throw new ServiceException("Error in creating Dashboard:", e);
    }
  }

  @Override
  public void updateDashboard(IDashboard db) throws ServiceException {
    PersistenceManager pm = PMF.get().getPersistenceManager();
    try {
      IDashboard dashboard = JDOUtils.getObjectById(IDashboard.class, db.getDashboardId(), pm);
      dashboard.setDescription(db.getDescription());
      dashboard.setInfo(db.getInfo());
      dashboard.setConfiguration(db.getConfiguration());
      dashboard.setTitle(db.getTitle());
      dashboard.setUpdatedBy(db.getUpdatedBy());
      dashboard.setUpdatedOn(db.getUpdatedOn());
      dashboard.setName(db.getName());
      pm.makePersistent(dashboard);
    } catch (Exception e) {
      xLogger.severe("Error in updating Dashboard:", e);
      throw new ServiceException("Error in updating Dashboard:", e);
    } finally {
      pm.close();
    }
  }


  private void create(Object o) throws ServiceException {
    PersistenceManager pm = PMF.get().getPersistenceManager();
    try {
      pm.makePersistent(o);
    } catch (Exception e) {
      throw new ServiceException(e);
    } finally {
      pm.close();
    }
  }

  @Override
  public List<IDashboard> getDashBoards(Long domainId) {
    return getAll(domainId, IDashboard.class);
  }

  @SuppressWarnings("unchecked")
  private <T> List<T> getAll(Long domainId, Class<T> clz) {
    return JDOUtils.getAll(domainId, clz);
  }

  @Override
  public String deleteDashboard(Long id) throws ServiceException {
    PersistenceManager pm = PMF.get().getPersistenceManager();
    try {
      IDashboard db = JDOUtils.getObjectById(IDashboard.class, id, pm);
      String name = db.getName();
      pm.deletePersistent(db);
      return name;
    } catch (Exception e) {
      xLogger.severe("Error in deleting Dashboard:", e);
      throw new ServiceException("Error while deleting Dashboard" + id, e);
    } finally {
      pm.close();
    }
  }

  @Override
  public IDashboard getDashBoard(Long dbId) throws ServiceException {
    try {
      return JDOUtils.get(dbId, IDashboard.class);
    } catch (Exception e) {
      xLogger.severe("Error in fetching Widget:", dbId, e);
      throw new ServiceException("Error in fetching Widget" + dbId, e);
    }
  }


}
