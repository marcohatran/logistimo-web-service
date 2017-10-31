package com.logistimo.materials.entity;

import java.math.BigDecimal;

/**
 * Created by smriti on 10/10/17.
 */
public interface IMaterialManufacturers {

  Long getKey();

  void setKey(Long key);

  Long getManufacturerCode();

  void setManufacturerCode(Long mfrCode);

  Long getMaterialCode();

  void setMaterialCode(Long materialCode);

  BigDecimal getQuantity();

  void setQuantity(BigDecimal qty);

  Long getMaterialId();

  void setMaterialId(Long materialId);

  String getManufacturerName();

  void setManufacturerName(String mfrName);
}
