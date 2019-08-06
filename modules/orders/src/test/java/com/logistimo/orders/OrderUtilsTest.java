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

package com.logistimo.orders;

import com.logistimo.models.shipments.ShipmentMaterialsModel;
import com.logistimo.proto.FulfillmentBatchMaterialRequest;
import com.logistimo.proto.FulfillmentMaterialRequest;
import com.logistimo.proto.UpdateOrderStatusRequest;

import org.junit.Test;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import static org.junit.Assert.*;

public class OrderUtilsTest {

  @Test
  public void testGetShipmentModels() {
    UpdateOrderStatusRequest updateOrderStatusRequest = new UpdateOrderStatusRequest();
    updateOrderStatusRequest.ost = "FULFILLED";
    List<FulfillmentMaterialRequest> materialRequests = new ArrayList<>();
    FulfillmentMaterialRequest fulfillmentMaterialRequest = new FulfillmentMaterialRequest();
    fulfillmentMaterialRequest.q = BigDecimal.valueOf(200);
    List<FulfillmentBatchMaterialRequest> fulfillmentBatchMaterialRequests = new ArrayList<>();
    FulfillmentBatchMaterialRequest batchMaterialRequest = new FulfillmentBatchMaterialRequest();
    batchMaterialRequest.bid = "B123";
    batchMaterialRequest.q = BigDecimal.valueOf(200);
    batchMaterialRequest.fmst = "VVM Usable";
    fulfillmentBatchMaterialRequests.add(batchMaterialRequest);
    fulfillmentMaterialRequest.bt = fulfillmentBatchMaterialRequests;
    materialRequests.add(fulfillmentMaterialRequest);
    updateOrderStatusRequest.mt = materialRequests;
    updateOrderStatusRequest.dar = new Date();
    ShipmentMaterialsModel model = OrderUtils.getShipmentMaterialsModel(updateOrderStatusRequest);
    assertNotNull(model);
  }

}