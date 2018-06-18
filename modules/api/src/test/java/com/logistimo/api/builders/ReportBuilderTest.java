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
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

import java.math.BigDecimal;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.powermock.api.mockito.PowerMockito.mockStatic;

/**
 * Created by vani on 05/06/18.
 */
@RunWith(PowerMockRunner.class)
@PrepareForTest({SecurityUtils.class, DomainConfig.class})
public class ReportBuilderTest {
  ReportBuilder reportBuilder = new ReportBuilder();

  @Before
  public void setup() {
    mockStatic(SecurityUtils.class);
    mockStatic(DomainConfig.class);
    PowerMockito.when(SecurityUtils.getCurrentDomainId()).thenReturn(1l);
    PowerMockito.when(DomainConfig.getInstance(1l))
        .thenReturn(getDomainConfig());
  }

  @Test
  public void testGetMinMaxHistoryReportLabelWithMonthlyPeriodicity() throws ParseException {
    String actualResult = reportBuilder.getMinMaxHistoryReportLabel(new DateTime(2018,5,20,0,0,0).toDate(), QueryHelper.MONTH, "2018-01-01");
    assertEquals("2018-05-01", actualResult);
  }

  @Test
  public void testGetMinMaxHistoryReportLabelWithWeeklyPeriodicity() throws ParseException {
    String actualResult = reportBuilder.getMinMaxHistoryReportLabel(new DateTime(2018,5,31,0,0,0).toDate(), QueryHelper.WEEK, "2018-01-01");
    assertEquals("2018-05-28", actualResult);
  }

  @Test (expected = ParseException.class)
  public void testGetMinMaxHistoryReportLabelWithMonthlyPeriodicityAndInvalidFromDateFormat() throws ParseException{
    String actualResult = reportBuilder.getMinMaxHistoryReportLabel(new DateTime(2018,5,20,0,0,0).toDate(), QueryHelper.MONTH, "2018-01");
    assertEquals("2018-05-01", actualResult);
  }

  @Test
  public void testGetMinMaxHistoryReportLabelWithMonthlyPeriodicityAndCreatedTimeLessThanFromTime() throws ParseException{
    String actualResult = reportBuilder.getMinMaxHistoryReportLabel(new DateTime(2017,12,31,0,0,0).toDate(), QueryHelper.MONTH, "2018-01-01");
    assertEquals("2018-01-01", actualResult);
  }

  @Test
  public void testGetMinMaxHistoryReportLabelWithWeeklyPeriodicityAndCreatedTimeLessThanFromTime() throws ParseException{
    String actualResult = reportBuilder.getMinMaxHistoryReportLabel(new DateTime(2017,12,31,0,0,0).toDate(), QueryHelper.WEEK, "2018-01-01");
    assertEquals("2018-01-01", actualResult);
  }

  @Test
  public void testGetMinMaxHistoryReportValuesWithNullMinMax() {
    List<ReportDataModel> actualResult = reportBuilder.getMinMaxHistoryReportValues(null,null);
    assertEquals(3,actualResult.size());
    assertEquals("",actualResult.get(0).value);
    assertEquals("0",actualResult.get(1).value);
    assertEquals("0",actualResult.get(2).value);
  }

  @Test
  public void testGetMinMaxHistoryReportValuesWithValidMinMax() {
    List<ReportDataModel> reportDataModels = reportBuilder.getMinMaxHistoryReportValues(
        BigDecimal.ONE, BigDecimal.TEN);
    assertEquals(3,reportDataModels.size());
    assertEquals("",reportDataModels.get(0).value);
    assertEquals("1",reportDataModels.get(1).value);
    assertEquals("10",reportDataModels.get(2).value);
  }

