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

package com.logistimo.shipments.action;

import com.logistimo.config.models.DomainConfig;
import com.logistimo.config.models.OrdersConfig;
import com.logistimo.constants.Constants;
import com.logistimo.constants.SourceConstants;
import com.logistimo.deliveryrequest.actions.ISyncDeliveryRequestAction;
import com.logistimo.deliveryrequest.entities.DeliveryRequest;
import com.logistimo.deliveryrequest.fleet.model.orders.DeliveryRequestMapper;
import com.logistimo.deliveryrequest.models.DeliveryRequestModel;
import com.logistimo.deliveryrequest.models.DeliveryRequestStatus;
import com.logistimo.deliveryrequest.models.DeliveryRequestUpdateWrapper;
import com.logistimo.exception.LogiException;
import com.logistimo.logger.XLog;
import com.logistimo.services.Resources;
import com.logistimo.services.ServiceException;
import com.logistimo.shipments.ShipmentRepository;
import com.logistimo.shipments.ShipmentStatus;
import com.logistimo.shipments.entity.IShipment;
import com.logistimo.shipments.service.impl.ShipmentService;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.ResourceBundle;

/**
 * Created by chandrakant on 13/06/19.
 */
@Component
@Transactional
public class SyncDeliveryRequestAndShipmentAction {

  private static final XLog xLogger = XLog.getLog(SyncDeliveryRequestAndShipmentAction.class);

  private ISyncDeliveryRequestAction syncDeliveryRequestAction;
  private ShipmentService shipmentService;
  private ShipmentRepository shipmentRepository;
  private DeliveryRequestMapper mapper;

  @Autowired
  @Qualifier("fleetSyncDeliveryRequestAction")
  public void setSyncDeliveryRequestAction(ISyncDeliveryRequestAction syncDeliveryRequestAction) {
    this.syncDeliveryRequestAction = syncDeliveryRequestAction;
  }

  @Autowired
  public void setShipmentService(ShipmentService shipmentService) {
    this.shipmentService = shipmentService;
  }

  @Autowired
  public void setShipmentRepository(ShipmentRepository shipmentRepository) {
    this.shipmentRepository = shipmentRepository;
  }

  @Autowired
  public void setMapper(DeliveryRequestMapper mapper) {
    this.mapper = mapper;
  }

  public DeliveryRequestModel invoke(String userId, Long domainId, Long drId)
      throws LogiException {
    DeliveryRequest deliveryRequest = syncDeliveryRequestAction.invoke(userId, drId);
    return updateShipment(domainId, deliveryRequest);
  }

  public DeliveryRequestModel invoke(String userId, DeliveryRequestUpdateWrapper updateModel)
      throws LogiException {
    DeliveryRequest deliveryRequest = syncDeliveryRequestAction.invoke(userId, updateModel);
    return updateShipment(deliveryRequest.getDomainId(), deliveryRequest);
  }

  private DeliveryRequestModel updateShipment(Long domainId,
                                              DeliveryRequest deliveryRequest)
      throws LogiException {
    DeliveryRequestModel model = mapper.mapFromEntity(deliveryRequest);
    IShipment shipment = shipmentRepository.getById(deliveryRequest.getShipmentId());
    ShipmentStatus newShipmentStatus =
        getNewShipmentStatus(deliveryRequest.getStatus(), shipment.getStatus());
    updateShipmentETA(shipment, deliveryRequest.getEta());
    if (shipment.getStatus() != newShipmentStatus &&
        autoUpdateOfShipmentEnabled(domainId, shipment, newShipmentStatus)) {
      try {
        updateShipmentStatus(shipment, newShipmentStatus);
      } catch (ServiceException e) {
        xLogger.warn("Invalid shipment state change request. CurrStatus: "
                     + shipment.getStatus() + "; NewStatus: " + newShipmentStatus, e);
        throw e;
      }
    }
    return model;
  }

  private boolean autoUpdateOfShipmentEnabled(Long domainId, IShipment shipment,
                                              ShipmentStatus newShipmentStatus) {
    Long dId = domainId != null ? domainId : shipment.getDomainId();
    OrdersConfig oc = DomainConfig.getInstance(dId).getOrdersConfig();
    if (newShipmentStatus == ShipmentStatus.SHIPPED) {
      if (oc.markOrderAsShippedOnPickup()) {
        return true;
      }
    } else if (newShipmentStatus == ShipmentStatus.FULFILLED) {
      if (oc.markOrderAsFulfilledOnDelivery()) {
        return true;
      }
    }
    return false;
  }

  private void updateShipmentStatus(IShipment shipment, ShipmentStatus newShipmentStatus)
      throws LogiException {
    validateShipmentStatusChange(shipment.getStatus(), newShipmentStatus);
    ResourceBundle backendMessages = Resources.getBundle(Locale.ENGLISH);
    if(newShipmentStatus == ShipmentStatus.SHIPPED) {
      shipmentService.updateShipmentStatus(shipment.getShipmentId(), newShipmentStatus,
          backendMessages.getString("shipment.shipped.tsp"), Constants.SYSTEM_USER_ID,
          "Delivery request status changed", true, null, SourceConstants.SYSTEM, true);
    } else if(newShipmentStatus == ShipmentStatus.FULFILLED) {
      shipmentService.fulfillShipment(shipment.getShipmentId(), Constants.SYSTEM_USER_ID,
          backendMessages.getString("shipment.delivered.tsp"), SourceConstants.WEB);
    }
  }

  private ShipmentStatus getNewShipmentStatus(
      DeliveryRequestStatus drStatus, ShipmentStatus shipmentStatus) {
    switch (drStatus) {
      case PICKED:
        return ShipmentStatus.SHIPPED;
      case DELIVERED:
        return ShipmentStatus.FULFILLED;
      default:
        return shipmentStatus;
    }
  }

  private void updateShipmentETA(IShipment shipment, Date eta)
      throws LogiException {
    if(eta == null) {
      return;
    }
    Map<String, String> metadata = new HashMap<>();
    metadata.put("date", new SimpleDateFormat(Constants.DATE_FORMAT).format(eta));
    shipmentService
        .updateShipmentData(metadata, null, shipment.getShipmentId(), Constants.SYSTEM_USER_ID);
  }

  private void validateShipmentStatusChange(ShipmentStatus currentStatus,
                                            ShipmentStatus newStatus) throws ServiceException {
    if (currentStatus == ShipmentStatus.PENDING || currentStatus == ShipmentStatus.CONFIRMED
        || currentStatus == ShipmentStatus.READY_FOR_DISPATCH) {
      if (newStatus == ShipmentStatus.SHIPPED) {
        return;
      }
    } else if (currentStatus == ShipmentStatus.SHIPPED
               && newStatus == ShipmentStatus.FULFILLED) {
        return;
    }
    throw new ServiceException("Invalid shipment state change");
  }
}