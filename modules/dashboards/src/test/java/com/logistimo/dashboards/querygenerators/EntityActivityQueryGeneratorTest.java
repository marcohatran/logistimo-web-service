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

import java.util.Date;

import static org.junit.Assert.assertNotNull;

/**
 * Created by charan on 12/01/18.
 */
public class EntityActivityQueryGeneratorTest {

  @Test
  public void testQueryGenerator() {
    EntityActivityQueryGenerator
        entityActivityQueryGenerator =
        EntityActivityQueryGenerator.getEntityActivityQueryGenerator();
    PreparedStatementModel query = entityActivityQueryGenerator.withDomainId(13l)
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
  }

  @Test
  public void testQueryGeneratorExcludeEntity() {
    EntityActivityQueryGenerator
        entityActivityQueryGenerator =
        EntityActivityQueryGenerator.getEntityActivityQueryGenerator();
    PreparedStatementModel query = entityActivityQueryGenerator.withDomainId(13l)
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

  }

  @Test
  public void testQueryGeneratorCountry() {
    EntityActivityQueryGenerator
        entityActivityQueryGenerator =
        EntityActivityQueryGenerator.getEntityActivityQueryGenerator();
    PreparedStatementModel query = entityActivityQueryGenerator.withDomainId(13l)
        .withCountry("INDIA")
        .withIsCount(false)
        .withExcludeEntityTags("'GMSD'")
        .withMaterialId(1234567l)
        .withAsOf(new Date())
        .withPeriod(7)
        .generate();
    assertNotNull(query);

  }

  @Test
  public void testQueryGeneratorState() {
    EntityActivityQueryGenerator
        entityActivityQueryGenerator =
        EntityActivityQueryGenerator.getEntityActivityQueryGenerator();
    PreparedStatementModel query = entityActivityQueryGenerator.withDomainId(13l)
        .withCountry("INDIA")
        .withState("KARNATAKA")
        .withIsCount(false)
        .withExcludeEntityTags("'GMSD'")
        .withMaterialId(1234567l)
        .withAsOf(new Date())
        .withPeriod(7)
        .generate();
    assertNotNull(query);
  }

  @Test
  public void testQueryGeneratorWithDateFilter() {
    EntityActivityQueryGenerator
        entityActivityQueryGenerator =
        EntityActivityQueryGenerator.getEntityActivityQueryGenerator();
      PreparedStatementModel query = entityActivityQueryGenerator.withDomainId(13l)
        .withCountry("INDIA")
        .withState("KARNATAKA")
        .withDistrict("BENGALURU")
        .withIsCount(false)
        .withAsOf(new Date())
        .withPeriod(7)
        .generate();
    assertNotNull(query);
  }


}