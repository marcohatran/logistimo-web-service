package com.logistimo.entities.utils;

import com.logistimo.assets.entity.Asset;
import com.logistimo.assets.entity.AssetRelation;
import com.logistimo.assets.entity.IAsset;
import com.logistimo.assets.entity.IAssetRelation;
import com.logistimo.assets.service.AssetManagementService;
import com.logistimo.auth.utils.SecurityUtils;
import com.logistimo.context.StaticApplicationContext;
import com.logistimo.entities.entity.IKiosk;
import com.logistimo.entities.entity.Kiosk;
import com.logistimo.services.ServiceException;
import com.logistimo.utils.MsgUtil;


import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.when;
import static org.powermock.api.mockito.PowerMockito.mockStatic;

/**
 * Created by smriti on 02/05/18.
 */
@RunWith(PowerMockRunner.class)
@PrepareForTest({SecurityUtils.class, StaticApplicationContext.class})
public class EntityMoveHelperTest {

  @Mock
  AssetManagementService assetManagementService;

  @Before
  public void setup() {
    mockStatic(SecurityUtils.class);
    PowerMockito.when(SecurityUtils.getLocale()).thenReturn(Locale.ENGLISH);
    mockStatic(StaticApplicationContext.class);
    PowerMockito.when(StaticApplicationContext.getBean(AssetManagementService.class)).thenReturn(
        assetManagementService);
  }

  @Test
  public void testIsAssetsMovePossibleWithAssetRelationship() throws ServiceException {
    List<IKiosk> kioskList = new ArrayList<>();
    kioskList.add(getKiosk(1L, "India GMSD"));
    List<IAsset> assets = new ArrayList<>();
    assets.add(getAsset(2038L, "ILR001", "haier", 2, 1L));
    assets.add(getAsset(2039L, "ILR002", "vestfrost", 5, 1L));
    when(assetManagementService.getAssetsByKiosk(kioskList.get(0).getKioskId())).thenReturn(assets);
    IAssetRelation assetRelation = new AssetRelation();
    assetRelation.setAssetId(3000L);
    when(assetManagementService.getAssetRelationByAsset(2038L)).thenReturn(assetRelation);
    when(assetManagementService.getAssetRelationByAsset(2039L)).thenReturn(null);
    when(assetManagementService.getAsset(assetRelation.getAssetId())).thenReturn(getAsset(3000L, "TL001", "nexleaf",1,3L));
    List<String> errors = EntityMoveHelper.isAssetsMovePossible(kioskList);
    assertEquals(MsgUtil.newLine() + "Entity: India GMSD" + MsgUtil.newLine() + "Asset(s): ILR001", errors.get(0));
  }

  @Test
  public void testIsAssetsMovePossibleWithNoAssetRelationship() throws ServiceException {
    List<IKiosk> kioskList = new ArrayList<>();
    kioskList.add(getKiosk(1L, "StoreTest"));
    List<IAsset> assets = new ArrayList<>();
    assets.add(getAsset(2038L, "TL001", "nexleaf", 1, 1L));
    when(assetManagementService.getAssetsByKiosk(kioskList.get(0).getKioskId())).thenReturn(assets);
    when(assetManagementService.getAssetRelationByRelatedAsset(2038L)).thenReturn(null);
    List<String> errors = EntityMoveHelper.isAssetsMovePossible(kioskList);
    assertEquals(true, errors.isEmpty());
  }

  @Test
  public void testIsAssetsMovePossibleForEntitiesWithNoAssets() throws ServiceException {
    List<IKiosk> kioskList = new ArrayList<>();
    kioskList.add(getKiosk(1L, "Store Test"));
    List<String> errors = EntityMoveHelper.isAssetsMovePossible(kioskList);
    assertEquals(true, errors.isEmpty());
  }

  private IAsset getAsset(Long assetId, String serialNumber, String vendorId, Integer assetType, Long kioskId) {
    IAsset asset = new Asset();
    asset.setId(assetId);
    asset.setSerialId(serialNumber);
    asset.setVendorId(vendorId);
    asset.setType(assetType);
    asset.setKioskId(kioskId);
    return asset;
  }

  private IKiosk getKiosk(Long kioskId, String kioskName) {
    IKiosk kiosk = new Kiosk();
    kiosk.setKioskId(kioskId);
    kiosk.setName(kioskName);
    return kiosk;
  }
}