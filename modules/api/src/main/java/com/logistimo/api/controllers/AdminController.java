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

import com.logistimo.AppFactory;
import com.logistimo.api.migrators.ConfigReasonsMigrator;
import com.logistimo.api.migrators.DomainLocIDConfigMigrator;
import com.logistimo.api.migrators.EventsConfigMigrator;
import com.logistimo.api.migrators.UserDomainIdsMigrator;
import com.logistimo.api.models.SimulateRequestModel;
import com.logistimo.api.util.KioskDataSimulator;
import com.logistimo.auth.SecurityConstants;
import com.logistimo.auth.utils.SecurityUtils;
import com.logistimo.config.entity.IConfig;
import com.logistimo.config.service.ConfigurationMgmtService;
import com.logistimo.constants.CharacterConstants;
import com.logistimo.constants.Constants;
import com.logistimo.events.handlers.EventHandler;
import com.logistimo.exception.InvalidServiceException;
import com.logistimo.exception.TaskSchedulingException;
import com.logistimo.exception.ValidationException;
import com.logistimo.inventory.entity.IInvntry;
import com.logistimo.inventory.service.InventoryManagementService;
import com.logistimo.locations.client.LocationClient;
import com.logistimo.logger.XLog;
import com.logistimo.security.SecureUserDetails;
import com.logistimo.services.ServiceException;
import com.logistimo.services.cache.MemcacheService;
import com.logistimo.services.impl.PMF;
import com.logistimo.services.taskqueue.ITaskService;
import com.logistimo.users.entity.IUserDevice;
import com.logistimo.users.service.UsersService;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

import java.io.IOException;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import javax.servlet.http.HttpServletResponse;

/**
 * Created by smriti on 30/07/15.
 */
@Controller
@RequestMapping("/admin")
public class AdminController {
  private static final String
      ACTION_SIMULATETRANSDATA =
      "simulatetransdata";
  private static final String KIOSKID_PARAM = "kioskid";
  private static final String STARTDATE_PARAM = "startdate";
  private static final String DURATION_PARAM = "duration";
  private static final String STOCKONHAND_PARAM = "stockonhand";
  private static final String ISSUEPERIODICITY_PARAM = "issueperiodicity";
  private static final String ISSUEMEAN_PARAM = "issuemean";
  private static final String ISSUESTDDEV_PARAM = "issuestddev";
  private static final String RECEIPTMEAN_PARAM = "receiptmean";
  private static final String RECEIPTSTDDEV_PARAM = "receiptstddev";
  private static final String ZEROSTOCKDAYSLOW_PARAM = "zerostockdayslow";
  private static final String ZEROSTOCKDAYSHIGH_PARAM = "zerostockdayshigh";
  private static final String MATERIALID_PARAM = "materialid";
  private static final String DOMAINID_PARAM = "domainid";
  private static final String USERID_PARAM = "userid";
  private static final String REQUESTTYPE_PARAM = "requesttype";
  private static final String REQUESTTYPE_EXECUTE = "execute";
  private static final String TASK_URL = "/task/datagenerator";
  private static final XLog xLogger = XLog.getLog(AdminController.class);

  private LocationClient locationClient;
  private ConfigurationMgmtService configurationMgmtService;
  private UsersService usersService;
  private InventoryManagementService inventoryManagementService;

  @Autowired
  public void setLocationClient(LocationClient locationClient) {
    this.locationClient = locationClient;
  }

  @Autowired
  public void setConfigurationMgmtService(ConfigurationMgmtService configurationMgmtService) {
    this.configurationMgmtService = configurationMgmtService;
  }

  @Autowired
  public void setUsersService(UsersService usersService) {
    this.usersService = usersService;
  }

  @Autowired
  public void setInventoryManagementService(InventoryManagementService inventoryManagementService) {
    this.inventoryManagementService = inventoryManagementService;
  }

  @RequestMapping(value = "/dailyevents", method = RequestMethod.GET)
  public
  @ResponseBody
  void dailyExport(@RequestParam Long dId) {
    try {
      SecureUserDetails sUser = SecurityUtils.getUserDetails();
      if (sUser.getRole().equals(SecurityConstants.ROLE_SUPERUSER) && dId != null) {
        EventHandler.createDailyEvents(dId);
      } else {
        throw new ServiceException("Permission denied");
      }
    } catch (ServiceException e) {
      xLogger.severe("Error while scheduling tasks for daily notification ", e);
    }
  }

