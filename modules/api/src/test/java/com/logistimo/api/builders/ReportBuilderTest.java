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

package com.logistimo.api.builders;

import com.logistimo.auth.utils.SecurityUtils;
import com.logistimo.config.models.DomainConfig;
import com.logistimo.inventory.entity.IInventoryMinMaxLog;
import com.logistimo.inventory.entity.InventoryMinMaxLog;
import com.logistimo.reports.models.ReportDataModel;
import com.logistimo.reports.models.ReportMinMaxHistoryFilters;
import com.logistimo.reports.plugins.internal.QueryHelper;
import com.logistimo.reports.plugins.models.ReportChartModel;

import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

import java.math.BigDecimal;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.TimeZone;
import java.util.stream.Collectors;

import static org.junit.Assert.assertEquals;
import static org.powermock.api.mockito.PowerMockito.doReturn;
import static org.powermock.api.mockito.PowerMockito.mockStatic;
import static org.powermock.api.mockito.PowerMockito.spy;

/**
 * Created by vani on 29/06/18.
 */
@RunWith(PowerMockRunner.class)
@PrepareForTest({SecurityUtils.class, DomainConfig.class})
public class ReportBuilderTest {
  private static final String EMPTY = "";
  private static final String ZERO = "0";
  private static final String HUNDRED = "100";
  private static final String THOUSAND = "1000";
  private static final String FIFTY = "50";
  private static final String FIVE_HUNDRED = "500";
  private static final String TWENTY_FIVE = "25";
  private static final String TWO_HUNDRED_AND_FIFTY = "250";
  private static final String FIFTEEN = "15";
  private static final String ONE_HUNDRED_AND_FIFTY = "150";

  private static final String UTC = "UTC";
  private static final String EST = "EST";
  private static final String IST = "Asia/Kolkata";
  private static final String ACWST = "Australia/Eucla";

  private SimpleDateFormat simpleDateFormat = new SimpleDateFormat(QueryHelper.DATE_FORMAT_DAILY);
  private ReportBuilder reportBuilder = spy(new ReportBuilder());

  @Before
  public void setup() {
    mockStatic(SecurityUtils.class);
    mockStatic(DomainConfig.class);
    simpleDateFormat.setTimeZone(TimeZone.getTimeZone(UTC));
    doReturn(simpleDateFormat).when(reportBuilder).getLabelDateFormat(QueryHelper.DATE_FORMAT_DAILY);
  }

  /**
   * This test asserts that the label returned by the method under test is the same as the from date passed when created time is less than the from date
   * Domain timezone is UTC and System time zone is UTC
   * Monthly periodicity
   * @throws ParseException
   */
  @Test
  public void testGetMinMaxHistoryReportLabel_1() throws ParseException {
    PowerMockito.when(SecurityUtils.getCurrentDomainId()).thenReturn(1l);
    PowerMockito.when(DomainConfig.getInstance(1l))
        .thenReturn(getDomainConfigWithUTCTimezone());
    String actualResult = reportBuilder.getMinMaxHistoryReportLabel(
        new DateTime(2018,4,30,1,0,0,DateTimeZone.forID(UTC)).toDate(), QueryHelper.MONTH,
        "2018-05-01");
    assertEquals("2018-05-01", actualResult);
  }

  /**
   * This test asserts that the label returned by the method under test is the same as the from date passed when created time is less than the from date.
   * Domain timezone is EST and System time zone is UTC
   * Monthly periodicity
   * @throws ParseException
   */
  @Test
  public void testGetMinMaxHistoryReportLabel_2() throws ParseException {
    PowerMockito.when(SecurityUtils.getCurrentDomainId()).thenReturn(2l);
    PowerMockito.when(DomainConfig.getInstance(2l))
        .thenReturn(getDomainConfigWithESTTimezone());
    String actualResult = reportBuilder.getMinMaxHistoryReportLabel(new DateTime(2018,4,30,1,0,0,DateTimeZone.forID(UTC)).toDate(), QueryHelper.MONTH, "2018-05-01");
    assertEquals("2018-05-01", actualResult);
  }

  /**
   * This test asserts that the label returned by the method under test is the same as the from date passed when created time is less than the from date.
   * Domain timezone is IST and System time zone is UTC
   * Monthly periodicity
   * @throws ParseException
   */
  @Test
  public void testGetMinMaxHistoryReportLabel_3() throws ParseException {
    PowerMockito.when(SecurityUtils.getCurrentDomainId()).thenReturn(3l);
    PowerMockito.when(DomainConfig.getInstance(3l))
        .thenReturn(getDomainConfigWithISTTimezone());
    String actualResult = reportBuilder.getMinMaxHistoryReportLabel(new DateTime(2018,4,30,23,0,0, DateTimeZone.forID(UTC)).toDate(), QueryHelper.MONTH, "2018-05-01");
    assertEquals("2018-05-01", actualResult);
  }

  /**
   * This test asserts that the label returned by the method under test is the same as the from date passed when created time is less than the from date.
   * Domain timezone is ACWST and System time zone is UTC
   * Monthly periodicity
   * @throws ParseException
   */
  @Test
  public void testGetMinMaxHistoryReportLabel_4() throws ParseException {
    PowerMockito.when(SecurityUtils.getCurrentDomainId()).thenReturn(4l);
    PowerMockito.when(DomainConfig.getInstance(4l))
        .thenReturn(getDomainConfigWithACWSTTimezone());
    String actualResult = reportBuilder.getMinMaxHistoryReportLabel(new DateTime(2018,4,30,23,0,0, DateTimeZone.forID(UTC)).toDate(), QueryHelper.MONTH, "2018-05-01");
    assertEquals("2018-05-01", actualResult);
  }

