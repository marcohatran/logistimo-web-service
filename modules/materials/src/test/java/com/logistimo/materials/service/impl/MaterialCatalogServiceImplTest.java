package com.logistimo.materials.service.impl;

import com.logistimo.materials.entity.IMaterialManufacturers;
import com.logistimo.materials.entity.MaterialManufacturers;
import com.logistimo.services.ServiceException;

import org.junit.Before;
import org.junit.Test;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

import javax.jdo.PersistenceManager;
import javax.jdo.Query;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertSame;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.when;

/**
 * Created by smriti on 12/10/17.
 */
public class MaterialCatalogServiceImplTest {
  MaterialCatalogServiceImpl mcs = spy(MaterialCatalogServiceImpl.class);
  List<IMaterialManufacturers> manufacturers = new ArrayList<>();
  PersistenceManager pm;
  Query q;

  @Before
  public void setup() throws ServiceException {
    pm = mock(PersistenceManager.class);
    q = mock(Query.class);
    String query = "SELECT * FROM MaterialManufacturers WHERE MATERIAL_ID = ?";
    doReturn(pm).when(mcs).getPM();
    when(pm.newQuery("javax.jdo.query.SQL", query)).thenReturn(q);
  }

  @Test
  public void testGetMaterialManufacturers() throws ServiceException {
    manufacturers = setManufacturers(1l, 11l, "Serum Institute of India", 2233l, 3345721l, new BigDecimal(20));
    when(q.execute(3345721l)).thenReturn(manufacturers);
    when(pm.detachCopyAll(manufacturers)).thenReturn(manufacturers);
    List<IMaterialManufacturers> manufacturerList = mcs.getMaterialManufacturers(3345721l);
    assertSame(manufacturerList, manufacturers);
  }

  @Test
  public void testEmptyMaterialManufacturers() throws ServiceException {
    List<IMaterialManufacturers> manufacturerList = mcs.getMaterialManufacturers(3345730l);
    assertEquals(manufacturerList.size(), 0);
  }

  List<IMaterialManufacturers> setManufacturers(Long key, Long code, String name, Long materialCode,
                                                Long materialId, BigDecimal qty) {
    MaterialManufacturers manufacturer = new MaterialManufacturers();
    manufacturer.setKey(key);
    manufacturer.setManufacturerCode(code);
    manufacturer.setManufacturerName(name);
    manufacturer.setMaterialCode(materialCode);
    manufacturer.setMaterialId(materialId);
    manufacturer.setQuantity(qty);
    manufacturers.add(manufacturer);
    return manufacturers;
  }
}