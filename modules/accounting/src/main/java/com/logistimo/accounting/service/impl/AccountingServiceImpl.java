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

package com.logistimo.accounting.service.impl;

import com.logistimo.accounting.entity.IAccount;
import com.logistimo.accounting.models.CreditData;
import com.logistimo.accounting.repository.AccountingRepository;
import com.logistimo.accounting.service.IAccountingService;
import com.logistimo.config.models.AccountingConfig;
import com.logistimo.config.models.DomainConfig;
import com.logistimo.dao.JDOUtils;
import com.logistimo.entities.entity.IKiosk;
import com.logistimo.entities.entity.IKioskLink;
import com.logistimo.entities.service.EntitiesService;
import com.logistimo.events.entity.IEvent;
import com.logistimo.events.exceptions.EventGenerationException;
import com.logistimo.events.processor.EventPublisher;
import com.logistimo.logger.XLog;
import com.logistimo.pagination.PageParams;
import com.logistimo.pagination.Results;
import com.logistimo.services.ObjectNotFoundException;
import com.logistimo.services.Resources;
import com.logistimo.services.ServiceException;
import com.logistimo.services.impl.PMF;
import com.logistimo.utils.BigUtil;
import com.logistimo.utils.LocalDateUtil;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.Date;
import java.util.Locale;
import java.util.ResourceBundle;

import javax.jdo.JDOObjectNotFoundException;
import javax.jdo.PersistenceManager;

/**
 * Created by charan on 05/03/17.
 */
@Service
public class AccountingServiceImpl implements IAccountingService {

  private static final XLog xLogger = XLog.getLog(AccountingServiceImpl.class);

  private EntitiesService entitiesService;
  @Autowired
  private AccountingRepository accountingRepository;

  @Autowired
  public void setEntitiesService(EntitiesService entitiesService) {
    this.entitiesService = entitiesService;
  }

  @Override
  public CreditData getCreditData(Long customerId, Long vendorId, DomainConfig dc)
      throws ServiceException {
    BigDecimal availableCredit;
    ResourceBundle backendMessages = Resources.getBundle(Locale.ENGLISH);
    if (customerId == null || vendorId == null) {
      throw new IllegalArgumentException(backendMessages.getString("no.cust.vend.lower"));
    }
    // Get the credit limit and amount paid for the customer
    BigDecimal creditLimit = BigDecimal.ZERO;
    BigDecimal receivable = BigDecimal.ZERO;
    AccountingConfig ac = dc.getAccountingConfig();
    try {
      IKioskLink link = entitiesService.getKioskLink(
              JDOUtils.createKioskLinkId(vendorId, IKioskLink.TYPE_CUSTOMER, customerId));
      creditLimit = link.getCreditLimit();
      if (BigUtil.equalsZero(creditLimit) && ac != null) // get default credit limit for the domain
      {
        creditLimit = ac.getCreditLimit();
      }
    } catch (ObjectNotFoundException e) {
      // No link found; so no credit limit applies
    }
    // Get the receivable
    try {
      IAccount a = getAccount(vendorId, customerId, LocalDateUtil.getCurrentYear());
      receivable =
          a.getPayable(); // customer's payable is vendor's receivable for a given vendor-customer pair
    } catch (ObjectNotFoundException e) {
      // No account started/available yet; so no receivables
    }
    availableCredit = creditLimit.subtract(receivable);
    return new CreditData(creditLimit, availableCredit);
  }

  // Get a given account
  @Override
  public IAccount getAccount(Long vendorId, Long customerId, int year)
      throws ObjectNotFoundException, ServiceException {
    xLogger.fine("Entered getAccount");
    if (vendorId == null || customerId == null || year <= 0) {
      throw new ServiceException("Illegal arguments - all arguments must be valid");
    }
    PersistenceManager pm = PMF.get().getPersistenceManager();
    IAccount a = null;
    try {
      a =
          JDOUtils
              .getObjectById(IAccount.class, JDOUtils.createAccountKey(vendorId, customerId, year),
                  pm);
      a = pm.detachCopy(a);
    } catch (JDOObjectNotFoundException e) {
      // Rollover receivables in case previous year account exists; if no previous year data present, then throw exception
      a = getRolledOverAccount(vendorId, customerId, year);
      if (a == null) {
        throw new ObjectNotFoundException(e.getMessage());
      } else {
        a = pm.detachCopy(a);
      }

    } catch (Exception e) {
      throw new ServiceException(e.getMessage());
    } finally {
      pm.close();
    }
    xLogger.fine("Exiting getAccount");
    return a;
  }

