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

package com.logistimo.api.controllers;

import com.logistimo.api.builders.ReturnsBuilder;
import com.logistimo.auth.utils.SecurityUtils;
import com.logistimo.config.models.DomainConfig;
import com.logistimo.constants.Constants;
import com.logistimo.exception.BadRequestException;
import com.logistimo.logger.XLog;
import com.logistimo.pagination.PageParams;
import com.logistimo.returns.Status;
import com.logistimo.returns.models.ReturnsModel;
import com.logistimo.returns.models.ReturnsUpdateStatusModel;
import com.logistimo.returns.models.ReturnsUpdateStatusRequestModel;
import com.logistimo.returns.models.ReturnsFilters;
import com.logistimo.returns.models.ReturnsModels;
import com.logistimo.returns.models.ReturnsRequestModel;
import com.logistimo.returns.models.UpdateStatusModel;
import com.logistimo.returns.service.ReturnsService;
import com.logistimo.returns.vo.ReturnsVO;
import com.logistimo.services.DuplicationException;
import com.logistimo.services.ServiceException;
import com.logistimo.utils.LocalDateUtil;

import org.apache.commons.lang.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

import java.text.ParseException;
import java.util.List;

import javax.validation.Valid;

/**
 * @author Mohan Raja
 */
@Controller
@RequestMapping("/returns")
public class ReturnController {

  private static final XLog xLogger = XLog.getLog(ReturnController.class);

  @Autowired
  ReturnsBuilder returnsBuilder;

  @Autowired
  ReturnsService returnsService;

  @RequestMapping(method = RequestMethod.POST)
  public
  @ResponseBody
  ReturnsModel create(@Valid @RequestBody ReturnsRequestModel returnsRequestModel)
      throws ServiceException {
    ReturnsVO returnsVO = returnsBuilder.buildReturns(returnsRequestModel);
    returnsVO=returnsService.createReturns(returnsVO);
    return returnsBuilder.buildMobileReturnsModel(returnsVO);
  }

  @RequestMapping(value = "/{returnId}/{status}", method = RequestMethod.POST)
  public
  @ResponseBody
  ReturnsUpdateStatusModel updateStatus(@PathVariable Long returnId,
                                              @PathVariable String status,
                                              @RequestBody ReturnsUpdateStatusRequestModel returnsUpdateStatusRequestModel)
      throws ServiceException, DuplicationException {
    UpdateStatusModel updateStatusModel=returnsBuilder.buildUpdateStatusModel(returnId,status,
        returnsUpdateStatusRequestModel);
    ReturnsVO returnsVO=returnsService.updateReturnsStatus(updateStatusModel);
    return returnsBuilder.buildMobileReturnsUpdateModel(returnsVO, returnsUpdateStatusRequestModel);
  }

  @RequestMapping(value = "/{returnId}", method = RequestMethod.GET)
  public
  @ResponseBody
  ReturnsModel get(@PathVariable Long returnId) throws ServiceException {
    ReturnsVO returnsVO=returnsService.getReturnsById(returnId);
    return returnsBuilder.buildMobileReturnsModel(returnsVO);
  }

  @RequestMapping(method = RequestMethod.GET)
  public
  @ResponseBody
  ReturnsModels getAll(@RequestParam(required = false) Long customerId,
                 @RequestParam(required = false) Long vendorId,
                 @RequestParam(required = false) String status,
                 @RequestParam(required = false) String startDate,
                 @RequestParam(required = false) String endDate,
                 @RequestParam(required = false) Long orderId,
                 @RequestParam(required = false, defaultValue = PageParams.DEFAULT_SIZE_STR) Integer size,
                 @RequestParam(required = false, defaultValue = PageParams.DEFAULT_OFFSET_STR) Integer offset
  ) throws ServiceException {
    try {
      DomainConfig dc = DomainConfig.getInstance(SecurityUtils.getCurrentDomainId());
      ReturnsFilters filters = new ReturnsFilters();
      filters.setCustomerId(customerId);
      filters.setVendorId(vendorId);
      filters.setStatus(Status.getStatus(status));
      if(StringUtils.isNotBlank(startDate)) {
        filters.setStartDate(
            LocalDateUtil.parseCustom(startDate, Constants.DATE_FORMAT, dc.getTimezone()));
      }
      if(StringUtils.isNotBlank(endDate)) {
        filters.setEndDate(LocalDateUtil.parseCustom(endDate, Constants.DATE_FORMAT,
            dc.getTimezone()));
      }
      filters.setOrderId(orderId);
      filters.setOffset(offset);
      filters.setSize(size);
      filters.setDomainId(SecurityUtils.getCurrentDomainId());
      filters.setLimitToUserKiosks(SecurityUtils.isManager());
      filters.setUserId(SecurityUtils.getUsername());
      List<ReturnsVO> returnsVOs = returnsService.getReturns(filters);
      Long totalCount = returnsService.getReturnsCount(filters);
      return new ReturnsModels(returnsBuilder.buildMobileReturnsModels(returnsVOs), totalCount);
    } catch (ParseException e) {
      xLogger.severe("Error while parsing date while getting returns on domain {0}",
          SecurityUtils.getCurrentDomainId(), e);
      throw new BadRequestException("Error while fetching all returns");
    }
  }

}
