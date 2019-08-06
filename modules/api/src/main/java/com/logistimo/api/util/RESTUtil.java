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

/**
 *
 */
package com.logistimo.api.util;


import com.logistimo.accounting.entity.IAccount;
import com.logistimo.accounting.service.IAccountingService;
import com.logistimo.accounting.service.impl.AccountingServiceImpl;
import com.logistimo.api.auth.AuthenticationUtil;
import com.logistimo.api.servlets.mobile.builders.MobileConfigBuilder;
import com.logistimo.api.servlets.mobile.builders.MobileDefaultReasonsConfigModel;
import com.logistimo.api.servlets.mobile.builders.MobileEntityBuilder;
import com.logistimo.api.servlets.mobile.builders.MobileReturnsConfigModel;
import com.logistimo.api.servlets.mobile.models.ParsedRequest;
import com.logistimo.auth.SecurityConstants;
import com.logistimo.auth.SecurityMgr;
import com.logistimo.auth.SecurityUtil;
import com.logistimo.auth.service.AuthenticationService;
import com.logistimo.auth.service.impl.AuthenticationServiceImpl;
import com.logistimo.auth.utils.SecurityUtils;
import com.logistimo.communications.service.SMSService;
import com.logistimo.config.entity.IConfig;
import com.logistimo.config.models.AccountingConfig;
import com.logistimo.config.models.ActualTransConfig;
import com.logistimo.config.models.ApprovalsConfig;
import com.logistimo.config.models.CapabilityConfig;
import com.logistimo.config.models.ConfigurationException;
import com.logistimo.config.models.DomainConfig;
import com.logistimo.config.models.GeneralConfig;
import com.logistimo.config.models.InventoryConfig;
import com.logistimo.config.models.MatStatusConfig;
import com.logistimo.config.models.OptimizerConfig;
import com.logistimo.config.models.OrdersConfig;
import com.logistimo.config.models.ReasonConfig;
import com.logistimo.config.models.ReturnsConfig;
import com.logistimo.config.models.SMSConfig;
import com.logistimo.config.models.SupportConfig;
import com.logistimo.config.models.SyncConfig;
import com.logistimo.config.service.ConfigurationMgmtService;
import com.logistimo.constants.CharacterConstants;
import com.logistimo.constants.Constants;
import com.logistimo.constants.SourceConstants;
import com.logistimo.context.StaticApplicationContext;
import com.logistimo.dao.JDOUtils;
import com.logistimo.entities.auth.EntityAuthoriser;
import com.logistimo.entities.entity.IApprover;
import com.logistimo.entities.entity.IKiosk;
import com.logistimo.entities.entity.IKioskLink;
import com.logistimo.entities.models.KioskLinkFilters;
import com.logistimo.entities.models.UserEntitiesModel;
import com.logistimo.entities.service.EntitiesService;
import com.logistimo.entities.service.EntitiesServiceImpl;
import com.logistimo.exception.ExceptionUtils;
import com.logistimo.exception.InvalidDataException;
import com.logistimo.exception.SystemException;
import com.logistimo.exception.UnauthorizedException;
import com.logistimo.exports.BulkExportMgr;
import com.logistimo.exports.MobileExportAdapter;
import com.logistimo.exports.util.MobileExportConstants;
import com.logistimo.inventory.TransactionUtil;
import com.logistimo.inventory.dao.ITransDao;
import com.logistimo.inventory.entity.IInvntry;
import com.logistimo.inventory.entity.IInvntryBatch;
import com.logistimo.inventory.entity.ITransaction;
import com.logistimo.inventory.models.InventoryFilters;
import com.logistimo.inventory.service.InventoryManagementService;
import com.logistimo.inventory.service.impl.InventoryManagementServiceImpl;
import com.logistimo.logger.XLog;
import com.logistimo.materials.entity.IHandlingUnit;
import com.logistimo.materials.entity.IMaterial;
import com.logistimo.materials.entity.IMaterialManufacturers;
import com.logistimo.materials.service.IHandlingUnitService;
import com.logistimo.materials.service.MaterialCatalogService;
import com.logistimo.materials.service.impl.HandlingUnitServiceImpl;
import com.logistimo.materials.service.impl.MaterialCatalogServiceImpl;
import com.logistimo.models.AuthRequest;
import com.logistimo.models.users.UserDetails;
import com.logistimo.orders.entity.IOrder;
import com.logistimo.pagination.PageParams;
import com.logistimo.pagination.Results;
import com.logistimo.proto.BasicOutput;
import com.logistimo.proto.JsonTagsZ;
import com.logistimo.proto.MaterialRequest;
import com.logistimo.proto.MobileAccountingConfigModel;
import com.logistimo.proto.MobileApprovalsConfigModel;
import com.logistimo.proto.ProtocolException;
import com.logistimo.proto.RestConstantsZ;
import com.logistimo.proto.UpdateInventoryInput;
import com.logistimo.proto.UpdateOrderRequest;
import com.logistimo.security.SecureUserDetails;
import com.logistimo.services.ObjectNotFoundException;
import com.logistimo.services.Resources;
import com.logistimo.services.ServiceException;
import com.logistimo.services.impl.PMF;
import com.logistimo.tags.TagUtil;
import com.logistimo.users.entity.IUserAccount;
import com.logistimo.users.service.UsersService;
import com.logistimo.users.service.impl.UsersServiceImpl;
import com.logistimo.utils.BigUtil;
import com.logistimo.utils.LocalDateUtil;
import com.logistimo.utils.StringUtil;

import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.collections.MapUtils;
import org.apache.commons.lang.StringUtils;

import java.io.IOException;
import java.io.PrintWriter;
import java.io.Serializable;
import java.math.BigDecimal;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Collections;
import java.util.Date;
import java.util.Enumeration;
import java.util.GregorianCalendar;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.ResourceBundle;
import java.util.Set;
import java.util.Vector;

import javax.jdo.JDOObjectNotFoundException;
import javax.jdo.PersistenceManager;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;


/**
 * @author Arun
 */
public class RESTUtil {

  public static final String VERSION_01 = "01";

  private static final XLog xLogger = XLog.getLog(RESTUtil.class);

  // URLs
  private static final String TRANSACTIONS = "transactions";
  private static final String INVENTORY = "inventory";
  private static final String ORDERS = "orders";
  private static final String TRANSFERS = "transfers";


  private static final String MINIMUM_RESPONSE_CODE_TWO = "2";

  @SuppressWarnings("unchecked")
  public static Results getInventoryData(Long kioskId, Locale locale,
                                         String timezone, boolean onlyStock,
                                         DomainConfig dc, boolean forceIntegerForStock, Date start,
                                         Optional<Date> modifiedSince,
                                         PageParams pageParams) throws ServiceException {
    xLogger.fine("Entered getInventoryData");
    // Get the services
    InventoryManagementService ims =
        StaticApplicationContext.getBean(InventoryManagementServiceImpl.class);
    MaterialCatalogService mcs = StaticApplicationContext.getBean(MaterialCatalogServiceImpl.class);
    // Get optimization config
    OptimizerConfig oc = dc.getOptimizerConfig();
    InventoryConfig ic = dc.getInventoryConfig();
    boolean hasDemandForecast = (oc.getCompute() == OptimizerConfig.COMPUTE_FORECASTEDDEMAND);
    boolean hasEOQ = (oc.getCompute() == OptimizerConfig.COMPUTE_EOQ);
    boolean hasStartDate = (start != null);
    // Get the inventories
    InventoryFilters filters = new InventoryFilters()
        .withKioskId(kioskId)
        .withUpdatedSince(modifiedSince.orElse(null));

    Results results = ims.getInventory(filters, pageParams);
    IKiosk kiosk = StaticApplicationContext.getBean(EntitiesServiceImpl.class).getKiosk(kioskId);
    List<IInvntry> inventories = (List<IInvntry>) results.getResults();
    String cursor = results.getCursor();
    boolean isBatchMgmtEnabled = kiosk.isBatchMgmtEnabled();
    Vector<Hashtable<String, Object>> invData = new Vector<>();
    for (IInvntry inv : inventories) {
      // Get the material data
      IMaterial m;
      List<IMaterialManufacturers> manufacturers;
      try {
        m = mcs.getMaterial(inv.getMaterialId());
        manufacturers = mcs.getMaterialManufacturers(m.getMaterialId());
      } catch (Exception e) {
        xLogger
            .warn("{0} when getting material {1}: {2}", e.getClass().getName(), inv.getMaterialId(),
                e.getMessage());
        continue;
      }
      // Create a material data hashtable
      Hashtable<String, Object> material = new Hashtable<>();
      material.put(JsonTagsZ.MATERIAL_ID, m.getMaterialId().toString());
      String stockStr;
      if (forceIntegerForStock) {
        stockStr = BigUtil.getFormattedValue(inv.getStock());
      } else {
        stockStr = BigUtil.getFormattedValue(inv.getStock());
      }
      material.put(JsonTagsZ.QUANTITY, stockStr);
      // Send the allocated quantity, in transit quantity and available quantity only if automactic posting of issues on shipping an order is configured.
      if (dc.autoGI()) {
        material
            .put(JsonTagsZ.ALLOCATED_QUANTITY, BigUtil.getFormattedValue(inv.getAllocatedStock()));
        material
            .put(JsonTagsZ.INTRANSIT_QUANTITY, BigUtil.getFormattedValue(inv.getInTransitStock()));
        material
            .put(JsonTagsZ.AVAILABLE_QUANTITY, BigUtil.getFormattedValue(inv.getAvailableStock()));
      }

      Date time = inv.getTimestamp();
      material.put(JsonTagsZ.TIMESTAMP, LocalDateUtil.format(time, locale, timezone));
      // Custom Id, if any
      // Check custom material ID
      String customMaterialId = m.getCustomId();
      if (customMaterialId != null && !customMaterialId.isEmpty()) {
        material.put(JsonTagsZ.CUSTOM_MATERIALID, customMaterialId);
      }
      // Add batches metadata, if any
      Vector<Hashtable<String, String>>
          batches =
          getBatchData(inv, locale, timezone, ims, isBatchMgmtEnabled, dc.autoGI());
      if (batches != null && !batches.isEmpty()) {
        material.put(JsonTagsZ.BATCHES, batches);
      }
      Vector<Hashtable<String, String>>
          expiredBatches =
          getExpiredBatchData(inv, locale, timezone, ims, isBatchMgmtEnabled, dc.autoGI());
      if (expiredBatches != null && !expiredBatches.isEmpty()) {
        material.put(JsonTagsZ.EXPIRED_NONZERO_BATCHES, expiredBatches);
      }
      // If metadata in addition to stock is required, add those here
      if (!onlyStock) {
        material.put(JsonTagsZ.NAME, m.getName());
        // Add 'additional info.' if required
        String addInfo = m.getInfo();
        if (addInfo != null && !addInfo.isEmpty() && m.displayInfo()) {
          material.put(JsonTagsZ.MESSAGE, addInfo);
        }
        BigDecimal price;
        if (BigUtil.greaterThanZero(price = m.getMSRP())) // manufacturer price
        {
          material.put(JsonTagsZ.MANUFACTURER_PRICE, BigUtil.getFormattedValue(price));
        }
        // Retailer price
        BigDecimal rprice = m.getRetailerPrice();
        if (BigUtil.notEqualsZero(
            inv.getRetailerPrice())) // check if price is specified at inventory level; use it, if it exists
        {
          rprice = inv.getRetailerPrice();
        }
        if (BigUtil.notEqualsZero(rprice)) // price to retailer
        {
          material.put(JsonTagsZ.RETAILER_PRICE, BigUtil.getFormattedValue(rprice));
        }
        BigDecimal tax;
        if (BigUtil.notEqualsZero(tax = inv.getTax())) /// tax
        {
          material.put(JsonTagsZ.TAX, BigUtil.getFormattedValue(tax));
        }
        if (m.getCurrency() != null) {
          material.put(JsonTagsZ.CURRENCY, m.getCurrency());
        }
        // If tags are specified, send that back
        List<String> tags = m.getTags();
        if (tags != null && !tags.isEmpty()) {
          material.put(JsonTagsZ.TAGS, StringUtil.getCSV(tags)); // earlier using TagUtil.getCSVTags
        }
        /// Min/Max, if any
        BigDecimal min = inv.getReorderLevel();
        BigDecimal max = inv.getMaxStock();
        if (BigUtil.notEqualsZero(min)) {
          material.put(JsonTagsZ.MIN, BigUtil.getFormattedValue(min));
        }
        if (BigUtil.notEqualsZero(max)) {
          material.put(JsonTagsZ.MAX, BigUtil.getFormattedValue(max));
        }

        BigDecimal minDur = inv.getMinDuration();
        BigDecimal maxDur = inv.getMaxDuration();
        if (minDur != null && BigUtil.notEqualsZero(minDur)) {
          material.put(JsonTagsZ.MINDUR, BigUtil.getFormattedValue(minDur));
        }
        if (maxDur != null && BigUtil.notEqualsZero(maxDur)) {
          material.put(JsonTagsZ.MAXDUR, BigUtil.getFormattedValue(maxDur));
        }
        BigDecimal stockAvailPeriod = ims.getStockAvailabilityPeriod(inv, dc);
        if (stockAvailPeriod != null && BigUtil.notEqualsZero(stockAvailPeriod)) {
          material.put(JsonTagsZ.STOCK_DURATION, BigUtil.getFormattedValue(stockAvailPeriod));
        }
        // Batch details
        if (m.isBatchEnabled() && kiosk.isBatchMgmtEnabled()) {
          material.put(JsonTagsZ.BATCH_ENABLED, "true");
        }
        // Add optimization parameters, if needed and present
        // Consumption rates
        material = getConsumptionRate(ic, inv, material);
        BigDecimal val;
        // Demand forecast
        if ((hasDemandForecast || hasEOQ) && (BigUtil
            .greaterThanZero(val = inv.getRevPeriodDemand()))) {
          material.put(JsonTagsZ.DEMAND_FORECAST, String.valueOf(BigUtil.getFormattedValue(val)));
          if (BigUtil.greaterThanZero(val = inv.getOrderPeriodicity())) {
            material
                .put(JsonTagsZ.ORDER_PERIODICITY, String.valueOf(BigUtil.getFormattedValue(val)));
          }
        }
        // EOQ
        if (hasEOQ && (BigUtil.greaterThanZero(val = inv.getEconomicOrderQuantity()))) {
          material.put(JsonTagsZ.EOQ, BigUtil.getFormattedValue(val));
        }

        // Is temperature sensitive
        if (m.isTemperatureSensitive()) {
          material.put(JsonTagsZ.IS_TEMPERATURE_SENSITIVE, "true");
        }
      }
      if (inv.getShortId() != null) {
        material.put(JsonTagsZ.SHORT_MATERIAL_ID, String.valueOf(inv.getShortId()));
      }
      Vector<Hashtable<String, String>> handlingUnit = getHandlingUnits(inv.getMaterialId());
      if (handlingUnit != null && !handlingUnit.isEmpty()) {
        material.put(JsonTagsZ.ENFORCE_HANDLING_UNIT_CONSTRAINT, "yes");
        material.put(JsonTagsZ.HANDLING_UNIT, handlingUnit);
      }
      material.put(JsonTagsZ.MANUFACTURERS, getManufacturerList(manufacturers));
      // If start date is specified, then check and add the material to invData only if the it was created or updated on or after the start date.
      Date materialCreatedOn = m.getTimeStamp();
      Date materialLastUpdatedOn = m.getLastUpdated();

      xLogger.fine(
          "material name: {0}, start: {1}, hasStartDate: {2}, materialCreatedOn: {3}, materialLastUpdatedOn: {4}",
          m.getName(), start, hasStartDate, materialCreatedOn, materialLastUpdatedOn);
      if (hasStartDate && ((materialCreatedOn != null && materialCreatedOn.compareTo(start) >= 0)
          || (materialLastUpdatedOn != null && materialLastUpdatedOn.compareTo(start) >= 0))) {
        // Add to vector
        invData.add(material);
      } else if (!hasStartDate) {
        // If start date is not specified, add the material to the vector
        invData.add(material);
      }
    }
    xLogger.fine("Exiting getInventoryData: {0} inventory items, invData: {1}", inventories.size(),
        invData);
    return new Results(invData, cursor, results.getNumFound(), results.getOffset());
  }

