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

package com.logistimo.api.controllers;

import com.logistimo.AppFactory;
import com.logistimo.api.builders.DomainBuilder;
import com.logistimo.api.models.superdomains.DomainModel;
import com.logistimo.api.request.AddDomainLinksRequestObj;
import com.logistimo.auth.SecurityConstants;
import com.logistimo.auth.utils.SecurityUtils;
import com.logistimo.config.models.DomainConfig;
import com.logistimo.constants.CharacterConstants;
import com.logistimo.dao.JDOUtils;
import com.logistimo.domains.entity.IDomain;
import com.logistimo.domains.entity.IDomainLink;
import com.logistimo.domains.entity.IDomainPermission;
import com.logistimo.domains.service.DomainsService;
import com.logistimo.entities.utils.DomainLinkUpdater;
import com.logistimo.exception.InvalidDataException;
import com.logistimo.exception.InvalidServiceException;
import com.logistimo.exception.SystemException;
import com.logistimo.exception.TaskSchedulingException;
import com.logistimo.logger.XLog;
import com.logistimo.models.superdomains.DomainSuggestionModel;
import com.logistimo.pagination.Navigator;
import com.logistimo.pagination.PageParams;
import com.logistimo.security.SecureUserDetails;
import com.logistimo.services.ObjectNotFoundException;
import com.logistimo.services.Resources;
import com.logistimo.services.ServiceException;
import com.logistimo.services.impl.PMF;
import com.logistimo.services.taskqueue.ITaskService;
import com.logistimo.users.entity.IUserAccount;
import com.logistimo.users.service.UsersService;
import com.logistimo.utils.LocalDateUtil;
import com.logistimo.utils.MsgUtil;
import com.logistimo.utils.QueryUtil;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

import java.text.ParseException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.ResourceBundle;

import javax.jdo.PersistenceManager;
import javax.jdo.Query;
import javax.servlet.http.HttpServletRequest;

/**
 * @author naveensnair, Mohan Raja
 */
@Controller
@RequestMapping("/linked/domain")
public class LinkedDomainController {
  private static final XLog xLogger = XLog.getLog(DomainConfigController.class);
  private static final String DOMAIN_LINK_UPDATE_TASK_URL = "/s2/api/linked/domain/domainupdate";

  private DomainBuilder domainBuilder;
  private DomainsService domainsService;
  private UsersService usersService;

  @Autowired
  public void setDomainBuilder(DomainBuilder domainBuilder) {
    this.domainBuilder = domainBuilder;
  }

  @Autowired
  public void setDomainsService(DomainsService domainsService) {
    this.domainsService = domainsService;
  }

  @Autowired
  public void setUsersService(UsersService usersService) {
    this.usersService = usersService;
  }

  @SuppressWarnings("unchecked")
  @RequestMapping(value = "/add", method = RequestMethod.POST)
  public
  @ResponseBody
  String addChildrenToDomain(@RequestBody AddDomainLinksRequestObj model) {
    Long domainId;
    if (model.domainId == null) {
      domainId = SecurityUtils.getCurrentDomainId();
    } else {
      domainId = model.domainId;
    }
    try {
      List<IDomainLink>
          domainLinkList =
          domainBuilder.buildDomainLink(model.domainModel.ldl, domainId);
      IDomainPermission
          permission =
          domainBuilder.buildDomainPermission(model.domainModel, domainId);
      domainsService.addDomainLinks(domainLinkList, permission);
      StringBuilder sb = new StringBuilder();
      for (DomainSuggestionModel domainSuggestionModel : model.domainModel.ldl) {
        sb.append(domainSuggestionModel.id).append(CharacterConstants.COMMA);
      }
      if (sb.length() > 0) {
        sb.setLength(sb.length() - 1);
      }
      Map<String, String> params = new HashMap<>();
      params.put("domainId", String.valueOf(domainId));
      params.put("childDomainIds", sb.toString());
      params.put("type", "add");
      AppFactory.get().getTaskService()
          .schedule(ITaskService.QUEUE_DEFAULT, DOMAIN_LINK_UPDATE_TASK_URL, params,
              ITaskService.METHOD_POST);
      return "Adding " + model.domainModel.ldl.size()
          + " domains as child is scheduled successfully. " + MsgUtil.newLine() + MsgUtil.newLine()
          +
          "NOTE: It will take some time to complete.";
    } catch (Exception e) {
      xLogger.severe("Error in scheduling adding of domains:", e);
      throw new InvalidServiceException("Unable to add the domains to the current domain");
    }
  }

