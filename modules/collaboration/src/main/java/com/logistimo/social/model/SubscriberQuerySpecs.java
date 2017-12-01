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

package com.logistimo.social.model;

/**
 * Created by kumargaurav on 26/11/17.
 */
public class SubscriberQuerySpecs {
  private String objid;
  private String objty;
  private String contxtid;
  private String contxtty;
  private String subcontxtty;
  private String userid;

  public SubscriberQuerySpecs(String objid, String objty, String contxtid, String contxtty,
                              String userid) {
    this.objid = objid;
    this.objty = objty;
    this.contxtid = contxtid;
    this.contxtty = contxtty;
    this.userid = userid;
  }

  public String getObjid() {
    return this.objid;
  }

  public String getObjty() {
    return this.objty;
  }

  public String getContxtid() {
    return this.contxtid;
  }

  public String getContxtty() {
    return this.contxtty;
  }

  public String getSubcontxtty() {
    return this.subcontxtty;
  }

  public String getUserid() {
    return this.userid;
  }

  public void setObjid(String objid) {
    this.objid = objid;
  }

  public void setObjty(String objty) {
    this.objty = objty;
  }

  public void setContxtid(String contxtid) {
    this.contxtid = contxtid;
  }

  public void setContxtty(String contxtty) {
    this.contxtty = contxtty;
  }

  public void setSubcontxtty(String subcontxtty) {
    this.subcontxtty = subcontxtty;
  }

  public void setUserid(String userid) {
    this.userid = userid;
  }
}

