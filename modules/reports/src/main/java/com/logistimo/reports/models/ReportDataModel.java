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

package com.logistimo.reports.models;

/**
 * Created by mohan on 31/03/17.
 */
public class ReportDataModel {

  public ReportDataModel(String value) {
    this.value = value;
  }
  public ReportDataModel(String value, String secValue) {
    this.value = value;
    this.secValue = secValue;
  }

  public ReportDataModel(String value, String num, String den) {
    this.value = value;
    this.num = num;
    this.den = den;
  }
  public ReportDataModel(String value, String num, String den, String secValue, String secNum, String secDen) {
    this.value = value;
    this.num = num;
    this.den = den;
    this.secValue = secValue;
    this.secNum = secNum;
    this.secDen = secDen;
  }
  public String value;
  public String num;
  public String den;
  public String secValue;
  public String secNum;
  public String secDen;

  @Override
  public boolean equals(Object o) {
    if (o == this) {
      return true;
    }
    if (!(o instanceof ReportDataModel)) {
      return false;
    }
    ReportDataModel reportDataModel = (ReportDataModel) o;
    return (value == null ? reportDataModel.value == null : value.equals(reportDataModel.value)) &&
        (num == null ? reportDataModel.num == null : num.equals(reportDataModel.num)) &&
        (den == null ? reportDataModel.den == null : den.equals(reportDataModel.den)) &&
        (secValue == null ? reportDataModel.secValue == null : secValue.equals(reportDataModel.secValue)) &&
        (secNum == null ? reportDataModel.secNum == null : secNum.equals(reportDataModel.secNum)) &&
        (secDen == null ? reportDataModel.secDen == null : secDen.equals(reportDataModel.secDen));
  }

  @Override public int hashCode() {
    int result = 17;
    int prime = 31;
    result = prime * result + (value == null ? 0 : value.hashCode());
    result = prime * result + (num == null ? 0 : num.hashCode());
    result = prime * result + (den == null ? 0 : den.hashCode());
    result = prime * result + (secValue == null ? 0 : secValue.hashCode());
    result = prime * result + (secNum == null ? 0 : secNum.hashCode());
    result = prime * result + (secDen == null ? 0 : secDen.hashCode());
    return result;
  }
}
