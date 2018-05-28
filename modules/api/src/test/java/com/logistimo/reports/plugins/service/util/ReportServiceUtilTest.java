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

package com.logistimo.reports.plugins.service.util;

import com.logistimo.entities.entity.IKiosk;
import com.logistimo.entities.entity.Kiosk;
import com.logistimo.reports.models.ReportDataModel;
import com.logistimo.users.entity.IUserAccount;
import com.logistimo.users.entity.UserAccount;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;


/**
 * Created by vani on 28/12/17.
 */
public class ReportServiceUtilTest {
  private static final String ZERO_FLOAT_VALUE = "0.0";
  private static final String ZERO_LONG_VALUE = "0";
  private ReportServiceUtil reportServiceUtil = new ReportServiceUtil();

  @Test
  public void testGetAverageTimeDataModel() throws Exception {
    ReportDataModel reportDataModel = reportServiceUtil.getAverageTimeDataModel(1000000l, 10l);
    assertEquals("0.0011574074",reportDataModel.value);
    assertEquals("0.011574074", reportDataModel.num);
    assertEquals("10", reportDataModel.den);
    reportDataModel = reportServiceUtil.getAverageTimeDataModel(0l, 10l);
    assertEquals(ZERO_FLOAT_VALUE,reportDataModel.value);
    assertEquals(ZERO_FLOAT_VALUE,reportDataModel.num);
    assertEquals("10", reportDataModel.den);
    reportDataModel = reportServiceUtil.getAverageTimeDataModel(1000000l, 0l);
    assertEquals(ZERO_LONG_VALUE,reportDataModel.value);
    assertNull(reportDataModel.num);
    assertNull(reportDataModel.den);
    reportDataModel = reportServiceUtil.getAverageTimeDataModel(0l, 0l);
    assertEquals(ZERO_LONG_VALUE,reportDataModel.value);
    assertNull(reportDataModel.num);
    assertNull(reportDataModel.den);
  }

  @Test
  public void testGetEntityMetaInfo() throws Exception {
    IKiosk kiosk = new Kiosk();
    kiosk.setKioskId(1l);
    kiosk.setName("AishBagh UCHC - Demo");
    kiosk.setState("Uttar Pradesh");
    kiosk.setDistrict("Lucknow");
    kiosk.setCity("Lucknow");
    String entityMetaInfo = reportServiceUtil.getEntityMetaInfo(kiosk);
    assertEquals("AishBagh UCHC - Demo|1|Lucknow||Lucknow|Uttar Pradesh", entityMetaInfo);
  }

  @Test
  public void testGetUserMetaInfo() throws Exception {
    IUserAccount user = new UserAccount();
    user.setUserId("aditya");
    user.setFirstName("Adityanath");
    user.setLastName("Yogi");
    user.setState("Uttar Pradesh");
    user.setDistrict("Lucknow");
    user.setCity("Lucknow");
    String userDetails = reportServiceUtil.getUserMetaInfo(user);
    assertEquals("Adityanath Yogi|aditya|Lucknow||Lucknow|Uttar Pradesh", userDetails);
  }
}