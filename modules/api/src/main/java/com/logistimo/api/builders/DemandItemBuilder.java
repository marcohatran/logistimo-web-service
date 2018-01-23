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
package com.logistimo.api.builders;

import com.logistimo.api.models.DemandModel;
import com.logistimo.auth.utils.SecurityUtils;
import com.logistimo.constants.Constants;
import com.logistimo.domains.entity.IDomain;
import com.logistimo.domains.service.DomainsService;
import com.logistimo.entities.entity.IKiosk;
import com.logistimo.entities.service.EntitiesService;
import com.logistimo.logger.XLog;
import com.logistimo.materials.entity.IMaterial;
import com.logistimo.materials.service.MaterialCatalogService;
import com.logistimo.orders.entity.IDemandItem;
import com.logistimo.pagination.Results;
import com.logistimo.security.SecureUserDetails;
import com.logistimo.services.ObjectNotFoundException;
import com.logistimo.services.ServiceException;
import com.logistimo.utils.CommonUtils;
import com.logistimo.utils.LocalDateUtil;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * @author charan
 */
@Component
public class DemandItemBuilder {

  private static final XLog xLogger = XLog.getLog(DemandItemBuilder.class);

  private EntityBuilder entityBuilder;
  private DomainsService domainsService;
  private MaterialCatalogService materialCatalogService;
  private EntitiesService entitiesService;

  @Autowired
  public void setEntityBuilder(EntityBuilder entityBuilder) {
    this.entityBuilder = entityBuilder;
  }

  @Autowired
  public void setDomainsService(DomainsService domainsService) {
    this.domainsService = domainsService;
  }

  @Autowired
  public void setMaterialCatalogService(MaterialCatalogService materialCatalogService) {
    this.materialCatalogService = materialCatalogService;
  }

  @Autowired
  public void setEntitiesService(EntitiesService entitiesService) {
    this.entitiesService = entitiesService;
  }

  public Results buildDemandItems(Results results, SecureUserDetails user, boolean skipDuplicates)
      throws ServiceException, ObjectNotFoundException {
    List demandItems;
    List<DemandModel> modelItems = null;
    if (results != null) {
      demandItems = results.getResults();
      modelItems = new ArrayList<>(demandItems.size());
      int count = results.getOffset() + 1;
      Set<String> kioskMaterials = new HashSet<>(demandItems.size());
      Map<Long, String> domainNames = new HashMap<>(1);
      for (Object obj : demandItems) {
        IDemandItem item = (IDemandItem) obj;
        String km = item.getKioskId() + "m:" + item.getMaterialId();
        if (skipDuplicates && kioskMaterials.contains(km)) {
          continue; // This is already processed
        }
        IMaterial m;
        try {
          m = materialCatalogService.getMaterial(item.getMaterialId());
        } catch (Exception e) {
          xLogger.warn("WARNING: " + e.getClass().getName()
              + " when getting material " + item.getMaterialId()
              + ": " + e.getMessage());
          continue;
        }
        // Add row
        DemandModel model = build(item, m, domainNames);
        if (model != null) {
          model.sno = count++;
          model.ts =
              LocalDateUtil.format(item.getTimestamp(), user.getLocale(), user.getTimezone());
          modelItems.add(model);
          if (skipDuplicates) {
            kioskMaterials.add(km);
          }
        }
      }
    }
    return new Results<>(modelItems, results.getCursor(), -1, results.getOffset());
  }

  private DemandModel build(IDemandItem item, IMaterial m,
                            Map<Long, String> domainNames)
      throws ServiceException, ObjectNotFoundException {
    SecureUserDetails sUser = SecurityUtils.getUserDetails();
    DemandModel itemModel = new DemandModel();
    Long kioskId = item.getKioskId();
    IKiosk k;
    try {
      k = entitiesService.getKiosk(kioskId);
    } catch (Exception e) {
      xLogger.warn("Error while fetching Kiosk {0}", item.getKioskId());
      return null;
    }
    itemModel.e = entityBuilder.buildBaseModel(k, sUser.getLocale(),
        sUser.getTimezone(), "");
    itemModel.nm = m.getName();
    itemModel.oid = item.getOrderId();
    itemModel.c = item.getCurrency();
    itemModel.id = item.getMaterialId();
    itemModel.q = item.getQuantity();
    itemModel.p = item.getFormattedPrice();
    itemModel.tx = item.getTax();
    itemModel.d = item.getDiscount();
    itemModel.a = CommonUtils.getFormattedPrice(item.computeTotalPrice(false));
    itemModel.isBn = m.isBinaryValued();
    itemModel.isBa = m.isBatchEnabled();
    itemModel.oq = item.getOriginalQuantity();
    itemModel.uid = item.getUserId();
    itemModel.stt = item.getStatus();
    itemModel.msg = item.getMessage();
    itemModel.sdid = item.getDomainId();
    String domainName = domainNames.get(item.getDomainId());
    if (domainName == null) {
      IDomain domain = null;
      try {
        domain = domainsService.getDomain(item.getDomainId());
      } catch (Exception e) {
        xLogger.warn("Error while fetching Domain {0}", item.getDomainId());
      }
      if (domain != null) {
        domainName = domain.getName();
      } else {
        domainName = Constants.EMPTY;
      }
      domainNames.put(item.getDomainId(), domainName);
    }
    itemModel.sdname = domainName;

    return itemModel;
  }

}
