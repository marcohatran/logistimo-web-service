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

package com.logistimo.api.servlets.mobile;

import com.logistimo.exception.LogiException;
import com.logistimo.services.Resources;
import com.logistimo.services.ServiceException;

import org.junit.Test;

import java.util.Locale;
import java.util.Optional;
import java.util.ResourceBundle;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/**
 * Created by charan on 30/11/17.
 */
public class OrderServletTest {

  @Test
  public void getErrorCodeShouldConvertM002ToO010() {
    OrderServlet orderServlet = new OrderServlet();
    ServiceException e = new ServiceException("M002", new Object[]{});
    Optional<String> message = orderServlet.getErrorMessage(Locale.getDefault(), e);
    assertTrue(message.isPresent());
    ResourceBundle
        messages =
        Resources.getBundle("errors", Locale.getDefault());
    assertEquals("message should have been converted to O010", message.get(),
        messages.getString("O010"));
  }

  @Test
  public void getErrorCodeShouldNotConvertErrorCode() {
    OrderServlet orderServlet = new OrderServlet();
    ServiceException e = new ServiceException("M003", new Object[]{});
    Optional<String> message = orderServlet.getErrorMessage(Locale.getDefault(), e);
    assertTrue(message.isPresent());
    ResourceBundle
        messages =
        Resources.getBundle("errors", Locale.getDefault());
    assertEquals("message should not have been converted", message.get(),
        messages.getString("M003"));
  }

  @Test
  public void getErrorCodeShouldHandleNestedLogiExceptionWithM002() {
    OrderServlet orderServlet = new OrderServlet();
    LogiException le = new LogiException("M002", new Object[]{});
    ServiceException e = new ServiceException(le);
    Optional<String> message = orderServlet.getErrorMessage(Locale.getDefault(), e);
    assertTrue(message.isPresent());
    ResourceBundle
        messages =
        Resources.getBundle("errors", Locale.getDefault());
    assertEquals("message should have been converted to O010", message.get(),
        messages.getString("O010"));
  }

  @Test
  public void getErrorCodeShouldHandleNestedLogiException() {
    OrderServlet orderServlet = new OrderServlet();
    LogiException le = new LogiException("M003", new Object[]{});
    ServiceException e = new ServiceException(le);
    Optional<String> message = orderServlet.getErrorMessage(Locale.getDefault(), e);
    assertTrue(message.isPresent());
    ResourceBundle
        messages =
        Resources.getBundle("errors", Locale.getDefault());
    assertEquals("message should not have been converted", message.get(),
        messages.getString("M003"));
  }

  @Test
  public void getErrorCodeShouldHandleNoCodes() {
    OrderServlet orderServlet = new OrderServlet();
    ServiceException e = new ServiceException("This is dummy");
    Optional<String> message = orderServlet.getErrorMessage(Locale.getDefault(), e);
    assertTrue(!message.isPresent());
  }

}