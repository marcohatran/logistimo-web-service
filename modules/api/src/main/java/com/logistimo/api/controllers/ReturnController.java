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
import com.logistimo.returns.models.ReturnsFilters;
import com.logistimo.returns.models.ReturnsModel;
import com.logistimo.returns.models.ReturnsModels;
import com.logistimo.returns.models.ReturnsQuantityModel;
import com.logistimo.returns.models.ReturnsRequestModel;
import com.logistimo.returns.models.ReturnsUpdateRequestModel;
import com.logistimo.returns.models.ReturnsUpdateStatusModel;
import com.logistimo.returns.models.submodels.ReturnsTrackingModel;
import com.logistimo.returns.service.ReturnsService;
import com.logistimo.returns.vo.ReturnsQuantityVO;
import com.logistimo.returns.vo.ReturnsTrackingDetailsVO;
import com.logistimo.returns.vo.ReturnsVO;
import com.logistimo.security.SecureUserDetails;
import com.logistimo.services.DuplicationException;
import com.logistimo.services.ServiceException;
import com.logistimo.utils.LocalDateUtil;

import org.apache.commons.lang.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.text.ParseException;
import java.util.List;

import javax.validation.Valid;

/**
 * @author Mohan Raja
 */
@RestController
@RequestMapping("/returns")
public class ReturnController {

  private static final XLog xLogger = XLog.getLog(ReturnController.class);

  @Autowired
  ReturnsBuilder returnsBuilder;

  @Autowired
  ReturnsService returnsService;

  @RequestMapping(method = RequestMethod.POST)
  public ReturnsModel create(@Valid @RequestBody ReturnsRequestModel returnsRequestModel)
      throws ServiceException, ParseException {
    SecureUserDetails sUser = SecurityUtils.getUserDetails();
    ReturnsVO returnsVO = returnsBuilder.buildReturns(returnsRequestModel);
    returnsVO = returnsService.createReturns(returnsVO);
    xLogger.info("AUDITLOG\t{0}\t{1}\tRETURN\t " +
        "CREATED\t{2}", sUser.getCurrentDomainId(), sUser.getUsername(), returnsVO.getId());
    return returnsBuilder.buildReturnsModel(returnsVO);
  }

  @RequestMapping(value = "/{returnId}/{status}", method = RequestMethod.POST)
  public ReturnsUpdateStatusModel updateStatus(@PathVariable Long returnId,
                                               @PathVariable String status,
                                               @RequestBody ReturnsUpdateRequestModel returnsUpdateRequestModel)
      throws ServiceException, DuplicationException, ParseException {
    SecureUserDetails sUser = SecurityUtils.getUserDetails();
    ReturnsVO returnsVO =
        returnsBuilder.buildReturnsVO(returnId, status, returnsUpdateRequestModel, null, null);
    returnsVO = returnsService.updateReturnsStatus(returnsVO, sUser.getCurrentDomainId());
    xLogger.info("AUDITLOG\t{0}\t{1}\tRETURN\t " +
        "STATUS UPDATED\t{2}\t{3}", sUser.getCurrentDomainId(), sUser.getUsername(), returnsVO.getId(), status);
    return returnsBuilder.buildMobileReturnsUpdateModel(returnsVO, returnsUpdateRequestModel);
  }

  @RequestMapping(value = "/{returnId}", method = RequestMethod.GET)
  public ReturnsModel get(@PathVariable Long returnId) throws ServiceException, ParseException {
    ReturnsVO returnsVO = returnsService.getReturn(returnId);
    return returnsBuilder.buildReturnsModel(returnsVO);
  }

  @RequestMapping(value = "/{returnId}/update-items", method = RequestMethod.POST)
  public void updateReturnItems(@PathVariable Long returnId,
                                @RequestBody ReturnsUpdateRequestModel returnsUpdateRequestModel)
      throws ServiceException, ParseException {
    final ReturnsVO returns = returnsService.getReturn(returnId);
    final ReturnsVO returnsVO =
        returnsBuilder.buildReturnsVO(returnId, null, returnsUpdateRequestModel, returns.getCustomerId(), returns.getVendorId());
    returnsVO.setStatus(returns.getStatus());
    returnsVO.setReturnsTrackingDetailsVO(returns.getReturnsTrackingDetailsVO());
    returnsService.updateReturnItems(returnsVO);
  }

  @RequestMapping(value = "/{returnId}/tracking-details", method = RequestMethod.POST)
  public ReturnsTrackingModel saveTrackingDetails(@PathVariable Long returnId,
                                                  @RequestBody ReturnsTrackingModel model)
      throws ParseException {
    final ReturnsTrackingDetailsVO returnsTrackingDetailsVO =
        returnsBuilder.buildTrackingDetailsVO(model);
    final ReturnsTrackingDetailsVO updatedReturnsTrackingDetailsVO =
        returnsService.saveTransporterDetails(returnsTrackingDetailsVO, returnId);
    return returnsBuilder.buildTrackingDetails(updatedReturnsTrackingDetailsVO);
  }

  @RequestMapping(value = "/order/{orderId}", method = RequestMethod.GET)
  public List<ReturnsQuantityModel> getReturnsQuantityDetailsByOrder(@PathVariable Long orderId)
      throws ServiceException {
    final List<ReturnsQuantityVO> returnsQuantityDetails =
        returnsService.getReturnsQuantityDetails(orderId);
    return returnsBuilder.buildReturnsQuantityModels(returnsQuantityDetails);
  }

  @RequestMapping(method = RequestMethod.GET)
  public ReturnsModels getAll(@RequestParam(required = false) Long customerId,
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
      ReturnsFilters filters = ReturnsFilters.builder()
          .customerId(customerId)
          .vendorId(vendorId)
          .status(Status.getStatus(status))
          .orderId(orderId)
          .offset(offset)
          .size(size).domainId(SecurityUtils.getCurrentDomainId())
          .limitToUserKiosks(SecurityUtils.isManager())
          .userId(SecurityUtils.getUsername())
          .startDate(StringUtils.isNotBlank(startDate) ? LocalDateUtil
              .parseCustom(startDate, Constants.DATE_FORMAT, dc.getTimezone()) : null)
          .endDate(StringUtils.isNotBlank(endDate) ? LocalDateUtil
              .parseCustom(endDate, Constants.DATE_FORMAT, dc.getTimezone()) : null)
          .build();
      List<ReturnsVO> returnsVOs = returnsService.getReturns(filters, false);
      Long totalCount = returnsService.getReturnsCount(filters);
      return new ReturnsModels(returnsBuilder.buildReturnsModels(returnsVOs), totalCount);
    } catch (ParseException e) {
      xLogger.severe("Error while parsing date while getting returns on domain {0}",
          SecurityUtils.getCurrentDomainId(), e);
      throw new BadRequestException("Error while fetching all returns");
    }
  }
}