  @RequestMapping(value = "/domainupdate", method = RequestMethod.POST)
  public
  @ResponseBody
  void updateDomainLinkRelatedObjects(@RequestParam Long domainId,
                                      @RequestParam String childDomainIds,
                                      @RequestParam String type) {
    try {
      DomainLinkUpdater.updateDomainLinks(domainId, childDomainIds, "add".equals(type));
    } catch (ServiceException e) {
      xLogger.severe("Error while updating domain links related object", e);
    }
  }

  @RequestMapping(value = "/", method = RequestMethod.GET)
  public
  @ResponseBody
  List<DomainSuggestionModel> getChildren(@RequestParam(required = false) Integer depth,
                                          @RequestParam(required = false) Long domainId) {
    SecureUserDetails sUser = SecurityUtils.getUserDetails();
    if (domainId == null) {
      domainId = sUser.getCurrentDomainId();
    }
    Long currentDomain = sUser.getCurrentDomainId();
    List<IDomainLink> childDomains;
    try {
      if (domainId != null) {
        childDomains =
            domainsService.getDomainLinks(domainId, IDomainLink.TYPE_CHILD, depth == null ? 0 : depth);
        if (childDomains != null && childDomains.size() > 0) {
          return domainBuilder.buildLinkedDomainModelList(childDomains, currentDomain);
        }
      }
    } catch (ServiceException e) {
      xLogger.severe("Unable to fetch the child domains to the current doamin", e);
      throw new InvalidServiceException("Unable to fetch the child domains to the current doamin");
    }
    return null;
  }

  @RequestMapping(value = "/permission", method = RequestMethod.GET)
  public
  @ResponseBody
  DomainModel getDomainPermission(
      @RequestParam(required = false, defaultValue = "false") boolean action,
      @RequestParam(required = false) Long domainId) {
    SecureUserDetails sUser = SecurityUtils.getUserDetails();
    boolean iMan = SecurityConstants.ROLE_SERVICEMANAGER.equals(sUser.getRole());
    Long userDomainId = sUser.getDomainId();
    if (domainId == null) {
      domainId = sUser.getCurrentDomainId();
    }
    try {
      IDomainPermission userDomainPermission = null;
      IDomain domain = domainsService.getDomainPermission(domainId);
      IDomainPermission permission = domainsService.getLinkedDomainPermission(domainId);
      IUserAccount account = usersService.getUserAccount(sUser.getUsername());
      if (SecurityConstants.ROLE_DOMAINOWNER.equals(sUser.getRole())) {
        userDomainPermission = domainsService.getLinkedDomainPermission(userDomainId);
      }
      boolean viewPermission = IUserAccount.PERMISSION_VIEW.equals(account.getPermission());
      boolean assetPermission = IUserAccount.PERMISSION_ASSET.equals(account.getPermission());
      DomainConfig dc = DomainConfig.getInstance(domainId);
      return domainBuilder
          .buildDomain(domain, userDomainPermission, permission, dc, action, viewPermission,
              assetPermission, iMan);
    } catch (ObjectNotFoundException e) {
      xLogger.severe("Unable to fetch the user details for {0}", sUser.getUsername(), e);
      throw new InvalidServiceException("Unable to fetch user details for " + sUser.getUsername());
    } catch (SystemException e) {
      xLogger.severe("Unable to fetch the domain permission for current domain", e);
      throw new InvalidServiceException("Unable to fetch the domain permission for current domain");
    }
  }

  @RequestMapping(value = "/delete", method = RequestMethod.GET)
  public
  @ResponseBody
  String deleteDomainLinks(@RequestParam Long domainId) {
    if (domainId != null) {
      IDomainLink parentDomainLink;
      try {
        parentDomainLink = domainsService.getDomainLinks(domainId, IDomainLink.TYPE_PARENT, 0).get(0);
        List<IDomainLink> childLinksOfParent =
            domainsService.getDomainLinks(parentDomainLink.getLinkedDomainId(), IDomainLink.TYPE_CHILD, 0);
        boolean hasChild = childLinksOfParent.size() > 1;
        domainsService.deleteDomainLink(parentDomainLink, hasChild);
        Map<String, String> params = new HashMap<>();
        params.put("domainId", String.valueOf(parentDomainLink.getLinkedDomainId()));
        params.put("childDomainIds", String.valueOf(domainId));
        params.put("type", "remove");
        AppFactory.get().getTaskService()
            .schedule(ITaskService.QUEUE_DEFAULT, DOMAIN_LINK_UPDATE_TASK_URL, params,
                ITaskService.METHOD_POST);
        return "Removing domain from parent is scheduled successfully. " + MsgUtil.newLine()
            + MsgUtil.newLine() +
            "NOTE: It will take some time to complete.";
      } catch (Exception e) {
        xLogger.severe("Unable to remove the domain link", e);
        throw new InvalidServiceException("Unable to remove domain link");
      }
    }
    return "success";
  }

