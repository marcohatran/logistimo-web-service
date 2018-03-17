/*
 * Copyright © 2018 Logistimo.
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

import com.logistimo.api.builders.ReturnsBuilder;
import com.logistimo.auth.utils.SecurityUtils;
import com.logistimo.constants.SourceConstants;
import com.logistimo.logger.XLog;
import com.logistimo.returns.Status;
import com.logistimo.returns.entity.Returns;
import com.logistimo.returns.entity.ReturnsItem;
import com.logistimo.returns.entity.ReturnsItemBatch;
import com.logistimo.returns.entity.values.Batch;
import com.logistimo.returns.entity.values.ReturnsStatus;
import com.logistimo.returns.models.MobileReturnsModel;
import com.logistimo.returns.models.MobileReturnsRequestModel;
import com.logistimo.returns.models.MobileReturnsUpdateStatusModel;
import com.logistimo.returns.models.MobileReturnsUpdateStatusRequestModel;
import com.logistimo.services.ServiceException;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/**
 * @author Mohan Raja
 */
@Controller
@RequestMapping("/returns")
public class ReturnController {

  @Autowired
  ReturnsBuilder returnsBuilder;

  @RequestMapping(method = RequestMethod.POST)
  public
  @ResponseBody
  MobileReturnsModel create(@RequestBody MobileReturnsRequestModel mobileReturnsRequestModel)
      throws ServiceException {
    Returns returns = returnsBuilder.createNewReturns(mobileReturnsRequestModel);
    //todo: Persist Returns using ReturnService and re-initialise the returns variable with updated one
    return returnsBuilder.buildMobileReturnsModel(returns);
  }

  @RequestMapping(value = "/{returnId}/{status}", method = RequestMethod.POST)
  public
  @ResponseBody
  MobileReturnsUpdateStatusModel updateStatus(@PathVariable String returnId,
                                              @PathVariable String status,
                                              @RequestBody MobileReturnsUpdateStatusRequestModel updateStatusModel)
      throws ServiceException {
    //todo: With ReturnService update the Returns with status by returnId and get the updated Returns
    return returnsBuilder.buildMobileReturnsUpdateModel(new Returns(), updateStatusModel);
  }

  @RequestMapping(value = "/{returnId}", method = RequestMethod.GET)
  public
  @ResponseBody
  MobileReturnsModel get(@PathVariable String returnId) throws ServiceException {
    //todo: With ReturnService get the Returns object by returnId
    return returnsBuilder.buildMobileReturnsModel(getTestReturns());
  }

  private Returns getTestReturns() {
    Returns returns = new Returns();
    returns.setSourceDomain(SecurityUtils.getCurrentDomainId());
    returns.setId(1l);
    returns.setOrderId(28l);
    returns.setCustomerId(1344728l);
    returns.setVendorId(1344729l);
    final Date now = new Date();
    final String username = "mohan";
    returns.setStatus(new ReturnsStatus(Status.OPEN, null, now, username));
    returns.setCreatedAt(now);
    returns.setCreatedBy(username);
    returns.setUpdatedAt(now);
    returns.setUpdatedBy(username);
    returns.setSource(SourceConstants.MOBILE);

    List<ReturnsItem> returnsItems = new ArrayList<>();
    ReturnsItem returnsItem = new ReturnsItem();
    returnsItem.setMaterialId(3345739l);
    returnsItem.setQuantity(BigDecimal.ONE);
    returnsItem.setMaterialStatus("test material status");
    returnsItem.setReason("test reason");
    returnsItem.setCreatedAt(now);
    returnsItem.setCreatedBy(username);
    returnsItem.setUpdatedAt(now);
    returnsItem.setUpdatedBy(username);
    returnsItems.add(returnsItem);

    ReturnsItem returnsItemBatch = new ReturnsItem();
    returnsItemBatch.setMaterialId(3345740l);
    returnsItemBatch.setQuantity(BigDecimal.ONE);
    returnsItemBatch.setMaterialStatus("should not display");
    returnsItemBatch.setReason("should not display");
    returnsItemBatch.setCreatedAt(now);
    returnsItemBatch.setCreatedBy(username);
    returnsItemBatch.setUpdatedAt(now);
    returnsItemBatch.setUpdatedBy(username);
    List<ReturnsItemBatch> itemBatches = new ArrayList<>();
    ReturnsItemBatch itemBatch = new ReturnsItemBatch();
    itemBatch.setBatch(new Batch("B1", now, "manufacturer", now));
    itemBatch.setQuantity(BigDecimal.ONE);
    itemBatch.setMaterialStatus("batch material status");
    itemBatch.setReason("batch reason");
    itemBatches.add(itemBatch);
    returnsItemBatch.setBatches(itemBatches);

    returnsItems.add(returnsItemBatch);

    returns.setItems(returnsItems);

    returns.setSource(SourceConstants.WEB);

    return returns;
  }

}
