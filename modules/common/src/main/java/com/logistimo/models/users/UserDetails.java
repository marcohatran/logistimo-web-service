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

package com.logistimo.models.users;

import org.springframework.util.StringUtils;

import java.util.Date;
import java.util.List;

import lombok.Data;

/**
 * Created by kumargaurav on 13/08/18.
 */
@Data
public class UserDetails {

  private String userId;

  private Long sdId; // source Domain ID

  private String encodedPassword;

  private String role;

  private String firstName;

  private String lastName;

  private String mobilePhoneNumber;

  private String email;

  private String landPhoneNumber;

  private Date lastLogin;

  private Date memberSince;

  private Date uo;

  private String ub;

  private int atexp;

  private boolean isEnabled;

  private String language;

  private String timezone;

  private String usrAgnt;

  private String prevUsrAgnt;

  private String ipAddr;

  private List<String> tags;

  private Integer lgSrc;

  private String password;

  private String salt;

  private String token;

  private String tokenExpiry;

  private String requestId;

  public boolean hasSalt() {
    return !StringUtils.isEmpty(getSalt());
  }
}
