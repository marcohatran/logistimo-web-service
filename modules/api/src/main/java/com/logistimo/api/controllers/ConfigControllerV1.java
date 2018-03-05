/*
 * Copyright © 2018 Logistimo.
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

import com.logistimo.api.action.AssetConfigurationAction;
import com.logistimo.api.auth.AuthenticationUtil;
import com.logistimo.api.models.configuration.AssetSystemConfigModel;
import com.logistimo.auth.utils.SecurityUtils;
import com.logistimo.exception.UnauthorizedException;
import com.logistimo.logger.XLog;
import com.logistimo.security.SecureUserDetails;
import com.logistimo.services.ObjectNotFoundException;
import com.logistimo.services.ServiceException;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;


import java.io.IOException;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 * Created by naveensnair on 23/02/18.
 */
@Controller
@RequestMapping("/configuration")
public class ConfigControllerV1 {


  private AssetConfigurationAction assetConfigurationAction;

  @Autowired
  public void setAssetConfigurationAction(AssetConfigurationAction assetConfigurationAction) {
    this.assetConfigurationAction = assetConfigurationAction;
  }

  private static final XLog xLogger = XLog.getLog(ConfigControllerV1.class);

  @RequestMapping(method = RequestMethod.GET)
  public
  @ResponseBody
  AssetSystemConfigModel
  getAssetConfiguration(@RequestParam(required = false) String src, HttpServletResponse response)
      throws ServiceException, IOException {
    AssetSystemConfigModel model = new AssetSystemConfigModel();
    try {
      SecureUserDetails userDetails = SecurityUtils.getUserDetails();
      model = assetConfigurationAction.invoke(src, userDetails.getDomainId(), userDetails.getLocale(), userDetails.getTimezone());
    } catch (UnauthorizedException | ObjectNotFoundException e) {
      response.sendError(HttpServletResponse.SC_UNAUTHORIZED, e.getMessage());

    } catch (Exception e) {
      xLogger.severe("Issue with api client authentication", e);
      response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, e.getMessage());
    }
    return model;
  }
}
