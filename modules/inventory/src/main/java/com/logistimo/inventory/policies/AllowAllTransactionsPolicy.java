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

package com.logistimo.inventory.policies;

import com.logistimo.exception.LogiException;
import com.logistimo.inventory.entity.ITransaction;
import com.logistimo.services.utils.ConfigUtil;

import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Created by charan on 14/11/17.
 */
@Component("AllowAllTransactionsPolicy")
public class AllowAllTransactionsPolicy implements InventoryUpdatePolicy {

  /**
   * This policy is dependent on the current behavior of the inventory service implementation
   * which updates the opening stock of the requese based on the server's stock value
   *
   * @param transactions - List of transactions
   * @param lastWebTrans - Last web transaction for a kid, mid and/or bid
   * @return
   */
  @Override
  public int apply(List<ITransaction> transactions, ITransaction lastWebTrans) {
    return -1;
  }

  @Override
  public void addStockCountIfNeeded(ITransaction lastWebTrans, List<ITransaction> transactions)
      throws LogiException {
    //no operation required, no need to add any stock counts.
  }

  @Override
  public boolean shouldDeduplicate() {
    return ConfigUtil.getBoolean("inventory.allowAllTransactionsPolicy.deduplicate", false);
  }
}
