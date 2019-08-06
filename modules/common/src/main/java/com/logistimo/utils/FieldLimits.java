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

package com.logistimo.utils;

/**
 * Created by vani on 07/07/17.
 */
public class FieldLimits {
  private FieldLimits() {
  }

  // Limits for fields
  public static final int USERID_MIN_LENGTH = 4;
  public static final int USERID_MAX_LENGTH = 20;
  public static final int FIRSTNAME_MIN_LENGTH = 1;
  public static final int MOBILE_PHONE_MAX_LENGTH = 20;
  public static final int LAND_PHONE_MAX_LENGTH = 20;
  public static final int MAX_USER_AGE = 100;
  public static final int STREET_ADDRESS_MAX_LENGTH = 200;
  public static final int TEXT_FIELD_MAX_LENGTH = 50;
  public static final int MATERIAL_NAME_MAX_LENGTH = 150;
  public static final int EMAIL_MAX_LENGTH = 100;
  public static final double LATITUDE_MIN = -90;
  public static final double LATITUDE_MAX = 90;
  public static final int LAT_LONG_MAX_DIGITS_AFTER_DECIMAL = 8;
  public static final double LONGITUDE_MIN = -180;
  public static final double LONGITUDE_MAX = 180;
  public static final double TEMP_MAX_VALUE = 99.99;
  public static final double TEMP_MIN_VALUE = -99.99;
  public static final double TAX_MIN_VALUE = 0.00;
  public static final double TAX_MAX_VALUE = 100.00;
  public static final String SYSTEM_DETERMINED_REPLENISHMENT = "sq";
  public static final String USER_SPECIFIED_REPLENISHMENT = "us";
  public static final int MATERIAL_NAME_MIN_LENGTH = 1;
  public static final int MATERIAL_SHORTNAME_MAX_LENGTH = 6;
  public static final int MATERIAL_DESCRIPTION_MAX_LENGTH = 200;
  public static final int MATERIAL_ADDITIONAL_INFO_MAX_LENGTH = 400;
  public static final int MIN_SERVICE_LEVEL = 65;
  public static final int MAX_SERVICE_LEVEL = 99;
  public static final int TOKEN_EXPIRY_MIN = 0;
  public static final int TOKEN_EXPIRY_MAX = 999;
  public static final int GUI_THEME_SAME_AS_IN_DOMAIN_CONFIGURATION = 0;
  public static final int GUI_THEME_DEFAULT = 1;
  public static final int GUI_THEME_SIDEBAR_AND_LANDING_SCREEN = 2;
  public static final String MAX_PRICE = "1 billion";
  public static final String MIN_PRICE = "0";
  public static final String YES = "yes";
  public static final String NO = "no";
  public static final String MIN_MAX_LOWER_LIMIT = "0";
  public static final String MIN_MAX_UPPER_LIMIT = "1 trillion";
  public static final String CONSUMPTION_RATE_LOWER_LIMIT = "0";
  public static final String CONSUMPTION_RATE_UPPER_LIMIT = "1 trillion";
  public static final int ENTITY_NAME_MIN_LENGTH = 1;
}
