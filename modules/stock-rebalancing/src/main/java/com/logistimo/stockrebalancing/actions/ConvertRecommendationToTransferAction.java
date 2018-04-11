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

import com.logistimo.AppFactory;
import com.logistimo.exception.LogiException;
import com.logistimo.exception.ValidationException;
import com.logistimo.models.RequestSource;
import com.logistimo.orders.actions.AddDemandItemsToOrderAction;
import com.logistimo.orders.actions.CreateOrderAction;
import com.logistimo.orders.actions.OrderAllocationAction;
import com.logistimo.orders.entity.IOrder;
import com.logistimo.orders.models.AddDemandItemsRequest;
import com.logistimo.orders.models.DemandAllocationRequest;
import com.logistimo.orders.models.DemandBatchAllocationRequest;
import com.logistimo.orders.models.DemandRequest;
import com.logistimo.orders.models.OrderAllocationRequest;
import com.logistimo.orders.models.OrderRequest;
import com.logistimo.orders.models.OrderType;
import com.logistimo.services.ServiceException;
import com.logistimo.services.impl.PMF;
import com.logistimo.stockrebalancing.entity.RecommendedTransfer;
import com.logistimo.stockrebalancing.entity.RecommendedTransferStatus;
import com.logistimo.stockrebalancing.models.ConvertRecommendationRequest;
import com.logistimo.stockrebalancing.models.RecommendationStatusModel;
import com.logistimo.stockrebalancing.repository.IRecommendedTransferRepository;
import com.logistimo.utils.BigUtil;

import org.apache.camel.ExchangePattern;
import org.apache.camel.ProducerTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import javax.jdo.PersistenceManager;
import javax.jdo.Transaction;

/**
 * Created by charan on 30/03/18.
 */
@Component
public class ConvertRecommendationToTransferAction {

  @Autowired
  private CreateOrderAction createOrderAction;

  @Autowired
  private AddDemandItemsToOrderAction addDemandItemsToOrderAction;

  @Autowired
  private OrderAllocationAction orderAllocationAction;

  @Autowired
  private IRecommendedTransferRepository recommendedTransferRepository;

  @Transactional
  public IOrder invoke(ConvertRecommendationRequest convertRecommendationRequest)
      throws LogiException {
    RecommendedTransfer
        recommendedTransfer =
        recommendedTransferRepository.findOne(convertRecommendationRequest.getRecommendationId());
    if (recommendedTransfer.isTransferInitiated()) {
      throw new ValidationException("SR002", recommendedTransfer.getTransferId());
    }
    PersistenceManager persistenceManager = PMF.get().getPersistenceManager();
    Transaction tx = persistenceManager.currentTransaction();
    try {
      tx.begin();
      IOrder order;
      if (convertRecommendationRequest.getOrderId() != null) {
        order =
            addToExistingTransfer(convertRecommendationRequest, recommendedTransfer,
                persistenceManager);
      } else {
        order =
            createNewTransfer(convertRecommendationRequest, recommendedTransfer,
                persistenceManager);
      }

      if (hasRecommendedBatches(recommendedTransfer)) {
        //Trigger allocations
        orderAllocationAction.invoke(OrderAllocationRequest.builder()
                .orderId(order.getOrderId())
                .userId(convertRecommendationRequest.getUsername())
                .demandRequest(DemandAllocationRequest.builder()
                    .materialId(recommendedTransfer.getMaterialId())
                    .demandBatchAllocationRequests(buildBatchRequests(recommendedTransfer))
                    .build())
                .persistenceManager(persistenceManager)
                .build()
        );
      }
      updateRecommendationStatus(order, recommendedTransfer);
      tx.commit();
      return order;
    } catch (Exception e) {
      tx.rollback();
      throw e;
    } finally {
      persistenceManager.close();
    }
  }

  private boolean hasRecommendedBatches(RecommendedTransfer recommendedTransfer) {
    return recommendedTransfer.getSourceStockRebalancingEvent().getStockRebalancingEventBatches()
        != null
        && !recommendedTransfer.getSourceStockRebalancingEvent().getStockRebalancingEventBatches()
        .isEmpty();
  }

