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

package com.logistimo.dashboards.querygenerators;

import com.logistimo.config.models.DashboardConfig;
import com.logistimo.config.models.DomainConfig;
import com.logistimo.tags.entity.ITag;

import org.apache.commons.lang.StringUtils;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;

/**
 * Created by charan on 12/01/18.
 */
public class EntityActivityQueryGenerator {

  private static final String
      SELECT_ID_FROM_TAG_WHERE_NAME_IN =
      "(SELECT ID FROM TAG WHERE NAME IN(";
  private static final String AND_TYPE = ") AND TYPE=";
  private Long domainId;
  private Integer period = -1;
  private String district;
  private String state;
  private String materialTags;
  private String entityTags;
  private String excludeEntityTags;
  private Date asOf;
  private Boolean isCount;
  private String country;
  private Long materialId;

  private EntityActivityQueryGenerator() {
  }

  public static EntityActivityQueryGenerator getEntityActivityQueryGenerator() {
    return new EntityActivityQueryGenerator();
  }

  public EntityActivityQueryGenerator withDomainId(Long domainId) {
    this.domainId = domainId;
    return this;
  }

  public EntityActivityQueryGenerator withPeriod(Integer period) {
    this.period = period;
    return this;
  }

  public EntityActivityQueryGenerator withDistrict(String district) {
    this.district = district;
    return this;
  }

  public EntityActivityQueryGenerator withState(String state) {
    this.state = state;
    return this;
  }

  public EntityActivityQueryGenerator withMaterialTags(String materialTags) {
    this.materialTags = materialTags;
    return this;
  }

  public EntityActivityQueryGenerator withEntityTags(String entityTags) {
    this.entityTags = entityTags;
    return this;
  }

  public EntityActivityQueryGenerator withExcludeEntityTags(String excludeEntityTags) {
    this.excludeEntityTags = excludeEntityTags;
    return this;
  }

  public EntityActivityQueryGenerator withAsOf(Date asOf) {
    this.asOf = asOf;
    return this;
  }

  public EntityActivityQueryGenerator withIsCount(Boolean isCount) {
    this.isCount = isCount;
    return this;
  }

  public EntityActivityQueryGenerator withCountry(String country) {
    this.country = country;
    return this;
  }

  public EntityActivityQueryGenerator withMaterialId(Long materialId) {
    this.materialId = materialId;
    return this;
  }

  public String generate() {
    StringBuilder query = new StringBuilder("SELECT ");
    StringBuilder fromClause = new StringBuilder(" FROM ")
        .append(getDistinctEntityMaterialsWithPeriodQuery())
        .append(",KIOSK K, KIOSK_DOMAINS KD WHERE A.KID = K.KIOSKID AND K.KIOSKID = KD.KIOSKID_OID "
            + "AND KD.DOMAIN_ID = ").append(domainId).append(" ");
    StringBuilder groupByClause = new StringBuilder();
    if(!isCount) {
      groupByClause.append(" GROUP BY ");
    }

    StringBuilder whereClause = new StringBuilder();
    if (district != null) {
      applyDistrict(query, groupByClause, whereClause);
    } else if (state != null) {
      applyState(query, groupByClause, whereClause);
    } else if (country != null) {
      applyCountry(query, groupByClause, whereClause);
    } else {
      throw new IllegalArgumentException(
          "One of Country, State, District should be defined to generate activity query");
    }

    applyMaterialFilters(whereClause);

    applyEntityFilters(whereClause);

    return query.append(" COUNT(DISTINCT KID) COUNT ").append(fromClause).append(whereClause)
        .append(groupByClause).toString();
  }

  private void applyMaterialFilters(StringBuilder whereClause) {
    if (StringUtils.isNotBlank(materialTags)) {
      whereClause.append(" AND A.MID IN (SELECT MATERIALID from MATERIAL_TAGS WHERE ID IN(")
          .append(SELECT_ID_FROM_TAG_WHERE_NAME_IN).append(materialTags)
          .append(AND_TYPE).append(ITag.MATERIAL_TAG).append("))")
          .append(")");
    } else if (materialId != null) {
      whereClause.append(" AND A.MID = ").append(materialId);
    }
  }

