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

package com.logistimo.returns.service;

import com.logistimo.constants.Constants;
import com.logistimo.returns.entity.Returns;
import com.logistimo.returns.entity.ReturnsItem;
import com.logistimo.returns.entity.ReturnsItemBatch;
import com.logistimo.returns.models.ReturnsFilters;
import com.logistimo.returns.vo.ReturnsItemBatchVO;
import com.logistimo.returns.vo.ReturnsItemVO;
import com.logistimo.returns.vo.ReturnsVO;
import com.logistimo.utils.LocalDateUtil;

import org.modelmapper.ModelMapper;

import java.math.BigInteger;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Created by pratheeka on 13/03/18.
 */
@org.springframework.stereotype.Repository
public class ReturnsRepository extends Repository {

  private ModelMapper modelMapper = new ModelMapper();

  private static final String
      KIOSK_DOMAIN_QUERY =
      "(SELECT KD.KIOSKID_OID from KIOSK_DOMAINS KD WHERE DOMAIN_ID = :domainId)";

  private static final String
      USER_DOMAIN_QUERY =
      "(SELECT KIOSKID FROM USERTOKIOSK UD,KIOSK K WHERE K.KIOSKID=UD.KIOSKID AND UD.USERID=:userId)";


  public void saveReturns(ReturnsVO returnsVO) {
    Returns returns = modelMapper.map(returnsVO, Returns.class);
    returns = super.save(returns);
    returnsVO.setId(returns.getId());
  }

  public void updateReturns(ReturnsVO returnsVO) {
    Returns returns = modelMapper.map(returnsVO, Returns.class);
    returns = super.update(returns);
    returnsVO.setId(returns.getId());
  }

  public void saveReturnsItems(ReturnsItemVO returnsItemVO) {
    ReturnsItem returnsItem = modelMapper.map(returnsItemVO, ReturnsItem.class);
    returnsItem = super.save(returnsItem);
    returnsItemVO.setId(returnsItem.getId());
  }

  public void saveReturnBatchItems(ReturnsItemBatchVO returnsItemBatchVO) {
    ReturnsItemBatch returnsItemBatch = modelMapper.map(returnsItemBatchVO, ReturnsItemBatch.class);
    returnsItemBatch = super.save(returnsItemBatch);
    returnsItemBatchVO.setId(returnsItemBatch.getId());
  }

  public ReturnsVO getReturnsById(Long returnId) {
    Returns returns = super.findById(Returns.class, returnId);
    return modelMapper.map(returns, ReturnsVO.class);
  }

  public List<ReturnsItemVO> getReturnedItems(Long returnId) {
    Map<String, Object> filters = new HashMap<>(1);
    filters.put("returnsId", returnId);
    List<ReturnsItem> returnsItemList = super.findAll("ReturnsItem.findAllByReturnId", filters);
    List<ReturnsItemVO> returnsItemVOList =
        returnsItemList.stream().map(i -> modelMapper.map(i, ReturnsItemVO.class))
            .collect(Collectors.toList());
    returnsItemVOList.forEach(returnsItem -> {
      filters.clear();
      filters.put("itemId", returnsItem.getId());
      List<ReturnsItemBatch> returnsItemBatchList =
          super.findAll("ReturnsItemBatch.findByItemId", filters);
      List<ReturnsItemBatchVO> returnsItemBatchVOList =
          returnsItemBatchList.stream().map(i -> modelMapper.map(i, ReturnsItemBatchVO.class))
              .collect(Collectors.toList());
      returnsItem.setReturnItemBatches(returnsItemBatchVOList);
    });

    return returnsItemVOList;
  }

  public List<ReturnsVO> getReturns(ReturnsFilters returnsFilters) {
    Map<String, Object> filters = new HashMap<>();
    StringBuilder query = new StringBuilder("select * from `RETURNS` r where ");
    buildQuery(returnsFilters, filters, query);
    List<Returns> returnsList =
        super.findAllByNativeQuery(query.toString(), filters, Returns.class, returnsFilters.getSize(),
            returnsFilters.getOffset());
    return returnsList.stream().map(returns ->
        modelMapper.map(returns, ReturnsVO.class)).collect(Collectors.toList());
  }

  public Long getReturnsCount(ReturnsFilters returnsFilters){
    Map<String, Object> filters = new HashMap<>();
    StringBuilder query = new StringBuilder("select COUNT(1) from `RETURNS` r where ");
    buildQuery(returnsFilters, filters, query);
    return ((BigInteger)super.findByNativeQuery(query.toString(), filters)).longValue();
  }

  private void buildQuery(ReturnsFilters returnsFilters, Map<String, Object> filters,
                          StringBuilder query) {
    if (!returnsFilters.hasVendorId() && !returnsFilters.hasCustomerId()) {
      if (returnsFilters.isLimitToUserKiosks()) {
        query.append(" (r.customer_id IN ").append(USER_DOMAIN_QUERY).append(" OR r.vendor_id IN ")
            .append(USER_DOMAIN_QUERY).append(")");
        filters.put("userId", returnsFilters.getUserId());
      } else {
        query.append(" (r.customer_id IN ").append(KIOSK_DOMAIN_QUERY).append(" OR r.vendor_id IN ")
            .append(KIOSK_DOMAIN_QUERY).append(")");
        filters.put("domainId", returnsFilters.getDomainId());
      }
    } else {
      if (returnsFilters.hasVendorId()) {
        query.append(" r.vendor_id=:vendorId");
        filters.put("vendorId", returnsFilters.getVendorId());
      } else if (returnsFilters.hasCustomerId()) {
        query.append(" r.customer_id=:customerId");
        filters.put("customerId", returnsFilters.getCustomerId());
      }
    }
    if (returnsFilters.hasOrderId()) {
      query.append(" and r.order_id=:orderId");
      filters.put("orderId", returnsFilters.getOrderId());
    }
    if (returnsFilters.hasStatus()) {
      query.append("  and  r.status=:status");
      filters.put("status", returnsFilters.getStatus().name());
    }
    if (returnsFilters.hasStartDate()) {
      query.append(" and r.created_at>=:fromDate");
      filters.put("fromDate", LocalDateUtil.formatCustom(returnsFilters.getStartDate(),
          Constants.DATETIME_CSV_FORMAT,null));
    }
    if (returnsFilters.hasEndDate()) {
      Date untilDate =
          LocalDateUtil.getOffsetDate(returnsFilters.getEndDate(), 1, Calendar.DAY_OF_MONTH);
      query.append(" and r.created_at<=:endDate");
      filters.put("endDate", LocalDateUtil.formatCustom(untilDate, Constants.DATETIME_CSV_FORMAT,
          null));
    }

    query.append(" order by r.id desc");
  }

}
