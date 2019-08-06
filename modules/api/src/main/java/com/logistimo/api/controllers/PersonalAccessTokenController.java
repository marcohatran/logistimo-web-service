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

import com.logistimo.api.models.APIRequest;
import com.logistimo.auth.service.AuthenticationService;
import com.logistimo.auth.utils.SecurityUtils;
import com.logistimo.models.users.PersonalAccessToken;
import com.logistimo.pagination.Results;
import com.logistimo.services.ServiceException;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.ResponseStatus;

import java.util.List;

import lombok.AllArgsConstructor;

@Controller
@RequestMapping("/users/personal-access-token")
@AllArgsConstructor
public class PersonalAccessTokenController {

  private AuthenticationService authenticationService;

  @RequestMapping(value = "", method = RequestMethod.POST)
  public
  @ResponseBody
  PersonalAccessToken create(@RequestBody APIRequest<String> apiRequest) throws ServiceException {
    return authenticationService.generatePersonalAccessToken(apiRequest.getPayload());
  }

  @RequestMapping(value = "", method = RequestMethod.GET)
  public
  @ResponseBody
  Results<PersonalAccessToken> get() {
    List<PersonalAccessToken> personalAccessTokens = authenticationService.getPersonalAccessTokens(SecurityUtils.getUsername(), SecurityUtils.getDomainId());
    return new Results<>(personalAccessTokens, personalAccessTokens.size(), 0);
  }


  @RequestMapping(value = "/{token}", method = RequestMethod.DELETE)
  public
  @ResponseStatus(HttpStatus.NO_CONTENT)
  void delete(@PathVariable String token) {
    authenticationService.deleteToken(token);
  }

}
