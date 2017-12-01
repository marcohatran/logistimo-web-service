/*
 * Copyright © 2017 Logistimo.
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

/**
 * Created by kumargaurav on 25/11/17.
 */

import com.logistimo.auth.utils.SecurityUtils;
import com.logistimo.pagination.PageParams;
import com.logistimo.security.SecureUserDetails;
import com.logistimo.services.ServiceException;
import com.logistimo.social.action.GetLikeAction;
import com.logistimo.social.action.GetLikerAction;
import com.logistimo.social.action.RegisterLikeAction;
import com.logistimo.collaboration.core.models.RegisterLikeRequestModel;
import com.logistimo.collaboration.core.models.RegisterLikeResponseModel;
import com.logistimo.social.model.LSLikeResponseModel;
import com.logistimo.social.model.LSLikerResponseModel;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

import javax.servlet.http.HttpServletRequest;

/**
 * Created by kumargaurav on 15/11/17.
 */
@Controller
@RequestMapping("/collaboration")
public class CollaborationController {

  RegisterLikeAction registerLikeAction;

  GetLikeAction getLikeAction;

  GetLikerAction getLikerAction;

  @Autowired
  public void setRegisterLikeAction(RegisterLikeAction registerLikeAction) {
    this.registerLikeAction = registerLikeAction;
  }

  @Autowired
  public void setGetLikeAction(GetLikeAction getLikeAction) {
    this.getLikeAction = getLikeAction;
  }

  @Autowired
  public void setGetLikerAction(GetLikerAction getLikerAction) {
    this.getLikerAction = getLikerAction;
  }

  @RequestMapping(value = "/likes", method = RequestMethod.POST)
  public
  @ResponseBody
  RegisterLikeResponseModel registerLike(@RequestBody RegisterLikeRequestModel requestModel)
      throws ServiceException {
    SecureUserDetails sUser = SecurityUtils.getUserDetails();
    return registerLikeAction.invoke(requestModel, sUser);
  }


  @RequestMapping(value = "/likes/{object_type}/{object_id}/likers", method = RequestMethod.GET)
  public
  @ResponseBody
  LSLikerResponseModel getLikers(@PathVariable String object_type, @PathVariable String object_id,
                                 @RequestParam(defaultValue = PageParams.DEFAULT_OFFSET_STR) int offset,
                                 @RequestParam(defaultValue = PageParams.DEFAULT_SIZE_STR) int size) {

    return getLikerAction.invoke(object_id, object_type, null, offset, size);
  }


  @RequestMapping(value = "/likes/{object_type}/{object_id}/{context_id}/likers", method = RequestMethod.GET)
  public
  @ResponseBody
  LSLikerResponseModel getLikers(@PathVariable String object_type, @PathVariable String object_id,
                                 @PathVariable String context_id,
                                 @RequestParam(defaultValue = PageParams.DEFAULT_OFFSET_STR) int offset,
                                 @RequestParam(defaultValue = PageParams.DEFAULT_SIZE_STR) int size) {

    return getLikerAction.invoke(object_id, object_type, context_id, offset, size);
  }

  @RequestMapping(value = "/likes", method = RequestMethod.GET)
  public
  @ResponseBody
  LSLikeResponseModel getLikes(@RequestParam String object_type,
                               @RequestParam String object_id,
                               @RequestParam(required = false) String context_id,
                               @RequestParam(defaultValue = "false") boolean count,
                               @RequestParam(defaultValue = PageParams.DEFAULT_OFFSET_STR) int offset,
                               @RequestParam(defaultValue = PageParams.DEFAULT_SIZE_STR) int size) {

    return getLikeAction.invoke(object_id, object_type, context_id, count, offset, size);
  }

}
