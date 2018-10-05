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

package com.logistimo.reports.plugins.internal;

import com.logistimo.constants.CharacterConstants;
import com.logistimo.constants.Constants;
import com.logistimo.reports.models.ReportMinMaxHistoryFilters;
import com.logistimo.services.utils.ConfigUtil;

import org.apache.commons.lang.StringUtils;
import org.joda.time.DateTime;
import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;
import org.json.JSONObject;

import java.text.ParseException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * @author Mohan Raja
 */
public class QueryHelper {

  /**
   * UI Variables
   */
  public static final String PERIODICITY = "periodicity";

  public static final String LEVEL = "level";
  public static final String LEVEL_PERIODICITY = "levelPeriodicity";
  public static final String FROM = "from";
  public static final String TO = "to";
  private static final String MATERIAL_TAG = "mtag";
  private static final String ENTITY_TAG = "etag";
  private static final String USER = "user";
  private static final String USER_TAG = "utag";
  private static final String ORDER_TAG = "otag";
  public static final String MATERIAL = "mat";
  public static final String ENTITY = "entity";
  private static final String OTYPE = "otype";
  private static final String STATE = "st";
  private static final String COUNTRY = "cn";
  public static final String DISTRICT = "dis";
  public static final String CITY = "cty";
  private static final String ATYPE = "at";
  private static final String MTYPE = "mt";
  private static final String VENDOR_ID = "mf";
  private static final String MYEAR = "myear";
  private static final String DMODEL = "mm";
  public static final String TALUK = "tlk";
  private static final String SIZE = "s";
  private static final String OFFSET = "o";
  private static final String LKID = "lkid";
  public static final String LOCATION_BY = "location_by";

  /** Query Id */
  public static final String QUERY_DOMAIN = "DID";

  public static final String QUERY_USER = "UID";
  public static final String QUERY_USER_TAG = "UTAG";
  public static final String QUERY_ENTITY_TAG = "KTAG";
  public static final String QUERY_MATERIAL_TAG = "MTAG";
  public static final String QUERY_ORDER_TAG = "OTAG";
  private static final String QUERY_SIZE = "SIZE";
  private static final String QUERY_OFFSET = "OFFSET";

  public static final String QUERY = "QUERY";
  public static final String QUERY_MATERIAL = "MID";
  public static final String QUERY_ENTITY = "KID";
  public static final String QUERY_ORDER_TYPE = "OTY";
  public static final String QUERY_LKID = "LKID";
  public static final String QUERY_COUNTRY = "CN";
  public static final String QUERY_STATE = "ST";
  public static final String QUERY_DISTRICT = "DIS";
  public static final String QUERY_TALUK = "TALUK";
  public static final String QUERY_CITY = "CITY";

  public static final String QUERY_LAST_RUN_TIME="LRES_QUERY";

  public static final String QUERY_MTYPE = "MTYPE"; //monitoring or monitored
  public static final String QUERY_ATYPE = "ATYPE"; //ILR, Freezer
  public static final String QUERY_VENDOR_ID = "VID";
  public static final String QUERY_MYEAR = "MYEAR";
  public static final String QUERY_DMODEL = "DMODEL"; //device model
  public static final String QUERY_DVID = "DVID"; //asset device id (combination of sid and vid)

  public static final String TOKEN_START_TIME = "TOKEN_START_TIME";
  public static final String TOKEN_END_TIME = "TOKEN_END_TIME";
  public static final String TOKEN_PERIODICITY = "TOKEN_PERIODICITY";
  public static final String TOKEN_LOCATION = "TOKEN_LOCATION";
  public static final String TOKEN_RUN_TIME = "TOKEN_APP_NAME";

  public static final String LOCATION_CITY = "CITY";
  public static final String LOCATION_TALUK = "TALUK";
  public static final String LOCATION_DISTRICT = "DISTRICT";
  public static final String LOCATION_STATE = "STATE";
  public static final String LOCATION_COUNTRY = "COUNTRY";

  public static final String TOKEN = "TOKEN_";

