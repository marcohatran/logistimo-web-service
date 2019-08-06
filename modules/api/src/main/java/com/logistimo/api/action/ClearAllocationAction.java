/*
 * Copyright © 2019 Logistimo.
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

package com.logistimo.api.action;

import com.logistimo.jpa.Repository;
import com.logistimo.logger.XLog;
import com.logistimo.models.InventoryAllocationModel;
import com.logistimo.orders.service.impl.DemandService;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by kumargaurav on 24/01/19.
 */
@Component
public class ClearAllocationAction {

  private static final XLog xLogger = XLog.getLog(ClearAllocationAction.class);

  private Repository repository;

  private DemandService demandService;

  @Autowired
  public void setRepository(Repository repository) {
    this.repository = repository;
  }

  @Autowired
  public void setDemandService(DemandService demandService) {
    this.demandService = demandService;
  }

  public void invoke() {
    allocations().stream().forEach(invallocation -> clearAllocation(invallocation));
    xLogger.info("Clear inventoty allocation job ran successfuly!!!");
  }

  private void clearAllocation(InventoryAllocationModel model) {
    try {
      demandService.clearAllocations(model.getKioskId(), model.getMaterialId(), model.getOrderId(), false, false);
      xLogger.info("Inventory allocation got cleared for with detail {0}",model);
    } catch (Exception e) {
      xLogger.warn("Issue with clearing inventory allocation  with detail {0}", model, e);
    }
  }
  private List<InventoryAllocationModel> allocations() {

    String query = "select IA.TYPEID, IA.MID, IA.KID, IA.Q, IA.CON, IA.UON, O.ST, K.NAME, IA.MST "
        + " from INVALLOCATION IA, `ORDER` O, KIOSK K where Q <> 0 and IA.TYPE = 'o' and IA.TYPEID = O.ID "
        + " and K.KIOSKID = IA.KID and O.ST in ('fl','cn') order by UON desc ";
    List<Object[]> result = repository.findAllByNativeQuery(query,null);
    return map(result);
  }

  private List<InventoryAllocationModel> map(List<Object[]> result) {

    List<InventoryAllocationModel> list = new ArrayList<>();
    InventoryAllocationModel model = null;
    for(Object[] obj: result) {
      model = new InventoryAllocationModel();
      model.setOrderId(Long.valueOf((String)obj[0]));
      model.setMaterialId(((BigInteger)obj[1]).longValue());
      model.setKioskId(((BigInteger)obj[2]).longValue());
      model.setQuantity(((BigDecimal)obj[3]).doubleValue());
      model.setCreatedOn(((Timestamp)obj[4]).toString());
      model.setUpdatedOn(((Timestamp)obj[5]).toString());
      model.setOrderStatus((String)obj[6]);
      model.setKioskName((String)obj[7]);
      list.add(model);
    }
    return list;
  }
}
