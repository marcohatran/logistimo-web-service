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

package com.logistimo.inventory.models;

/**
 * Created by vani on 09/06/17.
 */
public class ErrorDetailModel {
  /**
   * Error code
   */
  public String errorCode;
  /**
   * Index or position from or until which the error specified by the error code occured
   */
  public int index;

  /**
   * error message
   */
  public String message;

  /**
   * Constructor
   * @param errorCode
   * @param index
   */
  public ErrorDetailModel(String errorCode, int index, String message) {
    this.errorCode = errorCode;
    this.index = index;
    this.message = message;
  }

  @Override
  public boolean equals(Object o) {
    if (o == this) {
      return true;
    }
    if (!(o instanceof ErrorDetailModel)) {
      return false;
    }
    ErrorDetailModel errorDetailModel = (ErrorDetailModel) o;
    return (errorCode == null ? errorDetailModel.errorCode == null : errorCode.equals(errorDetailModel.errorCode)) &&
        (index == errorDetailModel.index) &&
        (message == null ? errorDetailModel.message == null : message.equals(errorDetailModel.message));
  }

  @Override public int hashCode() {
    int result = 17;
    int prime = 31;
    result = prime * result + (errorCode == null ? 0 : errorCode.hashCode());
    result = prime * result + index;
    result = prime * result + (message == null ? 0 : message.hashCode());
    return result;
  }
}