  public static Hashtable<String, Object> getConsumptionRate(InventoryConfig ic, IInvntry inv,
                                                             Hashtable<String, Object> material)
      throws ServiceException {
    boolean displayConsumptionRateDaily = Constants.FREQ_DAILY.equals(ic.getDisplayCRFreq());
    boolean displayConsumptionRateWeekly = Constants.FREQ_WEEKLY.equals(ic.getDisplayCRFreq());
    boolean displayConsumptionRateMonthly = Constants.FREQ_MONTHLY.equals(ic.getDisplayCRFreq());
    if (InventoryConfig.CR_MANUAL == ic.getConsumptionRate()) { // If consumption rate is manual
      InventoryManagementService ims =
          StaticApplicationContext.getBean(InventoryManagementServiceImpl.class);
      BigDecimal cr = BigUtil.getZeroIfNull(ims.getDailyConsumptionRate(inv));
      if (displayConsumptionRateDaily && BigUtil.greaterThanZero(cr)) {
        material.put(JsonTagsZ.CR_DAILY, BigUtil.getFormattedValue(cr));
      } else if (displayConsumptionRateWeekly && BigUtil
          .greaterThanZero(cr = cr.multiply(Constants.WEEKLY_COMPUTATION))) {
        material.put(JsonTagsZ.CR_WEEKLY, BigUtil.getFormattedValue(cr));
      } else if (displayConsumptionRateMonthly && BigUtil
          .greaterThanZero(cr = cr.multiply(Constants.MONTHLY_COMPUTATION))) {
        material.put(JsonTagsZ.CR_MONTHLY, BigUtil.getFormattedValue(cr));
      }
    } else if (InventoryConfig.CR_AUTOMATIC == ic
        .getConsumptionRate()) { // If consumption rate is automatic
      BigDecimal val;
      if (displayConsumptionRateDaily && (BigUtil
          .greaterThanZero(val = inv.getConsumptionRateDaily()))) {
        material.put(JsonTagsZ.CR_DAILY, BigUtil.getFormattedValue(val));
      } else if (displayConsumptionRateWeekly && (BigUtil
          .greaterThanZero(val = inv.getConsumptionRateWeekly()))) {
        material.put(JsonTagsZ.CR_WEEKLY, BigUtil.getFormattedValue(val));
      } else if (displayConsumptionRateMonthly && (BigUtil
          .greaterThanZero(val = inv.getConsumptionRateMonthly()))) {
        material.put(JsonTagsZ.CR_MONTHLY, BigUtil.getFormattedValue(val));
      }
    }
    return material;
  }

  private static Vector<Hashtable<String, String>> getHandlingUnits(Long materialId) {
    Vector<Hashtable<String, String>> hu = new Vector<>(1);
    IHandlingUnitService hus = StaticApplicationContext.getBean(HandlingUnitServiceImpl.class);
    Map<String, String> huMap = hus.getHandlingUnitDataByMaterialId(materialId);
    if (huMap != null) {
      Hashtable<String, String> h = new Hashtable<>();
      h.put(JsonTagsZ.HANDLING_UNIT_ID, huMap.get(IHandlingUnit.HUID));
      h.put(JsonTagsZ.HANDLING_UNIT_NAME, huMap.get(IHandlingUnit.NAME));
      h.put(JsonTagsZ.QUANTITY, huMap.get(IHandlingUnit.QUANTITY));
      hu.add(h);
      return hu;
    }
    return null;
  }


  /**
   * Create transactions from order request
   */
  public static List<ITransaction> getInventoryTransactions(UpdateOrderRequest uoReq,
                                                            String transType, Date time) throws ServiceException {
    List<ITransaction> list = new ArrayList<>();
    // Parameter checks
    if (uoReq == null) {
      throw new ServiceException("Invalid JSON input object during inventory update");
    }
    if (transType == null || transType.isEmpty()) {
      throw new ServiceException("Invalid transaction type during inventory update");
    }
    // Get the transactions in the input JSON
    List<MaterialRequest> transObjs = uoReq.mt;
    if (transObjs == null || transObjs.isEmpty()) {
      return null;
    }
    boolean checkBatchMgmt = ITransaction.TYPE_TRANSFER.equals(transType);
    List<String> bErrorMaterials = new ArrayList<>(1);
    MaterialCatalogService mcs = null;
    EntitiesService as;
    IKiosk kiosk = null;
    IKiosk linkedKiosk = null;
    try {
      if (checkBatchMgmt) {
        as = StaticApplicationContext.getBean(EntitiesServiceImpl.class);
        kiosk = as.getKiosk(uoReq.kid);
        linkedKiosk = as.getKiosk(uoReq.lkid);
        checkBatchMgmt =
            !kiosk.isBatchMgmtEnabled() && linkedKiosk != null && linkedKiosk.isBatchMgmtEnabled();
        mcs = StaticApplicationContext.getBean(MaterialCatalogServiceImpl.class);
      }

    } catch (ServiceException se) {
      xLogger.warn("ServiceException while getting kiosk details. Exception: {0}", se);
    }
    // Form the inventory transaction objects
    for (MaterialRequest mReq : transObjs) {
      if (checkBatchMgmt) {
        IMaterial material = mcs.getMaterial(mReq.mid);
        if (material.isBatchEnabled()) {
          mReq.isBa = true;
          bErrorMaterials.add(material.getName());
        }
      }
      ITransaction trans = JDOUtils.createInstance(ITransaction.class);
      trans.setKioskId(uoReq.kid);
      trans.setMaterialId(mReq.mid);
      trans.setType(transType);
      trans.setQuantity(mReq.q);
      trans.setSourceUserId(uoReq.uid);
      trans.setDestinationUserId(uoReq.duid);
      trans.setMessage(mReq.ms);
      trans.setLinkedKioskId(uoReq.lkid);
      if (uoReq.lat != null) {
        trans.setLatitude(uoReq.lat);
      }
      if (uoReq.lng != null) {
        trans.setLongitude(uoReq.lng);
      }
      if (uoReq.galt != null) {
        trans.setAltitude(uoReq.galt);
      }
      if (uoReq.gacc != null) {
        trans.setGeoAccuracy(uoReq.gacc);
      }
      trans.setGeoErrorCode(uoReq.gerr);
      trans.setReason(mReq.rsn);
      trans.setTrackingId(String.valueOf(uoReq.tid));
      trans.setTrackingObjectType(transType);

      Date t = time;
      if (t == null) {
        t = new Date();
      }
      trans.setTimestamp(t);
      trans.setEditOrderQtyRsn(mReq.rsneoq);

      // Set the material status
      trans.setMaterialStatus(mReq.mst);
      // Add to transaction list
      list.add(trans);

    }
    if (!bErrorMaterials.isEmpty()) {
      xLogger.severe(
          "Transfer failed because source entity {0} is batch disabled and destination entity {1} "
              +
              "is batch enabled. Affected materials: {2}", kiosk != null ? kiosk.getName() : null,
          linkedKiosk != null ? linkedKiosk.getName() : null, bErrorMaterials.toString());
      throw new InvalidDataException(bErrorMaterials.toString());
    }
    return list;
  }

  @SuppressWarnings({"rawtypes", "unchecked"})
  public static List<ITransaction> getInventoryTransactions(UpdateInventoryInput updInventoryJson,
                                                            String transType, Date time) throws ServiceException {
    List<ITransaction> list = new ArrayList<>();

    // Parameter checks
    if (updInventoryJson == null) {
      throw new ServiceException("Invalid JSON input object during inventory update");
    }
    if (transType == null || transType.isEmpty()) {
      throw new ServiceException("Invalid transaction type during inventory update");
    }
    // Get the transactions in the input JSON
    Vector transObjs = updInventoryJson.getMaterials();
    if (transObjs == null || transObjs.isEmpty()) {
      return null;
    }
    // Get the user and kiosk IDs
    String userId = updInventoryJson.getUserId();
    String kioskIdStr = updInventoryJson.getKioskId();
    String destUserId = updInventoryJson.getDestUserId();
    String linkedKioskIdStr = updInventoryJson.getLinkedKioskId();
    Long linkedKioskId = null;
    if (linkedKioskIdStr != null && !linkedKioskIdStr.isEmpty()) {
      linkedKioskId = Long.valueOf(linkedKioskIdStr);
    }

    boolean checkBatchMgmt = ITransaction.TYPE_TRANSFER.equals(transType);
    List<String> bErrorMaterials = new ArrayList<>(1);
    MaterialCatalogService mcs = null;
    EntitiesService as;
    IKiosk kiosk = null;
    IKiosk linkedKiosk = null;
    try {
      if (checkBatchMgmt) {
        as = StaticApplicationContext.getBean(EntitiesServiceImpl.class);
        kiosk = as.getKiosk(Long.valueOf(kioskIdStr));
        linkedKiosk = as.getKiosk(linkedKioskId);
        checkBatchMgmt =
            !kiosk.isBatchMgmtEnabled() && linkedKiosk != null && linkedKiosk.isBatchMgmtEnabled();
        mcs = StaticApplicationContext.getBean(MaterialCatalogServiceImpl.class);
      }

    } catch (ServiceException se) {
      xLogger.warn("ServiceException while getting kiosk details. Exception: {0}", se);
    }
    // Get the latitude and longitude, if present
    String latitudeStr = updInventoryJson.getLatitude();
    String longitudeStr = updInventoryJson.getLongitude();
    String geoAccuracyStr = updInventoryJson.getGeoAccuracy();
    String geoErrorCode = updInventoryJson.getGeoErrorCode();
    String altitudeStr = updInventoryJson.getAltitude();

    Double latitude = null, longitude = null, geoAccuracy = null, altitude = null;
    if (latitudeStr != null && !latitudeStr.isEmpty()) {
      try {
        latitude = Double.valueOf(latitudeStr);
      } catch (NumberFormatException e) {
        xLogger.warn("NumberFormatException when getting latitude {0}: {1}", latitudeStr,
            e.getMessage());
      }
    }
    if (longitudeStr != null && !longitudeStr.isEmpty()) {
      try {
        longitude = Double.valueOf(longitudeStr);
      } catch (NumberFormatException e) {
        xLogger.warn("NumberFormatException when getting longitude {0}: {1}", longitudeStr,
            e.getMessage());
      }
    }
    if (altitudeStr != null && !altitudeStr.isEmpty()) {
      try {
        altitude = Double.valueOf(altitudeStr);
      } catch (NumberFormatException e) {
        xLogger.warn("NumberFormatException when getting altitude {0}: {1}", altitudeStr,
            e.getMessage());
      }
    }
    if (geoAccuracyStr != null && !geoAccuracyStr.isEmpty()) {
      try {
        geoAccuracy = Double.valueOf(geoAccuracyStr);
      } catch (NumberFormatException e) {
        xLogger.warn("NumberFormatException when getting geo-accuracy {0}: {1}", geoAccuracyStr,
            e.getMessage());
      }
    }
    // Tracking Id
    String trackingIdStr = updInventoryJson.getTrackingId();
    Long trackingId = null;
    if (trackingIdStr != null && !trackingIdStr.isEmpty()) {
      try {
        trackingId = Long.valueOf(trackingIdStr);
      } catch (Exception e) {
        xLogger.warn("{0} when getting trackingId {1}: {2}", e.getClass().getName(), trackingIdStr,
            e.getMessage());
      }
    }

    // Transaction saved timestamp (in milliseconds), if any
    String transSavedTimestampStr = updInventoryJson.getTimestampSaveMillis();
    if (transSavedTimestampStr != null && !transSavedTimestampStr.isEmpty()) {
      try {
        Long.valueOf(transSavedTimestampStr);
      } catch (NumberFormatException e) {
        xLogger.warn("NumberFormatException when getting transaction saved time {0}: {1}",
            transSavedTimestampStr, e.getMessage());
      }
    }

    Enumeration<Hashtable> en = transObjs.elements();
    // Form the inventory transaction objects
    while (en.hasMoreElements()) {
      Hashtable<String, String> map = (Hashtable<String, String>) en.nextElement();

      // Get the material details
      String materialIdStr = map.get(JsonTagsZ.MATERIAL_ID);
      if (checkBatchMgmt) {
        IMaterial material = mcs.getMaterial(Long.valueOf(materialIdStr));
        if (material.isBatchEnabled()) {
          bErrorMaterials.add(material.getName());
        }
      }
      String quantityStr = map.get(JsonTagsZ.QUANTITY);
      String reason = map.get(JsonTagsZ.REASON);
      // FOR BACKWARD COMPATIBILITY (28/4/2012)
      if (reason == null || reason.isEmpty()) {
        reason = map.get(JsonTagsZ.REASONS_WASTAGE); // reason for wastage
      }
      // END BACKWARD COMPATIBITY
      String microMessage = map.get(JsonTagsZ.MESSAGE);
      // Batch parameters, if any
      String batchIdStr = map.get(JsonTagsZ.BATCH_ID);
      String batchExpiryStr = map.get(JsonTagsZ.BATCH_EXPIRY);
      String batchManufacturer = map.get(JsonTagsZ.BATCH_MANUFACTUER_NAME);
      String batchManufacturedDateStr = map.get(JsonTagsZ.BATCH_MANUFACTURED_DATE);

      // Material status, if any
      String matStatusStr = map.get(JsonTagsZ.MATERIAL_STATUS);
      // Actual date of transaction
      String actualDateOfTransStr = map.get(JsonTagsZ.ACTUAL_TRANSACTION_DATE);
      Date actualDateOfTrans = null;
      if (actualDateOfTransStr != null) {
        try {
          actualDateOfTrans =
              LocalDateUtil.parseCustom(actualDateOfTransStr, Constants.DATE_FORMAT, null);
        } catch (Exception e) {
          xLogger.warn("Error while setting actual date of transaction", e);
        }
      }
      String eoqrsn = map.get(JsonTagsZ.REASON_FOR_EDITING_ORDER_QUANTITY);
      try {
        ITransaction trans = JDOUtils.createInstance(ITransaction.class);
        Long kioskId = Long.valueOf(kioskIdStr);
        Long materialId = Long.valueOf(materialIdStr);
        trans.setKioskId(kioskId);
        trans.setMaterialId(materialId);
        trans.setType(transType);
        trans.setQuantity(new BigDecimal(quantityStr));
        if (matStatusStr != null && !matStatusStr
            .isEmpty()) // Material status is optional. Set it only if it is present.
        {
          trans.setMaterialStatus(matStatusStr);
        }
        trans.setSourceUserId(userId);
        if (destUserId != null) {
          trans.setDestinationUserId(destUserId);
        }
        if (microMessage != null) {
          trans.setMessage(microMessage);
        }
        if (time != null) {
          trans.setTimestamp(time);
        }
        if (linkedKioskId != null) {
          trans.setLinkedKioskId(linkedKioskId);
        }
        if (latitude != null) {
          trans.setLatitude(latitude);
        }
        if (longitude != null) {
          trans.setLongitude(longitude);
        }
        if (altitude != null) // Altitude is optional. Set it only if it is present
        {
          trans.setAltitude(altitude);
        }
        if (geoAccuracy != null) {
          trans.setGeoAccuracy(geoAccuracy);
        }
        if (geoErrorCode != null) {
          trans.setGeoErrorCode(geoErrorCode);
        }
        if (reason != null && !reason.isEmpty()) {
          trans.setReason(reason);
        }
        if (trackingId != null) {
          trans.setTrackingId(String.valueOf(trackingId));
          trans.setTrackingObjectType(transType);
        }
        if (actualDateOfTrans != null) {
          trans.setAtd(actualDateOfTrans);
        }

        if (batchIdStr != null) // add batch parameters, if needed
        {
          TransactionUtil.setBatchData(trans, batchIdStr, batchExpiryStr, batchManufacturer,
              batchManufacturedDateStr);
        }

        Date t = time;
        if (t == null) {
          t = new Date();
        }
        trans.setTimestamp(t);
        if (eoqrsn != null && !eoqrsn.isEmpty()) {
          trans.setEditOrderQtyRsn(eoqrsn);
        }
        ITransDao transDao = StaticApplicationContext.getBean(ITransDao.class);
        transDao.setKey(trans);
        // Add to transaction list
        list.add(trans);
      } catch (NumberFormatException e) {
        xLogger.warn("Invalid number sent during inventory update: kiosk-material = {0}-{1}: {2}",
            kioskIdStr, materialIdStr, e.getMessage());
      }
    }
    if (!bErrorMaterials.isEmpty()) {
      xLogger.severe(
          "Transfer failed because source entity {0} is batch disabled and destination entity {1} is batch enabled. Affected materials: {2}",
          kiosk.getName(), linkedKiosk.getName(), bErrorMaterials.toString());
      throw new InvalidDataException(bErrorMaterials.toString());
    }
    return list;
  }

