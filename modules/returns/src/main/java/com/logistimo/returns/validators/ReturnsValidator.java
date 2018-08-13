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
import com.logistimo.entities.auth.EntityAuthoriser;
import com.logistimo.exception.InvalidDataException;
import com.logistimo.exception.ValidationException;
import com.logistimo.inventory.entity.jpa.Inventory;
import com.logistimo.inventory.entity.jpa.InventoryBatch;
import com.logistimo.materials.model.HandlingUnitModel;
import com.logistimo.returns.Status;
import com.logistimo.returns.exception.ReturnsException;
import com.logistimo.returns.vo.BatchQuantityVO;
import com.logistimo.returns.vo.ItemQuantityVO;
import com.logistimo.returns.vo.ReturnsItemBatchVO;
import com.logistimo.returns.vo.ReturnsItemVO;
import com.logistimo.returns.vo.ReturnsQuantityDetailsVO;
import com.logistimo.returns.vo.ReturnsVO;
import com.logistimo.services.ServiceException;
import com.logistimo.shipments.FulfilledQuantityModel;
import com.logistimo.utils.Stream;

import org.apache.commons.collections.CollectionUtils;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Created by pratheeka on 13/03/18.
 */
public class ReturnsValidator {


  /**
   * Check if returned quantity is a multiple of handling unit
   *
   * @param returnItemList     List of return items
   * @param handlingUnitModels All the handling units configured for materials
   */
  public void validateHandlingUnit(List<ReturnsItemVO> returnItemList,
                                   List<HandlingUnitModel> handlingUnitModels) {
    if (CollectionUtils.isEmpty(handlingUnitModels)) {
      return;
    }
    Map<Long, BigDecimal> handlingUnits =
        Stream.toMap(handlingUnitModels,
            HandlingUnitModel::getMaterialId, HandlingUnitModel::getQuantity);

    returnItemList.forEach(returnsItemVO -> {
      final BigDecimal handlingUnitQuantity = handlingUnits.get(returnsItemVO.getMaterialId());
      if (!returnsItemVO.hasBatches()) {
        validateHandlingUnit(handlingUnitQuantity, returnsItemVO.getQuantity());
      } else {
        returnsItemVO.getReturnItemBatches().forEach(returnsItemBatchVO ->
                validateHandlingUnit(handlingUnitQuantity, returnsItemBatchVO.getQuantity())
        );
      }
    });
  }

  /**
   * If quantity is not a multiple of handling unit, throw exception
   *
   * @param handlingUnitQuantity Handling unit quantity
   * @param quantity             Return quantity
   */
  private void validateHandlingUnit(BigDecimal handlingUnitQuantity, BigDecimal quantity) {
    if (quantity.remainder(handlingUnitQuantity).compareTo(BigDecimal.ZERO) != 0) {
      throw new ValidationException("RT005", quantity.toString(), handlingUnitQuantity.toString());
    }
  }

  public void validateReturnsPolicy(ReturnsConfig returnsConfiguration, Long orderFulfillmentTime) {
    if (returnsConfiguration.getIncomingDuration() == null
        || returnsConfiguration.getIncomingDuration().compareTo(0) == 0
        || orderFulfillmentTime == null) {
      return;
    }
    Long incomingDuration = returnsConfiguration.getIncomingDuration() * 24 * 60 * 60 * 1000l;
    if ((System.currentTimeMillis() - orderFulfillmentTime) > incomingDuration) {
      throw new ValidationException("RT004", returnsConfiguration.getIncomingDuration());
    }
  }

  public void validateStatusChange(Status newStatus, Status oldStatus) {
    if (newStatus == null) {
      throw new InvalidDataException("Status cannot be empty!!");
    }
    if (oldStatus == Status.OPEN && !(newStatus == Status.CANCELLED
        || newStatus == Status.SHIPPED)) {
      throw new InvalidDataException("Invalid status");
    } else if (oldStatus == Status.CANCELLED || oldStatus == Status.RECEIVED) {
      throw new InvalidDataException("Status cannot be changed");
    } else if (oldStatus == Status.SHIPPED && !(newStatus == Status.RECEIVED
        || newStatus == Status.CANCELLED)) {
      throw new InvalidDataException("Invalid status");
    }
  }

  private boolean hasEntityAccess(Long entityId) throws ServiceException {
    return EntityAuthoriser.authoriseEntityPerm(entityId) > 1;
  }

