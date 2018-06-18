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

package com.logistimo.api.builders;

import com.logistimo.auth.utils.SecurityUtils;
import com.logistimo.config.models.DomainConfig;
import com.logistimo.constants.CharacterConstants;
import com.logistimo.inventory.entity.IInventoryMinMaxLog;
import com.logistimo.reports.models.ReportDataModel;
import com.logistimo.reports.models.ReportMinMaxHistoryFilters;
import com.logistimo.reports.plugins.internal.QueryHelper;
import com.logistimo.reports.plugins.models.ReportChartModel;
import com.logistimo.utils.BigUtil;
import com.logistimo.utils.LocalDateUtil;

import org.apache.commons.collections.MapUtils;
import org.apache.commons.lang.StringUtils;
import org.joda.time.DateTime;
import org.joda.time.DateTimeConstants;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Created by vani on 05/06/18.
 */
@Component
public class ReportBuilder {
  /**
   * Builds a list of ReportChartModel objects from a list of IInventoryMinMaxLog objects based on the periodicity and dates specified in the filters.
   * @param minMaxLogs
   * @param minMaxHistoryFilters
   * @return
   * @throws ParseException
   */
  public List<ReportChartModel> buildMinMaxHistoryReportsData(List<IInventoryMinMaxLog> minMaxLogs, ReportMinMaxHistoryFilters minMaxHistoryFilters) throws
      ParseException {
    Map<String,ReportChartModel> labelByReportChartModelMap = new LinkedHashMap<>();
    for (IInventoryMinMaxLog minMaxLog : minMaxLogs) {
      String label = getMinMaxHistoryReportLabel(minMaxLog.getCreatedTime(), minMaxHistoryFilters.getPeriodicity(), minMaxHistoryFilters.getFrom());
      if (!labelByReportChartModelMap.containsKey(label)) {
        ReportChartModel reportChartModel = new ReportChartModel();
        reportChartModel.label = label;
        reportChartModel.value = getMinMaxHistoryReportValues(minMaxLog.getMin(), minMaxLog.getMax());
        labelByReportChartModelMap.put(label,reportChartModel);
      }
    }
    fillMinMaxHistoryChartData(labelByReportChartModelMap, minMaxHistoryFilters);
    return labelByReportChartModelMap.values().stream().collect(
        Collectors.toList());
  }

  /**
   * Returns a label based on the created time and periodicity. If created time is less than the from date, then it sets the label based on the from date.
   * @param minMaxLogCreatedTime
   * @param periodicity
   * @param from
   * @return The label of the report data
   * @throws ParseException
   */
  protected String getMinMaxHistoryReportLabel(Date minMaxLogCreatedTime, String periodicity, String from) throws ParseException{
    DateTime createdTime = new DateTime(minMaxLogCreatedTime);
    SimpleDateFormat labelDateFormat = new SimpleDateFormat(QueryHelper.DATE_FORMAT_DAILY);
    Date fromDate = labelDateFormat.parse(from);
    if (minMaxLogCreatedTime.before(fromDate)) {
      createdTime =  new DateTime(fromDate);
    } else if (StringUtils.equals(periodicity, QueryHelper.MONTH)) {
      createdTime = createdTime.withDayOfMonth(1);
    } else if (StringUtils.equals(periodicity, QueryHelper.WEEK)) {
      createdTime = createdTime.withDayOfWeek(DateTimeConstants.MONDAY);
    }
    String domainTimezone = DomainConfig
        .getInstance(SecurityUtils.getCurrentDomainId()).getTimezone();
    return LocalDateUtil.formatCustom(createdTime.toDate(), QueryHelper.DATE_FORMAT_DAILY,
        domainTimezone);
  }

  /**
   * Returns a list of ReportDataModel objects with min and max values.
   * @param min
   * @param max
   * @return
   */
  protected List<ReportDataModel> getMinMaxHistoryReportValues(BigDecimal min, BigDecimal max) {
    List<ReportDataModel> values = new ArrayList<>(3);
    values.add(new ReportDataModel(CharacterConstants.EMPTY));
    values.add(addData(min));
    values.add(addData(max));
    return values;
  }

