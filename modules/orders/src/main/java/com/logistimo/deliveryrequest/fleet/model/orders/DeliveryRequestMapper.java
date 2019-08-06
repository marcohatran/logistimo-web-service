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

import com.logistimo.deliveryrequest.actions.IConstructTrackingUrlAction;
import com.logistimo.deliveryrequest.entities.DeliveryRequest;
import com.logistimo.deliveryrequest.models.DeliveryRequestModel;
import com.logistimo.deliveryrequest.models.DeliveryRequestStatus;
import com.logistimo.deliveryrequest.models.Place;
import com.logistimo.deliveryrequest.models.TrackingDetails;
import com.logistimo.entities.entity.IKiosk;
import com.logistimo.entities.service.EntitiesService;
import com.logistimo.logger.XLog;
import com.logistimo.mapper.Mapper;
import com.logistimo.models.shipments.ConsignmentModel;
import com.logistimo.models.shipments.PackageDimensions;
import com.logistimo.services.ServiceException;
import com.logistimo.transporters.actions.GetTransporterDetailsAction;
import com.logistimo.transporters.model.TransporterDetailsModel;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

import lombok.AllArgsConstructor;

/**
 * Created by chandrakant on 02/05/19.
 */
@Component
@AllArgsConstructor(onConstructor = @__(@Autowired))
public class DeliveryRequestMapper implements Mapper<DeliveryRequestModel, DeliveryRequest> {

  private GetTransporterDetailsAction getTransporterDetailsAction;
  private EntitiesService entitiesService;
  private IConstructTrackingUrlAction constructTrackingUrlAction;

  private static final XLog xLogger = XLog.getLog(DeliveryRequestMapper.class);

  @Override
  public DeliveryRequestModel mapFromEntity(DeliveryRequest dr) {
    TransporterDetailsModel transporter = getTransporterDetailsAction.invoke(dr.getTransporterId());
    TrackingDetails trackingDetails = new TrackingDetails(
        dr.getTrackingId(),
        dr.getTransporterId(),
        transporter.getName(),
        dr.getDeliveryType(),
        transporter.getPhoneNumber());

    ConsignmentModel consignment = ConsignmentModel.builder()
        .declaration(dr.getDescription())
        .dimension(new PackageDimensions(dr.getLength(), dr.getBreadth(), dr.getHeight()))
        .packageCount(dr.getNumberOfPackages())
        .value(dr.getValue())
        .weightInKg(dr.getWeight())
        .build();

    try {
      DeliveryRequestModel model = new DeliveryRequestModel();
      model.setOrderId(dr.getOrderId());
      model.setDomainId(dr.getDomainId());
      model.setShipper(buildPlace(dr.getShipperKid()));
      model.setReceiver(buildPlace(dr.getReceiverKid()));
      model.setInstructions(dr.getInstructions());
      model.setStatusUpdatedOn(dr.getStatusUpdatedAt());
      model.setTpRefNum(dr.getTpRefNum());
      model.setConsignment(consignment);
      model.setPickupReadyBy(dr.getPickupReadyBy());
      model.setTrackingDetails(trackingDetails);
      model.setId(dr.getId());
      model.setShipmentId(dr.getShipmentId());
      model.setCreatedBy(dr.getCreatedBy());
      model.setCreatedOn(dr.getCreatedAt());
      model.setStatus(dr.getStatus().value());
      model.setEta(dr.getEta());
      model.setTrackingURL(constructTrackingUrlAction.invoke(dr.getTrackingId(), transporter));
      return model;
    } catch (Exception e) {
      xLogger.severe("Error while building delivery request model", e);
    }
    return null;
  }

  @Override
  public void populateEntity(DeliveryRequest deliveryRequest,
                             DeliveryRequestModel model) {
    deliveryRequest.setShipmentId(model.getShipmentId());
    deliveryRequest.setOrderId(model.getOrderId());
    deliveryRequest.setDomainId(model.getDomainId());
    deliveryRequest.setShipperKid(model.getShipper().getKid());
    deliveryRequest.setReceiverKid(model.getReceiver().getKid());
    deliveryRequest.setNumberOfPackages(model.getConsignment().getPackageCount());
    deliveryRequest.setPickupReadyBy(model.getPickupReadyBy());
    deliveryRequest.setWeight(model.getConsignment().getWeightInKg());
    if(model.getConsignment().getDimension() != null) {
      deliveryRequest.setLength(model.getConsignment().getDimension().getLengthInInches());
      deliveryRequest.setBreadth(model.getConsignment().getDimension().getWidthInInches());
      deliveryRequest.setHeight(model.getConsignment().getDimension().getHeightInInches());
    }
    deliveryRequest.setValue(model.getConsignment().getValue());
    deliveryRequest.setEta(model.getEta());
    deliveryRequest.setStatus(DeliveryRequestStatus.fromValue(model.getStatus()));
    deliveryRequest.setStatusUpdatedAt(model.getStatusUpdatedOn());
    deliveryRequest.setDescription(model.getConsignment().getDeclaration());
    deliveryRequest.setInstructions(model.getInstructions());
    if(model.getTrackingDetails() != null) {
      deliveryRequest.setTransporterId(model.getTrackingDetails().getTransporterId());
      deliveryRequest.setTrackingId(model.getTrackingDetails().getTrackingId());
      deliveryRequest.setDeliveryType(model.getTrackingDetails().getDeliveryType());
    }
    deliveryRequest.setCategoryId(model.getCategoryId());
    deliveryRequest.setCreatedAt(model.getCreatedOn());
    deliveryRequest.setCreatedBy(model.getCreatedBy());
    deliveryRequest.setUpdatedAt(model.getUpdatedOn());
    deliveryRequest.setUpdatedBy(model.getUpdatedBy());
    deliveryRequest.setTpRefNum(model.getTpRefNum());
  }

  private Place buildPlace(Long kioskId) throws ServiceException {
    IKiosk kiosk = entitiesService.getKiosk(kioskId);
    Location location = new Location();
    location.setGeoLocation(new GeoLocation(kiosk.getLatitude(), kiosk.getLongitude()));
    return Place.builder()
        .name(kiosk.getName())
        .kid(kiosk.getKioskId())
        .build();
  }

  @Override
  public DeliveryRequest mapToEntity(DeliveryRequestModel model) {
    DeliveryRequest entity = new DeliveryRequest();
    populateEntity(entity, model);
    return entity;
  }
}