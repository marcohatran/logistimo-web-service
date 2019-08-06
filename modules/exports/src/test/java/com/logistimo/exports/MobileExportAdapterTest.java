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

package com.logistimo.exports;

import com.logistimo.inventory.entity.ITransaction;
import com.logistimo.services.Resources;
import org.junit.Test;

import java.util.Locale;
import java.util.ResourceBundle;

import static org.junit.Assert.assertEquals;

/**
 * Created by amitkumar on 18/03/19.
 */
public class MobileExportAdapterTest {

    @Test
    public void getTransactionTypeIssue() {
        MobileExportAdapter mobileExportAdapter = new MobileExportAdapter();
        ResourceBundle
                backendMessages =
                Resources.getBundle(Locale.getDefault());
        String message = mobileExportAdapter.getTransactionType(backendMessages,ITransaction.TYPE_ISSUE);
        assertEquals(backendMessages.getString("exports.transaction.type.issue"),message);
    }

    @Test
    public void getTransactionTypeReceipt() {
        MobileExportAdapter mobileExportAdapter = new MobileExportAdapter();
        ResourceBundle
                backendMessages =
                Resources.getBundle(Locale.getDefault());
        String message = mobileExportAdapter.getTransactionType(backendMessages,ITransaction.TYPE_RECEIPT);
        assertEquals(backendMessages.getString("exports.transaction.type.receipt"),message);
    }

    @Test
    public void getTransactionTypePhysicalCount() {
        MobileExportAdapter mobileExportAdapter = new MobileExportAdapter();
        ResourceBundle
                backendMessages =
                Resources.getBundle(Locale.getDefault());
        String message = mobileExportAdapter.getTransactionType(backendMessages,ITransaction.TYPE_PHYSICALCOUNT);
        assertEquals(backendMessages.getString("exports.transaction.type.stockcount"),message);
    }

    @Test
    public void getTransactionTypeWastage() {
        MobileExportAdapter mobileExportAdapter = new MobileExportAdapter();
        ResourceBundle
                backendMessages =
                Resources.getBundle(Locale.getDefault());
        String message = mobileExportAdapter.getTransactionType(backendMessages,ITransaction.TYPE_WASTAGE);
        assertEquals(backendMessages.getString("exports.transaction.type.wastage"),message);
    }

    @Test
    public void getTransactionTypeTransfer() {
        MobileExportAdapter mobileExportAdapter = new MobileExportAdapter();
        ResourceBundle
                backendMessages =
                Resources.getBundle(Locale.getDefault());
        String message = mobileExportAdapter.getTransactionType(backendMessages,ITransaction.TYPE_TRANSFER);
        assertEquals(backendMessages.getString("exports.transaction.type.transfers"),message);
    }

    @Test
    public void getTransactionTypeReturnsIncoming() {
        MobileExportAdapter mobileExportAdapter = new MobileExportAdapter();
        ResourceBundle
                backendMessages =
                Resources.getBundle(Locale.getDefault());
        String message = mobileExportAdapter.getTransactionType(backendMessages,ITransaction.TYPE_RETURNS_INCOMING);
        assertEquals(backendMessages.getString("exports.transaction.type.incoming.returns"),message);
    }

    @Test
    public void getTransactionTypeReturnsOutgoing() {
        MobileExportAdapter mobileExportAdapter = new MobileExportAdapter();
        ResourceBundle
                backendMessages =
                Resources.getBundle(Locale.getDefault());
        String message = mobileExportAdapter.getTransactionType(backendMessages,ITransaction.TYPE_RETURNS_OUTGOING);
        assertEquals(backendMessages.getString("exports.transaction.type.outgoing.returns"),message);
    }


    @Test
    public void getTransactionTypeReturnsInvalid() {
        MobileExportAdapter mobileExportAdapter = new MobileExportAdapter();
        ResourceBundle
                backendMessages =
                Resources.getBundle(Locale.getDefault());
        String message = mobileExportAdapter.getTransactionType(backendMessages,"Invalid");
        assertEquals("Invalid",message);
    }
}