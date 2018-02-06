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

package com.logistimo.reports.utils;

import com.logistimo.constants.CharacterConstants;
import com.logistimo.reports.constants.ReportAggregationConstants;
import com.logistimo.reports.constants.ReportType;

import org.junit.Test;

import java.util.HashMap;
import java.util.Map;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

/**
 * Created by vani on 28/12/17.
 */
public class ReportsUtilTest {
  @Test
  public void testGetAggregationReportType() throws Exception {
    Map<ReportType,String> reportTypeAggrTypeMap = getReportTypeAggregationTypeMap();
    String aggRepType = ReportsUtil.getAggregationReportType(null);
    assertNotNull(aggRepType);
    assertEquals(aggRepType, CharacterConstants.EMPTY);
    reportTypeAggrTypeMap.forEach((key, value) -> {
      String aggReportType = ReportsUtil.getAggregationReportType(key.toString());
      assertEquals(aggReportType,value);
    });
  }

  private Map<ReportType,String> getReportTypeAggregationTypeMap() {
    Map<ReportType,String> reportTypeAggrTypeMap = new HashMap<>(23,1);
    reportTypeAggrTypeMap.put(ReportType.AS_ASSET_STATUS, ReportAggregationConstants.ASSET_AGGREGATION_KEY);
    reportTypeAggrTypeMap.put(ReportType.AS_RESPONSE_TIME, ReportAggregationConstants.ASSET_AGGREGATION_KEY);
    reportTypeAggrTypeMap.put(ReportType.AS_UP_TIME, ReportAggregationConstants.ASSET_AGGREGATION_KEY);
    reportTypeAggrTypeMap.put(ReportType.INV_TRANSACTION_COUNT, ReportAggregationConstants.LOGISTIMO_AGGREGATION_KEY);
    reportTypeAggrTypeMap.put(ReportType.INV_ABNORMAL_STOCK, ReportAggregationConstants.LOGISTIMO_AGGREGATION_KEY);
    reportTypeAggrTypeMap.put(ReportType.INV_CONSUMPTION, ReportAggregationConstants.LOGISTIMO_AGGREGATION_KEY);
    reportTypeAggrTypeMap.put(ReportType.INV_DISCARDS, ReportAggregationConstants.LOGISTIMO_AGGREGATION_KEY);
    reportTypeAggrTypeMap.put(ReportType.INV_REPELISHMENT, ReportAggregationConstants.LOGISTIMO_AGGREGATION_KEY);
    reportTypeAggrTypeMap.put(ReportType.INV_STOCK_AVAILABILITY, ReportAggregationConstants.LOGISTIMO_AGGREGATION_KEY);
    reportTypeAggrTypeMap.put(ReportType.INV_STOCK_TREND, ReportAggregationConstants.LOGISTIMO_AGGREGATION_KEY);
    reportTypeAggrTypeMap.put(ReportType.INV_SUPPLY, ReportAggregationConstants.LOGISTIMO_AGGREGATION_KEY);
    reportTypeAggrTypeMap.put(ReportType.INV_UTILISATION, ReportAggregationConstants.LOGISTIMO_AGGREGATION_KEY);
    reportTypeAggrTypeMap.put(ReportType.ACTIVITY_USER, ReportAggregationConstants.LOGISTIMO_AGGREGATION_KEY);
    reportTypeAggrTypeMap.put(ReportType.ORD_APPROVAL_RESPONSE_TIME, ReportAggregationConstants.LOGISTIMO_AGGREGATION_KEY);
    reportTypeAggrTypeMap.put(ReportType.ORD_APPROVAL_REQUEST_STATUS, ReportAggregationConstants.LOGISTIMO_AGGREGATION_KEY);
    reportTypeAggrTypeMap.put(ReportType.ORD_ORDER_STATUS, ReportAggregationConstants.LOGISTIMO_AGGREGATION_KEY);
    reportTypeAggrTypeMap.put(ReportType.ORD_DEMAND, ReportAggregationConstants.LOGISTIMO_AGGREGATION_KEY);
    reportTypeAggrTypeMap.put(ReportType.ORD_ORDER_DISCREPANCIES, ReportAggregationConstants.LOGISTIMO_AGGREGATION_KEY);
    reportTypeAggrTypeMap.put(ReportType.ORD_ORDER_RESPONSE_TIME, ReportAggregationConstants.LOGISTIMO_AGGREGATION_KEY);
    reportTypeAggrTypeMap.put(ReportType.AS_CAPACITY, ReportAggregationConstants.DEVICE_STATUS_AGGREGATION_KEY);
    reportTypeAggrTypeMap.put(ReportType.AS_SICKNESS_RATE, ReportAggregationConstants.DEVICE_STATUS_AGGREGATION_KEY);
    reportTypeAggrTypeMap.put(ReportType.AS_POWER_AVAILABILITY, ReportAggregationConstants.ALARM_LOG_AGGREGATION_KEY);
    reportTypeAggrTypeMap.put(ReportType.AS_TEMPERATURE_EXCURSION, ReportAggregationConstants.ALARM_LOG_AGGREGATION_KEY);
    return reportTypeAggrTypeMap;
  }
}