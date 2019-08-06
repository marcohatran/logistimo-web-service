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
import com.logistimo.deliveryrequest.models.DeliveryRequestStatus;
import com.logistimo.deliveryrequest.models.DeliveryRequestUpdateWrapper;
import com.logistimo.deliveryrequest.repository.DeliveryRequestRepository;
import com.logistimo.services.ServiceException;
import com.logistimo.transporters.actions.GetTransporterApiMetadataAction;
import com.logistimo.transporters.entity.TransporterApiMetadata;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.Date;

/**
 * Created by chandrakant on 13/06/19.
 */
public abstract class AbstractSyncDeliveryRequestAction implements ISyncDeliveryRequestAction {

  @Autowired
  private DeliveryRequestRepository deliveryRequestRepository;

  @Autowired
  private GetTransporterApiMetadataAction getTransporterApiMetadataAction;

  protected abstract DeliveryRequestUpdateWrapper syncWithTransporter(String trackingId,
                                                           TransporterApiMetadata metadata);

  private void populateEntity(String userId, DeliveryRequest entity, DeliveryRequestUpdateWrapper
      deliveryRequestUpdateWrapper) {
    entity.setEta(deliveryRequestUpdateWrapper.getEta());
    entity.setStatus(DeliveryRequestStatus.fromValue(deliveryRequestUpdateWrapper.getStatus()));
    entity.setStatusUpdatedAt(deliveryRequestUpdateWrapper.getStatusUpdatedOn());
    entity.setUpdatedAt(new Date());
    entity.setUpdatedBy(userId);
  }

  @Override
  public DeliveryRequest invoke(String userId, Long id) throws ServiceException {
    DeliveryRequest entity = deliveryRequestRepository.findOne(id);
    DeliveryRequestUpdateWrapper info = syncWithTransporter(entity.getTrackingId(),
        getTransporterApiMetadataAction.invoke(entity.getTransporterId()));
    populateEntity(userId, entity, info);
    deliveryRequestRepository.save(entity);
    return entity;
  }

  @Override
  public DeliveryRequest invoke(String userId,
                                DeliveryRequestUpdateWrapper deliveryRequestUpdateWrapper)
      throws ServiceException {
    DeliveryRequest entity = deliveryRequestRepository.findOneByTrackingId
        (deliveryRequestUpdateWrapper.getDeliveryRequestTrackingId());
    populateEntity(userId, entity, deliveryRequestUpdateWrapper);
    deliveryRequestRepository.save(entity);
    return entity;
  }
}