  private IOrder createNewTransfer(ConvertRecommendationRequest convertRecommendationRequest,
                                   RecommendedTransfer recommendedTransfer,
                                   PersistenceManager persistenceManager) throws ServiceException {
    IOrder order;
    OrderRequest
        orderRequest =
        buildOrderRequest(recommendedTransfer, convertRecommendationRequest.getUsername(),
            convertRecommendationRequest.getDomainId(), persistenceManager);
    order = createOrderAction.invoke(orderRequest).getOrder();
    return order;
  }

  private IOrder addToExistingTransfer(ConvertRecommendationRequest convertRecommendationRequest,
                                       RecommendedTransfer recommendedTransfer,
                                       PersistenceManager persistenceManager) throws LogiException {
    IOrder order;
    order = addDemandItemsToOrderAction.invoke(buildAddDemandItemsRequest(recommendedTransfer,
        convertRecommendationRequest.getUsername(),
        convertRecommendationRequest.getDomainId(), persistenceManager,
        convertRecommendationRequest.getOrderId()));
    return order;
  }

  private AddDemandItemsRequest buildAddDemandItemsRequest(RecommendedTransfer recommendedTransfer,
                                                           String username,
                                                           Long domainId,
                                                           PersistenceManager persistenceManager,
                                                           Long orderId) {
    return AddDemandItemsRequest.builder().domainId(domainId)
        .persistenceManager(persistenceManager)
        .orderId(orderId)
        .userId(username)
        .demandRequest(buildDemandRequest(recommendedTransfer)).build();
  }

  private void updateRecommendationStatus(IOrder order, RecommendedTransfer recommendedTransfer) {
    ProducerTemplate camelTemplate =
        AppFactory.get().getTaskService().getContext()
            .getBean("camel-client", ProducerTemplate.class);
    camelTemplate.sendBody("direct:stock-rebalancing-recommendation-status", ExchangePattern.InOnly,
        buildRecommendationStatusModel(order, recommendedTransfer));
  }

  private RecommendationStatusModel buildRecommendationStatusModel(IOrder order,
                                                                   RecommendedTransfer recommendedTransfer) {
    return new RecommendationStatusModel(recommendedTransfer.getId(), order.getOrderId(),
        RecommendedTransferStatus.DONE);
  }

  private Collection<? extends DemandBatchAllocationRequest> buildBatchRequests(
      RecommendedTransfer recommendedTransfer) {
    final BigDecimal[] remaining = {recommendedTransfer.getQuantity()};
    List<DemandBatchAllocationRequest> demandBatchAllocationRequestList = new ArrayList<>();
    recommendedTransfer.getSourceStockRebalancingEvent().getStockRebalancingEventBatches()
        .forEach(recommendedTransferBatch -> {
          if (BigUtil.greaterThanZero(remaining[0])) {
            BigDecimal
                batchQuantity =
                remaining[0].min(recommendedTransferBatch.getTransferQuantity());
            demandBatchAllocationRequestList
                .add(new DemandBatchAllocationRequest(recommendedTransferBatch.getBatchId(),
                    batchQuantity, null));
            remaining[0] = remaining[0].subtract(batchQuantity);
          }
        });
    return demandBatchAllocationRequestList;
  }

  private OrderRequest buildOrderRequest(RecommendedTransfer recommendedTransfer, String username,
                                         Long domainId, PersistenceManager persistenceManager) {
    return OrderRequest.builder()
        .customerId(recommendedTransfer.getDestinationStockRebalancingEvent().getKioskId())
        .vendorId(recommendedTransfer.getSourceStockRebalancingEvent().getKioskId())
        .orderType(OrderType.TRANSFER)
        .requestSource(RequestSource.WEB)
        .userId(username)
        .demandRequest(buildDemandRequest(recommendedTransfer))
        .domainId(domainId)
        .persistenceManager(persistenceManager)
        .build();
  }

  private DemandRequest buildDemandRequest(RecommendedTransfer recommendedTransfer) {
    return new DemandRequest(recommendedTransfer.getMaterialId(),
        recommendedTransfer.getQuantity(), null);
  }
}
