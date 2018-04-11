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

package com.logistimo.api.controllers;

import com.logistimo.api.builders.stockrebalancing.RecommendedTransferBuilder;
import com.logistimo.api.converters.StockRebalancingEventConverter;
import com.logistimo.api.models.AddRecommendationToTransferRequest;
import com.logistimo.api.models.CreateTransferRecommendationRequest;
import com.logistimo.api.models.stockrebalancing.RecommendedTransferModel;
import com.logistimo.api.models.stockrebalancing.StockRebalancingEventModel;
import com.logistimo.approvals.builders.RestResponsePageBuilder;
import com.logistimo.approvals.client.models.RestResponsePage;
import com.logistimo.auth.GenericAuthoriser;
import com.logistimo.auth.utils.SecurityUtils;
import com.logistimo.entities.auth.EntityAuthoriser;
import com.logistimo.exception.ForbiddenAccessException;
import com.logistimo.exception.LogiException;
import com.logistimo.exception.UnauthorizedException;
import com.logistimo.orders.builders.OrderBuilder;
import com.logistimo.orders.entity.IOrder;
import com.logistimo.orders.models.OrderModel;
import com.logistimo.services.ObjectNotFoundException;
import com.logistimo.services.ServiceException;
import com.logistimo.stockrebalancing.actions.ConvertRecommendationToTransferAction;
import com.logistimo.stockrebalancing.actions.GetRecommendedTransfersAction;
import com.logistimo.stockrebalancing.actions.GetStockRebalancingEventsAction;
import com.logistimo.stockrebalancing.actions.ScheduleStockRebalancingAction;
import com.logistimo.stockrebalancing.actions.StockRebalancingAction;
import com.logistimo.stockrebalancing.entity.RecommendedTransfer;
import com.logistimo.stockrebalancing.models.ConvertRecommendationRequest;
import com.logistimo.stockrebalancing.models.RecommendedTransferFilters;
import com.logistimo.stockrebalancing.models.StockRebalancingFilters;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

import java.util.List;

/**
 * Created by charan on 19/03/18.
 */

@Controller
@RequestMapping("/stock-rebalancing")
public class StockRebalancingController {

  @Autowired
  private ScheduleStockRebalancingAction scheduleStockRebalancingAction;

  @Autowired
  private StockRebalancingAction stockRebalancingAction;

  @Autowired
  private GetStockRebalancingEventsAction getStockRebalancingEventsAction;

  @Autowired
  private GetRecommendedTransfersAction getRecommendedTransfersAction;

  @Autowired
  private ConvertRecommendationToTransferAction convertRecommendationToTransferAction;

  @Autowired
  private StockRebalancingEventConverter stockRebalancingEventConverter;

  @Autowired
  private RecommendedTransferBuilder recommendedTransferBuilder;

  @Autowired
  private OrderBuilder orderBuilder;

  @RequestMapping(value = "/automate", method = RequestMethod.GET)
  public
  @ResponseBody
  void automateOrders(@RequestParam(value = "domain_id") Long domainId) throws ServiceException {
    stockRebalancingAction.invoke(domainId);
  }

  @RequestMapping(value = "/schedule-automation", method = RequestMethod.GET)
  public
  @ResponseBody
  void scheduleOrderAutomation() throws ServiceException {
    scheduleStockRebalancingAction.invoke();
  }

