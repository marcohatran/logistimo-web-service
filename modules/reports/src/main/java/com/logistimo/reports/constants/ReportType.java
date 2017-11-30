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

package com.logistimo.reports.constants;

public enum ReportType {

  INV_ABNORMAL_STOCK("ias", "Abnormal stock"),
  INV_REPELISHMENT("ir", "Replenishment response time"),
  INV_TRANSACTION_COUNT("itc", "Transaction counts"),
  INV_CONSUMPTION("ic", "Consumption"),
  INV_DISCARDS("id", "Discards"),
  INV_STOCK_AVAILABILITY("isa", "Stock availability"),
  INV_STOCK_TREND("ist", "Stock trends"),
  INV_SUPPLY("is", "Supply"),
  INV_UTILISATION("iu", "Utilization"),
  AS_CAPACITY("asa", "Asset capacity"),
  AS_POWER_AVAILABILITY("apa", "Power availability"),
  AS_RESPONSE_TIME("art", "Response time to repair"),
  AS_SICKNESS_RATE("asr", "Sickness rate"),
  AS_TEMPERATURE_EXCURSION("ate", "Temperature excursions"),
  AS_UP_TIME("aut", "Up time"),
  AS_ASSET_STATUS("aas", "Asset status"),
  ACTIVITY_USER("ua", "User activity");

  private String value;
  private String name;

  ReportType(String value, String name) {
    this.value = value;
    this.name = name;
  }

  public static ReportType getReportType(String value) {
    for (ReportType type : ReportType.values()) {
      if (type.value.equals(value)) {
        return type;
      }
    }
    return null;
  }

  public static String getReportName(String value) {
    for (ReportType type : ReportType.values()) {
      if (type.value.equals(value)) {
        return type.name;
      }
    }
    return "";
  }

  @Override
  public String toString() {
    return value;
  }
}