  /**
   * This test asserts that the label returned by the method under test is the same as the from date passed when created time is less than the from date.
   * Domain timezone is UTC and System time zone is UTC
   * Weekly periodicity
   * @throws ParseException
   */
  @Test
  public void testGetMinMaxHistoryReportLabel_5() throws ParseException {
    PowerMockito.when(SecurityUtils.getCurrentDomainId()).thenReturn(1l);
    PowerMockito.when(DomainConfig.getInstance(1l))
        .thenReturn(getDomainConfigWithUTCTimezone());
    String actualResult = reportBuilder.getMinMaxHistoryReportLabel(new DateTime(2018,4,30,1,0,0,DateTimeZone.forID(UTC)).toDate(), QueryHelper.WEEK,
        "2018-05-07");
    assertEquals("2018-05-07", actualResult);
  }

  /**
   * This test asserts that the label returned by the method under test is the same as the from date passed when created time is less than the from date.
   * Domain timezone is EST and System time zone is UTC
   * Weekly periodicity
   * @throws ParseException
   */
  @Test
  public void testGetMinMaxHistoryReportLabel_6() throws ParseException {
    PowerMockito.when(SecurityUtils.getCurrentDomainId()).thenReturn(2l);
    PowerMockito.when(DomainConfig.getInstance(2l))
        .thenReturn(getDomainConfigWithESTTimezone());
    String actualResult = reportBuilder.getMinMaxHistoryReportLabel(new DateTime(2018,4,30,1,0,0,DateTimeZone.forID(UTC)).toDate(), QueryHelper.WEEK, "2018-05-07");
    assertEquals("2018-05-07", actualResult);
  }

  /**
   * This test asserts that the label returned by the method under test is the same as the from date passed when created time is less than the from date.
   * Domain timezone is IST and System time zone is UTC
   * Weekly periodicity
   * @throws ParseException
   */
  @Test
  public void testGetMinMaxHistoryReportLabel_7() throws ParseException {
    PowerMockito.when(SecurityUtils.getCurrentDomainId()).thenReturn(3l);
    PowerMockito.when(DomainConfig.getInstance(3l))
        .thenReturn(getDomainConfigWithISTTimezone());
    String actualResult = reportBuilder.getMinMaxHistoryReportLabel(new DateTime(2018,4,30,23,0,0,DateTimeZone.forID(UTC)).toDate(), QueryHelper.WEEK, "2018-05-07");
    assertEquals("2018-05-07", actualResult);
  }

  /**
   * This test asserts that the label returned by the method under test is the same as the from date passed when created time is less than the from date.
   * Domain timezone is Australian Central Western Standard Timezone and System time zone is UTC
   * Weekly periodicity
   * @throws ParseException
   */
  @Test
  public void testGetMinMaxHistoryReportLabel_8() throws ParseException {
    PowerMockito.when(SecurityUtils.getCurrentDomainId()).thenReturn(4l);
    PowerMockito.when(DomainConfig.getInstance(4l))
        .thenReturn(getDomainConfigWithACWSTTimezone());
    String actualResult = reportBuilder.getMinMaxHistoryReportLabel(new DateTime(2018,4,30,23,0,0,DateTimeZone.forID(UTC)).toDate(), QueryHelper.WEEK, "2018-05-07");
    assertEquals("2018-05-07", actualResult);
  }

  /**
   * This test asserts that the label returned by the method under test is the same as the from date passed when created time is less than the from date.
   * Domain timezone is UTC and System time zone is UTC
   * Daily periodicity
   * @throws ParseException
   */
  @Test
  public void testGetMinMaxHistoryReportLabel_9() throws ParseException {
    PowerMockito.when(SecurityUtils.getCurrentDomainId()).thenReturn(1l);
    PowerMockito.when(DomainConfig.getInstance(1l))
        .thenReturn(getDomainConfigWithUTCTimezone());
    String actualResult = reportBuilder.getMinMaxHistoryReportLabel(new DateTime(2018,5,6,1,0,0,DateTimeZone.forID(UTC)).toDate(), QueryHelper.DAY, "2018-05-24");
    assertEquals("2018-05-24", actualResult);
  }

  /**
   * This test asserts that the label returned by the method under test is the same as the from date passed when created time is less than the from date.
   * Domain timezone is EST and System time zone is UTC
   * Daily periodicity
   * @throws ParseException
   */
  @Test
  public void testGetMinMaxHistoryReportLabel_10() throws ParseException {
    PowerMockito.when(SecurityUtils.getCurrentDomainId()).thenReturn(2l);
    PowerMockito.when(DomainConfig.getInstance(2l))
        .thenReturn(getDomainConfigWithESTTimezone());
    String actualResult = reportBuilder.getMinMaxHistoryReportLabel(new DateTime(2018,5,6,1,0,0,DateTimeZone.forID(UTC)).toDate(), QueryHelper.DAY, "2018-05-24");
    assertEquals("2018-05-24", actualResult);
  }

  /**
   * This test asserts that the label returned by the method under test is the same as the from date passed when created time is less than the from date.
   * Domain timezone is UTC and System time zone is UTC
   * Daily periodicity
   * @throws ParseException
   */
  @Test
  public void testGetMinMaxHistoryReportLabel_11() throws ParseException {
    PowerMockito.when(SecurityUtils.getCurrentDomainId()).thenReturn(3l);
    PowerMockito.when(DomainConfig.getInstance(3l))
        .thenReturn(getDomainConfigWithISTTimezone());
    String actualResult = reportBuilder.getMinMaxHistoryReportLabel(new DateTime(2018,5,6,23,0,0,DateTimeZone.forID(UTC)).toDate(), QueryHelper.DAY, "2018-05-24");
    assertEquals("2018-05-24", actualResult);
  }

  /**
   * This test asserts that the label returned by the method under test is the same as the from date passed.
   * Domain timezone is UTC and System time zone is ACWST
   * Daily periodicity
   * @throws ParseException
   */
  @Test
  public void testGetMinMaxHistoryReportLabel_12() throws ParseException {
    PowerMockito.when(SecurityUtils.getCurrentDomainId()).thenReturn(3l);
    PowerMockito.when(DomainConfig.getInstance(3l))
        .thenReturn(getDomainConfigWithISTTimezone());
    String actualResult = reportBuilder.getMinMaxHistoryReportLabel(new DateTime(2018,5,6,23,0,0,DateTimeZone.forID(UTC)).toDate(), QueryHelper.DAY, "2018-05-24");
    assertEquals("2018-05-24", actualResult);
  }


