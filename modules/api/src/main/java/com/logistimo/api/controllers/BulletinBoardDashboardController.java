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
import com.logistimo.auth.utils.SessionMgr;
import com.logistimo.dashboards.entity.IDashboard;
import com.logistimo.dashboards.service.IBulletinBoardDashBoardService;
import com.logistimo.dashboards.service.impl.BulletinBoardDashBoardService;
import com.logistimo.exception.InvalidServiceException;
import com.logistimo.logger.XLog;
import com.logistimo.security.SecureUserDetails;
import com.logistimo.services.Resources;
import com.logistimo.services.ServiceException;
import com.logistimo.services.Services;
import com.logistimo.utils.MsgUtil;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

import java.util.List;
import java.util.Locale;
import java.util.ResourceBundle;

import javax.servlet.http.HttpServletRequest;

/**
 * @author Mohan Raja
 */
@Controller
@RequestMapping("/dashboards")
public class BulletinBoardDashboardController {
  private static final XLog xLogger = XLog.getLog(DashboardController.class);

  BulletinBoardDashBoardBuilder builder = new BulletinBoardDashBoardBuilder();

  @RequestMapping(value = "/", method = RequestMethod.POST)
  public
  @ResponseBody
  String create(@RequestBody BulletinBoardDashBoardModel model, HttpServletRequest request) {
    SecureUserDetails sUser = SecurityUtils.getUserDetails(request);
    Locale locale = sUser.getLocale();
    ResourceBundle backendMessages = Resources.get().getBundle("BackendMessages", locale);
    long domainId = SecurityUtils.getDomainId(request);
    try {
      IBulletinBoardDashBoardService ds = Services.getService(BulletinBoardDashBoardService.class);
      if (model.getDashboardId() != null) {
        IDashboard db = ds.getDashBoard(model.getDashboardId());
        db = builder.buildDashboardForUpdate(model, db, sUser.getUsername());
        ds.updateDashboard(db);
      } else {
        IDashboard db = builder.buildDashboard(model, domainId, sUser.getUsername());
        ds.createDashboard(db);
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
  List<BulletinBoardDashBoardModel> getAll(HttpServletRequest request) {
    SecureUserDetails sUser = SecurityUtils.getUserDetails(request);
    long domainId = SessionMgr.getCurrentDomain(request.getSession(), sUser.getUsername());
    BulletinBoardDashBoardService ds = Services.getService(BulletinBoardDashBoardService.class);
    List<IDashboard> dbList = ds.getDashBoards(domainId);
    return builder.buildDashboardModelList(dbList);
  }

  @RequestMapping(value = "/{dbId}", method = RequestMethod.GET)
  public
  @ResponseBody
  BulletinBoardDashBoardModel get(@PathVariable Long dbId,
                                  @RequestParam(required = false) String wc) {
    try {
      BulletinBoardDashBoardService ds = Services.getService(BulletinBoardDashBoardService.class);
      IDashboard db = ds.getDashBoard(dbId);
      BulletinBoardDashBoardModel model = builder.buildDashboardModel(db);
      return model;
    } catch (ServiceException e) {
      xLogger.warn("Error in getting Dashboard {0}", dbId, e);
      throw new InvalidServiceException("Error in getting Dashboard " + dbId);
    }
  }

  @RequestMapping(value = "/delete/{dbId}", method = RequestMethod.GET)
  public
  @ResponseBody
  String delete(@PathVariable Long dbId, HttpServletRequest request) {
    String name;
    SecureUserDetails sUser = SecurityUtils.getUserDetails(request);
    Locale locale = sUser.getLocale();
    ResourceBundle backendMessages = Resources.get().getBundle("BackendMessages", locale);
    try {
      BulletinBoardDashBoardService ds = Services.getService(BulletinBoardDashBoardService.class);
      name = ds.deleteDashboard(dbId);
    } catch (ServiceException e) {
      xLogger.severe("Error deleting Dashboard: {0}", dbId);
      throw new InvalidServiceException("Error deleting Dashboard: " + dbId);
    }
    return backendMessages.getString("dashboard.upper") + " " + MsgUtil.bold(name) + " " + backendMessages
        .getString("deleted.success");
  }

}
