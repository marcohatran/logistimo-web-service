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

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import com.logistimo.api.models.superdomains.LinkedDomainsModel;
import com.logistimo.api.models.superdomains.LinkedDomainsModel.LinkedDomainModel;
import com.logistimo.api.servlets.JsonRestServlet;
import com.logistimo.context.StaticApplicationContext;
import com.logistimo.domains.ObjectsToDomainModel;
import com.logistimo.domains.entity.IDomain;
import com.logistimo.domains.entity.IDomainLink;
import com.logistimo.domains.processor.ObjectsToDomainProcessor;
import com.logistimo.domains.service.DomainsService;
import com.logistimo.domains.service.impl.DomainsServiceImpl;
import com.logistimo.logger.XLog;
import com.logistimo.pagination.PageParams;
import com.logistimo.pagination.PagedExec;
import com.logistimo.pagination.QueryParams;
import com.logistimo.pagination.Results;
import com.logistimo.services.ServiceException;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.ResourceBundle;
import java.util.stream.Collectors;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

@SuppressWarnings("serial")
public class DomainsServlet extends JsonRestServlet {
  private static final XLog xLogger = XLog.getLog(DomainsServlet.class);

  private static final String ACTION_GETLINKEDDOMAINS = "getlinkeddomains";
  private static final String ACTION_ADDLINKEDDOMAINS = "addlinkeddomains";
  private static final String ACTION_REMOVELINKEDDOMAINS = "removelinkeddomains";
  private static final String ACTION_ADDOBJECTSTODOMAIN = "addobjectstodomain";
  private static final String ACTION_REMOVEOBJECTSFROMDOMAIN = "removeobjectsfromdomain";

  @Override
  protected void processGet(HttpServletRequest request, HttpServletResponse response,
                            ResourceBundle messages)
      throws ServletException, IOException,
      ServiceException {
    String action = request.getParameter("action");
    if (ACTION_GETLINKEDDOMAINS.equals(action)) {
      getLinkedDomains(request, response);
    } else if (ACTION_ADDLINKEDDOMAINS.equals(action)) {
      addLinkedDomains(response);
    } else if (ACTION_REMOVELINKEDDOMAINS.equals(action)) {
      removeLinkedDomains(request, response);
    } else {
      xLogger.severe("Invalid action: {0}", action);
    }
  }

  @Override
  protected void processPost(HttpServletRequest request,
                             HttpServletResponse response,
                             ResourceBundle messages)
      throws ServletException, IOException, ServiceException {
    String action = request.getParameter("action");
    if (ACTION_ADDLINKEDDOMAINS.equals(action)) {
      addLinkedDomains(response);
    } else if (ACTION_REMOVELINKEDDOMAINS.equals(action)) {
      removeLinkedDomains(request, response);
    } else if (ACTION_ADDOBJECTSTODOMAIN.equals(action)) {
      addRemoveObjectsToDomain(request,
          ObjectsToDomainModel.ACTION_ADD);
    } else if (ACTION_REMOVEOBJECTSFROMDOMAIN.equals(action)) {
      addRemoveObjectsToDomain(request,
          ObjectsToDomainModel.ACTION_REMOVE);
    } else {
      xLogger.severe("Invalid action: {0}", action);
    }
  }

  /**
   * Get linked domains (returns a JSON)
   */
  private void getLinkedDomains(HttpServletRequest req, HttpServletResponse resp)
      throws ServiceException, IOException {
    xLogger.fine("Entered getLinkedDomains");

    String domainIdStr = req.getParameter("domainid");
    String linkTypeStr = req.getParameter("type");
    String sizeStr = req.getParameter("size");
    String cursor = req.getParameter("cursor");

    if (domainIdStr == null || domainIdStr.isEmpty()) {
      throw new IllegalArgumentException("Invalid domain ID");
    }

    Long domainId;
    int linkType = IDomainLink.TYPE_CHILD;
    int size = PageParams.DEFAULT_SIZE;
    try {
      domainId = Long.valueOf(domainIdStr);
      if (linkTypeStr != null) {
        linkType = Integer.parseInt(linkTypeStr);
      }
      if (sizeStr != null) {
        size = Integer.parseInt(sizeStr);
      }
    } catch (Exception e) {
      throw new ServiceException(e.getMessage());
    }
    // Get the linked domains, and send back JSON
    try {
      DomainsService as = StaticApplicationContext.getBean(DomainsServiceImpl.class);
      Results results = as.getDomainLinks(domainId, linkType, new PageParams(cursor, size));
      IDomain d = as.getDomain(domainId);
      Gson gson = new GsonBuilder().create();
      sendJsonResponse(resp, 200,
          gson.toJson(new LinkedDomainsModel(domainId, d.getName(), results)));
    } catch (Exception e) {
      xLogger.severe("{0}: {1}", e.getClass().getName(), e.getMessage());
      sendJsonResponse(resp, 500, "");
    }

    xLogger.fine("Exiting getLinkedDomains");
  }