  @Test
  public void testGetMinMaxHistoryReportValuesWithNullMinMax() {
    List<ReportDataModel> actualResult = reportBuilder.getMinMaxHistoryReportValues(null,null);
    assertEquals(getReportDataModels(getListWithMinZeroMaxZero()),actualResult);
  }

  @Test
  public void testGetMinMaxHistoryReportValuesWithValidMinMax() {
    List<ReportDataModel> actualResult = reportBuilder.getMinMaxHistoryReportValues(
        new BigDecimal(HUNDRED), new BigDecimal(THOUSAND));
    assertEquals(getReportDataModels(getListWithMinHundredMaxThousand()),actualResult);
  }

  /**
   * This test asserts that the label returned by the method under test is 2018-05-01 when the date passed to it is May 1st 2018 1h
   * Domain timezone is UTC and System time zone is UTC
   * Monthly periodicity
   * @throws ParseException
   */
  @Test
  public void testGetMinMaxHistoryReportLabel_13() throws ParseException {
    PowerMockito.when(SecurityUtils.getCurrentDomainId()).thenReturn(1l);
    PowerMockito.when(DomainConfig.getInstance(1l))
        .thenReturn(getDomainConfigWithUTCTimezone());
    String actualResult =
        reportBuilder
            .getMinMaxHistoryReportLabel(new DateTime(2018, 5, 1, 1, 0, 0, DateTimeZone.forID(UTC)).toDate(), QueryHelper.MONTH, "2018-01-01");
    assertEquals("2018-05-01", actualResult);
  }

  /**
   * This test asserts that the label returned by the method under test is 2018-04-01 when the date passed to it is May 1st 2018 1h
   * Domain timezone is EST and System time zone is UTC
   * Monthly periodicity
   * @throws ParseException
   */
  @Test
  public void testGetMinMaxHistoryReportLabel_14() throws ParseException {
    PowerMockito.when(SecurityUtils.getCurrentDomainId()).thenReturn(2l);
    PowerMockito.when(DomainConfig.getInstance(2l))
        .thenReturn(getDomainConfigWithESTTimezone());
    String actualResult =
        reportBuilder
            .getMinMaxHistoryReportLabel(new DateTime(2018, 5, 1, 1, 0, 0, DateTimeZone.forID(UTC)).toDate(), QueryHelper.MONTH, "2018-01-01");
    assertEquals("2018-04-01", actualResult);
  }

  /**
   * This test asserts that the label returned by the method under test is 2018-06-01 when the date passed to it is May 31st 2018 23h
   * Domain timezone is IST and System time zone is UTC
   * Monthly periodicity
   * @throws ParseException
   */
  @Test
  public void testGetMinMaxHistoryReportLabel_15() throws ParseException {
    PowerMockito.when(SecurityUtils.getCurrentDomainId()).thenReturn(3l);
    PowerMockito.when(DomainConfig.getInstance(3l))
        .thenReturn(getDomainConfigWithISTTimezone());
    String actualResult = reportBuilder.getMinMaxHistoryReportLabel(new DateTime(2018,5,31,23,0,0,DateTimeZone.forID(UTC)).toDate(), QueryHelper.MONTH, "2018-01-01");
    assertEquals("2018-06-01", actualResult);
  }

  /**
   * This test asserts that the label returned by the method under test is 2018-04-30 when the date passed to it is May 6th 2018 1h
   * Domain timezone is UTC and System time zone is UTC
   * Weekly periodicity
   * @throws ParseException
   */
  @Test
  public void testGetMinMaxHistoryReportLabel_16() throws ParseException {
    PowerMockito.when(SecurityUtils.getCurrentDomainId()).thenReturn(1l);
    PowerMockito.when(DomainConfig.getInstance(1l))
        .thenReturn(getDomainConfigWithUTCTimezone());
    String actualResult = reportBuilder.getMinMaxHistoryReportLabel(new DateTime(2018,5,6,1,0,0,DateTimeZone.forID(UTC)).toDate(), QueryHelper.WEEK, "2018-01-01");
    assertEquals("2018-04-30", actualResult);
  }

  /**
   * This test asserts that the label returned by the method under test is 2018-04-30 when the date passed to it is May 7th 2018 1h
   * Domain timezone is EST and System time zone is UTC
   * Weekly periodicity
   * @throws ParseException
   */
  @Test
  public void testGetMinMaxHistoryReportLabel_17() throws ParseException {
    PowerMockito.when(SecurityUtils.getCurrentDomainId()).thenReturn(2l);
    PowerMockito.when(DomainConfig.getInstance(2l))
        .thenReturn(getDomainConfigWithESTTimezone());
    String actualResult = reportBuilder.getMinMaxHistoryReportLabel(new DateTime(2018,5,7,1,0,0,DateTimeZone.forID(UTC)).toDate(), QueryHelper.WEEK, "2018-01-01");
    assertEquals("2018-04-30", actualResult);
  }

  /**
   * This test asserts that the label returned by the method under test is 2018-05-07 when the date passed to it is May 6th 2018 23h
   * Domain timezone is IST and System time zone is UTC
   * Weekly periodicity
   * @throws ParseException
   */
  @Test
  public void testGetMinMaxHistoryReportLabel_18() throws ParseException {
    PowerMockito.when(SecurityUtils.getCurrentDomainId()).thenReturn(3l);
    PowerMockito.when(DomainConfig.getInstance(3l))
        .thenReturn(getDomainConfigWithISTTimezone());
    String actualResult = reportBuilder.getMinMaxHistoryReportLabel(new DateTime(2018,5,6,23,0,0,DateTimeZone.forID(UTC)).toDate(), QueryHelper.WEEK, "2018-01-01");
    assertEquals("2018-05-07", actualResult);
  }

