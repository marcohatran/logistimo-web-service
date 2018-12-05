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

package com.logistimo.reports.plugins.internal;

import com.logistimo.reports.models.ReportMinMaxHistoryFilters;

import org.apache.commons.lang.StringUtils;
import org.json.JSONException;
import org.json.JSONObject;
import org.junit.Test;

import static org.testng.Assert.assertEquals;

/**
 * Created by vani on 30/05/18.
 */
public class QueryHelperTest {

  @Test
  public void testGetToDateBasedOnPeriodicityWithMonthlyPeriodicity() {
      String expectedValue = QueryHelper.getToDateBasedOnPeriodicity("2018-5-1",
          QueryHelper.MONTH);
      assertEquals(expectedValue, "2018-06-01");
  }

  @Test
  public void testGetToDateBasedOnPeriodicityWithMonthlyPeriodicityForFebruaryInLeapYear() {
    String expectedValue = QueryHelper.getToDateBasedOnPeriodicity("2020-2-1",
        QueryHelper.MONTH);
    assertEquals(expectedValue, "2020-03-01");
  }

  @Test
  public void testGetToDateBasedOnPeriodicityWithMonthlyPeriodicityForFebruaryInNonLeapYear() {
    String expectedValue = QueryHelper.getToDateBasedOnPeriodicity("2018-2-1",
        QueryHelper.MONTH);
    assertEquals(expectedValue, "2018-03-01");
  }

  @Test
  public void testGetToDateBasedOnPeriodicityWithWeeklyPeriodicity() {
    String expectedValue = QueryHelper.getToDateBasedOnPeriodicity("2018-5-28",QueryHelper.WEEK);
    assertEquals(expectedValue, "2018-06-04");
  }

  @Test
  public void testGetToDateBasedOnPeriodicityWithDailyPeriodicity() {
    String expectedValue = QueryHelper.getToDateBasedOnPeriodicity("2018-5-28",QueryHelper.DAY);
    assertEquals(expectedValue, "2018-05-29");
  }

  @Test (expected = IllegalArgumentException.class)
  public void testGetToDateBasedOnPeriodicityWithNullValues() {
    QueryHelper.getToDateBasedOnPeriodicity(null,null);
  }

  @Test (expected = IllegalArgumentException.class)
  public void testGetToDateBasedOnPeriodicityWithEmptyValues() {
    QueryHelper.getToDateBasedOnPeriodicity(StringUtils.EMPTY,StringUtils.EMPTY);
  }

  @Test (expected = JSONException.class)
  public void testParseMinMaxHistoryFiltersWithEmptyJsonObject() {
    JSONObject jsonObject = new JSONObject();
    QueryHelper.parseMinMaxHistoryFilters(jsonObject);
  }

  @Test (expected = IllegalArgumentException.class)
  public void testParseMinMaxHistoryFiltersWithNullJsonObject() {
    QueryHelper.parseMinMaxHistoryFilters(null);
  }

  @Test (expected = IllegalArgumentException.class)
  public void testParseMinMaxHistoryFiltersWithToDateInInvalidFormat() {
    JSONObject jsonObject = getJsonObjectWithDateInInvalidFormat();
    QueryHelper.parseMinMaxHistoryFilters(jsonObject);
  }

  @Test
  public void testParseMinMaxHistoryFiltersWithDailyPeriodicity() {
    JSONObject jsonObject = getJsonObjectWithDailyPeriodicity();
    ReportMinMaxHistoryFilters reportMinMaxHistoryFilters = QueryHelper.parseMinMaxHistoryFilters(jsonObject);
    assertEquals(Long.valueOf(1l), reportMinMaxHistoryFilters.getEntityId());
    assertEquals(Long.valueOf(2l), reportMinMaxHistoryFilters.getMaterialId());
    assertEquals(QueryHelper.DAY, reportMinMaxHistoryFilters.getPeriodicity());
    assertEquals("2018-06-01", reportMinMaxHistoryFilters.getFrom());
    assertEquals("2018-06-11", reportMinMaxHistoryFilters.getTo());
  }