  private void addLinkedDomains(HttpServletResponse resp)
      throws ServiceException, IOException {
    xLogger.warn("Old UI don't support adding of linked domain. Please use new UI.");
    resp.setStatus(500); // error
  }

  private void removeLinkedDomains(HttpServletRequest req, HttpServletResponse resp)
      throws ServiceException, IOException {
    xLogger.fine("Entered removeLinkedDomains");
    String data = req.getParameter("data");
    try {
      // Get the JSON content
      Gson gson = new GsonBuilder().create();
      LinkedDomainsModel ldm = gson.fromJson(data, LinkedDomainsModel.class);
      int numLinks = ldm.size();
      if (numLinks == 0) {
        throw new ServiceException("No linked domains to remove");
      }
      // Get the domain links list
      List<String> domainLinkKeys = new ArrayList<>(numLinks);
      domainLinkKeys.addAll(ldm.getLinkedDomains().stream().map(LinkedDomainModel::getKey)
          .collect(Collectors.toList()));
      // Remove domain links
      DomainsService ds = StaticApplicationContext.getBean(DomainsServiceImpl.class);
      ds.deleteDomainLinks(domainLinkKeys);
      // Send positive response
      resp.setStatus(200); // done
    } catch (Exception e) {
      xLogger.severe("{0}: {1}", e.getClass().getName(), e.getMessage());
      resp.setStatus(500); // error
    }
    xLogger.fine("Exiting removeLinkedDomains");
  }

  // Add objects of any kind to a domain. Objects can be identified by specific IDs or it could be query based
  private void addRemoveObjectsToDomain(HttpServletRequest req,
                                        int action) throws ServletException, IOException {
    xLogger.fine("Entered addRemoveObjectsToDomain");
    String jsonData = req.getParameter("data");
    try {
      // Get Domains Service
      DomainsService ds = StaticApplicationContext.getBean(DomainsServiceImpl.class);
      // Get the input data object
      ObjectsToDomainModel aotdm = new Gson().fromJson(jsonData, ObjectsToDomainModel.class);
      List<Long> domainIds = aotdm.getDomainIds();
      String className = aotdm.getClassName();
      Class<?> clazz = Class.forName(className);
      // Check if objects are to be added by objects Ids
      if (aotdm.hasObjectIds()) {
        List<Object> objectIds = aotdm.getObjectIds();
        if (domainIds == null || domainIds.isEmpty() || clazz == null) {
          xLogger.severe("Either domainIds or Class was not specified");
        } else if (action == ObjectsToDomainModel.ACTION_ADD) {
          ds.addObjectsToDomains(objectIds, clazz, domainIds);
        } else if (action == ObjectsToDomainModel.ACTION_REMOVE) {
          ds.removeObjectsFromDomains(objectIds, clazz, domainIds);
        }
      } else { // check if query exists
        String queryStr = aotdm.getQueryString();
        Map<String, Object> params = aotdm.getQueryParams();
        Long sourceDomainId = aotdm.getSourceDomainId();
        if (queryStr == null || queryStr.isEmpty() || clazz == null || sourceDomainId == null) {
          xLogger.severe("Query string or class or source domain Id not specified");
        } else {
          // Send the clazz and the target domain Id
          ObjectsToDomainModel aotdParam =
              new ObjectsToDomainModel(action, domainIds, className, null);
          String jsonParam = new Gson().toJson(aotdParam);
          // Execute a add-to-domain processor on the paged query results
          PagedExec.exec(sourceDomainId, new QueryParams(queryStr, params),
              new PageParams(null, PageParams.DEFAULT_SIZE),
              ObjectsToDomainProcessor.class.getName(), jsonParam, null);
        }
      }
    } catch (Exception e) {
      xLogger.severe("{0}: {1}", e.getClass().getName(), e.getMessage());
    }
    // NOTE: Given this command is typically run as a task, there is no error response; else, the task can retry infinitely
    xLogger.fine("Exiting addRemoveObjectsToDomain");
  }
}
