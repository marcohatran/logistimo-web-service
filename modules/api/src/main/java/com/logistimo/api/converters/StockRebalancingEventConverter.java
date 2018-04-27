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

package com.logistimo.api.converters;

import com.logistimo.api.builders.EntityBuilder;
import com.logistimo.api.models.stockrebalancing.StockRebalancingEventBatchModel;
import com.logistimo.api.models.stockrebalancing.StockRebalancingEventModel;
import com.logistimo.materials.service.MaterialCatalogService;
import com.logistimo.services.ServiceException;
import com.logistimo.stockrebalancing.actions.GetRecommendedTransfersAction;
import com.logistimo.stockrebalancing.entity.RecommendedTransfer;
import com.logistimo.stockrebalancing.entity.StockRebalancingEvent;
import com.logistimo.stockrebalancing.entity.StockRebalancingEventBatch;
import com.logistimo.stockrebalancing.models.RecommendedTransferFilters;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.convert.converter.Converter;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Created by charan on 02/04/18.
 */
@Component
public class StockRebalancingEventConverter implements
    Converter<StockRebalancingEvent, StockRebalancingEventModel> {

  @Autowired
  private MaterialCatalogService materialCatalogueService;

  @Autowired
  private StockRebalancingEventBatchConverter stockRebalancingEventBatchConverter;

  @Autowired
  private GetRecommendedTransfersAction getRecommendedTransfersAction;

  @Autowired
  private EntityBuilder entityBuilder;


  @Override
  public StockRebalancingEventModel convert(StockRebalancingEvent source) {
    StockRebalancingEventModel model = new StockRebalancingEventModel();
    model.setEventId(source.getId());
    model.setEntityId(source.getKioskId());
    model.setEntityName(source.getKiosk().getName());
    model.setLocation(entityBuilder.getLocation(source.getKiosk()));
    model.setMaterialId(source.getMaterialId());
    try {
      model.setMaterialName(materialCatalogueService.getMaterial(source.getMaterialId()).getName());
    } catch (ServiceException e) {
      e.printStackTrace();
    }
    model.setTriggerCode(source.getShortCode());
    model.setType(source.getTriggerType().name());
    model.setStatus(source.getStatus());
    model.setValue(source.getValue());
    model.setQuantity(source.getQuantity());
    model.setBatches(convertBatches(source.getStockRebalancingEventBatches()));
    List<RecommendedTransfer> recommendedTransfers = getRecommendedTransfersAction
        .invoke(RecommendedTransferFilters.builder().eventId(source.getId()).build());
    model.setRecommendationsCount(recommendedTransfers.size());
    model.setTransfersCount(recommendedTransfers.stream()
        .filter(recommendedTransfer -> recommendedTransfer.getTransferId() != null).count());
    return model;
  }

  public List<StockRebalancingEventBatchModel> convertBatches(
      Set<StockRebalancingEventBatch> stockRebalancingEventBatches) {
    return stockRebalancingEventBatches.stream().map(stockRebalancingEventBatchConverter::convert)
        .collect(
            Collectors.toList());
  }
}
