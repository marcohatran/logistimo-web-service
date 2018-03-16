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
import com.logistimo.materials.model.HandlingUnitModel;
import com.logistimo.materials.service.IHandlingUnitService;
import com.logistimo.orders.entity.IDemandItem;
import com.logistimo.orders.entity.IDemandItemBatch;
import com.logistimo.orders.service.impl.DemandService;
import com.logistimo.returns.Status;
import com.logistimo.returns.vo.ReturnsItemBatchVO;
import com.logistimo.returns.vo.ReturnsItemVO;
import com.logistimo.services.ServiceException;

import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.collections.MapUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * Created by pratheeka on 13/03/18.
 */
@Component
public class ReturnsValidator {

  @Autowired
  DemandService demandService;

  @Autowired
  IHandlingUnitService handlingUnitService;





  public boolean isQuantityValid(List<ReturnsItemVO> returnItemList, Long orderId) {

    List<IDemandItem> demandItemList = demandService.getDemandItems(orderId);

    if (CollectionUtils.isEmpty(demandItemList)) {
      throw new InvalidDataException("No order present!");
    }
    Map<Long, IDemandItem> demandItemMap = demandItemList.stream().collect(Collectors.toMap(
        IDemandItem::getMaterialId, demandItem -> demandItem));
    Map<Long, BigDecimal>
        handlingUnitMap =
       getHandlingUnit(new ArrayList<>(demandItemMap.keySet())).orElse(MapUtils.EMPTY_MAP);
    for (ReturnsItemVO returnsItemVO : returnItemList) {
      IDemandItem demandItem = demandItemMap.get(returnsItemVO.getMaterialId());
      if (demandItem == null) {
        throw new InvalidDataException("No demand item entry present!!");
      }
      BigDecimal handlingUnitQuantity=BigDecimal.ONE;
      if(!handlingUnitMap.isEmpty()) {
         handlingUnitQuantity = handlingUnitMap.get(returnsItemVO.getMaterialId());
      }
      if (CollectionUtils.isEmpty(returnsItemVO.getReturnItemBatches())) {
        if (returnsItemVO.getQuantity().compareTo(demandItem.getFulfilledQuantity()) > 0) {
          throw new InvalidDataException("Returned quantity is greater than ordered quantity!!");
        }
        validateHandlingUnit(handlingUnitQuantity, returnsItemVO.getQuantity());
      } else {
        validateBatches(returnsItemVO, demandItem, handlingUnitQuantity);
      }
    }
    return true;
  }

  private void validateBatches(ReturnsItemVO returnsItemVO, IDemandItem demandItem,
                               BigDecimal handlingUnitQuantity) {

    Map<String, BigDecimal>
        batchItemMap =
        demandItem.getItemBatches().stream()
            .collect(Collectors.toMap(IDemandItemBatch::getBatchId,
                IDemandItemBatch::getQuantity));

    for (ReturnsItemBatchVO returnsItemBatch : returnsItemVO.getReturnItemBatches()) {
      BigDecimal quantity = batchItemMap.get(returnsItemBatch.getBatch().getBatchId());
      validateHandlingUnit(handlingUnitQuantity, quantity);

      if (returnsItemBatch.getQuantity().compareTo(quantity) > 0) {
        throw new InvalidDataException("Returned quantity is greater than ordered quantity!!");
      }
    }
  }

  private void validateHandlingUnit(BigDecimal handlingUnitQuantity, BigDecimal quantity) {
    if (quantity.remainder(handlingUnitQuantity).compareTo(BigDecimal.ZERO) != 0) {
      throw new InvalidDataException(
          "Returned quantity is not a multiple handling unit quantity!!");
    }
  }

  private Optional<Map<Long, BigDecimal>> getHandlingUnit(List<Long> materialIdList) {
    List<HandlingUnitModel>
        handlingUnitModelList =
        handlingUnitService.getHandlingUnitDataByMaterialIds(materialIdList);
    if(CollectionUtils.isNotEmpty(handlingUnitModelList)) {
      return Optional.of(handlingUnitModelList.stream().collect(
          Collectors
              .toMap(HandlingUnitModel::getMaterialId, HandlingUnitModel::getQuantity)));
    }
    return Optional.empty();
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

  public boolean hasAccessToEntity( Long entityId) throws ServiceException{
    return (EntityAuthoriser.authoriseEntity(entityId));
  }


}
