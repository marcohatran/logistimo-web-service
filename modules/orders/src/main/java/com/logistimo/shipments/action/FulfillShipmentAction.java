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

import com.logistimo.auth.utils.SecurityUtils;
import com.logistimo.constants.Constants;
import com.logistimo.entities.service.EntitiesService;
import com.logistimo.exception.InvalidServiceException;
import com.logistimo.exception.LogiException;
import com.logistimo.exception.ValidationException;
import com.logistimo.logger.XLog;
import com.logistimo.materials.entity.IMaterial;
import com.logistimo.materials.service.MaterialCatalogService;
import com.logistimo.models.ResponseModel;
import com.logistimo.models.shipments.ShipmentItemBatchModel;
import com.logistimo.models.shipments.ShipmentItemModel;
import com.logistimo.models.shipments.ShipmentMaterialsModel;
import com.logistimo.orders.entity.IDemandItem;
import com.logistimo.orders.entity.IOrder;
import com.logistimo.orders.service.IDemandService;
import com.logistimo.orders.service.OrderManagementService;
import com.logistimo.services.ObjectNotFoundException;
import com.logistimo.services.Resources;
import com.logistimo.services.ServiceException;
import com.logistimo.services.impl.PMF;
import com.logistimo.shipments.ShipmentRepository;
import com.logistimo.shipments.ShipmentStatus;
import com.logistimo.shipments.ShipmentUtils;
import com.logistimo.shipments.entity.IShipment;
import com.logistimo.shipments.entity.IShipmentItem;
import com.logistimo.shipments.entity.IShipmentItemBatch;
import com.logistimo.shipments.mapper.FulfillmentShipmentEntityModelMapper;
import com.logistimo.shipments.mapper.ShipmentItemBatchMapper;
import com.logistimo.users.entity.IUserAccount;
import com.logistimo.users.service.UsersService;
import com.logistimo.utils.BigUtil;
import com.logistimo.utils.LocalDateUtil;
import com.logistimo.utils.LockUtil;

import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang.StringUtils;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.ResourceBundle;
import java.util.Set;

import javax.jdo.PersistenceManager;
import javax.jdo.Transaction;

import lombok.AllArgsConstructor;

@Component
@AllArgsConstructor
public class FulfillShipmentAction {

  private static final XLog log = XLog.getLog(FulfillShipmentAction.class);
  private static SimpleDateFormat sdf = new SimpleDateFormat(Constants.DATE_FORMAT);

  private OrderManagementService orderManagementService;
  private UsersService usersService;
  private ShipmentRepository shipmentRepository;
  private EntitiesService entitiesService;
  private IDemandService demandService;
  private ShipmentItemBatchMapper shipmentItemBatchMapper;
  private UpdateShipmentStatusAction updateShipmentStatusAction;
  private FulfillmentShipmentEntityModelMapper fulfillmentShipmentEntityModelMapper;
  private MaterialCatalogService materialCatalogService;

  public ResponseModel invoke(String shipmentId, String userId, String message, int source) throws
      ServiceException {
    ShipmentMaterialsModel sModel = fulfillmentShipmentEntityModelMapper.from(shipmentId, userId);
    sModel.msg = message;
    return invoke(sModel, userId, source);
  }

  public ResponseModel invoke(ShipmentMaterialsModel model, String userId, int source) {
    Long orderId = ShipmentUtils.extractOrderId(model.sId);
    LockUtil.LockStatus lockStatus = acquireLock(orderId);
    PersistenceManager pm = null;
    Transaction tx = null;
    ResponseModel responseModel;
    try {
      pm = PMF.get().getPersistenceManager();
      tx = pm.currentTransaction();
      tx.begin();
      ensureNoOneChangedThisOrder(model, orderId);
      responseModel = fulfill(model, orderId, userId, source, pm);
      tx.commit();
    } catch (InvalidServiceException | LogiException e) {
      log.warn("Error while updating shipment", e);
      throw new InvalidServiceException(e.getMessage());
    } catch (Exception e1) {
      log.warn("Error while updating shipment", e1);
      throw new InvalidServiceException(e1);
    } finally {
      if (tx != null && tx.isActive()) {
        tx.rollback();
      }
      if (pm != null) {
        pm.close();
      }
      releaseLock(orderId, lockStatus);
    }

    return responseModel;
  }