  @Test
  public void testBuildMinMaxHistoryReportsDataForMonthlyPeriodicityWithUniqueCreatedDateList() throws ParseException{
    List<IInventoryMinMaxLog> inventoryMinMaxLogs = getInventoryMinMaxLogsWithUniqueCreatedDate();
    ReportMinMaxHistoryFilters reportMinMaxHistoryFilters = getReportMinMaxHistoryFiltersWithMonthlyPeriodicity();
    List<ReportChartModel> actualResult = reportBuilder.buildMinMaxHistoryReportsData(inventoryMinMaxLogs, reportMinMaxHistoryFilters);
    assertEquals(6, actualResult.size());
    assertEquals("2018-05-01", actualResult.get(0).getLabel());
    assertEquals("",actualResult.get(0).value.get(0).value);
    assertEquals("100",actualResult.get(0).value.get(1).value);
    assertEquals("1000",actualResult.get(0).value.get(2).value);

    assertEquals("2018-04-01", actualResult.get(1).getLabel());
    assertEquals("",actualResult.get(1).value.get(0).value);
    assertEquals("50",actualResult.get(1).value.get(1).value);
    assertEquals("500",actualResult.get(1).value.get(2).value);

    assertEquals("2018-03-01", actualResult.get(2).getLabel());
    assertEquals("",actualResult.get(2).value.get(0).value);
    assertEquals("25",actualResult.get(2).value.get(1).value);
    assertEquals("250",actualResult.get(2).value.get(2).value);

    assertEquals("2018-02-01", actualResult.get(3).getLabel());
    assertEquals("",actualResult.get(3).value.get(0).value);
    assertEquals("15",actualResult.get(3).value.get(1).value);
    assertEquals("150",actualResult.get(3).value.get(2).value);

    assertEquals("2018-01-01", actualResult.get(4).getLabel());
    assertEquals("",actualResult.get(4).value.get(0).value);
    assertEquals("0",actualResult.get(4).value.get(1).value);
    assertEquals("0",actualResult.get(4).value.get(2).value);

    assertEquals("2017-12-01", actualResult.get(5).getLabel());
    assertEquals("",actualResult.get(5).value.get(0).value);
    assertEquals("",actualResult.get(5).value.get(1).value);
    assertEquals("",actualResult.get(5).value.get(2).value);
  }

  @Test
  public void testBuildMinMaxHistoryReportsDataForMonthlyPeriodicityWithNonUniqueCreatedDateList() throws ParseException{
    List<IInventoryMinMaxLog> inventoryMinMaxLogs = getInventoryMinMaxLogsWithNonUniqueCreatedDate();
    ReportMinMaxHistoryFilters reportMinMaxHistoryFilters = getReportMinMaxHistoryFiltersWithMonthlyPeriodicity();
    List<ReportChartModel> actualResult = reportBuilder.buildMinMaxHistoryReportsData(inventoryMinMaxLogs, reportMinMaxHistoryFilters);
    assertEquals(6, actualResult.size());
    assertEquals("2018-05-01", actualResult.get(0).getLabel());
    assertEquals("",actualResult.get(0).value.get(0).value);
    assertEquals("100",actualResult.get(0).value.get(1).value);
    assertEquals("1000",actualResult.get(0).value.get(2).value);

    assertEquals("2018-04-01", actualResult.get(1).getLabel());
    assertEquals("",actualResult.get(1).value.get(0).value);
    assertEquals("25",actualResult.get(1).value.get(1).value);
    assertEquals("250",actualResult.get(1).value.get(2).value);

    assertEquals("2018-01-01", actualResult.get(2).getLabel());
    assertEquals("",actualResult.get(2).value.get(0).value);
    assertEquals("0",actualResult.get(2).value.get(1).value);
    assertEquals("0",actualResult.get(2).value.get(2).value);

    assertEquals("2017-12-01", actualResult.get(3).getLabel());
    assertEquals("",actualResult.get(3).value.get(0).value);
    assertEquals("",actualResult.get(3).value.get(1).value);
    assertEquals("",actualResult.get(3).value.get(2).value);

    assertEquals("2018-02-01", actualResult.get(4).getLabel());
    assertEquals("",actualResult.get(4).value.get(0).value);
    assertEquals("0",actualResult.get(4).value.get(1).value);
    assertEquals("0",actualResult.get(4).value.get(2).value);

    assertEquals("2018-03-01", actualResult.get(5).getLabel());
    assertEquals("",actualResult.get(5).value.get(0).value);
    assertEquals("0",actualResult.get(5).value.get(1).value);
    assertEquals("0",actualResult.get(5).value.get(2).value);

  }

