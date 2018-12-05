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

package com.logistimo.assets;

import com.logistimo.assets.entity.Asset;
import com.logistimo.assets.entity.IAsset;
import com.logistimo.config.models.AssetSystemConfig;
import com.logistimo.config.models.ConfigurationException;

import org.json.JSONObject;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import static org.junit.Assert.*;
import static org.mockito.Mockito.when;
import static org.powermock.api.mockito.PowerMockito.mockStatic;

/**
 * @author Smriti
 */
@RunWith(PowerMockRunner.class)
@PrepareForTest({AssetSystemConfig.class})
public class AssetUtilTest {

  AssetSystemConfig assetSystemConfig;
  private static final String YEAR_OF_MANUFACTURE = "dev.yom";

  @Before
  public void setUp() throws ConfigurationException {
    assetSystemConfig = new AssetSystemConfig();
    mockStatic(AssetSystemConfig.class);
    when(AssetSystemConfig.getInstance()).thenReturn(assetSystemConfig);
  }

  @Test
  public void testGetAssetCapacityForMonitoringAsset() throws ConfigurationException {
    IAsset asset = setAsset("TL001", "nexleaf", "CT5", IAsset.MONITORING_ASSET);
    setAssetSystemConfigData("nexleaf", null, IAsset.MONITORING_ASSET);
    AssetUtil.Capacity assetCapacity = AssetUtil.getAssetCapacity(asset);
    assertNull(assetCapacity.qty);
    assertNull(assetCapacity.met);
  }

  @Test
  public void testGetAssetCapacityForMonitoredAsset() throws ConfigurationException {
    IAsset asset = setAsset("ILR-001", "haier", "HBC-200", IAsset.MONITORED_ASSET);
    setAssetSystemConfigData("HBC-200", "112", IAsset.MONITORED_ASSET);
    AssetUtil.Capacity assetCapacity = AssetUtil.getAssetCapacity(asset);
    assertNotNull(assetCapacity);
    assertEquals("112", assetCapacity.qty);
    assertEquals("Litres", assetCapacity.met);
  }


  /**
   * Tests the method when the model number entered by user and the configured model numbers are same.
   */
  @Test
  public void testSetAssetModelMetaData() throws ConfigurationException {
    Map<String, Object> metaDataMap = new HashMap<>();
    metaDataMap.put(AssetUtil.DEV_MODEL, "MF115");
    metaDataMap.put(YEAR_OF_MANUFACTURE, "2018");

    IAsset asset = setAsset("H1234A", "haier", "MF115", IAsset.MONITORED_ASSET);
    setAssetSystemConfigData("MF115", "94", IAsset.MONITORED_ASSET);

    AssetUtil.setAssetModelMetaData(metaDataMap, asset);
    JSONObject capacity = (JSONObject) metaDataMap.get("cc");
    assertNotNull(metaDataMap);
    assertEquals("MF115", metaDataMap.get(AssetUtil.DEV_MODEL));
    assertEquals("2018", metaDataMap.get(YEAR_OF_MANUFACTURE));
    assertEquals("94", capacity.get("qty"));
    assertEquals("Litres", capacity.get("met"));
  }

  /**
   * Tests the method when the model number entered by the user and the configured model numbers are different in cases.
   */
  @Test
  public void testSetAssetModelMetaDataForDifferentModels() throws ConfigurationException {
    Map<String, Object> metaDataMap = new HashMap<>();
    metaDataMap.put(AssetUtil.DEV_MODEL, "mf115");
    metaDataMap.put(YEAR_OF_MANUFACTURE, "2018");

    IAsset asset = setAsset("H1234A", "haier", "MF115", IAsset.MONITORED_ASSET);
    setAssetSystemConfigData("MF115", "94", IAsset.MONITORED_ASSET);

    AssetUtil.setAssetModelMetaData(metaDataMap, asset);
    JSONObject capacity = (JSONObject) metaDataMap.get("cc");
    assertNotNull(metaDataMap);
    assertEquals("MF115", metaDataMap.get(AssetUtil.DEV_MODEL));
    assertEquals("2018", metaDataMap.get(YEAR_OF_MANUFACTURE));
    assertEquals("94", capacity.get("qty"));
    assertEquals("Litres", capacity.get("met"));
  }

  private IAsset setAsset(String serialId, String vendorId, String model, int monitoringAsset) {
    IAsset asset = new Asset();
    asset.setSerialId(serialId);
    asset.setVendorId(vendorId);
    asset.setModel(model);
    asset.setType(monitoringAsset);
    return asset;
  }

  private void setAssetSystemConfigData(String modelName, String capacity, Integer assetType) {
    setAssetSystemConfigData(modelName, capacity, assetType, null);
  }

  private void setAssetSystemConfigData(String modelName, String capacity, Integer assetType,
                                        String assetName) {

    AssetSystemConfig.Model model = new AssetSystemConfig.Model();
    model.name = modelName;
    model.capacityInLitres = capacity;

    AssetSystemConfig.Manufacturer manufacturer = new AssetSystemConfig.Manufacturer();
    manufacturer.model = new ArrayList<>(1);
    manufacturer.model.add(0, model);

    Map<String, AssetSystemConfig.Manufacturer> manufacturerMap = new HashMap<>();
    manufacturerMap.put("haier", manufacturer);

    AssetSystemConfig.Asset asset = new AssetSystemConfig.Asset();
    asset.setManufacturers(manufacturerMap);
    asset.type = assetType;
    asset.setName(assetName);

    Map<Integer, AssetSystemConfig.Asset> assetMap = new HashMap<>();
    assetMap.put(assetType, asset);
    assetSystemConfig.assets = assetMap;
  }
}