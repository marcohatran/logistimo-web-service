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

package com.logistimo.utils;

import org.testng.Assert;
import org.testng.annotations.Test;

import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.Locale;
import java.util.TimeZone;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

/**
 * Created by mohan on 20/02/17.
 */
public class LocalDateUtilTest {

  @Test
  public void testGetDateTimePattern(){
    Assert.assertEquals(LocalDateUtil.getDateTimePattern(Locale.ENGLISH, true), "M/d/yy",
        "Date pattern does not match");
    Assert.assertEquals(LocalDateUtil.getDateTimePattern(Locale.ENGLISH, false), "M/d/yy h:mm a",
        "Date time pattern does not match");
  }

  @Test
  public void testZambiaDateShortFormat(){
    String date = LocalDateUtil.format(getDate(),new Locale("en","ZM"),"CAT", true);
    assertEquals("1/12/18",date);
  }

  @Test
  public void testZambiaDateTimeShortFormat(){
    String date = LocalDateUtil.format(getDate(),new Locale("en","ZM"),"CAT", false);
    assertEquals("1/12/18 12:00 AM",date);
  }

  @Test
  public void testDateTimeShortFormatWithoutLocale(){
    Calendar calendar = Calendar.getInstance();
    calendar.set(Calendar.YEAR, 2018);
    calendar.set(Calendar.MONTH, 11);
    calendar.set(Calendar.DAY_OF_MONTH,1);
    String date = LocalDateUtil.format(calendar.getTime(),null,null, true);
    assertNotNull(date);
  }

  private Date getDate() {
    Calendar calendar = GregorianCalendar.getInstance();
    calendar.setTimeZone(TimeZone.getTimeZone("CAT"));
    calendar.set(Calendar.YEAR, 2018);
    calendar.set(Calendar.MONTH, 11);
    calendar.set(Calendar.DAY_OF_MONTH,1);
    LocalDateUtil.resetTimeFields(calendar);
    return calendar.getTime();
  }

}
