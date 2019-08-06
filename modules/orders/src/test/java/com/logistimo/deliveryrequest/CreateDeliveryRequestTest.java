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

package com.logistimo.deliveryrequest;

import com.logistimo.auth.GenericAuthoriser;
import com.logistimo.auth.SecurityConstants;
import com.logistimo.config.models.DomainConfig;
import com.logistimo.config.models.OrdersConfig;
import com.logistimo.constants.SourceConstants;
import com.logistimo.deliveryrequest.actions.AbstractCreateDeliveryRequestAction;
import com.logistimo.deliveryrequest.entities.DeliveryRequest;
import com.logistimo.deliveryrequest.fleet.model.orders.DeliveryRequestMapper;
import com.logistimo.deliveryrequest.models.DeliveryRequestModel;
import com.logistimo.deliveryrequest.models.DeliveryRequestStatus;
import com.logistimo.deliveryrequest.models.DeliveryRequestUpdateWrapper;
import com.logistimo.deliveryrequest.models.Place;
import com.logistimo.deliveryrequest.models.TrackingDetails;
import com.logistimo.deliveryrequest.repository.DeliveryRequestRepository;
import com.logistimo.deliveryrequest.validator.NoActiveDeliveryRequestValidator;
import com.logistimo.entities.auth.EntityAuthoriser;
import com.logistimo.exception.ForbiddenAccessException;
import com.logistimo.exception.LogiException;
import com.logistimo.models.shipments.ConsignmentModel;
import com.logistimo.models.shipments.ShipmentModel;
import com.logistimo.orders.entity.IOrder;
import com.logistimo.orders.entity.Order;
import com.logistimo.orders.service.OrderManagementService;
import com.logistimo.security.SecureUserDetails;
import com.logistimo.services.Resources;
import com.logistimo.services.ServiceException;
import com.logistimo.shipments.ShipmentStatus;
import com.logistimo.shipments.service.IShipmentService;
import com.logistimo.transporters.actions.GetTransporterApiMetadataAction;
import com.logistimo.transporters.entity.TransporterApiMetadata;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentMatcher;
import org.mockito.Mock;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

import java.util.Collections;
import java.util.Date;
import java.util.Enumeration;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.ResourceBundle;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Created by chandrakant on 08/07/19.
 */
@RunWith(PowerMockRunner.class)
@PrepareForTest( {EntityAuthoriser.class, Resources.class, DomainConfig.class} )
public class CreateDeliveryRequestTest {

  private AbstractCreateDeliveryRequestAction createDeliveryRequestAction;
  @Mock
  private IShipmentService shipmentService;
  @Mock
  private OrderManagementService orderService;
  @Mock
  private DeliveryRequestMapper deliveryRequestMapper;
  @Mock
  private DeliveryRequestRepository requestRepository;
  @Mock
  private GetTransporterApiMetadataAction getTransporterApiMetadataAction;
  @Mock
  private NoActiveDeliveryRequestValidator noActiveDeliveryRequestValidator;

  private ResourceBundle backendMessages = new ResourceBundle() {
    @Override
    protected Object handleGetObject(String key) {
      return "fake_translated_value";
    }

    @Override
    public Enumeration<String> getKeys() {
      return Collections.emptyEnumeration();
    }
  };

  @Before
  public void setup() throws ServiceException {
    createDeliveryRequestAction = new AbstractCreateDeliveryRequestAction() {
      @Override
      protected DeliveryRequestUpdateWrapper createDeliveryRequestWithTransporter(
          DeliveryRequestModel deliveryRequest, TransporterApiMetadata transporterApiMetadata) {
        DeliveryRequestUpdateWrapper model = new DeliveryRequestUpdateWrapper();
        model.setDeliveryRequestTrackingId("transporter-tracking-id");
        model.setShipmentTrackingId("shipment-tracking-id");
        model.setStatus("dr_cf");
        model.setStatusUpdatedOn(new Date());
        return model;
      }
    };
    createDeliveryRequestAction.setDeliveryRequestMapper(deliveryRequestMapper);
    createDeliveryRequestAction.setRequestRepository(requestRepository);
    createDeliveryRequestAction.setGetTransporterApiMetadataAction(getTransporterApiMetadataAction);
    createDeliveryRequestAction.setNoActiveDeliveryRequestValidator(
        noActiveDeliveryRequestValidator);
    PowerMockito.mockStatic(DomainConfig.class);
    PowerMockito.mockStatic(EntityAuthoriser.class);
    PowerMockito.mockStatic(Resources.class);
  }

