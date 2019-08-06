/*
 * Copyright © 2017 Logistimo.
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

package com.logistimo.shipments.service;

import com.logistimo.api.patch.Patch;
import com.logistimo.conversations.entity.IMessage;
import com.logistimo.deliveryrequest.models.DeliveryRequestUpdateWrapper;
import com.logistimo.exception.LogiException;
import com.logistimo.exception.ValidationException;
import com.logistimo.models.ResponseModel;
import com.logistimo.models.shipments.ConsignmentModel;
import com.logistimo.models.shipments.ShipmentMaterialsModel;
import com.logistimo.models.shipments.ShipmentModel;
import com.logistimo.orders.models.PDFResponseModel;
import com.logistimo.pagination.Results;
import com.logistimo.services.ObjectNotFoundException;
import com.logistimo.services.ServiceException;
import com.logistimo.shipments.FulfilledQuantityModel;
import com.logistimo.shipments.ShipmentStatus;
import com.logistimo.shipments.entity.IConsignment;
import com.logistimo.shipments.entity.IShipment;

import java.io.IOException;
import java.math.BigDecimal;
import java.util.Collection;
import java.util.Date;
import java.util.List;
import java.util.Map;

import javax.jdo.PersistenceManager;

/**
 * Created by Mohan Raja on 29/09/16
 */
public interface IShipmentService {

  String CNSGNMNT_PACKAGE_CNT = "cn_package_cnt";
  String CNSGNMNT_WEIGHT = "cn_wt";
  String CNSGNMNT_DIMENSIONS = "cn_dimensions";
  String CNSGNMNT_DECLARATION = "cn_declaration";
  String CNSGNMNT_VALUE = "cn_value";
  String TRACKING_ID = "tId";
  String DATE = "date";
  String PACKAGE_SIZE = "ps";
  String SHIPMENT_TRANSPORTER_ID = "tpId";
  String SHIPMENT_TRANSPORTER_NAME = "tpName";
  String SHIPMENT_CONTACT_PHONE_NUM = "phnm";
  String SHIPMENT_VEHICLE_DTLS = "vhcl";


  String createShipment(ShipmentModel model, int source, Boolean updateOrderFields,
                        PersistenceManager pm) throws ServiceException, ValidationException;

  ResponseModel updateShipmentStatus(String shipmentId, ShipmentStatus status, String message,
                                     String userId, String reason, int source) throws LogiException;

  /**
   * Update the shipment status with message
   *
   * @param shipmentId        Shipment Id
   * @param status            New status for shipment
   * @param message           Message to be saved against this status update
   * @param userId            Id of user who made this change
   * @param reason            Reason for changing status, while cancelling.
   * @param updateOrderStatus Should order status be updated, this is to break circular status updates
   * @param pm                Persistent manager instance (optional)
   * @return - ResponseModel  containing true/false for success or failure and a message in case of partial success
   */
  ResponseModel updateShipmentStatus(String shipmentId, ShipmentStatus status, String message,
                               String userId,
                                     String reason, boolean updateOrderStatus,
                                     PersistenceManager pm, int source, boolean transferAllocations)
      throws LogiException;

  boolean updateShipment(ShipmentMaterialsModel model) throws LogiException;

  IShipment updateShipmentData(Map<String, String> shipmentMetadata, String orderUpdatedAt,
                               String sId,
                               String userId) throws LogiException;

  ResponseModel fulfillShipment(String shipmentId, String userId, String message, int source)
      throws ServiceException;

  /**
   * Change the status of shipment as fulfilled with message and capture discrepancy if any with reasons.
   *
   * @param model {@code ShipmentMaterialsModel}
   * @return - ResponseModel  containing true/false for success or failure and a message in case of partial success
   */
  ResponseModel fulfillShipment(ShipmentMaterialsModel model, String userId, int source);

//    boolean cancelShipmentsByOrderId(Long orderId, String message, String userId);

  List<IShipment> getShipmentsByOrderId(Long orderId, PersistenceManager persistenceManager);

  List<IShipment> getShipmentsByOrderId(Long orderId);

  /**
   * Get list of shipments according to permissions of user
   *
   * @param userId      Session User Id
   * @param domainId    Domain Id
   * @param custId      Customer Id
   * @param vendId      Vendor Id
   * @param from        From date
   * @param to          To date
   * @param etaFrom     Expected time of arrival from
   * @param etaTo       Expected time of arrival to
   * @param transporter Name of transporter
   * @param trackingId  Tracking id of shipment
   * @return -
   */
  Results getShipments(String userId, Long domainId, Long custId, Long vendId, Date from, Date to, Date etaFrom,
                       Date etaTo, String transporter, String trackingId, ShipmentStatus status,
                       int size, int offset);

  IShipment getShipment(String shipId);

  List<String> getTransporterSuggestions(Long domainId, String text);

  IMessage addMessage(String shipmentId, String message, String userId) throws ServiceException;

  void includeShipmentItems(IShipment shipment);

  BigDecimal getAllocatedQuantityForShipmentItem(String sId, Long kId, Long mId);

  /**
   * Generate shipment voucher
   */
  PDFResponseModel generateShipmentVoucher(String shipmentId)
      throws ServiceException, ObjectNotFoundException, IOException, ValidationException;

  List<FulfilledQuantityModel> getFulfilledQuantityByOrderId(Long orderId,
                                                                    List<Long> materialIdList)
      throws ServiceException;

  IShipment patchShipmentDetails(String userId, String sId, Collection<Patch> patchRequests,
                                 String orderUpdatedAt) throws LogiException;

  IConsignment updateConsignmentDetails(Long consignmentId, ConsignmentModel consignmentModel,
                                        PersistenceManager pm)
      throws ServiceException;

  void updateShipmentDetails(String shipmentId, String username,
                             DeliveryRequestUpdateWrapper updateModel) throws LogiException;
}