  public static final String TOKEN_COLUMNS = "TOKEN_COLUMNS";
  public static final Integer MONTHS_LIMIT = 3;
  public static final Integer WEEKS_LIMIT = 4;
  public static final Integer DAYS_LIMIT = 7;
  public static final String PERIODICITY_MONTH = "m";
  public static final String PERIODICITY_WEEK = "w";

  public static final String MONTH = "month";
  public static final String WEEK = "week";
  public static final String DAY = "day";

  public static final String LEVEL_DAY = "d";

  public static final String DATE_FORMAT_MONTH = "yyyy-MM";
  public static final String DATE_FORMAT_DAILY = "yyyy-MM-dd";

  private static final Map<String, String> OPTIONAL_FILTER_MAP;

  private static final String[] QUERY_ID_ORDER = {
    QUERY_DOMAIN,
    QUERY_MTYPE,
    QUERY_ATYPE,
    QUERY_VENDOR_ID,
    QUERY_MYEAR,
    QUERY_DMODEL,
    QUERY_USER,
    QUERY_MATERIAL,
    QUERY_ENTITY,
    QUERY_ORDER_TYPE,
    QUERY_LKID,
    QUERY_USER_TAG,
    QUERY_ENTITY_TAG,
    QUERY_MATERIAL_TAG,
    QUERY_ORDER_TAG,
    QUERY_COUNTRY,
    QUERY_STATE,
    QUERY_DISTRICT,
    QUERY_TALUK,
    QUERY_CITY
  };

  private static final List<String> NUMERIC_FIELDS = Arrays.asList(MATERIAL, ENTITY, SIZE, OFFSET, MYEAR, LKID, USER, OTYPE, ATYPE);

  static {
    OPTIONAL_FILTER_MAP = new HashMap<>(21,1);
    OPTIONAL_FILTER_MAP.put(USER, QUERY_USER);
    OPTIONAL_FILTER_MAP.put(MATERIAL, QUERY_MATERIAL);
    OPTIONAL_FILTER_MAP.put(ENTITY, QUERY_ENTITY);
    OPTIONAL_FILTER_MAP.put(OTYPE, QUERY_ORDER_TYPE);
    OPTIONAL_FILTER_MAP.put(LKID, QUERY_LKID);
    OPTIONAL_FILTER_MAP.put(USER_TAG, QUERY_USER_TAG);
    OPTIONAL_FILTER_MAP.put(ENTITY_TAG, QUERY_ENTITY_TAG);
    OPTIONAL_FILTER_MAP.put(MATERIAL_TAG, QUERY_MATERIAL_TAG);
    OPTIONAL_FILTER_MAP.put(ORDER_TAG, QUERY_ORDER_TAG);
    OPTIONAL_FILTER_MAP.put(COUNTRY, QUERY_COUNTRY);
    OPTIONAL_FILTER_MAP.put(STATE, QUERY_STATE);
    OPTIONAL_FILTER_MAP.put(DISTRICT, QUERY_DISTRICT);
    OPTIONAL_FILTER_MAP.put(TALUK, QUERY_TALUK);
    OPTIONAL_FILTER_MAP.put(ATYPE, QUERY_ATYPE);
    OPTIONAL_FILTER_MAP.put(MTYPE, QUERY_MTYPE);
    OPTIONAL_FILTER_MAP.put(MYEAR, QUERY_MYEAR);
    OPTIONAL_FILTER_MAP.put(VENDOR_ID, QUERY_VENDOR_ID);
    OPTIONAL_FILTER_MAP.put(DMODEL, QUERY_DMODEL);
    OPTIONAL_FILTER_MAP.put(CITY, QUERY_CITY);
    OPTIONAL_FILTER_MAP.put(SIZE, QUERY_SIZE);
    OPTIONAL_FILTER_MAP.put(OFFSET, QUERY_OFFSET);
  }

