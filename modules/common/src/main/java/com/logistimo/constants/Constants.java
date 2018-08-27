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

/**
 *
 */
package com.logistimo.constants;

import java.math.BigDecimal;

/**
 * @author arun
 */
public final class Constants {

  /**
   * Order approval locking key prefix
   */
  public static final String TX_OA = "TX_OA";
  public static final String X_ACCESS_USER = "x-access-user";
  public static final String GROUP_BY_NAME = "GROUP_BY_NAME";
  public static final String DOMAIN = "x-access-domain";

  private Constants() {
  }

  // Country codes - ISO 3166 standard codes
  public static final String COUNTRY_INDIA = "IN";
  public static final String COUNTRY_DEFAULT = COUNTRY_INDIA;

  // Language codes - ISO 639.2
  public static final String LANG_ENGLISH = "en";
  public static final String LANG_DEFAULT = LANG_ENGLISH;

  // Currency codes - ISO 4217
  public static final String CURRENCY_INDIANRUPEES = "INR";
  public static final String CURRENCY_DEFAULT = CURRENCY_INDIANRUPEES;

  // Timezones - Java timezone IDs
  public static final String TIMEZONE_INDIA = "Asia/Kolkata";
  public static final String TIMEZONE_DEFAULT = TIMEZONE_INDIA;

  // Lead-time default, in days
  public static final int LEADTIME_DEFAULT = 3; // days

  // Session variable names
  public static final String PARAM_USER = "usr";
  public static final String PARAM_DOMAINID = "dId";

  // Separator in a primary key
  public static final String KEY_SEPARATOR = ".";

  // Domain defaults
  public static final Long DOMAINID_DEFAULT = -1l;

  // System name default
  public static final String SYSTEM_ID = "__sys__";

  // Tabbed views identifiers
  public static final String VIEW_MATERIALS = "materials";
  public static final String VIEW_KIOSKS = "kiosks";
  public static final String VIEW_INVENTORY = "inventory";
  public static final String VIEW_KIOSKMATERIALS = "kioskmaterials";
  public static final String VIEW_USERS = "users";
  public static final String VIEW_ORDERS = "orders";
  public static final String VIEW_DATA = "data";
  public static final String VIEW_DOMAINS = "domains";
  public static final String VIEW_HOME = "home";
  public static final String VIEW_SYSTEMCONFIGURATION = "system_configuration";

  public static final String VIEW_POOLGROUPS = "poolgroups";

  // Unicode replace-any character
  public static final String UNICODE_REPLACEANY = "\ufffd";

  // Modes
  public static final String MODE_MANAGE = "manage";

  // Date format
  public static final String DATE_FORMAT = "dd/MM/yyyy";
  public static final String DATE_FORMAT_CSV = "yyyy-MM-dd";
  public static final String DATE_FORMAT_MONTH ="yyyy-MM";
  public static final String DATETIME_FORMAT = "dd/MM/yyyy HH:mm:ss";
  public static final String DATETIME_CSV_FORMAT = "yyyy-MM-dd HH:mm:ss";
  public static final String DATETIME_IN_MILLIS_FORMAT = "dd/MM/yyyy HH:mm:ss.SSS";
  public static final String DATE_FORMAT_CUSTOMREPORT = "dd-MMM-yyyy";
  public static final String ANALYTICS_DATE_FORMAT = "yyyy-MM-dd'T'HH:mm:ss.SSSXXX";
  public static final String IF_MODIFIED_SINCE_DATE_FORMAT = "EEE, dd MMM yyyy HH:mm:ss";
  public static final String ISO_PATTERN = "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'";
  public static final String DATE_FORMAT_EXCEL = "yyyy.MM.dd";

  public static final String IF_MODIFIED_SINCE_HEADER = "If-Modified-Since";
  public static final String LAST_MODIFIED_HEADER = "Last-Modified";

  // Pagination cursor session attributes
  public static final String CURSOR_TRANSACTIONS = "cursortrans";
  public static final String CURSOR_ORDERS = "cursororders";
  public static final String CURSOR_USERS = "cursorusers";
  public static final String CURSOR_KIOSKS = "cursorkiosks";
  public static final String CURSOR_INVENTORY = "cursorinventory";
  public static final String CURSOR_MATERIALS = "cursormaterials";
  public static final String CURSOR_KIOSKLINKS = "cursorkiosklinks";

  // Login expiry time default
  public static final int LOGIN_DURATION = 15; // days