  // Authenticate a user and get the user account data
  // If Basic Auth. header is present, authenticate using that, else authenticate using userId and password in query string (to ensure backward compatibility since 1.3.0 of HTML5 app. - Aug 2015)
  // NOTE: The returned user will NOT have associated kiosks; you will need to get them separately (say, via a as.getUserAccount( userId ) call)
  // NOTE: kiosk-based authorization check is made ONLY if kioskId is supplied; kioskId null check should be done by caller, if it is needed
  public static IUserAccount authenticate(String userId, String password, Long kioskId,
                                          HttpServletRequest req, HttpServletResponse resp)
      throws ServiceException, UnauthorizedException {
    // Get service
    UsersService as = StaticApplicationContext.getBean(UsersServiceImpl.class);
    EntitiesService entitiesService = StaticApplicationContext.getBean(EntitiesServiceImpl.class);
    AuthenticationService aus = StaticApplicationContext.getBean(AuthenticationServiceImpl.class);
    String errMsg = null;
    IUserAccount u = null;
    UserDetails ud = null;
    ResourceBundle backendMessages;
    String uId = userId;
    String pwd = password;
    String token = req.getHeader(Constants.TOKEN);
    String sourceInitiatorStr = req.getHeader(Constants.ACCESS_INITIATOR);
    int actionInitiator = -1;
    if (sourceInitiatorStr != null) {
      try {
        actionInitiator = Integer.parseInt(sourceInitiatorStr);
      } catch (NumberFormatException ignored) {

      }
    }
    // Get default backendMessages
    backendMessages =
        Resources.getBundle(new Locale(Constants.LANG_DEFAULT));
    try {
      boolean authenticated = false;
      // Check if Basic auth. header exists
      SecurityMgr.Credentials creds = SecurityMgr.getUserCredentials(req);
      if (StringUtils.isNotEmpty(token)) {
        u = AuthenticationUtil.authenticateToken(token, actionInitiator);
        authenticated = true;
      } else {
        if (creds != null) {
          uId = creds.userId;
          pwd = creds.password;
        }
        // Authenticate using password (either from auth. header or parameters (for backward compatibility)
        if (uId != null && pwd != null) {
          AuthRequest authRequest = AuthRequest.builder()
              .userId(uId)
              .password(pwd)
              .loginSource(SourceConstants.MOBILE).build();
          ud = aus.authenticate(authRequest);
          authenticated = (ud != null);
          if (!authenticated) {
            backendMessages =
                Resources.getBundle(new Locale(Constants.LANG_DEFAULT));
            errMsg = backendMessages.getString("error.invalidusername");
          } else {
            u = as.getUserAccount(uId);
          }
        } else if (uId == null) {
          // no proper credentials
          backendMessages = Resources.getBundle(new Locale(Constants.LANG_DEFAULT));
          errMsg = backendMessages.getString("error.invalidusername");
        } else {
          // Brought this back, without this few pages in old UI break. ( Stock view dashboard)
          // DEPRECATED
          // No password - check whether an authenticated session exists
          SecureUserDetails sUser = SecurityMgr.getUserDetailsIfPresent();
          if (sUser != null && sUser.getUsername().equals(uId)) {
            // authenticated via web login session
            u = as.getUserAccount(uId);
            authenticated = true;
          } else { // no authenticated session either
            backendMessages =
                Resources.getBundle(new Locale(Constants.LANG_DEFAULT));
            errMsg = backendMessages.getString("error.invalidusername");
          }
        }
      }

      // If authenticated, check permissions for this kiosk, if available
      if (authenticated) { // authenticated, so proceed...
        // Check if switch to new host is required. If yes, then return an error message. Otherwise, proceed.
        if (checkIfLoginShouldNotBeAllowed(u, resp, req)) {
          xLogger.warn("Switched user {0} to new host", uId);
        } else if (kioskId != null) {
          // If not, proceed.
          String role = u.getRole();
          if (SecurityUtil.compareRoles(role, SecurityConstants.ROLE_DOMAINOWNER)
              < 0) { // user has a role less than domain owner (say, entity operator or manager)
            if (!EntityAuthoriser.authoriseEntity(kioskId, u.getRole(), u.getUserId(),
                u.getDomainId())) {
              try {
                errMsg =
                    "You do not have authorization for " + entitiesService.getKiosk(kioskId, false).getName();
              } catch (Exception e) {
                xLogger.warn(
                    "Exception {0} when getting find kiosk {1} when authenticating user {2}: {3}",
                    e.getClass().getName(), kioskId, uId, e.getMessage());
                errMsg = "This entity could not be found.";
              }
            }
          } else if (SecurityConstants.ROLE_DOMAINOWNER.equals(role)) { // user is domain owner
            // Ensure that the kiosk belongs to this domain
            try {
              IKiosk k = entitiesService.getKiosk(kioskId, false);
              if (!k.getDomainIds().contains(u.getDomainId())) {
                errMsg = "You do not have authorization for " + k.getName();
              }
            } catch (Exception e) {
              xLogger.warn(
                  "Exception {0} when getting find kiosk {1} when authenticating user {2}: {3}",
                  e.getClass().getName(), kioskId, uId, e.getMessage());
              errMsg = "This entity could not be found.";
            }
          } else {
            try {
              entitiesService.getKiosk(kioskId, false);
            } catch (Exception e) {
              xLogger.warn(
                  "Exception {0} when getting find kiosk {1} when authenticating user {2}: {3}",
                  e.getClass().getName(), kioskId, uId, e.getMessage());
              errMsg = "This entity could not be found";
            }
          }
        }
      }
    } catch (InvalidDataException | JDOObjectNotFoundException e) {
      throw new UnauthorizedException("Invalid token");
    } catch (ObjectNotFoundException e) {
      errMsg = backendMessages != null ? backendMessages.getString("error.invalidusername") : null;
    } catch (UnauthorizedException e) {
      throw new UnauthorizedException("G006",new Object[]{});
    }
    // Check for error
    if (errMsg != null) {
      throw new ServiceException(errMsg);
    }
    SecurityMgr.setSessionDetails(u);
    return u;
  }

  public static Long getDomainId() {
    return SecurityUtils.getCurrentDomainId();
  }

  public static String getJsonOutputAuthenticate(boolean status, IUserAccount user, String message,
                                                 DomainConfig dc,
                                                 String minResponseCode,
                                                 boolean onlyAuthenticate,
                                                 boolean forceIntegerForStock, Date start,
                                                 Optional<Date> modifiedSinceDate,
                                                 PageParams kioskPageParams, boolean skipInventory)
      throws ProtocolException, ServiceException {
    Hashtable<String, Object> config = null;
    String expiryTime = null;
    boolean hasStartDate = (start != null);
    UserEntitiesModel fullUser = new UserEntitiesModel(user, null);
    if (status && !onlyAuthenticate) {
      List kioskList = new ArrayList();
      // Get the kiosks
      EntitiesService as = StaticApplicationContext.getBean(EntitiesServiceImpl.class);
      Results results = as.getKiosksForUser(user, null, kioskPageParams);
      List<IKiosk> kiosks = results.getResults();
      boolean isSingleKiosk = (kiosks != null && kiosks.size() == 1);
      // Get the kiosk maps
      Vector<Hashtable> kiosksData = new Vector();
      if (kiosks != null && !kiosks.isEmpty()) {
        boolean getUsersForKiosk = (MINIMUM_RESPONSE_CODE_TWO.equals(minResponseCode));
        boolean
            getMaterials =
            (isSingleKiosk || StringUtils.isEmpty(minResponseCode)) && !skipInventory;
        boolean
            getLinkedKiosks =
            (!getUsersForKiosk);
        for (IKiosk k : kiosks) {
          // set users if minResponseCode is 2 (local editing of kiosks/users is now possible)
          if (getUsersForKiosk) {
            k.setUsers(as.getUsersForKiosk(k.getKioskId(), null).getResults());
          }
          // Get user locale
          Locale locale = user.getLocale();
          // Get kiosk data
          Hashtable<String, Object>
              kioskData =
              getKioskData(k, locale, user.getTimezone(), dc, getUsersForKiosk, getMaterials,
                  getLinkedKiosks, forceIntegerForStock, user.getUserId(), user.getRole(),
                  modifiedSinceDate);
          Date kioskCreatedOn = k.getTimeStamp();
          Date kioskLastUpdatedOn = k.getLastUpdated();
          xLogger.fine(
              "kiosk name: {0}, start: {1}, hasStartDate: {2}, kioskCreatedOn: {3}, kioskLastUpdatedOn: {4}",
              k.getName(), start, hasStartDate, k.getTimeStamp(), k.getLastUpdated());
          if (hasStartDate && ((kioskCreatedOn != null && kioskCreatedOn.compareTo(start) >= 0) || (
              kioskLastUpdatedOn != null && kioskLastUpdatedOn.compareTo(start) >= 0))) {
            // Add kiosk data
            kiosksData.add(kioskData);
          } else if (!hasStartDate) {
            // Add kiosk data
            kiosksData.add(kioskData);
            kioskList.add(kioskData);
          }
        }
        // Get the expiry time
        expiryTime = String.valueOf(getLoginExpiry());
      }
      // Get config. for transaction inclusion and naming
      config = getConfig(dc, user, modifiedSinceDate);
      fullUser = new UserEntitiesModel(user, kioskList);
    }
    return GsonUtil.authenticateOutputToJson(status, message, expiryTime, fullUser, config,
        RESTUtil.VERSION_01);
  }

  public static List<Map<String, Object>> getManufacturerList(
      List<IMaterialManufacturers> manufacturers) {
    List<Map<String, Object>> manufacturerList = new ArrayList<>();
    if (manufacturers != null && !manufacturers.isEmpty()) {
      for (IMaterialManufacturers manufacturer : manufacturers) {
        Map<String, Object> model = new HashMap<>();
        model.put(JsonTagsZ.MANUFACTURER_CODE, manufacturer.getManufacturerCode());
        model.put(JsonTagsZ.MANUFACTURER_NAME, manufacturer.getManufacturerName());
        model.put(JsonTagsZ.MATERIAL_CODE, manufacturer.getMaterialCode());
        model.put(JsonTagsZ.QUANTITY, manufacturer.getQuantity());
        manufacturerList.add(model);
      }
    }
    return manufacturerList;
  }

