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
import com.logistimo.returns.models.MobileReturnsModel;
import com.logistimo.returns.models.ReturnsRequestModel;
import com.logistimo.returns.models.MobileReturnsUpdateStatusModel;
import com.logistimo.returns.models.MobileReturnsUpdateStatusRequestModel;
import com.logistimo.returns.models.UpdateStatusModel;
import com.logistimo.returns.service.ReturnsService;
import com.logistimo.returns.vo.ReturnsVO;
import com.logistimo.services.ServiceException;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;

import javax.validation.Valid;

/**
 * @author Mohan Raja
 */
@Controller
@RequestMapping("/returns")
public class ReturnController {

  @Autowired
  ReturnsBuilder returnsBuilder;

  @Autowired
  ReturnsService returnsService;

  @RequestMapping(method = RequestMethod.POST)
  public
  @ResponseBody
  MobileReturnsModel create(@Valid @RequestBody ReturnsRequestModel returnsRequestModel)
      throws ServiceException {
    ReturnsVO returnsVO = returnsBuilder.buildReturns(returnsRequestModel);
    returnsVO=returnsService.createReturns(returnsVO);
    return returnsBuilder.buildMobileReturnsModel(returnsVO);
  }

  @RequestMapping(value = "/{returnId}/{status}", method = RequestMethod.POST)
  public
  @ResponseBody
  MobileReturnsUpdateStatusModel updateStatus(@PathVariable Long returnId,
                                              @PathVariable String status,
                                              @RequestBody MobileReturnsUpdateStatusRequestModel mobileReturnsUpdateStatusRequestModel)
      throws ServiceException {
    UpdateStatusModel updateStatusModel=returnsBuilder.buildUpdateStatusModel(returnId,status,mobileReturnsUpdateStatusRequestModel);
    ReturnsVO returnsVO=returnsService.updateReturnsStatus(updateStatusModel);
    return returnsBuilder.buildMobileReturnsUpdateModel(returnsVO, mobileReturnsUpdateStatusRequestModel);
  }

  @RequestMapping(value = "/{returnId}", method = RequestMethod.GET)
  public
  @ResponseBody
  MobileReturnsModel get(@PathVariable Long returnId) throws ServiceException {
    ReturnsVO returnsVO=returnsService.getReturnsById(returnId);
    return returnsBuilder.buildMobileReturnsModel(returnsVO);
  }

}
