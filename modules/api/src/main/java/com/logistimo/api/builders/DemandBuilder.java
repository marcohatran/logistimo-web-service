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

import com.logistimo.api.models.DemandBreakdownModel;
import com.logistimo.api.models.DemandModel;
import com.logistimo.auth.utils.SecurityUtils;
import com.logistimo.config.models.DomainConfig;
import com.logistimo.constants.CharacterConstants;
import com.logistimo.constants.Constants;
import com.logistimo.domains.entity.IDomain;
import com.logistimo.domains.service.DomainsService;
import com.logistimo.entities.entity.IKiosk;
import com.logistimo.entities.service.EntitiesService;
import com.logistimo.inventory.dao.IInvntryDao;
import com.logistimo.inventory.entity.IInvAllocation;
import com.logistimo.inventory.entity.IInvntry;
import com.logistimo.inventory.entity.IInvntryBatch;
import com.logistimo.inventory.entity.IInvntryEvntLog;
import com.logistimo.inventory.service.InventoryManagementService;
import com.logistimo.logger.XLog;
import com.logistimo.materials.entity.IHandlingUnit;
import com.logistimo.materials.service.IHandlingUnitService;
import com.logistimo.materials.service.MaterialCatalogService;
import com.logistimo.pagination.Results;
import com.logistimo.security.SecureUserDetails;
import com.logistimo.services.ServiceException;
import com.logistimo.utils.BigUtil;
import com.logistimo.utils.StringUtil;

import org.apache.commons.lang.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Created by smriti on 10/3/16.
 */
@Component
public class DemandBuilder {
  private static final XLog xLogger = XLog.getLog(DemandBuilder.class);

  private EntityBuilder entityBuilder;
  private EntitiesService entitiesService;
  private InventoryManagementService inventoryManagementService;
  private DomainsService domainsService;
  private MaterialCatalogService materialCatalogService;
  private IHandlingUnitService handlingUnitService;
  private IInvntryDao invntryDao;

  @Autowired
  public void setEntityBuilder(EntityBuilder entityBuilder) {
    this.entityBuilder = entityBuilder;
  }

  @Autowired
  public void setEntitiesService(EntitiesService entitiesService) {
    this.entitiesService = entitiesService;
  }

