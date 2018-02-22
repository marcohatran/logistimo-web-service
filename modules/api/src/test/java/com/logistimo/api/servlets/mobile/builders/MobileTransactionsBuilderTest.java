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

package com.logistimo.api.servlets.mobile.builders;

import com.logistimo.inventory.models.ErrorDetailModel;
import com.logistimo.inventory.models.ResponseDetailModel;
import com.logistimo.inventory.models.SuccessDetailModel;
import com.logistimo.proto.MobileTransSuccessModel;

import org.junit.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

/**
 * Created by vani on 16/02/18.
 */
public class MobileTransactionsBuilderTest {

  @Test
  public void testIsSuccessForFalse() throws Exception {
    MobileTransactionsBuilder mobileTransactionsBuilder = new MobileTransactionsBuilder();
    Map<Long,ResponseDetailModel> responseDetailModelByMaterial = buildResponseDetailModelByMaterial();
    boolean isSuccess = mobileTransactionsBuilder.isSuccess(responseDetailModelByMaterial);
    assertFalse(isSuccess);
  }

  private Map<Long,ResponseDetailModel> buildResponseDetailModelByMaterial() {
    Map<Long,ResponseDetailModel> responseDetailModelByMaterial = new HashMap<>(2,1);
    responseDetailModelByMaterial.put(1l,buildResponseDetailModel());
    responseDetailModelByMaterial.put(2l,buildResponseDetailModel());
    return responseDetailModelByMaterial;
  }

  private ResponseDetailModel buildResponseDetailModel() {
    List<SuccessDetailModel> successDetailModels = new ArrayList<>(1);
    successDetailModels.add(new SuccessDetailModel("M015", 2, Arrays.asList("1111", "2222","3333")));

    List<ErrorDetailModel> errorDetailModels = new ArrayList<>(1);
    errorDetailModels.add(new ErrorDetailModel("M010", 8, null));
    errorDetailModels.add(new ErrorDetailModel("M011", 1, null));
    errorDetailModels.add(new ErrorDetailModel("M012", 5, null));
    return (new ResponseDetailModel(successDetailModels,errorDetailModels));
  }

  @Test
  public void testIsSuccessWithEmptyInput() throws Exception {
    MobileTransactionsBuilder mobileTransactionsBuilder = new MobileTransactionsBuilder();
    Map<Long,ResponseDetailModel> responseDetailModelByMaterial = new HashMap<>(1,1);
    boolean isSuccess = mobileTransactionsBuilder.isSuccess(responseDetailModelByMaterial);
    assertTrue(!isSuccess);
  }

  @Test
  public void testIsSuccessWithOnlySuccess() throws Exception {
    MobileTransactionsBuilder mobileTransactionsBuilder = new MobileTransactionsBuilder();
    Map<Long,ResponseDetailModel> responseDetailModelByMaterial = buildResponseDetailModelByMaterialWithOnlySuccess();
    boolean isSuccess = mobileTransactionsBuilder.isSuccess(responseDetailModelByMaterial);
    assertTrue(isSuccess);
    responseDetailModelByMaterial = buildResponseDetailModelByMaterialWithOnlyError();
    isSuccess = mobileTransactionsBuilder.isSuccess(responseDetailModelByMaterial);
    assertTrue(!isSuccess);
  }

  private Map<Long,ResponseDetailModel> buildResponseDetailModelByMaterialWithOnlySuccess() {
    Map<Long,ResponseDetailModel> responseDetailModelByMaterial = new HashMap<>(2,1);
    responseDetailModelByMaterial.put(1l,buildResponseDetailModelWithOnlySuccess());
    responseDetailModelByMaterial.put(2l,buildResponseDetailModelWithOnlySuccess());
    return responseDetailModelByMaterial;
  }

  private ResponseDetailModel buildResponseDetailModelWithOnlySuccess() {
    List<SuccessDetailModel> successDetailModels = new ArrayList<>(1);
    successDetailModels.add(new SuccessDetailModel("M015", 2, Arrays.asList("1111", "2222","3333")));
    return (new ResponseDetailModel(successDetailModels,new ArrayList<>(1)));
  }

  @Test
  public void testIsSuccessWithOnlyError() throws Exception {
    MobileTransactionsBuilder mobileTransactionsBuilder = new MobileTransactionsBuilder();
    Map<Long,ResponseDetailModel> responseDetailModelByMaterial = buildResponseDetailModelByMaterialWithOnlyError();
    boolean isSuccess = mobileTransactionsBuilder.isSuccess(responseDetailModelByMaterial);
    assertTrue(!isSuccess);
  }

