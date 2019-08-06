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

package com.logistimo.shipments.mapper;

import com.logistimo.dao.JDOUtils;
import com.logistimo.domains.utils.DomainsUtil;
import com.logistimo.models.shipments.ShipmentItemBatchModel;
import com.logistimo.services.ServiceException;
import com.logistimo.shipments.entity.IShipmentItemBatch;

import org.springframework.stereotype.Component;

import java.util.Date;

@Component
public class ShipmentItemBatchMapper {

  public IShipmentItemBatch invoke(ShipmentItemBatchModel batches)
      throws ServiceException {
    Date now = new Date();
    IShipmentItemBatch sbItem = JDOUtils.createInstance(IShipmentItemBatch.class);
    sbItem.setCreatedBy(batches.uid);
    sbItem.setCreatedOn(now);
    sbItem.setUpdatedBy(batches.uid);
    sbItem.setUpdatedOn(now);
    sbItem.setMaterialId(batches.mid);
    sbItem.setKioskId(batches.kid);
    sbItem.setBatchManufacturer(batches.bmfnm);
    sbItem.setShipmentItemId(batches.siId);

    sbItem.setQuantity(batches.q);
    sbItem.setBatchId(batches.id);
    sbItem.setShippedMaterialStatus(batches.smst);
    DomainsUtil.addToDomain(sbItem, batches.sdid, null);
    return sbItem;
  }
}