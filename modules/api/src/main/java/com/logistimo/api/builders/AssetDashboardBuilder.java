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

package com.logistimo.api.builders;

import com.logistimo.api.models.AssetDashboardModel;
import com.logistimo.api.models.DashboardChartModel;

import org.springframework.stereotype.Component;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

/**
 * Created by smriti on 19/03/18.
 */
@Component
public class AssetDashboardBuilder {
  private static final String OTHERS = "Others";
  private static final String COUNT = "COUNT";
  private Long denominator;

  public AssetDashboardModel buildAssetDashboardModel(ResultSet assetRes, ResultSet assetOverallRes,
                                                      String columnFilter)
      throws SQLException {
    AssetDashboardModel assetDashboardModel = new AssetDashboardModel();
    assetDashboardModel.setAsset(getAssetDrillDown(assetRes, columnFilter));
    assetDashboardModel.setAssetDomain(getAssetDomainData(assetDashboardModel.getAsset(),
        assetOverallRes));
    return assetDashboardModel;
  }

  private Map<String, Long> getAssetDomainData(
      Map<String, Map<String, DashboardChartModel>> assetsDrillDownData, ResultSet assetOverallRes)
      throws SQLException {
    Map<String, Long> assetsCountData = getAssetsCount(assetsDrillDownData);
    if (assetOverallRes != null) {
      addTotalAssetsCountForStatusAndPeriodFilter(assetOverallRes, assetsCountData);
    }
    addAssetsDrillDownMeta(assetsDrillDownData);
    return assetsCountData;
  }

  private Map<String, Long> getAssetsCount(Map<String, Map<String, DashboardChartModel>> assets) {
    Map<String, Long> assetsCountData = new HashMap<>();
    denominator = 0l;
    for (Map.Entry<String, Map<String, DashboardChartModel>> entry : assets.entrySet()) {
      Long statusCount = 0l;
      for (Map.Entry<String, DashboardChartModel> location : entry.getValue().entrySet()) {
        statusCount = statusCount + location.getValue().value;
      }
      denominator = denominator + statusCount;
      assetsCountData.put(entry.getKey(), statusCount);
    }
    return assetsCountData;
  }

  private void addTotalAssetsCountForStatusAndPeriodFilter(ResultSet assetOverallRes,
                                                           Map<String, Long> assetsCountData)
      throws SQLException {
    long totalAssetCount = 0l;
    while (assetOverallRes.next()) {
      totalAssetCount = totalAssetCount + Long.parseLong(assetOverallRes.getString(COUNT));
    }
    // There will always be one row for status and period filter combination
    Optional<String> statusKey = assetsCountData.keySet().stream().findFirst();
    long otherStatusCount;
    if (statusKey.isPresent()) {
      otherStatusCount = totalAssetCount - assetsCountData.get(statusKey.get());
    } else {
      otherStatusCount = totalAssetCount;
    }
    assetsCountData.put(OTHERS, otherStatusCount);
    denominator = totalAssetCount;
  }

  /**
   * Sets meta info like percentage, denominator for each drill down object
   */
  private void addAssetsDrillDownMeta(Map<String, Map<String, DashboardChartModel>> assets) {
    for (Map<String, DashboardChartModel> model : assets.values()) {
      for (Map.Entry<String, DashboardChartModel> modelEntry : model.entrySet()) {
        DashboardChartModel dashboardModel = modelEntry.getValue();
        dashboardModel.den = denominator;
        dashboardModel.per =
            (dashboardModel.value.doubleValue() / dashboardModel.den.doubleValue()) * 100;
      }
    }
  }

  private Map<String, Map<String, DashboardChartModel>> getAssetDrillDown(ResultSet assetRes,
                                                                          String colFilter)
      throws SQLException {
    Map<String, Map<String, DashboardChartModel>> assetsDrillDownData = new HashMap<>();
    if (assetRes == null) {
      return assetsDrillDownData;
    }
    while (assetRes.next()) {
      String status = assetRes.getString("STATUS");
      Map<String, DashboardChartModel> model;
      if (!assetsDrillDownData.containsKey(status)) {
        model = new HashMap<>();
        assetsDrillDownData.put(status, model);
      } else {
        model = assetsDrillDownData.get(status);
      }
      long assetCount = Long.parseLong(assetRes.getString(COUNT));
      if ("NAME".equals(colFilter)) {
        model.put(assetRes.getString(colFilter), new DashboardChartModel(assetCount, Long.parseLong(
            assetRes.getString("KIOSKID"))));
      } else {
        model.put(assetRes.getString(colFilter), new DashboardChartModel(assetCount));
      }
    }
    return assetsDrillDownData;
  }
}