  private ResponseModel fulfill(ShipmentMaterialsModel model, Long orderId, String userId, int source, PersistenceManager pm)
      throws LogiException, ParseException {
    IShipment shipment = shipmentRepository.getById(model.sId, true, pm);
    ResponseModel responseModel;
    if (shipment != null) {
      updateShipmentItems(model, shipment, pm);
      updateDemandItems(model, shipment, pm);
      updateShipment(model, shipment, pm);
      responseModel =
          updateShipmentStatusAction
              .invoke(model.sId, ShipmentStatus.FULFILLED, model.msg, model.userId, pm, null, shipment, true,
                  model.isOrderFulfil, source, null, null, false, true);
      orderManagementService.updateOrderMetadata(orderId, userId, pm);
    } else {
      throw new ObjectNotFoundException("No such shipment exists");
    }
    return responseModel;
  }

  private void updateShipmentItems(ShipmentMaterialsModel model, IShipment shipment,
                                   PersistenceManager pm)
      throws ServiceException, ParseException {
    for (ShipmentItemModel item : model.items) {
      updateShipmentItem(model, shipment, item);
    }
    persistShipmentItems(pm, shipment);
  }

  private void updateShipmentItem(ShipmentMaterialsModel model, IShipment shipment,
                                  ShipmentItemModel item) throws ServiceException, ParseException {

    IShipmentItem shipmentItem = getShipmentItemForMaterial(shipment, item.mId);
    if (item.isBa) {
      updateBatchEnabledShipmentItem(model, item, shipmentItem);
    } else {
      updateBatchDisabledShipmentItem(shipment, item, shipmentItem);
    }
  }

  private void updateShipment(ShipmentMaterialsModel model, IShipment shipment, PersistenceManager pm) throws ParseException {
    shipment.setActualFulfilmentDate(sdf.parse(model.afd));
    pm.makePersistent(shipment);
  }

  private void updateDemandItems(ShipmentMaterialsModel model, IShipment shipment,  PersistenceManager pm) {
    Map<Long, IDemandItem> items = demandService.getDemandMetadata(ShipmentUtils.extractOrderId(model.sId), pm);
    for (IShipmentItem sItem : shipment.getShipmentItems()) {
      IDemandItem item = items.get(sItem.getMaterialId());
      item.setFulfilledQuantity(item.getFulfilledQuantity().add(sItem.getFulfilledQuantity()));
      item.setDiscrepancyQuantity(item.getDiscrepancyQuantity().add(sItem.getDiscrepancyQuantity()));
    }
    pm.makePersistentAll(items.values());
  }

  private void persistShipmentItems(PersistenceManager pm, IShipment shipment) {
    for (IShipmentItem shipmentItem : shipment.getShipmentItems()) {
      List<IShipmentItemBatch> shipmentItemBatches = (List<IShipmentItemBatch>) shipmentItem
          .getShipmentItemBatch();
      if (CollectionUtils.isNotEmpty(shipmentItemBatches)) {
        pm.makePersistentAll(shipmentItemBatches);
      }
    }

    pm.makePersistentAll(shipment.getShipmentItems());
  }

  private void updateBatchDisabledShipmentItem(IShipment shipment, ShipmentItemModel item, IShipmentItem shipmentItem) throws ServiceException {
    if (entitiesService.getKiosk(shipment.getKioskId(), false).isBatchMgmtEnabled() && CollectionUtils
        .isNotEmpty(shipmentItem.getShipmentItemBatch())) {
      throw new ValidationException("O014", shipment.getOrderId(), shipmentItem.getMaterialId());
    }
    shipmentItem.setFulfilledQuantity(item.fq);
    shipmentItem.setDiscrepancyQuantity(shipmentItem.getQuantity().subtract(item.fq));
    shipmentItem.setFulfilledMaterialStatus(item.fmst);
    shipmentItem.setFulfilledDiscrepancyReason(item.frsn);
  }