  public static Map<String, String> parseFilters(Long domainId, JSONObject jsonObject)
      throws ParseException {
    Map<String, String> filters = new HashMap<>();
    filters.put(TOKEN + QUERY_DOMAIN, String.valueOf(domainId));
    String periodicity;
    String dateFormat;
    switch (jsonObject.getString(PERIODICITY)) {
      case PERIODICITY_MONTH:
        periodicity = MONTH;
        dateFormat = DATE_FORMAT_MONTH;
        break;
      case PERIODICITY_WEEK:
        periodicity = WEEK;
        dateFormat = DATE_FORMAT_DAILY;
        break;
      default:
        periodicity = DAY;
        dateFormat = DATE_FORMAT_DAILY;
    }
    filters.put(TOKEN_PERIODICITY, periodicity);
    DateTimeFormatter dateTimeFormatter = DateTimeFormat.forPattern(Constants.DATE_FORMAT_CSV);
    DateTimeFormatter mDateTimeFormatter = DateTimeFormat.forPattern(dateFormat);
    String from;
    String to;
    if (jsonObject.has(LEVEL) && LEVEL_DAY.equals(jsonObject.getString(LEVEL))) {
      DateTime toDateTime = dateTimeFormatter.parseDateTime(jsonObject.getString(FROM));
      if(PERIODICITY_WEEK.equals(jsonObject.getString(LEVEL_PERIODICITY))) {
        toDateTime = toDateTime.plusWeeks(1).minusDays(1);
      } else {
        toDateTime = toDateTime.plusMonths(1).minusDays(1);
      }
      to = dateTimeFormatter.print(toDateTime);
      from = dateTimeFormatter.print(dateTimeFormatter.parseDateTime(jsonObject.getString(FROM)));
    } else {
      from = mDateTimeFormatter.print(dateTimeFormatter.parseDateTime(jsonObject.getString(FROM)));
      to = mDateTimeFormatter.print(dateTimeFormatter.parseDateTime(jsonObject.getString(TO)));
    }
    filters.put(TOKEN_START_TIME, from);
    filters.put(TOKEN_END_TIME, to);
    for (String filter : OPTIONAL_FILTER_MAP.keySet()) {
      if (jsonObject.has(filter)) {
        switch (filter) {
          case MTYPE:
            if (jsonObject.has(filter) && StringUtils.isNotEmpty(jsonObject.getString(filter))) {
              filters.put(TOKEN + OPTIONAL_FILTER_MAP.get(filter),
                  CharacterConstants.SINGLE_QUOTES+jsonObject.getString(filter)+CharacterConstants.SINGLE_QUOTES);
            }
            break;
          default:
            String value = String.valueOf(jsonObject.get(filter));
            if (NUMERIC_FIELDS.contains(filter)) {
              filters.put(TOKEN + OPTIONAL_FILTER_MAP.get(filter), value);
            } else {
              filters.put(TOKEN + OPTIONAL_FILTER_MAP.get(filter), encloseFilter(value));
            }
            break;
        }
      }
    }
    if(filters.containsKey(TOKEN+QUERY_ATYPE) && filters.containsKey(TOKEN+QUERY_MTYPE)){
      filters.remove(TOKEN+QUERY_MTYPE);
    }
    if(jsonObject.has(LOCATION_BY)) {
      filters.put(LOCATION_BY, jsonObject.getString(LOCATION_BY));
    }
    return filters;
  }

  private static String encloseFilter(String values) {
    if (values.contains(CharacterConstants.COMMA)) {
      Set<String> valSet = new HashSet<>(Arrays.asList(values.split(CharacterConstants.COMMA)));
      StringBuilder val = new StringBuilder();
      for (String s : valSet) {
        val.append(CharacterConstants.S_QUOTE)
            .append(s)
            .append(CharacterConstants.S_QUOTE)
            .append(CharacterConstants.COMMA);
      }
      val.setLength(val.length() - 1);
      return val.toString();
    } else {
      return CharacterConstants.S_QUOTE + values + CharacterConstants.S_QUOTE;
    }
  }