  // Get data for kiosks for JSON output
  @SuppressWarnings({"unchecked", "rawtypes"})
  private static Hashtable<String, Object> getKioskData(IKiosk k, Locale locale, String timezone,
                                                        DomainConfig dc, boolean getUsersForKiosk,
                                                        boolean getMaterials,
                                                        boolean getLinkedKiosks,
                                                        boolean forceIntegerForStock, String userId,
                                                        String role, Optional<Date> modifiedSince) {
    xLogger.fine("Entered getKioskData");
    Hashtable
        kioskData =
        k.getMapZ(true,
            null); // NOTE: here all operators can be managed by the given user; so not restricting them to those registered by this user
    // Add the materials and other data
    try {
      // Add currency, if present
      String currency = k.getCurrency();
      if (currency == null) {
        currency = Constants.CURRENCY_DEFAULT;
      }
      kioskData.put(JsonTagsZ.CURRENCY, currency);
      if (k.getCustomerPerm() > 0 || k.getVendorPerm() > 0) {
        Hashtable<String, Hashtable<String, String>> perm = new Hashtable<>();
        if (k.getCustomerPerm() > 0) {
          Hashtable<String, String> customerPermission = new Hashtable<>();
          if (k.getCustomerPerm() > 1) {
            customerPermission.put(JsonTagsZ.MANAGE_MASTER_DATA, "yes");
          } else {
            customerPermission.put(JsonTagsZ.VIEW_INVENTORY_ONLY, "yes");
          }
          perm.put(JsonTagsZ.CUSTOMERS, customerPermission);
        }
        if (k.getVendorPerm() > 0) {
          Hashtable<String, String> vendorPermission = new Hashtable<>();
          if (k.getVendorPerm() > 1) {
            vendorPermission.put(JsonTagsZ.MANAGE_MASTER_DATA, "yes");
          } else {
            vendorPermission.put(JsonTagsZ.VIEW_INVENTORY_ONLY, "yes");
          }
          perm.put(JsonTagsZ.VENDORS, vendorPermission);
        }
        kioskData.put(JsonTagsZ.PERMISSIONS, perm);
      }
      // Add materials
      if (getMaterials) {
        Results results = RESTUtil
            .getInventoryData(k.getKioskId(), locale, timezone, false,
                dc, forceIntegerForStock, null, modifiedSince, null);
        kioskData.put(JsonTagsZ.MATERIALS, results.getResults());
        kioskData.put(JsonTagsZ.NUMBER_OF_INVENTORY, results.getNumFound());
      }
      if (getLinkedKiosks) {
        CapabilityConfig cconf = dc.getCapabilityByRole(role);
        boolean sendVendors = dc.sendVendors();
        boolean sendCustomers = dc.sendCustomers();
        if (cconf != null) {
          sendVendors = cconf.sendVendors();
          sendCustomers = cconf.sendCustomers();
        }
        if (sendVendors) {
          // Add vendor info., if present
          Results
              results =
              getLinkedKiosks(k.getKioskId(), IKioskLink.TYPE_VENDOR, userId, getUsersForKiosk, dc,
                  null, modifiedSince);
          if (results.getResults() != null && !results.getResults().isEmpty()) {
            kioskData
                .put(JsonTagsZ.VENDORS, results.getResults());
          }
          kioskData.put(JsonTagsZ.NUMBER_OF_VENDORS, results.getNumFound());
        }

        if (sendCustomers) {
          // Add customer info., if present
          Results results =
              getLinkedKiosks(k.getKioskId(),
                  IKioskLink.TYPE_CUSTOMER, userId, getUsersForKiosk, dc, null, modifiedSince);
          if (dc.sendCustomers() && results.getResults() != null && !results.getResults()
              .isEmpty()) {
            kioskData.put(JsonTagsZ.CUSTOMERS, results.getResults());
          }
          kioskData.put(JsonTagsZ.NUMBER_OF_CUSTOMERS, results.getNumFound());
        }
      }
      if (dc.isPurchaseApprovalEnabled() || dc.isSalesApprovalEnabled()) {
        EntitiesService es = StaticApplicationContext.getBean(EntitiesServiceImpl.class);
        MobileEntityBuilder mobileEntityBuilder = new MobileEntityBuilder();

        List<IApprover> approversList = es.getApprovers(k.getKioskId());
        if (approversList != null && !approversList.isEmpty()) {
          kioskData.put(JsonTagsZ.APPROVERS,
              mobileEntityBuilder.buildApproversModel(approversList, dc.isPurchaseApprovalEnabled(),
                  dc.isSalesApprovalEnabled()));
        }
      }
      // Add kiosk tags if any configured for the kiosk
      // If tags are specified, send that back as an array
      List<String> tags = k.getTags();
      if (tags != null && !tags.isEmpty()) {
        kioskData.put(JsonTagsZ.ENTITY_TAG, tags);
      }
    } catch (ServiceException e) {
      xLogger
          .warn("Unable to get material data for JSON output of REST authentication for kiosk: {0}",
              k.getKioskId());
    }
    xLogger.fine("Exiting getKioskData");
    return kioskData;
  }

  // Get the vendor hashtable of a given kiosk
  @SuppressWarnings("unchecked")
  public static Results getLinkedKiosks(Long kioskId, String linkType, String userId,
                                        boolean getUsersForKiosk, DomainConfig dc,
                                        PageParams pageParams, Optional<Date> modifiedSinceDate) {
    Vector<Hashtable> linkedKiosks = new Vector<>();
    Results results = null;
    String cursor = null;
    try {
      EntitiesService as = StaticApplicationContext.getBean(EntitiesServiceImpl.class);
      KioskLinkFilters filters = new KioskLinkFilters()
          .withKioskId(kioskId)
          .withLinkType(linkType)
          .withModifiedSince(modifiedSinceDate.orElse(null));
      results = as.getKioskLinks(filters, pageParams, false);
      List<IKioskLink> vlinks = results.getResults();
      cursor = results.getCursor();
      if (vlinks == null || vlinks.isEmpty()) {
        return new Results(linkedKiosks, cursor, results.getNumFound(), results.getOffset());
      }
      PersistenceManager pm = PMF.get().getPersistenceManager();
      // Check if accounting is enabled - to send credit limit and payable information
      boolean isAccounting = dc.isAccountingEnabled();
      BigDecimal
          defaultCreditLimit =
          (isAccounting && dc.getAccountingConfig() != null ? dc.getAccountingConfig()
              .getCreditLimit() : BigDecimal.ZERO);
      try {
        for (IKioskLink link : vlinks) {
          try {
            Long linkedKioskId = link.getLinkedKioskId();
            IKiosk k = JDOUtils.getObjectById(IKiosk.class, linkedKioskId);
            if (getUsersForKiosk) {
              k.setUsers(as.getUsersForKiosk(linkedKioskId, null, pm)
                  .getResults()); // get the users associated with kiosk
            }
            // Set the route index / tag, if present
            k.setRouteIndex(link.getRouteIndex());
            k.setRouteTag(link.getRouteTag());
            Hashtable
                linkedKiosk =
                k.getMapZ(true,
                    userId); // allow only users created by userId to be editable; others are not editable by this user
            // If accounting is enabled, add credit limit and payable (NOTE: payable is always to the vendor from the customer)
            if (isAccounting) {
              BigDecimal
                  creditLimit =
                  (BigUtil.notEqualsZero(link.getCreditLimit()) ? link.getCreditLimit()
                      : defaultCreditLimit);
              if (BigUtil.notEqualsZero(creditLimit)) {
                linkedKiosk.put(JsonTagsZ.CREDIT_LIMIT, BigUtil.getFormattedValue(creditLimit));
                Long vId, cId;
                if (IKioskLink.TYPE_VENDOR.equals(linkType)) {
                  cId = link.getKioskId();
                  vId = link.getLinkedKioskId();
                } else {
                  vId = link.getKioskId();
                  cId = link.getLinkedKioskId();
                }
                // Get the account payable/receiveable as the case may be
                int year = GregorianCalendar.getInstance().get(Calendar.YEAR);
                try {
                  IAccountingService oms =
                      StaticApplicationContext.getBean(AccountingServiceImpl.class);
                  IAccount account = oms.getAccount(vId, cId, year);
                  linkedKiosk
                      .put(JsonTagsZ.PAYABLE, BigUtil.getFormattedValue(account.getPayable()));
                } catch (ObjectNotFoundException e) {
                  // ignore
                } catch (Exception e) {
                  xLogger.warn(
                      "{0} when getting account for kiosk link vendor {1}, customer {2}, year {3}: {4}",
                      e.getClass().getName(), vId, cId, year, e.getMessage());
                }
              }
            }
            // Add the disabled batch management flag, if applicable
            if (!k.isBatchMgmtEnabled()) {
              linkedKiosk.put(JsonTagsZ.DISABLE_BATCH_MGMT, "true");
            }

            List<String> tags = k.getTags();
            if (CollectionUtils.isNotEmpty(tags)) {
              linkedKiosk.put(JsonTagsZ.ENTITY_TAG,tags);
            }
            // Add to vector
            linkedKiosks.add(linkedKiosk);
          } catch (Exception e) {
            xLogger.warn("{0} when getting linked kiosk {1} of kiosk {2} and link type {3}: {4}",
                e.getClass().getName(), link.getLinkedKioskId(), kioskId, linkType, e.getMessage());
          }
        } // end while
      } finally {
        // Close pm
        pm.close();
      }
    } catch (ServiceException e) {
      xLogger
          .warn("ServiceException when getting KioskLinks for {0}: {1}", kioskId, e.getMessage());
    }
    xLogger.fine("getLinkedKiosks: linkedKiosks: {0}", linkedKiosks);
    return new Results(linkedKiosks, cursor, results.getNumFound(), results.getOffset());
  }

  // Schedule export of inventory, transactions or orders via REST for a given kiosk
  // JSON response string is returned
  public static String scheduleKioskDataExport(HttpServletRequest req,
                                               ResourceBundle backendMessages,
                                               HttpServletResponse resp) throws ProtocolException {
    xLogger.fine("Entered scheduleKioskDataExport");
    Locale locale = null;
    String message;
    boolean isValid;
    Long domainId;
    Map<String, String> reqParamsMap = new HashMap<>(1);
    String userId = req.getParameter(RestConstantsZ.USER_ID);
    String password = req.getParameter(RestConstantsZ.PASSWRD);
    reqParamsMap.put(RestConstantsZ.TYPE, req.getParameter(RestConstantsZ.TYPE));
    reqParamsMap.put(RestConstantsZ.KIOSK_ID, req.getParameter(RestConstantsZ.KIOSK_ID));
    reqParamsMap.put(RestConstantsZ.USER_ID, userId);
    reqParamsMap.put(RestConstantsZ.PASSWRD, password);
    reqParamsMap.put(RestConstantsZ.EMAIL, req.getParameter(RestConstantsZ.EMAIL));
    reqParamsMap.put(RestConstantsZ.ORDER_TYPE, req.getParameter(RestConstantsZ.ORDER_TYPE));
    ParsedRequest parsedRequest = parseGeneralExportFilters(reqParamsMap, backendMessages);
    if (StringUtils.isNotEmpty(parsedRequest.errMessage)) {
      isValid = false;
      message = parsedRequest.errMessage;
      return getKioskDataExportJsonOutput(locale, isValid, message);
    }
    try {
      // Authenticate user
      IUserAccount
          u =
          RESTUtil.authenticate(userId, password, (Long) parsedRequest.parsedReqMap.get(
                  RestConstantsZ.KIOSK_ID), req,
              resp); // NOTE: throws ServiceException in case of invalid credentials or no authentication
      if (userId == null) { // can be the case if BasicAuth was used
        userId = u.getUserId();
      }
      domainId = u.getDomainId();
      locale = u.getLocale();
    } catch (UnauthorizedException e) {
      message = e.getMessage();
      resp.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
      return getKioskDataExportJsonOutput(locale, false, message);
    } catch (Exception e) {
      message = e.getMessage();
      return getKioskDataExportJsonOutput(locale, false, message);
    }
    // Order filters
    ParsedRequest parsedOrderExportFilters = parseOrdersExportFilters(reqParamsMap);
    if (StringUtils.isNotEmpty(parsedOrderExportFilters.errMessage)) {
      message = parsedOrderExportFilters.errMessage;
      return getKioskDataExportJsonOutput(locale, false, message);
    }
    parsedRequest.parsedReqMap.putAll(parsedOrderExportFilters.parsedReqMap);

    // Date filters
    reqParamsMap.put(RestConstantsZ.STARTDATE, req.getParameter(RestConstantsZ.STARTDATE));
    reqParamsMap.put(RestConstantsZ.ENDDATE, req.getParameter(RestConstantsZ.ENDDATE));
    ParsedRequest parsedDateFilters = parseDateFilters(reqParamsMap, backendMessages);
    if (StringUtils.isNotEmpty(parsedDateFilters.errMessage)) {
      message = parsedDateFilters.errMessage;
      return getKioskDataExportJsonOutput(locale, false, message);
    }
    parsedRequest.parsedReqMap.putAll(parsedDateFilters.parsedReqMap);

    // Transaction filters
    reqParamsMap
        .put(RestConstantsZ.TRANSACTION_TYPE, req.getParameter(RestConstantsZ.TRANSACTION_TYPE));
    reqParamsMap.put(RestConstantsZ.MATERIAL_ID, req.getParameter(RestConstantsZ.MATERIAL_ID));

    ParsedRequest parsedTransExportFilters =
        parseTransactionsExportFilters(reqParamsMap, backendMessages);
    if (StringUtils.isNotEmpty(parsedTransExportFilters.errMessage)) {
      message = parsedTransExportFilters.errMessage;
      return getKioskDataExportJsonOutput(locale, false, message);
    }
    parsedRequest.parsedReqMap.putAll(parsedTransExportFilters.parsedReqMap);

    Map<String, String> params = new HashMap<>();
    params.put(MobileExportConstants.EXPORT_TYPE_KEY,
        (String) parsedRequest.parsedReqMap.get(RestConstantsZ.TYPE));
    params.put(MobileExportConstants.DOMAIN_ID_KEY, domainId.toString());
    params.put(MobileExportConstants.KIOSK_ID_KEY,
        parsedRequest.parsedReqMap.get(RestConstantsZ.KIOSK_ID).toString());
    String exportType = (String) parsedRequest.parsedReqMap.get(RestConstantsZ.TYPE);
    if (isExportTypeTransactions(exportType) || isExportTypeOrders(exportType) || isExportTypeTransfers(exportType)) {
      if (parsedRequest.parsedReqMap.get(RestConstantsZ.ENDDATE) != null) {
        params.put(MobileExportConstants.END_DATE_KEY, LocalDateUtil
            .formatCustom((Date) parsedRequest.parsedReqMap.get(RestConstantsZ.ENDDATE),
                Constants.DATE_FORMAT_CSV, null));
      }
      if (parsedRequest.parsedReqMap.get(RestConstantsZ.STARTDATE) != null) {
        params.put(MobileExportConstants.FROM_DATE_KEY, LocalDateUtil
            .formatCustom((Date) parsedRequest.parsedReqMap.get(RestConstantsZ.STARTDATE),
                Constants.DATE_FORMAT_CSV, null));
      } else {
        // Export a year's worth of transactions/orders
        Calendar cal = GregorianCalendar.getInstance();
        cal.add(Calendar.YEAR, -1);
        params.put(MobileExportConstants.FROM_DATE_KEY,
            LocalDateUtil.formatCustom(cal.getTime(), Constants.DATE_FORMAT_CSV, null));
      }
    }
    if (isExportTypeTransactions(exportType)) {
      if (parsedRequest.parsedReqMap.get(RestConstantsZ.MATERIAL_ID) != null) {
        params.put(MobileExportConstants.MATERIAL_ID_KEY,
            parsedRequest.parsedReqMap.get(RestConstantsZ.MATERIAL_ID).toString());
      }
      if (parsedRequest.parsedReqMap.get(RestConstantsZ.TRANSACTION_TYPE) != null) {
        params.put(MobileExportConstants.TRANSACTIONS_TYPE_KEY,
            parsedRequest.parsedReqMap.get(RestConstantsZ.TRANSACTION_TYPE).toString());
      }
    }
    if (isExportTypeOrders(exportType)
        || isExportTypeTransfers(exportType)) {
      params.put(MobileExportConstants.ORDER_TYPE_KEY,
          (String) parsedRequest.parsedReqMap.get(MobileExportConstants.ORDERS_SUB_TYPE_KEY));
      params.put(MobileExportConstants.ORDERS_SUB_TYPE_KEY,
          (String) parsedRequest.parsedReqMap.get(RestConstantsZ.ORDER_TYPE));
    }
    try {
      params.put(MobileExportConstants.EMAIL_ID_KEY,
          (String) parsedRequest.parsedReqMap.get(RestConstantsZ.EMAIL));
      MobileExportAdapter
          exportAdapter =
          StaticApplicationContext.getBean(MobileExportAdapter.class);
      exportAdapter.buildExportRequest(params);
      message = backendMessages.getString("exports.successfully.scheduled.message") + CharacterConstants.DOUBLE_QUOTES + parsedRequest.parsedReqMap.get(RestConstantsZ.EMAIL)
          + CharacterConstants.DOUBLE_QUOTES + CharacterConstants.DOT;
    } catch (Exception e) {
      xLogger.severe(
          "{0} when scheduling task for kiosk data export of type {1} for kiosk {2} in domain {3} by user {4}: ",
          e.getClass().getName(), parsedRequest.parsedReqMap.get(RestConstantsZ.TYPE),
          parsedRequest.parsedReqMap.get(RestConstantsZ.KIOSK_ID), domainId, userId, e
      );
      message = backendMessages.getString("exports.failure.message");
      return getKioskDataExportJsonOutput(locale, false, message);
    }
    return getKioskDataExportJsonOutput(locale, true, message);
  }

