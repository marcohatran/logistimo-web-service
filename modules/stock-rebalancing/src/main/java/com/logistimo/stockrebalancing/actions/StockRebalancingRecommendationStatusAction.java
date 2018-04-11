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

package com.logistimo.stockrebalancing.actions;

import com.logistimo.context.StaticApplicationContext;
import com.logistimo.logger.XLog;
import com.logistimo.stockrebalancing.entity.RecommendedTransfer;
import com.logistimo.stockrebalancing.entity.RecommendedTransferStatus;
import com.logistimo.stockrebalancing.entity.StockRebalancingEvent;
import com.logistimo.stockrebalancing.entity.StockRebalancingEventStatus;
import com.logistimo.stockrebalancing.models.RecommendationStatusModel;
import com.logistimo.stockrebalancing.models.RecommendedTransferFilters;
import com.logistimo.stockrebalancing.repository.IRecommendedTransferRepository;
import com.logistimo.stockrebalancing.repository.IStockRebalancingEventsRepository;
import com.logistimo.utils.BigUtil;

import org.apache.camel.Handler;

import java.math.BigDecimal;
import java.util.List;

import javax.transaction.Transactional;

/**
 * Created by charan on 30/03/18.
 */
public class StockRebalancingRecommendationStatusAction {
  private static final XLog xLogger = XLog.getLog(StockRebalancingRecommendationStatusAction.class);

  @Handler
  @Transactional
  public void execute(RecommendationStatusModel recommendationStatus) {
    xLogger.info("Recommendation converted to transfer {0}", recommendationStatus);
    IRecommendedTransferRepository recommendedTransferRepository = StaticApplicationContext.getBean(
        IRecommendedTransferRepository.class);
    RecommendedTransfer
        recommendedTransfer =
        recommendedTransferRepository.findOne(recommendationStatus.getRecommendationId());
    recommendedTransfer.setStatus(RecommendedTransferStatus.DONE);
    recommendedTransfer.setTransferId(recommendationStatus.getOrderId());
    recommendedTransferRepository.save(recommendedTransfer);
    checkAndUpdateEventStatus(recommendedTransfer.getSourceStockRebalancingEvent());
    checkAndUpdateEventStatus(recommendedTransfer.getDestinationStockRebalancingEvent());
  }

  private void checkAndUpdateEventStatus(StockRebalancingEvent stockRebalancingEvent) {

    GetRecommendedTransfersAction
        getRecommendedTransfersAction =
        StaticApplicationContext.getBean(GetRecommendedTransfersAction.class);

    IRecommendedTransferRepository recommendedTransferRepository = StaticApplicationContext.getBean(
        IRecommendedTransferRepository.class);

    List<RecommendedTransfer>
        allRecommendedTransfers =
        getRecommendedTransfersAction.invoke(
            RecommendedTransferFilters.builder().eventId(stockRebalancingEvent.getId()).build());

    BigDecimal transferInitiatedQuantity = allRecommendedTransfers.stream()
        .filter(recommendedTransfer -> recommendedTransfer.getStatus()
            .equals(RecommendedTransferStatus.DONE))
        .map(RecommendedTransfer::getQuantity)
        .reduce(BigDecimal.ZERO, BigDecimal::add);

    if (BigUtil.greaterThanEquals(transferInitiatedQuantity, stockRebalancingEvent.getQuantity())) {
      allRecommendedTransfers.stream()
          .filter(recommendedTransfer -> !recommendedTransfer.getStatus()
              .equals(RecommendedTransferStatus.DONE))
          .forEach(recommendedTransfer -> {
            recommendedTransfer.setStatus(RecommendedTransferStatus.REJECTED);
            recommendedTransferRepository.save(recommendedTransfer);
          });
      stockRebalancingEvent.setStatus(StockRebalancingEventStatus.DONE);
    } else {
      stockRebalancingEvent.setStatus(StockRebalancingEventStatus.PARTIAL);
    }

    IStockRebalancingEventsRepository
        eventsRepository =
        StaticApplicationContext.getBean(IStockRebalancingEventsRepository.class);
    eventsRepository.save(stockRebalancingEvent);

  }
}
