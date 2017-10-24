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

package com.logistimo.config.models;

import com.logistimo.config.utils.eventsummary.EventSummaryTemplateLoader;
import com.logistimo.logger.XLog;

import junit.framework.TestCase;

import org.junit.Test;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Created by vani on 16/10/17.
 */
public class EventSummaryConfigModelTest extends TestCase {
  private static final XLog LOGGER = XLog.getLog(EventSummaryConfigModelTest.class);
  private static final String INVENTORY = "inventory";
  private static final String ASSETS = "assets";
  private static final String STOCK_OUTS = "stock_outs";
  private static final String STOCK_EXPIRY = "stock_expiry";
  private static final String INVENTORY_PERFORMANCE_BY_ENTITY = "inventory_performance_by_entity";
  private static final String HEATING = "heating";
  private static final String FREEZING = "freezing";
  private static final String ASSET_PERFORMANCE_BY_ENTITY = "asset_performance_by_entity";
  private static final String CATEGORY_TYPE_DELIMITER = "$";
  private static final String DURATION = "duration";
  private static final String TEN = "10";
  private static final String DAYS = "days";
  private static final String GREATER_THAN_OR_EQUAL_TO = ">=";
  private static final String INCLUDE_MATERIAL_TAGS = "include_material_tags";
  private static final String ANTIBIOTIC = "Antibiotic";
  private static final String POPULATE_THRESHOLD_MAP = "populateThresholdMap";
  private static final String BUILD_CONDITIONS = "buildConditions";
  private static final String UPDATE_THRESHOLDS = "updateThresholds";

  @Test
  public void testAddEvents() {
    EventSummaryConfigModel eventSummaryConfigModel = new EventSummaryConfigModel();
    List<EventSummaryConfigModel.Events> eventsList = new ArrayList<>(0);
    eventsList.add(getEvent(eventSummaryConfigModel, INVENTORY, STOCK_OUTS));
    eventsList.add(getEvent(eventSummaryConfigModel, ASSETS, HEATING));
    eventSummaryConfigModel.addEvents(eventsList);
    assertEquals(eventSummaryConfigModel.getEvents().size(), 2);
   }

  @Test
  public void testRemoveEventsByCategory() {
    EventSummaryConfigModel eventSummaryConfigModel = new EventSummaryConfigModel();
    List<EventSummaryConfigModel.Events> eventsList = new ArrayList<>(0);
    eventsList.add(getEvent(eventSummaryConfigModel, INVENTORY, STOCK_OUTS));
    eventsList.add(getEvent(eventSummaryConfigModel, INVENTORY, STOCK_EXPIRY));
    eventsList.add(getEvent(eventSummaryConfigModel, INVENTORY, INVENTORY_PERFORMANCE_BY_ENTITY));
    eventsList.add(getEvent(eventSummaryConfigModel, ASSETS, HEATING));
    eventsList.add(getEvent(eventSummaryConfigModel, ASSETS, FREEZING));
    eventsList.add(getEvent(eventSummaryConfigModel, ASSETS, ASSET_PERFORMANCE_BY_ENTITY));
    eventSummaryConfigModel.addEvents(eventsList);
    eventSummaryConfigModel.removeEventsByCategory(ASSETS);
    assertEquals(eventSummaryConfigModel.getEvents().size(), 3);
  }
  @Test
  public void testPopulateThresholdMap() {
    try {
      EventSummaryConfigModel defaultTemplate = getDefaultTemplate();
      assertNotNull(defaultTemplate);
      EventSummaryConfigModel eventSummaryConfigModel = new EventSummaryConfigModel();
      Method populateThresholdMap = eventSummaryConfigModel.getClass().getDeclaredMethod(POPULATE_THRESHOLD_MAP, new Class[] {List.class});
      populateThresholdMap.setAccessible(true);
      Map<String, Map<String, EventSummaryConfigModel.Condition>> map = (Map) populateThresholdMap.invoke(
          eventSummaryConfigModel, new Object[]{defaultTemplate.getEvents()});
      assertNotNull(map);
      assertEquals(map.size(),9);
      List<String> expectedMapKeys = defaultTemplate.getEvents().stream().map(
          events -> events.getCategory() + CATEGORY_TYPE_DELIMITER + events.getType())
      .collect(Collectors.toList());
      List mapKeys = new ArrayList<>(map.keySet());
      assertTrue(mapKeys.equals(expectedMapKeys));
    } catch (Exception e) {
      LOGGER.warn("Ignoring exception" , e);
    }
  }

