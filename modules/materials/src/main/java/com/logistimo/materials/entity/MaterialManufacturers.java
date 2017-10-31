package com.logistimo.materials.entity;

import java.math.BigDecimal;

import javax.jdo.annotations.Column;
import javax.jdo.annotations.IdGeneratorStrategy;
import javax.jdo.annotations.IdentityType;
import javax.jdo.annotations.PersistenceCapable;
import javax.jdo.annotations.Persistent;
import javax.jdo.annotations.PrimaryKey;

/**
 * Created by smriti on 10/10/17.
 */

@PersistenceCapable(identityType = IdentityType.APPLICATION, detachable = "true")
public class MaterialManufacturers implements IMaterialManufacturers {

  @PrimaryKey
  @Persistent(valueStrategy = IdGeneratorStrategy.IDENTITY)
  private Long key;

  @Persistent
  @Column(name = "material_id")
  private Long materialId;

  @Persistent
  @Column(name = "mfr_code")
  private Long mfrCode;

  @Persistent
  @Column(name = "mfr_name")
  private String mfrName;

  @Persistent
  @Column(name = "material_code")
  private Long materialCode;

  @Persistent
  private BigDecimal qty;

  @Override
  public Long getKey() { return key; }

  @Override
  public void setKey(Long key) { this.key = key; }

  @Override
  public Long getManufacturerCode() { return mfrCode; }

  @Override
  public void setManufacturerCode(Long mfrCode) { this.mfrCode = mfrCode; }

  @Override
  public Long getMaterialCode() { return materialCode; }

  @Override
  public void setMaterialCode(Long materialCode) { this.materialCode = materialCode; }

  @Override
  public BigDecimal getQuantity() { return qty; }

  @Override
  public void setQuantity(BigDecimal qty) { this.qty = qty; }

  @Override
  public Long getMaterialId() { return materialId; }

  @Override
  public void setMaterialId(Long materialId) { this.materialId = materialId; }

  @Override
  public String getManufacturerName() { return mfrName; }

  @Override
  public void setManufacturerName(String mfrName) { this.mfrName = mfrName; }

}