  public boolean checkAccessForStatusChange(Status newStatus, ReturnsVO returnsVO)
      throws ServiceException {
    switch (newStatus) {
      case SHIPPED:
        return hasEntityAccess(returnsVO.getCustomerId());
      case RECEIVED:
        return hasEntityAccess(returnsVO.getVendorId());
      case CANCELLED:
        if (returnsVO.isShipped()) {
          return hasEntityAccess(returnsVO.getCustomerId());
        } else if (returnsVO.isOpen()) {
          return hasEntityAccess(returnsVO.getCustomerId()) ||
              hasEntityAccess(returnsVO.getVendorId());
        }
        return false;
      default:
        return true;
    }
  }

  public void validateShippedQtyAgainstAvailableQty(List<ReturnsItemVO> returnsItemVOList,
                                                    List<Inventory> inventoryList,
                                                    List<InventoryBatch> inventoryBatches)
      throws ReturnsException {
    Map<Long, ItemQuantityVO> itemQuantityVOMap =
        Stream.toMap(inventoryList, Inventory::getMaterialId, this::getItemQuantityVO);

    setAvaiableStockForBatches(inventoryBatches, itemQuantityVOMap);
    Set<Long> errorMaterialIds = returnsItemVOList.stream().map(returnsItemVO -> {
      ItemQuantityVO itemQuantityVO = itemQuantityVOMap.get(returnsItemVO.getMaterialId());
      if (returnsItemVO.hasBatches()) {
        Map<String, BatchQuantityVO> batchQuantityVOMap = itemQuantityVO.getBatchDetails();
        final Optional<Long> errorMaterialId =
            returnsItemVO.getReturnItemBatches().stream().map(returnsItemBatchVO -> {
            BatchQuantityVO batchQuantityVO =
                batchQuantityVOMap.get(returnsItemBatchVO.getBatch().getBatchId());
            if (returnsItemBatchVO.getQuantity().compareTo(batchQuantityVO.getQuantity()) > 0) {
              return returnsItemVO.getMaterialId();
            }
            return null;
          }).filter(Objects::nonNull).findAny();
        if (errorMaterialId.isPresent()) {
          return errorMaterialId.get();
        }
      } else {
        if (returnsItemVO.getQuantity().compareTo(itemQuantityVO.getQuantity()) > 0) {
          return returnsItemVO.getMaterialId();
        }
      }
      return null;
    }).filter(Objects::nonNull).collect(Collectors.toSet());

    if (CollectionUtils.isNotEmpty(errorMaterialIds)) {
      throw new ReturnsException("RT009", new ArrayList<>(errorMaterialIds));
    }
  }

  private void setAvaiableStockForBatches(List<InventoryBatch> inventoryBatches,
                                          Map<Long, ItemQuantityVO> itemQuantityVOMap) {
    inventoryBatches.forEach(inventoryBatch -> {
      ItemQuantityVO itemQuantityVO = itemQuantityVOMap.get(inventoryBatch.getMaterialId());
      if (itemQuantityVO.getBatchDetails() == null) {
        itemQuantityVO.setBatchDetails(new HashMap<>());
      }
      itemQuantityVO.getBatchDetails().put(inventoryBatch.getBatchId(),
          new BatchQuantityVO(inventoryBatch.getBatchId(), inventoryBatch.getAvailableStock()));
    });
  }

  private ItemQuantityVO getItemQuantityVO(Inventory inventory) {
    ItemQuantityVO itemQuantityVO = new ItemQuantityVO();
    itemQuantityVO.setMaterialId(inventory.getMaterialId());
    itemQuantityVO.setQuantity(inventory.getAvailableStock());
    return itemQuantityVO;
  }

  /**
   * This method sums all quanitites in returns and verifies it against fulfilled quanitity
   *
   * @param returnsQuantityDetailsVOs List of all returns with quantites
   * @param fulfilledQuantityModels   list of fulfilled Quantities
   * @param returnsItemVOs            new ReturnsItemVO
   */
  public void validateReturnedQtyAgainstFulfilledQty(
      List<ReturnsQuantityDetailsVO> returnsQuantityDetailsVOs,
      List<FulfilledQuantityModel> fulfilledQuantityModels,
      List<ReturnsItemVO> returnsItemVOs) {
    Map<Long, ItemQuantityVO> returnItemQtyByMaterialID =
        Stream.toMap(returnsItemVOs, ReturnsItemVO::getMaterialId, this::getItemQuantityVO);
    calculateTotalReturnedQty(returnsQuantityDetailsVOs, returnItemQtyByMaterialID);
    validateReturnedQtyAgainstFulfilledQty(fulfilledQuantityModels, returnItemQtyByMaterialID);
  }