  /**
   * This test asserts that the label returned by the method under test is 2018-05-06 when the date passed to it is May 6th 2018 1h
   * Domain timezone is UTC and System time zone is UTC
   * Weekly periodicity
   * @throws ParseException
   */
  @Test
  public void testGetMinMaxHistoryReportLabel_19() throws ParseException {
    PowerMockito.when(SecurityUtils.getCurrentDomainId()).thenReturn(1l);
    PowerMockito.when(DomainConfig.getInstance(1l))
        .thenReturn(getDomainConfigWithUTCTimezone());
    String actualResult = reportBuilder.getMinMaxHistoryReportLabel(new DateTime(2018,5,6,1,0,0,DateTimeZone.forID(UTC)).toDate(), QueryHelper.DAY, "2018-05-01");
    assertEquals("2018-05-06", actualResult);
  }

  /**
   * This test asserts that the label returned by the method under test is 2018-05-05 when the date passed to it is May 6th 2018 1h
   * Domain timezone is EST and System time zone is UTC
   * Weekly periodicity
   * @throws ParseException
   */
  @Test
  public void testGetMinMaxHistoryReportLabel_20() throws ParseException {
    PowerMockito.when(SecurityUtils.getCurrentDomainId()).thenReturn(2l);
    PowerMockito.when(DomainConfig.getInstance(2l))
        .thenReturn(getDomainConfigWithESTTimezone());
    String actualResult = reportBuilder.getMinMaxHistoryReportLabel(new DateTime(2018,5,6,1,0,0,DateTimeZone.forID(UTC)).toDate(), QueryHelper.DAY, "2018-05-01");
    assertEquals("2018-05-05", actualResult);
  }

  /**
   * This test asserts that the label returned by the method under test is 2018-05-07 when the date passed to it is May 6th 2018 23h
   * Domain timezone is IST and System time zone is UTC
   * Weekly periodicity
   * @throws ParseException
   */
  @Test
  public void testGetMinMaxHistoryReportLabel_21() throws ParseException {
    PowerMockito.when(SecurityUtils.getCurrentDomainId()).thenReturn(3l);
    PowerMockito.when(DomainConfig.getInstance(3l))
        .thenReturn(getDomainConfigWithISTTimezone());
    String actualResult = reportBuilder.getMinMaxHistoryReportLabel(new DateTime(2018,5,6,23,0,0,DateTimeZone.forID(UTC)).toDate(), QueryHelper.DAY, "2018-05-01");
    assertEquals("2018-05-07", actualResult);
  }


  @Test
  public void testFillMinMaxHistoryChartDataWithNullInputs() throws ParseException{
    reportBuilder.fillMinMaxHistoryChartData(null,null);
  }

  @Test
  public void testBuildMinMaxHistoryReportsDataForMonthlyPeriodicityWithDomainTimeUTC() throws
      ParseException {
    PowerMockito.when(SecurityUtils.getCurrentDomainId()).thenReturn(1l);
    PowerMockito.when(DomainConfig.getInstance(1l))
        .thenReturn(getDomainConfigWithUTCTimezone());
    List<IInventoryMinMaxLog> inventoryMinMaxLogs = getInventoryMinMaxLogsForMonthlyPeriodicity();
    ReportMinMaxHistoryFilters
        reportMinMaxHistoryFilters = getReportMinMaxHistoryFiltersWithMonthlyPeriodicity();
    List<ReportChartModel> actualResult = reportBuilder.buildMinMaxHistoryReportsData(inventoryMinMaxLogs, reportMinMaxHistoryFilters);
    assertEquals(6, actualResult.size());
    assertEquals(getReportChartModel("2018-05-01", getListWithMinHundredMaxThousand()), actualResult.get(0));
    assertEquals(getReportChartModel("2018-04-01", getListWithMinTwentyFiveMaxTwoHundredAndFifty()), actualResult.get(1));
    assertEquals(getReportChartModel("2018-01-01", getListWithMinZeroMaxZero()),
        actualResult.get(2));
    assertEquals(getReportChartModel("2017-12-01", getListWithMinEmptyMaxEmpty()),
        actualResult.get(3));
    assertEquals(getReportChartModel("2018-02-01", getListWithMinZeroMaxZero()), actualResult.get(4));
    assertEquals(getReportChartModel("2018-03-01", getListWithMinZeroMaxZero()), actualResult.get(5));
  }

  @Test
  public void testBuildMinMaxHistoryReportsDataForMonthlyPeriodicityWithDomainTimeEST() throws
      ParseException {
    PowerMockito.when(SecurityUtils.getCurrentDomainId()).thenReturn(2l);
    PowerMockito.when(DomainConfig.getInstance(2l))
        .thenReturn(getDomainConfigWithESTTimezone());
    List<IInventoryMinMaxLog> inventoryMinMaxLogs = getInventoryMinMaxLogsForMonthlyPeriodicity();
    ReportMinMaxHistoryFilters
        reportMinMaxHistoryFilters = getReportMinMaxHistoryFiltersWithMonthlyPeriodicity();
    List<ReportChartModel> actualResult = reportBuilder.buildMinMaxHistoryReportsData(inventoryMinMaxLogs, reportMinMaxHistoryFilters);
    assertEquals(6, actualResult.size());
    assertEquals(getReportChartModel("2018-05-01", getListWithMinHundredMaxThousand()), actualResult.get(0));
    assertEquals(getReportChartModel("2018-04-01", getListWithMinTwentyFiveMaxTwoHundredAndFifty()), actualResult.get(1));
    assertEquals(getReportChartModel("2017-12-01", getListWithMinZeroMaxZero()), actualResult.get(2));
    assertEquals(getReportChartModel("2018-01-01", getListWithMinZeroMaxZero()), actualResult.get(3));
    assertEquals(getReportChartModel("2018-02-01", getListWithMinZeroMaxZero()), actualResult.get(4));
    assertEquals(getReportChartModel("2018-03-01", getListWithMinZeroMaxZero()), actualResult.get(5));
  }