  @RequestMapping(value = "/push", method = RequestMethod.GET)
  public
  @ResponseBody
  String pushConfiguration() {
    Long domainId = SecurityUtils.getCurrentDomainId();
    try {
      List<IDomainLink> domainLinkList = domainsService.getDomainLinks(domainId, IDomainLink.TYPE_CHILD, -1);
      if (domainLinkList != null && domainLinkList.size() > 0) {
        for (IDomainLink iDomainLink : domainLinkList) {
          domainsService.copyConfiguration(domainId, iDomainLink.getLinkedDomainId());
        }
      }
    } catch (ServiceException | TaskSchedulingException e) {
      xLogger.severe("Unable to push configuration to the selected domains", e);
      throw new InvalidServiceException("Unable to push configuration to the selected domains");
    }
    return "Pushing  configuration to child domains is scheduled successfully. " + MsgUtil.newLine()
        + MsgUtil.newLine() +
        "NOTE: The configuration in each child domain will be overwritten by the configuration of this domain. "
        + MsgUtil.newLine() + MsgUtil.newLine() +
        "Please review and edit the configurations in the child domains, as appropriate to that domain.";
  }

  @RequestMapping(value = "/suggestions", method = RequestMethod.GET)
  public
  @ResponseBody
  List<IDomain> suggestLinkedDomains() {
    SecureUserDetails sUser = SecurityUtils.getUserDetails();
    Long domainId = sUser.getDomainId();
    try {
      List<IDomain> suggestions = new ArrayList<>();
      IUserAccount ua = usersService.getUserAccount(sUser.getUsername());
      List<IDomainLink> fullDomainLinks = new ArrayList<>();
      for (Long dId : ua.getAccessibleDomainIds()) {
        List<IDomainLink> childDomains = domainsService.getAllDomainLinks(dId, IDomainLink.TYPE_CHILD);
        if (childDomains != null && childDomains.size() > 0) {
          fullDomainLinks.addAll(childDomains);
        }
        if (!domainId.equals(dId)) {
          suggestions.add(domainsService.getDomain(dId));
        }
      }
      IDomain currentDomain = domainsService.getDomain(domainId);
      List<IDomain> sugg =
          domainBuilder.buildDomainSwitchSuggestions(fullDomainLinks, currentDomain);
      if (sugg != null) {
        suggestions.addAll(sugg);
      }
      return suggestions;
    } catch (ServiceException | ObjectNotFoundException e) {
      throw new InvalidServiceException("Unable to fetch domain details for the domain");
    }
  }

  @RequestMapping(value = "/parents", method = RequestMethod.GET)
  public
  @ResponseBody
  List<IDomain> getParents(@RequestParam(required = false) Long domainId,
                           @RequestParam Integer domainType) {
    List<IDomain> domains;
    List<IDomainLink> linkedDomains;
    if (domainId == null) {
      domainId = SecurityUtils.getCurrentDomainId();
    }
    try {
      linkedDomains = domainsService.getAllDomainLinks(domainId, domainType);
      IDomain currentDomain = domainsService.getDomain(domainId);
      domains = domainBuilder.buildParentList(linkedDomains, currentDomain);
      return domains;
    } catch (ServiceException | ObjectNotFoundException e) {
      throw new InvalidServiceException("Unable to fetch domain details for the domain");
    }
  }

  @RequestMapping(value = "/domainlinks/{domainId}", method = RequestMethod.GET)
  public
  @ResponseBody
  List<IDomain> fetchLinkedDomainsById(@PathVariable Long domainId, @RequestParam Integer type) {
    List<IDomain> domains;
    List<IDomainLink> linkedDomains;
    try {
      linkedDomains = domainsService.getAllDomainLinks(domainId, type);
      domains = domainBuilder.buildDomainSwitchSuggestions(linkedDomains, null);
      return domains;
    } catch (ServiceException e) {
      throw new InvalidServiceException("Unable to fetch domain details for the domain");
    }
  }