  @Test
  public void testBuildMinMaxHistoryReportsDataForWeeklyPeriodicityWithUniqueCreatedDateList() throws ParseException{
    List<IInventoryMinMaxLog> inventoryMinMaxLogs = getInventoryMinMaxLogsWithUniqueCreatedDateForWeeklyPeriodicity();
    ReportMinMaxHistoryFilters reportMinMaxHistoryFilters = getReportMinMaxHistoryFiltersWithWeeklyPeriodicity();
    List<ReportChartModel> actualResult = reportBuilder.buildMinMaxHistoryReportsData(inventoryMinMaxLogs, reportMinMaxHistoryFilters);
    assertEquals(6, actualResult.size());
    assertEquals("2018-04-30", actualResult.get(0).getLabel());
    assertEquals("",actualResult.get(0).value.get(0).value);
    assertEquals("100",actualResult.get(0).value.get(1).value);
    assertEquals("1000",actualResult.get(0).value.get(2).value);

    assertEquals("2018-04-02", actualResult.get(1).getLabel());
    assertEquals("",actualResult.get(1).value.get(0).value);
    assertEquals("50",actualResult.get(1).value.get(1).value);
    assertEquals("500",actualResult.get(1).value.get(2).value);

    assertEquals("2018-04-09", actualResult.get(2).getLabel());
    assertEquals("",actualResult.get(2).value.get(0).value);
    assertEquals("50",actualResult.get(2).value.get(1).value);
    assertEquals("500",actualResult.get(2).value.get(2).value);

    assertEquals("2018-04-16", actualResult.get(3).getLabel());
    assertEquals("",actualResult.get(3).value.get(0).value);
    assertEquals("50",actualResult.get(3).value.get(1).value);
    assertEquals("500",actualResult.get(3).value.get(2).value);

    assertEquals("2018-04-23", actualResult.get(4).getLabel());
    assertEquals("",actualResult.get(4).value.get(0).value);
    assertEquals("50",actualResult.get(4).value.get(1).value);
    assertEquals("500",actualResult.get(4).value.get(2).value);

    assertEquals("2018-05-07", actualResult.get(5).getLabel());
    assertEquals("",actualResult.get(5).value.get(0).value);
    assertEquals("100",actualResult.get(5).value.get(1).value);
    assertEquals("1000",actualResult.get(5).value.get(2).value);
  }