  /**
   * Check if total quantity in returns is less than fulfilledQuantity
   *
   * @param fulfilledQuantityModels-List of fulfilled quantity
   * @param returnItemQtyByMaterialID    Map of material id as key,ItemQuantityVO as value
   */
  private void validateReturnedQtyAgainstFulfilledQty(
      List<FulfilledQuantityModel> fulfilledQuantityModels,
      Map<Long, ItemQuantityVO> returnItemQtyByMaterialID) {
    fulfilledQuantityModels.stream()
      .filter(fulfilledQuantityModel ->
          returnItemQtyByMaterialID.get(fulfilledQuantityModel.getMaterialId()) != null)
      .forEach(fulfilledQuantityModel -> {
        ItemQuantityVO itemQuantityVO =
            returnItemQtyByMaterialID.get(fulfilledQuantityModel.getMaterialId());
        BigDecimal quantity = BigDecimal.ZERO;
        if (fulfilledQuantityModel.getBatchId() != null) {
          if (itemQuantityVO.getBatchDetails().get(fulfilledQuantityModel.getBatchId()) != null) {
            quantity = itemQuantityVO.getBatchDetails().get(fulfilledQuantityModel.getBatchId())
                .getQuantity();
          }
        } else {
          quantity = itemQuantityVO.getQuantity();
        }
        if (quantity.compareTo(fulfilledQuantityModel.getFulfilledQuantity()) > 0) {
          throw new InvalidDataException("Returned Quantity is more than fulfilled Quantity");
        }
      });
  }

  /**
   * Sums the new Returned quantity with the total returned quantity across all returns for
   * the given order ID.
   *
   * @param returnsQuantityDetailsVOs List of returned quantity for items
   * @param returnItemQtyByMaterialID Map of material id and new returned quantity
   */
  private void calculateTotalReturnedQty(
      List<ReturnsQuantityDetailsVO> returnsQuantityDetailsVOs,
      Map<Long, ItemQuantityVO> returnItemQtyByMaterialID) {
    returnsQuantityDetailsVOs.stream()
      .filter(returnsQuantityDetailsVO -> returnItemQtyByMaterialID
          .containsKey(returnsQuantityDetailsVO.getMaterialId()))
      .forEach(returnsQuantityDetailsVO -> {
        ItemQuantityVO itemQuantityVO =
            returnItemQtyByMaterialID.get(returnsQuantityDetailsVO.getMaterialId());
        if (returnsQuantityDetailsVO.getBatchId() != null) {
          Map<String, BatchQuantityVO> batchDetails = itemQuantityVO.getBatchDetails();
          if (batchDetails.containsKey(returnsQuantityDetailsVO.getBatchId())) {
            BatchQuantityVO batchQuantityVO =
                batchDetails.get(returnsQuantityDetailsVO.getBatchId());
            batchQuantityVO.setQuantity(
                returnsQuantityDetailsVO.getBatchQuantity().add(batchQuantityVO.getQuantity()));
          }
        } else {
          itemQuantityVO.setQuantity(returnsQuantityDetailsVO.getItemQuantity()
              .add(itemQuantityVO.getQuantity()));
        }
      });
  }

  private ItemQuantityVO getItemQuantityVO(ReturnsItemVO returnsItemVO) {
    ItemQuantityVO itemQuantityVO = new ItemQuantityVO();
    if (returnsItemVO.hasBatches()) {
      //Build batch details
      itemQuantityVO.setBatchDetails(
          getReturnedQuantityByBatch(returnsItemVO.getReturnItemBatches()));
    } else {
      itemQuantityVO.setQuantity(returnsItemVO.getQuantity());
    }
    itemQuantityVO.setMaterialId(returnsItemVO.getMaterialId());
    return itemQuantityVO;
  }

  /**
   * Get a map with batch id as key and BatchQuantityVO as value
   *
   * @return A map with batch id as key and BatchQuantityVO as value
   */
  private Map<String, BatchQuantityVO> getReturnedQuantityByBatch(
      List<ReturnsItemBatchVO> returnsItemBatchVOs) {
    return Stream.toMap(returnsItemBatchVOs,
        returnItemBatchVO -> returnItemBatchVO.getBatch().getBatchId(),
        returnItemBatchVO -> new BatchQuantityVO(returnItemBatchVO.getBatch().getBatchId(),
            returnItemBatchVO.getQuantity()));
  }
}