  private static String getKioskDataExportJsonOutput(Locale locale, boolean status, String message)
      throws ProtocolException {
    String localeStr = Constants.LANG_DEFAULT;
    if (locale != null) {
      localeStr = locale.toString();
    }
    // Form the basic out and sent back JSON return string
    BasicOutput jsonOutput =
        new BasicOutput(status, message, null, localeStr, RESTUtil.VERSION_01);
    return jsonOutput.toJSONString();
  }

  // Get the login expiry time (in milliseconds)
  private static long getLoginExpiry() {
    Calendar cal = GregorianCalendar.getInstance();
    cal.setTime(new Date());
    // Add expiry time to this date
    cal.add(Calendar.DATE, Constants.LOGIN_DURATION);

    return cal.getTimeInMillis();
  }

  // Get domain-specific configuration to be sent to mobile
  private static Hashtable<String, Object> getConfig(DomainConfig dc, IUserAccount user,
                                                     Optional<Date> modifiedSinceDate)
      throws ServiceException {
    xLogger.fine("Entered getConfig");
    // Get domain config
    if (dc == null) {
      return null;
    }
    OrdersConfig oc = dc.getOrdersConfig();
    Hashtable<String, Object>
        config =
        new Hashtable<>();

    if (!isConfigModified(modifiedSinceDate, user.getDomainId())) {
      return config;
    }
    String transNaming = dc.getTransactionNaming();
    if (transNaming != null) {
      config.put(JsonTagsZ.TRANSACTION_NAMING, transNaming);
    }
    String orderGen = dc.getOrderGeneration();
    if (orderGen == null) {
      orderGen = DomainConfig.ORDERGEN_DEFAULT;
    }
    config.put(JsonTagsZ.ORDER_GENERATION, orderGen);
    // Get config. on auto-posting of inventory
    config.put(JsonTagsZ.UPDATESTOCK_ON_SHIPPED, String.valueOf(dc.autoGI()));
    config.put(JsonTagsZ.UPDATESTOCK_ON_FULFILLED, String.valueOf(dc.autoGR()));
    // Role-specific configs.
    CapabilityConfig cconf = dc.getCapabilityByRole(user.getRole());
    String transMenu, tagsInventory, tagsOrders, geoCodingStrategy, creatableEntityTypes;
    boolean allowRouteTagEditing, loginAsReconnect;
    boolean sendVendors, sendCustomers, disableShippingOnMobile, enableBarcoding, enableRFID;
    // Inventory tags to hide by operation, if any
    Hashtable<String, String> invTgsToHide;
    if (cconf != null) { // send role-specific configuration
      transMenu = StringUtil.getCSV(cconf.getCapabilities());
      tagsInventory = cconf.getTagsInventory();
      tagsOrders = cconf.getTagsOrders();
      sendVendors = cconf.sendVendors();
      sendCustomers = cconf.sendCustomers();
      geoCodingStrategy = cconf.getGeoCodingStrategy();
      creatableEntityTypes = StringUtil.getCSV(cconf.getCreatableEntityTypes());
      allowRouteTagEditing = cconf.allowRouteTagEditing();
      loginAsReconnect = cconf.isLoginAsReconnect();
      disableShippingOnMobile = cconf.isDisableShippingOnMobile();
      enableBarcoding = cconf.isBarcodingEnabled();
      enableRFID = cconf.isRFIDEnabled();
      invTgsToHide = getInventoryTagsToHide(cconf);
    } else { // send generic configuration
      transMenu = dc.getTransactionMenusString();
      tagsInventory = dc.getTagsInventory();
      tagsOrders = dc.getTagsOrders();
      sendVendors = dc.sendVendors();
      sendCustomers = dc.sendCustomers();
      geoCodingStrategy = dc.getGeoCodingStrategy();
      creatableEntityTypes = StringUtil.getCSV(dc.getCreatableEntityTypes());
      allowRouteTagEditing = dc.allowRouteTagEditing();
      loginAsReconnect = dc.isLoginAsReconnect();
      disableShippingOnMobile = dc.isDisableShippingOnMobile();
      enableBarcoding = dc.isBarcodingEnabled();
      enableRFID = dc.isRFIDEnabled();
      invTgsToHide = getInventoryTagsToHide(dc);
    }
    if (invTgsToHide != null && !invTgsToHide.isEmpty()) {
      config.put(JsonTagsZ.TAGS_INVENTORY_OPERATION, invTgsToHide);
    }

    if (transMenu != null) {
      config.put(JsonTagsZ.TRANSACTIONS, transMenu);
    }
    if (tagsInventory != null && !tagsInventory.isEmpty()) {
      config.put(JsonTagsZ.TAGS_INVENTORY, tagsInventory);
    }
    if (tagsOrders != null && !tagsOrders.isEmpty()) {
      config.put(JsonTagsZ.TAGS_ORDERS, tagsOrders);
    }
    if (sendVendors) {
      config.put(JsonTagsZ.VENDORS_MANDATORY, JsonTagsZ.STATUS_TRUE);
    }
    if (sendCustomers) {
      config.put(JsonTagsZ.CUSTOMERS_MANDATORY, JsonTagsZ.STATUS_TRUE);
    }
    if (dc.allowEmptyOrders()) {
      config.put(JsonTagsZ.ALLOW_EMPTY_ORDERS, JsonTagsZ.STATUS_TRUE);
    }
    if (dc.allowMarkOrderAsFulfilled()) {
      config.put(JsonTagsZ.ORDER_MARKASFULFILLED, JsonTagsZ.STATUS_TRUE);
    }
    if (dc.getPaymentOptions() != null && !dc.getPaymentOptions().isEmpty()) {
      config.put(JsonTagsZ.PAYMENT_OPTION, dc.getPaymentOptions());
    }
    if (dc.getPackageSizes() != null && !dc.getPackageSizes().isEmpty()) {
      config.put(JsonTagsZ.PACKAGE_SIZE, dc.getPackageSizes());
    }
    if (dc.getOrderTags() != null && !dc.getOrderTags().isEmpty()) {
      config.put(JsonTagsZ.ORDER_TAGS, dc.getOrderTags());
    }
    if (geoCodingStrategy != null) {
      config.put(JsonTagsZ.GEOCODING_STRATEGY, geoCodingStrategy);
    }
    if (creatableEntityTypes != null && !creatableEntityTypes.isEmpty()) {
      config.put(JsonTagsZ.CREATABLE_ENTITY_TYPES, creatableEntityTypes);
    }
    if (allowRouteTagEditing) {
      config.put(JsonTagsZ.ALLOW_ROUTETAG_EDITING, String.valueOf(true));
    }
    if (loginAsReconnect) {
      config.put(JsonTagsZ.LOGIN_AS_RECONNECT, String.valueOf(true));
    }
    if (!disableShippingOnMobile) {
      config.put(JsonTagsZ.ENABLE_SHIPPING_MOBILE, String.valueOf(true));
    }
    if (enableBarcoding) {
      config.put(JsonTagsZ.ENABLE_BARCODING_MOBULE, String.valueOf(true));
    }
    if (enableRFID) {
      config.put(JsonTagsZ.ENABLE_RFIDS_MOBILE, String.valueOf(true));
    }
    // Send transaction reasons
    // Get wastage reason from the higest level, always
    String wastageReasons = dc.getWastageReasons();
    if (wastageReasons != null && !wastageReasons.isEmpty()) {
      config.put(JsonTagsZ.REASONS_WASTAGE, StringUtil.getCSV(new ArrayList<>(
          new LinkedHashSet<>(Arrays.asList(wastageReasons.split(CharacterConstants.COMMA))))));
    }
    InventoryConfig ic = dc.getInventoryConfig();
    // Issue reasons, if any
    String issueReasons = ic.getTransReason(ITransaction.TYPE_ISSUE);
    if (StringUtils.isNotEmpty(issueReasons)) {
      config.put(JsonTagsZ.REASONS_ISSUE, issueReasons);
    }
    // Receipt reasons, if any
    String receiptReasons = ic.getTransReason(ITransaction.TYPE_RECEIPT);
    if (StringUtils.isNotEmpty(receiptReasons)) {
      config.put(JsonTagsZ.REASONS_RECEIPT, receiptReasons);
    }
    // Stockcount reasons, if any
    String stockcountReasons = ic.getTransReason(ITransaction.TYPE_PHYSICALCOUNT);
    if (StringUtils.isNotEmpty(stockcountReasons)) {
      config.put(JsonTagsZ.REASONS_STOCKCOUNT, stockcountReasons);
    }
    // Transfer reasons, if any
    String transferReasons = ic.getTransReason(ITransaction.TYPE_TRANSFER);
    if (StringUtils.isNotEmpty(transferReasons)) {
      config.put(JsonTagsZ.REASONS_TRANSFER, transferReasons);
    }
    // Returns-incoming reasons, if any
    String retIncReasons = ic.getTransReason(ITransaction.TYPE_RETURNS_INCOMING);
    if (StringUtils.isNotEmpty(retIncReasons)) {
      config.put(JsonTagsZ.REASONS_RETURNS_INCOMING, StringUtil.getCSV(retIncReasons.split(CharacterConstants.COMMA)));
    }
    // Returns-outgoing reasons, if any
    String retOutReasons = ic.getTransReason(ITransaction.TYPE_RETURNS_OUTGOING);
    if (StringUtils.isNotEmpty(retOutReasons)) {
      config.put(JsonTagsZ.REASONS_RETURNS_OUTGOING, retOutReasons);
    }
    MobileConfigBuilder mobileConfigBuilder = StaticApplicationContext.getBean(MobileConfigBuilder.class);
    // Reasons by material tag, if any
    Map<String,Map<String,String>> rsnsByMtag = mobileConfigBuilder.buildReasonsByTag(ic);
    if (MapUtils.isNotEmpty(rsnsByMtag)) {
      config.put(JsonTagsZ.REASONS_BY_TAG, rsnsByMtag);
    }
    Map<String,MobileDefaultReasonsConfigModel> defRsnsCfgByTransTypeMap = mobileConfigBuilder.buildMobileDefaultReasonsConfigModelByTransType(
        ic);
    if (MapUtils.isNotEmpty(defRsnsCfgByTransTypeMap)) {
      config.put(JsonTagsZ.DEFAULT_REASONS, defRsnsCfgByTransTypeMap);
    }
    if (CollectionUtils.isNotEmpty(ic.getTransactionTypesWithReasonMandatory())) {
      config.put(JsonTagsZ.TRANSACTION_TYPES_WITH_REASON_MANDATORY,
          ic.getTransactionTypesWithReasonMandatory());
    }
    // Material Status, if any
    Map<String,Map<String,String>> matStatusByType = getMaterialStatusByType(ic);
    if (MapUtils.isNotEmpty(matStatusByType)) {
      config.put(JsonTagsZ.MATERIAL_STATUS_OPERATION, matStatusByType);
    }
    //asset configuration
    Hashtable<String,Object> enableAssetMgmt = new Hashtable();
    enableAssetMgmt.put(JsonTagsZ.ENABLE_ASSET_MANAGEMENT, dc.getAssetConfig().getEnable());
    config.put(JsonTagsZ.ASSET,enableAssetMgmt);

    //Min Max Frequency
    String minMaxFreq = ic.getMinMaxDur();
    if (minMaxFreq != null && !minMaxFreq.isEmpty()) {
      if (InventoryConfig.FREQ_DAILY.equals(minMaxFreq)) {
        config.put(JsonTagsZ.MINMAX_FREQUENCY, 'd');
      } else if (InventoryConfig.FREQ_WEEKLY.equals(minMaxFreq)) {
        config.put(JsonTagsZ.MINMAX_FREQUENCY, 'w');
      } else if (InventoryConfig.FREQ_MONTHLY.equals(minMaxFreq)) {
        config.put(JsonTagsZ.MINMAX_FREQUENCY, 'm');
      }
    }
    if (ic.getPermissions() != null && ic.getPermissions().invCustomersVisible) {
      config.put(JsonTagsZ.INVENTORY_VISIBLE, true);
    }
    // Currency
    String currency = dc.getCurrency();
    if (currency != null && !currency.isEmpty()) {
      config.put(JsonTagsZ.CURRENCY, currency);
    }
    // Route tags, if any
    if (dc.getRouteTags() != null) {
      config.put(JsonTagsZ.ROUTE_TAG, dc.getRouteTags());
    }
    // The following code was added to fix LS-1227.
    // Default vendor ID and Default vendor Name if any
    Long defaultVendorId = dc.getVendorId();
    if (defaultVendorId != null) {
      config.put(JsonTagsZ.VENDORID, defaultVendorId.toString());
      // Get the default vendor name
      try {
        EntitiesService as = StaticApplicationContext.getBean(EntitiesServiceImpl.class);
        IKiosk k = as.getKiosk(defaultVendorId, false);
        if (k != null) {
          config.put(JsonTagsZ.VENDOR, k.getName());
          config.put(JsonTagsZ.VENDOR_CITY, k.getCity());
        }
      } catch (ServiceException se) {
        xLogger.warn("ServiceException when getting default vendor name for vendor id {0}: {1}",
            defaultVendorId, se.getMessage());
      }
    }
    // The support related information should be added here.
    // Get the general config from system configuration
    GeneralConfig gc = null;
    try {
      gc = GeneralConfig.getInstance();
    } catch (ConfigurationException ce) {
      xLogger.warn("Exception while getting GeneralConfiguration. Message: {0}", ce.getMessage());
    }
    if (gc != null && gc.getAupg() != null) {
      Hashtable<String, String> upgradeConfig = getAppUpgradeVersion(gc);
      if (upgradeConfig != null) {
        config.put(JsonTagsZ.APP_UPGRADE, upgradeConfig);
      }
    }
    SupportConfig
        supportConfig =
        dc.getSupportConfigByRole(
            user.getRole()); // Get the support configuration for the role of the logged in user
    String supportEmail = null, supportPhone = null, supportContactName = null;
    // If Support is configured in Domain configuration, get support information.
    if (supportConfig != null) {
      // Get the supportUserId or support user name from Support configuration.
      String supportUserId = supportConfig.getSupportUser();

      if (supportUserId != null && !supportUserId.isEmpty()) {
        // If userId is configured in SupportConfig, get the phone number, email and contact name from the UserAccount object
        try {
          UsersService as = StaticApplicationContext.getBean(UsersServiceImpl.class);
          IUserAccount ua = as.getUserAccount(supportUserId);
          supportEmail = ua.getEmail();
          supportPhone = ua.getMobilePhoneNumber();
          supportContactName = ua.getFullName();
        } catch (SystemException se) {
          xLogger
              .warn("ServiceException when getting support user with id {0}: {1}", supportUserId,
                  se.getMessage());
        }
      } else {
        supportPhone = supportConfig.getSupportPhone();
        supportEmail = supportConfig.getSupportEmail();
        supportContactName = supportConfig.getSupportUserName();
        // The extra check below is added because if a support info is removed from the
        // General Configuration, supportConfig exists but values for supportPhone, supportEmail and supportContactName are ""
        // In that case, it has to send the support information present in System Configuration -> generalconfig
        if (supportPhone == null || supportPhone.isEmpty() && gc != null) {
          supportPhone = gc.getSupportPhone();
        }
        if (supportEmail == null || supportEmail.isEmpty() && gc != null) {
          supportEmail = gc.getSupportEmail();
        }
      }
    } else {
      // If Support is not configured under Domain configuration, get the support phone number and email from the System configuration
      if (gc != null) {
        supportEmail = gc.getSupportEmail();
        supportPhone = gc.getSupportPhone();
      }
    }
    // Add support configuration only if it is present in Domain configuration or in System configuration.
    if (supportContactName != null && !supportContactName.isEmpty()) {
      config.put(JsonTagsZ.SUPPORT_CONTACT_NAME, supportContactName);
    }
    if (supportEmail != null && !supportEmail.isEmpty()) {
      config.put(JsonTagsZ.SUPPORT_EMAIL, supportEmail);
    }
    if (supportPhone != null && !supportPhone.isEmpty()) {
      config.put(JsonTagsZ.SUPPORT_PHONE, supportPhone);
    }
    // Synchronization by mobile configuration to be read from DomainConfig. It should be sent to the mobile app, only if the values of intervals are > 0
    SyncConfig syncConfig = dc.getSyncConfig();
    if (syncConfig != null) {
      Hashtable<String, String> intrvlsHt = getIntervalsHashtable(syncConfig);
      if (intrvlsHt != null && !intrvlsHt.isEmpty()) {
        config.put(JsonTagsZ.INTERVALS, intrvlsHt);
      }
    }
    // Local login required
    config.put(JsonTagsZ.NO_LOCAL_LOGIN_WITH_VALID_TOKEN,
        String.valueOf(!dc.isLocalLoginRequired()));
    // Add the config. to capture actual date of trans, if enabled
    Map<String,Map<String,String>> atdByType = getActualTransDateConfigByType(ic);
    if (MapUtils.isNotEmpty(atdByType)) {
      config.put(JsonTagsZ.CAPTURE_ACTUAL_TRANSACTION_DATE, atdByType);
    }
    if (dc.isDisableOrdersPricing()) {
      config.put(JsonTagsZ.DISABLE_ORDER_PRICING, String.valueOf(dc.isDisableOrdersPricing()));
    }
    // Add order reasons to config
    addReasonsConfiguration(config, oc);
    Hashtable<String, Object> oCfg = getOrdersConfiguration(dc);
    if (!oCfg.isEmpty()) {
      config.put(JsonTagsZ.ORDER_CONFIG, oCfg);
    }

    try {
      SMSConfig smsConfig = SMSConfig.getInstance();
      if (smsConfig != null) {
        String country = dc.getCountry() != null ? dc.getCountry() : Constants.COUNTRY_DEFAULT;
        // For incoming
        SMSConfig.ProviderConfig
            iProviderConfig =
            smsConfig.getProviderConfig(smsConfig.getProviderId(country, SMSService.INCOMING));
        Hashtable<String, String> sms = new Hashtable<>(3);
        if (iProviderConfig != null) {
          sms.put(JsonTagsZ.GATEWAY_PHONE_NUMBER,
              iProviderConfig.getString(SMSConfig.ProviderConfig.LONGCODE));
          sms.put(JsonTagsZ.GATEWAY_ROUTING_KEYWORD,
              iProviderConfig.getString(SMSConfig.ProviderConfig.KEYWORD));
        }
        //For outgoing
        SMSConfig.ProviderConfig
            oProviderConfig =
            smsConfig.getProviderConfig(smsConfig.getProviderId(country, SMSService.OUTGOING));
        if (oProviderConfig != null) {
          sms.put(JsonTagsZ.SENDER_ID,
              oProviderConfig.getString(SMSConfig.ProviderConfig.SENDER_ID));
        }
        config.put(JsonTagsZ.SMS, sms);
      }

      // Approval configuration
      ApprovalsConfig approvalsConfig = dc.getApprovalsConfig();
      if (approvalsConfig != null) {
        MobileApprovalsConfigModel
            mobileApprovalsConfigModel =
            mobileConfigBuilder.buildApprovalConfiguration(approvalsConfig);
        if (mobileApprovalsConfigModel != null) {
          config.put(JsonTagsZ.APPROVALS, mobileApprovalsConfigModel);
        }
      }
      // Add the domain specific Store app theme configuration
      config.put(JsonTagsZ.GUI_THEME, dc.getStoreAppTheme());
      // Accounting configuration
      AccountingConfig acctConfig = dc.getAccountingConfig();
      if (acctConfig != null) {
        MobileAccountingConfigModel
            mobileAccountingConfigModel =
            mobileConfigBuilder.buildAccountingConfiguration(
                dc.isAccountingEnabled(), acctConfig);
        config.put(JsonTagsZ.ACCOUNTING_CONFIG, mobileAccountingConfigModel);
      }
      if (IUserAccount.LR_LOGIN_RECONNECT.equals(user.getLoginReconnect())) {
        config.put(JsonTagsZ.LOGIN_AS_RECONNECT, Constants.TRUE);
      } else if (IUserAccount.LR_LOGIN_DONT_RECONNECT.equals(user.getLoginReconnect())) {
        config.remove(JsonTagsZ.LOGIN_AS_RECONNECT);
      }
      // Get user specific gui theme configuration and add it to config
      int storeAppTheme = user.getStoreAppTheme();
      if (storeAppTheme != Constants.GUI_THEME_SAME_AS_IN_DOMAIN_CONFIGURATION) {
        config.put(JsonTagsZ.GUI_THEME, storeAppTheme);
      }
      // Add kiosk tags, if configured in the domain
      String[] kioskTags = TagUtil.getTagsArray(dc.getKioskTags());
      if (kioskTags != null) {
        config.put(JsonTagsZ.ENTITY_TAG, Arrays.asList(kioskTags));
      }


      // Add default material tags, if configured in the domain
      String[] defaultMaterialTags = TagUtil.getTagsArray(dc.getDashboardConfig().getDbOverConfig().dmtg);
      if (defaultMaterialTags != null) {
        Hashtable<String, Object>
            configDB =
            new Hashtable<>();
        configDB.put(JsonTagsZ.DEFAULT_MATERIAL_TAGS, Arrays.asList(defaultMaterialTags));
        config.put(JsonTagsZ.DASHBOARD, configDB);
      }
      // Returns policy configuration
      List<ReturnsConfig> returnsConfigs = dc.getInventoryConfig().getReturnsConfig();
      if (CollectionUtils.isNotEmpty(returnsConfigs)) {
        Optional<MobileReturnsConfigModel> mobileReturnsConfigModel = mobileConfigBuilder.buildMobileReturnsConfigModel(
            returnsConfigs);
        mobileReturnsConfigModel.ifPresent(returnsModel -> config.put(JsonTagsZ.RETURNS,
            returnsModel));
      }
    } catch (Exception e) {
      xLogger.warn("Error in getting system configuration: {0}", e);
    }
    config.put(JsonTagsZ.ENABLE_TWO_FACTOR_AUTHENTICATION, dc.isTwoFactorAuthenticationEnabled());
    // Return config
    if (config.isEmpty()) {
      return null;
    } else {
      xLogger.fine("Exiting getConfig, config: {0}", config.toString());
      return config;
    }
  }