  @Test
  public void testBuildMinMaxHistoryReportsDataForWeeklyPeriodicityWithNonUniqueCreatedDateList() throws ParseException{
    List<IInventoryMinMaxLog> inventoryMinMaxLogs = getInventoryMinMaxLogsWithNonUniqueCreatedDateForWeeklyPeriodicity();
    ReportMinMaxHistoryFilters reportMinMaxHistoryFilters = getReportMinMaxHistoryFiltersWithWeeklyPeriodicity();
    List<ReportChartModel> actualResult = reportBuilder.buildMinMaxHistoryReportsData(inventoryMinMaxLogs, reportMinMaxHistoryFilters);
    assertEquals(6, actualResult.size());
    assertEquals("2018-04-30", actualResult.get(0).getLabel());
    assertEquals("",actualResult.get(0).value.get(0).value);
    assertEquals("100",actualResult.get(0).value.get(1).value);
    assertEquals("1000",actualResult.get(0).value.get(2).value);

    assertEquals("2018-04-02", actualResult.get(1).getLabel());
    assertEquals("",actualResult.get(1).value.get(0).value);
    assertEquals("0",actualResult.get(1).value.get(1).value);
    assertEquals("0",actualResult.get(1).value.get(2).value);

    assertEquals("2018-04-09", actualResult.get(2).getLabel());
    assertEquals("",actualResult.get(2).value.get(0).value);
    assertEquals("0",actualResult.get(2).value.get(1).value);
    assertEquals("0",actualResult.get(2).value.get(2).value);

    assertEquals("2018-04-16", actualResult.get(3).getLabel());
    assertEquals("",actualResult.get(3).value.get(0).value);
    assertEquals("0",actualResult.get(3).value.get(1).value);
    assertEquals("0",actualResult.get(3).value.get(2).value);

    assertEquals("2018-04-23", actualResult.get(4).getLabel());
    assertEquals("",actualResult.get(4).value.get(0).value);
    assertEquals("0",actualResult.get(4).value.get(1).value);
    assertEquals("0",actualResult.get(4).value.get(2).value);

    assertEquals("2018-05-07", actualResult.get(5).getLabel());
    assertEquals("",actualResult.get(5).value.get(0).value);
    assertEquals("100",actualResult.get(5).value.get(1).value);
    assertEquals("1000",actualResult.get(5).value.get(2).value);
  }

  @Test
  public void testBuildMinMaxHistoryReportsDataForDailyPeriodicityWithUniqueCreatedDateList() throws ParseException{
    List<IInventoryMinMaxLog> inventoryMinMaxLogs = getInventoryMinMaxLogsWithUniqueCreatedDateForDailyPeriodicity();
    ReportMinMaxHistoryFilters reportMinMaxHistoryFilters = getReportMinMaxHistoryFiltersWithDailyPeriodicity();
    List<ReportChartModel> actualResult = reportBuilder.buildMinMaxHistoryReportsData(inventoryMinMaxLogs, reportMinMaxHistoryFilters);
    assertEquals(10, actualResult.size());
    assertEquals("2018-05-05", actualResult.get(0).getLabel());
    assertEquals("",actualResult.get(0).value.get(0).value);
    assertEquals("100",actualResult.get(0).value.get(1).value);
    assertEquals("1000",actualResult.get(0).value.get(2).value);

    assertEquals("2018-05-01", actualResult.get(1).getLabel());
    assertEquals("",actualResult.get(1).value.get(0).value);
    assertEquals("50",actualResult.get(1).value.get(1).value);
    assertEquals("500",actualResult.get(1).value.get(2).value);

    assertEquals("2018-05-02", actualResult.get(2).getLabel());
    assertEquals("",actualResult.get(2).value.get(0).value);
    assertEquals("50",actualResult.get(2).value.get(1).value);
    assertEquals("500",actualResult.get(2).value.get(2).value);

    assertEquals("2018-05-03", actualResult.get(3).getLabel());
    assertEquals("",actualResult.get(3).value.get(0).value);
    assertEquals("50",actualResult.get(3).value.get(1).value);
    assertEquals("500",actualResult.get(3).value.get(2).value);

    assertEquals("2018-05-04", actualResult.get(4).getLabel());
    assertEquals("",actualResult.get(4).value.get(0).value);
    assertEquals("50",actualResult.get(4).value.get(1).value);
    assertEquals("500",actualResult.get(4).value.get(2).value);

    assertEquals("2018-05-06", actualResult.get(5).getLabel());
    assertEquals("",actualResult.get(5).value.get(0).value);
    assertEquals("100",actualResult.get(5).value.get(1).value);
    assertEquals("1000",actualResult.get(5).value.get(2).value);

    assertEquals("2018-05-07", actualResult.get(6).getLabel());
    assertEquals("",actualResult.get(6).value.get(0).value);
    assertEquals("100",actualResult.get(6).value.get(1).value);
    assertEquals("1000",actualResult.get(6).value.get(2).value);

    assertEquals("2018-05-08", actualResult.get(7).getLabel());
    assertEquals("",actualResult.get(7).value.get(0).value);
    assertEquals("100",actualResult.get(7).value.get(1).value);
    assertEquals("1000",actualResult.get(7).value.get(2).value);

    assertEquals("2018-05-09", actualResult.get(8).getLabel());
    assertEquals("",actualResult.get(8).value.get(0).value);
    assertEquals("100",actualResult.get(8).value.get(1).value);
    assertEquals("1000",actualResult.get(8).value.get(2).value);

    assertEquals("2018-05-10", actualResult.get(9).getLabel());
    assertEquals("",actualResult.get(9).value.get(0).value);
    assertEquals("100",actualResult.get(9).value.get(1).value);
    assertEquals("1000",actualResult.get(9).value.get(2).value);
  }