  private void updateBatchEnabledShipmentItem(ShipmentMaterialsModel model,
                                              ShipmentItemModel item, IShipmentItem shipmentItem) throws ServiceException, ParseException {
    validateBatchQuantity(model, item);

    BigDecimal totalFQ = BigDecimal.ZERO;
    BigDecimal totalDQ = BigDecimal.ZERO;
    Set<String> updatedBatchesMap = new HashSet<>();
    List<IShipmentItemBatch> shipmentItemBatches = (List<IShipmentItemBatch>) shipmentItem.getShipmentItemBatch();
    for (ShipmentItemBatchModel shipmentItemBatchModel : item.bq) {
      validateFulfilledQuantity(shipmentItemBatchModel);
      Optional<IShipmentItemBatch> shipmentItemBatchOptional = getShipmentItemBatchByBid(shipmentItemBatches, shipmentItemBatchModel.id);
      if(shipmentItemBatchOptional.isPresent()) {
        IShipmentItemBatch shipmentItemBatch = shipmentItemBatchOptional.get();
        updateShipmentItemBatch(shipmentItemBatch, shipmentItemBatchModel);
        //shipmentItemBatch.setDiscrepancyQuantity(shipmentItemBatch.getQuantity().subtract(shipmentItemBatchModel.q));
        totalFQ = totalFQ.add(shipmentItemBatch.getFulfilledQuantity());
        totalDQ = totalDQ.add(shipmentItemBatch.getDiscrepancyQuantity());
        updatedBatchesMap.add(shipmentItemBatchModel.id);
      } else if(BigUtil.greaterThanZero(shipmentItemBatchModel.fq)) {
        // We have a new batch item, create a new shipmentItem batch
        IShipmentItemBatch shipmentItemBatch = buildShipmentItemBatch(model.userId, shipmentItem, shipmentItemBatchModel);
        updateShipmentItemBatch(shipmentItemBatch, shipmentItemBatchModel);
        totalFQ = totalFQ.add(shipmentItemBatch.getFulfilledQuantity());
        totalDQ = totalDQ.add(shipmentItemBatch.getDiscrepancyQuantity());
        shipmentItemBatches.add(shipmentItemBatch);
      }
    }
    if(updatedBatchesMap.size() != shipmentItemBatches.size()) {
      IMaterial material = materialCatalogService.getMaterial(shipmentItem.getMaterialId());
      throw new ValidationException("O015", shipmentItem.getShipmentId(), material.getName());
    }
    shipmentItem.setFulfilledQuantity(totalFQ);
    shipmentItem.setDiscrepancyQuantity(totalDQ);
  }

  private void validateFulfilledQuantity(ShipmentItemBatchModel shipmentItemBatchModel) throws ServiceException {
    if (shipmentItemBatchModel.fq == null || BigUtil.lesserThanZero(
        shipmentItemBatchModel.fq)) {
      ResourceBundle
          backendMessages =
          Resources.getBundle("BackendMessages", SecurityUtils.getLocale());
      throw new ServiceException(
          backendMessages.getString("shipment.batch.item") + " " +
              shipmentItemBatchModel.id + " " + backendMessages
              .getString("qty.lower") + " " +
              shipmentItemBatchModel.fq + backendMessages.getString("valid.qty"));
    }
  }

  private Optional<IShipmentItemBatch> getShipmentItemBatchByBid(List<? extends IShipmentItemBatch> shipmentItemBatches, String batchId) {
    for (IShipmentItemBatch shipmentItemBatch : shipmentItemBatches) {
      if (shipmentItemBatch.getBatchId().equals(batchId)) {
        return Optional.of(shipmentItemBatch);
      }
    }
    return Optional.empty();
  }

