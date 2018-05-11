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

import org.junit.Test;

import java.util.Date;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

/**
 * Created by charan on 12/01/18.
 */
public class EntityActivityQueryGeneratorTest {

  @Test
  public void testQueryGenerator() {
    EntityActivityQueryGenerator
        entityActivityQueryGenerator =
        EntityActivityQueryGenerator.getEntityActivityQueryGenerator();
    String query = entityActivityQueryGenerator.withDomainId(13l)
        .withCountry("INDIA")
        .withState("KARNATAKA")
        .withDistrict("BENGALURU")
        .withIsCount(false)
        .withEntityTags("'GMSD'")
        .withExcludeEntityTags("CCP")
        .withMaterialTags("'RI Vaccines'")
        .withMaterialId(12345l)
        .withPeriod(7)
        .generate();
    assertNotNull(query);
    assertTrue("Query should contain country INDIA", query.contains("INDIA"));
    assertTrue("Query should contain state KARNATAKA", query.contains("KARNATAKA"));
    assertTrue("Query should contain district BENGALURU", query.contains("BENGALURU"));
    assertTrue("Query should contain entity tag GMSD", query.contains("GMSD"));
    assertTrue("Query should contain material tag RI Vaccines", query.contains("RI Vaccines"));
    assertTrue("Query should NOT contain material id 12345", !query.contains("12345"));
    assertTrue("Query should NOT contain exclude entity tags CCP", !query.contains("CCP"));
    assertTrue("Query should fetch data from INVNTRY table", query.contains("INVNTRY WHERE IAT IS NOT NULL"));
  }

  @Test
  public void testQueryGeneratorExcludeEntity() {
    EntityActivityQueryGenerator
        entityActivityQueryGenerator =
        EntityActivityQueryGenerator.getEntityActivityQueryGenerator();
    String query = entityActivityQueryGenerator.withDomainId(13l)
        .withCountry("INDIA")
        .withState("KARNATAKA")
        .withDistrict("BENGALURU")
        .withIsCount(false)
        .withExcludeEntityTags("'GMSD'")
        .withMaterialId(1234567l)
        .withAsOf(new Date())
        .withPeriod(7)
        .generate();
    assertNotNull(query);
    assertTrue("Query should contain country INDIA", query.contains("INDIA"));
    assertTrue("Query should contain state KARNATAKA", query.contains("KARNATAKA"));
    assertTrue("Query should contain district BENGALURU", query.contains("BENGALURU"));
    assertTrue("Query should contain exclude entity tag GMSD", query.contains("GMSD"));
    assertTrue("Query should contain material id 1234567", query.contains("1234567"));
  }

  @Test
  public void testQueryGeneratorCountry() {
    EntityActivityQueryGenerator
        entityActivityQueryGenerator =
        EntityActivityQueryGenerator.getEntityActivityQueryGenerator();
    String query = entityActivityQueryGenerator.withDomainId(13l)
        .withCountry("INDIA")
        .withIsCount(false)
        .withExcludeEntityTags("'GMSD'")
        .withMaterialId(1234567l)
        .withAsOf(new Date())
        .withPeriod(7)
        .generate();
    assertNotNull(query);
    assertTrue("Query should contain country INDIA", query.contains("INDIA"));
    assertTrue("Query should contain exclude entity tag GMSD", query.contains("GMSD"));
    assertTrue("Query should contain material id 1234567", query.contains("1234567"));
  }

  @Test
  public void testQueryGeneratorState() {
    EntityActivityQueryGenerator
        entityActivityQueryGenerator =
        EntityActivityQueryGenerator.getEntityActivityQueryGenerator();
    String query = entityActivityQueryGenerator.withDomainId(13l)
        .withCountry("INDIA")
        .withState("KARNATAKA")
        .withIsCount(false)
        .withExcludeEntityTags("'GMSD'")
        .withMaterialId(1234567l)
        .withAsOf(new Date())
        .withPeriod(7)
        .generate();
    assertNotNull(query);
    assertTrue("Query should contain country INDIA", query.contains("INDIA"));
    assertTrue("Query should contain state KARNATAKA", query.contains("KARNATAKA"));
    assertTrue("Query should contain exclude entity tag GMSD", query.contains("GMSD"));
    assertTrue("Query should contain material id 1234567", query.contains("1234567"));
  }

  @Test
  public void testQueryGeneratorWithDateFilter() {
    EntityActivityQueryGenerator
        entityActivityQueryGenerator =
        EntityActivityQueryGenerator.getEntityActivityQueryGenerator();
    String query = entityActivityQueryGenerator.withDomainId(13l)
        .withCountry("INDIA")
        .withState("KARNATAKA")
        .withDistrict("BENGALURU")
        .withIsCount(false)
        .withAsOf(new Date())
        .withPeriod(7)
        .generate();
    assertNotNull(query);
    assertTrue("Query should contain country INDIA", query.contains("INDIA"));
    assertTrue("Query should contain state KARNATAKA", query.contains("KARNATAKA"));
    assertTrue("Query should contain district BENGALURU", query.contains("BENGALURU"));
    assertTrue("Query should fetch data from TRANSACTION table", query.contains("TRANSACTION"));
  }

}