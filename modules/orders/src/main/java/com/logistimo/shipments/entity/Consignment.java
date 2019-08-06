/*
 * Copyright © 2019 Logistimo.
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

package com.logistimo.shipments.entity;


import java.math.BigDecimal;

import javax.jdo.annotations.IdGeneratorStrategy;
import javax.jdo.annotations.IdentityType;
import javax.jdo.annotations.PersistenceCapable;
import javax.jdo.annotations.Persistent;
import javax.jdo.annotations.PrimaryKey;

@PersistenceCapable(identityType = IdentityType.APPLICATION, detachable = "true",
    table = "CONSIGNMENTS")
public class Consignment implements IConsignment {

  @PrimaryKey
  @Persistent(valueStrategy = IdGeneratorStrategy.IDENTITY)
  private Long id;

  @Persistent
  private Integer numPackages;
  @Persistent
  private BigDecimal wtKg;
  @Persistent
  private Double length;
  @Persistent
  private Double breadth;
  @Persistent
  private Double height;
  @Persistent
  private String declaration;
  @Persistent
  private Double value;

  @Override
  public Long getId() {
    return id;
  }

  @Override
  public void setId(Long id) {
    this.id = id;
  }

  @Override
  public Integer getNumberOfPackages() {
    return numPackages;
  }

  @Override
  public void setNumberOfPackages(Integer numOfPckgs) {
    this.numPackages = numOfPckgs;
  }

  @Override
  public BigDecimal getWeightInKg() {
    return wtKg;
  }

  @Override
  public void setWeightInKg(BigDecimal wt) {
    this.wtKg = wt;
  }

  @Override
  public Double getLength() {
    return length;
  }

  @Override
  public void setLength(Double length) {
    this.length = length;
  }

  @Override
  public Double getBreadth() {
    return breadth;
  }

  @Override
  public void setBreadth(Double breadth) {
    this.breadth = breadth;
  }

  @Override
  public Double getHeight() {
    return height;
  }

  @Override
  public void setHeight(Double height) {
    this.height = height;
  }

  @Override
  public String getDeclaration() {
    return declaration;
  }

  @Override
  public void setDeclaration(String dec) {
    this.declaration = dec;
  }

  @Override
  public Double getValue() {
    return value;
  }

  @Override
  public void setValue(Double value) {
    this.value = value;
  }
}