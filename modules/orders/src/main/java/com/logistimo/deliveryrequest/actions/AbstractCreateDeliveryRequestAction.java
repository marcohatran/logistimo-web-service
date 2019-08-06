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

import com.logistimo.config.models.DomainConfig;
import com.logistimo.config.models.OrdersConfig;
import com.logistimo.deliveryrequest.entities.DeliveryRequest;
import com.logistimo.deliveryrequest.fleet.model.orders.DeliveryRequestMapper;
import com.logistimo.deliveryrequest.models.DeliveryRequestModel;
import com.logistimo.deliveryrequest.models.DeliveryRequestStatus;
import com.logistimo.deliveryrequest.models.DeliveryRequestUpdateWrapper;
import com.logistimo.deliveryrequest.repository.DeliveryRequestRepository;
import com.logistimo.deliveryrequest.validator.NoActiveDeliveryRequestValidator;
import com.logistimo.entities.auth.EntityAuthoriser;
import com.logistimo.exception.ForbiddenAccessException;
import com.logistimo.exception.LogiException;
import com.logistimo.logger.XLog;
import com.logistimo.security.SecureUserDetails;
import com.logistimo.services.Resources;
import com.logistimo.services.ServiceException;
import com.logistimo.transporters.actions.GetTransporterApiMetadataAction;
import com.logistimo.transporters.entity.TransporterApiMetadata;

import org.springframework.beans.factory.annotation.Autowired;

import java.util.Date;
import java.util.ResourceBundle;

/**
 * Created by kumargaurav on 06/02/19.
 */
public abstract class AbstractCreateDeliveryRequestAction implements ICreateDeliveryRequestAction {

  private static final XLog log = XLog.getLog(AbstractCreateDeliveryRequestAction.class);

  private DeliveryRequestMapper deliveryRequestMapper;
  private DeliveryRequestRepository requestRepository;
  private GetTransporterApiMetadataAction getTransporterApiMetadataAction;
  private NoActiveDeliveryRequestValidator noActiveDeliveryRequestValidator;

  @Autowired
  public void setDeliveryRequestMapper(DeliveryRequestMapper deliveryRequestMapper) {
    this.deliveryRequestMapper = deliveryRequestMapper;
  }

  @Autowired
  public void setRequestRepository(DeliveryRequestRepository requestRepository) {
    this.requestRepository = requestRepository;
  }

  @Autowired
  public void setGetTransporterApiMetadataAction(
      GetTransporterApiMetadataAction getTransporterApiMetadataAction) {
    this.getTransporterApiMetadataAction = getTransporterApiMetadataAction;
  }

  @Autowired
  public void setNoActiveDeliveryRequestValidator(
      NoActiveDeliveryRequestValidator noActiveDeliveryRequestValidator) {
    this.noActiveDeliveryRequestValidator = noActiveDeliveryRequestValidator;
  }

  /**
   * Implement this method to call respective transporter service
   * @param model DeliveryRequestModel: Map this to provider's delivery request model
   * @param transporterApiMetadata Configurations to connect to third party APIs
   * @return
   */
  protected abstract DeliveryRequestUpdateWrapper createDeliveryRequestWithTransporter(
      DeliveryRequestModel model, TransporterApiMetadata transporterApiMetadata);

  @Override
  public DeliveryRequestUpdateWrapper invoke(SecureUserDetails user, Long dId,
                                             DeliveryRequestModel model) throws LogiException {
    Date now = new Date();
    validatePermissions(user, dId, model.getShipper().getKid(), model.getReceiver().getKid());
    noActiveDeliveryRequestValidator.validate(model.getShipmentId());
    updateAuditParams(user.getUsername(), model, now);
    DeliveryRequest entity = deliveryRequestMapper.mapToEntity(model);
    entity = persist(user.getUsername(), entity);
    DeliveryRequestUpdateWrapper drUpdateModel;
    try {
      drUpdateModel = createDeliveryRequestWithTransporter(model,
          getTransporterApiMetadataAction.invoke(entity.getTransporterId()));
    } catch (Exception e) {
      handleDeliveryRequestCreationError(e, entity);
      throw new ServiceException("O021", new Object[]{});
    }
    handleDeliveryRequestCreationSuccess(drUpdateModel, entity);
    return drUpdateModel;
  }

  private void updateAuditParams(String userId, DeliveryRequestModel model, Date now) {
    model.setCreatedBy(userId);
    model.setUpdatedBy(userId);
    model.setCreatedOn(now);
    model.setUpdatedOn(now);
  }

  private void validatePermissions(SecureUserDetails user, Long dId,
                                   Long vendorKid, Long customerKid) throws ServiceException {
    if(EntityAuthoriser.authoriseEntityPerm(vendorKid, user.getRole(), user
        .getUsername(), dId) > 1) {
        // access to vendor entity
        return;
    } else if((EntityAuthoriser.authoriseEntityPerm(customerKid,
        user.getRole(), user.getUsername(), dId) > 1)) {
      // access to customer entity
      OrdersConfig oc = DomainConfig.getInstance(dId).getOrdersConfig();
      if(!oc.isDeliveryRequestByCustomerDisabled()) {
        // config to disable delivery request by customer is disabled
        return;
      }
    }
    ResourceBundle backendMessages = Resources.getBundle(user.getLocale());
    throw new ForbiddenAccessException(backendMessages.getString("permission.denied"));
  }

  private void handleDeliveryRequestCreationSuccess(DeliveryRequestUpdateWrapper updModel,
                                                    DeliveryRequest deliveryRequest) {
    updateDeliveryRequestInfoOnCreation(updModel, deliveryRequest);
  }

  private void updateDeliveryRequestInfoOnCreation(
      DeliveryRequestUpdateWrapper updateWrapper, DeliveryRequest deliveryRequest) {
    deliveryRequest.setStatus(DeliveryRequestStatus.fromValue(updateWrapper.getStatus()));
    deliveryRequest.setEta(updateWrapper.getEta());
    deliveryRequest.setTrackingId(updateWrapper.getDeliveryRequestTrackingId());
    deliveryRequest.setUpdatedAt(new Date());
    requestRepository.save(deliveryRequest);
  }

  private void handleDeliveryRequestCreationError(Throwable t, DeliveryRequest deliveryRequest) {
    deliveryRequest.setStatus(DeliveryRequestStatus.CANCELLED);
    requestRepository.save(deliveryRequest);
    log.severe("Error while creating delivery request with TSP", t);
  }

  private DeliveryRequest persist(String username, DeliveryRequest entity) {
    Date now = new Date();
    entity.setCreatedAt(now);
    entity.setCreatedBy(username);
    entity.setUpdatedAt(now);
    entity.setUpdatedBy(username);
    entity.setStatus(DeliveryRequestStatus.DRAFT);
    return requestRepository.save(entity);
  }
}
