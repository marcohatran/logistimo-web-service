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

package com.logistimo.dashboards.querygenerators;

import com.logistimo.constants.CharacterConstants;
import com.logistimo.constants.QueryConstants;

import org.apache.commons.lang.StringUtils;

/**
 * Created by smriti on 12/02/18.
 */
public class AssetsDashboardQueryGenerator {
  public static final String K_COUNTRY = " K.COUNTRY = ";
  private Long domainId;
  private String includeEntityTags;
  private String excludeEntityTags;
  private Integer workingStatus;
  private String assetTypes;
  private Integer period;
  private String country;
  private String state;
  private String district;

  private static final String TAG_QUERY = "SELECT ID FROM TAG WHERE NAME IN";

  public AssetsDashboardQueryGenerator withDomainId(Long domainId) {
    this.domainId = domainId;
    return this;
  }

  public AssetsDashboardQueryGenerator withIncludeEntityTags(String includeEntityTags) {
    this.includeEntityTags = includeEntityTags;
    return this;
  }

  public AssetsDashboardQueryGenerator withExcludeEntityTags(String excludeEntityTags) {
    this.excludeEntityTags = excludeEntityTags;
    return this;
  }

  public AssetsDashboardQueryGenerator withAssetTypes(String assetTypes) {
    this.assetTypes = assetTypes;
    return this;
  }

  public AssetsDashboardQueryGenerator withWorkingStatus(Integer workingStatus) {
    this.workingStatus = workingStatus;
    return this;
  }

  public AssetsDashboardQueryGenerator withPeriod(Integer period) {
    this.period = period;
    return this;
  }

  public AssetsDashboardQueryGenerator withCountry(String country) {
    this.country = country;
    return this;
  }

  public AssetsDashboardQueryGenerator withState(String state) {
    this.state = state;
    return this;
  }

  public AssetsDashboardQueryGenerator withDistrict(String district) {
    this.district = district;
    return this;
  }

  public String generate() {
    StringBuilder query = new StringBuilder("SELECT ASI.STATUS");

    StringBuilder fromClause = new StringBuilder(" FROM ASSETSTATUS ASI LEFT JOIN "
        + "ASSET A ON ASI.ASSETID = A.ID LEFT JOIN "
        + "ASSET_DOMAINS AD ON ASI.ASSETID = AD.ID_OID ");

    StringBuilder
        whereClause =
        new StringBuilder(" WHERE ASI.TYPE = 7 AND AD.DOMAIN_ID = ").append(domainId);
    applyEntityTagsFilter(fromClause);
    StringBuilder groupBy = new StringBuilder(" GROUP BY STATUS");
    applyLocations(fromClause, query, groupBy);
    applyAssetFilters(whereClause);

    return query.append(", COUNT(1) AS COUNT").append(fromClause.toString()).append(whereClause)
        .append(groupBy).toString();
  }

  private void applyEntityTagsFilter(StringBuilder fromClause) {
    fromClause.append("LEFT JOIN (SELECT KIOSKID_OID FROM KIOSK_DOMAINS WHERE DOMAIN_ID = ")
        .append(domainId);
    if (StringUtils.isNotEmpty(includeEntityTags) || StringUtils.isNotEmpty(excludeEntityTags)) {
      fromClause.append(" AND KIOSKID_OID IN(SELECT KIOSKID FROM KIOSK_TAGS WHERE ID");

      if (StringUtils.isNotEmpty(includeEntityTags)) {
        fromClause.append(QueryConstants.IN).append(CharacterConstants.O_BRACKET).append(TAG_QUERY)
            .append(CharacterConstants.O_BRACKET)
            .append(includeEntityTags).append(
            CharacterConstants.C_BRACKET)
            .append(CharacterConstants.C_BRACKET);
      } else if (StringUtils.isNotEmpty(excludeEntityTags)) {
        fromClause.append(" NOT IN")
            .append(CharacterConstants.O_BRACKET).append(TAG_QUERY)
            .append(CharacterConstants.O_BRACKET)
            .append(excludeEntityTags).append(CharacterConstants.C_BRACKET)
            .append(CharacterConstants.C_BRACKET);
      }
      fromClause.append(CharacterConstants.C_BRACKET);
    }
    fromClause.append(CharacterConstants.C_BRACKET).append("KT").append(" ON KT.KIOSKID_OID = ")
        .append("A.KID")
        .append(" LEFT JOIN KIOSK K ON K.KIOSKID = KT.KIOSKID_OID AND ");
  }

  private void applyAssetFilters(StringBuilder whereClause) {
    whereClause.append(" AND K.KIOSKID IS NOT NULL");
    if (StringUtils.isNotEmpty(assetTypes)) {
      whereClause.append(" AND A.TYPE IN").append(CharacterConstants.O_BRACKET).append(assetTypes)
          .append(CharacterConstants.C_BRACKET);
    }
    if (workingStatus != null && period != null) {
      whereClause.append(" AND ASI.STATUS = ").append(workingStatus)
          .append(" AND ASI.TS <= SUBDATE(CURDATE(), INTERVAL ").append(period).append(" DAY)");
    }
  }


  private void applyLocations(StringBuilder whereClause, StringBuilder query,
                              StringBuilder groupBy) {
    if (district != null) {
      applyDistrict(whereClause, query, groupBy);
    } else if (state != null) {
      applyState(whereClause, query, groupBy);
    } else if (country != null) {
      applyCountry(whereClause, query, groupBy);
    } else {
      throw new IllegalArgumentException(
          "One of Country, State, District should be defined to generate asset status dashboard query");
    }
  }

  private void applyDistrict(StringBuilder whereClause, StringBuilder query,
                             StringBuilder groupBy) {
    whereClause.append(K_COUNTRY).append(CharacterConstants.S_QUOTE).append(country)
        .append(CharacterConstants.S_QUOTE).append(
        " AND K.STATE = ").append(CharacterConstants.S_QUOTE).append(state)
        .append(CharacterConstants.S_QUOTE);
    if (CharacterConstants.EMPTY.equals(district)) {
      whereClause.append(" AND K.DISTRICT = '' OR K.DISTRICT IS NULL");
    } else {
      whereClause.append(" AND K.DISTRICT = ").append(CharacterConstants.S_QUOTE).append(district)
          .append(CharacterConstants.S_QUOTE);
    }
    query.append(", K.NAME NAME, K.KIOSKID AS KIOSKID");
    groupBy.append(", KIOSKID");
  }

  private void applyState(StringBuilder whereClause, StringBuilder query, StringBuilder groupBy) {
    whereClause.append(K_COUNTRY).append(CharacterConstants.S_QUOTE).append(country)
        .append(CharacterConstants.S_QUOTE).append(" AND K.STATE = ")
        .append(CharacterConstants.S_QUOTE).append(state).append(CharacterConstants.S_QUOTE);

    query.append(", K.DISTRICT DISTRICT, K.DISTRICT_ID DISTRICT_ID");
    groupBy.append(", DISTRICT");
  }

  private void applyCountry(StringBuilder whereClause, StringBuilder query, StringBuilder groupBy) {
    whereClause.append(K_COUNTRY).append(CharacterConstants.S_QUOTE).append(country)
        .append(CharacterConstants.S_QUOTE);

    query.append(",K.STATE STATE, K.STATE_ID STATE_ID");
    groupBy.append(", STATE");
  }
}
