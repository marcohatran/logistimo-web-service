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

import com.logistimo.api.builders.HUBuilder;
import com.logistimo.api.models.HUModel;
import com.logistimo.api.util.SearchUtil;
import com.logistimo.auth.GenericAuthoriser;
import com.logistimo.auth.utils.SecurityUtils;
import com.logistimo.exception.ForbiddenAccessException;
import com.logistimo.exception.InvalidDataException;
import com.logistimo.exception.InvalidServiceException;
import com.logistimo.logger.XLog;
import com.logistimo.materials.entity.IHandlingUnit;
import com.logistimo.materials.service.IHandlingUnitService;
import com.logistimo.models.ICounter;
import com.logistimo.pagination.Navigator;
import com.logistimo.pagination.PageParams;
import com.logistimo.pagination.Results;
import com.logistimo.security.SecureUserDetails;
import com.logistimo.services.Resources;
import com.logistimo.services.ServiceException;
import com.logistimo.users.entity.IUserAccount;
import com.logistimo.users.service.UsersService;
import com.logistimo.utils.Counter;
import com.logistimo.utils.MsgUtil;

import org.apache.commons.lang.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

import java.util.Locale;
import java.util.ResourceBundle;

import javax.servlet.http.HttpServletRequest;

/**
 * @author Mohan Raja
 */
@Controller
@RequestMapping("/hu")
public class HandlingUnitController {

  private static final XLog xLogger = XLog.getLog(HandlingUnitController.class);

  private HUBuilder huBuilder;
  private IHandlingUnitService handlingUnitService;
  private UsersService usersService;

  @Autowired
  public void setHuBuilder(HUBuilder huBuilder) {
    this.huBuilder = huBuilder;
  }

  @Autowired
  public void setHandlingUnitService(IHandlingUnitService handlingUnitService) {
    this.handlingUnitService = handlingUnitService;
  }

  @Autowired
  public void setUsersService(UsersService usersService) {
    this.usersService = usersService;
  }

  @RequestMapping(value = "/create", method = RequestMethod.POST)
  public
  @ResponseBody
  String createHandlingUnit(@RequestBody HUModel huModel) {
    if (huModel == null) {
      throw new InvalidDataException("Error while creating handing unit.");
    }
    SecureUserDetails sUser = SecurityUtils.getUserDetails();
    Locale locale = sUser.getLocale();
    ResourceBundle backendMessages = Resources.getBundle(locale);
    if (!GenericAuthoriser.authoriseAdmin()) {
      throw new ForbiddenAccessException(backendMessages.getString("permission.denied"));
    }
    IHandlingUnit hu = huBuilder.buildHandlingUnit(huModel);
    hu.setCreatedBy(sUser.getUsername());
    hu.setUpdatedBy(sUser.getUsername());
    try {
      if (hu.getName() != null) {
        long domainId = sUser.getCurrentDomainId();
        IHandlingUnit temp = handlingUnitService.getHandlingUnitByName(domainId, hu.getName());
        if (temp != null) {
          throw new InvalidDataException("Handling unit " + hu.getName() + " " + backendMessages
              .getString("error.alreadyexists"));
        } else {
          handlingUnitService.addHandlingUnit(domainId, hu);
          xLogger.info("AUDITLOG\t {0}\t {1}\t HANDLING UNIT\t CREATE \t {2} \t {3}", domainId,
              sUser.getUsername(), hu.getId(), hu.getName());
        }
      } else {
        throw new InvalidDataException("No handling unit name");
      }
    } catch (ServiceException e) {
      xLogger.warn("Error creating handling unit {0} {1}", hu.getName(), hu.getId(), e);
      throw new InvalidServiceException(
          "Error while creating handling unit " + MsgUtil.bold(hu.getName()) +
              MsgUtil.addErrorMsg(e.getMessage()));
    }
    return "Handling unit " + MsgUtil.bold(huModel.name) + " " + backendMessages
        .getString("create.success");
  }

