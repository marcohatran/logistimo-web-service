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

import com.logistimo.returns.vo.ReturnsQuantityDetailsVO;
import com.logistimo.returns.models.ReturnsFilters;
import com.logistimo.returns.vo.ReturnsItemBatchVO;
import com.logistimo.returns.vo.ReturnsItemVO;
import com.logistimo.returns.vo.ReturnsTrackingDetailsVO;
import com.logistimo.returns.vo.ReturnsVO;
import com.logistimo.security.SecureUserDetails;
import com.logistimo.shipments.FulfilledQuantityModel;

import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.io.IOUtils;

import java.io.IOException;
import java.io.InputStream;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import static com.logistimo.returns.utility.ReturnsTestConstant.RETURNS_BATCH_QUANTITY_VO;
import static com.logistimo.returns.utility.ReturnsTestConstant.RETURNS_BATCH_QUANTITY_VO_2;
import static com.logistimo.returns.utility.ReturnsTestConstant.RETURNS_BATCH_QUANTITY_VO_3;
import static com.logistimo.returns.utility.ReturnsTestConstant.RETURNS_BATCH_QUANTITY_VO_4;
import static com.logistimo.returns.utility.ReturnsTestConstant.RETURNS_BATCH_QUANTITY_VO_5;
import static com.logistimo.returns.utility.ReturnsTestConstant.RETURNS_NON_BATCH_QUANTITY_VO;
import static com.logistimo.returns.utility.ReturnsTestConstant.RETURNS_NON_BATCH_QUANTITY_VO_1;

/**
 * Created by pratheeka on 22/05/18.
 */
public class ReturnsTestUtility {
  private ReturnsTestUtility(){

  }

  private static final String TEST_USER = "test";

  public static SecureUserDetails getSecureUserDetails() {
    SecureUserDetails userDetails = new SecureUserDetails();
    userDetails.setCurrentDomainId(123456L);
    userDetails.setUsername(TEST_USER);
    userDetails.setLocale(Locale.ENGLISH);
    return userDetails;
  }

  public static ReturnsVO getReturnsVO() throws IOException {
    InputStream inputStream =
        Thread.currentThread().getContextClassLoader()
            .getResourceAsStream("testReturns.json");
    return ReturnsGsonMapper.getTestObject(IOUtils.toString(inputStream), ReturnsVO.class);
  }
  public static ReturnsVO getUpdatedReturnsVO() throws IOException {
    InputStream inputStream =
        Thread.currentThread().getContextClassLoader()
            .getResourceAsStream("updatedTestReturns.json");
    return ReturnsGsonMapper.getTestObject(IOUtils.toString(inputStream), ReturnsVO.class);
  }
  public static List<ReturnsItemVO> getReturnsItemVOList() {
    ReturnsItemVO vo = ReturnsGsonMapper
        .getTestObject(ReturnsTestConstant.RETURNS_ITEM_BATCH_1, ReturnsItemVO.class);
    List<ReturnsItemVO> list = new ArrayList<>(1);
    list.add(vo);
    return list;
  }

  public static ReturnsTrackingDetailsVO getTrackingDetailsVO(){
    return ReturnsGsonMapper.getTestObject(
        ReturnsTestConstant.RETURNS_TRACKING_DETAILS,ReturnsTrackingDetailsVO.class);
  }

  public static List<ReturnsVO> getReturnVOList() throws IOException {
    ReturnsVO vo = ReturnsTestUtility.getReturnsVO();
    List<ReturnsVO> list = new ArrayList<>(1);
    list.add(vo);
    return list;
  }


  public static ReturnsFilters getReturnsFilters() {
    ReturnsFilters returnsFilters = ReturnsFilters.builder().build();
    returnsFilters.setUserId("TEST_USER");
    return returnsFilters;
  }

  public static List<ReturnsQuantityDetailsVO> getTotalReturnedQty(){
    List<ReturnsQuantityDetailsVO> returnsQuantityDetailsVOList = new ArrayList<>(6);
    ReturnsQuantityDetailsVO returnsQuantityDetailsVOBatch1 =
        ReturnsGsonMapper.getTestObject(RETURNS_BATCH_QUANTITY_VO, ReturnsQuantityDetailsVO.class);
    returnsQuantityDetailsVOList.add(returnsQuantityDetailsVOBatch1);
    ReturnsQuantityDetailsVO
        returnsQuantityDetailsVOBatch3 =
        ReturnsGsonMapper
            .getTestObject(RETURNS_BATCH_QUANTITY_VO_2, ReturnsQuantityDetailsVO.class);
    returnsQuantityDetailsVOList.add(returnsQuantityDetailsVOBatch3);
    ReturnsQuantityDetailsVO
        returnsQuantityDetailsVOBatch4 =
        ReturnsGsonMapper
            .getTestObject(RETURNS_BATCH_QUANTITY_VO_3, ReturnsQuantityDetailsVO.class);
    returnsQuantityDetailsVOList.add(returnsQuantityDetailsVOBatch4);
    ReturnsQuantityDetailsVO
        returnsQuantityDetailsVONonBatch =
        ReturnsGsonMapper
            .getTestObject(RETURNS_NON_BATCH_QUANTITY_VO, ReturnsQuantityDetailsVO.class);
    returnsQuantityDetailsVOList.add(returnsQuantityDetailsVONonBatch);

    ReturnsQuantityDetailsVO
        returnsQuantityDetailsVONonBatch1 =
        ReturnsGsonMapper
            .getTestObject(RETURNS_NON_BATCH_QUANTITY_VO_1, ReturnsQuantityDetailsVO.class);
    returnsQuantityDetailsVOList.add(returnsQuantityDetailsVONonBatch1);

    ReturnsQuantityDetailsVO
        returnsQuantityDetailsVOBatch5 =
        ReturnsGsonMapper
            .getTestObject(RETURNS_BATCH_QUANTITY_VO_4, ReturnsQuantityDetailsVO.class);
    returnsQuantityDetailsVOList.add(returnsQuantityDetailsVOBatch5);
    ReturnsQuantityDetailsVO
        returnsQuantityDetailsVOBatch6 =
        ReturnsGsonMapper
            .getTestObject(RETURNS_BATCH_QUANTITY_VO_5, ReturnsQuantityDetailsVO.class);
    returnsQuantityDetailsVOList.add(returnsQuantityDetailsVOBatch6);
    return returnsQuantityDetailsVOList;
  }


