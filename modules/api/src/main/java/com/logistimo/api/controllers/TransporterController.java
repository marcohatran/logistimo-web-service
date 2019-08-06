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

package com.logistimo.api.controllers;

import com.logistimo.auth.Authorize;
import com.logistimo.auth.Role;
import com.logistimo.auth.utils.SecurityUtils;
import com.logistimo.exception.ForbiddenAccessException;
import com.logistimo.models.ResponseModel;
import com.logistimo.pagination.Results;
import com.logistimo.security.SecureUserDetails;
import com.logistimo.services.ObjectNotFoundException;
import com.logistimo.services.Resources;
import com.logistimo.services.ServiceException;
import com.logistimo.transporters.model.TransporterDetailsModel;
import com.logistimo.transporters.model.TransporterModel;
import com.logistimo.transporters.service.TransporterService;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Objects;
import java.util.ResourceBundle;

@RestController
@RequestMapping("/transporters")
public class TransporterController {

  @Autowired
  private TransporterService transporterService;

  @RequestMapping(method = RequestMethod.GET)
  public ResponseEntity getTransporters(@RequestParam(required = false) Integer page,
                                        @RequestParam(required = false) Integer size,
                                        @RequestParam(required = false, name = "q") String searchName,
                                        @RequestParam(defaultValue = "false", required = false,
                                            name = "api_enabled") boolean apiEnabled) {
    Long domainId = SecurityUtils.getDomainId();
    Results<TransporterModel> transporters = transporterService.getTransporters(domainId, apiEnabled,
        searchName, page, size);
    return new ResponseEntity<>(transporters, HttpStatus.OK);
  }

  @RequestMapping(method = RequestMethod.POST)
  @Authorize(role= Role.ADMIN)
  public ResponseEntity createTransporter(@RequestBody TransporterDetailsModel model) {
    SecureUserDetails sUser = SecurityUtils.getUserDetails();
    ResourceBundle backendMessages = Resources.getBundle("BackendMessages", sUser.getLocale());
    transporterService.createTransporter(model, sUser.getUsername(), SecurityUtils.getDomainId());
    return new ResponseEntity<>(new ResponseModel(true,
        backendMessages.getString("transporter.success.create")), HttpStatus.OK);
  }

  @RequestMapping(value = "/{tid}", method = RequestMethod.PUT)
  @Authorize(role= Role.ADMIN)
  public ResponseEntity updateTransporter(@PathVariable Long tid ,@RequestBody
  TransporterDetailsModel model) throws ServiceException {
    SecureUserDetails sUser = SecurityUtils.getUserDetails();
    ResourceBundle backendMessages = Resources.getBundle("BackendMessages", sUser.getLocale());
    transporterService.updateTransporter(tid, model, sUser.getUsername());
    return new ResponseEntity<>(new ResponseModel(true,
        backendMessages.getString("transporter.success.update")), HttpStatus.OK);
  }

  @RequestMapping(value = "/{transporterIds}", method = RequestMethod.DELETE)
  @Authorize(role= Role.ADMIN)
  public ResponseEntity deleteTransporters(@PathVariable String transporterIds) throws ServiceException {
    SecureUserDetails sUser = SecurityUtils.getUserDetails();
    ResourceBundle backendMessages = Resources.getBundle("BackendMessages", sUser.getLocale());
    transporterService.deleteTransporters(transporterIds);
    return new ResponseEntity<>(new ResponseModel(true,
        backendMessages.getString("transporter.success.delete")), HttpStatus.OK);
  }

  @RequestMapping(value = "/{id}", method = RequestMethod.GET)
  public ResponseEntity getTransporterDetail(@PathVariable Long id) {
    TransporterDetailsModel model = transporterService.getTransporterDetails(id);
    if(model == null) {
      ResourceBundle backendMessages = Resources.getBundle("BackendMessages",
          SecurityUtils.getUserDetails().getLocale());
      throw new ObjectNotFoundException(backendMessages.getString("transporter.not.found"));
    }
    if(!Objects.equals(model.getSourceDomainId(), SecurityUtils.getDomainId())) {
      throw new ForbiddenAccessException("Forbidden");
    }
    return new ResponseEntity<>(model, HttpStatus.OK);
  }
}