  @RequestMapping(value = "/", method = RequestMethod.GET)
  public
  @ResponseBody
  Results getHandlingUnits(@RequestParam(defaultValue = PageParams.DEFAULT_OFFSET_STR) int offset,
                           @RequestParam(defaultValue = PageParams.DEFAULT_SIZE_STR) int size,
                           @RequestParam(required = false) String q,
                           HttpServletRequest request) {
    SecureUserDetails sUser = SecurityUtils.getUserDetails();
    Long domainId = sUser.getCurrentDomainId();
    try {
      Navigator
          navigator =
          new Navigator(request.getSession(), "HandlingUnitController.getHandlingUnits", offset,
              size, "dummy", 0);
      PageParams pageParams = new PageParams(navigator.getCursor(offset), offset, size);
      Results results;
      if (StringUtils.isNotBlank(q)) {
        results = SearchUtil.findHandlingUnits(domainId, q, pageParams);
      } else {
        results = handlingUnitService.getAllHandlingUnits(domainId, pageParams);
      }
      ICounter counter = Counter.getHandlingUnitCounter(domainId);
      if (StringUtils.isNotEmpty(q)) {
        results.setNumFound(-1);
      } else if (counter.getCount() > results.getSize()) {
        results.setNumFound(counter.getCount());
      } else {
        results.setNumFound(results.getSize());
      }
      results.setOffset(offset);
      navigator.setResultParams(results);
      return huBuilder.buildHandlingUnitModelList(results);
    } catch (ServiceException e) {
      xLogger.warn("Error fetching handling unit details for domain {0}", domainId, e);
      throw new InvalidServiceException(
          " Error fetching handling unit details for domain " + domainId);
    }
  }

  @RequestMapping(value = "/hu/{huId}", method = RequestMethod.GET)
  public
  @ResponseBody
  HUModel getHandlingUnitById(@PathVariable Long huId) {
    try {
      IHandlingUnit hu = handlingUnitService.getHandlingUnit(huId);

      IUserAccount cb = null, ub = null;
      if (hu.getCreatedBy() != null) {
        try {
          cb = usersService.getUserAccount(hu.getCreatedBy());
        } catch (Exception ignored) {
          // do nothing
        }
      }
      if (hu.getUpdatedBy() != null) {
        try {
          ub = usersService.getUserAccount(hu.getUpdatedBy());
        } catch (Exception ignored) {
          // do nothing
        }
      }
      return huBuilder.buildHUModel(hu, cb, ub);
    } catch (ServiceException e) {
      xLogger.warn("Error fetching handling unit details for {0}", huId, e);
      throw new InvalidServiceException("Error fetching handling unit details for " + huId);
    }
  }

  @RequestMapping(value = "/update", method = RequestMethod.POST)
  public
  @ResponseBody
  String updateHandlingUnit(@RequestBody HUModel huModel) {
    SecureUserDetails sUser = SecurityUtils.getUserDetails();
    Locale locale = sUser.getLocale();
    ResourceBundle backendMessages = Resources.getBundle(locale);
    if (!GenericAuthoriser.authoriseAdmin()) {
      throw new ForbiddenAccessException(backendMessages.getString("permission.denied"));
    }
    IHandlingUnit hu = huBuilder.buildHandlingUnit(huModel);
    hu.setUpdatedBy(sUser.getUsername());
    Long domainId = sUser.getCurrentDomainId();
    try {
      if (hu.getName() != null) {
        handlingUnitService.updateHandlingUnit(hu, domainId);
        xLogger.info("AUDITLOG\t{0}\t{1}\tHANDLING UNIT\t UPDATE\t{2}\t{3}", domainId,
            sUser.getUsername(), hu.getId(), hu.getName());
      } else {
        throw new InvalidDataException("No handling unit name");
      }
    } catch (Exception e) {
      xLogger.warn("Error updating handling unit {0}", hu.getId(), e);
      throw new InvalidServiceException(
          "Error updating handling unit " + MsgUtil.bold(hu.getName()) +
              MsgUtil.addErrorMsg(e.getMessage()));
    }
    return "Handling unit " + MsgUtil.bold(huModel.name) + " " + backendMessages
        .getString("updated.successfully.lowercase");
  }
}
