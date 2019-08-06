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

import com.logistimo.api.builders.PoolGroupBuilder;
import com.logistimo.api.models.EntityGroupModel;
import com.logistimo.auth.SecurityConstants;
import com.logistimo.auth.SecurityUtil;
import com.logistimo.auth.utils.SecurityUtils;
import com.logistimo.dao.JDOUtils;
import com.logistimo.entities.entity.IKiosk;
import com.logistimo.entities.entity.IPoolGroup;
import com.logistimo.entities.service.EntitiesService;
import com.logistimo.exception.BadRequestException;
import com.logistimo.exception.InvalidServiceException;
import com.logistimo.logger.XLog;
import com.logistimo.pagination.Results;
import com.logistimo.security.SecureUserDetails;
import com.logistimo.services.Resources;
import com.logistimo.services.ServiceException;
import com.logistimo.utils.LocalDateUtil;
import com.logistimo.utils.MsgUtil;

import org.apache.commons.lang.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.ResourceBundle;

/**
 * @author charan
 */
@Controller
@RequestMapping("/ent-grps")
public class EntityGroupsController {

  private static final XLog xLogger = XLog.getLog(EntityGroupsController.class);
  private PoolGroupBuilder poolGroupBuilder;
  private EntitiesService entitiesService;

  @Autowired
  public void setPoolGroupBuilder(PoolGroupBuilder poolGroupBuilder) {
    this.poolGroupBuilder = poolGroupBuilder;
  }

  @Autowired
  public void setEntitiesService(EntitiesService entitiesService) {
    this.entitiesService = entitiesService;
  }

  @RequestMapping("/")
  public
  @ResponseBody
  Results getEntityGroups() {
    SecureUserDetails sUser = SecurityUtils.getUserDetails();
    Locale locale = sUser.getLocale();
    ResourceBundle backendMessages = Resources.getBundle(locale);
    Long domainId = sUser.getCurrentDomainId();
    try {
      List<IPoolGroup> poolGroups = entitiesService.findAllPoolGroups(domainId, 1, 0);
      int count = 0;
      List<EntityGroupModel> models = new ArrayList<>(1);
      for (IPoolGroup pg : poolGroups) {
        if (SecurityUtil.compareRoles(sUser.getRole(), SecurityConstants.ROLE_DOMAINOWNER) < 0
            && !sUser.getUsername().equals(pg.getOwnerId())) {
          continue;
        }
        EntityGroupModel model = new EntityGroupModel();
        model.sno = ++count;
        model.id = pg.getGroupId();
        model.nm = pg.getName();
        model.ct = pg.getCity();
        model.dt = pg.getDistrict();
        model.st = pg.getState();
        model.cnt = pg.getCountry();
        model.uid = pg.getOwnerId();
        model.num = pg.getKiosks() != null ? pg.getKiosks().size() : 0;
        model.t = pg.getTimeStamp();
        model.updOn = LocalDateUtil.format(pg.getTimeStamp(), locale, sUser.getTimezone());
        models.add(model);
      }
      return new Results<>(models, null, count, 0);
    } catch (ServiceException e) {
      xLogger.severe("Error in fetching entity groups", e);
      throw new InvalidServiceException(backendMessages.getString("poolgroup.fetch.error"));
    }
  }

