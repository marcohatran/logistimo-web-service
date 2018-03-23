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

package com.logistimo.api.builders;

import com.logistimo.api.models.configuration.CapabilitiesConfigModel;
import com.logistimo.api.models.configuration.InventoryConfigModel;
import com.logistimo.api.models.configuration.ReasonConfigModel;
import com.logistimo.api.models.configuration.ReturnsConfigModel;
import com.logistimo.config.models.ActualTransConfig;
import com.logistimo.config.models.InventoryConfig;
import com.logistimo.config.models.MatStatusConfig;
import com.logistimo.config.models.ReasonConfig;
import com.logistimo.config.models.ReturnsConfig;
import com.logistimo.inventory.entity.ITransaction;
import com.logistimo.services.ServiceException;

import org.junit.Before;
import org.junit.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;


/**
 * Created by vani on 25/01/18.
 */
public class ConfigurationModelBuilderTest {
  ConfigurationModelBuilder configModelBuilder;
  private static final String ANTIBIOTIC = "Antibiotic";
  private static final String GENERAL = "General";
  private static final String EMERGENCY = "Emergency";
  private static final String VACCINES = "Vaccines";
  private static final String SYRINGES = "Syringes";
  private static final String CAMPAIGN = "Campaign";
  private static final String OPEN_VIALS = "Open vials";
  private static final String TAGS_RETURNS_INCOMING = "Antibiotic,General,Emergency";
  private static final String TAGS_RETURNS_OUTGOING = "Vaccines,Syringes,Campaign,Open vials";
  private static final String UNSORTED_TAGS = "General,Antibiotic,Emergency";
  private static final String SORTED_TAGS = "Antibiotic,Emergency,General";
  private static final String ANTIBIOTIC_REASONS_UNTRIMMED_CSV = "  Broken,  Bad condition";
  private static final String GENERAL_REASONS_UNTRIMMED_CSV = "  Damaged,  Good condition";
  private static final String BROKEN = "Broken";
  private static final String BAD_CONDITION = "Bad condition";
  private static final String DAMAGED = "Damaged";
  private static final String GOOD_CONDITION = "Good condition";




  @Before
  public void setup() throws ServiceException {
    configModelBuilder = new ConfigurationModelBuilder();
  }
  @Test
  public void testGetActualTransConfigType() throws Exception {
    InventoryConfig invConfig = new InventoryConfig();
    ActualTransConfig actTransConfig = getActualTransactionConfig("0");
    invConfig.setActualTransDateByType(ITransaction.TYPE_RETURNS_INCOMING, actTransConfig);
    String actualTransConfigType = configModelBuilder.getActualTransConfigType(
        ITransaction.TYPE_RETURNS_INCOMING,
        invConfig);
    assertEquals(ActualTransConfig.ACTUAL_NONE, actualTransConfigType);
    actTransConfig = getActualTransactionConfig("1");
    invConfig.setActualTransDateByType(ITransaction.TYPE_RETURNS_INCOMING, actTransConfig);
    actualTransConfigType = configModelBuilder.getActualTransConfigType(
        ITransaction.TYPE_RETURNS_INCOMING,
        invConfig);
    assertEquals("1", actualTransConfigType);
    actTransConfig = getActualTransactionConfig("2");
    invConfig.setActualTransDateByType(ITransaction.TYPE_RETURNS_INCOMING, actTransConfig);
    actualTransConfigType = configModelBuilder.getActualTransConfigType(
        ITransaction.TYPE_RETURNS_INCOMING,
        invConfig);
    assertEquals("2", actualTransConfigType);

    invConfig.setActualTransDateByType(ITransaction.TYPE_RETURNS_INCOMING, null);
    actualTransConfigType = configModelBuilder.getActualTransConfigType(
        ITransaction.TYPE_RETURNS_INCOMING,
        invConfig);
    assertEquals(ActualTransConfig.ACTUAL_NONE, actualTransConfigType);
  }

