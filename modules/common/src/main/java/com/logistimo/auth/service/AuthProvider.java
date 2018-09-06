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

package com.logistimo.auth.service;

import com.logistimo.models.users.UserDetails;
import com.logistimo.services.ObjectNotFoundException;
import com.logistimo.services.ServiceException;

/**
 * Created by kumargaurav on 10/08/18.
 */
public interface AuthProvider {

  /**
   * Authenticate a user in the context of a given domain and password
   */
  UserDetails authenticate(String userId, String password);

  /**
   * Change the password of a given user
   */
  void changePassword(String userId, String oldPassword, String newPassword,boolean isEnhanced) throws ServiceException;

  /**
   * Enable a previously disabled user account (pass in fully qualified user Id - i.e. domainId.userId)
   */
  void enableAccount(String userId) throws ServiceException;

  /**
   * Disable a user account (pass in fully qualified user Id - i.e. domainId.userId)
   */
  void disableAccount(String userId) throws ServiceException;

  /**
   *
   * @param userId
   * @return user's salt
   */
  String getUserSalt(String userId);

}