  /**
   * Returns a ReportDataModel object with it's value set
   * @param value
   * @return
   */
  private ReportDataModel addData(BigDecimal value) {
    return new ReportDataModel(BigUtil.getZeroIfNull(value).toString());
  }

  /**
   * Returns a list of ReportChartModels with data filled for all periods in the filters.
   * @param labelByReportChartModelMap
   * @param minMaxHistoryFilters
   * @return
   */
  protected void fillMinMaxHistoryChartData(Map<String,ReportChartModel> labelByReportChartModelMap, ReportMinMaxHistoryFilters minMaxHistoryFilters) throws ParseException {
    if (MapUtils.isEmpty(labelByReportChartModelMap)){
      return;
    }
    String from = minMaxHistoryFilters.getFrom();
    String to = minMaxHistoryFilters.getTo();
    SimpleDateFormat labelDateFormat = new SimpleDateFormat(QueryHelper.DATE_FORMAT_DAILY);
    Calendar calendar = new GregorianCalendar();
    Integer period;

    Date fromDate;
    Date toDate;
    SimpleDateFormat dateFormat;
    switch (minMaxHistoryFilters.getPeriodicity()) {
      case QueryHelper.MONTH:
        dateFormat = new SimpleDateFormat(QueryHelper.DATE_FORMAT_MONTH);
        fromDate = dateFormat.parse(from);
        calendar.setTime(fromDate);
        from = labelDateFormat.format(fromDate);
        toDate = dateFormat.parse(to);
        period = Calendar.MONTH;
        break;
      case QueryHelper.WEEK:
        dateFormat = new SimpleDateFormat(QueryHelper.DATE_FORMAT_DAILY);
        fromDate = dateFormat.parse(from);
        toDate = dateFormat.parse(to);
        calendar.setTime(dateFormat.parse(from));
        period = Calendar.WEEK_OF_YEAR;
        break;
      default:
        dateFormat = new SimpleDateFormat(QueryHelper.DATE_FORMAT_DAILY);
        fromDate = dateFormat.parse(from);
        toDate = dateFormat.parse(to);
        calendar.setTime(dateFormat.parse(from));
        period = Calendar.DAY_OF_YEAR;
    }
    String previousFrom = from;
    Calendar today = new GregorianCalendar();
    today = LocalDateUtil.resetTimeFields(today);
    while (toDate.after(fromDate) && today.getTime().after(fromDate)) {
      if (!labelByReportChartModelMap.containsKey(from)) {
        ReportChartModel previousDateReportChartModel = labelByReportChartModelMap.get(previousFrom);
        labelByReportChartModelMap.put(from, buildReportChartModel(from, previousDateReportChartModel));
      } else {
        previousFrom = from;
      }
      calendar.add(period, 1);
      from = labelDateFormat.format(calendar.getTime());
      fromDate = calendar.getTime();
    }
  }

  /**
   * Builds a ReportChartModel object
   * @param label
   * @param previousDateReportChartModel
   * @return
   */
  private ReportChartModel buildReportChartModel(String label, ReportChartModel previousDateReportChartModel ) {
    if (StringUtils.isEmpty(label)) {
      throw new IllegalArgumentException("Invalid label while building report chart model");
    }
    ReportChartModel reportChartModel = new ReportChartModel();
    reportChartModel.label = label;
    reportChartModel.value = new ArrayList<>(3);
    if (previousDateReportChartModel != null) {
      for (int i = 0; i < 3; i++) {
        reportChartModel.value.add(new ReportDataModel(previousDateReportChartModel.value.get(i).value));
      }
    } else {
      ReportDataModel emptyReportDataModel = new ReportDataModel(CharacterConstants.EMPTY);
      reportChartModel.value.add(emptyReportDataModel);
      reportChartModel.value.add(emptyReportDataModel);
      reportChartModel.value.add(emptyReportDataModel);
    }
    return reportChartModel;
  }
}
