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

import com.logistimo.accounting.entity.IAccount;
import com.logistimo.accounting.service.IAccountingService;
import com.logistimo.api.builders.AccountBuilder;
import com.logistimo.api.models.AccountModel;
import com.logistimo.auth.utils.SecurityUtils;
import com.logistimo.entities.auth.EntityAuthoriser;
import com.logistimo.exception.ForbiddenAccessException;
import com.logistimo.exception.InvalidServiceException;
import com.logistimo.logger.XLog;
import com.logistimo.pagination.Navigator;
import com.logistimo.pagination.PageParams;
import com.logistimo.pagination.Results;
import com.logistimo.security.SecureUserDetails;
import com.logistimo.services.Resources;
import com.logistimo.services.ServiceException;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

import java.util.List;
import java.util.Locale;
import java.util.ResourceBundle;

import javax.servlet.http.HttpServletRequest;

/**
 * Created by mohan raja on 03/12/14
 */
@Controller
@RequestMapping("/accounts")
public class AccountsController {
  private static final XLog xLogger = XLog.getLog(AccountsController.class);

  private AccountBuilder accountBuilder;
  private IAccountingService accountingService;

  @Autowired
  public void setAccountBuilder(AccountBuilder accountBuilder) {
    this.accountBuilder = accountBuilder;
  }

  @Autowired
  public void setAccountingService(IAccountingService accountingService) {
    this.accountingService = accountingService;
  }

  @RequestMapping("/")
  public
  @ResponseBody
  Results getAccounts(
      @RequestParam String kioskId, @RequestParam String type, @RequestParam String yr,
      @RequestParam String sb,
      @RequestParam(defaultValue = PageParams.DEFAULT_OFFSET_STR) int offset,
      @RequestParam(defaultValue = PageParams.DEFAULT_SIZE_STR) int size,
      HttpServletRequest request) {
    SecureUserDetails sUser = SecurityUtils.getUserDetails();
    Locale locale = sUser.getLocale();
    ResourceBundle backendMessages;
    Long domainId = sUser.getCurrentDomainId();
    Navigator
        navigator =
        new Navigator(request.getSession(), "AccountsController.getAccounts", offset, size,
            "act" + type, 0);
    PageParams pageParams = new PageParams(navigator.getCursor(offset), offset, size);
    backendMessages = Resources.get().getBundle("BackendMessages", locale);
    try {
      Results results;
      if (EntityAuthoriser.authoriseEntity(Long.valueOf(kioskId))) {
        if (IAccount.RECEIVABLE.equals(type)) {
          results =
              accountingService.getAccounts(Long.valueOf(kioskId), null, Integer.valueOf(yr), sb, pageParams);
        } else {
          results =
              accountingService.getAccounts(null, Long.valueOf(kioskId), Integer.valueOf(yr), sb, pageParams);
        }
      } else {
        throw new ForbiddenAccessException(backendMessages.getString("permission.denied"));
      }
      navigator.setResultParams(results);
      results.setNumFound(-1);
      List<AccountModel> models = accountBuilder.buildAccountModelList(results.getResults(), type,
          Long.parseLong(kioskId), domainId);
      return new Results<>(models, results.getCursor(), results.getNumFound(), results.getOffset());
    } catch (ServiceException e) {
      xLogger.severe("Error in getting accounts details");
      throw new InvalidServiceException(backendMessages.getString("account.error"));
    }
  }

  @RequestMapping("/config")
  public
  @ResponseBody
  AccountModel getAccountsConfig() {
    return accountBuilder.buildAccountConfigModel();
  }
}