  @RequestMapping(value = "/events", method = RequestMethod.GET)
  public
  @ResponseBody
  RestResponsePage<StockRebalancingEventModel> getEvents(
      @RequestParam(value = "entity_id", required = false) Long entityId,
      @RequestParam(value = "material_id", required = false) Long materialId,
      @RequestParam(value = "event_type", required = false) String eventType,
      @RequestParam(value = "entity_tag", required = false) String entityTag,
      @RequestParam(value = "material_tag", required = false) String materialTag,
      @RequestParam(value = "offset", defaultValue = "0") Integer offset,
      @RequestParam(value = "size", defaultValue = "50") Integer size) throws ServiceException {

    if (!SecurityUtils.isAdmin()) {
      throw new UnauthorizedException("G002");
    }
    if (entityId != null && !EntityAuthoriser.authoriseEntity(entityId)) {
      throw new UnauthorizedException("G002");
    }

    Page<StockRebalancingEventModel>
        eventModelPage =
        getStockRebalancingEventsAction.invoke(StockRebalancingFilters.builder()
                .domainId(SecurityUtils.getCurrentDomainId())
                .entityId(entityId)
                .materialId(materialId)
                .entityTag(entityTag)
                .materialTag(materialTag)
                .triggerShortCode(eventType).build()
            , offset, size).map(stockRebalancingEventConverter);
    return
        new RestResponsePageBuilder<StockRebalancingEventModel>().withContent(
            eventModelPage.getContent()).withOffset(offset).withSize(
            eventModelPage.getNumberOfElements()).withTotalElements(
            eventModelPage.getTotalElements()).build();

  }

  @RequestMapping(value = "/events/{eventId}/recommendations", method = RequestMethod.GET)
  public
  @ResponseBody
  RestResponsePage<RecommendedTransferModel> getRecommendationsByEvent(
      @PathVariable String eventId) {

    if (!SecurityUtils.isAdmin()) {
      throw new UnauthorizedException("G002");
    }

    List<RecommendedTransfer> recommendedTransfers = getRecommendedTransfersAction.invoke(
        RecommendedTransferFilters.builder().eventId(eventId).build());
    return new RestResponsePage<>(0, recommendedTransfers.size(), recommendedTransfers.size(),
        recommendedTransferBuilder.build(
            recommendedTransfers, eventId), null);
  }

  @RequestMapping(value = "/create-transfer", method = RequestMethod.POST)
  public
  @ResponseBody
  OrderModel createTransfer(
      @RequestBody CreateTransferRecommendationRequest createTransferRecommendationRequest)
      throws Exception {
    checkTransferPermission(createTransferRecommendationRequest.getRecommendationId());
    IOrder
        order =
        convertRecommendationToTransferAction.invoke(ConvertRecommendationRequest.builder()
            .domainId(SecurityUtils.getCurrentDomainId())
            .recommendationId(createTransferRecommendationRequest.getRecommendationId())
            .username(SecurityUtils.getUsername()).build());
    return orderBuilder.build(order.getOrderId());
  }

  @RequestMapping(value = "/add-to-transfer", method = RequestMethod.POST)
  public
  @ResponseBody
  OrderModel addRecommendationToTransfer(
      @RequestBody AddRecommendationToTransferRequest addRecommendationToTransferRequest)
      throws LogiException {
    checkTransferPermission(addRecommendationToTransferRequest.getRecommendationId());
    IOrder order = convertRecommendationToTransferAction.invoke(
        ConvertRecommendationRequest.builder()
            .domainId(SecurityUtils.getCurrentDomainId())
            .recommendationId(addRecommendationToTransferRequest.getRecommendationId())
            .username(SecurityUtils.getUsername())
            .orderId(addRecommendationToTransferRequest.getTransferId())
            .build());
    return orderBuilder.build(order.getOrderId());
  }

  private void checkTransferPermission(
      String recommendedTransferId)
      throws ServiceException {
    List<RecommendedTransfer>
        recommendedTransferList = getRecommendedTransfersAction.invoke(
        RecommendedTransferFilters.builder().id(
            recommendedTransferId).build());
    if (recommendedTransferList == null || recommendedTransferList.isEmpty()) {
      throw new ObjectNotFoundException("SR001", recommendedTransferId);
    }
    RecommendedTransfer recommendedTransfer = recommendedTransferList.get(0);
    if (!(GenericAuthoriser.authoriseAdmin() && EntityAuthoriser
        .authoriseEntity(recommendedTransfer.getSourceStockRebalancingEvent().getKioskId())
        && EntityAuthoriser
        .authoriseEntity(recommendedTransfer.getDestinationStockRebalancingEvent().getKioskId()))) {
      throw new ForbiddenAccessException("G003");
    }
  }

}
