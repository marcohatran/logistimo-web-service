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

import com.logistimo.api.models.CurrentUserModel;
import com.logistimo.auth.SecurityConstants;
import com.logistimo.auth.utils.SecurityUtils;
import com.logistimo.domains.service.DomainsService;
import com.logistimo.entities.entity.IKiosk;
import com.logistimo.entities.models.UserEntitiesModel;
import com.logistimo.entities.service.EntitiesService;
import com.logistimo.exception.InvalidServiceException;
import com.logistimo.security.SecureUserDetails;
import com.logistimo.services.ObjectNotFoundException;
import com.logistimo.services.Resources;
import com.logistimo.services.ServiceException;
import com.logistimo.users.entity.IUserAccount;

import org.apache.commons.lang.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Locale;
import java.util.ResourceBundle;

/**
 * @author Mohan Raja
 */
@Component
public class CurrentUserBuilder {

  private EntitiesService entitiesService;
  private DomainsService domainsService;

  @Autowired
  public void setEntitiesService(EntitiesService entitiesService) {
    this.entitiesService = entitiesService;
  }

  @Autowired
  public void setDomainsService(DomainsService domainsService) {
    this.domainsService = domainsService;
  }

  public CurrentUserModel buildCurrentUserModel() {
    SecureUserDetails sUser = SecurityUtils.getUserDetails();
    String userId = sUser.getUsername();
    Locale locale = sUser.getLocale();
    Long domainId = sUser.getCurrentDomainId();
    ResourceBundle backendMessages = Resources.getBundle(locale);
    try {
      String domainName = domainsService.getDomain(domainId).getName();
      UserEntitiesModel userEntitiesModel = entitiesService.getUserWithKiosks(userId);
      IUserAccount user = userEntitiesModel.getUserAccount();
      String loggedUserLng = user.getLanguage();
      CurrentUserModel model = new CurrentUserModel();
      if (StringUtils.isNotEmpty(user.getFullName())) {
        model.ufn = user.getFullName();
      } else {
        model.ufn = user.getFirstName();
      }
      model.unm = userId;
      model.dnm = domainName;
      model.lng = loggedUserLng;
      model.tz = sUser.getTimezone();
      model.em = user.getEmail();
      model.eid = user.getPrimaryKiosk();
      if (model.eid == null) {
        List<IKiosk> kiosks;
        if (SecurityConstants.ROLE_SERVICEMANAGER.equals(sUser.getRole())) {
          kiosks = userEntitiesModel.getKiosks();
          if (kiosks != null && kiosks.size() == 1) {
            model.eid = kiosks.get(0).getKioskId();
          }
        }
      }
      return model;
    } catch (ServiceException | ObjectNotFoundException e) {
      throw new InvalidServiceException(backendMessages.getString("current.user.fetch.error"));
    }
  }
}
