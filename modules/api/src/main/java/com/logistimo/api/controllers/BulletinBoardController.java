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
import com.logistimo.api.models.BulletinBoardModel;
import com.logistimo.auth.utils.SecurityUtils;
import com.logistimo.bulletinboard.entity.IBulletinBoard;
import com.logistimo.bulletinboard.service.impl.BulletinBoardService;
import com.logistimo.exception.InvalidServiceException;
import com.logistimo.logger.XLog;
import com.logistimo.security.SecureUserDetails;
import com.logistimo.services.Resources;
import com.logistimo.utils.MsgUtil;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;

import java.util.List;
import java.util.Locale;
import java.util.ResourceBundle;

/**
 * Created by naveensnair on 14/11/17.
 */
@Controller
@RequestMapping("/bulletinboard")
public class BulletinBoardController {
  private static final XLog xLogger = XLog.getLog(BulletinBoardController.class);

  private static final String BULLETIN_BOARD = "bulletin.board";

  private BulletinBoardDashBoardBuilder bulletinBoardDashboardBuilder;
  private BulletinBoardService bulletinBoardService;

  @Autowired
  public void setBulletinBoardDashboardBuilder(
      BulletinBoardDashBoardBuilder bulletinBoardDashboardBuilder) {
    this.bulletinBoardDashboardBuilder = bulletinBoardDashboardBuilder;
  }

  @Autowired
  public void setBulletinBoardService(BulletinBoardService bulletinBoardService) {
    this.bulletinBoardService = bulletinBoardService;
  }

  @RequestMapping(value = "/", method = RequestMethod.POST)
  public
  @ResponseBody
  String create(@RequestBody BulletinBoardModel model) {
    SecureUserDetails sUser = SecurityUtils.getUserDetails();
    Locale locale = sUser.getLocale();
    ResourceBundle backendMessages = Resources.get().getBundle("BackendMessages", locale);
    long domainId = SecurityUtils.getDomainId();
    String msg;
    try {
      if (model.getBulletinBoardId() != null) {
        IBulletinBoard bulletinBoard =
            bulletinBoardService.getBulletinBoard(model.getBulletinBoardId());
        bulletinBoard =
            bulletinBoardDashboardBuilder.buildBulletinBoardForUpdate(model, bulletinBoard, sUser.getUsername());
        bulletinBoardService.updateBulletinBoard(bulletinBoard);
        msg =
            backendMessages.getString(BULLETIN_BOARD) + " " + MsgUtil.bold(model.getName()) + " "
                + backendMessages.getString("update.success");
      } else {
        IBulletinBoard
            bulletinBoard =
            bulletinBoardDashboardBuilder.buildBulletinBoard(model, domainId, sUser.getUsername());
        bulletinBoardService.createBulletinBoard(bulletinBoard);
        msg =
            backendMessages.getString(BULLETIN_BOARD) + " " + MsgUtil.bold(model.getName()) + " "
                + backendMessages
                .getString("created.success");
      }
    } catch (Exception e) {
      xLogger.severe("Error creating bulletin board for domain ", domainId);
      throw new InvalidServiceException("Error creating bulletin board for " + domainId);
    }
    return msg;
  }

  @RequestMapping(value = "/all", method = RequestMethod.GET)
  public
  @ResponseBody
  List<BulletinBoardModel> getAll() {
    List<IBulletinBoard> bulletinBoardList = bulletinBoardService.getBulletinBoards(SecurityUtils.getDomainId());
    return bulletinBoardDashboardBuilder.buildBulletinBoardModelList(bulletinBoardList);
  }

  @RequestMapping(value = "/{bulletinBoardId}", method = RequestMethod.GET)
  public
  @ResponseBody
  BulletinBoardModel getBulletinBoard(@PathVariable Long bulletinBoardId) {
    try {
      IBulletinBoard bulletinBoard = bulletinBoardService.getBulletinBoard(bulletinBoardId);
      return bulletinBoardDashboardBuilder.buildBulletinBoardModel(bulletinBoard);
    } catch (Exception e) {
      xLogger.warn("Error in getting BulletinBoard {0}", bulletinBoardId, e);
      throw new InvalidServiceException("Error in getting BulletinBoard " + bulletinBoardId);
    }
  }

  @RequestMapping(value = "/delete/{bulletinBoardId}", method = RequestMethod.GET)
  public
  @ResponseBody
  String delete(@PathVariable Long bulletinBoardId) {
    String name;
    SecureUserDetails sUser = SecurityUtils.getUserDetails();
    Locale locale = sUser.getLocale();
    java.util.ResourceBundle backendMessages = Resources.get().getBundle("BackendMessages", locale);
    try {
      name = bulletinBoardService.deleteBulletinBoard(bulletinBoardId);
    } catch (Exception e) {
      xLogger.severe("Error deleting Dashboard: {0}", bulletinBoardId);
      throw new InvalidServiceException("Error deleting Dashboard: " + bulletinBoardId);
    }

    return backendMessages.getString(BULLETIN_BOARD) + " " + MsgUtil.bold(name) + " "
        + backendMessages.getString("deleted.success");
  }

}
