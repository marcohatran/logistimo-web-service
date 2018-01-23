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

package com.logistimo.api.builders;

import com.logistimo.accounting.entity.IAccount;
import com.logistimo.api.models.AccountModel;
import com.logistimo.config.models.AccountingConfig;
import com.logistimo.config.models.DomainConfig;
import com.logistimo.dao.JDOUtils;
import com.logistimo.entities.entity.IKiosk;
import com.logistimo.entities.entity.IKioskLink;
import com.logistimo.entities.service.EntitiesService;
import com.logistimo.services.ServiceException;
import com.logistimo.utils.BigUtil;
import com.logistimo.utils.CommonUtils;
import com.logistimo.utils.LocalDateUtil;

import org.apache.commons.lang.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by mohan raja on 03/12/14
 */

@Component
public class AccountBuilder {

  private EntitiesService entitiesService;

  @Autowired
  public void setEntitiesService(EntitiesService entitiesService) {
    this.entitiesService = entitiesService;
  }

  // Get the accounting years
  public static int[] getAccountingYears() {
    // Get currenty year
    int curYear = LocalDateUtil.getCurrentYear();
    int numYears = curYear - IAccount.START_YEAR;
    int[] years;
    if (numYears == 0) {
      years = new int[1];
      years[0] = curYear;
    } else {
      years = new int[numYears];
      for (int i = 0; i < numYears; i++) {
        years[i] = curYear;
        curYear--;
      }
    }
    return years;
  }

  public AccountModel buildAccountConfigModel() {
    AccountModel model = new AccountModel();
    model.years = getAccountingYears();
    model.curyear = LocalDateUtil.getCurrentYear();
    return model;
  }

  public List<AccountModel> buildAccountModelList(List<IAccount> accounts, String type,
                                                  Long kioskId, Long domainId)
      throws ServiceException {
    List<AccountModel> models = new ArrayList<>(accounts.size());
    int sno = 1;
    DomainConfig dc = DomainConfig.getInstance(domainId);
    for (IAccount account : accounts) {
      AccountModel model = buildAccountModel(account, type, kioskId, dc);
      model.sno = sno++;
      models.add(model);
    }
    return models;
  }

  public AccountModel buildAccountModel(IAccount account, String type,
                                        Long kioskId, DomainConfig dc)
      throws ServiceException {
    AccountModel model = new AccountModel();
    String linkId = null;
    if (IAccount.RECEIVABLE.equals(type)) {
      model.name = account.getCustomerName();
      if (!kioskId.equals(account.getCustomerId())) {
        linkId =
            JDOUtils.createKioskLinkId(kioskId, IKioskLink.TYPE_CUSTOMER, account.getCustomerId());
      }
    } else {
      model.name = account.getVendorName();
      if (!kioskId.equals(account.getVendorId())) {
        linkId =
            JDOUtils.createKioskLinkId(account.getVendorId(), IKioskLink.TYPE_CUSTOMER, kioskId);
      }
    }
    IKiosk k = entitiesService.getKiosk(kioskId, false);
    model.cur = k.getCurrency();
    model.add = CommonUtils.getAddress(k.getCity(), k.getTaluk(), k.getDistrict(), k.getState());
    if (StringUtils.isBlank(model.cur)) {
      model.cur = dc.getCurrency();
    }
    model.npay = CommonUtils.getFormattedPrice(account.getPayable());
    IKioskLink kl = null;
    try {
      if (linkId != null) {
        kl = entitiesService.getKioskLink(linkId);
      }
    } catch (Exception e) {
      System.out.println("accounts.jsp: " + e.getClass().getName() + ": " + e.getMessage());
    }
    BigDecimal creditLimit = BigDecimal.ZERO;
    if (kl != null) {
      creditLimit = kl.getCreditLimit();
    }
    if (BigUtil.equalsZero(creditLimit)) {
      AccountingConfig ac = dc.getAccountingConfig();
      BigDecimal defaultCreditLimit = BigDecimal.ZERO;
      if (ac != null) {
        defaultCreditLimit = ac.getCreditLimit();
      }
      creditLimit = defaultCreditLimit;
    }
    model.bal = CommonUtils.getFormattedPrice(creditLimit.subtract(account.getPayable()));
    return model;
  }

}