  @Test
  public void testBuildMTagReasonModelList() throws Exception {
    Map<String,ReasonConfig> reasonConfigByTagMap = getReasonConfigByTagMap();
    List<InventoryConfigModel.MTagReason> mtagReasons = configModelBuilder.buildMTagReasonModelList(reasonConfigByTagMap);
    assertNotNull(mtagReasons);
    assertEquals(2, mtagReasons.size());
    assertEquals(ANTIBIOTIC, mtagReasons.get(0).mtg);
    assertEquals(getReasonConfigModelForAntibiotic().defRsn, mtagReasons.get(0).rsnCfgModel.defRsn);
    assertEquals(getReasonConfigModelForAntibiotic().rsns, mtagReasons.get(0).rsnCfgModel.rsns);
    assertEquals(GENERAL, mtagReasons.get(1).mtg);
    assertEquals(getReasonConfigModelForGeneral().defRsn, mtagReasons.get(1).rsnCfgModel.defRsn);
    assertEquals(getReasonConfigModelForGeneral().rsns, mtagReasons.get(1).rsnCfgModel.rsns);
    mtagReasons = configModelBuilder.buildMTagReasonModelList(null);
    assertNotNull(mtagReasons);
    assertTrue(mtagReasons.isEmpty());
    mtagReasons = configModelBuilder.buildMTagReasonModelList(new HashMap<>(1));
    assertNotNull(mtagReasons);
    assertTrue(mtagReasons.isEmpty());
  }

  @Test
  public void testBuildReasonConfigByTransType() throws Exception {
    InventoryConfigModel invConfigModel = new InventoryConfigModel();
    invConfigModel.ri = invConfigModel.rr = invConfigModel.rs = invConfigModel.rd = invConfigModel.rt = invConfigModel.rri = invConfigModel.rro = getReasonConfigModelForAntibiotic();
    Map<String,ReasonConfig> reasonConfigByTransType = configModelBuilder.buildReasonConfigByTransType(
        invConfigModel);
    assertNotNull(reasonConfigByTransType);
    assertEquals(7, reasonConfigByTransType.size());
    reasonConfigByTransType.entrySet().stream().forEach(e -> {
      assertNotNull(e.getValue());
      assertEquals(getReasonConfigForAntibiotic().getDefaultReason(),e.getValue().getDefaultReason());
      assertEquals(getReasonConfigForAntibiotic().getReasons(),e.getValue().getReasons());
    });
  }

  @Test
  public void testBuildReasonConfig() throws Exception {
    ReasonConfigModel reasonConfigModel = getReasonConfigModelForAntibiotic();
    ReasonConfig reasonConfig = configModelBuilder.buildReasonConfig(reasonConfigModel);
    assertNotNull(reasonConfig);
    assertEquals(getReasonConfigForAntibiotic().getDefaultReason(),reasonConfig.getDefaultReason());
    assertEquals(getReasonConfigForAntibiotic().getReasons(),reasonConfig.getReasons());
    reasonConfig = configModelBuilder.buildReasonConfig(null);
    assertNotNull(reasonConfig);
  }

  @Test
  public void testGetTagsByInvOperationAsList() throws Exception {
    Map<String,String> invOperationTypeTags = getInvOperationTagsMap();
    List<String> tagsRetInc = new ArrayList<>(3);
    tagsRetInc.add(ANTIBIOTIC);
    tagsRetInc.add(GENERAL);
    tagsRetInc.add(EMERGENCY);
    List<String> tagsRetOut = new ArrayList<>(4);
    tagsRetOut.add(VACCINES);
    tagsRetOut.add(SYRINGES);
    tagsRetOut.add(CAMPAIGN);
    tagsRetOut.add(OPEN_VIALS);
    List<String>
        tags = configModelBuilder.getTagsByInvOperationAsList(invOperationTypeTags,
        ITransaction.TYPE_RETURNS_INCOMING);
    assertNotNull(tags);
    assertEquals(3, tags.size());
    assertEquals(tagsRetInc, tags);
    tags = configModelBuilder.getTagsByInvOperationAsList(invOperationTypeTags,
        ITransaction.TYPE_RETURNS_OUTGOING);
    assertEquals(4, tags.size());
    assertEquals(tagsRetOut, tags);
    tags = configModelBuilder.getTagsByInvOperationAsList(null, ITransaction.TYPE_RETURNS_INCOMING);
    assertNotNull(tags);
    assertTrue(tags.isEmpty());
    tags = configModelBuilder.getTagsByInvOperationAsList(new HashMap<>(),
        ITransaction.TYPE_RETURNS_INCOMING);
    assertNotNull(tags);
    assertTrue(tags.isEmpty());
    tags = configModelBuilder.getTagsByInvOperationAsList(invOperationTypeTags, "Unknown");
    assertNotNull(tags);
    assertTrue(tags.isEmpty());
  }