  @Test
  public void testBuildMinMaxHistoryReportsDataForDailyPeriodicityWithNonUniqueCreatedDateList() throws ParseException{
    List<IInventoryMinMaxLog> inventoryMinMaxLogs = getInventoryMinMaxLogsWithNonUniqueCreatedDateForDailyPeriodicity();
    ReportMinMaxHistoryFilters reportMinMaxHistoryFilters = getReportMinMaxHistoryFiltersWithDailyPeriodicity();
    List<ReportChartModel> actualResult = reportBuilder.buildMinMaxHistoryReportsData(inventoryMinMaxLogs, reportMinMaxHistoryFilters);
    assertEquals(10, actualResult.size());
    assertEquals("2018-05-05", actualResult.get(0).getLabel());
    assertEquals("",actualResult.get(0).value.get(0).value);
    assertEquals("100",actualResult.get(0).value.get(1).value);
    assertEquals("1000",actualResult.get(0).value.get(2).value);

    assertEquals("2018-05-01", actualResult.get(1).getLabel());
    assertEquals("",actualResult.get(1).value.get(0).value);
    assertEquals("25",actualResult.get(1).value.get(1).value);
    assertEquals("250",actualResult.get(1).value.get(2).value);

    assertEquals("2018-05-02", actualResult.get(2).getLabel());
    assertEquals("",actualResult.get(2).value.get(0).value);
    assertEquals("25",actualResult.get(2).value.get(1).value);
    assertEquals("250",actualResult.get(2).value.get(2).value);

    assertEquals("2018-05-03", actualResult.get(3).getLabel());
    assertEquals("",actualResult.get(3).value.get(0).value);
    assertEquals("25",actualResult.get(3).value.get(1).value);
    assertEquals("250",actualResult.get(3).value.get(2).value);

    assertEquals("2018-05-04", actualResult.get(4).getLabel());
    assertEquals("",actualResult.get(4).value.get(0).value);
    assertEquals("25",actualResult.get(4).value.get(1).value);
    assertEquals("250",actualResult.get(4).value.get(2).value);

    assertEquals("2018-05-06", actualResult.get(5).getLabel());
    assertEquals("",actualResult.get(5).value.get(0).value);
    assertEquals("100",actualResult.get(5).value.get(1).value);
    assertEquals("1000",actualResult.get(5).value.get(2).value);

    assertEquals("2018-05-07", actualResult.get(6).getLabel());
    assertEquals("",actualResult.get(6).value.get(0).value);
    assertEquals("100",actualResult.get(6).value.get(1).value);
    assertEquals("1000",actualResult.get(6).value.get(2).value);

    assertEquals("2018-05-08", actualResult.get(7).getLabel());
    assertEquals("",actualResult.get(7).value.get(0).value);
    assertEquals("100",actualResult.get(7).value.get(1).value);
    assertEquals("1000",actualResult.get(7).value.get(2).value);

    assertEquals("2018-05-09", actualResult.get(8).getLabel());
    assertEquals("",actualResult.get(8).value.get(0).value);
    assertEquals("100",actualResult.get(8).value.get(1).value);
    assertEquals("1000",actualResult.get(8).value.get(2).value);

    assertEquals("2018-05-10", actualResult.get(9).getLabel());
    assertEquals("",actualResult.get(9).value.get(0).value);
    assertEquals("100",actualResult.get(9).value.get(1).value);
    assertEquals("1000",actualResult.get(9).value.get(2).value);
  }

