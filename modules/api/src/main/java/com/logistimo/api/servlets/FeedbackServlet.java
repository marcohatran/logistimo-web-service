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

package com.logistimo.api.servlets;

import com.logistimo.api.action.SubmitFeedbackAction;
import com.logistimo.api.models.FeedbackModel;
import com.logistimo.context.StaticApplicationContext;
import com.logistimo.exception.InvalidServiceException;
import com.logistimo.logger.XLog;
import com.logistimo.proto.RestConstantsZ;
import com.logistimo.services.Resources;

import java.util.Locale;
import java.util.ResourceBundle;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 * Created by mohansrinivas on 1/25/16.
 */
public class FeedbackServlet extends HttpServlet {

  private static final XLog _LOGGER = XLog.getLog(FeedbackServlet.class);

  @Override
  public void doPost(HttpServletRequest request, HttpServletResponse response)
      throws ServletException, java.io.IOException {
    ResourceBundle bundle = Resources.get().getBundle("Messages", Locale.ENGLISH);
    String strUserId = request.getParameter(RestConstantsZ.USER_ID);
    String data = request.getParameter("text");
    String title = request.getParameter("title");
    //changes done for backward compatibility
    FeedbackModel model = new FeedbackModel();
    model.setUserId(strUserId);
    model.setTitle(title);
    model.setText(data);
    model.setApp(bundle.getString("mob"));
    SubmitFeedbackAction action = StaticApplicationContext.getBean(SubmitFeedbackAction.class);
    try {
      action.invoke(model);
      _LOGGER
          .info("Feedback Submitted for older apps with content {0}",model);
    } catch (Exception e) {
      _LOGGER.warn("Failed to submit the feedback with error {0}",e.getMessage(), e);
      throw new InvalidServiceException("Failed to submit the feedback");
    }
  }

  public void doGet(HttpServletRequest request, HttpServletResponse response)
      throws ServletException, java.io.IOException {
    throw new ServletException(
        "GET method used with " + getClass().getName() + ": POST method required.");
  }
}
