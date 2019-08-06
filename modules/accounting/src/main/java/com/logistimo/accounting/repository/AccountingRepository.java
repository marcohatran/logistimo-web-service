/*
 * Copyright © 2019 Logistimo.
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

package com.logistimo.accounting.repository;

import com.logistimo.accounting.entity.Account;
import com.logistimo.accounting.entity.IAccount;
import com.logistimo.dao.JDOUtils;
import com.logistimo.jdo.JDORepository;
import com.logistimo.pagination.PageParams;
import com.logistimo.pagination.Results;
import com.logistimo.services.impl.PMF;
import com.logistimo.utils.QueryUtil;

import org.springframework.stereotype.Repository;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.jdo.PersistenceManager;
import javax.jdo.Query;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@Repository
public class AccountingRepository extends JDORepository<Account, String> {
  @Override
  public Class<Account> getClassMetadata() {
    return Account.class;
  }

  public Results findAccounts(Long vendorId, Long customerId, int year, String orderBy,
                              PageParams pageParams) {
    PersistenceManager pm = PMF.get().getPersistenceManager();
    List<IAccount> results = null;
    // Form the query
    Query q = pm.newQuery(JDOUtils.getImplClass(IAccount.class));
    String filter = "", declaration = "";
    String ordering = "py desc"; // default order by payables
    Map<String, Object> params = new HashMap<>();
    if (vendorId != null) {
      filter = "vId == vIdParam";
      declaration = "Long vIdParam";
      params.put("vIdParam", vendorId);
      if (IAccount.FIELD_ENTITY.equals(orderBy)) {
        ordering = "cNm asc";
      }
    } else if (customerId != null) {
      filter = "cId == cIdParam";
      declaration = "Long cIdParam";
      params.put("cIdParam", customerId);
      if (IAccount.FIELD_ENTITY.equals(orderBy)) {
        ordering = "vNm asc";
      }
    }
    if (year > 0) {
      if (!filter.isEmpty()) {
        filter += " && ";
        declaration += ", ";
      }
      filter += "y == yParam";
      declaration += "Integer yParam";
      params.put("yParam", year);
    }
    // Update query
    q.setFilter(filter);
    q.declareParameters(declaration);
    // Set ordering
    q.setOrdering(ordering);
    // Update page params in query, if needed
    if (pageParams != null) {
      QueryUtil.setPageParams(q, pageParams);
    }
    String cursor = null;
    try {
      results = (List<IAccount>) q.executeWithMap(params);
      results.size(); // To get all results before pm is closed
      cursor = QueryUtil.getCursor(results);
      results = (List<IAccount>) pm.detachCopyAll(results);
      // Get the cursor, if present
    } finally {
      try {
        q.closeAll();
      } catch (Exception ignored) {
        log.warn("Exception while closing query", ignored);
      }
      pm.close();
    }
    return new Results(results, cursor);
  }
}
