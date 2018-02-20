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

import org.junit.Test;

import static junit.framework.Assert.assertNotNull;
import static junit.framework.Assert.assertTrue;

/**
 * Created by smriti on 17/02/18.
 */
public class AssetsDashboardQueryGeneratorTest {

  @Test
  public void testGenerateQueryWithIncludeEntityTagFilter() throws Exception {
    AssetsDashboardQueryGenerator queryGenerator = new AssetsDashboardQueryGenerator();
    String query = queryGenerator.withDomainId(1343724l)
        .withCountry("IN")
        .withIncludeEntityTags("'CCP','SVS'")
        .generate();
    assertNotNull(query);
    assertTrue("Query should contain entity tags", query.contains("'CCP','SVS'"));
    assertTrue("Query should not contain exclude entity tags", !query.contains("NOT IN"));
    assertTrue("Query should contain domain Id 1343724", query.contains("1343724"));
    assertTrue("Query should contain country as IN", query.contains("IN"));
    assertTrue("Query should contain state and state Id",
        query.contains("K.STATE STATE, K.STATE_ID STATE_ID"));
    assertTrue("Group by should be with status and state",
        query.contains("GROUP BY STATUS, STATE"));
  }

  @Test
  public void testGenerateQueryWithExcludeEntityTagFilter() throws Exception {
    AssetsDashboardQueryGenerator queryGenerator = new AssetsDashboardQueryGenerator();
    String query = queryGenerator.withDomainId(1343724l)
        .withCountry("IN")
        .withState("Assam")
        .withExcludeEntityTags("'CCP','SVS','RVS'")
        .generate();
    assertNotNull(query);
    assertTrue("Query should contain exclude entity tags", query.contains("ID NOT IN"));
    assertTrue("Query should contain exclude entity tags", query.contains("'CCP','SVS','RVS'"));
    assertTrue("Query should contain domain id", query.contains("1343724"));
    assertTrue("Query should contain country as IN", query.contains("IN"));
    assertTrue("Query should contain state as Assam", query.contains("Assam"));
    assertTrue("Query should contain district Id", query.contains("DISTRICT_ID"));
    assertTrue("Group by should be with status and district",
        query.contains("GROUP BY STATUS, DISTRICT"));
    assertTrue("Query should not contain asset type filter", !query.contains("A.TYPE"));
  }

  @Test
  public void testGenerateQueryWithDistrictFilter() throws Exception {
    AssetsDashboardQueryGenerator queryGenerator = new AssetsDashboardQueryGenerator();
    String query = queryGenerator.withDomainId(1343724l)
        .withCountry("IN")
        .withState("Uttar Pradesh")
        .withDistrict("Allahabad")
        .generate();
    assertNotNull(query);
    assertTrue("Query should contain domain Id", query.contains("1343724"));
    assertTrue("Query should contain kiosk name", query.contains("K.NAME"));
    assertTrue("Query should group by kiosk name", query.contains("GROUP BY STATUS, NAME"));
    assertTrue("Query should contain country as IN", query.contains("IN"));
    assertTrue("Query should contain state as Uttar Pradesh", query.contains("Uttar Pradesh"));
    assertTrue("Query should contain district as Allahabad", query.contains("Allahabad"));
  }

  @Test
  public void testGenerateQueryWithDistrictFilterEmpty() throws Exception {
    AssetsDashboardQueryGenerator queryGenerator = new AssetsDashboardQueryGenerator();
    String query = queryGenerator.withDomainId(1343724l)
        .withCountry("IN")
        .withState("Goa")
        .withDistrict("")
        .generate();
    assertNotNull(query);
    assertTrue("Query should contain country as IN", query.contains("IN"));
    assertTrue("Query should contain state as Goa", query.contains("Goa"));
    assertTrue("Query should contain district as empty",
        query.contains("K.DISTRICT = '' OR K.DISTRICT IS NULL"));
    assertTrue("Query should contain domain id 1343724", query.contains("1343724"));
    assertTrue("Query should group by name", query.contains("GROUP BY STATUS, NAME"));
  }

  @Test
  public void testGenerateQueryWithAssetTypeFilter() throws Exception {
    AssetsDashboardQueryGenerator queryGenerator = new AssetsDashboardQueryGenerator();
    String query = queryGenerator.withDomainId(1343724l)
        .withAssetTypes("2,3,5,6,7")
        .withCountry("IN")
        .withState("Goa")
        .generate();
    assertNotNull(query);
    assertTrue("Query should contain domain Id", query.contains("1343724"));
    assertTrue("Query should contain country as IN", query.contains("IN"));
    assertTrue("Query should contain state as Goa", query.contains("Goa"));
    assertTrue("Query should contain asset type", query.contains("A.TYPE IN(2,3,5,6,7)"));
  }

  @Test
  public void testGenerateQueryWithStatusAndPeriodFilter() throws Exception {
    AssetsDashboardQueryGenerator queryGenerator = new AssetsDashboardQueryGenerator();
    String query = queryGenerator.withDomainId(1343724l)
        .withCountry("IN")
        .withAssetTypes("2,5")
        .withWorkingStatus(5)
        .withPeriod(7)
        .generate();
    assertNotNull(query);
    assertTrue("Query should contain domain Id", query.contains("1343724"));
    assertTrue("Query should contain country as IN", query.contains("IN"));
    assertTrue("Query should contain asset type", query.contains("A.TYPE IN(2,5)"));
    assertTrue("Query should contain status", query.contains("ASI.STATUS = 5"));
    assertTrue("Query should contain period",
        query.contains("ASI.TS <= SUBDATE(CURDATE(), INTERVAL 7 DAY"));
  }

  @Test
  public void testGenerateQueryWithOnlyStatusFilter() throws Exception {
    AssetsDashboardQueryGenerator queryGenerator = new AssetsDashboardQueryGenerator();
    String query = queryGenerator.withDomainId(1343724l)
        .withCountry("IN")
        .withWorkingStatus(4)
        .generate();
    assertNotNull(query);
    assertTrue("Query should contain domain Id", query.contains("1343724"));
    assertTrue("Query should contain country", query.contains("IN"));
    assertTrue("Query should not contain working status", !query.contains("ASI.STATUS = 4"));
    assertTrue("Query should not contain period", !query.contains("ASI.TS"));
  }

  @Test
  public void testGenerateQueryWithOnlyPeriodFilter() throws Exception {
    AssetsDashboardQueryGenerator queryGenerator = new AssetsDashboardQueryGenerator();
    String query = queryGenerator.withDomainId(1343724l)
        .withCountry("IN")
        .withPeriod(15)
        .generate();
    assertNotNull(query);
    assertTrue("Query should contain domain Id", query.contains("1343724"));
    assertTrue("Query should contain country", query.contains("IN"));
    assertTrue("Query should not contain working status", !query.contains("ASI.STATUS ="));
    assertTrue("Query should not contain period",
        !query.contains("ASI.TS <= SUBDATE(CURDATE(), INTERVAL 15 DAY"));
  }
}