  // Check if the stock value should be sent back as an integer or float - depending on app. version (beyond 1.2.0 it is float)
  public static boolean forceIntegerForStock(String appVersion) {
    if (appVersion == null) {
      return true;
    }
    String v = appVersion.replaceAll("\\.", "");
    try {
      int n = Integer.parseInt(v);
      if (n >= 200) {
        return false;
      }
    } catch (NumberFormatException e) {
      xLogger.warn("Invalid app. version number when checking force-integer-for-stock, {0}: {1}",
          appVersion, e.getMessage());
    }
    return true;
  }

  private static void addReasonsConfiguration(Hashtable config, OrdersConfig oc) {
    Hashtable<String, Object> hs = new Hashtable<>();
    if (oc.getOrderRecommendationReasons() != null && !oc.getOrderRecommendationReasons()
        .isEmpty()) {
      hs.put(JsonTagsZ.REASONS, oc.getOrderRecommendationReasons());
    }
    hs.put(JsonTagsZ.MANDATORY, oc.getOrderRecommendationReasonsMandatory());
    config.put(JsonTagsZ.IGNORE_ORDER_RECOMMENDATION_REASONS, hs);

    // Partial Fulfillment reasons
    Hashtable<String, Object> pfRsnsHt = new Hashtable<>();
    if (oc.getPartialFulfillmentReasons() != null && !oc.getPartialFulfillmentReasons().isEmpty()) {
      pfRsnsHt.put(JsonTagsZ.REASONS, oc.getPartialFulfillmentReasons());
    }
    pfRsnsHt.put(JsonTagsZ.MANDATORY, oc.getPartialFulfillmentReasonsMandatory());
    config.put(JsonTagsZ.REASONS_FOR_PARTIAL_FULFILLMENT, pfRsnsHt);

    // Partial shipment reasons
    Hashtable<String, Object> psRsnsHt = new Hashtable<>();
    if (oc.getPartialShipmentReasons() != null && !oc.getPartialShipmentReasons().isEmpty()) {
      psRsnsHt.put(JsonTagsZ.REASONS, oc.getPartialShipmentReasons());
    }
    psRsnsHt.put(JsonTagsZ.MANDATORY, oc.getPartialShipmentReasonsMandatory());
    config.put(JsonTagsZ.REASONS_FOR_PARTIAL_SHIPMENT, psRsnsHt);

    // Cancellation order reasons
    Hashtable<String, Object> coRsnsHt = new Hashtable<>();
    if (oc.getCancellingOrderReasons() != null && !oc.getCancellingOrderReasons().isEmpty()) {
      coRsnsHt.put(JsonTagsZ.REASONS, oc.getCancellingOrderReasons());
    }
    coRsnsHt.put(JsonTagsZ.MANDATORY, oc.getCancellingOrderReasonsMandatory());
    config.put(JsonTagsZ.REASONS_FOR_CANCELLING_ORDER, coRsnsHt);

    // Reasons for editing order quantity
    Hashtable<String, Object> eoRsnsHt = new Hashtable<>();
    if (oc.getEditingQuantityReasons() != null && !oc.getEditingQuantityReasons().isEmpty()) {
      eoRsnsHt.put(JsonTagsZ.REASONS, oc.getEditingQuantityReasons());
    }
    eoRsnsHt.put(JsonTagsZ.MANDATORY, oc.getEditingQuantityReasonsMandatory());
    config.put(JsonTagsZ.REASONS_FOR_EDITING_ORDER_QUANTITY, eoRsnsHt);
  }

  // Method to switch to new host if configured in the Domain configuration for a user's domain
  public static boolean checkIfLoginShouldNotBeAllowed(IUserAccount u, HttpServletResponse resp,
                                                       HttpServletRequest req) throws ServiceException {
    // Get the configuration. Check if switch to new host is enabled. If yes, get the new host name and return 409 response.
    return checkAppVersion(u,resp,req) || checkDomainSwith(u,resp);
  }