  @Test
  public void testBuildMinMaxHistoryReportsDataForMonthlyPeriodicityWithDomainTimeIST() throws
      ParseException {
    PowerMockito.when(SecurityUtils.getCurrentDomainId()).thenReturn(3l);
    PowerMockito.when(DomainConfig.getInstance(3l))
        .thenReturn(getDomainConfigWithISTTimezone());
    List<IInventoryMinMaxLog> inventoryMinMaxLogs = getInventoryMinMaxLogsForMonthlyPeriodicity();
    ReportMinMaxHistoryFilters
        reportMinMaxHistoryFilters = getReportMinMaxHistoryFiltersWithMonthlyPeriodicity();
    List<ReportChartModel> actualResult = reportBuilder.buildMinMaxHistoryReportsData(inventoryMinMaxLogs, reportMinMaxHistoryFilters);
    assertEquals(7, actualResult.size());
    assertEquals(getReportChartModel("2018-06-01", getListWithMinHundredMaxThousand()), actualResult.get(0));
    assertEquals(getReportChartModel("2018-05-01", getListWithMinFiftyMaxFiveHundred()), actualResult.get(1));
    assertEquals(getReportChartModel("2018-04-01", getListWithMinFifteenMaxOneHundredAndFifty()), actualResult.get(2));
    assertEquals(getReportChartModel("2018-01-01", getListWithMinZeroMaxZero()), actualResult.get(3));
    assertEquals(getReportChartModel("2017-12-01", getListWithMinEmptyMaxEmpty()), actualResult.get(4));
    assertEquals(getReportChartModel("2018-02-01", getListWithMinZeroMaxZero()), actualResult.get(5));
    assertEquals(getReportChartModel("2018-03-01", getListWithMinZeroMaxZero()), actualResult.get(6));
  }

  @Test
  public void testBuildMinMaxHistoryReportsDataForMonthlyPeriodicityWithDomainTimeACWST() throws
      ParseException {
    PowerMockito.when(SecurityUtils.getCurrentDomainId()).thenReturn(4l);
    PowerMockito.when(DomainConfig.getInstance(4l))
        .thenReturn(getDomainConfigWithACWSTTimezone());
    List<IInventoryMinMaxLog> inventoryMinMaxLogs = getInventoryMinMaxLogsForMonthlyPeriodicity();
    ReportMinMaxHistoryFilters
        reportMinMaxHistoryFilters = getReportMinMaxHistoryFiltersWithMonthlyPeriodicity();
    List<ReportChartModel> actualResult = reportBuilder.buildMinMaxHistoryReportsData(inventoryMinMaxLogs, reportMinMaxHistoryFilters);
    assertEquals(7, actualResult.size());
    assertEquals(getReportChartModel("2018-06-01", getListWithMinHundredMaxThousand()), actualResult.get(0));
    assertEquals(getReportChartModel("2018-05-01", getListWithMinFiftyMaxFiveHundred()), actualResult.get(1));
    assertEquals(getReportChartModel("2018-04-01", getListWithMinFifteenMaxOneHundredAndFifty()), actualResult.get(2));
    assertEquals(getReportChartModel("2018-01-01", getListWithMinZeroMaxZero()), actualResult.get(3));
    assertEquals(getReportChartModel("2017-12-01", getListWithMinEmptyMaxEmpty()), actualResult.get(4));
    assertEquals(getReportChartModel("2018-02-01", getListWithMinZeroMaxZero()), actualResult.get(5));
    assertEquals(getReportChartModel("2018-03-01", getListWithMinZeroMaxZero()), actualResult.get(6));
  }

  @Test
  public void testBuildMinMaxHistoryReportsDataForWeeklyPeriodicityWithDomainTimeUTC() throws
      ParseException {
    PowerMockito.when(SecurityUtils.getCurrentDomainId()).thenReturn(1l);
    PowerMockito.when(DomainConfig.getInstance(1l))
        .thenReturn(getDomainConfigWithUTCTimezone());
    List<IInventoryMinMaxLog> inventoryMinMaxLogs = getInventoryMinMaxLogsForWeeklyPeriodicity();
    ReportMinMaxHistoryFilters
        reportMinMaxHistoryFilters = getReportMinMaxHistoryFiltersWithWeeklyPeriodicity();
    List<ReportChartModel> actualResult = reportBuilder.buildMinMaxHistoryReportsData(inventoryMinMaxLogs, reportMinMaxHistoryFilters);
    assertEquals(6, actualResult.size());
    assertEquals(getReportChartModel("2018-06-04", getListWithMinHundredMaxThousand()),
        actualResult.get(0));
    assertEquals(getReportChartModel("2018-05-28", getListWithMinFiftyMaxFiveHundred()), actualResult.get(1));
    assertEquals(getReportChartModel("2018-05-14", getListWithMinTwentyFiveMaxTwoHundredAndFifty()), actualResult.get(2));
    assertEquals(getReportChartModel("2018-04-30", getListWithMinFifteenMaxOneHundredAndFifty()), actualResult.get(3));
    assertEquals(getReportChartModel("2018-05-07", getListWithMinFifteenMaxOneHundredAndFifty()), actualResult.get(4));
    assertEquals(getReportChartModel("2018-05-21", getListWithMinTwentyFiveMaxTwoHundredAndFifty()), actualResult.get(5));
  }

  @Test
  public void testBuildMinMaxHistoryReportsDataForWeeklyPeriodicityWithDomainTimeIST() throws
      ParseException {
    PowerMockito.when(SecurityUtils.getCurrentDomainId()).thenReturn(3l);
    PowerMockito.when(DomainConfig.getInstance(3l))
        .thenReturn(getDomainConfigWithISTTimezone());
    List<IInventoryMinMaxLog> inventoryMinMaxLogs = getInventoryMinMaxLogsForWeeklyPeriodicity();
    ReportMinMaxHistoryFilters
        reportMinMaxHistoryFilters = getReportMinMaxHistoryFiltersWithWeeklyPeriodicity();
    List<ReportChartModel> actualResult = reportBuilder.buildMinMaxHistoryReportsData(inventoryMinMaxLogs, reportMinMaxHistoryFilters);
    assertEquals(7, actualResult.size());
    assertEquals(getReportChartModel("2018-06-11", getListWithMinHundredMaxThousand()), actualResult.get(0));
    assertEquals(getReportChartModel("2018-05-28", getListWithMinFiftyMaxFiveHundred()), actualResult.get(1));
    assertEquals(getReportChartModel("2018-05-14", getListWithMinTwentyFiveMaxTwoHundredAndFifty()), actualResult.get(2));
    assertEquals(getReportChartModel("2018-04-30", getListWithMinFifteenMaxOneHundredAndFifty()), actualResult.get(3));
    assertEquals(getReportChartModel("2018-05-07", getListWithMinFifteenMaxOneHundredAndFifty()), actualResult.get(4));
    assertEquals(getReportChartModel("2018-05-21", getListWithMinTwentyFiveMaxTwoHundredAndFifty()), actualResult.get(5));
    assertEquals(getReportChartModel("2018-06-04", getListWithMinFiftyMaxFiveHundred()), actualResult.get(6));
  }

