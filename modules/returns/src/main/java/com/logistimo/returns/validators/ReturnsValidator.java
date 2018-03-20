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

import com.logistimo.entities.auth.EntityAuthoriser;
import com.logistimo.exception.InvalidDataException;
import com.logistimo.exception.UnauthorizedException;
import com.logistimo.materials.model.HandlingUnitModel;
import com.logistimo.returns.Status;
import com.logistimo.returns.models.UpdateStatusModel;
import com.logistimo.returns.vo.ReturnsItemVO;
import com.logistimo.returns.vo.ReturnsVO;
import com.logistimo.services.ServiceException;
import com.logistimo.shipments.FulfilledQuantityModel;

import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.collections.MapUtils;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * Created by pratheeka on 13/03/18.
 */
@Component
public class ReturnsValidator {



  public boolean validateReturnedQuantity(List<ReturnsItemVO> returnItemList, List<FulfilledQuantityModel> shipmentList, List<HandlingUnitModel>
      handlingUnitModelList){

    if (CollectionUtils.isEmpty(shipmentList)) {
      throw new InvalidDataException("No order present!");
    }

    Map<Long, Map<String, BigDecimal>> shipments = getShipments(shipmentList);

    Map<Long, BigDecimal> handlingUnits = getHandlingUnit(handlingUnitModelList);
    for (ReturnsItemVO returnsItemVO : returnItemList) {

      Map<String, BigDecimal> fulfilledQuantityByBatches =
          shipments.get(returnsItemVO.getMaterialId());
      if (fulfilledQuantityByBatches == null) {
        throw new InvalidDataException("No demand item entry present!!");
      }
      BigDecimal handlingUnitQuantity = BigDecimal.ONE;
      if (!handlingUnits.isEmpty()) {
        handlingUnitQuantity = handlingUnits.get(returnsItemVO.getMaterialId());
      }
      if (CollectionUtils.isEmpty(returnsItemVO.getReturnItemBatches())) {
        if (returnsItemVO.getQuantity().compareTo(fulfilledQuantityByBatches.get(null)) > 0) {
          throw new InvalidDataException("Returned quantity is greater than ordered quantity!!");
        }
        validateHandlingUnit(handlingUnitQuantity, returnsItemVO.getQuantity());
      } else {
        validateBatches(returnsItemVO, fulfilledQuantityByBatches, handlingUnitQuantity);
      }
    }
    return true;
  }

  private Map<Long, Map<String, BigDecimal>> getShipments(
      List<FulfilledQuantityModel> shipmentList) {
    Map<Long, Map<String, BigDecimal>> shipmentItemMap = new HashMap<>();
    shipmentList.forEach(fulfilledQuantityModel -> {
      Long materialId = fulfilledQuantityModel.getMaterialId();
      Map<String, BigDecimal> batchesMap;
      if (!shipmentItemMap.containsKey(materialId)) {
        batchesMap = new HashMap<>();
        shipmentItemMap.put(materialId, batchesMap);
      } else {
        batchesMap = shipmentItemMap.get(materialId);
      }
      batchesMap
          .put(fulfilledQuantityModel.getBatchId(), fulfilledQuantityModel.getFulfilledQuantity());
    });
    return shipmentItemMap;
  }

  private void validateBatches(ReturnsItemVO returnsItemVO,
                               Map<String, BigDecimal> fulfilledQuantityByBatches,
                               BigDecimal handlingUnitQuantity) {

    returnsItemVO.getReturnItemBatches().forEach(returnsItemBatchVO -> {
      validateHandlingUnit(handlingUnitQuantity, returnsItemBatchVO.getQuantity());
      BigDecimal quantity =
          fulfilledQuantityByBatches.get(returnsItemBatchVO.getBatch().getBatchId());
      if (returnsItemBatchVO.getQuantity().compareTo(quantity) > 0) {
        throw new InvalidDataException("Returned quantity is greater than ordered quantity!!");
      }
    });
  }

  private void validateHandlingUnit(BigDecimal handlingUnitQuantity, BigDecimal quantity) {
    if (quantity.remainder(handlingUnitQuantity).compareTo(BigDecimal.ZERO) != 0) {
      throw new InvalidDataException(
          "Returned quantity is not a multiple handling unit quantity!!");
    }
  }

  private Map<Long, BigDecimal> getHandlingUnit( List<HandlingUnitModel>
                                                               handlingUnitModelList) {

    if (CollectionUtils.isNotEmpty(handlingUnitModelList)) {
      return handlingUnitModelList.stream().collect(
          Collectors
              .toMap(HandlingUnitModel::getMaterialId, HandlingUnitModel::getQuantity));
    }
    return MapUtils.EMPTY_MAP;
  }

  public void validateStatusChange(Status newStatus, Status oldStatus) {
    if (oldStatus == Status.OPEN && !(newStatus == Status.CANCELLED
        || newStatus == Status.SHIPPED)) {
      throw new InvalidDataException("Invalid status");
    } else if (oldStatus == Status.CANCELLED || oldStatus == Status.RECEIVED) {
      throw new InvalidDataException("Status cannot be changed");
    } else if (oldStatus == Status.SHIPPED && newStatus != Status.RECEIVED) {
      throw new InvalidDataException("Invalid status");
    }
  }

  private boolean hasEntityAccess(Long entityId) throws ServiceException {
    if(!EntityAuthoriser.authoriseEntity(entityId)){
      throw new UnauthorizedException("No access to entity");
    }
    return true;
  }

  public boolean checkAccessForStatusChange(UpdateStatusModel statusModel, ReturnsVO returnsVO)
      throws ServiceException {
    return (statusModel.getStatus() == Status.SHIPPED && hasEntityAccess(
        returnsVO.getCustomerId())) ||
        (statusModel.getStatus() == Status.RECEIVED && hasEntityAccess(
            returnsVO.getVendorId())) || (
        statusModel.getStatus() == Status.CANCELLED && (hasEntityAccess(
            returnsVO.getVendorId()) || hasEntityAccess(returnsVO.getCustomerId())));
  }

}