  private static boolean checkAppVersion(IUserAccount u, HttpServletResponse resp,
                                         HttpServletRequest req) throws ServiceException {
    String version = req.getParameter(RestConstantsZ.VERSION);
    try {
      String[] blockedVersions = StringUtil.getCSVTokens(
          GeneralConfig.getInstance().getBlockedAppVersion());
      if (blockedVersions != null && blockedVersions.length > 0 && Arrays.asList(blockedVersions)
          .contains(version)) {
        resp.setStatus(200);
        resp.setContentType("application/json; charset=\"UTF-8\"");
        PrintWriter pw = resp.getWriter();
        pw.write(GsonUtil
            .authenticateOutputToJson(false, ExceptionUtils.constructMessage("G004", u.getLocale(),
                new Object[]{version, GeneralConfig.getInstance().getUpgradeToVersion()}), null, new UserEntitiesModel(u,null), null,
                RESTUtil.VERSION_01));
        pw.close();
        return true;
      }
    } catch (IOException e) {
      xLogger
          .severe("Exception {0} sending error message to user {1} in domain {2}. Message: {3}",
              e.getClass().getName(), u.getUserId(), u.getDomainId(), e);
    } catch (ConfigurationException e) {
      throw new ServiceException(e);
    }
    return false;
  }

  private static boolean checkDomainSwith(IUserAccount u, HttpServletResponse resp) {
    Long domainId = u.getDomainId();
    DomainConfig dc = DomainConfig.getInstance(domainId);
    if (dc.isEnableSwitchToNewHost() && dc.getNewHostName() != null && !dc.getNewHostName()
        .isEmpty()) {
      xLogger.info("Switch to new host is enabled for domainId: {0}, dc.getNewHostName(): {1}",
          domainId, dc.getNewHostName());
      try {
        resp.setStatus(409);
        resp.setContentType("text/plain; charset=UTF-8");
        PrintWriter pw = resp.getWriter();
        pw.write(dc.getNewHostName());
        pw.close();
        return true;
      } catch (IOException e) {
        xLogger
            .severe("Exception {0} sending error message to user {1} in domain {2}. Message: {3}",
                e.getClass().getName(), u.getUserId(), domainId, e.getMessage());
      }
    }
    return false;
  }

  // Method to get transaction history
  @SuppressWarnings("unchecked")
  public static Results getTransactions(Long kioskId, Locale locale, String timezone,
                                        Date untilDate, PageParams pageParams,
                                        String materialTag)
      throws ServiceException {
    // Get the services
    InventoryManagementService ims =
        StaticApplicationContext.getBean(InventoryManagementServiceImpl.class);
    UsersService as = StaticApplicationContext.getBean(UsersServiceImpl.class);
    EntitiesService entitiesService = StaticApplicationContext.getBean(EntitiesServiceImpl.class);
    MaterialCatalogService mcs = StaticApplicationContext.getBean(MaterialCatalogServiceImpl.class);
    // Get the transactions
    Results
        results =
        ims.getInventoryTransactionsByKiosk(kioskId, materialTag, null, untilDate, null, pageParams, null,
            false, null);
    List<ITransaction> transactions = (List<ITransaction>) results.getResults();
    String cursor = results.getCursor();
    Vector<Hashtable<String, String>> transData = new Vector<>();
    for (ITransaction trans : transactions) {
      // Create a transaction data Hashtable
      Hashtable<String, String> transaction = new Hashtable<>();
      transaction.put(JsonTagsZ.MATERIAL_ID, trans.getMaterialId().toString());

      IMaterial m;
      try {
        m = mcs.getMaterial(trans.getMaterialId());
      } catch (ServiceException e) {
        xLogger.warn("Exception while getting material for material ID {0}",
            trans.getMaterialId()); // Material may have been deleted, skip this transaction
        continue;
      }
      if (!materialExistsInKiosk(trans.getKioskId(), trans.getMaterialId())) {
        transaction.put(JsonTagsZ.MATERIAL_NAME, m.getName());
      }
      transaction.put(JsonTagsZ.TRANSACTION_TYPE, trans.getType());
      transaction.put(JsonTagsZ.QUANTITY, String.valueOf(trans.getQuantity()));

      Date time = trans.getTimestamp();
      transaction.put(JsonTagsZ.TIMESTAMP, LocalDateUtil.format(time, locale, timezone));

      if (trans.getReason() != null && !trans.getReason().isEmpty()) {
        transaction.put(JsonTagsZ.REASON, trans.getReason());
      }
      transaction.put(JsonTagsZ.OPENING_STOCK, String.valueOf(trans.getOpeningStock()));
      transaction.put(JsonTagsZ.CLOSING_STOCK, String.valueOf(trans.getClosingStock()));
      String userId = trans.getSourceUserId();
      if (userId
          != null) { // typically, this is not null, but could be for very old trans. generated using very old code where this functionality did not exist
        transaction.put(JsonTagsZ.USER_ID, userId);
        try {
          IUserAccount u = as.getUserAccount(userId);
          String userName = u.getFullName();
          transaction.put(JsonTagsZ.USER, userName);
        } catch (Exception e) {
          xLogger.warn("{0} while getting user name for userId: {1}. Message:{2}",
              e.getClass().getName(), userId, e.getMessage());
        }
      }

      Long linkedKioskId = trans.getLinkedKioskId();
      if (linkedKioskId != null) {
        transaction.put(JsonTagsZ.LINKED_KIOSK_ID, String.valueOf(linkedKioskId));
        try {
          IKiosk linkedKiosk = entitiesService.getKiosk(linkedKioskId, false);
          String linkedKioskName = linkedKiosk.getName();
          transaction.put(JsonTagsZ.LINKED_KIOSK_NAME, linkedKioskName);
        } catch (Exception e) {
          xLogger.warn("{0} while getting kiosk name for linkedKioskId: {1}. Message:{2}",
              e.getClass().getName(), linkedKioskId, e.getMessage());
        }
      }
      if (trans.getBatchId() != null && !trans.getBatchId().isEmpty()) {
        transaction.put(JsonTagsZ.BATCH_ID, trans.getBatchId());
      }
      if (m.isBatchEnabled()) {
        transaction
            .put(JsonTagsZ.OPENING_STOCK_IN_BATCH, String.valueOf(trans.getOpeningStockByBatch()));
      }

      Date batchExpiry = trans.getBatchExpiry();
      if (batchExpiry != null) {
        transaction.put(JsonTagsZ.BATCH_EXPIRY,
            LocalDateUtil.formatCustom(batchExpiry, Constants.DATE_FORMAT, null));
      }

      if (trans.getBatchManufacturer() != null && !trans.getBatchManufacturer().isEmpty()) {
        transaction.put(JsonTagsZ.BATCH_MANUFACTUER_NAME, trans.getBatchManufacturer());
      }

      Date batchMfdDate = trans.getBatchManufacturedDate();
      if (batchMfdDate != null) {
        transaction.put(JsonTagsZ.BATCH_MANUFACTURED_DATE,
            LocalDateUtil.formatCustom(batchMfdDate, Constants.DATE_FORMAT, null));
      }

      if (m.isBatchEnabled()) {
        transaction
            .put(JsonTagsZ.CLOSING_STOCK_IN_BATCH, String.valueOf(trans.getClosingStockByBatch()));
      }

      if (StringUtils.isNotBlank(trans.getMaterialStatus())) {
        transaction.put(JsonTagsZ.MATERIAL_STATUS, trans.getMaterialStatus());
      }

      Date atd = trans.getAtd();
      if (atd != null) {
        transaction.put(JsonTagsZ.ACTUAL_TRANSACTION_DATE,
            LocalDateUtil.formatCustom(atd, Constants.DATE_FORMAT, null));
      }
      String mTags = StringUtil.getCSV(trans.getTags(TagUtil.TYPE_MATERIAL));
      if (StringUtils.isNotEmpty(mTags)) {
        transaction.put(JsonTagsZ.TAGS, mTags);
      }
      transData.add(transaction);
    }
    return new Results(transData, cursor);
  }

  // Get relevant (valid (non zero stock) and non expired) batches for a given inventory
  public static Vector<Hashtable<String, String>> getBatchData(IInvntry inv, Locale locale,
                                                               String timezone,
                                                               InventoryManagementService ims,
                                                               boolean isBatchMgmtEnabled,
                                                               boolean isAutoPostingIssuesEnabled) {
    xLogger.fine("Entered getBatchData");
    Vector<Hashtable<String, String>> batches = null;
    // Only if kiosk is batch management enabled, return batches. Otherwise, return null.
    if (isBatchMgmtEnabled) {
      try {
        Results<IInvntryBatch>
            results =
            ims.getValidBatches(inv.getMaterialId(), inv.getKioskId(), new PageParams(null,
                PageParams.DEFAULT_SIZE)); // NOTE: Get only up to the 50 last batches
        if (results != null && results.getResults() != null) {
          batches = new Vector<>();
          for (IInvntryBatch batch : results.getResults()) {
            if (!batch.isExpired()) {
              batches.add(batch.toMapZ(locale, timezone, isAutoPostingIssuesEnabled));
            }
          }
        }
      } catch (Exception e) {
        xLogger.warn("{0} when trying to get batch info. for inv. {1}-{2} in domain {3}: {4}",
            e.getClass().getName(), inv.getKioskId(), inv.getMaterialId(), inv.getDomainId(),
            e.getMessage());
        e.printStackTrace();
      }
    }
    return batches;
  }

  public static Vector<Hashtable<String, String>> getExpiredBatchData(IInvntry inv, Locale locale,
                                                                      String timezone,
                                                                      InventoryManagementService ims,
                                                                      boolean isBatchMgmtEnabled,
                                                                      boolean isAutoPostingIssuesEnabled) {
    xLogger.fine("Entered getExpiredBatchData");
    Vector<Hashtable<String, String>> expiredBatches = null;
    // Only if kiosk is batch management enabled, return batches. Otherwise, return null.
    if (isBatchMgmtEnabled) {
      try {
        Results<IInvntryBatch>
            results =
            ims.getBatches(inv.getMaterialId(), inv.getKioskId(), new PageParams(null,
                PageParams.DEFAULT_SIZE)); // NOTE: Get only up to the 50 last batches
        if (results != null && results.getResults() != null) {
          expiredBatches = new Vector<>();
          for (IInvntryBatch batch : results.getResults()) {
            if (batch.isExpired() && BigUtil.greaterThanZero(batch.getQuantity())) {
              expiredBatches.add(batch.toMapZ(locale, timezone, isAutoPostingIssuesEnabled));
            }
          }
        }
      } catch (Exception e) {
        xLogger.warn("{0} when trying to get batch info. for inv. {1}-{2} in domain {3}: {4}",
            e.getClass().getName(), inv.getKioskId(), inv.getMaterialId(), inv.getDomainId(),
            e.getMessage());
        e.printStackTrace();
      }
    }
    return expiredBatches;
  }

  protected static Map<String, Map<String, String>> getReasonsByTag(InventoryConfig ic) {
    Map<String, Map<String, String>> rsnsByMtag = new HashMap<>(1,1);
    if (MapUtils.isNotEmpty(ic.getImTransReasons())) {
      rsnsByMtag.put(JsonTagsZ.ISSUES, getReasonsAsCSVByTag(ic.getImTransReasons()));
    }
    if (MapUtils.isNotEmpty(ic.getRmTransReasons())) {
      rsnsByMtag.put(JsonTagsZ.RECEIPTS, getReasonsAsCSVByTag(ic.getRmTransReasons()));
    }
    if (MapUtils.isNotEmpty(ic.getSmTransReasons())) {
      rsnsByMtag.put(JsonTagsZ.PHYSICAL_STOCK, getReasonsAsCSVByTag(ic.getSmTransReasons()));
    }
    if (MapUtils.isNotEmpty(ic.getDmTransReasons())) {
      rsnsByMtag.put(JsonTagsZ.DISCARDS, getReasonsAsCSVByTag(ic.getDmTransReasons()));
    }
    if (MapUtils.isNotEmpty(ic.getTmTransReasons())) {
      rsnsByMtag.put(JsonTagsZ.TRANSFER, getReasonsAsCSVByTag(ic.getTmTransReasons()));
    }
    if (MapUtils.isNotEmpty(ic.getMtagRetIncRsns())) {
      rsnsByMtag.put(JsonTagsZ.RETURNS_INCOMING, getReasonsAsCSVByTag(ic.getMtagRetIncRsns()));
    }
    if (MapUtils.isNotEmpty(ic.getMtagRetOutRsns())) {
      rsnsByMtag.put(JsonTagsZ.RETURNS_OUTGOING, getReasonsAsCSVByTag(ic.getMtagRetOutRsns()));
    }
    return rsnsByMtag;
  }

  protected static Map<String,String> getReasonsAsCSVByTag(Map<String,ReasonConfig> reasonConfigByTagMap) {
    if (MapUtils.isEmpty(reasonConfigByTagMap)) {
      return Collections.emptyMap();
    }
    Map<String,String> reasonsByTag = new HashMap<>();
    reasonConfigByTagMap.entrySet()
                    .stream()
                    .filter(entry->CollectionUtils.isNotEmpty(entry.getValue().getReasons()))
                    .forEach(entry->reasonsByTag.put(entry.getKey(),StringUtils.join(
                        entry.getValue().getReasons(), CharacterConstants.COMMA)));
    return reasonsByTag;
  }

  protected static Map<String, Map<String, String>> getMaterialStatusByType(
      InventoryConfig ic) {
    Map<String, Map<String, String>> matStatusByType = new HashMap<>(1,1);
    Map<String, MatStatusConfig> matStatusConfigByType = ic.getMatStatusConfigMapByType();
    if (MapUtils.isNotEmpty(matStatusConfigByType)) {
      matStatusConfigByType.entrySet().forEach(entry -> {
            if (MapUtils.isNotEmpty(getMaterialStatusConfigAsMap(entry.getValue()))) {
              matStatusByType.put(entry.getKey(), getMaterialStatusConfigAsMap(entry.getValue()));
            }
          }
      );
    }
    return matStatusByType;
  }

  protected static Map<String,String> getMaterialStatusConfigAsMap(MatStatusConfig matStatusConfig) {
    if (matStatusConfig == null) {
      return Collections.emptyMap();
    }
    Map<String,String> matStatusMap = new HashMap<>(3,1);
    if(StringUtils.isNotEmpty(matStatusConfig.getDf())) {
      String df = StringUtil.getUniqueValueCSV(matStatusConfig.getDf());
      if (StringUtils.isNotEmpty(df)) {
        matStatusMap.put(JsonTagsZ.ALL, df);
      }
    }
    if(StringUtils.isNotEmpty(matStatusConfig.getEtsm())) {
      String etsm = StringUtil.getUniqueValueCSV(matStatusConfig.getEtsm());
      if (StringUtils.isNotEmpty(etsm)) {
        matStatusMap.put(JsonTagsZ.TEMP_SENSITVE_MATERIALS, etsm);
      }
    }
    if (MapUtils.isNotEmpty(matStatusMap)) {
      matStatusMap.put(JsonTagsZ.MANDATORY, String.valueOf(matStatusConfig.isStatusMandatory()));
    }
    return matStatusMap;
  }