  private DeliveryRequestModel buildDeliveryRequest() {
    DeliveryRequestModel model = new DeliveryRequestModel();
    model.setOrderId(123456l);
    model.setPickupReadyBy(new Date());
    model.setShipper(Place.builder().kid(3333l).name("Shipper place").build());
    model.setReceiver(Place.builder().kid(2222l).name("Receiver place").build());
    model.setConsignment(ConsignmentModel.builder().packageCount(2).value(1000.0)
        .declaration("some").weightInKg(100.0).build());
    model.setCategoryId("12");
    TrackingDetails trackingDetails = new TrackingDetails(null, 10l, "transporter", "normal", null);
    model.setTrackingDetails(trackingDetails);
    return model;
  }

  @Test
  public void creationFromOrderTest() throws LogiException {
    SecureUserDetails user = new SecureUserDetails();
    user.setUsername("test-user");
    user.setLocale(Locale.ENGLISH);
    user.setRole(SecurityConstants.ROLE_SERVICEMANAGER);
    PowerMockito.when(EntityAuthoriser.authoriseEntityPerm(3333l, SecurityConstants
        .ROLE_SERVICEMANAGER, "test-user", 1l)).thenReturn(GenericAuthoriser.MANAGE_MASTER_DATA);
    IOrder order = buildOrder(IOrder.READY_FOR_DISPATCH);
    DeliveryRequestModel model = buildDeliveryRequest();
    when(orderService.getOrder(123456l)).thenReturn(order);
    DeliveryRequest dr = mock(DeliveryRequest.class);
    when(shipmentService.createShipment(any(), eq(SourceConstants.WEB), eq(true), any()))
        .thenReturn("123456-1");
    when(deliveryRequestMapper.mapToEntity(any())).thenReturn(dr);
    when(deliveryRequestMapper.mapFromEntity(any())).thenReturn(model);
    when(requestRepository.save(dr)).thenReturn(dr);
    PowerMockito.when(Resources.getBundle(Locale.ENGLISH)).thenReturn(backendMessages);
    createDeliveryRequestAction.invoke(user, 1l, model);
    verify(dr).setTrackingId("transporter-tracking-id");
    verify(dr).setStatus(DeliveryRequestStatus.CONFIRMED);
  }

  @Test(expected = ForbiddenAccessException.class)
  public void creationFromOrderWithoutVendorAndCustomerPermissionTest() throws LogiException {
    SecureUserDetails user = new SecureUserDetails();
    user.setUsername("test-user");
    user.setRole(SecurityConstants.ROLE_SERVICEMANAGER);
    user.setLocale(Locale.ENGLISH);
    PowerMockito.when(Resources.getBundle(Locale.ENGLISH)).thenReturn(backendMessages);
    PowerMockito.when(EntityAuthoriser.authoriseEntityPerm(3333l, SecurityConstants
        .ROLE_SERVICEMANAGER, "test-user", 1l)).thenReturn(GenericAuthoriser.NO_ACCESS);
    PowerMockito.when(EntityAuthoriser.authoriseEntityPerm(2222l, SecurityConstants
        .ROLE_SERVICEMANAGER, "test-user", 1l)).thenReturn(GenericAuthoriser.NO_ACCESS);
    DeliveryRequestModel model = buildDeliveryRequest();
    createDeliveryRequestAction.invoke(user, 1l, model);
  }

