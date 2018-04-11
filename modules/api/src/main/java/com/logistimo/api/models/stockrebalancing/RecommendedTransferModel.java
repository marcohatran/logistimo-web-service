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

package com.logistimo.api.models.stockrebalancing;

import com.google.gson.annotations.SerializedName;

import com.logistimo.orders.models.OrderModel;

import java.math.BigDecimal;
import java.util.List;

import lombok.Data;

/**
 * Created by naveensnair on 28/03/18.
 */

@Data
public class RecommendedTransferModel {

  @SerializedName("id")
  private String id;

  @SerializedName("entity_id")
  private Long entityId;

  @SerializedName("entity_name")
  private String entityName;

  @SerializedName("location")
  private String location;

  @SerializedName("material_id")
  private Long materialId;

  @SerializedName("material_name")
  private String materialName;

  @SerializedName("quantity")
  private BigDecimal quantity;

  @SerializedName("source_short_code")
  private String sourceShortCode;

  @SerializedName("destination_short_code")
  private String destinationShortCode;

  @SerializedName("current_stock")
  private BigDecimal currentStock;

  @SerializedName("stock_duration_before_transfer")
  private Integer stockDurationBeforeTransfer;

  @SerializedName("stock_after_transfer")
  private Long stockAfterTransfer;

  @SerializedName("stock_duration_after_transfer")
  private Integer stockDurationAfterTransfer;

  @SerializedName("min")
  private BigDecimal minStock;

  @SerializedName("max")
  private BigDecimal maxStock;

  @SerializedName("value")
  private BigDecimal value;

  @SerializedName("cost")
  private BigDecimal cost;

  @SerializedName("batch_details")
  private List<StockRebalancingEventBatchModel> batchDetails;

  @SerializedName("open_transfers")
  private List<OrderModel> openTransfers;

  @SerializedName("transfer")
  private OrderModel transfer;

}
