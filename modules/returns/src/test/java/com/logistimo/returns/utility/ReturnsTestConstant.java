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

  private ReturnsTestConstant() {
  }

  public static final String
      RETURNS_ITEM_BATCH_1 = "{\"materialId\":1,\"quantity\":13,\"returnsId\":1,"
      + "\"returnItemBatches\":[{\"itemId\":1,\"quantity\":10,\"batch\":{\"batchId\":\"BATCH1\","
      + "\"manufacturer\":\"MANUFACTURER1\"}},{\"itemId\":2,\"quantity\":10, \"batch\":{\"batchId\""
      + ":\"BATCH2\",\"manufacturer\":\"MANUFACTURER2\" }}]}";

  public static final String
      RETURNS_ITEM_NON_BATCH_1 = "{\"materialId\":2,\"quantity\":13,\"returnsId\":1,"
      + "\"materialStatus\":\"materialStatus1\",\"reason\":\"reason1\"}";

  public static final String
      RETURNS_ITEM_NON_BATCH_2 = "{\"materialId\":2,\"quantity\":23}";

  public static final String
      RETURNS_ITEM_BATCH_2 = "{\"materialId\":1,\"quantity\":13,\"returnItemBatches\":[{\"itemId\":"
      + "1,\"quantity\":60,\"batch\":{\"batchId\":\"BATCH1\",\"manufacturer\":\"MANUFACTURER3\"}}"
      + ",{\"itemId\":1,\"quantity\":30, \"batch\":{\"batchId\":\"BATCH2\",\"manufacturer\":"
      + "\"MANUFACTURER4\" }}]}";

  public static final String
      FULFILLED_QTY_MODEL_1 =
      "{\"materialId\":1,\"fulfilledQuantity\":50,\"batchId\":\"BATCH1\"}";

  public static final String
      FULFILLED_QTY_MODEL_2 =
      "{\"materialId\":1,\"fulfilledQuantity\":20,\"batchId\":\"BATCH2\"}";

  public static final String FULFILLED_QTY_MODEL_3 = "{\"materialId\":2,\"fulfilledQuantity\":20}";

  public static final String HANDLING_UNIT_1 = "{\"materialId\":2,\"quantity\":3}";

  public static final String INVENTORY_1 = "{\"materialId\":1,\"atpStk\":25}";

  public static final String INVENTORY_BATCH_1 = "{\"batchId\":\"BATCH1\",\"atpStk\":25,\"materialId\":1}";

  public static final String INVENTORY_BATCH_2 = "{\"batchId\":\"BATCH2\",\"atpStk\":25,\"materialId\":1}";

  public static final String INVENTORY_2 = "{\"materialId\":1,\"atpStk\":25}";

  public static final String STATUS_MODEL = "{\"status\":\"op\",\"userId\":\"TEST\",\"source\":1}";

  public static final String
      RETURNS =
      "{\"src\":1,\"sourceDomain\":1,\"id\":1,\"customerId\":1,\"vendorId\":1,\"q\":10,\"uId\":"
          + "\"TEST\",\"rs\":\"reason1\",\"mst\":\"materialStatus1\",\"tid\":\"1\",\"tot\":\"tr\","
          + "\"status\":{\"status\":\"op\"},\"transporterDetailsVO\":{\"trackingId\":\"12356\","
          + "\"transporter\":\"fafsfsfs\"}}";

  public static final String
      TEST_RETURNS_1 =
      "{\"src\":1,\"sourceDomain\":1,\"id\":1,\"customerId\":1,\"vendorId\":1,\"q\":10,\"uId\":"
          + "\"TEST\",\"rs\":\"reason1\",\"mst\":\"materialStatus1\",\"tid\":\"1\",\"tot\":\"tr\","
          + "\"status\":{\"status\":\"op\"},\"transporterDetailsVO\":{\"trackingId\":\"12356\","
          + "\"transporter\":\"fafsfsfs\"},\"items\":[{\"materialId\":1,\"quantity\":13,"
          + "\"returnsId\":1,\"returnItemBatches\":[{\"itemId\":1,\"quantity\":10,\"batch\":"
          + "{\"batchId\":\"BATCH1\",\"manufacturer\":\"MANUFACTURER1\"}}],{\"materialId\":2,"
          + "\"quantity\":50,\"returnsId\":1,\"returnItemBatches\":[{\"itemId\":3,\"quantity\":10,"
          + "\"batch\":{\"batchId\":\"BATCH1\",\"manufacturer\":\"MANUFACTURER1\"}}"
          + "]}";

  public static final String
      TEST_RETURNS_2 =
      "{\"src\":1,\"sourceDomain\":1,\"id\":1,\"customerId\":1,\"vendorId\":1,\"q\":10,\"uId\":"
          + "\"TEST\",\"rs\":\"reason1\""
          + ",\"mst\":\"materialStatus1\",\"tid\":\"1\",\"tot\":\"tr\",\"status\":{\"status\":"
          + "\"op\"},\"transporterDetailsVO\":{\"trackingId\":\"12356\",\"transporter\":"
          + "\"fafsfsfs\"},\"items\":{\"materialId\":1,\"quantity\":13,\"returnsId\":1,"
          + "\"returnItemBatches\":[{\"itemId\":1,\"quantity\":30,"
          + "\"batch\":{\"batchId\":\"BATCH1\",\"manufacturer\":\"MANUFACTURER1\"}},"
          + "{\"itemId\":1,\"quantity\":5, \"batch\":{\"batchId\":\"BATCH2\","
          + "\"manufacturer\":\"MANUFACTURER2\" }}]}}";

  public static final String
      TRANSACTION = "{\"src\":1,\"sdId\":1,\"kId\":1,\"mId\":2,\"q\":13,\"uId\":\"TEST\",\"rs\":"
      + "\"reason1\",\"mst\":\"materialStatus1\",\"tid\":\"1\",\"tot\":\"or\",\"lkid\":1 ,\"sdf\":"
      + "0,\"useCustomTimestamp\":false,\"systemCreated\":false}";

  public static final String
      RETURNS_TRACKING_DETAILS = "{\"trackingId\":\"TrackingID1\",\"transporter\":\"transporter1\""
      + ",\"returnsId\":\"1\",\"estimatedArrivalDate\":\"2018-08-08\",\"createdAt\":\"2018-05-05\","
      + "\"createdBy\":\"TEST_USER\"}";

  public static final String
      UPDATED_RETURNS_TRACKING_DETAILS =
      "{\"trackingId\":\"TrackingID2\",\"transporter\":\"transporter2\",\"returnsId\":\"1\","
          + "\"estimatedArrivalDate\":\"2018-09-09\",\"createdAt\":\"2018-05-05\",\"transporter\":"
          + "\"transporter2\",\"createdBy\":\"TEST_USER\",\"updatedAt\":\"2018-05-05\","
          + "\"updatedBy\":\"TEST_USER\",\"requiredOn\":\"2018-09-09\"}";

  public static final String
      RETURNS_BATCH_QUANTITY_VO = "{\"batchId\":BATCH1,\"materialId\":1,\"batchQuantity\":20,"
      + "  \"status\":\"OPEN\",\"manufacturer\":\"MANUFACTURER1\",\"expiryDate\":\"2018-09-09\","
      + "\"manufacturedDate\":\"2018-01-01\"}";

  public static final String
      RETURNS_BATCH_QUANTITY_VO_2 = "{\"batchId\":BATCH3,\"materialId\":2,\"batchQuantity\":20,"
      + "  \"status\":\"OPEN\",\"manufacturer\":\"MANUFACTURER1\",\"expiryDate\":\"2018-09-09\","
      + "\"manufacturedDate\":\"2018-01-01\"}";

  public static final String
      RETURNS_BATCH_QUANTITY_VO_3 = "{\"batchId\":BATCH4,\"materialId\":2,\"batchQuantity\":20,"
      + "  \"status\":\"OPEN\",\"manufacturer\":\"MANUFACTURER1\",\"expiryDate\":\"2018-09-09\","
      + "\"manufacturedDate\":\"2018-01-01\"}";

  public static final String
      RETURNS_NON_BATCH_QUANTITY_VO =
      "{\"materialId\":4,\"itemQuantity\":20,\"status\":\"RECEIVED\"}";

  public static final String
      RETURNS_NON_BATCH_QUANTITY_VO_1 =
      "{\"materialId\":6,\"itemQuantity\":20,\"status\":\"OPEN\"}";

  public static final String
      RETURNS_BATCH_QUANTITY_VO_4 = "{\"batchId\":BATCH5,\"materialId\":5,\"batchQuantity\":20,"
      + "  \"status\":\"SHIPPED\",\"manufacturer\":\"MANUFACTURER1\",\"expiryDate\":\"2018-09-09\","
      + "\"manufacturedDate\":\"2018-01-01\"}";

  public static final String
      RETURNS_BATCH_QUANTITY_VO_5 = "{\"batchId\":BATCH6,\"materialId\":5,\"batchQuantity\":20,"
      + "  \"status\":\"OPEN\",\"manufacturer\":\"MANUFACTURER1\",\"expiryDate\":\"2018-09-09\","
      + "\"manufacturedDate\":\"2018-01-01\"}";


}
