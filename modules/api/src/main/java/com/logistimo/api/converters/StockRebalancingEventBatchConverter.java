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

import com.logistimo.api.models.stockrebalancing.StockRebalancingEventBatchModel;
import com.logistimo.stockrebalancing.entity.StockRebalancingEventBatch;
import com.logistimo.utils.LocalDateUtil;

import org.springframework.core.convert.converter.Converter;
import org.springframework.stereotype.Component;

/**
 * Created by charan on 02/04/18.
 */
@Component
public class StockRebalancingEventBatchConverter implements
    Converter<StockRebalancingEventBatch, StockRebalancingEventBatchModel> {

  private static final String DATE_FORMAT = "dd/MM/yyyy";
  @Override
  public StockRebalancingEventBatchModel convert(StockRebalancingEventBatch source) {
    StockRebalancingEventBatchModel batchModel = new StockRebalancingEventBatchModel();
    batchModel.setBatchId(source.getBatchId());
    if(source.getExpiryDate() != null) {
      batchModel.setExpiry(LocalDateUtil.formatCustom(source.getExpiryDate(),
          DATE_FORMAT, null));
    }
    if(source.getManufactureDate() != null) {
      batchModel.setManufacturedDate(LocalDateUtil.formatCustom(source.getManufactureDate(),
          DATE_FORMAT, null));
    }
    batchModel.setManufacturer(source.getManufacturerName());
    batchModel.setQuantity(source.getTransferQuantity());
    return batchModel;
  }
}