  public static List<FulfilledQuantityModel> getFulfilledQuantityModelList() {
    List<FulfilledQuantityModel> fulfilledQuantityModelList = new ArrayList<>();
    FulfilledQuantityModel fulfilledQuantityModel = new FulfilledQuantityModel();
    fulfilledQuantityModel.setBatchId("BATCH1");
    fulfilledQuantityModel.setMaterialId(1L);
    fulfilledQuantityModel.setFulfilledQuantity(new BigDecimal(100));
    fulfilledQuantityModelList.add(fulfilledQuantityModel);
    FulfilledQuantityModel fulfilledQtyBatch2 = new FulfilledQuantityModel();
    fulfilledQtyBatch2.setBatchId("BATCH2");
    fulfilledQtyBatch2.setMaterialId(1L);
    fulfilledQtyBatch2.setFulfilledQuantity(new BigDecimal(100));
    fulfilledQuantityModelList.add(fulfilledQtyBatch2);
    FulfilledQuantityModel fulfilledQtyBatch3 = new FulfilledQuantityModel();
    fulfilledQtyBatch3.setBatchId("BATCH3");
    fulfilledQtyBatch3.setMaterialId(2L);
    fulfilledQtyBatch3.setFulfilledQuantity(new BigDecimal(100));
    fulfilledQuantityModelList.add(fulfilledQtyBatch3);

    FulfilledQuantityModel fulfilledQtyBatch4 = new FulfilledQuantityModel();
    fulfilledQtyBatch4.setBatchId("BATCH4");
    fulfilledQtyBatch4.setMaterialId(2L);
    fulfilledQtyBatch4.setFulfilledQuantity(new BigDecimal(100));
    fulfilledQuantityModelList.add(fulfilledQtyBatch4);

    FulfilledQuantityModel fulfilledQtyNonBatch = new FulfilledQuantityModel();
    fulfilledQtyNonBatch.setMaterialId(4L);
    fulfilledQtyNonBatch.setFulfilledQuantity(new BigDecimal(100));
    fulfilledQuantityModelList.add(fulfilledQtyNonBatch);

    FulfilledQuantityModel fulfilledQtyBatch5 = new FulfilledQuantityModel();
    fulfilledQtyBatch5.setBatchId("BATCH5");
    fulfilledQtyBatch5.setMaterialId(5L);
    fulfilledQtyBatch5.setFulfilledQuantity(new BigDecimal(100));
    fulfilledQuantityModelList.add(fulfilledQtyBatch5);

    FulfilledQuantityModel fulfilledQtyBatch6 = new FulfilledQuantityModel();
    fulfilledQtyBatch6.setBatchId("BATCH6");
    fulfilledQtyBatch6.setMaterialId(5L);
    fulfilledQtyBatch6.setFulfilledQuantity(new BigDecimal(100));
    fulfilledQuantityModelList.add(fulfilledQtyBatch6);

    FulfilledQuantityModel fulfilledQtyNonBatch2 = new FulfilledQuantityModel();
    fulfilledQtyNonBatch2.setMaterialId(6L);
    fulfilledQtyNonBatch2.setFulfilledQuantity(new BigDecimal(100));
    fulfilledQuantityModelList.add(fulfilledQtyNonBatch2);

    return fulfilledQuantityModelList;
  }

  public static List<ReturnsItemBatchVO> getAllBatches(List<ReturnsItemVO> returnsItemVOList){
    List<ReturnsItemBatchVO> returnsItemBatchVOList=new ArrayList<>();
    returnsItemVOList.forEach(returnsItemVO -> {
      if(returnsItemVO.hasBatches())
        returnsItemBatchVOList.addAll(returnsItemVO.getReturnItemBatches());
    });
    return returnsItemBatchVOList;
  }

  public static ReturnsTrackingDetailsVO getTrackingDetails(String jsonString) {
    return ReturnsGsonMapper.getTestObject(jsonString, ReturnsTrackingDetailsVO.class);
  }
}