  @Test(expected = ForbiddenAccessException.class)
  public void creationFromOrderWithoutVendorPermissionTest() throws LogiException {
    SecureUserDetails user = new SecureUserDetails();
    user.setUsername("test-user");
    user.setRole(SecurityConstants.ROLE_SERVICEMANAGER);
    user.setLocale(Locale.ENGLISH);
    PowerMockito.when(Resources.getBundle(Locale.ENGLISH)).thenReturn(backendMessages);
    DomainConfig dc = new DomainConfig();
    OrdersConfig oc = new OrdersConfig();
    oc.setDeliveryRequestByCustomerDisabled(true);
    dc.setOrdersConfig(oc);
    PowerMockito.when(DomainConfig.getInstance(anyLong())).thenReturn(dc);
    PowerMockito.when(EntityAuthoriser.authoriseEntityPerm(3333l, SecurityConstants
        .ROLE_SERVICEMANAGER, "test-user", 1l)).thenReturn(GenericAuthoriser.NO_ACCESS);
    PowerMockito.when(EntityAuthoriser.authoriseEntityPerm(2222l, SecurityConstants
        .ROLE_SERVICEMANAGER, "test-user", 1l)).thenReturn(GenericAuthoriser.MANAGE_MASTER_DATA);
    DeliveryRequestModel model = buildDeliveryRequest();
    createDeliveryRequestAction.invoke(user, 1l, model);
  }

  @Test
  public void creationFromOrderWithoutVendorAndCustomerConfigPermissionTest() throws LogiException {
    SecureUserDetails user = new SecureUserDetails();
    user.setUsername("test-user");
    user.setRole(SecurityConstants.ROLE_SERVICEMANAGER);
    user.setLocale(Locale.ENGLISH);
    PowerMockito.when(Resources.getBundle(Locale.ENGLISH)).thenReturn(backendMessages);
    DomainConfig dc = new DomainConfig();
    OrdersConfig oc = new OrdersConfig();
    oc.setDeliveryRequestByCustomerDisabled(false);
    dc.setOrdersConfig(oc);
    PowerMockito.when(DomainConfig.getInstance(anyLong())).thenReturn(dc);
    PowerMockito.when(EntityAuthoriser.authoriseEntityPerm(3333l, SecurityConstants
        .ROLE_SERVICEMANAGER, "test-user", 1l)).thenReturn(GenericAuthoriser.NO_ACCESS);
    PowerMockito.when(EntityAuthoriser.authoriseEntityPerm(2222l, SecurityConstants
        .ROLE_SERVICEMANAGER, "test-user", 1l)).thenReturn(GenericAuthoriser.MANAGE_MASTER_DATA);
    IOrder order = buildOrder(IOrder.READY_FOR_DISPATCH);
    DeliveryRequestModel model = buildDeliveryRequest();
    when(orderService.getOrder(123456l)).thenReturn(order);
    DeliveryRequest dr = mock(DeliveryRequest.class);
    when(shipmentService.createShipment(any(), eq(SourceConstants.WEB), eq(true), any()))
        .thenReturn("123456-1");
    when(deliveryRequestMapper.mapToEntity(any())).thenReturn(dr);
    when(deliveryRequestMapper.mapFromEntity(any())).thenReturn(model);
    when(requestRepository.save(dr)).thenReturn(dr);
    PowerMockito.when(Resources.getBundle(Locale.ENGLISH)).thenReturn(backendMessages);
    createDeliveryRequestAction.invoke(user, 1l, model);
    verify(dr).setTrackingId("transporter-tracking-id");
    verify(dr).setStatus(DeliveryRequestStatus.CONFIRMED);
  }

  private IOrder buildOrder(String status) {
    IOrder order = new Order();
    order.setStatus(status);
    return order;
  }

  private ShipmentModel buildShipmentModel(ShipmentStatus status) {
    ShipmentModel model = new ShipmentModel();
    model.status = status;
    return model;
  }

  private ArgumentMatcher<Map<String, String>>
        getShipmentUpdateDataMatcher(String shipmentTrackingId) {
    return argument -> Objects.equals(argument.get("tId"), shipmentTrackingId);
  }
}