  @Test
  public void testGetTagsByInventoryOperation() throws Exception {
    CapabilitiesConfigModel capConfigModel = new CapabilitiesConfigModel();
    String[] tags = new String[]{UNSORTED_TAGS};
    capConfigModel.hii = capConfigModel.hir = capConfigModel.hip = capConfigModel.hiw
        = capConfigModel.hit = capConfigModel.hiri = capConfigModel.hiro = Arrays.asList(tags);
    Map<String,String> tagsByInvOperation = configModelBuilder.getTagsByInventoryOperation(capConfigModel);
    assertNotNull(tagsByInvOperation);
    assertTrue(tagsByInvOperation.size() == 7);
    assertEquals(SORTED_TAGS, tagsByInvOperation.get(ITransaction.TYPE_PHYSICALCOUNT));
    capConfigModel.hii = capConfigModel.hir = capConfigModel.hip = capConfigModel.hiw
        = capConfigModel.hit = capConfigModel.hiri = capConfigModel.hiro = Collections.emptyList();
    tagsByInvOperation = configModelBuilder.getTagsByInventoryOperation(capConfigModel);
    assertNotNull(tagsByInvOperation);
    assertTrue(tagsByInvOperation.get(ITransaction.TYPE_PHYSICALCOUNT).isEmpty());
  }


  @Test
  public void testBuildActualTransConfig() throws Exception {
    ActualTransConfig actualTransConfig = configModelBuilder.buildActualTransConfig("2");
    assertNotNull(actualTransConfig);
    assertEquals(actualTransConfig.getTy(),"2");
    actualTransConfig = configModelBuilder.buildActualTransConfig(null);
    assertNotNull(actualTransConfig);
    assertEquals(actualTransConfig.getTy(),"0");
    actualTransConfig = configModelBuilder.buildActualTransConfig("");
    assertNotNull(actualTransConfig);
    assertEquals(actualTransConfig.getTy(),"");
  }

  @Test
  public void testBuildMatStatusConfig() throws Exception {
    MatStatusConfig matStatusConfig = configModelBuilder.buildMatStatusConfig(",Bad,Good,,,",
        ",,,,Frozen,,,,Overheated,,,", true);
    assertNotNull(matStatusConfig);
    assertEquals("Bad,Good", matStatusConfig.getDf());
    assertEquals("Frozen,Overheated", matStatusConfig.getEtsm());
    assertTrue(matStatusConfig.isStatusMandatory());
  }

  @Test
  public void testBuildReturnsConfigs() throws Exception {
    List<ReturnsConfig> returnsConfigs = configModelBuilder.buildReturnsConfigs(
        getReturnsConfigModels());
    assertNotNull(returnsConfigs);
    assertTrue(!returnsConfigs.isEmpty());
    assertTrue(returnsConfigs.size() == 1);
    assertEquals(getEntityTags(), returnsConfigs.get(0).getEntityTags());
    assertEquals(10, returnsConfigs.get(0).getIncomingDuration().intValue());
    assertEquals(1, returnsConfigs.get(0).getOutgoingDuration().intValue());

    returnsConfigs = configModelBuilder.buildReturnsConfigs(null);
    assertNotNull(returnsConfigs);
    assertTrue(returnsConfigs.isEmpty());
  }

  @Test
  public void testBuildReturnsConfig() throws Exception {
    ReturnsConfigModel returnsConfigModel = getReturnsConfigModel();
    ReturnsConfig returnsConfig = configModelBuilder.buildReturnsConfig(returnsConfigModel);
    assertNotNull(returnsConfig);
    assertEquals(10, returnsConfig.getIncomingDuration().intValue());
    assertEquals(1, returnsConfig.getOutgoingDuration().intValue());
    returnsConfig = configModelBuilder.buildReturnsConfig(null);
    assertNotNull(returnsConfig);
    assertNull(returnsConfig.getIncomingDuration());
    assertNull(returnsConfig.getOutgoingDuration());
  }

