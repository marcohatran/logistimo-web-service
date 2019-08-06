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

package com.logistimo.users.entity;

import org.apache.commons.lang.StringUtils;

import java.util.Date;

import javax.jdo.annotations.Column;
import javax.jdo.annotations.IdentityType;
import javax.jdo.annotations.NotPersistent;
import javax.jdo.annotations.PersistenceCapable;
import javax.jdo.annotations.Persistent;
import javax.jdo.annotations.PrimaryKey;

/**
 * Created by naveensnair on 03/11/15.
 */
@PersistenceCapable(identityType = IdentityType.APPLICATION)
public class UserToken implements IUserToken {
  @PrimaryKey
  @Persistent
  private String token;
  @Persistent
  private String userId;
  @Persistent
  private Date expires;
  @Persistent
  private Long domainId;

  @Persistent
  private String accessKey;

  @NotPersistent
  private String rawToken;

  @Persistent
  @Column(name = "TOKEN_TYPE")
  private TokenType tokenType;

  @Persistent
  @Column(name = "CREATED_ON")
  private Date createdOn;

  @Persistent
  private String description;

  public String getUserId() {
    return userId;
  }

  public void setUserId(String userId) {
    this.userId = userId;
  }

  public String getToken() {
    return token;
  }

  public void setToken(String token) {
    this.token = token;
  }

  public Date getExpires() {
    return expires;
  }

  public void setExpires(Date expires) {
    this.expires = expires;
  }

  public Long getDomainId() {
    return domainId;
  }

  public void setDomainId(Long domainId) {
    this.domainId = domainId;
  }

  @Override
  public String getRawToken() {
    return rawToken;
  }

  @Override
  public void setRawToken(String rawToken) {
    this.rawToken = rawToken;
  }

  @Override
  public String getAccessKey() {
    return accessKey;
  }

  @Override
  public void setAccessKey(String accessKey) {
    this.accessKey = accessKey;
  }

  @Override
  public boolean hasAccessKey() {
    return StringUtils.isNotEmpty(accessKey) && !"NULL".equals(accessKey);
  }

  @Override
  public void setTokenType(TokenType tokenType) {
    this.tokenType = tokenType;
  }

  @Override
  public TokenType getTokenType() {
    return tokenType;
  }

  @Override
  public Date getCreatedOn() {
    return createdOn;
  }

  @Override
  public void setCreatedOn(Date createdOn) {
    this.createdOn = createdOn;
  }

  @Override
  public String getDescription() {
    return description;
  }

  @Override
  public void setDescription(String description) {
    this.description = description;
  }
}
