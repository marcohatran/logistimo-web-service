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

package com.logistimo.api.builders;

import com.logistimo.api.models.BulletinBoardDashBoardModel;
import com.logistimo.api.models.BulletinBoardModel;
import com.logistimo.bulletinboard.entity.IBulletinBoard;
import com.logistimo.dao.JDOUtils;
import com.logistimo.dashboards.entity.IDashboard;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;

/**
 * @author Mohan Raja
 */
public class BulletinBoardDashBoardBuilder {


  public IDashboard buildDashboard(BulletinBoardDashBoardModel model, Long domainId, String userName) {
    IDashboard db = JDOUtils.createInstance(IDashboard.class);
    db.setDomainId(domainId);
    db.setCreatedOn(new Date());
    db.setCreatedBy(userName);
    db.setName(model.getName());
    db.setDescription(model.getDescription());
    db.setConfiguration(model.getWidgets());
    db.setDashboardId(model.getDashboardId());
    db.setTitle(model.getTitle());
    db.setInfo(model.getInfo());
    return db;
  }

  public IDashboard buildDashboardForUpdate(BulletinBoardDashBoardModel model, IDashboard dashboard, String userName) {
    dashboard.setConfiguration(model.getWidgets());
    dashboard.setName(model.getName());
    dashboard.setDescription(model.getDescription());
    dashboard.setTitle(model.getTitle());
    dashboard.setInfo(model.getInfo());
    dashboard.setUpdatedOn(new Date());
    dashboard.setUpdatedBy(userName);
    return dashboard;
  }

  public List<BulletinBoardDashBoardModel> buildDashboardModelList(Collection<IDashboard> dbList) {
    List<BulletinBoardDashBoardModel> models = new ArrayList<>(dbList.size());
    for (IDashboard iDashboard : dbList) {
      models.add(buildDashboardModel(iDashboard));
    }
    return models;
  }

  public BulletinBoardDashBoardModel buildDashboardModel(IDashboard db) {
    BulletinBoardDashBoardModel model = new BulletinBoardDashBoardModel();
    model.setDashboardId(db.getDashboardId());
    model.setName(db.getName());
    model.setDomainId(db.getDomainId());
    model.setDescription(db.getDescription());
    model.setTitle(db.getTitle());
    model.setCreatedBy(db.getCreatedBy());
    model.setCreatedOn(db.getCreatedOn());
    model.setWidgets(db.getConfiguration());
    model.setUpdatedBy(db.getUpdatedBy());
    model.setUpdatedOn(db.getUpdatedOn());
    return model;
  }

  public IBulletinBoard buildBulletinBoard(BulletinBoardModel model, Long domainId, String userName) {
    IBulletinBoard bulletinBoard = JDOUtils.createInstance(IBulletinBoard.class);
    bulletinBoard.setConfiguration(model.getDashboards());
    bulletinBoard.setDomainId(domainId);
    bulletinBoard.setName(model.getName());
    bulletinBoard.setDescription(model.getDescription());
    bulletinBoard.setCreatedBy(userName);
    bulletinBoard.setCreatedOn(new Date());
    bulletinBoard.setMinScrollTime(model.getMinScrollTime());
    bulletinBoard.setMaxScrollTime(model.getMaxScrollTime());
    return bulletinBoard;
  }

  public IBulletinBoard buildBulletinBoardForUpdate(BulletinBoardModel model, IBulletinBoard bulletinBoard, String userName) {
    bulletinBoard.setConfiguration(model.getDashboards());
    bulletinBoard.setName(model.getName());
    bulletinBoard.setDescription(model.getDescription());
    bulletinBoard.setMinScrollTime(model.getMinScrollTime());
    bulletinBoard.setMaxScrollTime(model.getMaxScrollTime());
    bulletinBoard.setUpdatedBy(userName);
    bulletinBoard.setUpdatedOn(new Date());
    return bulletinBoard;
  }

  public BulletinBoardModel buildBulletinBoardModel(IBulletinBoard bulletinBoard) {
    BulletinBoardModel model = new BulletinBoardModel();
    model.setBulletinBoardId(bulletinBoard.getBulletinBoardId());
    model.setName(bulletinBoard.getName());
    model.setDescription(bulletinBoard.getDescription());
    model.setDashboards(bulletinBoard.getConfiguration());
    model.setMinScrollTime(bulletinBoard.getMinScrollTime());
    model.setMaxScrollTime(bulletinBoard.getMaxScrollTime());
    model.setCreatedBy(bulletinBoard.getCreatedBy());
    model.setCreatedOn(bulletinBoard.getCreatedOn());
    model.setUpdatedBy(bulletinBoard.getUpdatedBy());
    model.setUpdatedOn(bulletinBoard.getUpdatedOn());
    return model;
  }

  public List<BulletinBoardModel> buildBulletinBoardModelList(Collection<IBulletinBoard> bulletinBoardList) {
    List<BulletinBoardModel> models = new ArrayList<>(bulletinBoardList.size());
    models.addAll(
        bulletinBoardList.stream().map(this::buildBulletinBoardModel).collect(Collectors.toList()));
    return models;
  }
}