  @Test
  public void testUpdateThresholdsWithoutThresholdsInDBEvents() {
    try {
      EventSummaryConfigModel defaultTemplate = getDefaultTemplate();
      assertNotNull(defaultTemplate);
      EventSummaryConfigModel eventSummaryConfigModel = new EventSummaryConfigModel();
      Method populateThresholdMap = eventSummaryConfigModel.getClass().getDeclaredMethod(POPULATE_THRESHOLD_MAP, new Class[] {List.class});
      populateThresholdMap.setAccessible(true);
      Map<String, Map<String, EventSummaryConfigModel.Condition>> map = (Map) populateThresholdMap.invoke(
          eventSummaryConfigModel, new Object[]{defaultTemplate.getEvents()});
      Map<String,EventSummaryConfigModel.Condition> keyConditionMap = map.get(INVENTORY + CATEGORY_TYPE_DELIMITER + STOCK_OUTS);
      EventSummaryConfigModel.Events events = getEvent(eventSummaryConfigModel, INVENTORY,
          STOCK_OUTS);
      assertNull(events.getThresholds());
      Method updateThresholds = eventSummaryConfigModel.getClass().getDeclaredMethod(UPDATE_THRESHOLDS, new Class[] {EventSummaryConfigModel.Events.class, Map.class});
      updateThresholds.setAccessible(true);
      updateThresholds.invoke(
          eventSummaryConfigModel, new Object[]{events,keyConditionMap});
      assertEquals(events.getThresholds().size(), 1);
      assertEquals(events.getThresholds().get(0).getConditions().size(),5);
    } catch (Exception e) {
      LOGGER.warn("Ignoring exception" , e);
    }
  }

  @Test
  public void testUpdateThresholdsWithThresholdsInDBEvents() {
    try {
      EventSummaryConfigModel defaultTemplate = getDefaultTemplate();
      assertNotNull(defaultTemplate);
      EventSummaryConfigModel eventSummaryConfigModel = new EventSummaryConfigModel();
      Method populateThresholdMap = eventSummaryConfigModel.getClass().getDeclaredMethod(POPULATE_THRESHOLD_MAP, new Class[] {List.class});
      populateThresholdMap.setAccessible(true);
      Map<String, Map<String, EventSummaryConfigModel.Condition>> map = (Map) populateThresholdMap.invoke(
          eventSummaryConfigModel, new Object[]{defaultTemplate.getEvents()});
      Map<String,EventSummaryConfigModel.Condition> keyConditionMap = map.get(INVENTORY + CATEGORY_TYPE_DELIMITER + STOCK_OUTS);
      EventSummaryConfigModel.Events events = getEventWithThresholds(eventSummaryConfigModel,
          INVENTORY,
          STOCK_OUTS);
      assertNotNull(events.getThresholds());
      assertEquals(events.getThresholds().get(0).getConditions().size(),2);
      Method updateThresholds = eventSummaryConfigModel.getClass().getDeclaredMethod(UPDATE_THRESHOLDS, new Class[] {EventSummaryConfigModel.Events.class, Map.class});
      updateThresholds.setAccessible(true);
      updateThresholds.invoke(
          eventSummaryConfigModel, new Object[]{events,keyConditionMap});
      assertEquals(events.getThresholds().size(), 1);
      assertEquals(events.getThresholds().get(0).getConditions().size(),5);
    } catch (Exception e) {
      LOGGER.warn("Ignoring exception" , e);
    }
  }

