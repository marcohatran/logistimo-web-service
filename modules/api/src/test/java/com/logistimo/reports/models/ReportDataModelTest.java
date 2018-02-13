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

package com.logistimo.reports.models;

import org.junit.Test;

import java.util.HashMap;
import java.util.Map;

import static org.junit.Assert.assertTrue;

/**
 * Created by vani on 12/01/18.
 */
public class ReportDataModelTest {
  private static final  String EMPTY = "";

  @Test
  public void testEquals() {
    assertTrue(getReportDataModelWithOneParameter().equals(getReportDataModelWithOneParameter()));
    assertTrue(getReportDataModelWithTwoParameters().equals(
        getReportDataModelWithTwoParameters()));
    assertTrue(
        getReportDataModelWithThreeParameters().equals(getReportDataModelWithThreeParameters()));
    assertTrue(getReportDataModelWithSixParameters().equals(
        getReportDataModelWithSixParameters()));
    assertTrue(getReportDataModelWithEmptyParameters().equals(getReportDataModelWithEmptyParameters()));
    assertTrue(getReportDataModelWithOneNullParameter().equals(getReportDataModelWithOneNullParameter()));
    assertTrue(!getReportDataModelWithOneParameter().equals(getReportDataModelWithOneNullParameter()));
    assertTrue(!getReportDataModelWithOneParameter().equals(new ReportDataModel("300")));
  }
  @Test
  public void testHashCode() {
    assertTrue(getReportDataModelWithOneParameter().hashCode() == getReportDataModelWithOneParameter().hashCode());
    assertTrue(getReportDataModelWithTwoParameters().hashCode() == getReportDataModelWithTwoParameters().hashCode());
    assertTrue(getReportDataModelWithThreeParameters().hashCode() == getReportDataModelWithThreeParameters().hashCode());
    assertTrue(getReportDataModelWithSixParameters()
        .hashCode() == getReportDataModelWithSixParameters().hashCode());
    assertTrue(getReportDataModelWithEmptyParameters().hashCode() == getReportDataModelWithEmptyParameters().hashCode());
    assertTrue(getReportDataModelWithOneNullParameter().hashCode() == getReportDataModelWithOneNullParameter()
        .hashCode());
    assertTrue(getReportDataModelWithOneParameter().hashCode() !=  getReportDataModelWithOneNullParameter().hashCode());
    assertTrue(
        getReportDataModelWithOneParameter().hashCode() != new ReportDataModel("300").hashCode());

    Map<ReportDataModel,Integer> map = new HashMap<>(12,1);
    map.put(getReportDataModelWithOneParameter(),1);
    map.put(getReportDataModelWithOneParameter(),2);
    map.put(getReportDataModelWithTwoParameters(),1);
    map.put(getReportDataModelWithTwoParameters(),2);
    map.put(getReportDataModelWithThreeParameters(),1);
    map.put(getReportDataModelWithThreeParameters(),2);
    map.put(getReportDataModelWithSixParameters(),1);
    map.put(getReportDataModelWithSixParameters(),2);
    map.put(getReportDataModelWithOneNullParameter(),1);
    map.put(getReportDataModelWithOneNullParameter(),2);
    map.put(getReportDataModelWithEmptyParameters(),1);
    map.put(getReportDataModelWithEmptyParameters(),2);

    assertTrue(map.size() == 6);
    assertTrue(map.get(getReportDataModelWithOneParameter()) == 2);
    assertTrue(map.get(getReportDataModelWithTwoParameters()) == 2);
    assertTrue(map.get(getReportDataModelWithThreeParameters()) == 2);
    assertTrue(map.get(getReportDataModelWithSixParameters()) == 2);
    assertTrue(map.get(getReportDataModelWithOneNullParameter()) == 2);
    assertTrue(map.get(getReportDataModelWithEmptyParameters()) == 2);
  }

  private ReportDataModel getReportDataModelWithOneParameter() {
    return new ReportDataModel("100");
  }

  private ReportDataModel getReportDataModelWithTwoParameters() {
    return new ReportDataModel("100","200");
  }

  private ReportDataModel getReportDataModelWithThreeParameters() {
    return new ReportDataModel("100","1000","10");
  }
  private ReportDataModel getReportDataModelWithSixParameters() {
    return new ReportDataModel("100","1000","10","200","2000","20");
  }
  private ReportDataModel getReportDataModelWithEmptyParameters() {
    return new ReportDataModel(EMPTY,EMPTY,EMPTY,EMPTY,EMPTY,EMPTY);
  }
  private ReportDataModel getReportDataModelWithOneNullParameter() {
    return new ReportDataModel(null);
  }
}