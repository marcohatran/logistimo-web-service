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

/**
 *
 */
package com.logistimo.api.servlets;

import com.logistimo.auth.utils.SecurityUtils;
import com.logistimo.dao.JDOUtils;
import com.logistimo.entity.IBBoard;
import com.logistimo.events.handlers.BBHandler;
import com.logistimo.logger.XLog;
import com.logistimo.security.SecureUserDetails;
import com.logistimo.services.ServiceException;

import java.io.IOException;
import java.net.URLDecoder;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.ResourceBundle;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 * @author Arun
 */
public class BBoardMgrServlet extends JsonRestServlet {

  private static final long serialVersionUID = 1L;
  // Logger
  private static final XLog xLogger = XLog.getLog(BBoardMgrServlet.class);
  // Actions
  private static final String ACTION_POSTMESSAGE = "post";
  private static final String ACTION_REMOVE = "rm";

  // Remove messages from a bulletin board
  private static void removeMessages(HttpServletRequest request) {
    xLogger.fine("Entered removeMessages");
    // Get the item Ids
    String itemIdsCSV = request.getParameter("itemids");
    if (itemIdsCSV == null || itemIdsCSV.isEmpty()) {
      xLogger.severe("Nothing to remove from bulletin board");
      return;
    }
    List<Long> itemIds = new ArrayList<Long>();
    String[] itemIdsArray = itemIdsCSV.split(",");
    for (int i = 0; i < itemIdsArray.length; i++) {
      itemIds.add(Long.valueOf(itemIdsArray[i]));
    }
    // Remove
    BBHandler.remove(itemIds);
    xLogger.fine("Exiting removeMessages");
  }

  @Override
  protected void processGet(HttpServletRequest request, HttpServletResponse response,
                            ResourceBundle messages) throws ServletException, IOException,
      ServiceException {
    processPost(request, response, messages);
  }

  @Override
  protected void processPost(HttpServletRequest request, HttpServletResponse response,
                             ResourceBundle messages)
      throws ServletException, IOException, ServiceException {
    xLogger.fine("Entered processPost");
    String action = request.getParameter("action");
    if (ACTION_POSTMESSAGE.equals(action)) {
      postMessage(request, response);
    } else if (ACTION_REMOVE.equals(action)) {
      removeMessages(request);
    } else {
      xLogger.severe("Invalid action: {0}", action);
    }
    xLogger.fine("Exiting processPost");
  }

  // Post message to BBoard
  private void postMessage(HttpServletRequest request, HttpServletResponse response)
      throws IOException {
    xLogger.fine("Entered postMessage");
    String message = request.getParameter("message");
    if (message != null && !message.isEmpty()) {
      message = URLDecoder.decode(message, "UTF-8");
    } else {
      xLogger.severe("No message to post to Bulletin Board");
      sendJsonResponse(response, 200, "{\"st\": \"0\", \"ms\": \"No message to post\" }");
    }
    String jsonResp;
    try {
      // Get the user Id
      SecureUserDetails sUser = SecurityUtils.getUserDetails();
      xLogger.fine("sUser: {0}", sUser);
      String userId = sUser.getUsername();
      Long domainId = SecurityUtils.getCurrentDomainId();
      // Create the BBoard message
      IBBoard bb = JDOUtils.createInstance(IBBoard.class);
      bb.setDomainId(domainId);
      bb.setMessage(message);
      bb.setTimestamp(new Date());
      bb.setType(IBBoard.TYPE_POST);
      bb.setUserId(userId);
      BBHandler.add(bb);
      // Get back to user
      jsonResp = "{ \"st\": \"1\" }"; // success
    } catch (Exception e) {
      jsonResp = "{\"st\": \"0\", \"ms\": \"" + e.getMessage() + "\" }";
    }
    xLogger.fine("Exiting postMessage: {0}", jsonResp);
    sendJsonResponse(response, 200, jsonResp);
  }
}