  @Test
  public void testBuildConditions() {
    try {
      EventSummaryConfigModel defaultTemplate = getDefaultTemplate();
      assertNotNull(defaultTemplate);
      EventSummaryConfigModel eventSummaryConfigModel = new EventSummaryConfigModel();
      Method populateThresholdMap = eventSummaryConfigModel.getClass().getDeclaredMethod(POPULATE_THRESHOLD_MAP, new Class[] {List.class});
      populateThresholdMap.setAccessible(true);
      Map<String, Map<String, EventSummaryConfigModel.Condition>> map = (Map) populateThresholdMap.invoke(
          eventSummaryConfigModel, new Object[]{defaultTemplate.getEvents()});
      Map<String,EventSummaryConfigModel.Condition> keyConditionMap = map.get(INVENTORY + CATEGORY_TYPE_DELIMITER + STOCK_OUTS);
      EventSummaryConfigModel.Events events = getEventWithThresholds(eventSummaryConfigModel,
          INVENTORY,
          STOCK_OUTS);
      assertNotNull(events.getThresholds());
      assertEquals(events.getThresholds().get(0).getConditions().size(),2);
      Method buildConditions = eventSummaryConfigModel.getClass().getDeclaredMethod(BUILD_CONDITIONS, new Class[] {EventSummaryConfigModel.Events.class, Map.class});
      buildConditions.setAccessible(true);
      buildConditions.invoke(
          eventSummaryConfigModel, new Object[]{events,keyConditionMap});
      assertEquals(events.getThresholds().size(), 1);
      assertEquals(events.getThresholds().get(0).getConditions().size(),5);
      assertEquals(events.getThresholds().get(0).getConditions().get(0).getName(), DURATION);
      assertEquals(events.getThresholds().get(0).getConditions().get(0).getValue(), TEN);
      assertEquals(events.getThresholds().get(0).getConditions().get(0).getUnits(), DAYS);
      assertEquals(events.getThresholds().get(0).getConditions().get(0).getOper(), GREATER_THAN_OR_EQUAL_TO);
      assertEquals(events.getThresholds().get(0).getConditions().get(0).getValues(),null);
      assertEquals(events.getThresholds().get(0).getConditions().get(1).getName(),INCLUDE_MATERIAL_TAGS);
      assertEquals(events.getThresholds().get(0).getConditions().get(1).getValue(),null);
      assertEquals(events.getThresholds().get(0).getConditions().get(1).getUnits(),"");
      assertEquals(events.getThresholds().get(0).getConditions().get(1).getOper(),"");
      assertEquals(events.getThresholds().get(0).getConditions().get(1).getValues().get(0),ANTIBIOTIC);
    } catch (Exception e) {
      LOGGER.warn("Ignoring exception" , e);
    }
  }
  @Test
  public void testBuildEventsWithoutDBValues() {
    try {
      EventSummaryConfigModel defaultTemplate = getDefaultTemplate();
      assertNotNull(defaultTemplate);
      EventSummaryConfigModel eventSummaryConfigModel = new EventSummaryConfigModel();
      eventSummaryConfigModel.buildEvents(defaultTemplate.getEvents());
      assertEquals(defaultTemplate.getEvents().size(), eventSummaryConfigModel.getEvents().size());
    } catch (Exception e) {
      LOGGER.warn("Exception while testing buildEvents without DB values", e);
    }
  }

  @Test
  public void testBuildEventsWithEventsThresholdsConditionsInDB() {
    try {
      EventSummaryConfigModel defaultTemplate = getDefaultTemplate();
      assertNotNull(defaultTemplate);
      EventSummaryConfigModel eventSummaryConfigModel = new EventSummaryConfigModel();
      List<EventSummaryConfigModel.Events> eventsList = new ArrayList<>(0);
      eventsList.add(getEventWithThresholds(eventSummaryConfigModel, INVENTORY, STOCK_OUTS));
      eventSummaryConfigModel.addEvents(eventsList);
      eventSummaryConfigModel.buildEvents(defaultTemplate.getEvents());
      assertEquals(eventSummaryConfigModel.getEvents().get(0).getThresholds().size(),1);
      assertEquals(eventSummaryConfigModel.getEvents().get(0).getThresholds().get(0).getConditions().get(0).getName(),DURATION);
      assertEquals(eventSummaryConfigModel.getEvents().get(0).getThresholds().get(0).getConditions().get(0).getValue(),TEN);
      assertEquals(eventSummaryConfigModel.getEvents().get(0).getThresholds().get(0).getConditions().get(0).getUnits(), DAYS);
      assertEquals(eventSummaryConfigModel.getEvents().get(0).getThresholds().get(0).getConditions().get(0).getOper(), GREATER_THAN_OR_EQUAL_TO);
      assertEquals(eventSummaryConfigModel.getEvents().get(0).getThresholds().size(),1);
      assertEquals(eventSummaryConfigModel.getEvents().get(0).getThresholds().get(0).getConditions().get(1).getName(), INCLUDE_MATERIAL_TAGS);
      assertEquals(eventSummaryConfigModel.getEvents().get(0).getThresholds().get(0).getConditions().get(1).getValues().size(), 1);
      assertEquals(eventSummaryConfigModel.getEvents().get(0).getThresholds().get(0).getConditions().get(1).getValues().get(0), ANTIBIOTIC);
    } catch (Exception e) {
      LOGGER.warn("Ignoring exception" , e);
    }
  }

