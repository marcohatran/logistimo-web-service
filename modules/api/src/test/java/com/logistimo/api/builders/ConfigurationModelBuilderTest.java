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
import com.logistimo.api.models.configuration.ReturnsConfigModel;
import com.logistimo.config.models.ActualTransConfig;
import com.logistimo.config.models.InventoryConfig;
import com.logistimo.config.models.MatStatusConfig;
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
  private static final String ANTIBIOTIC_REASONS_CSV = "Broken,Bad condition";
  private static final String GENERAL_REASONS_CSV = "Damaged,Good condition";
  private static final String UNSORTED_TAGS = "General,Antibiotic,Emergency";
  private static final String SORTED_TAGS = "Antibiotic,Emergency,General";
  private static final String ANTIBIOTIC_REASONS_UNTRIMMED_CSV = "  Broken,  Bad condition";
  private static final String GENERAL_REASONS_UNTRIMMED_CSV = "  Damaged,  Good condition";


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
    Map<String,String> tagReasons = getTagReasons();
    List<InventoryConfigModel.MTagReason> mtagReasons = configModelBuilder.buildMTagReasonModelList(tagReasons);
    assertNotNull(mtagReasons);
    assertEquals(2, mtagReasons.size());
    assertEquals(ANTIBIOTIC, mtagReasons.get(0).mtg);
    assertEquals(ANTIBIOTIC_REASONS_CSV, mtagReasons.get(0).rsn);
    assertEquals(GENERAL, mtagReasons.get(1).mtg);
    assertEquals(GENERAL_REASONS_CSV, mtagReasons.get(1).rsn);
    mtagReasons = configModelBuilder.buildMTagReasonModelList(null);
    assertNotNull(mtagReasons);
    assertTrue(mtagReasons.isEmpty());
    mtagReasons = configModelBuilder.buildMTagReasonModelList(new HashMap<>(1));
    assertNotNull(mtagReasons);
    assertTrue(mtagReasons.isEmpty());
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
  public void testGetMapWithTrimmedReasons() throws Exception {
    List<InventoryConfigModel.MTagReason> reasons = getMTagReasons();
    Map<String,String>
        mapWithTrimmedRsns = configModelBuilder.getMapWithTrimmedReasons(reasons);
    assertNotNull(mapWithTrimmedRsns);
    assertEquals(mapWithTrimmedRsns.size(), 2);
    assertEquals(ANTIBIOTIC_REASONS_CSV, mapWithTrimmedRsns.get(ANTIBIOTIC));
    assertEquals(GENERAL_REASONS_CSV, mapWithTrimmedRsns.get(GENERAL));
    mapWithTrimmedRsns = configModelBuilder.getMapWithTrimmedReasons(null);
    assertNotNull(mapWithTrimmedRsns);
    assertTrue(mapWithTrimmedRsns.isEmpty());
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
    assertEquals(10, returnsConfigs.get(0).getIncomingDuration());
    assertEquals(1, returnsConfigs.get(0).getOutgoingDuration());

    returnsConfigs = configModelBuilder.buildReturnsConfigs(null);
    assertNotNull(returnsConfigs);
    assertTrue(returnsConfigs.isEmpty());
  }

  @Test
  public void testBuildReturnsConfig() throws Exception {
    ReturnsConfigModel returnsConfigModel = getReturnsConfigModel();
    ReturnsConfig returnsConfig = configModelBuilder.buildReturnsConfig(returnsConfigModel);
    assertNotNull(returnsConfig);
    assertEquals(10, returnsConfig.getIncomingDuration());
    assertEquals(1, returnsConfig.getOutgoingDuration());
    returnsConfig = configModelBuilder.buildReturnsConfig(null);
    assertNotNull(returnsConfig);
    assertEquals(ReturnsConfig.DEFAULT_INCOMING_DURATION, returnsConfig.getIncomingDuration());
    assertEquals(ReturnsConfig.DEFAULT_OUTGOING_DURATION, returnsConfig.getOutgoingDuration());
  }

  @Test
  public void testBuildReturnsConfigModels() throws Exception {
    List<ReturnsConfigModel> returnsConfigModels = configModelBuilder.buildReturnsConfigModels(
        getReturnsConfigs());
    assertNotNull(returnsConfigModels);
    assertEquals(getEntityTags(), returnsConfigModels.get(0).eTags);
    assertEquals(60, returnsConfigModels.get(0).incDur);
    assertEquals(10, returnsConfigModels.get(0).outDur);

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
    assertEquals(60, returnsConfigModel.incDur);
    assertEquals(10, returnsConfigModel.outDur);
    returnsConfigModel = configModelBuilder.buildReturnsConfigModel(new ReturnsConfig());
    assertNotNull(returnsConfigModel);
    assertNotNull(returnsConfigModel.eTags);
    assertTrue(returnsConfigModel.eTags.isEmpty());
    assertEquals(ReturnsConfig.DEFAULT_INCOMING_DURATION, returnsConfigModel.incDur);
    assertEquals(ReturnsConfig.DEFAULT_OUTGOING_DURATION, returnsConfigModel.outDur);
    returnsConfigModel = configModelBuilder.buildReturnsConfigModel(null);
    assertNotNull(returnsConfigModel);
    assertNotNull(returnsConfigModel.eTags);
    assertTrue(returnsConfigModel.eTags.isEmpty());
    assertEquals(ReturnsConfig.DEFAULT_INCOMING_DURATION, returnsConfigModel.incDur);
    assertEquals(ReturnsConfig.DEFAULT_OUTGOING_DURATION, returnsConfigModel.outDur);
  }

  @Test
  public void testGetReasonsByTagType() throws Exception {
    InventoryConfigModel invConfigModel = new InventoryConfigModel();
    invConfigModel.ri = invConfigModel.rr = invConfigModel.rs = invConfigModel.rd = invConfigModel.rt = invConfigModel.rri = invConfigModel.rro = ANTIBIOTIC_REASONS_UNTRIMMED_CSV;
    Map<String,String> reasonsByTransType = configModelBuilder.getReasonsByTransType(
        invConfigModel);
    assertNotNull(reasonsByTransType);
    assertEquals(7, reasonsByTransType.size());
    reasonsByTransType.entrySet().stream().forEach(e->assertEquals(ANTIBIOTIC_REASONS_CSV,e.getValue()));
  }

  @Test
  public void testTrimReasons() throws Exception {
    assertEquals("", configModelBuilder.trimReasons(""));
    assertEquals("", configModelBuilder.trimReasons(null));
    assertEquals(ANTIBIOTIC_REASONS_CSV,configModelBuilder.trimReasons(ANTIBIOTIC_REASONS_CSV));
    assertEquals(ANTIBIOTIC_REASONS_CSV,configModelBuilder.trimReasons(ANTIBIOTIC_REASONS_UNTRIMMED_CSV));
  }

  private Map<String,String> getInvOperationTagsMap() {
    Map<String,String> invOperationTags = new HashMap<>(2);
    invOperationTags.put(ITransaction.TYPE_RETURNS_INCOMING,TAGS_RETURNS_INCOMING);
    invOperationTags.put(ITransaction.TYPE_RETURNS_OUTGOING,TAGS_RETURNS_OUTGOING);
    return invOperationTags;
  }

  private Map<String,String> getTagReasons() {
    Map<String,String> tagReasons = new LinkedHashMap<>(2);
    tagReasons.put(ANTIBIOTIC,ANTIBIOTIC_REASONS_CSV);
    tagReasons.put(GENERAL,GENERAL_REASONS_CSV);
    return tagReasons;
  }

  private ActualTransConfig getActualTransactionConfig(String type) {
    ActualTransConfig actTransConfig = new ActualTransConfig();
    actTransConfig.setTy(type);
    return actTransConfig;
  }

  private List<InventoryConfigModel.MTagReason> getMTagReasons() {
    List<InventoryConfigModel.MTagReason> reasons = new ArrayList<>(2);
    reasons.add(getMTagReason(ANTIBIOTIC, ANTIBIOTIC_REASONS_UNTRIMMED_CSV));
    reasons.add(getMTagReason(GENERAL,GENERAL_REASONS_UNTRIMMED_CSV));
    return reasons;
  }

  private InventoryConfigModel.MTagReason getMTagReason(String mtag, String reason) {
    InventoryConfigModel.MTagReason mtagReason = new InventoryConfigModel.MTagReason();
    mtagReason.mtg = mtag;
    mtagReason.rsn = reason;
    return mtagReason;
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