  @Test
  public void testBuildReturnsConfigModels() throws Exception {
    List<ReturnsConfigModel> returnsConfigModels = configModelBuilder.buildReturnsConfigModels(
        getReturnsConfigs());
    assertNotNull(returnsConfigModels);
    assertEquals(getEntityTags(), returnsConfigModels.get(0).eTags);
    assertEquals(60, returnsConfigModels.get(0).incDur.intValue());
    assertEquals(10, returnsConfigModels.get(0).outDur.intValue());

    returnsConfigModels = configModelBuilder.buildReturnsConfigModels(Collections.emptyList());
    assertNotNull(returnsConfigModels);
    assertTrue(returnsConfigModels.isEmpty());

    returnsConfigModels = configModelBuilder.buildReturnsConfigModels(null);
    assertNotNull(returnsConfigModels);
    assertTrue(returnsConfigModels.isEmpty());
  }

  @Test
  public void testBuildReturnsConfigModel() throws Exception {
    ReturnsConfig returnsConfig = getReturnsConfig();
    ReturnsConfigModel
        returnsConfigModel = configModelBuilder.buildReturnsConfigModel(returnsConfig);
    assertNotNull(returnsConfigModel);
    assertEquals(getEntityTags(), returnsConfigModel.eTags);
    assertEquals(60, returnsConfigModel.incDur.intValue());
    assertEquals(10, returnsConfigModel.outDur.intValue());
    returnsConfigModel = configModelBuilder.buildReturnsConfigModel(new ReturnsConfig());
    assertNotNull(returnsConfigModel);
    assertNotNull(returnsConfigModel.eTags);
    assertTrue(returnsConfigModel.eTags.isEmpty());
    assertNull(returnsConfigModel.incDur);
    assertNull(returnsConfigModel.outDur);
    returnsConfigModel = configModelBuilder.buildReturnsConfigModel(null);
    assertNotNull(returnsConfigModel);
    assertNotNull(returnsConfigModel.eTags);
    assertTrue(returnsConfigModel.eTags.isEmpty());
    assertNull(returnsConfigModel.incDur);
    assertNull(returnsConfigModel.outDur);
  }

  @Test
  public void testBuildActualTransConfigAsStringByTransType() {
    InventoryConfig invConfig = new InventoryConfig();
    ActualTransConfig actTransConfig = getActualTransactionConfig("0");
    invConfig.setActualTransDateByType(ITransaction.TYPE_ISSUE, actTransConfig);
    invConfig.setActualTransDateByType(ITransaction.TYPE_RECEIPT, actTransConfig);
    invConfig.setActualTransDateByType(ITransaction.TYPE_PHYSICALCOUNT, actTransConfig);
    invConfig.setActualTransDateByType(ITransaction.TYPE_WASTAGE, actTransConfig);
    invConfig.setActualTransDateByType(ITransaction.TYPE_TRANSFER, actTransConfig);
    invConfig.setActualTransDateByType(ITransaction.TYPE_RETURNS_INCOMING, actTransConfig);
    invConfig.setActualTransDateByType(ITransaction.TYPE_RETURNS_OUTGOING, actTransConfig);
    Map<String,String> actualTransConfigAsStringByTransType = configModelBuilder.buildActualTransConfigAsStringByTransType(invConfig);
    assertNotNull(actualTransConfigAsStringByTransType);
    assertEquals(7,actualTransConfigAsStringByTransType.size());
    Map<String,String> expectedMap = Collections.unmodifiableMap(new HashMap<String, String>() {
      {
        put(ITransaction.TYPE_ISSUE, "0");
        put(ITransaction.TYPE_RECEIPT, "0");
        put(ITransaction.TYPE_PHYSICALCOUNT, "0");
        put(ITransaction.TYPE_WASTAGE, "0");
        put(ITransaction.TYPE_TRANSFER, "0");
        put(ITransaction.TYPE_RETURNS_INCOMING, "0");
        put(ITransaction.TYPE_RETURNS_OUTGOING, "0");
      }
    });
    assertEquals(expectedMap,actualTransConfigAsStringByTransType);
    invConfig.setActualTransDateByType(ITransaction.TYPE_ISSUE, null);
    invConfig.setActualTransDateByType(ITransaction.TYPE_RECEIPT, null);
    invConfig.setActualTransDateByType(ITransaction.TYPE_PHYSICALCOUNT, null);
    invConfig.setActualTransDateByType(ITransaction.TYPE_WASTAGE, null);
    invConfig.setActualTransDateByType(ITransaction.TYPE_TRANSFER, null);
    invConfig.setActualTransDateByType(ITransaction.TYPE_RETURNS_INCOMING, null);
    invConfig.setActualTransDateByType(ITransaction.TYPE_RETURNS_OUTGOING, null);
    actualTransConfigAsStringByTransType = configModelBuilder.buildActualTransConfigAsStringByTransType(invConfig);
    assertNotNull(actualTransConfigAsStringByTransType);
    assertEquals(7,actualTransConfigAsStringByTransType.size());
    assertEquals(expectedMap,actualTransConfigAsStringByTransType);
  }

