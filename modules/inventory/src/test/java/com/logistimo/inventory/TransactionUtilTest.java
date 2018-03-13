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

package com.logistimo.inventory;

import com.logistimo.inventory.entity.ITransaction;
import com.logistimo.inventory.entity.Transaction;

import org.junit.Test;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/**
 * Created by vani on 08/03/18.
 */
public class TransactionUtilTest {
  @Test
  public void testIsTrackingDetailsPresent() {
    ITransaction transaction = new Transaction();
    // Tracking ID and Tracking Object Type
    transaction.setTrackingId("1");
    transaction.setTrackingObjectType("iss_trn");
    assertTrue(TransactionUtil.isTrackingDetailsPresent(transaction));

    // Tracking ID, Tracking Object Type and Local Tracking ID
    transaction.setLocalTrackingID("ltid_1");
    assertTrue(TransactionUtil.isTrackingDetailsPresent(transaction));

    // Tracking Object Type and Local tracking ID
    transaction.setTrackingId(null);
    assertTrue(TransactionUtil.isTrackingDetailsPresent(transaction));

    // Tracking object type
    transaction.setLocalTrackingID(null);
    assertFalse(TransactionUtil.isTrackingDetailsPresent(transaction));

    // No tracking details
    transaction.setTrackingObjectType(null);
    assertFalse(TransactionUtil.isTrackingDetailsPresent(transaction));

    // Tracking ID
    transaction.setTrackingId("1");
    assertFalse(TransactionUtil.isTrackingDetailsPresent(transaction));

    // Tracking ID and Local tracking ID
    transaction.setLocalTrackingID("ltd_1");
    assertFalse(TransactionUtil.isTrackingDetailsPresent(transaction));
  }

}