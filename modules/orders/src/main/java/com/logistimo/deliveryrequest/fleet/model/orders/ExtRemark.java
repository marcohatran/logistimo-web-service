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

package com.logistimo.deliveryrequest.fleet.model.orders;

import com.google.gson.annotations.SerializedName;

/**
 * Created by chandrakant on 25/06/19.
 */
class ExtRemark
{
  public static final String SYS_RMK = "SYS";
  public static final String SYS_RMK_FOR_CSD_SUB_KEY = "CSD.SYS";
  public static final String[] GET_KEY = new String[] { "getKey", "getKey" };

  public static final String INSTR_PREFIX = "INST.";   // Prefix of Remark [Type = instruction]

  public ExtRemark(String instructions) {
    this.key = new Key("ORD", INSTR_PREFIX + "PICKED");
    this.remark = new Remark(instructions);
  }

  private Key key;
  @SerializedName("rmk")
  private Remark remark;

  public static class Key
  {
    @SerializedName("k_typ")
    private String keyType;
    @SerializedName("s_key")
    private String subKeyType;

    public Key(String keyType, String subKeyType)
    {
      this.keyType = keyType;
      this.subKeyType = subKeyType;
    }
  }

  public static class Remark
  {
    @SerializedName("rjn")
    private Integer code;
    @SerializedName("rmk")
    private String remarks;

    public Remark(String remarks)
    {
      this.remarks = remarks;
    }
  }
}