  @Test
  public void testBuildMinMaxHistoryReportsDataForWeeklyPeriodicityWithDomainTimeEST() throws
      ParseException {
    PowerMockito.when(SecurityUtils.getCurrentDomainId()).thenReturn(2l);
    PowerMockito.when(DomainConfig.getInstance(2l))
        .thenReturn(getDomainConfigWithESTTimezone());
    List<IInventoryMinMaxLog> inventoryMinMaxLogs = getInventoryMinMaxLogsForWeeklyPeriodicity();
    ReportMinMaxHistoryFilters
        reportMinMaxHistoryFilters = getReportMinMaxHistoryFiltersWithWeeklyPeriodicity();
    List<ReportChartModel> actualResult = reportBuilder.buildMinMaxHistoryReportsData(inventoryMinMaxLogs, reportMinMaxHistoryFilters);
    assertEquals(6, actualResult.size());
    assertEquals(getReportChartModel("2018-06-04", getListWithMinHundredMaxThousand()), actualResult.get(0));
    assertEquals(getReportChartModel("2018-05-28", getListWithMinFiftyMaxFiveHundred()), actualResult.get(1));
    assertEquals(getReportChartModel("2018-05-14", getListWithMinTwentyFiveMaxTwoHundredAndFifty()), actualResult.get(2));
    assertEquals(getReportChartModel("2018-04-30", getListWithMinFifteenMaxOneHundredAndFifty()), actualResult.get(3));
    assertEquals(getReportChartModel("2018-05-07", getListWithMinFifteenMaxOneHundredAndFifty()), actualResult.get(4));
    assertEquals(getReportChartModel("2018-05-21", getListWithMinTwentyFiveMaxTwoHundredAndFifty()), actualResult.get(5));
  }

  @Test
  public void testBuildMinMaxHistoryReportsDataForDailyPeriodicityWithDomainTimeUTC() throws
      ParseException {
    PowerMockito.when(SecurityUtils.getCurrentDomainId()).thenReturn(1l);
    PowerMockito.when(DomainConfig.getInstance(1l))
        .thenReturn(getDomainConfigWithUTCTimezone());
    List<IInventoryMinMaxLog> inventoryMinMaxLogs = getInventoryMinMaxLogsForDailyPeriodicity();
    ReportMinMaxHistoryFilters
        reportMinMaxHistoryFilters = getReportMinMaxHistoryFiltersWithDailyPeriodicity();
    List<ReportChartModel> actualResult = reportBuilder.buildMinMaxHistoryReportsData(inventoryMinMaxLogs, reportMinMaxHistoryFilters);
    assertEquals(5, actualResult.size());
    assertEquals(getReportChartModel("2018-05-31", getListWithMinHundredMaxThousand()), actualResult.get(0));
    assertEquals(getReportChartModel("2018-05-29", getListWithMinFifteenMaxOneHundredAndFifty()), actualResult.get(1));
    assertEquals(getReportChartModel("2018-05-28", getListWithMinZeroMaxZero()), actualResult.get(2));
    assertEquals(getReportChartModel("2018-05-30", getListWithMinFifteenMaxOneHundredAndFifty()), actualResult.get(3));
    assertEquals(getReportChartModel("2018-06-01", getListWithMinHundredMaxThousand()), actualResult.get(4));
  }

  @Test
  public void testBuildMinMaxHistoryReportsDataForDailyPeriodicityWithDomainTimeEST() throws
      ParseException {
    PowerMockito.when(SecurityUtils.getCurrentDomainId()).thenReturn(2l);
    PowerMockito.when(DomainConfig.getInstance(2l))
        .thenReturn(getDomainConfigWithESTTimezone());
    List<IInventoryMinMaxLog> inventoryMinMaxLogs = getInventoryMinMaxLogsForDailyPeriodicity();
    ReportMinMaxHistoryFilters
        reportMinMaxHistoryFilters = getReportMinMaxHistoryFiltersWithDailyPeriodicity();
    List<ReportChartModel> actualResult = reportBuilder.buildMinMaxHistoryReportsData(inventoryMinMaxLogs, reportMinMaxHistoryFilters);
    assertEquals(5, actualResult.size());
    assertEquals(getReportChartModel("2018-05-31", getListWithMinHundredMaxThousand()), actualResult.get(0));
    assertEquals(getReportChartModel("2018-05-30", getListWithMinTwentyFiveMaxTwoHundredAndFifty()), actualResult.get(1));
    assertEquals(getReportChartModel("2018-05-28", getListWithMinFifteenMaxOneHundredAndFifty()), actualResult.get(2));
    assertEquals(getReportChartModel("2018-05-29", getListWithMinFifteenMaxOneHundredAndFifty()), actualResult.get(3));
    assertEquals(getReportChartModel("2018-06-01", getListWithMinHundredMaxThousand()), actualResult.get(4));
  }

