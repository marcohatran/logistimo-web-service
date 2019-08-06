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

package com.logistimo.deliveryrequest.actions;

import com.logistimo.deliveryrequest.entities.DeliveryRequest;
import com.logistimo.deliveryrequest.fleet.model.orders.DeliveryRequestMapper;
import com.logistimo.deliveryrequest.models.DeliveryRequestModel;
import com.logistimo.deliveryrequest.models.DeliveryRequestStatus;
import com.logistimo.deliveryrequest.repository.DeliveryRequestRepository;
import com.logistimo.services.ObjectNotFoundException;
import com.logistimo.services.ServiceException;
import com.logistimo.transporters.actions.GetTransporterApiMetadataAction;
import com.logistimo.transporters.entity.TransporterApiMetadata;

import org.springframework.beans.factory.annotation.Autowired;

import java.util.Date;

/**
 * Created by chandrakant on 13/06/19.
 */
public abstract class AbstractCancelDeliveryRequestAction implements
    ICancelDeliveryRequestAction {

  private DeliveryRequestRepository repository;
  private GetTransporterApiMetadataAction getTransporterApiMetadataAction;
  private DeliveryRequestMapper deliveryRequestMapper;

  @Autowired
  public final void setRepository(DeliveryRequestRepository repository) {
    this.repository = repository;
  }

  @Autowired
  public final void setGetTransporterApiMetadataAction(
      GetTransporterApiMetadataAction getTransporterApiMetadataAction) {
    this.getTransporterApiMetadataAction = getTransporterApiMetadataAction;
  }

  @Autowired
  public void setDeliveryRequestMapper(DeliveryRequestMapper deliveryRequestMapper) {
    this.deliveryRequestMapper = deliveryRequestMapper;
  }

  /**
   * Method to cancel the delivery request with the transport service provider APIs
   * @param trackingId trackingId provided by the transport service provider
   * @param transporterApiMetadata metadata to connect with TSP APIs
   */
  protected abstract void cancelWithTransporter(String trackingId,
                                                TransporterApiMetadata transporterApiMetadata);

  public DeliveryRequestModel invoke(String userId, Long id) throws ServiceException {
    DeliveryRequest deliveryRequest = repository.findOne(id);
    if (deliveryRequest == null) {
      throw new ObjectNotFoundException("No delivery found with given Id");
    }
    cancelWithTransporter(deliveryRequest.getTrackingId(),
        getTransporterApiMetadataAction.invoke(deliveryRequest.getTransporterId()));
    deliveryRequest.setStatus(DeliveryRequestStatus.CANCELLED);
    deliveryRequest.setUpdatedAt(new Date());
    deliveryRequest.setUpdatedBy(userId);
    deliveryRequest = repository.save(deliveryRequest);
    return deliveryRequestMapper.mapFromEntity(deliveryRequest);
  }
}