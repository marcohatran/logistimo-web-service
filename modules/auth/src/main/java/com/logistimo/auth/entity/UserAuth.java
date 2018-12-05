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

package com.logistimo.auth.entity;

import com.logistimo.utils.PasswordEncoder;

import org.springframework.util.StringUtils;

import java.util.Date;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Table;

import lombok.Data;

/**
 * Created by kumargaurav on 13/08/18.
 */
@Entity
@Table(name="USERACCOUNT")
@Data
public class UserAuth {

  @Id
  @Column(name = "USERID", updatable = false)
  private String userId;

  @Column(name = "ENCODEDPASSWORD")
  private String encodedPassword;

  @Column(name="PASSWORD")
  private String password;

  @Column(name="SALT")
  private String salt;

  @Column(name="NNAME")
  private String name;

  @Column(name="FIRSTNAME")
  private String firstName;

  @Column(name="LASTNAME")
  private String lastName;

  @Column(name="GENDER")
  private String gender;

  @Column(name="EMAIL")
  private String email;

  @Column(name = "SDID")
  private Long sourceDomain;

  @Column(name = "MOBILEPHONENUMBER")
  private String mobile;

  @Column(name = "LANDPHONENUMBER")
  private String phone;

  @Column(name = "LANGUAGE")
  private String language;

  @Column(name = "TIMEZONE")
  private String timezone;

  @Column(name = "LASTLOGIN")
  private Date lastLogin;

  @Column(name = "ISENABLED")
  private boolean enabled;

  @Column(name = "USRAGNT")
  private String userAgent;

  @Column(name = "PREVUSRAGNT")
  private String prevUserAgent;

  @Column(name = "IPADDR")
  private String ipAddress;

  @Column(name = "LGSRC")
  private Integer loginSource;

  @Column(name = "UB")
  private String updateBy;

  @Column(name = "UO")
  private Date updatedOn;

  @Column(name = "ROLE")
  private String role;

  @Column(name = "V")
  private String version;

  public boolean hasSalt() {
    return !StringUtils.isEmpty(getSalt());
  }
}