  @Test
  public void testBuildEventsWithEventsWithoutThresholdsInDB() {
    try {
      EventSummaryConfigModel defaultTemplate = getDefaultTemplate();
      assertNotNull(defaultTemplate);
      EventSummaryConfigModel eventSummaryConfigModel = new EventSummaryConfigModel();
      List<EventSummaryConfigModel.Events> eventsList = new ArrayList<>(0);
      eventsList.add(getEvent(eventSummaryConfigModel, INVENTORY, STOCK_OUTS));
      eventsList.add(getEvent(eventSummaryConfigModel, INVENTORY, STOCK_EXPIRY));
      eventsList.add(getEvent(eventSummaryConfigModel, INVENTORY, INVENTORY_PERFORMANCE_BY_ENTITY));
      eventsList.add(getEvent(eventSummaryConfigModel, ASSETS, HEATING));
      eventsList.add(getEvent(eventSummaryConfigModel, ASSETS, FREEZING));
      eventsList.add(getEvent(eventSummaryConfigModel, ASSETS, ASSET_PERFORMANCE_BY_ENTITY));
      eventSummaryConfigModel.addEvents(eventsList);
      eventSummaryConfigModel.buildEvents(defaultTemplate.getEvents());
      assertEquals(defaultTemplate.getEvents().size(),eventSummaryConfigModel.getEvents().size());
    } catch (Exception e) {
      LOGGER.warn("Ignoring exception" , e);
    }
  }

  @Test
  public void testBuildEventsWithThresholdsRemovedfromDefaultTemplate() {
    try {
      EventSummaryConfigModel defaultTemplate = getDefaultTemplate();
      assertNotNull(defaultTemplate);
      // Remove a threshold from the default template
      defaultTemplate.removeEventsByCategory("inventory");
      EventSummaryConfigModel eventSummaryConfigModel = new EventSummaryConfigModel();
      List<EventSummaryConfigModel.Events> eventsList = new ArrayList<>(0);
      eventsList.add(getEventWithThresholds(eventSummaryConfigModel, INVENTORY, STOCK_OUTS));
      eventSummaryConfigModel.addEvents(eventsList);
      eventSummaryConfigModel.buildEvents(defaultTemplate.getEvents());
      assertEquals(eventSummaryConfigModel.getEvents().size(),6);
    } catch (Exception e) {
      LOGGER.warn("Ignoring exception" , e);
    }
  }

  @Test
  public void testCopy() {
    EventSummaryConfigModel eventSummaryConfigModel = new EventSummaryConfigModel();
    try {
      EventSummaryConfigModel eventSummaryConfigModelCopy = eventSummaryConfigModel.copy();
      assertNotSame(eventSummaryConfigModel, eventSummaryConfigModelCopy);
    } catch (Exception e) {
      LOGGER.warn("Ignoring exception" , e);
    }
  }

