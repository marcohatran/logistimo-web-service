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

package com.logistimo.exports.builders;

import com.logistimo.auth.utils.SecurityUtils;
import com.logistimo.config.models.DomainConfig;
import com.logistimo.domains.entity.Domain;
import com.logistimo.domains.entity.IDomain;
import com.logistimo.domains.service.impl.DomainsServiceImpl;
import com.logistimo.exports.model.RequestModel;
import com.logistimo.reports.plugins.internal.ExportModel;
import com.logistimo.security.SecureUserDetails;
import com.logistimo.services.ServiceException;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

import static org.junit.Assert.assertEquals;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.powermock.api.mockito.PowerMockito.mockStatic;
import static org.powermock.api.mockito.PowerMockito.whenNew;

/**
 * @author Mohan Raja
 */
@RunWith(PowerMockRunner.class)
@PrepareForTest({SecurityUtils.class, DomainConfig.class, ExportBuilder.class})
public class ExportBuilderTest {
  public static final String TEST_USER = "test";
  public static final String TEST_TIMEZONE = "Asia/Kolkata";
  public static final String TEST_DOMAIN = "test domain";
  private final ExportBuilder builder = new ExportBuilder();

  @Before
  public void setup() throws Exception {
    DomainsServiceImpl domainsService = mock(DomainsServiceImpl.class);
    builder.setDomainsService(domainsService);
    try {
      IDomain domain = new Domain();
      domain.setName(TEST_DOMAIN);
      when(domainsService.getDomain(anyLong())).thenReturn(domain);
    } catch (ServiceException ignored) {
    }
    DomainConfig domainConfig = new DomainConfig();
    domainConfig.setTimezone(TEST_TIMEZONE);

    mockStatic(SecurityUtils.class);
    mockStatic(DomainConfig.class);

    Calendar cal = new GregorianCalendar();
    cal.set(2018,Calendar.JANUARY,1,0,0,0);

    Date date = new Date();
    date.setTime(cal.getTimeInMillis());
    whenNew(Date.class).withNoArguments().thenReturn(date);

    SecureUserDetails userDetails = getSecureUserDetails();
    PowerMockito.when(SecurityUtils.getUserDetails()).thenReturn(userDetails);
    PowerMockito.when(DomainConfig.getInstance(anyLong())).thenReturn(domainConfig);

  }

  private SecureUserDetails getSecureUserDetails() {
    SecureUserDetails userDetails = new SecureUserDetails();
    userDetails.setCurrentDomainId(123456L);
    userDetails.setUsername(TEST_USER);
    userDetails.setLocale(Locale.ENGLISH);
    userDetails.setTimezone(TEST_TIMEZONE);
    return userDetails;
  }

  @Test
  public void testBuildExportModel() throws Exception {
    RequestModel model = getRequestModel();
    ExportModel exportModel = builder.buildExportModel(model);
    assertEquals(TEST_USER, exportModel.userId);
    assertEquals(TEST_TIMEZONE, exportModel.timezone);
    assertEquals(model.getTemplateId(), exportModel.templateId);
    assertEquals("123456",exportModel.filters.get("TOKEN_DID"));
    assertEquals("2018-01-01 00:00:00.000",exportModel.filters.get("TOKEN_START_TIME"));
    assertEquals("2018-01-02 00:00:00.000",exportModel.filters.get("TOKEN_END_TIME"));
    assertEquals("DEFAULT", exportModel.additionalData.get("typeId"));
    assertEquals(TEST_DOMAIN, exportModel.additionalData.get("domainName"));
    assertEquals(TEST_TIMEZONE, exportModel.additionalData.get("domainTimezone"));
    assertEquals("2018-01-01 00:00:00", exportModel.additionalData.get("exportTime"));
    assertEquals(model.getTitles().get("name"), exportModel.titles.get("name"));

  }

  private RequestModel getRequestModel() {
    RequestModel model = new RequestModel();
    model.setDomainId("123456");
    model.setEntityId("");
    model.setMtag("");
    model.setMtag("");
    model.setKtag("");
    model.setFromDate("2018-01-01");
    model.setEndDate("2018-01-02");
    model.setMaterialId("");
    model.setBatchId("");
    model.setReason("");
    model.setLinkedKioskId("");
    model.setTransactionType("");
    model.setAtd("");
    model.setFirstName("");
    model.setMobileNumber("");
    model.setRole("");
    model.setUserActive("");
    model.setAppVersion("");
    model.setShowNeverLoggedInUsers("");
    model.setUserTag("");
    model.setMaterialName("");
    model.setEntityName("");
    Map<String,String> titles = new HashMap<>(1,1);
    titles.put("name","test name");
    model.setTitles(titles);
    return model;
  }
}