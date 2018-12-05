/*
 * Copyright © 2018 Logistimo.
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

package com.logistimo.returns.validators;

import com.logistimo.config.models.ReturnsConfig;
import com.logistimo.exception.SystemException;
import com.logistimo.exception.ValidationException;
import com.logistimo.inventory.entity.jpa.Inventory;
import com.logistimo.inventory.entity.jpa.InventoryBatch;
import com.logistimo.inventory.service.InventoryManagementService;
import com.logistimo.inventory.service.jpa.InventoryService;
import com.logistimo.materials.entity.jpa.Material;
import com.logistimo.materials.model.HandlingUnitModel;
import com.logistimo.materials.service.IHandlingUnitService;
import com.logistimo.materials.service.jpa.MaterialService;
import com.logistimo.orders.service.OrderManagementService;
import com.logistimo.returns.Status;
import com.logistimo.returns.exception.ReturnsException;
import com.logistimo.returns.service.ReturnsRepository;
import com.logistimo.returns.vo.ReturnsItemVO;
import com.logistimo.returns.vo.ReturnsQuantityDetailsVO;
import com.logistimo.returns.vo.ReturnsVO;
import com.logistimo.services.ServiceException;
import com.logistimo.shipments.FulfilledQuantityModel;
import com.logistimo.shipments.service.impl.ShipmentService;
import com.logistimo.utils.MsgUtil;
import com.logistimo.utils.Stream;

import org.apache.commons.collections.CollectionUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.Date;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Created by pratheeka on 26/07/18.
 */
@Component
public class ReturnsValidationHandler {

  @Autowired
  private InventoryService inventoryService;

  @Autowired
  private InventoryManagementService inventoryManagementService;

  private final ReturnsValidator returnsValidator = new ReturnsValidator();

  @Autowired
  private MaterialService materialService;

  @Autowired
  private ShipmentService shipmentService;

  @Autowired
  private ReturnsRepository returnsRepository;

  @Autowired
  private IHandlingUnitService handlingUnitService;

  @Autowired
  private OrderManagementService orderManagementService;

  public void validateShippingQtyAgainstAvailableQty(ReturnsVO returnsVO) {
    try {
      List<Long> materialIds = Stream.toList(returnsVO.getItems(), ReturnsItemVO::getMaterialId);
      List<Inventory> inventories = inventoryService
          .getInventoriesByKioskId(returnsVO.getCustomerId(), materialIds);
      List<InventoryBatch> inventoryBatches = inventoryService
          .getInventoryBatchesByKioskId(returnsVO.getCustomerId(), materialIds);
      returnsValidator.validateShippedQtyAgainstAvailableQty(returnsVO.getItems(),
          inventories, inventoryBatches);
    } catch (ReturnsException e) {
      List<Long> lowStockMaterialIds = e.getMaterialIds();
      List<Material> materials = materialService.getMaterialDetails(lowStockMaterialIds);
      StringBuilder message = new StringBuilder();
      AtomicInteger index = new AtomicInteger(1);
      materials.forEach(material -> message.append(MsgUtil.newLine())
          .append(index.getAndIncrement()).append(". ").append(material.getName()));
      throw new ValidationException(e.getCode(), message.toString());
    }
  }

  public void validateQuantity(Long orderId,Long returnsId,List<ReturnsItemVO> returnsItemVOs){
    if (CollectionUtils.isEmpty(returnsItemVOs)) {
      throw new ValidationException("RT001", (Object[]) null);
    }
    try {
      List<Long> materialIds = Stream.toList(returnsItemVOs, ReturnsItemVO::getMaterialId);
      List<FulfilledQuantityModel> shipmentList =
          shipmentService.getFulfilledQuantityByOrderId(orderId, materialIds);
      List<ReturnsQuantityDetailsVO> returnsQuantityDetailsVOs =
          returnsRepository.getReturnsQuantityDetailsByOrderId(orderId, returnsId);
      returnsValidator.validateReturnedQtyAgainstFulfilledQty(returnsQuantityDetailsVOs,
          shipmentList, returnsItemVOs);
      validateHandlingUnit(returnsItemVOs, materialIds);
    }catch (ServiceException e){
      throw new SystemException(e.getMessage());
    }
  }

  private void validateHandlingUnit(List<ReturnsItemVO> returnsItemVOList, List<Long> materialIds) {
    List<HandlingUnitModel> handlingUnitModels =
        handlingUnitService.getHandlingUnitDataByMaterialIds(materialIds);
    returnsValidator.validateHandlingUnit(returnsItemVOList, handlingUnitModels);
  }

  public void validateHandlingUnit(List<ReturnsItemVO> returnsItemVOList) {
    List<Long> materialIds = Stream.toList(returnsItemVOList, ReturnsItemVO::getMaterialId);
    validateHandlingUnit(returnsItemVOList, materialIds);
  }

  public void validateReturnsPolicy(Long orderId, Long vendorId) {
    Optional<ReturnsConfig> returnsConfiguration =
        inventoryManagementService.getReturnsConfig(vendorId);
    returnsConfiguration.ifPresent(rc -> {
      try {
        Date fulfilDate = orderManagementService.getOrder(orderId).getStatusUpdatedOn();
        returnsValidator.validateReturnsPolicy(rc, fulfilDate.getTime());
      } catch (ServiceException e) {
        throw new SystemException(e);
      }
    });
  }

  public void validateStatusChange(Status newStatus, Status oldStatus) {
    returnsValidator.validateStatusChange(newStatus, oldStatus);
  }

  public boolean checkAccessForStatusChange(Status newStatus, ReturnsVO returnsVO)
      throws ServiceException {
    return returnsValidator.checkAccessForStatusChange(newStatus, returnsVO);
  }
}