  @RequestMapping(value = "/action/{action}", method = RequestMethod.POST)
  public
  @ResponseBody
  String create(@RequestBody EntityGroupModel entityGroupModel, @PathVariable String action) {
    SecureUserDetails sUser = SecurityUtils.getUserDetails();
    Locale locale = sUser.getLocale();
    ResourceBundle backendMessages = Resources.getBundle(locale);
    if (entityGroupModel == null) {
      throw new BadRequestException(backendMessages.getString("poolgroup.create.error"));
    }
    Long domainId = sUser.getCurrentDomainId();
    if (domainId == null) {
      xLogger.severe("Error in creating entity group");
      throw new InvalidServiceException(backendMessages.getString("poolgroup.create.error"));
    }
    try {
      IPoolGroup poolGroup = JDOUtils.createInstance(IPoolGroup.class);
      if (StringUtils.isNotBlank(entityGroupModel.nm)) {
        poolGroup.setName(entityGroupModel.nm);
      }
      if (StringUtils.isNotBlank(entityGroupModel.dsc)) {
        poolGroup.setDescription(entityGroupModel.dsc);
      }
      if (StringUtils.isNotBlank(entityGroupModel.uid)) {
        poolGroup.setOwnerId(entityGroupModel.uid);
      }
      if (entityGroupModel.ent != null && entityGroupModel.ent.size() > 0) {
        String[] kiosksArr = new String[entityGroupModel.ent.size()];
        for (int i = 0; i < entityGroupModel.ent.size(); i++) {
          kiosksArr[i] = String.valueOf(entityGroupModel.ent.get(i).id);
        }
        List<IKiosk> Kiosks = new ArrayList<>();
        for (String k : kiosksArr) {
          Kiosks.add(entitiesService.getKiosk(Long.parseLong(k.trim()), false));
        }
        poolGroup.setKiosks(Kiosks);
      }
      poolGroup.setUpdatedBy(sUser.getUsername());
      if (action.equalsIgnoreCase("add")) {
        poolGroup.setCreatedBy(sUser.getUsername());
        entitiesService.addPoolGroup(domainId, poolGroup);
      } else if (action.equalsIgnoreCase("update")) {
        if (StringUtils.isNotEmpty(entityGroupModel.id.toString())) {
          poolGroup.setGroupId(entityGroupModel.id);
        }
        poolGroup.setDomainId(domainId);
        entitiesService.updatePoolGroup(poolGroup);
        return backendMessages.getString("poolgroup") + ' ' + MsgUtil.bold(entityGroupModel.nm)
            + ' ' + backendMessages.getString("update.success") + '.';
      }
      return backendMessages.getString("poolgroup") + ' ' + MsgUtil.bold(entityGroupModel.nm) + ' '
          + backendMessages.getString("create.success") + '.';
    } catch (ServiceException e) {
      if (action.equalsIgnoreCase("add")) {
        xLogger.severe("Error in creating entity group", e);
        throw new InvalidServiceException(backendMessages.getString("poolgroup.create.error"));
      } else {
        xLogger.severe("Error in updating entity group", e);
        throw new InvalidServiceException(backendMessages.getString("poolgroup.update.error"));
      }
    }
  }

  @RequestMapping(value = "/groupId/{groupId}", method = RequestMethod.GET)
  public
  @ResponseBody
  EntityGroupModel getPoolGroup(@PathVariable Long groupId) {
    SecureUserDetails sUser = SecurityUtils.getUserDetails();
    Locale locale = sUser.getLocale();
    ResourceBundle backendMessages = Resources.getBundle(locale);
    Long domainId = sUser.getCurrentDomainId();
    if (domainId == null) {
      xLogger.severe("Error in fetching entity groups");
      throw new InvalidServiceException(backendMessages.getString("poolgroup.fetch.error"));
    }
    try {
      String timeZone = sUser.getTimezone();
      EntityGroupModel entityGroupModel = null;
      IPoolGroup pg = entitiesService.getPoolGroup(groupId);
      if (pg != null) {
        entityGroupModel = poolGroupBuilder.buildPoolGroupModel(pg, locale, timeZone);
      }
      return entityGroupModel;
    } catch (ServiceException e) {
      xLogger.severe("Error in fetching entity groups", e);
      throw new InvalidServiceException(backendMessages.getString("poolgroup.fetch.error"));
    }
  }

  @RequestMapping(value = "/delete", method = RequestMethod.POST)
  public
  @ResponseBody
  String deletePoolGroups(@RequestBody String groupIds) {
    SecureUserDetails sUser = SecurityUtils.getUserDetails();
    Locale locale = sUser.getLocale();
    ResourceBundle backendMessages = Resources.getBundle(locale);
    if (groupIds == null) {
      throw new BadRequestException(backendMessages.getString("poolgroup.delete.error"));
    }
    Long domainId = sUser.getCurrentDomainId();
    if (domainId == null) {
      xLogger.severe("Error in deleting entity group");
      throw new InvalidServiceException(backendMessages.getString("poolgroup.delete.error"));
    }
    try {
      String[] pgIds = groupIds.split(",");
      ArrayList<Long> poolGroupIDs = new ArrayList<>();
      for (String pgID : pgIds) {
        poolGroupIDs.add(Long.parseLong(pgID.trim()));
      }
      entitiesService.deletePoolGroups(domainId, poolGroupIDs);
      return backendMessages.getString("poolgroup.delete.success");
    } catch (ServiceException e) {
      xLogger.severe("Error in deleting entity groups", e);
      throw new InvalidServiceException(backendMessages.getString("poolgroup.delete.error"));
    }
  }
}
