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

package com.logistimo.inventory.service.impl;

import org.junit.Test;

import java.text.ParseException;
import java.text.SimpleDateFormat;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/**
 * Created by charan on 15/12/17.
 */
public class InventoryManagementServiceImplTest {

  @Test
  public void testIsATDValid() throws ParseException {
    InventoryManagementServiceImpl ims = new InventoryManagementServiceImpl();
    SimpleDateFormat df = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSZ");
    assertFalse("ATD should be considered valid, but not",
        ims.isAtdNotValid("Asia/Kolkata", df.parse("2017-12-16T01:30:00.000+0000"),
            df.parse("2017-12-15T19:30:00.000+0000")));
  }

  @Test
  public void testIsATDNotValid() throws ParseException {
    InventoryManagementServiceImpl ims = new InventoryManagementServiceImpl();
    SimpleDateFormat df = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSZ");
    assertTrue("ATD should be considered in future, but not",
        ims.isAtdNotValid("Asia/Kolkata", df.parse("2017-12-16T01:30:00.000+0000"),
            df.parse("2017-12-15T17:30:00.000+0000")));
  }

}