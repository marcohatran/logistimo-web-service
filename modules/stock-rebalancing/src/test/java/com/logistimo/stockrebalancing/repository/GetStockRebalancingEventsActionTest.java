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

package com.logistimo.stockrebalancing.repository;

import com.logistimo.stockrebalancing.actions.GetStockRebalancingEventsAction;
import com.logistimo.stockrebalancing.config.TestConfig;
import com.logistimo.stockrebalancing.models.StockRebalancingFilters;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

/**
 * Created by charan on 02/04/18.
 */
@ContextConfiguration(classes = {TestConfig.class})
@RunWith(SpringJUnit4ClassRunner.class)
public class GetStockRebalancingEventsActionTest {
  @Autowired
  private GetStockRebalancingEventsAction getStockRebalancingEventsAction;

  @Test
  public void testInvoke() {
    StockRebalancingFilters filters = StockRebalancingFilters.builder().build();
    getStockRebalancingEventsAction.invoke(filters, 0, 10);
  }

  @Test
  public void testInvokeWithKioskTag() {
    StockRebalancingFilters filters = StockRebalancingFilters.builder().entityTag(
        "test_tag").build();
    getStockRebalancingEventsAction.invoke(filters, 0, 10);
  }

  @Test
  public void testInvokeAll() {
    StockRebalancingFilters filters = StockRebalancingFilters.builder()
        .entityId(1l)
        .domainId(2l)
        .entityTag("test")
        .materialId(1l)
        .materialTag("test_material")
        .triggerShortCode("short_code")
        .build();
    getStockRebalancingEventsAction.invoke(filters, 0, 10);
  }

}
