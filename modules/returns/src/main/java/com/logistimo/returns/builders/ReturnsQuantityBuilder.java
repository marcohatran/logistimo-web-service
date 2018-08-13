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

package com.logistimo.returns.builders;

import com.logistimo.returns.Status;
import com.logistimo.returns.models.ReturnsBatchInfo;
import com.logistimo.returns.models.ReturnsInfo;
import com.logistimo.returns.vo.ReturnsBatchQuantityVO;
import com.logistimo.returns.vo.ReturnsQuantityDetailsVO;
import com.logistimo.returns.vo.ReturnsQuantityVO;
import com.logistimo.shipments.FulfilledQuantityModel;
import com.logistimo.utils.ModelMapperUtil;
import com.logistimo.utils.Stream;

import org.apache.commons.lang.StringUtils;

import java.math.BigDecimal;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Created by pratheeka on 19/07/18.
 */

public class ReturnsQuantityBuilder {

  public List<ReturnsQuantityVO> buildReturnsQuantityList(
      List<FulfilledQuantityModel> fulfilledQuantityModelList,
      List<ReturnsQuantityDetailsVO> returnsQuantityDetailsVOs) {

    Map<Long, ReturnsInfo> returnsInfoByMaterialId = new HashMap<>();

    //Add all quantities from Returns
    fillReturnsQuantities(ModelMapperUtil.map(returnsQuantityDetailsVOs, ReturnsQuantity.class),
        returnsInfoByMaterialId);

    //Add fulfilled quantity from Shipments
    fillReturnsQuantities(ModelMapperUtil.map(fulfilledQuantityModelList, ReturnsQuantity.class),
        returnsInfoByMaterialId);
    return buildReturnsQuantityVOs(returnsInfoByMaterialId);
  }

  /**
   * Convert ReturnsInfo's to ReturnsQuantityVO's
   */
  private List<ReturnsQuantityVO> buildReturnsQuantityVOs(
      Map<Long, ReturnsInfo> returnsInfoByMaterialId) {
    return Stream.toList(returnsInfoByMaterialId.values(), returnsInfo -> {
      ReturnsQuantityVO returnsQuantityVO = ModelMapperUtil.map(returnsInfo, ReturnsQuantityVO.class);
      if (returnsInfo.hasBatches()) {
        List<ReturnsBatchQuantityVO> returnsBatchQuantityVOList =
            ModelMapperUtil.map(returnsInfo.getBatches().values(), ReturnsBatchQuantityVO.class);
        returnsQuantityVO.setBatchList(returnsBatchQuantityVOList);
      }
      return returnsQuantityVO;
    });
  }

  /**
   * Fill the map of materialId and ReturnsInfo, for both items and batch items with
   * return quantity from across all returns and
   * fulfilled quantities from across all shipments
   *
   * {@code returnsQuantity} can be either of one quantities across all returns or shipments
   *
   * @param returnsQuantities All return quantity broken down to batches across all returns
   * @param returnsInfoByMaterialId    Map to be filled
   */
  private void fillReturnsQuantities(List<ReturnsQuantity> returnsQuantities,
                                     Map<Long, ReturnsInfo> returnsInfoByMaterialId) {
    returnsQuantities.forEach(returnsQuantity -> {
      ReturnsInfo returnsInfo = returnsInfoByMaterialId
              .computeIfAbsent(returnsQuantity.getMaterialId(), ReturnsInfo::new);
      if (returnsQuantity.isBatch()) {
        ReturnsBatchInfo
            returnsBatchInfo = returnsInfo.getBatches().get(returnsQuantity.getBatchId());
        if (returnsBatchInfo == null) {
          returnsBatchInfo = buildReturnsInfoBatch(returnsQuantity.getBatchId(),
              returnsQuantity.getManufacturedDate(), returnsQuantity.getManufacturer(),
              returnsQuantity.getExpiryDate());
        }
        setReturnQuantitiesForBatches(returnsQuantity, returnsBatchInfo);
        returnsBatchInfo.setFulfilledQuantity(returnsQuantity.getFulfilledQuantity());
        returnsInfo.getBatches().put(returnsQuantity.getBatchId(), returnsBatchInfo);
      } else {
        setReturnQuantities(returnsQuantity, returnsInfo);
      }
      returnsInfo.setFulfilledQuantity(returnsQuantity.getFulfilledQuantity());
    });
  }

  /**
   * Set the returned quantity, requested returns quantity and total quantity in returns for items
   *
   * @param returnsInfo ReturnsInfo
   */
  private void setReturnQuantities(ReturnsQuantity returnsQuantity, ReturnsInfo returnsInfo) {
    if (returnsQuantity.getStatus() != Status.OPEN) {
      returnsInfo.setReturnedQuantity(
          returnsInfo.getReturnedQuantity().add(returnsQuantity.getItemQuantity()));
    } else {
      returnsInfo.setRequestedReturnQuantity(
          returnsInfo.getRequestedReturnQuantity().add(returnsQuantity.getItemQuantity()));
    }
    returnsInfo.setTotalQuantityInReturns(
        returnsInfo.getTotalQuantityInReturns().add(returnsQuantity.getItemQuantity()));
  }

  /**
   * Set the returned quantity, requested returns quantity and total quantity in returns for batch
   * items
   *
   * @param returnsBatchInfo ReturnsBatchInfo
   */
  private void setReturnQuantitiesForBatches(ReturnsQuantity returnsQuantity,
                                             ReturnsBatchInfo returnsBatchInfo) {
    if (returnsQuantity.getStatus() != Status.OPEN) {
      returnsBatchInfo.setReturnedQuantity(
          returnsBatchInfo.getReturnedQuantity().add(returnsQuantity.getBatchQuantity()));
    } else {
      returnsBatchInfo.setRequestedReturnQuantity(
          returnsBatchInfo.getRequestedReturnQuantity().add(returnsQuantity.getBatchQuantity()));
    }
    returnsBatchInfo.setTotalQuantityInReturns(
        returnsBatchInfo.getTotalQuantityInReturns().add(returnsQuantity.getBatchQuantity()));
  }

  /**
   * Build a ReturnsBatchInfo object with the required fields
   *
   * @param batchId          Item Batch ID
   * @param manufacturedDate Date of manufacture
   * @param manufacturer     Manufacturer
   * @param expiryDate       expiry date
   * @return ReturnsBatchInfo with the fields populated
   */
  private ReturnsBatchInfo buildReturnsInfoBatch(String batchId, Date manufacturedDate,
                                                 String manufacturer, Date expiryDate) {
    return ReturnsBatchInfo.builder()
        .id(batchId)
        .manufacturedDate(manufacturedDate)
        .manufacturer(manufacturer)
        .expiryDate(expiryDate)
        .build();
  }
}

@Data
@NoArgsConstructor
class ReturnsQuantity {
  private String batchId;
  private Long materialId;
  private String manufacturer;
  private Date expiryDate;
  private Date manufacturedDate;
  private BigDecimal fulfilledQuantity = BigDecimal.ZERO;
  private BigDecimal batchQuantity = BigDecimal.ZERO;
  private BigDecimal itemQuantity = BigDecimal.ZERO;
  private Status status;
  boolean isBatch() {
    return StringUtils.isNotBlank(batchId);
  }
}