  @RequestMapping(value = "/burstcache", method = RequestMethod.GET)
  public
  @ResponseBody
  void burstCache() {
    try {
      SecureUserDetails sUser = SecurityUtils.getUserDetails();
      if (sUser.getRole().equals(SecurityConstants.ROLE_SUPERUSER)) {
        PMF.get().getDataStoreCache().evictAll();
      } else {
        throw new ServiceException("Permission denied");
      }
    } catch (ServiceException e) {
      xLogger.severe("Error while bursting cache", e);
    }
  }

  @RequestMapping(value = "/batchNotify", method = RequestMethod.GET)
  public
  @ResponseBody
  void batchExport(@RequestParam Long dId, @RequestParam(required = false) Date start,
                   @RequestParam(required = false) Date end) {
    try {
      SecureUserDetails sUser = SecurityUtils.getUserDetails();
      if (sUser.getRole().equals(SecurityConstants.ROLE_SUPERUSER) && dId != null) {
        EventHandler.CustomDuration customDuration = null;
        if (start != null || end != null) {
          customDuration = new EventHandler.CustomDuration();
          customDuration.duration = new EventHandler.Duration();
          customDuration.duration.start = start;
          customDuration.duration.end = end;
        }
        EventHandler.batchNotify(dId, customDuration);
      } else {
        throw new ServiceException("Permission denied");
      }
    } catch (ServiceException e) {
      xLogger.severe("Error while scheduling tasks for daily notification", e);
    }
  }

  @RequestMapping(value = "/burstDashboardCache", method = RequestMethod.GET)
  public
  @ResponseBody
  void burstDashboardCache() {
    try {
      SecureUserDetails sUser = SecurityUtils.getUserDetails();
      xLogger.info("User {0} requested for object cache burst", sUser.getUsername());
      if (sUser.getRole().equals(SecurityConstants.ROLE_SUPERUSER)) {
        MemcacheService mcs = AppFactory.get().getMemcacheService();
        //Clear all dashboard
        mcs.deleteByPattern(Constants.DASHBOARD_CACHE_PREFIX + CharacterConstants.ASTERISK);
        mcs.deleteByPattern(
            Constants.PREDICTIVE_DASHBOARD_CACHE_PREFIX + CharacterConstants.ASTERISK);
        mcs.deleteByPattern(Constants.SESSACT_DASHBOARD_CACHE_PREFIX + CharacterConstants.ASTERISK);
        mcs.deleteByPattern(Constants.INV_DASHBOARD_CACHE_PREFIX + CharacterConstants.ASTERISK);
        //Clear all network views cache
        mcs.deleteByPattern(Constants.NW_HIERARCHY_CACHE_PREFIX + CharacterConstants.ASTERISK);
      } else {
        throw new ServiceException("Permission denied");
      }
    } catch (ServiceException e) {
      xLogger.severe("Error while bursting cache", e);
    }
  }

  @RequestMapping(value = "/migrateusers", method = RequestMethod.GET)
  public
  @ResponseBody
  void migrateUsers() {
    UserDomainIdsMigrator migrator = new UserDomainIdsMigrator();
    try {
      migrator.migrateUserDomainIds();
    } catch (Exception e) {
      xLogger.severe("Exception occurred during user domain ids migration", e);
    }
  }

  @RequestMapping(value = "/migrate240", method = RequestMethod.GET)
  public
  @ResponseBody
  void migrate240() {
    EventsConfigMigrator migrator = new EventsConfigMigrator();
    try {
      migrator.migrateEventsConfig();
    } catch (Exception e) {
      xLogger.severe("Exception occurred during user domain ids migration", e);
      throw new InvalidServiceException(e);
    }
  }

  @RequestMapping(value = "/migrate270", method = RequestMethod.GET)
  public
  @ResponseBody
  void migrate270(@RequestParam(required = false) Boolean json) {
    try {
      ConfigReasonsMigrator.update(null, json == null ? false : json);
    } catch (Exception e) {
      xLogger.severe("Exception occurred during updating", e);
      throw new InvalidServiceException(e);
    }
  }

  @RequestMapping(value = "/updatedomainlocids", method = RequestMethod.GET)
  public
  @ResponseBody
  void updateDomainLocIds() {
    DomainLocIDConfigMigrator migrator = new DomainLocIDConfigMigrator();
    try {
      migrator.updateDomainLocConfig();
    } catch (Exception e) {
      xLogger.severe("Exception occurred during user domain ids migration", e);
      throw new InvalidServiceException(e);
    }
  }

  @RequestMapping(value = "/update-locations-data", method = RequestMethod.POST)
  public
  @ResponseBody
  void updateLocationsData() throws ServiceException {
    IConfig c = configurationMgmtService.getConfiguration(IConfig.LOCATIONS);
    locationClient.updateLocationsMasterdata(c.getConfig());
  }


