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

import com.logistimo.jpa.OffsetBasedPageRequest;
import com.logistimo.stockrebalancing.entity.StockRebalancingEvent;
import com.logistimo.stockrebalancing.models.StockRebalancingFilters;
import com.logistimo.stockrebalancing.repository.IStockRebalancingEventsRepository;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Component;

import static com.logistimo.stockrebalancing.specifications.StockRebalancingEventSpecifications.domainIdIs;
import static com.logistimo.stockrebalancing.specifications.StockRebalancingEventSpecifications.eventIsActive;
import static com.logistimo.stockrebalancing.specifications.StockRebalancingEventSpecifications.eventIsPrimary;
import static com.logistimo.stockrebalancing.specifications.StockRebalancingEventSpecifications.kioskIdIs;
import static com.logistimo.stockrebalancing.specifications.StockRebalancingEventSpecifications.kioskTagIs;
import static com.logistimo.stockrebalancing.specifications.StockRebalancingEventSpecifications.materialIdIs;
import static com.logistimo.stockrebalancing.specifications.StockRebalancingEventSpecifications.materialTagIs;
import static com.logistimo.stockrebalancing.specifications.StockRebalancingEventSpecifications.triggerShortCodeIs;
import static org.springframework.data.jpa.domain.Specifications.where;

/**
 * Created by charan on 21/03/18.
 */

@Component
public class GetStockRebalancingEventsAction {

  @Autowired
  private IStockRebalancingEventsRepository stockRebalancingEventsRepository;

  public Page<StockRebalancingEvent> invoke(StockRebalancingFilters filters, Integer offset,
                                            Integer size) {

    Pageable pageable = new OffsetBasedPageRequest(offset, size, new Sort(new Sort.Order(
        Sort.Direction.DESC, "value")));

    return stockRebalancingEventsRepository.findAll(
        where(kioskIdIs(filters.getEntityId()))
            .and(materialIdIs(filters.getMaterialId()))
            .and(triggerShortCodeIs(
                filters.getTriggerShortCode()))
            .and(domainIdIs(filters.getDomainId()))
            .and(eventIsActive())
            .and(eventIsPrimary())
            .and(materialTagIs(filters.getMaterialTag()))
            .and(kioskTagIs(filters.getEntityTag())), pageable);
  }
}
