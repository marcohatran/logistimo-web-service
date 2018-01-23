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

package com.logistimo.bulletinboard.service.impl;

import com.logistimo.bulletinboard.entity.IBulletinBoard;
import com.logistimo.bulletinboard.service.IBulletinBoardService;
import com.logistimo.dao.JDOUtils;
import com.logistimo.logger.XLog;
import com.logistimo.services.ServiceException;
import com.logistimo.services.impl.PMF;

import org.springframework.stereotype.Service;

import java.util.Date;
import java.util.List;

import javax.jdo.PersistenceManager;
import javax.jdo.Query;

/**
 * Created by naveensnair on 14/11/17.
 */

@Service
public class BulletinBoardService implements IBulletinBoardService {

  private static final XLog xLogger = XLog.getLog(IBulletinBoardService.class);

  @Override
  public void createBulletinBoard(IBulletinBoard bulletinBoard) throws ServiceException {
    try {
      create(bulletinBoard);
    } catch (Exception e) {
      xLogger.severe("Error in creating Dashboard:", e);
      throw new ServiceException("Error in creating Dashboard:", e);
    }
  }

  @Override
  public List<IBulletinBoard> getBulletinBoards(Long domainId) {
    return getAll(domainId, IBulletinBoard.class);
  }

  @Override
  public IBulletinBoard getBulletinBoard(Long bbId) throws ServiceException {
    try {
      return get(bbId, IBulletinBoard.class);
    } catch (Exception e) {
      xLogger.severe("Error in fetching bulletinBoard:", bbId, e);
      throw new ServiceException("Error in fetching bulletinBoard" + bbId, e);
    }
  }

  @Override
  public String deleteBulletinBoard(Long bulletinBoardId) throws ServiceException {
    PersistenceManager pm = PMF.get().getPersistenceManager();
    try {
      IBulletinBoard
          bulletinBoard =
          JDOUtils.getObjectById(IBulletinBoard.class, bulletinBoardId, pm);
      String name = bulletinBoard.getName();
      pm.deletePersistent(bulletinBoard);
      return name;
    } catch (Exception e) {
      xLogger.severe("Error in deleting BulletinBoard:", e);
      throw new ServiceException("Error while deleting BulletinBoard" + bulletinBoardId, e);
    } finally {
      pm.close();
    }
  }

  @Override
  public void updateBulletinBoard(IBulletinBoard bulletinBoard) throws ServiceException {
    PersistenceManager pm = PMF.get().getPersistenceManager();
    try {
      IBulletinBoard
          bulletinBoardObject =
          JDOUtils.getObjectById(IBulletinBoard.class, bulletinBoard.getBulletinBoardId(), pm);
      bulletinBoardObject.setConfiguration(bulletinBoard.getConfiguration());
      bulletinBoardObject.setDescription(bulletinBoard.getDescription());
      bulletinBoardObject.setMinScrollTime(bulletinBoard.getMinScrollTime());
      bulletinBoardObject.setMaxScrollTime(bulletinBoard.getMaxScrollTime());
      bulletinBoardObject.setName(bulletinBoard.getName());
      bulletinBoardObject.setUpdatedBy(bulletinBoard.getUpdatedBy());
      bulletinBoardObject.setUpdatedOn(new Date());
      pm.makePersistent(bulletinBoardObject);
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

  private <T> List<T> getAll(Long domainId, Class<T> clz) {
    List<T> o = null;
    PersistenceManager pm = PMF.get().getPersistenceManager();
    Query q = pm.newQuery(JDOUtils.getImplClass(clz));
    String declaration = " Long dIdParam";
    q.setFilter("dId == dIdParam");
    q.declareParameters(declaration);
    try {
      o = (List<T>) q.execute(domainId);
      if (o != null) {
        o.size();
        o = (List<T>) pm.detachCopyAll(o);
      }
    } finally {
      try {
        q.closeAll();
      } catch (Exception ignored) {
        xLogger.warn("Exception while closing query", ignored);
      }
      pm.close();
    }
    return o;
  }

  private <T> T get(Long id, Class<T> clz) {
    PersistenceManager pm = PMF.get().getPersistenceManager();
    try {
      return JDOUtils.getObjectById(clz, id, pm);
    } finally {
      pm.close();
    }
  }
}