  @Test
  public void testBuildMinMaxHistoryReportsDataForDailyPeriodicityWithDomainTimeIST() throws
      ParseException {
    PowerMockito.when(SecurityUtils.getCurrentDomainId()).thenReturn(3l);
    PowerMockito.when(DomainConfig.getInstance(3l))
        .thenReturn(getDomainConfigWithISTTimezone());
    List<IInventoryMinMaxLog> inventoryMinMaxLogs = getInventoryMinMaxLogsForDailyPeriodicity();
    ReportMinMaxHistoryFilters
        reportMinMaxHistoryFilters = getReportMinMaxHistoryFiltersWithDailyPeriodicity();
    List<ReportChartModel> actualResult = reportBuilder.buildMinMaxHistoryReportsData(inventoryMinMaxLogs, reportMinMaxHistoryFilters);
    assertEquals(5, actualResult.size());
    assertEquals(getReportChartModel("2018-06-01", getListWithMinHundredMaxThousand()), actualResult.get(0));
    assertEquals(getReportChartModel("2018-05-31", getListWithMinFiftyMaxFiveHundred()), actualResult.get(1));
    assertEquals(getReportChartModel("2018-05-29", getListWithMinFifteenMaxOneHundredAndFifty()), actualResult.get(2));
    assertEquals(getReportChartModel("2018-05-28", getListWithMinEmptyMaxEmpty()), actualResult.get(3));
    assertEquals(getReportChartModel("2018-05-30", getListWithMinFifteenMaxOneHundredAndFifty()), actualResult.get(4));
  }

  @Test (expected = NullPointerException.class)
  public void  testGetLabelDateFormatWithNullInput() {
    reportBuilder.getLabelDateFormat(null);
  }

  @Test (expected = IllegalArgumentException.class)
  public void testGetLabelDateFormatWithInvalidInput() {
    reportBuilder.getLabelDateFormat("INVALID");
  }

  private DomainConfig getDomainConfigWithUTCTimezone() {
    DomainConfig domainConfig = new DomainConfig();
    domainConfig.setTimezone(UTC);
    return domainConfig;
  }

  private DomainConfig getDomainConfigWithESTTimezone() {
    DomainConfig domainConfig = new DomainConfig();
    domainConfig.setTimezone(EST);
    return domainConfig;
  }

  private DomainConfig getDomainConfigWithISTTimezone() {
    DomainConfig domainConfig = new DomainConfig();
    domainConfig.setTimezone(IST);
    return domainConfig;
  }

  private DomainConfig getDomainConfigWithACWSTTimezone() {
    DomainConfig domainConfig = new DomainConfig();
    domainConfig.setTimezone(ACWST);
    return domainConfig;
  }

  /**
   * Returns a list of IInventoryMinMaxLog objects with dates in UTC time zone. The dates and min max values are as follows:
   * May 31st 2018 23h 100,1000
   * May 25th 2018 0h 50,500
   * April 30th 2018 23h 25,250
   * April 30th 2018 1h 15,150
   * Jan 1st 2018 1h 0,0
   * @return
   */
  private List<IInventoryMinMaxLog> getInventoryMinMaxLogsForMonthlyPeriodicity() {
    List<IInventoryMinMaxLog> minMaxLogs = new ArrayList<>(5);
    InventoryMinMaxLog minMaxLog1 = getInventoryMinMaxLog(1l,
        new DateTime(2018, 5, 31, 23, 0, 0, DateTimeZone.forID(UTC)).toDate(),
        BigDecimal.valueOf(100), BigDecimal.valueOf(1000));
    InventoryMinMaxLog minMaxLog2 = getInventoryMinMaxLog(2l,
        new DateTime(2018,5,25,0,0,0,DateTimeZone.forID(UTC)).toDate(), BigDecimal.valueOf(50),BigDecimal.valueOf(500));
    InventoryMinMaxLog minMaxLog3 = getInventoryMinMaxLog(3l,new DateTime(2018,4,30,23,0,0,DateTimeZone.forID(UTC)).toDate(), BigDecimal.valueOf(25), BigDecimal.valueOf(250));
    InventoryMinMaxLog minMaxLog4 = getInventoryMinMaxLog(4l,new DateTime(2018,4,30,1,0,0,DateTimeZone.forID(UTC)).toDate(), BigDecimal.valueOf(15), BigDecimal.valueOf(150));
    InventoryMinMaxLog minMaxLog5 = getInventoryMinMaxLog(5l,new DateTime(2018,1,1,0,0,0,DateTimeZone.forID(UTC)).toDate(), BigDecimal.ZERO, BigDecimal.ZERO);
    minMaxLogs.add(minMaxLog1);
    minMaxLogs.add(minMaxLog2);
    minMaxLogs.add(minMaxLog3);
    minMaxLogs.add(minMaxLog4);
    minMaxLogs.add(minMaxLog5);
    return minMaxLogs;
  }

  /**
   * Returns a list of IInventoryMinMaxLog objects with dates in UTC time zone. The dates and min max values are as follows:
   * June 10th 2018 23h 100,1000
   * May 31st 2018 1h 50,500
   * May 15th, 2018 10h 25,250
   * May 1st, 2018 0h 15,150
   * April 30th, 2018 0h 0,0
   * @return
   */
  private List<IInventoryMinMaxLog> getInventoryMinMaxLogsForWeeklyPeriodicity() {
    List<IInventoryMinMaxLog> minMaxLogs = new ArrayList<>(5);
    InventoryMinMaxLog minMaxLog1 = getInventoryMinMaxLog(1l, new DateTime(2018,6,10,23,0,0,DateTimeZone.forID(UTC)).toDate(),
        BigDecimal.valueOf(100), BigDecimal.valueOf(1000));
    InventoryMinMaxLog minMaxLog2 = getInventoryMinMaxLog(1l, new DateTime(2018,5,31,1,0,0,DateTimeZone.forID(UTC)).toDate(),
        BigDecimal.valueOf(50), BigDecimal.valueOf(500));
    InventoryMinMaxLog minMaxLog3 = getInventoryMinMaxLog(1l, new DateTime(2018,5,15,10,0,0,DateTimeZone.forID(UTC)).toDate(),
        BigDecimal.valueOf(25), BigDecimal.valueOf(250));
    InventoryMinMaxLog minMaxLog4 = getInventoryMinMaxLog(2l,
        new DateTime(2018,5,1,0,0,0,DateTimeZone.forID(UTC)).toDate(), BigDecimal.valueOf(15),BigDecimal.valueOf(150));
    InventoryMinMaxLog minMaxLog5 = getInventoryMinMaxLog(3l,new DateTime(2018,4,30,23,0,0,DateTimeZone.forID(UTC)).toDate(), BigDecimal.valueOf(0), BigDecimal.valueOf(0));
    minMaxLogs.add(minMaxLog1);
    minMaxLogs.add(minMaxLog2);
    minMaxLogs.add(minMaxLog3);
    minMaxLogs.add(minMaxLog4);
    minMaxLogs.add(minMaxLog5);
    return minMaxLogs;
  }