  @Test
  public void testParseMinMaxHistoryFiltersWithWeeklyPeriodicity() {
    JSONObject jsonObject = getJsonObjectWithWeeklyPeriodicity();
    ReportMinMaxHistoryFilters reportMinMaxHistoryFilters = QueryHelper.parseMinMaxHistoryFilters(jsonObject);
    assertEquals(Long.valueOf(1l), reportMinMaxHistoryFilters.getEntityId());
    assertEquals(Long.valueOf(2l), reportMinMaxHistoryFilters.getMaterialId());
    assertEquals(QueryHelper.WEEK, reportMinMaxHistoryFilters.getPeriodicity());
    assertEquals("2018-05-01", reportMinMaxHistoryFilters.getFrom());
    assertEquals("2018-06-11", reportMinMaxHistoryFilters.getTo());
  }

  @Test
  public void testParseMinMaxHistoryFiltersWithMonthlyPeriodicity() {
    JSONObject jsonObject = getJsonObjectWithMonthlyPeriodicity();
    ReportMinMaxHistoryFilters reportMinMaxHistoryFilters = QueryHelper.parseMinMaxHistoryFilters(jsonObject);
    assertEquals(Long.valueOf(1l), reportMinMaxHistoryFilters.getEntityId());
    assertEquals(Long.valueOf(2l), reportMinMaxHistoryFilters.getMaterialId());
    assertEquals(QueryHelper.MONTH, reportMinMaxHistoryFilters.getPeriodicity());
    assertEquals("2018-05-01", reportMinMaxHistoryFilters.getFrom());
    assertEquals("2018-07-01", reportMinMaxHistoryFilters.getTo());
  }

  @Test
  public void testParseMinMaxHistoryFiltersWithDailyPeriodicityAndLevelPeriodicityDaily() {
    JSONObject jsonObject = getJsonObjectWithDailyPeriodicityAndLevelPeriodicityDaily();
    ReportMinMaxHistoryFilters reportMinMaxHistoryFilters = QueryHelper.parseMinMaxHistoryFilters(jsonObject);
    assertEquals("2018-06-01", reportMinMaxHistoryFilters.getFrom());
    assertEquals("2018-07-01", reportMinMaxHistoryFilters.getTo());
  }

  @Test
  public void testParseMinMaxHistoryFiltersWithDailyPeriodicityAndLevelPeriodicityMonthly() {
    JSONObject jsonObject = getJsonObjectWithDailyPeriodicityAndLevelPeriodicityMonthly();
    ReportMinMaxHistoryFilters reportMinMaxHistoryFilters = QueryHelper.parseMinMaxHistoryFilters(jsonObject);
    assertEquals("2018-06-01", reportMinMaxHistoryFilters.getFrom());
    assertEquals("2018-07-01", reportMinMaxHistoryFilters.getTo());
  }

  @Test
  public void testParseMinMaxHistoryFiltersWithDailyPeriodicityAndLevelPeriodicityWeekly() {
    JSONObject jsonObject = getJsonObjectWithDailyPeriodicityAndLevelPeriodicityWeekly();
    ReportMinMaxHistoryFilters reportMinMaxHistoryFilters = QueryHelper.parseMinMaxHistoryFilters(jsonObject);
    assertEquals("2018-06-04", reportMinMaxHistoryFilters.getFrom());
    assertEquals("2018-06-11", reportMinMaxHistoryFilters.getTo());
  }

  private JSONObject getJsonObjectWithDateInInvalidFormat() {
    JSONObject jsonObject = new JSONObject();
    jsonObject.put(QueryHelper.ENTITY, 1);
    jsonObject.put(QueryHelper.MATERIAL, 2);
    jsonObject.put(QueryHelper.PERIODICITY,QueryHelper.LEVEL_DAY);
    jsonObject.put(QueryHelper.FROM, "2018-1");
    jsonObject.put(QueryHelper.TO, "2018-6");
    return jsonObject;
  }

