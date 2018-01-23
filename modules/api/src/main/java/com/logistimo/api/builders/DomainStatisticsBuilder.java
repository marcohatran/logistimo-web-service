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

import com.logistimo.api.models.DomainStatisticsModel;
import com.logistimo.domains.entity.IDomain;
import com.logistimo.domains.service.DomainsService;
import com.logistimo.exception.InvalidServiceException;
import com.logistimo.logger.XLog;
import com.logistimo.reports.entity.slices.IDomainStats;
import com.logistimo.services.ObjectNotFoundException;
import com.logistimo.services.ServiceException;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Created by mohansrinivas on 10/20/16.
 */
@Component
public class DomainStatisticsBuilder {
  private static final XLog xLogger = XLog.getLog(DomainStatisticsBuilder.class);

  private DomainsService domainsService;

  @Autowired
  public void setDomainsService(DomainsService domainsService) {
    this.domainsService = domainsService;
  }

  public DomainStatisticsModel buildParentModel(IDomainStats masterData) {
    DomainStatisticsModel parentDataModel = new DomainStatisticsModel();
    try {
      Long domainId = masterData.getDomainId();
      if (domainId != 0) {
        IDomain domain = domainsService.getDomain(domainId);
        parentDataModel.hc = domain.getHasChild();
        parentDataModel.did = domainId;
        parentDataModel.dnm = domain.getName();
        parentDataModel.akc = masterData.getActiveKioskCount();
        parentDataModel.lkc = masterData.getLiveKioskCount();
        parentDataModel.kc = masterData.getKioskCount();
        parentDataModel.uc = masterData.getUserCount();
        parentDataModel.mwa = masterData.getMonitoredWorkingAssets();
        parentDataModel.mawa = masterData.getMonitoredActiveWorkingAssets();
        parentDataModel.mac = masterData.getMonitoredAssetCounts();
        parentDataModel.mlwa = masterData.getMonitoredLiveWorkingAssets();
        parentDataModel.miac = masterData.getMonitoringAssetCounts();
      }
    } catch (ServiceException | ObjectNotFoundException e) {
      xLogger.severe("unable to get the get the details for domainId", e);
      throw new InvalidServiceException("unable to get the get the details for domainId");
    }
    return parentDataModel;
  }

  public DomainStatisticsModel buildChildModel(List<? extends IDomainStats> childrenData,
                                               DomainStatisticsModel domainStatisticsModel) {
    try {
      for (IDomainStats childData : childrenData) {
        DomainStatisticsModel childDataModel = new DomainStatisticsModel();
        Long domainId = childData.getDomainId();
        // if domainID is null, we are assigning it to zero while fetching
        // this will check whether domainId is 0 or not
        if (domainId != 0) {
          IDomain domain = domainsService.getDomain(domainId);
          childDataModel.hc = domain.getHasChild();
          childDataModel.dnm = domain.getName();
          childDataModel.did = childData.getDomainId();
          childDataModel.akc = childData.getActiveKioskCount();
          childDataModel.lkc = childData.getLiveKioskCount();
          childDataModel.kc = childData.getKioskCount();
          childDataModel.uc = childData.getUserCount();
          childDataModel.mwa = childData.getMonitoredWorkingAssets();
          childDataModel.mawa = childData.getMonitoredActiveWorkingAssets();
          childDataModel.mac = childData.getMonitoredAssetCounts();
          childDataModel.mlwa = childData.getMonitoredLiveWorkingAssets();
          childDataModel.miac = childData.getMonitoringAssetCounts();
          domainStatisticsModel.child.add(childDataModel);
        }
      }
    } catch (ServiceException | ObjectNotFoundException e) {
      xLogger.severe("unable to get the details for domainId", e);
      throw new InvalidServiceException("unable to get the get the details for domainId");
    }
    return domainStatisticsModel;
  }



}
