/*
 * Copyright © 2017 Logistimo.
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

package com.logistimo.config.utils.eventsummary;

import com.logistimo.config.models.EventSummaryConfigModel;

import junit.framework.TestCase;

import org.junit.Test;

import java.util.List;

/**
 * Created by vani on 11/10/17.
 */
public class EventSummaryTemplateLoaderTest extends TestCase {
  private static final int NUM_EVENTS = 9;
  private static final String CATEGORY_INVENTORY = "inventory";
  private static final String CATEGORY_ASSETS = "assets";
  private static final String CATEGORY_ACTIVITY = "activity";
  private static final String CATEGORY_SUPPLY = "supply";
  private static final String TYPE_STOCK_OUTS = "stock_outs";
  private static final String TYPE_STOCK_EXPIRY = "stock_expiry";
  private static final String TYPE_INVENTORY_PERFORMANCE = "inventory_performance_by_entity";
  private static final String TYPE_HEATING = "heating";
  private static final String TYPE_FREEZING = "freezing";
  private static final String TYPE_ASSET_PERFORMANCE = "asset_performance_by_entity";
  private static final String TYPE_LOGIN = "login";
  private static final String TYPE_DATA_ENTRY_PERFORMANCE = "data_entry_performance_by_entity";
  private static final String TYPE_SUPPLY_PERFORMANCE = "supply_performance";

  @Test
  public void testGetDefaultTemplate() throws Exception {
    EventSummaryConfigModel defaultTemplate = EventSummaryTemplateLoader.getDefaultTemplate();
    assertNotNull(defaultTemplate);
    assertEquals(defaultTemplate.getEvents().size(), NUM_EVENTS);
    assertEquals(TYPE_STOCK_OUTS, getEventType(defaultTemplate.getEvents(), 0));
    assertEquals(CATEGORY_INVENTORY, getEventCategory(defaultTemplate.getEvents(), 0));
    assertEquals(TYPE_STOCK_EXPIRY, getEventType(defaultTemplate.getEvents(), 1));
    assertEquals(CATEGORY_INVENTORY, getEventCategory(defaultTemplate.getEvents(), 1));
    assertEquals(TYPE_INVENTORY_PERFORMANCE, getEventType(defaultTemplate.getEvents(), 2));
    assertEquals(CATEGORY_INVENTORY, getEventCategory(defaultTemplate.getEvents(), 2));
    assertEquals(TYPE_HEATING, getEventType(defaultTemplate.getEvents(), 3));
    assertEquals(CATEGORY_ASSETS, getEventCategory(defaultTemplate.getEvents(), 3));
    assertEquals(TYPE_FREEZING, getEventType(defaultTemplate.getEvents(), 4));
    assertEquals(CATEGORY_ASSETS, getEventCategory(defaultTemplate.getEvents(), 4));
    assertEquals(TYPE_ASSET_PERFORMANCE, getEventType(defaultTemplate.getEvents(), 5));
    assertEquals(CATEGORY_ASSETS, getEventCategory(defaultTemplate.getEvents(), 5));
    assertEquals(TYPE_LOGIN, getEventType(defaultTemplate.getEvents(), 6));
    assertEquals(CATEGORY_ACTIVITY, getEventCategory(defaultTemplate.getEvents(), 6));
    assertEquals(TYPE_DATA_ENTRY_PERFORMANCE, getEventType(defaultTemplate.getEvents(), 7));
    assertEquals(CATEGORY_ACTIVITY, getEventCategory(defaultTemplate.getEvents(), 7));
    assertEquals(TYPE_SUPPLY_PERFORMANCE, getEventType(defaultTemplate.getEvents(), 8));
    assertEquals(CATEGORY_SUPPLY, getEventCategory(defaultTemplate.getEvents(), 8));
  }

  private String getEventType(List<EventSummaryConfigModel.Events> events, int index) {
    return events.get(index).getType();
  }

  private String getEventCategory(List<EventSummaryConfigModel.Events> events, int index) {
    return events.get(index).getCategory();
  }


}