  @RequestMapping(value = "/notification-token", method = RequestMethod.GET)
  public
  @ResponseBody
  String getUserDeviceToken(@RequestParam String userId, @RequestParam String appName,
                            HttpServletResponse response) {
    IUserDevice result = null;
    try {
      result = usersService.getUserDevice(userId, appName);
    } catch (ServiceException e) {
      xLogger.warn("Error while getting device token for user {0}", userId, e);
      try {
        response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, e.getMessage());
      } catch (IOException e1) {
        xLogger.warn("Error while getting device token for user {0}", userId, e1);
      }
    }
    return (result != null) ? result.getToken() : CharacterConstants.EMPTY;
  }

  @RequestMapping(value = "/simulate-data", method = RequestMethod.POST)
  public
  @ResponseBody
  String simulateData(@RequestBody SimulateRequestModel simulateRequestModel)
      throws ServiceException, TaskSchedulingException {
    String message;
    List<IInvntry> inventories =
        inventoryManagementService.getInventoryByKiosk(simulateRequestModel.entityId, null).getResults();

    if (inventories == null || inventories.size() == 0) {
      throw new ValidationException(
          "No materials are associated with the kiosk. No transaction data was generated.");
    } else {
      Iterator<IInvntry> it = inventories.iterator();
      // Schedule data generation tasks per material
      int totalMaterials = inventories.size();
      int numMaterials = 0;
      while (it.hasNext() && numMaterials < KioskDataSimulator.MATERIAL_LIMIT) {
        IInvntry inv = it.next();
        // Get the parameter map
        Map<String, String> params = new HashMap<>();
        // Add action and type parameters
        params.put("action", ACTION_SIMULATETRANSDATA);
        params.put(REQUESTTYPE_PARAM, REQUESTTYPE_EXECUTE);
        // Add the data simulation parameters
        params.put(KIOSKID_PARAM, String.valueOf(simulateRequestModel.entityId));
        params.put(STARTDATE_PARAM, simulateRequestModel.startDate);
        params.put(DURATION_PARAM, String.valueOf(simulateRequestModel.duration));
        params.put(STOCKONHAND_PARAM, String.valueOf(simulateRequestModel.stockOnHand));
        params.put(ISSUEPERIODICITY_PARAM, simulateRequestModel.periodicity);
        params.put(ISSUEMEAN_PARAM, String.valueOf(simulateRequestModel.issueMean));
        params.put(ISSUESTDDEV_PARAM, String.valueOf(simulateRequestModel.issueStdDev));
        params.put(RECEIPTMEAN_PARAM, String.valueOf(simulateRequestModel.receiptMean));
        params.put(RECEIPTSTDDEV_PARAM, String.valueOf(simulateRequestModel.receiptStdDev));
        params.put(ZEROSTOCKDAYSLOW_PARAM, String.valueOf(simulateRequestModel.zeroStockDaysLow));
        params.put(ZEROSTOCKDAYSHIGH_PARAM,
            String.valueOf(simulateRequestModel.zeroStockDaysHigh));
        // Add the material Id as a new parameter to the scheduled task URL
        params.put(MATERIALID_PARAM, inv.getMaterialId().toString());
        // Add the domain Id as a new parameter to the scheduled task URL
        params.put(DOMAINID_PARAM, String.valueOf((SecurityUtils.getCurrentDomainId())));
        // Add the user Id
        params.put(USERID_PARAM, SecurityUtils.getUsername());
        // Schedule the task of simulating trans. data for a given kiosk-material
        try {
          getTaskService().schedule(ITaskService.QUEUE_DATASIMULATOR, TASK_URL, params, null,
              ITaskService.METHOD_POST, SecurityUtils.getCurrentDomainId(),
              SecurityUtils.getUsername(), "SIMULATE_TRANS");
          numMaterials++; // count materials for successful scheduling
        } catch (TaskSchedulingException e) {
          xLogger.warn("Exception while scheduling task for kiosk-material: {0}-{1} : {2}",
              simulateRequestModel.entityId, inv.getMaterialId(), e.getMessage());
          throw e;
        }
      }

      // Form the success message
      message =
          "Scheduled transaction-data simulation request for <b>" + String.valueOf(numMaterials)
              + "</b> material(s).";
      if ((totalMaterials - numMaterials) > 0) {
        message +=
            " Could not schedule task(s) for <b>" + String.valueOf(totalMaterials - numMaterials)
                + "</b> material(s).";
      }
    }
    return message;
  }


  public ITaskService getTaskService() {
    return AppFactory.get().getTaskService();
  }
}