  private JSONObject getJsonObjectWithDailyPeriodicity() {
    JSONObject jsonObject = new JSONObject();
    jsonObject.put(QueryHelper.ENTITY, 1);
    jsonObject.put(QueryHelper.MATERIAL, 2);
    jsonObject.put(QueryHelper.PERIODICITY,QueryHelper.LEVEL_DAY);
    jsonObject.put(QueryHelper.FROM, "2018-6-1");
    jsonObject.put(QueryHelper.TO, "2018-6-10");
    return jsonObject;
  }

  private JSONObject getJsonObjectWithWeeklyPeriodicity() {
    JSONObject jsonObject = new JSONObject();
    jsonObject.put(QueryHelper.ENTITY, 1);
    jsonObject.put(QueryHelper.MATERIAL, 2);
    jsonObject.put(QueryHelper.PERIODICITY,QueryHelper.PERIODICITY_WEEK);
    jsonObject.put(QueryHelper.FROM, "2018-5-1");
    jsonObject.put(QueryHelper.TO, "2018-6-4");
    return jsonObject;
  }

  private JSONObject getJsonObjectWithMonthlyPeriodicity() {
    JSONObject jsonObject = new JSONObject();
    jsonObject.put(QueryHelper.ENTITY, 1);
    jsonObject.put(QueryHelper.MATERIAL, 2);
    jsonObject.put(QueryHelper.PERIODICITY,QueryHelper.PERIODICITY_MONTH);
    jsonObject.put(QueryHelper.FROM, "2018-5-1");
    jsonObject.put(QueryHelper.TO, "2018-6-1");
    return jsonObject;
  }

  private JSONObject getJsonObjectWithDailyPeriodicityAndLevelPeriodicityDaily() {
    JSONObject jsonObject = new JSONObject();
    jsonObject.put(QueryHelper.ENTITY, 1);
    jsonObject.put(QueryHelper.MATERIAL, 2);
    jsonObject.put(QueryHelper.PERIODICITY,QueryHelper.LEVEL_DAY);
    jsonObject.put(QueryHelper.FROM, "2018-6-1");
    jsonObject.put(QueryHelper.LEVEL, QueryHelper.LEVEL_DAY);
    jsonObject.put(QueryHelper.LEVEL_PERIODICITY,  QueryHelper.LEVEL_DAY);
    return jsonObject;
  }

  private JSONObject getJsonObjectWithDailyPeriodicityAndLevelPeriodicityMonthly() {
    JSONObject jsonObject = new JSONObject();
    jsonObject.put(QueryHelper.ENTITY, 1);
    jsonObject.put(QueryHelper.MATERIAL, 2);
    jsonObject.put(QueryHelper.PERIODICITY,QueryHelper.LEVEL_DAY);
    jsonObject.put(QueryHelper.FROM, "2018-6-1");
    jsonObject.put(QueryHelper.LEVEL, QueryHelper.LEVEL_DAY);
    jsonObject.put(QueryHelper.LEVEL_PERIODICITY,  QueryHelper.PERIODICITY_MONTH);
    return jsonObject;
  }
  private JSONObject getJsonObjectWithDailyPeriodicityAndLevelPeriodicityWeekly() {
    JSONObject jsonObject = new JSONObject();
    jsonObject.put(QueryHelper.ENTITY, 1);
    jsonObject.put(QueryHelper.MATERIAL, 2);
    jsonObject.put(QueryHelper.PERIODICITY,QueryHelper.LEVEL_DAY);
    jsonObject.put(QueryHelper.FROM, "2018-6-4");
    jsonObject.put(QueryHelper.LEVEL, QueryHelper.LEVEL_DAY);
    jsonObject.put(QueryHelper.LEVEL_PERIODICITY,  QueryHelper.PERIODICITY_WEEK);
    return jsonObject;
  }
}