  private void validateBatchQuantity(ShipmentMaterialsModel model, ShipmentItemModel item) {
    if (item.bq == null || item.bq.isEmpty()) {
      ResourceBundle
          backendMessages =
          Resources.getBundle("BackendMessages", SecurityUtils.getLocale());
      throw new IllegalArgumentException(
          backendMessages.getString("shipment.fulfill.error") + " " + model.sId +
              ", " + backendMessages.getString("materials.batch.empty") + " "
              + item.mnm);
    }
  }

  private IShipmentItem getShipmentItemForMaterial(IShipment shipment, Long mId) {
    for (IShipmentItem shipmentItem : shipment.getShipmentItems()) {
      if (mId.equals(shipmentItem.getMaterialId())) {
        return shipmentItem;
      }
    }
    throw new ValidationException("O016",mId, shipment.getShipmentId());
  }

  private void updateShipmentItemBatch(IShipmentItemBatch shipmentItemBatch,
                                       ShipmentItemBatchModel shipmentItemBatchModel) {

    shipmentItemBatch.setFulfilledMaterialStatus(shipmentItemBatchModel.fmst);
    shipmentItemBatch.setFulfilledQuantity(shipmentItemBatchModel.fq);
    shipmentItemBatch.setDiscrepancyQuantity(
        shipmentItemBatch.getQuantity().subtract(shipmentItemBatchModel.fq));
    shipmentItemBatch.setFulfilledDiscrepancyReason(shipmentItemBatchModel.frsn);
  }

  private IShipmentItemBatch buildShipmentItemBatch(String userId,
                                                    IShipmentItem shipmentItem,
                                                    ShipmentItemBatchModel shipmentItemBatchModel)
      throws ServiceException, ParseException {
    shipmentItemBatchModel.uid = userId;
    shipmentItemBatchModel.kid = shipmentItem.getKioskId();
    shipmentItemBatchModel.mid = shipmentItem.getMaterialId();
    shipmentItemBatchModel.siId = shipmentItem.getShipmentItemId();
    shipmentItemBatchModel.sdid = shipmentItem.getDomainId();
    IShipmentItemBatch shipmentItemBatch = shipmentItemBatchMapper.invoke(shipmentItemBatchModel);

    //set expiry and manufacturing dates
    shipmentItemBatch.setBatchExpiry(LocalDateUtil
        .parseCustom(shipmentItemBatchModel.e, Constants.DATE_FORMAT, null));
    if (StringUtils.isNotEmpty(shipmentItemBatchModel.bmfdt)) {
      shipmentItemBatch.setBatchManufacturedDate(LocalDateUtil
          .parseCustom(shipmentItemBatchModel.bmfdt, Constants.DATE_FORMAT, null));
    }
    return shipmentItemBatch;
  }

  private void releaseLock(Long orderId, LockUtil.LockStatus lockStatus) {
    if (LockUtil.shouldReleaseLock(lockStatus) && !LockUtil.release(Constants.TX_O + orderId)) {
      log.warn("Unable to release lock for key {0}", Constants.TX_O + orderId);
    }
  }

  private LockUtil.LockStatus acquireLock(Long orderId) {
    LockUtil.LockStatus lockStatus = LockUtil.lock(Constants.TX_O + orderId);
    if (!LockUtil.isLocked(lockStatus)) {
      throw new InvalidServiceException(new ServiceException("O002", orderId));
    }
    return lockStatus;
  }

  private void ensureNoOneChangedThisOrder(ShipmentMaterialsModel model, Long orderId) throws LogiException {
    IOrder order = orderManagementService.getOrder(orderId);
    if (StringUtils.isNotBlank(model.orderUpdatedAt)) {
      if (!model.orderUpdatedAt.equals(
          LocalDateUtil.formatCustom(order.getUpdatedOn(), Constants.DATETIME_FORMAT, null))) {
        IUserAccount userAccount = usersService.getUserAccount(order.getUpdatedBy());
        throw new LogiException("O004", userAccount.getFullName(),
            LocalDateUtil.format(order.getUpdatedOn(), SecurityUtils.getLocale(),
                userAccount.getTimezone()));
      }
    }
  }

}