  /**
   * Returns a list of IInventoryMinMaxLog objects with dates in UTC time zone. The dates and min max values are as follows:
   * May 31st, 2018 23h 100,1000
   * May 31st 2018 11h 50,500
   * May 31st, 2018 0h 25,250
   * May 29th, 2018 1h 15,150
   * May 28th, 2018 0h 0,0
   * @return
   */
  private List<IInventoryMinMaxLog> getInventoryMinMaxLogsForDailyPeriodicity() {
    List<IInventoryMinMaxLog> minMaxLogs = new ArrayList<>(5);
    InventoryMinMaxLog minMaxLog1 = getInventoryMinMaxLog(1l,
        new DateTime(2018, 5, 31, 23, 0, 0, DateTimeZone.forID(UTC)).toDate(),
        BigDecimal.valueOf(100), BigDecimal.valueOf(1000));
    InventoryMinMaxLog minMaxLog2 = getInventoryMinMaxLog(1l, new DateTime(2018,5,31,11,0,0,DateTimeZone.forID(UTC)).toDate(),
        BigDecimal.valueOf(50), BigDecimal.valueOf(500));
    InventoryMinMaxLog minMaxLog3 = getInventoryMinMaxLog(1l, new DateTime(2018,5,31,0,0,0,DateTimeZone.forID(UTC)).toDate(),
        BigDecimal.valueOf(25), BigDecimal.valueOf(250));
    InventoryMinMaxLog minMaxLog4 = getInventoryMinMaxLog(2l,
        new DateTime(2018,5,29,1,0,0,DateTimeZone.forID(UTC)).toDate(), BigDecimal.valueOf(15),BigDecimal.valueOf(150));
    InventoryMinMaxLog minMaxLog5 = getInventoryMinMaxLog(3l,new DateTime(2018,5,28,23,0,0,DateTimeZone.forID(UTC)).toDate(), BigDecimal.valueOf(0), BigDecimal.valueOf(0));
    minMaxLogs.add(minMaxLog1);
    minMaxLogs.add(minMaxLog2);
    minMaxLogs.add(minMaxLog3);
    minMaxLogs.add(minMaxLog4);
    minMaxLogs.add(minMaxLog5);
    return minMaxLogs;
  }

  private InventoryMinMaxLog getInventoryMinMaxLog(Long id, Date createdTime, BigDecimal min, BigDecimal max) {
    InventoryMinMaxLog minMaxLog = new InventoryMinMaxLog();
    minMaxLog.setId(id);
    minMaxLog.setCreatedTime(createdTime);
    minMaxLog.setMin(min);
    minMaxLog.setMax(max);
    return minMaxLog;
  }

  /**
   * Returns ReportMinMaxHistoryFilters object with from date 1st December, 2017 and to date 1st June 2018.
   * @return
   */
  private ReportMinMaxHistoryFilters getReportMinMaxHistoryFiltersWithMonthlyPeriodicity() {
    return (ReportMinMaxHistoryFilters.builder().periodicity(QueryHelper.MONTH).from("2017-12-01").to("2018-06-01").build());
  }

  /**
   * Returns ReportMinMaxHistoryFilters object with from date 30th April, 2018 and to date 11th June 2018.
   * @return
   */
  private ReportMinMaxHistoryFilters getReportMinMaxHistoryFiltersWithWeeklyPeriodicity() {
    return (ReportMinMaxHistoryFilters.builder().periodicity(QueryHelper.WEEK).from("2018-04-30").to(
        "2018-06-11").build());
  }

  /**
   * Returns ReportMinMaxHistoryFilters object with from date 28th May, 2018 and to date 2nd June 2018.
   * @return
   */
  private ReportMinMaxHistoryFilters getReportMinMaxHistoryFiltersWithDailyPeriodicity() {
    return (ReportMinMaxHistoryFilters.builder().periodicity(QueryHelper.DAY).from("2018-05-28").to(
        "2018-06-02").build());
  }

  private ReportChartModel getReportChartModel(String label, List<String> values) {
    ReportChartModel reportChartModel = new ReportChartModel();
    reportChartModel.setLabel(label);
    List<ReportDataModel> reportDataModels = values.stream().map(value -> new ReportDataModel(value)).collect(Collectors.toList());
    reportChartModel.setValue(reportDataModels);
    return reportChartModel;
  }

  private List<String> getListWithMinZeroMaxZero() {
    return Arrays.asList(EMPTY,ZERO,ZERO);
  }

  private List<String> getListWithMinHundredMaxThousand() {
    return Arrays.asList(EMPTY,HUNDRED,THOUSAND);
  }

  private List<String> getListWithMinFiftyMaxFiveHundred() {
    return Arrays.asList(EMPTY,FIFTY,FIVE_HUNDRED);
  }

  private List<String> getListWithMinFifteenMaxOneHundredAndFifty() {
    return Arrays.asList(EMPTY,FIFTEEN,ONE_HUNDRED_AND_FIFTY);
  }

  private List<String> getListWithMinTwentyFiveMaxTwoHundredAndFifty() {
    return Arrays.asList(EMPTY,TWENTY_FIVE,TWO_HUNDRED_AND_FIFTY);
  }

  private List<String> getListWithMinEmptyMaxEmpty() {
    return Arrays.asList(EMPTY,EMPTY,EMPTY);
  }

  private List<ReportDataModel> getReportDataModels(List<String> values) {
    return (values.stream().map(value -> new ReportDataModel(value)).collect(Collectors.toList()));
  }
}