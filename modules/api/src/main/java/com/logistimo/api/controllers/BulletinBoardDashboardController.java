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

package com.logistimo.api.controllers;

import com.logistimo.api.builders.BulletinBoardDashBoardBuilder;
import com.logistimo.api.models.BulletinBoardDashBoardModel;
import com.logistimo.auth.utils.SecurityUtils;
import com.logistimo.dashboards.entity.IDashboard;
import com.logistimo.dashboards.service.IBulletinBoardDashBoardService;
import com.logistimo.exception.InvalidServiceException;
import com.logistimo.logger.XLog;
import com.logistimo.security.SecureUserDetails;
import com.logistimo.services.Resources;
import com.logistimo.services.ServiceException;
import com.logistimo.utils.MsgUtil;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;

import java.util.List;
import java.util.ResourceBundle;


/**
 * @author Mohan Raja
 */
@Controller
@RequestMapping("/dashboards")
public class BulletinBoardDashboardController {
  private static final XLog xLogger = XLog.getLog(DashboardController.class);

  private BulletinBoardDashBoardBuilder bulletinBoardDashboardBuilder;

  private IBulletinBoardDashBoardService bulletinBoardDashBoardService;

  @Autowired
  public void setBulletinBoardDashboardBuilder(
      BulletinBoardDashBoardBuilder bulletinBoardDashboardBuilder) {
    this.bulletinBoardDashboardBuilder = bulletinBoardDashboardBuilder;
  }

  @Autowired
  public void setBulletinBoardDashBoardService(
      IBulletinBoardDashBoardService bulletinBoardDashBoardService) {
    this.bulletinBoardDashBoardService = bulletinBoardDashBoardService;
  }

  @RequestMapping(value = "/", method = RequestMethod.POST)
  public
  @ResponseBody
  String create(@RequestBody BulletinBoardDashBoardModel model) {
    SecureUserDetails sUser = SecurityUtils.getUserDetails();
    ResourceBundle backendMessages = Resources.get().getBundle("BackendMessages", sUser.getLocale());
    long domainId = SecurityUtils.getDomainId();
    try {
      if (model.getDashboardId() != null) {
        IDashboard db = bulletinBoardDashBoardService.getDashBoard(model.getDashboardId());
        db = bulletinBoardDashboardBuilder.buildDashboardForUpdate(model, db, sUser.getUsername());
        bulletinBoardDashBoardService.updateDashboard(db);
      } else {
        IDashboard db = bulletinBoardDashboardBuilder.buildDashboard(model, domainId, sUser.getUsername());
        bulletinBoardDashBoardService.createDashboard(db);
      }
    } catch (ServiceException e) {
      xLogger.severe("Error creating Dashboard for domain ", domainId);
      throw new InvalidServiceException("Error creating Dashboard for " + domainId);
    }
    return backendMessages.getString("dashboard.upper") + " " + MsgUtil.bold(model.getName()) + " "
        + backendMessages
        .getString("created.success");
  }

  @RequestMapping(value = "/all", method = RequestMethod.GET)
  public
  @ResponseBody
  List<BulletinBoardDashBoardModel> getAll() {
    List<IDashboard> dbList = bulletinBoardDashBoardService.getDashBoards(SecurityUtils.getCurrentDomainId());
    return bulletinBoardDashboardBuilder.buildDashboardModelList(dbList);
  }

  @RequestMapping(value = "/{dbId}", method = RequestMethod.GET)
  public
  @ResponseBody
  BulletinBoardDashBoardModel get(@PathVariable Long dbId) {
    try {
      IDashboard db = bulletinBoardDashBoardService.getDashBoard(dbId);
      return bulletinBoardDashboardBuilder.buildDashboardModel(db);
    } catch (ServiceException e) {
      xLogger.warn("Error in getting Dashboard {0}", dbId, e);
      throw new InvalidServiceException("Error in getting Dashboard " + dbId);
    }
  }

  @RequestMapping(value = "/delete/{dbId}", method = RequestMethod.GET)
  public
  @ResponseBody
  String delete(@PathVariable Long dbId) {
    ResourceBundle backendMessages = Resources.get().getBundle("BackendMessages", SecurityUtils.getLocale());
    try {
      String name = bulletinBoardDashBoardService.deleteDashboard(dbId);
      return backendMessages.getString("dashboard.upper") + " " + MsgUtil.bold(name) + " " + backendMessages
          .getString("deleted.success");
    } catch (ServiceException e) {
      xLogger.severe("Error deleting Dashboard: {0}", dbId);
      throw new InvalidServiceException("Error deleting Dashboard: " + dbId);
    }
  }

}