  @Test
  public void testFillMinMaxHistoryChartData() throws ParseException{
    reportBuilder.fillMinMaxHistoryChartData(null,getReportMinMaxHistoryFiltersWithDailyPeriodicity());
  }

  /**
   * Returns a list of IInventoryMinMaxLog objects with the following values for date(format: yyyy-MM-dd HH:mm:ss)  and min-max
   * 2018-05-05 100,1000; 2018-04-05 50,500; 2018-03-05 25,250; 2018-02-05 15,150; 2018-01-05 0,0; time fields set to 0
   * @return
   */
  private List<IInventoryMinMaxLog> getInventoryMinMaxLogsWithUniqueCreatedDate() {
    List<IInventoryMinMaxLog> minMaxLogs = new ArrayList<>(5);
    InventoryMinMaxLog minMaxLog1 = getInventoryMinMaxLog(1l, new DateTime(2018,5,5,0,0,0).toDate(),
        BigDecimal.valueOf(100), BigDecimal.valueOf(1000));
    InventoryMinMaxLog minMaxLog2 = getInventoryMinMaxLog(2l,
        new DateTime(2018,4,5,0,0,0).toDate(), BigDecimal.valueOf(50),BigDecimal.valueOf(500));
    InventoryMinMaxLog minMaxLog3 = getInventoryMinMaxLog(3l,new DateTime(2018,3,5,0,0,0).toDate(), BigDecimal.valueOf(25), BigDecimal.valueOf(250));
    InventoryMinMaxLog minMaxLog4 = getInventoryMinMaxLog(4l,new DateTime(2018,2,5,0,0,0).toDate(), BigDecimal.valueOf(15), BigDecimal.valueOf(150));
    InventoryMinMaxLog minMaxLog5 = getInventoryMinMaxLog(5l,new DateTime(2018,1,5,0,0,0).toDate(), BigDecimal.ZERO, BigDecimal.ZERO);
    minMaxLogs.add(minMaxLog1);
    minMaxLogs.add(minMaxLog2);
    minMaxLogs.add(minMaxLog3);
    minMaxLogs.add(minMaxLog4);
    minMaxLogs.add(minMaxLog5);
    return minMaxLogs;
  }