  public static String getQueryID(Map<String, String> filters, String type) {
    String prefix =
        StringUtils.equals(ConfigUtil.get("reports.callisto.prefix"), "report.type")
            ? type.toUpperCase() + CharacterConstants.UNDERSCORE : CharacterConstants.EMPTY;
    String suffix =
        StringUtils.equals(ConfigUtil.get("reports.callisto.suffix"), "periodicity")
            ? CharacterConstants.UNDERSCORE + filters.get(TOKEN_PERIODICITY).toUpperCase() : CharacterConstants.EMPTY;
    StringBuilder queryId = new StringBuilder(prefix);
    for (String id : QUERY_ID_ORDER) {
      if (filters.containsKey(TOKEN + id)) {
        queryId.append(id).append(CharacterConstants.UNDERSCORE);
      }
    }
    queryId.setLength(queryId.length() - 1);
    return queryId.toString() + suffix;
  }

  /**
   * Parses the filters in the input json into a ReportMinMaxHistoryFilters object
   * @param jsonObject
   * @return ReportMinMaxHistoryFilters object
   */
  public static ReportMinMaxHistoryFilters parseMinMaxHistoryFilters(JSONObject jsonObject) {
    if (jsonObject == null) {
      throw new IllegalArgumentException("Invalid filters while parsing min max history filters");
    }
    String periodicity = getPeriodicity(jsonObject.getString(PERIODICITY));
    DateTimeFormatter dateTimeFormatter = DateTimeFormat.forPattern(Constants.DATE_FORMAT_CSV);
    String toDate;
    if (jsonObject.has(LEVEL) && LEVEL_DAY.equals(jsonObject.getString(LEVEL))) {
      DateTime toDateTime = dateTimeFormatter.parseDateTime(jsonObject.getString(FROM));
      if(PERIODICITY_WEEK.equals(jsonObject.getString(LEVEL_PERIODICITY))) {
        toDateTime = toDateTime.plusWeeks(1);
      } else {
        toDateTime = toDateTime.plusMonths(1);
      }
      toDate = dateTimeFormatter.print(toDateTime);
    } else {
      toDate = jsonObject.getString(TO);
      toDate =
          getToDateBasedOnPeriodicity(toDate, periodicity);
    }
    String from = dateTimeFormatter.print(dateTimeFormatter.parseDateTime(jsonObject.getString(FROM)));
    String to = dateTimeFormatter.print(dateTimeFormatter.parseDateTime(toDate));

    return (ReportMinMaxHistoryFilters.builder()
        .entityId((((Integer)jsonObject.get(ENTITY)).longValue()))
        .materialId(((Integer)jsonObject.get(MATERIAL)).longValue())
        .periodicity(periodicity)
        .from(from)
        .to(to)
        .build());
  }

  /**
   * Get the periodicity string
   */
  private static String getPeriodicity(String periodicity) {
    String periodicityStr;
    switch (periodicity) {
      case PERIODICITY_MONTH:
        periodicityStr = MONTH;
        break;
      case PERIODICITY_WEEK:
        periodicityStr = WEEK;
        break;
      default:
        periodicityStr = DAY;
        break;
    }
    return periodicityStr;
  }

  /**
   * Get the to date string based on periodicity. For monthly periodicity, to date is set as beginning of next month.
   * For weekly periodicity to date is set as beginning of the next week. For daily periodicity, to date is set as next day.
   * @param dateStr
   * @param periodicity
   * @return the modified to date string
   */
  protected static String getToDateBasedOnPeriodicity(String dateStr, String periodicity) {
    if (StringUtils.isEmpty(dateStr) || StringUtils.isEmpty(periodicity)) {
      throw new IllegalArgumentException("Invalid input parameters while getting to date");
    }
    DateTimeFormatter dateTimeFormatter = DateTimeFormat.forPattern(DATE_FORMAT_DAILY);
    DateTime date = dateTimeFormatter.parseDateTime(dateStr);
    switch(periodicity) {
      case MONTH:
        date = date.plusMonths(1);
        break;
      case WEEK:
        date = date.plusWeeks(1);
        break;
      case DAY:
        date = date.plusDays(1);
        break;
      default:
        break;
    }
    return dateTimeFormatter.print(date);
  }
}