  private void applyEntityFilters(StringBuilder whereClause) {
    if (StringUtils.isNotBlank(entityTags)) {
      whereClause.append(" AND A.KID IN(SELECT KIOSKID from KIOSK_TAGS WHERE ID IN(")
          .append(SELECT_ID_FROM_TAG_WHERE_NAME_IN).append(entityTags)
          .append(AND_TYPE).append(ITag.KIOSK_TAG).append(")))");
    } else if (StringUtils.isNotBlank(excludeEntityTags)) {
      whereClause.append(" AND KID NOT IN(SELECT KIOSKID from KIOSK_TAGS WHERE ID IN(")
          .append(SELECT_ID_FROM_TAG_WHERE_NAME_IN).append(excludeEntityTags)
          .append(AND_TYPE).append(ITag.KIOSK_TAG).append(")))");
    }
  }

  private void applyCountry(StringBuilder query, StringBuilder groupByClause,
                            StringBuilder whereClause) {
    if (!isCount) {
      query.append("K.STATE STATE,K.STATE_ID STATE_ID,");
      groupByClause.append("STATE, STATE_ID");
    }
    appendCountryFilter(whereClause);
  }

  private void applyState(StringBuilder query, StringBuilder groupByClause,
                          StringBuilder whereClause) {
    if (country == null) {
      throw new IllegalArgumentException("Country is mandatory when state is provided");
    }
    if (!isCount) {
      query.append("K.DISTRICT DISTRICT,K.DISTRICT_ID DISTRICT_ID,");
      groupByClause.append("DISTRICT, DISTRICT_ID");
    }
    appendCountryFilter(whereClause)
        .append(" AND K.STATE = '").append(state).append("'");
  }

  private void applyDistrict(StringBuilder query, StringBuilder groupByClause,
                             StringBuilder whereClause) {
    if (country == null || state == null) {
      throw new IllegalArgumentException(
          "Country and State are mandatory when district is provided");
    }
    if (!isCount) {
      query.append("K.NAME NAME,A.KID,");
      groupByClause.append("NAME,A.KID");
    }
    appendCountryFilter(whereClause)
        .append(" AND K.STATE = '").append(state).append("'");
    if ("".equals(district)) {
      whereClause.append("AND (DISTRICT = '' OR DISTRICT IS NULL)");
    } else {
      whereClause.append("AND DISTRICT = '").append(district).append("'");
    }
  }

  private StringBuilder appendCountryFilter(StringBuilder whereClause) {
    return whereClause.append(" AND K.COUNTRY = '").append(country).append("'");
  }

  /**
   * Generates distict entity materials which were transacted in the specified period
   * (select KID,MID,COUNT(1) from TRANSACTION T where T >= '2018-01-04 10:34:52' group by KID, MID) A
   *
   * @return query
   */
  private String getDistinctEntityMaterialsWithPeriodQuery() {
    StringBuilder query = new StringBuilder("(SELECT KID,MID,COUNT(1) FROM INVNTRY WHERE IAT IS NOT NULL AND");
    SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
    Calendar cal = new GregorianCalendar();
    if (asOf != null) {
      cal.setTime(asOf);
    }
    cal.add(Calendar.DAY_OF_MONTH, -getPeriod());
    cal.add(Calendar.SECOND, 1);
    if (asOf == null) {
      query.append(" T >= '").append(sdf.format(cal.getTime())).append("'");
    } else {
      query.append(" T BETWEEN '").append(sdf.format(cal.getTime())).append("' AND '")
          .append(sdf.format(asOf)).append("'");
    }
    query.append(" GROUP BY KID,MID) A");
    return query.toString();
  }

  /**
   * Return default value, if not specified
   *
   * @return period to use in the query
   */
  private Integer getPeriod() {
    Integer finalPeriod = 7;
    if (period != null && period > 0) {
      finalPeriod = period;
    } else {
      DomainConfig dc = DomainConfig.getInstance(domainId);
      DashboardConfig dbc = dc.getDashboardConfig();
      if (dbc != null && dbc.getDbOverConfig() != null && StringUtils
          .isNotEmpty(dbc.getDbOverConfig().aper)) {
        finalPeriod = Integer.parseInt(dbc.getDbOverConfig().aper);
      }
    }
    return finalPeriod;
  }


}
