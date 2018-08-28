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

package com.logistimo.api.controllers;

import com.logistimo.api.action.SubmitFeedbackAction;
import com.logistimo.api.models.FeedbackModel;
import com.logistimo.config.models.ConfigurationException;
import com.logistimo.logger.XLog;
import com.logistimo.services.ServiceException;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;

import javax.servlet.http.HttpServletRequest;

/**
 * Created by yuvaraj on 02/07/18.
 */
@Controller
@RequestMapping("/feedback")
public class FeedbackController {

  private static final XLog xLogger = XLog.getLog(FeedbackController.class);

  private SubmitFeedbackAction submitFeedbackAction;

  @Autowired
  public void setSubmitFeedbackAction(SubmitFeedbackAction submitFeedbackAction) {
    this.submitFeedbackAction = submitFeedbackAction;
  }

  @RequestMapping(value = "", method = RequestMethod.POST)
  public
  @ResponseBody
  void create(@RequestBody FeedbackModel model, HttpServletRequest request) throws ServiceException, ConfigurationException {
    String app = request.getHeader("X-app-name");
    String appVer = request.getHeader("X-app-ver");
    model.setApp(app);
    model.setAppVersion(appVer);
    xLogger.fine("Feedback request with data {0}",model);
    submitFeedbackAction.invoke(model);
  }

}