  @Autowired
  public void setInventoryManagementService(InventoryManagementService inventoryManagementService) {
    this.inventoryManagementService = inventoryManagementService;
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
  public void setHandlingUnitService(IHandlingUnitService handlingUnitService) {
    this.handlingUnitService = handlingUnitService;
  }

  @Autowired
  public void setInvntryDao(IInvntryDao invntryDao) {
    this.invntryDao = invntryDao;
  }

  public Results buildDemandModelList(Results results, Long kioskId, Long materialId) throws ServiceException {
    List items = results.getResults();
    List<DemandModel> modelItems = new ArrayList<>(results.getSize());
    for (Object item : items) {
      modelItems.add(buildDemandModel(kioskId, materialId, (Object[]) item, true));
    }
    return new Results<>(modelItems, results.getCursor(), results.getNumFound(), results.getOffset());
  }

  public DemandModel buildDemandModel(Long kioskId, Long materialId,
                                      Object[] item, boolean deep) throws ServiceException {
    SecureUserDetails sUser = SecurityUtils.getUserDetails();
    DemandModel model = new DemandModel();
    String kidStr = String.valueOf(item[0]);
    if (StringUtils.isNotEmpty(kidStr)) {
      IKiosk k = entitiesService.getKiosk(Long.parseLong(kidStr), false);
      if (deep) {
        model.e = entityBuilder.buildBaseModel(k, sUser.getLocale(), sUser.getTimezone(), sUser.getUsername());
        try {
          IDomain domain = domainsService.getDomain(model.e.sdid);
          model.e.sdname = domain.getName();
        } catch (Exception e) {
          xLogger.warn("Error while fetching Domain {0}", model.e.sdid);
        }
      }
      if (materialId != null) {
        DomainConfig dc = DomainConfig.getInstance(k.getDomainId());
        IInvntry inv = inventoryManagementService.getInventory(k.getKioskId(), materialId);
        if (inv != null) {
          model.csavibper = inventoryManagementService.getStockAvailabilityPeriod(inv, dc);
          IInvntryEvntLog lastEventLog = invntryDao.getInvntryEvntLog(inv);
          if (lastEventLog != null) {
            model.event = lastEventLog.getType();
          }
        }

      }
    }
    model.q = new BigDecimal(String.valueOf(item[1]));
    String val = String.valueOf(item[2]);
    if (StringUtils.isNotBlank(val)) {
      model.sq = new BigDecimal(val);
    } else {
      model.sq = BigDecimal.ZERO;
    }
    model.nm = String.valueOf(item[3]);
    val = String.valueOf(item[4]);
    if (StringUtils.isNotBlank(val)) {
      model.stk = new BigDecimal(val);
    }
    val = String.valueOf(item[5]);
    if (StringUtils.isNotBlank(val)) {
      model.atpstk = new BigDecimal(val);
    }
    val = String.valueOf(item[6]);
    if (StringUtils.isNotBlank(val)) {
      model.itstk = new BigDecimal(val);
    } else {
      model.itstk = BigDecimal.ZERO;
    }
    val = String.valueOf(item[7]);
    if (StringUtils.isNotBlank(val)) {
      model.min = new BigDecimal(val);
    }
    val = String.valueOf(item[8]);
    if (StringUtils.isNotBlank(val)) {
      model.max = new BigDecimal(val);
    }
    List<IInvAllocation> inv = new ArrayList<>();
    //demand list
    if (materialId == null) {
      model.id = Long.parseLong(String.valueOf(item[9]));
      List<String> oid = StringUtil.getList(String.valueOf(item[10]));
      if (oid.size() > 0) {
        for (String id : oid) {
          String tag = IInvAllocation.Type.ORDER + CharacterConstants.COLON + id;
          List<IInvAllocation> allocations = inventoryManagementService.getAllocationsByTagMaterial(model.id, tag);
          if (allocations != null) {
            inv.addAll(allocations);
          }
        }
      }
    } else { // demand detail
      model.id = materialId;
      model.oid = item[9] == null ? null : Long.parseLong(String.valueOf(item[9]));
      model.oty = item[10] == null ? null : Integer.parseInt(String.valueOf(item[10]));
      model.st = item[11] == null ? null : String.valueOf(item[11]);
      val = String.valueOf(item[12]);
      if (StringUtils.isNotBlank(val)) {
        model.fq = new BigDecimal(val);
      } else {
        model.fq = BigDecimal.ZERO;
      }
      model.mst = item[13] == null ? null : String.valueOf(item[13]);
      String tag = IInvAllocation.Type.ORDER + CharacterConstants.COLON + String.valueOf(model.oid);
      inv = inventoryManagementService.getAllocationsByTagMaterial(materialId, tag);
    }
    model.tm = materialCatalogService.getMaterial(model.id).isTemperatureSensitive();
    model.astk = BigDecimal.ZERO;
    List<DemandBreakdownModel> list = new ArrayList<>();
    Results<IInvntryBatch> rs = inventoryManagementService.getValidBatches(materialId, kioskId, null);
    if (rs.getSize() > 0) {
      model.isBa = true;
      for (IInvntryBatch ib : rs.getResults()) {
        DemandBreakdownModel breakdownModel = new DemandBreakdownModel();
        breakdownModel.bid = ib.getBatchId();
        if (ib.getBatchExpiry() != null) {
          SimpleDateFormat sdf = new SimpleDateFormat(Constants.DATE_FORMAT);
          breakdownModel.bexp = sdf.format(ib.getBatchExpiry());
        }
        breakdownModel.batpstk = ib.getAvailableStock();
        breakdownModel.matId = model.id;
        breakdownModel.kId = kioskId == null ? model.e.id : kioskId;
        breakdownModel.oQty = BigDecimal.ZERO;
        breakdownModel.bQty = BigDecimal.ZERO;
        breakdownModel.orderId = model.oid;
        breakdownModel.aQty = ib.getAllocatedStock();
        for (IInvAllocation invAllocation : inv) {
          if (breakdownModel.bid.equals(invAllocation.getBatchId())) {
            model.astk = model.astk.add(invAllocation.getQuantity());
            breakdownModel.mst = invAllocation.getMaterialStatus();
            if (invAllocation.getTypeId().contains(CharacterConstants.HYPHEN)) {
              breakdownModel.allocations
                  .put(invAllocation.getTypeId(), invAllocation.getQuantity());
            } else {
              breakdownModel.oQty = invAllocation.getQuantity();
            }
            breakdownModel.bQty = breakdownModel.bQty.add(invAllocation.getQuantity());
          }
        }
        if (breakdownModel.allocations.size() == 0) {
          breakdownModel.allocations = null;
        }
        if (BigUtil.notEqualsZero(ib.getAvailableStock().add(breakdownModel.bQty))) {
          list.add(breakdownModel);
        }
      }
    } else {
      DemandBreakdownModel breakdownModel = new DemandBreakdownModel();
      breakdownModel.kId = kioskId;
      breakdownModel.matId = materialId;
      breakdownModel.orderId = model.oid;
      for (IInvAllocation iInvAllocation : inv) {
        model.astk = model.astk.add(iInvAllocation.getQuantity());
        breakdownModel.mst = iInvAllocation.getMaterialStatus();
        if (iInvAllocation.getTypeId().contains(CharacterConstants.HYPHEN)) {
          breakdownModel.allocations.put(iInvAllocation.getTypeId(), iInvAllocation.getQuantity());
        } else {
          breakdownModel.oQty = iInvAllocation.getQuantity();
        }
      }
      if (breakdownModel.allocations.size() == 0) {
        breakdownModel.allocations = null;
      }
      list.add(breakdownModel);
    }
    model.allocations.addAll(list);
    model.yts = model.q.subtract(model.sq);
    // model.yta = model.q.subtract(model.astk);
    model.yta = model.q.subtract(model.astk.add(model.sq));
    try {
      Map<String, String> hu = handlingUnitService.getHandlingUnitDataByMaterialId(model.id);
      if (hu != null) {
        model.huQty = new BigDecimal(hu.get(IHandlingUnit.QUANTITY));
        model.huName = hu.get(IHandlingUnit.NAME);
      }
    } catch (Exception e) {
      xLogger.warn("Error while fetching Handling Unit {0}", model.id, e);
    }
    return model;
  }
}