  private Map<Long,ResponseDetailModel> buildResponseDetailModelByMaterialWithOnlyError() {
    Map<Long,ResponseDetailModel> responseDetailModelByMaterial = new HashMap<>(2,1);
    responseDetailModelByMaterial.put(1l,buildResponseDetailModelWithOnlyError());
    responseDetailModelByMaterial.put(2l,buildResponseDetailModelWithOnlyError());
    return responseDetailModelByMaterial;
  }

  private ResponseDetailModel buildResponseDetailModelWithOnlyError() {
    List<ErrorDetailModel> errorDetailModels = new ArrayList<>(1);
    errorDetailModels.add(new ErrorDetailModel("M010", 8, null));
    errorDetailModels.add(new ErrorDetailModel("M011", 1, null));
    errorDetailModels.add(new ErrorDetailModel("M012", 5, null));
    return (new ResponseDetailModel(new ArrayList<>(1),errorDetailModels));
  }

  @Test
  public void testGetErrorDetailModelsByMaterial() throws Exception {
    MobileTransactionsBuilder mobileTransactionsBuilder = new MobileTransactionsBuilder();
    Map<Long,ResponseDetailModel> responseDetailModelByMaterial = buildResponseDetailModelByMaterial();
    Map<Long,List<ErrorDetailModel>> errorDetailModelsByMaterial = mobileTransactionsBuilder.getErrorDetailModelsByMaterial(responseDetailModelByMaterial);
    assertNotNull(errorDetailModelsByMaterial);
    assertTrue(!errorDetailModelsByMaterial.isEmpty());
    assertEquals(2,errorDetailModelsByMaterial.size());
    assertEquals(responseDetailModelByMaterial.get(1l).errorDetailModels,errorDetailModelsByMaterial.get(1l));
    assertEquals(responseDetailModelByMaterial.get(2l).errorDetailModels,errorDetailModelsByMaterial.get(2l));
  }

  @Test
  public void testGetSuccessDetailModelsByMaterial() throws Exception {
    MobileTransactionsBuilder mobileTransactionsBuilder = new MobileTransactionsBuilder();
    Map<Long,ResponseDetailModel> responseDetailModelByMaterial = buildResponseDetailModelByMaterial();
    Map<Long,List<SuccessDetailModel>> successDetailModelsByMaterial = mobileTransactionsBuilder.getSuccessDetailModelsByMaterial(
        responseDetailModelByMaterial);
    assertNotNull(successDetailModelsByMaterial);
    assertTrue(!successDetailModelsByMaterial.isEmpty());
    assertEquals(2,successDetailModelsByMaterial.size());
    assertEquals(responseDetailModelByMaterial.get(1l).successDetailModels,successDetailModelsByMaterial.get(1l));
    assertEquals(responseDetailModelByMaterial.get(2l).successDetailModels,successDetailModelsByMaterial.get(2l));
  }

  @Test
  public void testBuildSuccessModels() throws Exception {
    MobileTransactionsBuilder mobileTransactionsBuilder = new MobileTransactionsBuilder();
    Map<Long,ResponseDetailModel> responseDetailModelByMaterial = buildResponseDetailModelByMaterial();
    List<MobileTransSuccessModel> mobTransSuccessModels = mobileTransactionsBuilder.buildSuccessModels(
        mobileTransactionsBuilder.getSuccessDetailModelsByMaterial(responseDetailModelByMaterial));
    assertNotNull(mobTransSuccessModels);
    assertTrue(!mobTransSuccessModels.isEmpty());
    assertEquals(2, mobTransSuccessModels.size());
    assertEquals(1, mobTransSuccessModels.get(0).successDetails.size());
    assertSame(2, mobTransSuccessModels.get(0).successDetails.get(0).index);
    assertEquals(Arrays.asList("1111","2222","3333"), mobTransSuccessModels.get(0).successDetails.get(0).keys);
    assertEquals(1, mobTransSuccessModels.get(1).successDetails.size());
    assertSame(2, mobTransSuccessModels.get(1).successDetails.get(0).index);
    assertEquals(Arrays.asList("1111","2222","3333"), mobTransSuccessModels.get(1).successDetails.get(0).keys);
  }
}