  // Get an account with receivables rolled over from previous year
  private IAccount getRolledOverAccount(Long vendorId, Long customerId, int year)
      throws ServiceException {
    xLogger.fine("Entered getRolledOverAccount");
    // Get the previous year's account, if any
    IAccount newa = null;
    PersistenceManager pm = PMF.get().getPersistenceManager();
    try {
      IAccount
          a =
          JDOUtils.getObjectById(IAccount.class,
              JDOUtils.createAccountKey(vendorId, customerId, year - 1), pm);
      // Form the newer account
      newa = JDOUtils.createInstance(IAccount.class);
      newa.setKey(JDOUtils.createAccountKey(vendorId, customerId, year));
      newa.setCustomerId(customerId);
      newa.setCustomerName(a.getCustomerName());
      newa.setVendorId(vendorId);
      newa.setVendorName(a.getVendorName());
      newa.setDomainId(a.getDomainId());
      newa.setYear(year);
      newa.setPayable(a.getPayable()); // rollover any payables from the previous year
      newa.setTimestamp(new Date());
      // Persist account
      persistAccount(newa);
    } catch (JDOObjectNotFoundException e) {
      return null;
    } finally {
      pm.close();
    }
    xLogger.fine("Exiting getRolledOverAccount");
    return newa;
  }

  // Get the list of accounts given parameters - if vendorId is null, all of customer Id accounts are obtained and vice-versa; year of -1 is ignored, i.e. year is not used
  @SuppressWarnings("unchecked")
  @Override
  public Results getAccounts(Long vendorId, Long customerId, int year, String orderBy,
                             PageParams pageParams) throws ServiceException {
    return accountingRepository.findAccounts(vendorId, customerId, year, orderBy, pageParams);
  }

  // Add or update a given account
  @Override
  public void persistAccount(IAccount account) throws ServiceException {
    xLogger.fine("Entered persistAccount");
    PersistenceManager pm = PMF.get().getPersistenceManager();
    try {
      // Update account parameters
      account.setTimestamp(new Date());
      // Get kiosk accounts service
      IKiosk k = entitiesService.getKiosk(account.getCustomerId(), false);
      account.setCustomerName(k.getName());
      k = entitiesService.getKiosk(account.getVendorId(), false);
      account.setVendorName(k.getName());
      // Persist
      pm.makePersistent(account);
    } finally {
      pm.close();
    }
    xLogger.fine("Exiting persistAccount");
  }

  // Update an account with payable or receivable (pass 0 to avoid any increments)
  public void updateAccount(Long domainId, Long vendorId, Long customerId, int year,
                            BigDecimal payable, BigDecimal paid) throws ServiceException {
    xLogger.fine(
        "Entered updateAccount - vendor = {0}, customer = {1}, year = {2}, payable = {3}, paid = {4}",
        vendorId, customerId, year, payable, paid);
    IAccount a;
    try {
      // See if an account already exists
      a = getAccount(vendorId, customerId, year);
    } catch (ObjectNotFoundException e) {
      // Get a rolledover account, if any
      a = getRolledOverAccount(vendorId, customerId, year);
      if (a == null) {
        // Get kiosk names
        // Create an account
        a = JDOUtils.createInstance(IAccount.class);
        a.setCustomerId(customerId);
        a.setCustomerName(entitiesService.getKiosk(customerId, false).getName());
        a.setDomainId(domainId);
        a.setKey(JDOUtils.createAccountKey(vendorId, customerId, year));
        a.setVendorId(vendorId);
        a.setVendorName(entitiesService.getKiosk(vendorId, false).getName());
        a.setYear(year);
      }
    }
    // Increment amounts
    if (BigUtil.notEqualsZero(payable) || BigUtil.notEqualsZero(paid)) {
      a.setPayable(a.getPayable().add(payable).subtract(paid));
      // Persist account
      persistAccount(a);
      // Check if credit limit is exceeded, and whether an event needs to be generated
      DomainConfig dc = DomainConfig.getInstance(domainId);
      if (BigUtil.lesserThanEqualsZero(
          getCreditData(customerId, vendorId, dc).availabeCredit)) {
        // Genereate credit-limit-exceeded event
        try {
          EventPublisher.generate(domainId, IEvent.CREDIT_LIMIT_EXCEEDED, null, IAccount.class.getName(), a.getKey(), null);
        } catch (EventGenerationException e) {
          xLogger.warn("Failed to generate event for credit limit exceed {0}:{1}:{2}:{3}",domainId,
              a.getKey(),a.getCustomerName(), a.getVendorName());
        }
      }
    }
    xLogger.fine("Exiting updateAccount");
  }



}
