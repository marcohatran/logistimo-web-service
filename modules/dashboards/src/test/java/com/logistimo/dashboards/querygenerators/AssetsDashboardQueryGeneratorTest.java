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

import com.logistimo.sql.PreparedStatementModel;

import org.junit.Test;

import static junit.framework.Assert.assertNotNull;

/**
 * Created by smriti on 17/02/18.
 */
public class AssetsDashboardQueryGeneratorTest {

  @Test
  public void testGenerateQueryWithIncludeEntityTagFilter() throws Exception {
    AssetsDashboardQueryGenerator queryGenerator = new AssetsDashboardQueryGenerator();
    PreparedStatementModel query = queryGenerator.withDomainId(1343724l)
        .withCountry("IN")
        .withIncludeEntityTags("'CCP','SVS'")
        .generate();
    assertNotNull(query);
  }

  @Test
  public void testGenerateQueryWithExcludeEntityTagFilter() throws Exception {
    AssetsDashboardQueryGenerator queryGenerator = new AssetsDashboardQueryGenerator();
    PreparedStatementModel query = queryGenerator.withDomainId(1343724l)
        .withCountry("IN")
        .withState("Assam")
        .withExcludeEntityTags("'CCP','SVS','RVS'")
        .generate();
    assertNotNull(query);

  }

  @Test
  public void testGenerateQueryWithDistrictFilter() throws Exception {
    AssetsDashboardQueryGenerator queryGenerator = new AssetsDashboardQueryGenerator();
    PreparedStatementModel query = queryGenerator.withDomainId(1343724l)
        .withCountry("IN")
        .withState("Uttar Pradesh")
        .withDistrict("Allahabad")
        .generate();
    assertNotNull(query);
  }

  @Test
  public void testGenerateQueryWithDistrictFilterEmpty() throws Exception {
    AssetsDashboardQueryGenerator queryGenerator = new AssetsDashboardQueryGenerator();
    PreparedStatementModel query = queryGenerator.withDomainId(1343724l)
        .withCountry("IN")
        .withState("Goa")
        .withDistrict("")
        .generate();
    assertNotNull(query);
  }

  @Test
  public void testGenerateQueryWithAssetTypeFilter() throws Exception {
    AssetsDashboardQueryGenerator queryGenerator = new AssetsDashboardQueryGenerator();
    PreparedStatementModel query = queryGenerator.withDomainId(1343724l)
        .withAssetTypes("2,3,5,6,7")
        .withCountry("IN")
        .withState("Goa")
        .generate();
    assertNotNull(query);
  }

  @Test
  public void testGenerateQueryWithStatusAndPeriodFilter() throws Exception {
    AssetsDashboardQueryGenerator queryGenerator = new AssetsDashboardQueryGenerator();
    PreparedStatementModel query = queryGenerator.withDomainId(1343724l)
        .withCountry("IN")
        .withAssetTypes("2,5")
        .withWorkingStatus(5)
        .withPeriod(7)
        .generate();
    assertNotNull(query);
  }

  @Test
  public void testGenerateQueryWithOnlyStatusFilter() throws Exception {
    AssetsDashboardQueryGenerator queryGenerator = new AssetsDashboardQueryGenerator();
    PreparedStatementModel query = queryGenerator.withDomainId(1343724l)
        .withCountry("IN")
        .withWorkingStatus(4)
        .generate();
    assertNotNull(query);

  }

  @Test
  public void testGenerateQueryWithOnlyPeriodFilter() throws Exception {
    AssetsDashboardQueryGenerator queryGenerator = new AssetsDashboardQueryGenerator();
    PreparedStatementModel query = queryGenerator.withDomainId(1343724l)
        .withCountry("IN")
        .withPeriod(15)
        .generate();
    assertNotNull(query);

  }
}