  @Test
  public  void testSetUniqueIdentifierWithSameConditions() {
    try {
      EventSummaryConfigModel defaultTemplate = getDefaultTemplate();
      assertNotNull(defaultTemplate);
      EventSummaryConfigModel eventSummaryConfigModel = new EventSummaryConfigModel();
      List<EventSummaryConfigModel.Events> eventsList = new ArrayList<>(0);
      eventsList.add(getEventWithThresholds(eventSummaryConfigModel, INVENTORY, STOCK_OUTS));
      eventSummaryConfigModel.addEvents(eventsList);
      assertNull(eventSummaryConfigModel.getEvents().get(0).getThresholds().get(0).getId());
      eventSummaryConfigModel.setUniqueIdentifier();
      String id = eventSummaryConfigModel.getEvents().get(0).getThresholds().get(0).getId();
      assertNotNull(id);
      eventSummaryConfigModel.setUniqueIdentifier();
      String idAgain = eventSummaryConfigModel.getEvents().get(0).getThresholds().get(0).getId();
      assertNotNull(idAgain);
      assertEquals(id, idAgain);
    } catch (Exception e) {
      LOGGER.warn("Ignoring exception" , e);
    }
  }

  @Test
  public  void testSetUniqueIdentifierWithDifferentConditions() {
    try {
      EventSummaryConfigModel defaultTemplate = getDefaultTemplate();
      assertNotNull(defaultTemplate);
      EventSummaryConfigModel eventSummaryConfigModel = new EventSummaryConfigModel();
      List<EventSummaryConfigModel.Events> eventsList = new ArrayList<>(0);
      eventsList.add(getEventWithThresholds(eventSummaryConfigModel, INVENTORY, STOCK_OUTS));
      eventSummaryConfigModel.addEvents(eventsList);
      assertNull(eventSummaryConfigModel.getEvents().get(0).getThresholds().get(0).getId());
      eventSummaryConfigModel.setUniqueIdentifier();
      String id = eventSummaryConfigModel.getEvents().get(0).getThresholds().get(0).getId();
      assertNotNull(id);
      eventSummaryConfigModel.getEvents().add(getEventWithThresholds(eventSummaryConfigModel, INVENTORY, STOCK_EXPIRY));
      eventSummaryConfigModel.setUniqueIdentifier();
      String idAgain = eventSummaryConfigModel.getEvents().get(1).getThresholds().get(0).getId();
      assertNotNull(idAgain);
      assertNotSame(id,idAgain);
    } catch (Exception e) {
      LOGGER.warn("Ignoring exception" , e);
    }
  }

  private EventSummaryConfigModel.Events getEvent(EventSummaryConfigModel eventSummaryConfigModel, String category, String type) {
    EventSummaryConfigModel.Events events = eventSummaryConfigModel.new Events();
    events.setCategory(category);
    events.setType(type);
    return events;
  }

  private EventSummaryConfigModel.Events getEventWithThresholds(EventSummaryConfigModel eventSummaryConfigModel, String category, String type) {
    EventSummaryConfigModel.Events events = eventSummaryConfigModel.new Events();
    events.setCategory(category);
    events.setType(type);
    List<EventSummaryConfigModel.Threshold> thresholds = new ArrayList<>(0);
    EventSummaryConfigModel.Threshold threshold = eventSummaryConfigModel.new Threshold();
    List<EventSummaryConfigModel.Condition> conditions = new ArrayList<>(0);
    EventSummaryConfigModel.Condition nameCondition = eventSummaryConfigModel.new Condition(DURATION, DAYS, GREATER_THAN_OR_EQUAL_TO);
    nameCondition.setValue(TEN);
    EventSummaryConfigModel.Condition includeMTagsCondition = eventSummaryConfigModel.new Condition(INCLUDE_MATERIAL_TAGS, null, null);
    includeMTagsCondition.setValues(Arrays.asList(ANTIBIOTIC));
    conditions.add(nameCondition);
    conditions.add(includeMTagsCondition);
    threshold.setConditions(conditions);
    thresholds.add(threshold);
    events.setThresholds(thresholds);
    return events;
  }

  private EventSummaryConfigModel getDefaultTemplate() {
    try {
      return EventSummaryTemplateLoader.getDefaultTemplate();
    } catch (Exception e) {
      LOGGER.warn("Ignoring exception" , e);
    }
    return null;
  }
}