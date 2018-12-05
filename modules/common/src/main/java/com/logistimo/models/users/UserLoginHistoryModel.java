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

package com.logistimo.models.users;

import java.util.Date;
import lombok.Getter;

/**
 * Created by mohansrinivas on 10/18/16.
 */
@Getter
public class UserLoginHistoryModel {

  private final String referer;
  private final String userId;
  private final Integer lgSrc;
  private final String usrAgnt;
  private final String ipAddr;
  private final Date loginTime;
  private final String version;
  private final LoginStatus status;

  public UserLoginHistoryModel(String userId, Integer lgSrc, String usrAgnt, String ipAddr,
                               Date loginTime, String version) {
    this(userId,lgSrc,usrAgnt,ipAddr,loginTime,version,null, null);
  }

  public UserLoginHistoryModel(String userId, Integer lgSrc, String usrAgnt, String ipAddr,
                               Date loginTime, String version, LoginStatus status, String referer) {
    this.userId = userId;
    this.lgSrc = lgSrc;
    this.usrAgnt = usrAgnt;
    this.ipAddr = ipAddr;
    this.loginTime = loginTime;
    this.version = version;
    this.status = status;
    this.referer = referer;
  }
}
