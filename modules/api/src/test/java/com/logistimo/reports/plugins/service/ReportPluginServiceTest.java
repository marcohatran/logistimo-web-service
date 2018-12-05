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

package com.logistimo.reports.plugins.service;

import com.logistimo.auth.utils.SecurityUtils;
import com.logistimo.config.models.DomainConfig;
import com.logistimo.inventory.entity.IInventoryMinMaxLog;
import com.logistimo.inventory.entity.InventoryMinMaxLog;
import com.logistimo.inventory.service.InventoryManagementService;
import com.logistimo.reports.models.ReportMinMaxHistoryFilters;
import com.logistimo.services.ServiceException;

import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.when;
import static org.powermock.api.mockito.PowerMockito.mockStatic;

/**
 * Created by vani on 06/06/18.
 */
@RunWith(PowerMockRunner.class)
@PrepareForTest({SecurityUtils.class, DomainConfig.class})
public class ReportPluginServiceTest {
  @Mock
  InventoryManagementService inventoryManagementService;

  @InjectMocks
  ReportPluginService reportPluginService;

  private static final String UTC = "UTC";
  private static final String EST = "EST";
  private static final String IST = "Asia/Kolkata";

  @Before
  public void setup() {
    mockStatic(SecurityUtils.class);
    mockStatic(DomainConfig.class);
  }

  @Test(expected = IllegalArgumentException.class)
  public void testGetMinMaxHistoryReportDataWithNullFilters() throws Exception {
    reportPluginService.getMinMaxHistoryReportData(null);
  }

  @Test
  public void testGetMinMaxHistoryReportDataWithDomainTimeUTC() throws Exception {
    PowerMockito.when(SecurityUtils.getCurrentDomainId()).thenReturn(1l);
    PowerMockito.when(DomainConfig.getInstance(1l))
        .thenReturn(getDomainConfigWithUTCTimezone());
    try {
      Date fromDate = new DateTime(2018,5,1,0,0,0,
          DateTimeZone.forID(UTC)).toDate();
      Date toDate = new DateTime(2018,5,8,0,0,0,
          DateTimeZone.forID(UTC)).toDate();
      when(inventoryManagementService
          .fetchMinMaxLogByInterval(1l, 1l, fromDate,
              toDate))
          .thenReturn(getInventoryMinMaxLogs());
    } catch(ServiceException e) {
      // ignored
    }
    ReportMinMaxHistoryFilters minMaxHistoryFilters = ReportMinMaxHistoryFilters.builder().from("2018-05-1").to("2018-05-8").entityId(1l).materialId(1l).build();
    assertEquals(1, reportPluginService.getMinMaxHistoryReportData(minMaxHistoryFilters).size());
  }

  @Test
  public void testGetMinMaxHistoryReportDataWithDomainTimeEST() throws Exception {
    PowerMockito.when(SecurityUtils.getCurrentDomainId()).thenReturn(2l);
    PowerMockito.when(DomainConfig.getInstance(2l))
        .thenReturn(getDomainConfigWithESTTimezone());

    try {
      Date fromDate = new DateTime(2018,5,1,0,0,0,
          DateTimeZone.forID(EST)).toDate();
      Date toDate = new DateTime(2018,5,8,0,0,0,
          DateTimeZone.forID(EST)).toDate();
      when(inventoryManagementService
          .fetchMinMaxLogByInterval(1l, 1l, fromDate,
              toDate))
          .thenReturn(getInventoryMinMaxLogs());
    } catch(ServiceException e) {
      // ignored
    }
    ReportMinMaxHistoryFilters minMaxHistoryFilters = ReportMinMaxHistoryFilters.builder().from("2018-05-1").to("2018-05-8").entityId(1l).materialId(1l).build();
    assertEquals(1, reportPluginService.getMinMaxHistoryReportData(minMaxHistoryFilters).size());
  }

  @Test
  public void testGetMinMaxHistoryReportDataWithDomainTimeIST() throws Exception {
    PowerMockito.when(SecurityUtils.getCurrentDomainId()).thenReturn(3l);
    PowerMockito.when(DomainConfig.getInstance(3l))
        .thenReturn(getDomainConfigWithISTTimezone());
    try {
      Date fromDate = new DateTime(2018,5,1,0,0,0,
          DateTimeZone.forID(IST)).toDate();
      Date toDate = new DateTime(2018,5,8,0,0,0,
          DateTimeZone.forID(IST)).toDate();
      when(inventoryManagementService
          .fetchMinMaxLogByInterval(1l, 1l, fromDate,
              toDate))
          .thenReturn(getInventoryMinMaxLogs());
    } catch(ServiceException e) {
      // ignored
    }
    ReportMinMaxHistoryFilters minMaxHistoryFilters = ReportMinMaxHistoryFilters.builder().from("2018-05-1").to("2018-05-8").entityId(1l).materialId(1l).build();
    assertEquals(1, reportPluginService.getMinMaxHistoryReportData(minMaxHistoryFilters).size());
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

  private List<IInventoryMinMaxLog> getInventoryMinMaxLogs() {
    List<IInventoryMinMaxLog> minMaxLogs = new ArrayList<>(1);
    minMaxLogs.add(new InventoryMinMaxLog());
    return minMaxLogs;
  }
}