  @RequestMapping(value = "/updatepermission", method = RequestMethod.POST)
  public
  @ResponseBody
  String updateChildDomainPermissions(@RequestBody DomainModel model) {
    if (model == null) {
      throw new InvalidDataException("No domain data found to update.");
    }
    SecureUserDetails user = SecurityUtils.getUserDetails();
    String username = user.getUsername();
    Long domainId = model.dId;
    IDomainPermission permission = domainBuilder.buildDomainPermission(model, domainId);
    domainsService.updateDomainPermission(permission, model.dId, username);
    return MsgUtil.bold(model.name) + " domain permissions updated successfully.";
  }

  @RequestMapping(value = "/domain", method = RequestMethod.GET)
  public
  @ResponseBody
  IDomain fetchDomainById(@RequestParam Long domainId) {
    try {
      if (domainId == null) {
        throw new ServiceException("Domain is not available");
      }
      SecureUserDetails user = SecurityUtils.getUserDetails();
      IDomain domain = domainsService.getDomain(domainId);
      String dateStr =
          LocalDateUtil.format(domain.getCreatedOn(), user.getLocale(), user.getTimezone());
      domain.setCreatedOn(LocalDateUtil.parse(dateStr, user.getLocale(), user.getTimezone()));
      return domain;
    } catch (ServiceException | ObjectNotFoundException | ParseException e) {
      xLogger.severe("unable to get the get the details for domainId", e);
      throw new InvalidServiceException("unable to get the get the details for domainId");
    }
  }

  @RequestMapping(value = "/unlinked", method = RequestMethod.GET)
  public
  @ResponseBody
  List<DomainSuggestionModel> getDomains(@RequestParam(required = false) String q,
                                         @RequestParam(required = false) Long reqDomainId,
                                         HttpServletRequest request) {
    PersistenceManager pm = PMF.get().getPersistenceManager();
    Query query = pm.newQuery(JDOUtils.getImplClass(IDomain.class));
    Map<String, Object> params = new HashMap<>();
    Navigator navigator;
    String filter = "hasParent == false";
    if (q != null && !q.isEmpty()) {
      filter += " && nNm.startsWith(txtParam)";
      query.declareParameters("String txtParam");
      params.put("txtParam", q);
    } else {
      query.setOrdering("nNm asc");
    }
    query.setFilter(filter);
    List<IDomain> domains = null;
    List<IDomain> finalDomainList = null;
    List<IDomainLink> domainList = null;
    SecureUserDetails sUser = SecurityUtils.getUserDetails();

    Long currentDomain = reqDomainId;
    if (currentDomain == null) {
      currentDomain = sUser.getCurrentDomainId();
    }

    Locale locale = sUser.getLocale();
    ResourceBundle backendMessages = Resources.getBundle(locale);
    navigator =
        new Navigator(request.getSession(), "DomainConfigController.getDomains", 0, 10, "dummy", 0);
    PageParams pageParams = new PageParams(navigator.getCursor(0), 0, 10);
    QueryUtil.setPageParams(query, pageParams);
    try {
      if (q != null && !q.isEmpty()) {
        domains = (List<IDomain>) query.executeWithMap(params);
      } else {
        domains = (List<IDomain>) query.execute();
      }
      domains = (List<IDomain>) pm.detachCopyAll(domains);
      if (domains != null) {
        domains.size(); // to retrieve the results before closing the PM
      }
    } catch (Exception e) {
      xLogger.severe("Error in fetching list of domains", e);
      throw new InvalidServiceException(backendMessages.getString("domains.fetch.error"));
    } finally {
      try {
        query.closeAll();
      } catch (Exception ignored) {
        xLogger.warn("Exception while closing query", ignored);
      }
      pm.close();
    }
    if (domains != null) {
      Long linkedDomainId = null;
      finalDomainList = new ArrayList<>(domains.size());
      try {
        domainList = domainsService.getDomainLinks(currentDomain, 1, -1);
      } catch (Exception e) {
        xLogger.severe("unable to get the get the details for domainId", e);
      }
      if (domainList != null && domainList.size() > 0) {
        IDomainLink domainLink = domainList.get(domainList.size() - 1);
        linkedDomainId = domainLink.getLinkedDomainId();
      }
      for (IDomain d : domains) {
        if (!d.getId().equals(currentDomain) && !d.getId().equals(linkedDomainId)) {
          finalDomainList.add(d);
        }

      }
    }
    return domainBuilder.buildChildSuggestion(finalDomainList);
  }
}
