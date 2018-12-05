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

/**
 *
 */
package com.logistimo.api.controllers;

import com.logistimo.api.models.ConfigJSONModel;
import com.logistimo.auth.utils.SecurityUtils;
import com.logistimo.config.entity.IConfig;
import com.logistimo.config.models.DomainConfig;
import com.logistimo.config.service.impl.ConfigurationMgmtServiceImpl;
import com.logistimo.dao.JDOUtils;
import com.logistimo.exception.ForbiddenAccessException;
import com.logistimo.exception.InvalidServiceException;
import com.logistimo.logger.XLog;
import com.logistimo.security.SecureUserDetails;
import com.logistimo.services.ObjectNotFoundException;
import com.logistimo.services.Resources;
import com.logistimo.services.ServiceException;
import com.logistimo.utils.LocalDateUtil;

import org.apache.commons.lang.StringUtils;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

import java.util.Map;
import java.util.ResourceBundle;
import java.util.TreeMap;

import javax.servlet.http.HttpServletRequest;

/**
 * @author charan, Mohan Raja
 */
@Controller
@RequestMapping("/config")
public class ConfigController {
  private static final XLog xLogger = XLog.getLog(ConfigController.class);

  private ConfigurationMgmtServiceImpl configurationMgmtService;

  @Autowired
  public void setConfigurationMgmtService(ConfigurationMgmtServiceImpl configurationMgmtService) {
    this.configurationMgmtService = configurationMgmtService;
  }

  @RequestMapping(value = "/locations", method = RequestMethod.GET)
  public
  @ResponseBody
  String getLocations() {
    ResourceBundle backendMessages = Resources.get().getBundle("BackendMessages",
        SecurityUtils.getLocale());
    try {
      return getObject(IConfig.LOCATIONS);
    } catch (ServiceException e) {
      xLogger.severe("Error in getting Location details");
      throw new InvalidServiceException("Error in getting accounts details");
    } catch (ObjectNotFoundException e) {
      xLogger.severe("Error in getting Location details");
      throw new InvalidServiceException(backendMessages.getString("general.config.fetch.error"));
    }
  }

  @RequestMapping(value = "/locations/currentdomain", method = RequestMethod.GET)
  public
  @ResponseBody
  String getCurrentDomainLocations() {
    SecureUserDetails sUser = SecurityUtils.getUserDetails();
    ResourceBundle backendMessages = Resources.get().getBundle("BackendMessages", sUser.getLocale());
    try {
      String key = IConfig.CONFIG_PREFIX + sUser.getDomainId();
      DomainConfig dc = new DomainConfig(configurationMgmtService.getConfiguration(key).getConfig());
      String locations = getObject(IConfig.LOCATIONS);
      JSONObject jsonObject = new JSONObject(locations);
      return jsonObject.getJSONObject("data").getJSONObject(dc.getCountry()).toString();
    } catch (Exception e) {
      xLogger.severe("Error in getting Location details");
      throw new InvalidServiceException(backendMessages.getString("general.config.fetch.error"));
    }
  }

  @RequestMapping(value = "/currencies", method = RequestMethod.GET)
  public
  @ResponseBody
  String getCurrencies() {
    ResourceBundle backendMessages = Resources.get().getBundle("BackendMessages", SecurityUtils.getLocale());
    try {
      return getObject(IConfig.CURRENCIES);
    } catch (ServiceException | ObjectNotFoundException e) {
      xLogger.severe("Error in getting Currencies details");
      throw new InvalidServiceException(backendMessages.getString("general.config.fetch.error"));
    }
  }

  @RequestMapping(value = "/languages", method = RequestMethod.GET)
  public
  @ResponseBody
  String getLanguages(@RequestParam(required = false) String type) {
    ResourceBundle backendMessages = Resources.get().getBundle("BackendMessages", SecurityUtils.getLocale());
    try {
      if ("mobile".equalsIgnoreCase(type)) {
        return getObject(IConfig.LANGUAGES_MOBILE);
      } else {
        return getObject(IConfig.LANGUAGES);
      }
    } catch (ServiceException | ObjectNotFoundException e) {
      xLogger.severe("Error in getting Languages details");
      throw new InvalidServiceException(backendMessages.getString("general.config.fetch.error"));
    }
  }

  @RequestMapping(value = "/timezones", method = RequestMethod.GET)
  public
  @ResponseBody
  Map<String, String> getTimezones() {
    return new TreeMap<>(LocalDateUtil.getTimeZoneNames()); //sorted timezones
  }

  @RequestMapping(value = "/timezoneskvreversed", method = RequestMethod.GET)
  public
  @ResponseBody
  Map<String, String> getTimezonesKVReversed() {
    return new TreeMap<>(LocalDateUtil.getTimeZoneNamesKVReversed()); //sorted timezones
  }

  @RequestMapping(value = "/timezones/offset", method = RequestMethod.GET)
  public
  @ResponseBody
  Map<String, String> getTimezonesWithOffset(HttpServletRequest request) {
    SecureUserDetails user = SecurityUtils.getUserDetails();
    return new TreeMap<>(
        LocalDateUtil.getTimeZoneNamesWithOffset(user.getLocale())); //sorted timezones
  }

  private String getObject(String config)
      throws ServiceException, ObjectNotFoundException {
    IConfig c = configurationMgmtService.getConfiguration(config);
    String jsonObject = null;
    if (c != null && c.getConfig() != null) {
      jsonObject = c.getConfig();
    }
    return jsonObject;
  }

  @RequestMapping(value = "/generalconfig", method = RequestMethod.GET)
  public
  @ResponseBody
  String getGeneralConfig() {
    ResourceBundle backendMessages = Resources.get().getBundle("BackendMessages", SecurityUtils.getLocale());
    try {
      return getObject(IConfig.GENERALCONFIG);
    } catch (ServiceException e) {
      xLogger.severe("Error in getting General Configuration details");
      throw new InvalidServiceException(backendMessages.getString("general.config.fetch.error"));
    } catch (ObjectNotFoundException e) {
      xLogger.severe("Error in getting General Config details");
      throw new InvalidServiceException(backendMessages.getString("general.config.fetch.error"));
    }
  }

  @RequestMapping(value = "/json", method = RequestMethod.GET)
  public
  @ResponseBody
  String getConfigJson(@RequestParam(name = "config_type") String configType)
      throws ServiceException {
    if (!SecurityUtils.isSuperUser()) {
      throw new ForbiddenAccessException("Forbidden");
    }
    IConfig config = configurationMgmtService.getConfiguration(configType);
    if (config != null && StringUtils.isNotEmpty(config.getConfig())) {
      return config.getConfig();
    }
    return "{}";
  }

  @RequestMapping(value = "/json", method = RequestMethod.POST)
  public
  @ResponseBody
  String updateConfigJSON(@RequestBody ConfigJSONModel configJSONModel)
      throws ServiceException {
    SecureUserDetails user = SecurityUtils.getUserDetails();
    if (!SecurityUtils.isSuperUser()) {
      throw new ForbiddenAccessException("Forbidden");
    }
    IConfig config = JDOUtils.createInstance(IConfig.class);
    config.setKey(configJSONModel.type);
    config.setConfig(configJSONModel.configJson);
    config.setUserId(user.getUsername());
    configurationMgmtService.updateConfiguration(config);
    return configJSONModel.type + " updated successfully";
  }

}
