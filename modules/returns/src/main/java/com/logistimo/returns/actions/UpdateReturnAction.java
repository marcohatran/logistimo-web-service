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

package com.logistimo.returns.actions;

import com.logistimo.returns.service.ReturnsRepository;
import com.logistimo.returns.vo.ReturnsItemBatchVO;
import com.logistimo.returns.vo.ReturnsItemVO;
import com.logistimo.returns.vo.ReturnsTrackingDetailsVO;
import com.logistimo.returns.vo.ReturnsVO;
import com.logistimo.utils.Stream;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.Map;

/**
 * Created by pratheeka on 25/06/18.
 */
@Component
public class UpdateReturnAction {


  @Autowired
  ReturnsRepository returnsRepository;

  @Autowired
  GetReturnsAction getReturnsAction;


  public ReturnsVO invoke(ReturnsVO updatedReturnVO) {

    ReturnsVO savedReturnsVO = getReturnsAction.invoke(updatedReturnVO.getId());

    if (updatedReturnVO.hasItems()) {
      Map<Long, ReturnsItemVO> returnItemsByMaterial =
          Stream.toMap(savedReturnsVO.getItems(), ReturnsItemVO::getMaterialId);
      updatedReturnVO.getItems().forEach(updatedReturnsItemVO -> {
        ReturnsItemVO savedReturnItem =
            returnItemsByMaterial.get(updatedReturnsItemVO.getMaterialId());
        if (savedReturnItem != null) {
          if (savedReturnItem.hasBatches() && updatedReturnsItemVO.hasBatches()) {
            Map<String, ReturnsItemBatchVO> returnItemsByBatch =
              Stream.toMap(savedReturnItem.getReturnItemBatches(), x -> x.getBatch().getBatchId());
            updatedReturnsItemVO.getReturnItemBatches().forEach(returnsItemBatchVO -> {
              if (returnItemsByBatch.containsKey(returnsItemBatchVO.getBatch().getBatchId())) {
                ReturnsItemBatchVO savedReturnBatchVO =
                    returnItemsByBatch.get(returnsItemBatchVO.getBatch().getBatchId());
                setBatchInfo(returnsItemBatchVO, savedReturnBatchVO);
              }
            });
          }
          setReturnItemInfo(updatedReturnsItemVO, savedReturnItem);
        }
      });
    }
    setReturnVOFields(updatedReturnVO, savedReturnsVO);
    if(savedReturnsVO.hasTrackingDetails()) {
      returnsRepository.saveReturnsTrackingDetails(savedReturnsVO.getReturnsTrackingDetailsVO());
    }
    returnsRepository.saveReturns(savedReturnsVO);
    return savedReturnsVO;
  }

  /**
   * Update batch item fields
   * @param returnsItemBatchVO updated batch items
   * @param savedReturnBatchVO saved batch items
   */
  private void setBatchInfo(ReturnsItemBatchVO returnsItemBatchVO,
                            ReturnsItemBatchVO savedReturnBatchVO) {
    returnsItemBatchVO.setId(savedReturnBatchVO.getId());
    returnsItemBatchVO.setItemId(savedReturnBatchVO.getItemId());
    returnsItemBatchVO.setBatch(savedReturnBatchVO.getBatch());
  }

  /**
   * Update ReturnItem fields
   * @param updatedReturnsItemVO updated ReturnsItems
   * @param savedReturnItem saved ReturnsItems
   */
  private void setReturnItemInfo(ReturnsItemVO updatedReturnsItemVO,
                                 ReturnsItemVO savedReturnItem) {
    updatedReturnsItemVO.setMaterialId(savedReturnItem.getMaterialId());
    updatedReturnsItemVO.setId(savedReturnItem.getId());
    updatedReturnsItemVO.setCreatedBy(savedReturnItem.getCreatedBy());
    updatedReturnsItemVO.setCreatedAt(savedReturnItem.getCreatedAt());
    updatedReturnsItemVO.setReturnsId(savedReturnItem.getReturnsId());
  }

  /**
   * Set the updated fields of Returns
   * @param updatedReturnVO updated Returns
   * @param savedReturnsVO saved Returns
   */
  private void setReturnVOFields(ReturnsVO updatedReturnVO, ReturnsVO savedReturnsVO) {
    savedReturnsVO.setUpdatedAt(updatedReturnVO.getUpdatedAt());
    savedReturnsVO.setUpdatedBy(updatedReturnVO.getUpdatedBy());
    savedReturnsVO.setStatus(updatedReturnVO.getStatus());
    if (updatedReturnVO.hasItems()) {
      savedReturnsVO.setItems(updatedReturnVO.getItems());
    }
    if (updatedReturnVO.hasTrackingDetails()) {
      setTrackingDetails(updatedReturnVO, savedReturnsVO);
    }
  }

  /**
   * Update tracking details
   */
  private void setTrackingDetails(ReturnsVO updatedReturnVO, ReturnsVO savedReturnsVO) {
    if (!savedReturnsVO.hasTrackingDetails()) {
      updatedReturnVO.getReturnsTrackingDetailsVO().setReturnsId(savedReturnsVO.getId());
      savedReturnsVO.setReturnsTrackingDetailsVO(updatedReturnVO.getReturnsTrackingDetailsVO());
    } else {
      ReturnsTrackingDetailsVO savedTrackingDetails=savedReturnsVO.getReturnsTrackingDetailsVO();
      ReturnsTrackingDetailsVO updatedTrackingDetails=updatedReturnVO.getReturnsTrackingDetailsVO();
      savedTrackingDetails
          .setTransporter(updatedTrackingDetails.getTransporter());
      savedTrackingDetails
          .setTrackingId(updatedTrackingDetails.getTrackingId());
      savedTrackingDetails.setEstimatedArrivalDate(updatedTrackingDetails.getEstimatedArrivalDate());
      savedTrackingDetails
          .setUpdatedBy(updatedTrackingDetails.getUpdatedBy());
      savedTrackingDetails
          .setUpdatedAt(updatedTrackingDetails.getUpdatedAt());
    }
  }
}