  private Map<String,String> getInvOperationTagsMap() {
    Map<String,String> invOperationTags = new HashMap<>(2);
    invOperationTags.put(ITransaction.TYPE_RETURNS_INCOMING,TAGS_RETURNS_INCOMING);
    invOperationTags.put(ITransaction.TYPE_RETURNS_OUTGOING,TAGS_RETURNS_OUTGOING);
    return invOperationTags;
  }

  private Map<String,ReasonConfig> getReasonConfigByTagMap() {
    Map<String,ReasonConfig> reasonConfigByTagMap = new LinkedHashMap<>(2);
    reasonConfigByTagMap.put(ANTIBIOTIC, getReasonConfigForAntibiotic());
    reasonConfigByTagMap.put(GENERAL,getReasonConfigForGeneral());
    return reasonConfigByTagMap;
  }

  private ReasonConfig getReasonConfigForAntibiotic() {
    ReasonConfig reasonConfig = new ReasonConfig();
    reasonConfig.setReasons(Arrays.asList(new String[]{BROKEN,BAD_CONDITION}));
    reasonConfig.setDefaultReason(BROKEN);
    return  reasonConfig;
  }
  private ReasonConfig getReasonConfigForGeneral() {
    ReasonConfig reasonConfig = new ReasonConfig();
    reasonConfig.setReasons(Arrays.asList(new String[]{DAMAGED,GOOD_CONDITION}));
    reasonConfig.setDefaultReason(DAMAGED);
    return  reasonConfig;
  }
  private ReasonConfigModel getReasonConfigModelForAntibiotic() {
    ReasonConfigModel reasonConfigModel = new ReasonConfigModel();
    reasonConfigModel.rsns = Arrays.asList(new String[]{BROKEN,BAD_CONDITION});
    reasonConfigModel.defRsn = BROKEN;
    return reasonConfigModel;
  }

  private ReasonConfigModel getReasonConfigModelForGeneral() {
    ReasonConfigModel reasonConfigModel = new ReasonConfigModel();
    reasonConfigModel.rsns = Arrays.asList(new String[]{DAMAGED,GOOD_CONDITION});
    reasonConfigModel.defRsn = DAMAGED;
    return reasonConfigModel;
  }

  private ActualTransConfig getActualTransactionConfig(String type) {
    ActualTransConfig actTransConfig = new ActualTransConfig();
    actTransConfig.setTy(type);
    return actTransConfig;
  }

  private List<ReturnsConfigModel> getReturnsConfigModels() {
    List<ReturnsConfigModel>  returnsConfigModels = new ArrayList<>(1);
    returnsConfigModels.add(getReturnsConfigModel());
    return returnsConfigModels;
  }

  private ReturnsConfigModel getReturnsConfigModel() {
    ReturnsConfigModel returnsConfigModel = new ReturnsConfigModel();
    returnsConfigModel.eTags = Arrays.asList("CCP","DVS");
    returnsConfigModel.incDur = 10;
    returnsConfigModel.outDur = 1;
    return returnsConfigModel;
  }

  private List<ReturnsConfig> getReturnsConfigs() {
    List<ReturnsConfig>  returnsConfigs = new ArrayList<>(1);
    returnsConfigs.add(getReturnsConfig());
    return returnsConfigs;
  }

  private ReturnsConfig getReturnsConfig() {
    ReturnsConfig returnsConfig = new ReturnsConfig();
    returnsConfig.setEntityTags(getEntityTags());
    returnsConfig.setIncomingDuration(60);
    returnsConfig.setOutgoingDuration(10);
    return returnsConfig;
  }

  private List<String> getEntityTags() {
    List<String> entityTags = new ArrayList<>(2);
    entityTags.add("CCP");
    entityTags.add("DVS");
    return entityTags;
  }
}