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

package com.logistimo.customreports;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import java.util.Calendar;
import java.util.TimeZone;

/**
 * @author Mohan Raja.
 */
public class TestCustomReportExportMgr {

  final static private String UTC = "UTC";
  Calendar utcCalendar;

  @Before
  public void setup() {
    utcCalendar = Calendar.getInstance(TimeZone.getTimeZone(UTC));
  }

  @Test
  public void testIsScheduleExport_UTC() {
    TimeZone utcTimeZone = TimeZone.getTimeZone(UTC);

    Calendar runTime = (Calendar) utcCalendar.clone();
    runTime.set(Calendar.DAY_OF_MONTH, 1);
    runTime.set(Calendar.HOUR_OF_DAY, 2);

    boolean firstDay = CustomReportsExportMgr.isScheduleExport(
        CustomReportConstants.FREQUENCY_MONTHLY,0,1, utcTimeZone, runTime, "23:00");
    Assert.assertEquals(true, firstDay);

    runTime.set(Calendar.DAY_OF_MONTH, 2);

    boolean nextDay = CustomReportsExportMgr.isScheduleExport(
        CustomReportConstants.FREQUENCY_MONTHLY,0,1, utcTimeZone, runTime, "23:00");
    Assert.assertEquals(false, nextDay);

    runTime.set(Calendar.DAY_OF_MONTH, 25);

    boolean betweenDay = CustomReportsExportMgr.isScheduleExport(
        CustomReportConstants.FREQUENCY_MONTHLY,0,1, utcTimeZone, runTime, "23:00");
    Assert.assertEquals(false, betweenDay);
  }

  @Test
  public void testIsScheduleExport_IST() {
    TimeZone istTimeZone = TimeZone.getTimeZone("Asia/Kolkata");

    Calendar runTime = (Calendar) utcCalendar.clone();
    runTime.set(Calendar.HOUR_OF_DAY, 2);

    runTime.set(Calendar.DAY_OF_MONTH, 1);
    boolean firstDay = CustomReportsExportMgr.isScheduleExport(
        CustomReportConstants.FREQUENCY_MONTHLY,0,1, istTimeZone, runTime, "22:30");
    Assert.assertEquals(false, firstDay);

    runTime.add(Calendar.DAY_OF_MONTH, -1);
    boolean previousDay = CustomReportsExportMgr.isScheduleExport(
        CustomReportConstants.FREQUENCY_MONTHLY,0,1, istTimeZone, runTime, "23:00");
    Assert.assertEquals(true, previousDay);

    runTime.set(Calendar.DAY_OF_MONTH, 25);
    boolean betweenDay = CustomReportsExportMgr.isScheduleExport(
        CustomReportConstants.FREQUENCY_MONTHLY,0,1, istTimeZone, runTime, "4:00");
    Assert.assertEquals(false, betweenDay);
  }
}
