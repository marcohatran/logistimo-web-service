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

package com.logistimo.api.controllers;

import com.logistimo.auth.GenericAuthoriser;
import com.logistimo.auth.utils.SecurityUtils;
import com.logistimo.deliveryrequest.actions.GetDeliveryRequestsAction;
import com.logistimo.deliveryrequest.actions.ICancelDeliveryRequestAction;
import com.logistimo.deliveryrequest.actions.IGetShippingOptionsAction;
import com.logistimo.deliveryrequest.models.DeliveryRequestModel;
import com.logistimo.deliveryrequest.models.ShippingOptions;
import com.logistimo.entities.auth.EntityAuthoriser;
import com.logistimo.exception.ForbiddenAccessException;
import com.logistimo.pagination.Results;
import com.logistimo.services.ServiceException;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Objects;

/**
 * Created by kumargaurav on 06/02/19.
 */
@RestController
@RequestMapping(path = "/delivery-requests")
public class DeliveryRequestController {

  @Autowired
  private GetDeliveryRequestsAction getDeliveryRequestsAction;

  @Autowired
  @Qualifier("fleetGetShippingOptionsAction")
  private IGetShippingOptionsAction getShippingOptionsAction;

  @Autowired
  @Qualifier("fleetCancelDeliveryRequestAction")
  private ICancelDeliveryRequestAction cancelDeliveryRequestAction;

  @RequestMapping(method = RequestMethod.GET)
  public ResponseEntity getDeliveryRequest(@RequestParam String sId,
                   @RequestParam(defaultValue = "false", required = false) boolean countOnly) {
    Results<DeliveryRequestModel> drs = getDeliveryRequestsAction.getByShipmentId(sId, countOnly);
    return new ResponseEntity<>(drs, HttpStatus.OK);
  }

  @RequestMapping(value = "/shipping-options",method = RequestMethod.POST)
  public ResponseEntity getShippingOptions(@RequestBody DeliveryRequestModel deliveryRequestModel)
      throws ServiceException {
    ShippingOptions options = getShippingOptionsAction.invoke(deliveryRequestModel);
    return new ResponseEntity<>(options, HttpStatus.OK);
  }

  @RequestMapping(value = "/{deliveryRequestId}", method = RequestMethod.DELETE)
  public ResponseEntity cancelDeliveryRequest(@PathVariable Long deliveryRequestId)
      throws ServiceException {
    Long vendorId = getDeliveryRequestsAction.getById(deliveryRequestId).getShipper().getKid();
    if (!SecurityUtils.isAdmin() && Objects.equals(EntityAuthoriser.authoriseEntityPerm(vendorId),
        GenericAuthoriser.NO_ACCESS)) {
      throw new ForbiddenAccessException("G002");
    }
    return ResponseEntity.ok().body(
        cancelDeliveryRequestAction.invoke(SecurityUtils.getUsername(), deliveryRequestId));
  }

}
