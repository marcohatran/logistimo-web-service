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

package com.logistimo.deliveryrequest.validator;

import com.logistimo.auth.utils.SecurityUtils;
import com.logistimo.deliveryrequest.actions.GetDeliveryRequestsAction;
import com.logistimo.deliveryrequest.models.DeliveryRequestModel;
import com.logistimo.deliveryrequest.models.DeliveryRequestStatus;
import com.logistimo.pagination.Results;
import com.logistimo.services.Resources;
import com.logistimo.services.ServiceException;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.ResourceBundle;

/**
 * Created by chandrakant on 18/06/19.
 */
@Component
public class NoActiveDeliveryRequestValidator {

  private GetDeliveryRequestsAction getDeliveryRequestsAction;

  @Autowired
  public void setGetDeliveryRequestsAction(GetDeliveryRequestsAction getDeliveryRequestsAction) {
    this.getDeliveryRequestsAction = getDeliveryRequestsAction;
  }

  public void validate(String shipmentId) throws ServiceException {
    Results<DeliveryRequestModel> drs = getDeliveryRequestsAction.getByShipmentId(shipmentId, false);
    if(drs.getResults() != null) {
      ResourceBundle backendMessages = Resources
          .getBundle("BackendMessages", SecurityUtils.getLocale());

      boolean atLeastOneActiveDr = drs.getResults().stream().anyMatch(s ->
          !DeliveryRequestStatus.INACTIVE_DELIVERY_REQUEST_STATUSES.contains(s.getStatus()));
      if(atLeastOneActiveDr) {
        throw new ServiceException(backendMessages.getString("active.dr.error"));
      }
    }
  }
}