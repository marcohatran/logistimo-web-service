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

import com.logistimo.deliveryrequest.models.DeliveryRequestModel;
import com.logistimo.deliveryrequest.models.DeliveryRequestUpdateWrapper;
import com.logistimo.deliveryrequest.models.Place;
import com.logistimo.deliveryrequest.models.TrackingDetails;
import com.logistimo.entities.entity.IKiosk;
import com.logistimo.entities.service.EntitiesService;
import com.logistimo.logger.XLog;
import com.logistimo.services.ServiceException;
import com.logistimo.transporters.actions.GetTransporterDetailsAction;
import com.logistimo.transporters.model.TransporterDetailsModel;
import com.logistimo.users.entity.IUserAccount;

import org.apache.commons.lang.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.List;

/**
 * Created by kumargaurav on 26/02/19.
 */
@Component
public class FleetOrderMapper extends ExtDeliveryRequestMapper<Order> {

  private static final XLog log = XLog.getLog(FleetOrderMapper.class);

  public static final String DELIVERY_TYPE_JALDI = "jaldi";
  public static final String DELIVERY_TYPE_NORMAL = "normal";
  public static final String PRICE_SURCHARGE_CONFIG_KEY = "price_surcharge";

  private EntitiesService entitiesService;
  private final GetTransporterDetailsAction getTransportersAction;

  @Autowired
  public FleetOrderMapper(EntitiesService entitiesService,
                          FleetOrderStatusMapper orderStatusMapper,
                          GetTransporterDetailsAction getTransportersAction) {
    super(orderStatusMapper);
    this.entitiesService = entitiesService;
    this.getTransportersAction = getTransportersAction;
  }

  @Override
  public void populateDeliveryRequestModel(Order order, DeliveryRequestUpdateWrapper model) {
    model.setEta(order.getEta());
    model.setDeliveryRequestTrackingId(order.getTrackingId());
    model.setShipmentTrackingId(order.getOrderRefNum());
    model.setStatusUpdatedOn(order.getStsUpdatedOn());
  }

  @Override
  public Order mapToExtDeliveryRequest(DeliveryRequestModel model) {
    Order order = new Order();
    order.setODt(new Date());
    DateSlot ewt = new DateSlot();
    ewt.setStart(model.getPickupReadyBy());
    order.setEptW(ewt);
    //consignment
    Consignment consignment = new Consignment();
    Volume vol = new Volume();
    vol.setBreadth(model.getConsignment().getDimension().getWidthInInches());
    vol.setHeight(model.getConsignment().getDimension().getHeightInInches());
    vol.setLength(model.getConsignment().getDimension().getLengthInInches());
    consignment.setVolume(vol);
    consignment.setCategory(model.getCategoryId());
    consignment.setItemsSmall(model.getConsignment().getPackageCount());
    consignment.setValueDeclared(model.getConsignment().getValue());
    consignment.setDescription(model.getConsignment().getDeclaration());
    Collection<Double> weights = new ArrayList<>();
    weights.add(model.getConsignment().getWeightInKg());
    consignment.setWeightDeclared(weights);
    order.setConsgt(consignment);
    //pricing
    Pricing prc = new Pricing();
    order.setPrc(prc);
    order.getPrc().setTimeSurcharge(getTimeSurchargeValue(model.getTrackingDetails()));
    populatePlacesAndContactDetails(model, order);
    if(StringUtils.isNotBlank(model.getInstructions())) {
      order.setInstructions(Collections.singleton(new ExtRemark(model.getInstructions())));
    }
    return order;
  }



  private Double getTimeSurchargeValue(TrackingDetails trackingDetails) {
    if(DELIVERY_TYPE_JALDI.equalsIgnoreCase(trackingDetails.getDeliveryType())) {
      return 1.0;
    } else if(DELIVERY_TYPE_NORMAL.equalsIgnoreCase(trackingDetails.getDeliveryType())) {
      return 0.0;
    } else {
      Double surcharge = 0.0;
      TransporterDetailsModel model = getTransportersAction.invoke(trackingDetails
          .getTransporterId());
      String priceSurcharge = model.getAttributes() != null ?
          model.getAttributes().get(PRICE_SURCHARGE_CONFIG_KEY) : null;
      if(StringUtils.isNotBlank(priceSurcharge)) {
        surcharge = Double.parseDouble(priceSurcharge);
      }
      return surcharge;
    }
  }

  private void populatePlacesAndContactDetails(DeliveryRequestModel model, Order order) {
    long shipper = model.getShipper().getKid();
    long receiver = model.getReceiver().getKid();
    List<Long> kiosks = Arrays.asList(shipper, receiver);
    List<IKiosk> kiosksList;
    try {
      kiosksList = entitiesService.getKiosksByIds(kiosks);
    } catch (ServiceException se) {
      log.warn("Issue with getting kiosk details for kiosks {0} {1}", shipper, receiver, se);
      throw new RuntimeException(se);
    }
    for(IKiosk k : kiosksList) {
      if (k.getKioskId().equals(shipper)) {
        populatePlaceDetails(model.getShipper(), k);
        order.setShipper(mapToLocation(k));
      } else if (k.getKioskId().equals(receiver)) {
        populatePlaceDetails(model.getReceiver(), k);
        order.setReceiver(mapToLocation(k));
      }
    }
  }

  private void populatePlaceDetails(Place place, IKiosk k) {
    place.setName(k.getName());
  }

  private Location mapToLocation(IKiosk kiosk) {
    Location loc = new Location();
    loc.setShortDesc(kiosk.getName());
    Address address = new Address();
    address.setCountryCode(kiosk.getCountry());
    address.setState(kiosk.getState());
    address.setDistrict(kiosk.getDistrict());
    address.setCity(kiosk.getCity());
    address.setLocality(kiosk.getCity());
    address.setPostalCode(kiosk.getPinCode());
    //geo location
    GeoLocation geoLocation = new GeoLocation(kiosk.getLatitude(), kiosk.getLongitude());
    loc.setAddress(address);
    loc.setGeoLocation(geoLocation);
    //contact
    IUserAccount u = kiosk.getUser();
    Contact contact =  new Contact();
    contact.setFirstName(u.getFirstName());
    contact.setLastName(u.getLastName());
    contact.setPhoneMobile(u.getMobilePhoneNumber());
    loc.setContact(contact);
    return loc;
  }
}
