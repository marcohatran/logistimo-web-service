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

package com.logistimo.returns.utility;

/**
 * Created by pratheeka on 23/05/18.
 */
public class ReturnsTestConstant {

  private ReturnsTestConstant(){}

  public static final String
      RETURNS_ITEM_BATCH_1 ="{\"materialId\":1,\"quantity\":13,\"returnsId\":1,\"returnItemBatches\":[{\"itemId\":1,\"quantity\":10,\n"
      + "  \"batch\":{\"batchId\":\"BATCH1\",\"manufacturer\":\"MANUFACTURER1\"}},{\"itemId\":2,\"quantity\":10, \"batch\":{\"batchId\":\"BATCH2\",\n"
      + "    \"manufacturer\":\"MANUFACTURER2\" }}]}";

  public static final String
      RETURNS_ITEM_NON_BATCH_1 ="{\"materialId\":2,\"quantity\":13,\"returnsId\":1,\"materialStatus\":\"materialStatus1\",\"reason\":\"reason1\"}";

  public static final String
      RETURNS_ITEM_NON_BATCH_2 ="{\"materialId\":2,\"quantity\":23}";
  public static final String
      RETURNS_ITEM_BATCH_2 ="{\"materialId\":1,\"quantity\":13,\"returnItemBatches\":[{\"itemId\":1,\"quantity\":60,\n"
      + "  \"batch\":{\"batchId\":\"BATCH1\",\"manufacturer\":\"MANUFACTURER3\"}},{\"itemId\":2,\"quantity\":30, \"batch\":{\"batchId\":\"BATCH2\",\n"
      + "    \"manufacturer\":\"MANUFACTURER4\" }}]}";

  public static final String FULFILLED_QTY_MODEL_1="{\"materialId\":1,\"fulfilledQuantity\":50,\"batchId\":\"BATCH1\"}";

  public static final String FULFILLED_QTY_MODEL_2="{\"materialId\":1,\"fulfilledQuantity\":20,\"batchId\":\"BATCH2\"}";

  public static final String FULFILLED_QTY_MODEL_3="{\"materialId\":2,\"fulfilledQuantity\":20}";

  public static final String HANDLING_UNIT_1="{\"materialId\":2,\"quantity\":3}";

  public static final String INVENTORY_1="{\"mId\":1,\"atpStk\":25}";

  public static final String INVENTORY_2="{\"mId\":1,\"atpStk\":25}";

  public static final String STATUS_MODEL="{\"status\":\"op\",\"userId\":\"TEST\",\"source\":1}";

  public static final String RETURNS="{\"src\":1,\"sourceDomain\":1,\"id\":1,\"customerId\":1,\"vendorId\":1,\"q\":10,\"uId\":\"TEST\",\"rs\":\"reason1\""
      + ",\"mst\":\"materialStatus1\",\"tid\":\"1\",\"tot\":\"tr\",\"status\":{\"status\":\"op\"}}";


  public static final String TRANSACTION="{\"src\":1,\"sdId\":1,\"kId\":1,\"mId\":2,\"q\":13,\"uId\":\"TEST\",\"rs\":\"reason1\","
      + "\"mst\":\"materialStatus1\",\"tid\":\"1\",\"tot\":\"or\",\"lkid\":1 ,\"sdf\":0,\"useCustomTimestamp\":false,\"systemCreated\":false}";


}
