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

package com.logistimo.inventory.models;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by vani on 15/02/18.
 */
public class SuccessDetailModel {
  /**
   * Success code
   */
  public String successCode;
  /**
   * Index or position from or until which the transactions are successful
   */
  public int index;

  /**
   * List of successful transaction keys
   */
  public List keys = new ArrayList<>(1);

  /**
   * Constructor
   * @param successCode
   * @param index
   */
  public SuccessDetailModel(String successCode, int index, List<String> keys) {
    this.successCode = successCode;
    this.index = index;
    this.keys.addAll(keys);
  }

  @Override
  public boolean equals(Object o) {
    if (o == this) {
      return true;
    }
    if (!(o instanceof SuccessDetailModel)) {
      return false;
    }
    SuccessDetailModel successDetailModel = (SuccessDetailModel) o;
    return (successCode == null ? successDetailModel.successCode == null : successCode.equals(successDetailModel.successCode)) &&
        (index == successDetailModel.index) &&
        (keys == null ? successDetailModel.keys == null : keys.equals(successDetailModel.keys));
  }

  @Override public int hashCode() {
    int result = 17;
    int prime = 31;
    result = prime * result + (successCode == null ? 0 : successCode.hashCode());
    result = prime * result + index;
    result = prime * result + (keys == null ? 0 : keys.hashCode());
    return result;
  }
}
