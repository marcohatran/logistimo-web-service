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

package com.logistimo.deliveryrequest.fleet.model.orders;

import com.logistimo.deliveryrequest.mapper.DeliveryRequestStatusMapper;
import com.logistimo.deliveryrequest.models.DeliveryRequestStatus;

import org.springframework.stereotype.Component;

/**
 * Created by chandrakant on 08/05/19.
 */
@Component
class FleetOrderStatusMapper implements DeliveryRequestStatusMapper<FleetOrderStatus> {
  @Override
  public DeliveryRequestStatus map(FleetOrderStatus orderStatus) {
    switch (orderStatus) {
      case DRAFT:
      case APPROVAL_REQUIRED:
        return DeliveryRequestStatus.PENDING_CONFIRMATION;
      case OPEN:
      case TO_REVIEW:
        return DeliveryRequestStatus.CONFIRMED;
      case PICKED:
      case STG_SD:
      case IN_TRANSIT:
      case STG_ID:
      case STG_DD:
        return DeliveryRequestStatus.PICKED;
      case OUT_FOR_DELIVERY:
        return DeliveryRequestStatus.OUT_FOR_DELIVERY;
      case DELIVERED:
        return DeliveryRequestStatus.DELIVERED;
      case CLOSED:
      case CANCELLED:
        return DeliveryRequestStatus.CANCELLED;
      default:
        return DeliveryRequestStatus.PENDING_CONFIRMATION;
    }
  }
}