  protected static Map<String, Map<String, String>> getActualTransDateConfigByType(
      InventoryConfig ic) {
    Map<String, Map<String, String>> actualTransDateConfigByType = new HashMap<>(1, 1);
    Map<String, ActualTransConfig> actualTransConfigByType = ic.getActualTransConfigMapByType();
    if (MapUtils.isNotEmpty(actualTransConfigByType)) {
      actualTransConfigByType.entrySet().forEach(entry -> {
            Map<String, String> actualTransConfig = getActualTransConfigByType(entry.getValue());
            if (MapUtils.isNotEmpty(actualTransConfig)) {
              actualTransDateConfigByType
                  .put(entry.getKey(), actualTransConfig);
            }
          }
      );
    }
    return actualTransDateConfigByType;
  }

  protected static Map<String,String> getActualTransConfigByType(ActualTransConfig actualTransConfig) {
    if (actualTransConfig == null || StringUtils.isEmpty(actualTransConfig.getTy())) {
      return Collections.emptyMap();
    }
    Map<String,String> actualTransConfigMap = new HashMap<>(1,1);
    actualTransConfigMap.put(JsonTagsZ.TYPE, actualTransConfig.getTy());
    return actualTransConfigMap;
  }

  private static Hashtable<String, String> getInventoryTagsToHide(Serializable config) {
    Hashtable<String, String> invTgsToHide = new Hashtable<>();
    Map<String, String> tagsByInvOp = null;
    if (config instanceof CapabilityConfig) {
      tagsByInvOp = ((CapabilityConfig) config).gettagsInvByOperation();
    } else if (config instanceof DomainConfig) {
      tagsByInvOp = ((DomainConfig) config).gettagsInvByOperation();
    }

    if (tagsByInvOp != null && !tagsByInvOp.isEmpty()) {
      Set<String> transTypes = tagsByInvOp.keySet();
      if (!transTypes.isEmpty()) {
        for (String transType : transTypes) {
          String tagsToHide = tagsByInvOp.get(transType);
          if (tagsToHide != null && !tagsToHide.isEmpty()) {
            invTgsToHide.put(transType, tagsToHide);
          }
        }
      }
    }
    return invTgsToHide;
  }

  private static Hashtable<String, String> getIntervalsHashtable(SyncConfig syncConfig) {
    Hashtable intrvlsHt = null;
    if (syncConfig != null) {
      intrvlsHt = new Hashtable<String, String>();
      if (syncConfig.getMasterDataRefreshInterval() > 0) {
        intrvlsHt.put(JsonTagsZ.INTERVAL_REFRESHING_MASTER_DATA_HOURS,
            Integer.valueOf(syncConfig.getMasterDataRefreshInterval()).toString());
      }
      if (syncConfig.getAppLogUploadInterval() > 0) {
        intrvlsHt.put(JsonTagsZ.INTERVAL_SENDING_APP_LOG_HOURS,
            Integer.valueOf(syncConfig.getAppLogUploadInterval()).toString());
      }
      if (syncConfig.getSmsTransmissionWaitDuration() > 0) {
        intrvlsHt.put(JsonTagsZ.INTERVAL_WAIT_BEFORE_SENDING_SMS_HOURS,
            Integer.valueOf(syncConfig.getSmsTransmissionWaitDuration()).toString());
      }
    }
    return intrvlsHt;
  }

  private static Hashtable<String, String> getAppUpgradeVersion(GeneralConfig config) {
    Hashtable intrvlsHt = null;
    if (config != null) {
      intrvlsHt = new Hashtable<String, Long>();
      if (config.getAupg() != null) {
        if (config.getAupg().get(JsonTagsZ.VERSION) != null) {
          intrvlsHt.put(JsonTagsZ.VERSION, config.getAupg().get(JsonTagsZ.VERSION));
        }
        if (config.getAupg().get(JsonTagsZ.TIMESTAMP) != null) {
          intrvlsHt
              .put(JsonTagsZ.TIMESTAMP, String.valueOf(config.getAupg().get(JsonTagsZ.TIMESTAMP)));
        }
      }
    }

    return intrvlsHt;
  }


  private static Hashtable<String, Object> getOrdersConfiguration(DomainConfig dc) {
    Hashtable<String, Object> ordCfg = new Hashtable<>();
    if (dc.autoGI()) {
      ordCfg.put(JsonTagsZ.AUTOMATICALLY_POST_ISSUES_ON_SHIPPING_ORDER, dc.autoGI());
    }
    if (dc.autoGR()) {
      ordCfg.put(JsonTagsZ.AUTOMATICALLY_POST_RECEIPTS_ON_FULFILLING_ORDER, dc.autoGR());
    }
    OrdersConfig oc = dc.getOrdersConfig();
    if (oc != null) {
      ordCfg.put(JsonTagsZ.TRANSFER_RELEASE, oc.isTransferRelease());
      // Configuration for automatic allocation and material status assignment to order
      ordCfg.put(JsonTagsZ.AUTO_ALLOCATE_INVENTORY_TO_ORDERS, oc.allocateStockOnConfirmation());
      ordCfg.put(JsonTagsZ.AUTO_ASSIGN_MATERIAL_STATUS_TO_ORDERS, oc.autoAssignFirstMatStatus());
      ordCfg.put(JsonTagsZ.MANDATORY_FIELDS, getMandatoryOrderFields(oc));
    }
    if (dc.isTransporterMandatory()) {
      ordCfg.put(JsonTagsZ.TRANSPORTER_MANDATORY, dc.isTransporterMandatory());
    }
    return ordCfg;
  }

  protected static HashMap<String , Object> getMandatoryOrderFields(OrdersConfig oc) {
    HashMap<String, Object> mandatoryFields = new HashMap<>();
    HashMap<String, Object> salesOrderFields = new HashMap<>();
    HashMap<String, Boolean> shippingFields = new HashMap<>();
    HashMap<String, Boolean> referenceFields = new HashMap<>();
    shippingFields.put(JsonTagsZ.REFERENCE_ID, oc.isReferenceIdMandatory());
    referenceFields.put(JsonTagsZ.PURCHASE_REFERENCE_ID, oc.isPurchaseReferenceIdMandatory());
    referenceFields.put(JsonTagsZ.TRANSFER_REFERENCE_ID, oc.isTransferReferenceIdMandatory());
    shippingFields.put(JsonTagsZ.EXPECTED_TIME_OF_ARRIVAL, oc.isExpectedArrivalDateMandatory());
    salesOrderFields.put(JsonTagsZ.SHIPPING, shippingFields);
    mandatoryFields.put(JsonTagsZ.MANDATE_REFERENCE_ID, referenceFields);
    mandatoryFields.put(JsonTagsZ.SALES_ORDERS, salesOrderFields);
    return mandatoryFields;
  }

  public static boolean materialExistsInKiosk(Long kioskId, Long materialId) {
    try {
      InventoryManagementService ims =
          StaticApplicationContext.getBean(InventoryManagementServiceImpl.class);
      IInvntry inv = ims.getInventory(kioskId, materialId);
      if (inv == null) {
        return false;
      }
    } catch (Exception e) {
      xLogger.warn("Exception while getting inventory for kiosk: {0}, material: {0}", kioskId,
          materialId, e);
      return false;
    }
    return true;
  }

  private static ParsedRequest parseGeneralExportFilters(
      Map<String, String> reqParamsMap,
      ResourceBundle backendMessages) {
    ParsedRequest parsedRequest = new ParsedRequest();
    if (reqParamsMap == null || reqParamsMap.isEmpty()) {
      return parsedRequest;
    }
    String kioskIdStr = reqParamsMap.get(RestConstantsZ.KIOSK_ID);
    if (StringUtils.isEmpty(kioskIdStr)) {
      parsedRequest.errMessage = backendMessages.getString("error.invalidkioskid");
      return parsedRequest;
    }
    try {
      Long kioskId = Long.parseLong(kioskIdStr);
      parsedRequest.parsedReqMap.put(RestConstantsZ.KIOSK_ID, kioskId);
    } catch (NumberFormatException e) {
      parsedRequest.errMessage = backendMessages.getString("error.invalidkioskid");
      xLogger.severe("Exception while parsing kiosk id. {0}",
          kioskIdStr, e);
      return parsedRequest;
    }
    String type = reqParamsMap.get(RestConstantsZ.TYPE);
    if (StringUtils.isEmpty(type) || !(INVENTORY.equals(type) || TRANSACTIONS.equals(type)
        || ORDERS.equals(type) || TRANSFERS.equals(type))) {
      parsedRequest.errMessage = backendMessages.getString("error.invalidexporttype");
      return parsedRequest;
    }
    parsedRequest.parsedReqMap.put(RestConstantsZ.TYPE, type);

    String email = reqParamsMap.get(RestConstantsZ.EMAIL);
    if (StringUtils.isEmpty(email)) {
      parsedRequest.errMessage = backendMessages.getString("error.invalidemail");
      return parsedRequest;
    }
    if (!StringUtil.isEmailValid(email)) {
      parsedRequest.errMessage =
          "Email address " + email
              + " is not valid. Please provide a correct one, or contact your Administrator to configure your email.";
      return parsedRequest;
    }
    parsedRequest.parsedReqMap.put(RestConstantsZ.EMAIL, reqParamsMap.get(RestConstantsZ.EMAIL));
    return parsedRequest;
  }

  private static ParsedRequest parseOrdersExportFilters(Map<String, String> reqParamsMap) {
    ParsedRequest parsedRequest = new ParsedRequest();
    if (MapUtils.isEmpty(reqParamsMap)) {
      return parsedRequest;
    }
    // Get the order type, if sent
    String otype = reqParamsMap.get(RestConstantsZ.ORDER_TYPE);
    if (StringUtils.isEmpty(otype)) {
      otype = IOrder.TYPE_PURCHASE; // defaulted to purchase orders - Can be sales or purchase
    }
    parsedRequest.parsedReqMap.put(RestConstantsZ.ORDER_TYPE, otype);
    int
        orderType =
        IOrder.NONTRANSFER;
    if (reqParamsMap.get(RestConstantsZ.TYPE).equalsIgnoreCase(TRANSFERS)) {
      orderType = IOrder.TRANSFER;
    }
    // defaulted to non transfer. Can be non transfer or transfer
    String
        spTransfer =
        String.valueOf(
            orderType); // Should be sent from the mobile when transfers are enabled on mobile.
    parsedRequest.parsedReqMap.put("orderType", spTransfer);
    return parsedRequest;
  }

  private static ParsedRequest parseTransactionsExportFilters(
      Map<String, String> reqParamsMap,
      ResourceBundle backendMessages) {
    ParsedRequest parsedRequest = new ParsedRequest();
    if (MapUtils.isEmpty(reqParamsMap)) {
      return parsedRequest;
    }

    String transTypeStr = reqParamsMap.get(RestConstantsZ.TRANSACTION_TYPE);
    if (StringUtils.isNotEmpty(transTypeStr)) {
      if (ITransaction.EXPORT_TYPES.contains(transTypeStr)) {
        parsedRequest.parsedReqMap.put(RestConstantsZ.TRANSACTION_TYPE, transTypeStr);
      } else {
        parsedRequest.errMessage = backendMessages.getString("error.invalidtransactiontype");
        return parsedRequest;
      }
    }
    String materialIdStr = reqParamsMap.get(RestConstantsZ.MATERIAL_ID);
    if (StringUtils.isNotEmpty(materialIdStr)) {
      try {
        Long materialId = Long.parseLong(materialIdStr);
        parsedRequest.parsedReqMap.put(RestConstantsZ.MATERIAL_ID, materialId);
      } catch (NumberFormatException e) {
        parsedRequest.errMessage = backendMessages.getString("error.invalidmaterialid");
        xLogger.severe("Exception while parsing material id. {0}",
            materialIdStr, e);
        return parsedRequest;
      }
    }
    return parsedRequest;
  }

  private static ParsedRequest parseDateFilters(
      Map<String, String> reqParamsMap,
      ResourceBundle backendMessages) {
    ParsedRequest parsedRequest = new ParsedRequest();
    if (MapUtils.isEmpty(reqParamsMap)) {
      return parsedRequest;
    }
    String endDateStr = reqParamsMap.get(RestConstantsZ.ENDDATE);
    Date endDate = new Date();
    if (StringUtils.isNotEmpty(endDateStr)) {
      try {
        endDate = LocalDateUtil.parseCustom(endDateStr, Constants.DATE_FORMAT, null);
        parsedRequest.parsedReqMap.put(RestConstantsZ.ENDDATE, endDate);
      } catch (ParseException pe) {
        parsedRequest.errMessage = backendMessages.getString("error.invalidenddate");
        xLogger.severe("Exception while parsing end date {0}: ",
            endDateStr, pe);
        return parsedRequest;
      }
    } else {
      parsedRequest.parsedReqMap.put(RestConstantsZ.ENDDATE, endDate);
    }
    String startDateStr = reqParamsMap.get(RestConstantsZ.STARTDATE);
    Date startDate = null;
    if (StringUtils.isNotEmpty(startDateStr)) {
      try {
        startDate = LocalDateUtil.parseCustom(startDateStr, Constants.DATE_FORMAT, null);
        parsedRequest.parsedReqMap.put(RestConstantsZ.STARTDATE, startDate);
      } catch (ParseException pe) {
        parsedRequest.errMessage = backendMessages.getString("error.notvalidstartdate");
        xLogger.severe("Exception while parsing start date {0}: ", startDateStr, pe);
        return parsedRequest;
      }
    }
    // If startDate is greater than endDate set the error message
    if (startDate != null && startDate.after(endDate)) {
      parsedRequest.errMessage = backendMessages.getString("error.startdateisgreaterthanenddate");
      return parsedRequest;
    }
    return parsedRequest;
  }

  protected static boolean isConfigModified(Optional<Date> modifiedSinceDate, Long domainId)
      throws ServiceException {
    ConfigurationMgmtService cms = StaticApplicationContext.getBean(
        ConfigurationMgmtService.class);
    IConfig config = cms.getConfiguration(IConfig.CONFIG_PREFIX + domainId);
    return !modifiedSinceDate.isPresent() || !modifiedSinceDate.get()
        .after(config.getLastUpdated());
  }

  private static boolean isExportTypeTransactions(String exportType) {
    return BulkExportMgr.TYPE_TRANSACTIONS
        .equals(exportType);
  }

  private static boolean isExportTypeOrders(String exportType) {
    return BulkExportMgr.TYPE_ORDERS
        .equals(exportType);
  }

  private static boolean isExportTypeTransfers(String exportType) {
    return BulkExportMgr.TYPE_TRANSFERS
        .equals(exportType);
  }
}