  // URLS
  public static final String URL_OPTIMIZE = "/task/optimize";

  // Max. limit to contains query in GAE
  public static final int MAX_LIST_SIZE_FOR_CONTAINS_QUERY = 30;

  // Backend hosts
  public static final String BACKEND1 = "backend1";

  public static final String GAE_DEPLOYMENT = "gae.deployment";
  public static final String GCS_BUCKET_DEFAULT = "default";
  public static final String INTERNAL_TASK = "INTERNAL_TASK";
  public static final String EMPTY = "";
  public static final String UTF8 = "UTF-8";
  public static final String TRUE = "true";
  public static final String EXPIRES = "expires";
  public static final String NULL = "null";
  public static final String USER_SESS_PREFIX = "U:SESS:";
  public static final String EMPTYQUOTES = "\"\"";
  public static final String OFFSET = "o";
  public static final String TX = "TX";
  public static final String OK = "OK";
  public static final String TX_O = "TX_O_";
  // Freq. values
  public static final String FREQ_DAILY = "daily";
  public static final String FREQ_WEEKLY = "weekly";
  public static final String FREQ_MONTHLY = "monthly";
  public static final String FREQ_QUARTERLY = "quarterly";
  public static final String FREQ_HALF_YEARLY = "halfyearly";
  public static final String X_APP_ENGINE_TASK_NAME = "X-AppEngine-TaskName";
  public static final String DEFAULT = "default";
  public static final String TOKEN = "x-access-token";
  public static final String ACCESS_INITIATOR = "x-access-initiator";
  public static final String TEMPSERVICE_SIGNATURE_HEADER = "X-TempService-Signature";
  public static final String MINUSONE = "-1";
  public static final String ERROR = "ERROR";
  public static final Integer LAST_ACCESSED_BY_SYSTEM = 1;
  public static final BigDecimal WEEKLY_COMPUTATION = BigDecimal.valueOf(7);
  public static final BigDecimal MONTHLY_COMPUTATION = BigDecimal.valueOf(30.4375);
  public static final String YES = "yes";
  public static final String NO = "no";
  public static final String UNKNOWN = "Unknown";
  public static final String UPDATE_PREDICTION_TASK = "/s2/api/inventory/task/prediction";
  public static final String DASHBOARD_CACHE_PREFIX = "DB_";
  public static final String MDASHBOARD_CACHE_PREFIX = "MDB_";
  public static final String NW_HIERARCHY_CACHE_PREFIX = "NH_";
  public static final String PREDICTIVE_DASHBOARD_CACHE_PREFIX = "PDB_";
  public static final String INV_DASHBOARD_CACHE_PREFIX = "IDB_";
  public static final String SESSACT_DASHBOARD_CACHE_PREFIX = "SDB_";
  public static final String ENTITY = "entity";

  public static final String TYPE_PS = "ps";
  public static final String TYPE_DQ = "dq";

  public static final String TYPE_ISSUE = "i";
  public static final String TYPE_RECEIPT = "r";
  public static final String TYPE_PHYSICALCOUNT = "p";
  public static final String TYPE_TRANSFER = "t";
  public static final String TYPE_ORDER = "o";
  public static final String TYPE_REORDER = "oo";
  public static final String TYPE_WASTAGE = "w";
  public static final String MATERIAL_TAG = "mTag";

  public static final String REQ_ID = "x-request-id";
  public static final String APP_NAME = "x-app-name";
  public static final String APP_VER = "x-app-ver";
  public static final String MMA_NAME = "mma";

  public static final int FREQ_TYPE_DAILY = 0;
  public static final int FREQ_TYPE_WEEKLY = 1;
  public static final int FREQ_TYPE_MONTHLY = 2;

  public static final int GUI_THEME_SAME_AS_IN_DOMAIN_CONFIGURATION = -1;
  public static final int GUI_THEME_DEFAULT = 0;
  public static final int GUI_THEME_SIDEBAR_AND_LANDING_SCREEN = 1;

  public static final String JAVAX_JDO_QUERY_SQL = "javax.jdo.query.SQL";

  public static final String SYSTEM_USER_ID = "system";

  public static final String WEB = "w";

  public static final String USER_KEY = "user";
  public static final String KIOSK_KEY = "kiosk";

  public static final String MESSAGES = "Messages";

  public static final String BACKEND_MESSAGES = "BackendMessages";
  public static final String SKIP_INVENTORY = "skip_inventory";
}
