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

package com.logistimo.exports.util;

/**
 * Created by pratheeka on 22/03/18.
 */
public class MobileExportConstants {

  private MobileExportConstants(){
    throw new AssertionError("cannot instantiate");
  }

  //Mobile request constants
  public static final String DOMAIN_ID_KEY = "domainid";
  public static final String END_DATE_KEY = "to";
  public static final String FROM_DATE_KEY = "from";
  public static final String KIOSK_ID_KEY = "kioskid";
  public static final String MATERIAL_ID_KEY = "materialid";
  public static final String EXPORT_TYPE_KEY = "exportType";
  public static final String ORDERS_KEY = "orders";
  public static final String TRANSFERS_KEY = "transfers";
  //to identify sales/purchase
  public static final String ORDERS_SUB_TYPE_KEY = "orderType";
  public static final String TRANSACTIONS_KEY = "transactions";
  //to identify transfer/non transfer
  public static final String ORDER_TYPE_KEY = "otype";
  public static final String TRANSACTIONS_TYPE_KEY = "transactiontype";
  public static final String INVENTORY_KEY = "inventory";
  public static final String INVENTORY_TEMPLATE_KEY = "stock_views";

  public static final String INVENTORY_MODULE_KEY = "inventory";

  public static final String EMAIL_ID_KEY = "emailid";
}