  /**
   * Returns a list of IInventoryMinMaxLog objects with the following values for date(format: yyyy-MM-dd HH:mm:ss) and min-max
   * 2018-05-05 100,1000; 2018-04-05 50,500; 2018-03-05 25,250; time fields set to 0
   * @return
   */
  private List<IInventoryMinMaxLog> getInventoryMinMaxLogsWithUniqueCreatedDateForWeeklyPeriodicity() {
    List<IInventoryMinMaxLog> minMaxLogs = new ArrayList<>(5);
    InventoryMinMaxLog minMaxLog1 = getInventoryMinMaxLog(1l, new DateTime(2018,5,5,0,0,0,0).toDate(),
        BigDecimal.valueOf(100), BigDecimal.valueOf(1000));
    InventoryMinMaxLog minMaxLog2 = getInventoryMinMaxLog(2l,
        new DateTime(2018,4,5,0,0,0,0).toDate(), BigDecimal.valueOf(50),BigDecimal.valueOf(500));
    InventoryMinMaxLog minMaxLog3 = getInventoryMinMaxLog(3l,new DateTime(2018,3,5,0,0,0,0).toDate(), BigDecimal.valueOf(25), BigDecimal.valueOf(250));

    minMaxLogs.add(minMaxLog1);
    minMaxLogs.add(minMaxLog2);
    minMaxLogs.add(minMaxLog3);

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
   * Returns a list of IInventoryMinMaxLog objects with created date (yyyy-MM-dd HH:mm:ss) and min max.
   * 2018-05-31 00:00:00 100,1000; 2018-05-25 00:00:00 50, 500; 2018-04-30 15:00:00 25,250; 2018-04-30 13:00:00 15, 150; 2018-01-01 00:00:00
   * @return
   */
  private List<IInventoryMinMaxLog> getInventoryMinMaxLogsWithNonUniqueCreatedDate() {
    List<IInventoryMinMaxLog> minMaxLogs = new ArrayList<>(5);
    InventoryMinMaxLog minMaxLog1 = getInventoryMinMaxLog(1l, new DateTime(2018,5,31,0,0,0).toDate(), BigDecimal.valueOf(100), BigDecimal.valueOf(1000));
    InventoryMinMaxLog minMaxLog2 = getInventoryMinMaxLog(2l, new DateTime(2018,5,25,0,0,0).toDate(), BigDecimal.valueOf(50), BigDecimal.valueOf(500));
    InventoryMinMaxLog minMaxLog3 = getInventoryMinMaxLog(3l, new DateTime(2018,4,30,15,0,0).toDate(), BigDecimal.valueOf(25), BigDecimal.valueOf(250));
    InventoryMinMaxLog minMaxLog4 = getInventoryMinMaxLog(4l, new DateTime(2018,4,30,13,0,0).toDate(), BigDecimal.valueOf(15), BigDecimal.valueOf(150));
    InventoryMinMaxLog minMaxLog5 = getInventoryMinMaxLog(5l, new DateTime(2018,1,1,0,0,0).toDate(), BigDecimal.ZERO, BigDecimal.ZERO);
    minMaxLogs.add(minMaxLog1);
    minMaxLogs.add(minMaxLog2);
    minMaxLogs.add(minMaxLog3);
    minMaxLogs.add(minMaxLog4);
    minMaxLogs.add(minMaxLog5);

    return minMaxLogs;
  }

  /**
   * Returns a list of IInventoryMinMaxLog objects with created date (format: yyyy-MM-dd HH:mm:ss) and min max.
   * 2018-05-05 00:00:00 100,1000; 2018-05-01 00:00:00 50, 500; 2018-04-30 15:00:00 25,250; 2018-04-30 13:00:00 15, 150; 2018-03-01 00:00:00
   * @return
   */
  private List<IInventoryMinMaxLog> getInventoryMinMaxLogsWithNonUniqueCreatedDateForWeeklyPeriodicity() {
    List<IInventoryMinMaxLog> minMaxLogs = new ArrayList<>(5);
    InventoryMinMaxLog minMaxLog1 = getInventoryMinMaxLog(1l, new DateTime(2018,5,5,0,0,0,0).toDate(), BigDecimal.valueOf(
        100), BigDecimal.valueOf(1000));
    InventoryMinMaxLog minMaxLog2 = getInventoryMinMaxLog(2l,
        new DateTime(2018,5,1,0,0,0,0).toDate(), BigDecimal.valueOf(50), BigDecimal.valueOf(500));
    InventoryMinMaxLog minMaxLog3 = getInventoryMinMaxLog(3l,
        new DateTime(2018,4,30,15,0,0,0).toDate(), BigDecimal.valueOf(25), BigDecimal.valueOf(250));
    InventoryMinMaxLog minMaxLog4 = getInventoryMinMaxLog(4l,new DateTime(2018,4,30,13,0,0,0).toDate(), BigDecimal.valueOf(15), BigDecimal.valueOf(150));
    InventoryMinMaxLog minMaxLog5 = getInventoryMinMaxLog(5l,new DateTime(2018,3,1,0,0,0,0).toDate(), BigDecimal.ZERO, BigDecimal.ZERO);
    minMaxLogs.add(minMaxLog1);
    minMaxLogs.add(minMaxLog2);
    minMaxLogs.add(minMaxLog3);
    minMaxLogs.add(minMaxLog4);
    minMaxLogs.add(minMaxLog5);

    return minMaxLogs;
  }

  /**
   * Returns a list of IInventoryMinMaxLog objects with the following values for date (format: yyyy-MM-dd HH:mm:ss) and min-max, with time feilds set to 0
   * 2018-05-05 100,1000; 2018-04-05 50,500;
   * @return
   */
  private List<IInventoryMinMaxLog> getInventoryMinMaxLogsWithUniqueCreatedDateForDailyPeriodicity() {
    List<IInventoryMinMaxLog> minMaxLogs = new ArrayList<>(5);
    DateTime createdTime = new DateTime(2018,5,5,0,0,0,0);
    InventoryMinMaxLog minMaxLog1 = getInventoryMinMaxLog(1l, createdTime.toDate(),
        BigDecimal.valueOf(100), BigDecimal.valueOf(1000));
    InventoryMinMaxLog minMaxLog2 = getInventoryMinMaxLog(2l,
        createdTime.minusMonths(1).toDate(), BigDecimal.valueOf(50),BigDecimal.valueOf(500));

    minMaxLogs.add(minMaxLog1);
    minMaxLogs.add(minMaxLog2);

    return minMaxLogs;
  }

  /**
   * Returns a list of IInventoryMinMaxLog objects with created date(format: yyyy-MM-dd HH:mm:ss) and min max.
   * 2018-05-05 15:00:00 100,1000; 2018-05-05 10:00:00 50, 500; 2018-04-30 15:00:00 25,250;
   * @return
   */
  private List<IInventoryMinMaxLog> getInventoryMinMaxLogsWithNonUniqueCreatedDateForDailyPeriodicity() {
    List<IInventoryMinMaxLog> minMaxLogs = new ArrayList<>(5);
    InventoryMinMaxLog minMaxLog1 = getInventoryMinMaxLog(1l, new DateTime(2018,5,5,15,0,0,0).toDate(), BigDecimal.valueOf(
        100), BigDecimal.valueOf(1000));
    InventoryMinMaxLog minMaxLog2 = getInventoryMinMaxLog(2l,
        new DateTime(2018,5,5,10,0,0).toDate(), BigDecimal.valueOf(50), BigDecimal.valueOf(500));
    InventoryMinMaxLog minMaxLog3 = getInventoryMinMaxLog(3l,
        new DateTime(2018,4,30,15,0,0,0).toDate(), BigDecimal.valueOf(25), BigDecimal.valueOf(250));

    minMaxLogs.add(minMaxLog1);
    minMaxLogs.add(minMaxLog2);
    minMaxLogs.add(minMaxLog3);

    return minMaxLogs;
  }

  private ReportMinMaxHistoryFilters getReportMinMaxHistoryFiltersWithMonthlyPeriodicity() {
    return (ReportMinMaxHistoryFilters.builder()
        .periodicity(QueryHelper.MONTH)
        .from("2017-12-01")
        .to("2018-05-01").build());
  }

  private ReportMinMaxHistoryFilters getReportMinMaxHistoryFiltersWithWeeklyPeriodicity() {
    return (ReportMinMaxHistoryFilters.builder()
        .periodicity(QueryHelper.WEEK)
        .from("2018-04-02")
        .to("2018-05-13").build());
  }

  private ReportMinMaxHistoryFilters getReportMinMaxHistoryFiltersWithDailyPeriodicity() {
    return (ReportMinMaxHistoryFilters.builder()
        .periodicity(QueryHelper.DAY)
        .from("2018-05-01")
        .to("2018-05-11").build());
  }

  private DomainConfig getDomainConfig() {
    DomainConfig domainConfig = new DomainConfig();
    domainConfig.setTimezone("Asia/Kolkata");
    return domainConfig;
  }
}