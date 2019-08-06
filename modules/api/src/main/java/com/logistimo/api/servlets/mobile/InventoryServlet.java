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
package com.logistimo.api.servlets.mobile;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import com.logistimo.api.servlets.JsonRestServlet;
import com.logistimo.api.servlets.mobile.builders.MobileTransactionsBuilder;
import com.logistimo.api.servlets.mobile.json.GetInventoryBatchesOutput;
import com.logistimo.api.servlets.mobile.json.JsonOutput;
import com.logistimo.api.util.GsonUtil;
import com.logistimo.api.util.RESTUtil;
import com.logistimo.config.models.DomainConfig;
import com.logistimo.config.models.InventoryConfig;
import com.logistimo.config.models.KioskConfig;
import com.logistimo.config.models.StockboardConfig;
import com.logistimo.constants.Constants;
import com.logistimo.context.StaticApplicationContext;
import com.logistimo.entities.entity.IKiosk;
import com.logistimo.entities.service.EntitiesService;
import com.logistimo.entities.service.EntitiesServiceImpl;
import com.logistimo.exception.InvalidDataException;
import com.logistimo.exception.LogiException;
import com.logistimo.exception.UnauthorizedException;
import com.logistimo.inventory.TransactionUtil;
import com.logistimo.inventory.entity.IInvntry;
import com.logistimo.inventory.entity.IInvntryBatch;
import com.logistimo.inventory.entity.ITransaction;
import com.logistimo.inventory.models.ResponseDetailModel;
import com.logistimo.inventory.service.InventoryManagementService;
import com.logistimo.inventory.service.impl.InventoryManagementServiceImpl;
import com.logistimo.logger.XLog;
import com.logistimo.pagination.PageParams;
import com.logistimo.pagination.Results;
import com.logistimo.proto.JsonTagsZ;
import com.logistimo.proto.MobileUpdateInvTransRequest;
import com.logistimo.proto.MobileUpdateInvTransResponse;
import com.logistimo.proto.RestConstantsZ;
import com.logistimo.proto.UpdateInventoryInput;
import com.logistimo.services.DuplicationException;
import com.logistimo.services.ServiceException;
import com.logistimo.users.entity.IUserAccount;
import com.logistimo.utils.BigUtil;
import com.logistimo.utils.HttpUtil;
import com.logistimo.utils.LocalDateUtil;

import org.apache.commons.lang.StringUtils;

import java.io.IOException;
import java.math.BigDecimal;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Date;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.ResourceBundle;
import java.util.Set;
import java.util.Vector;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;


/**
 * @author arun
 */
@SuppressWarnings("serial")
public class InventoryServlet extends JsonRestServlet {

  private static final XLog xLogger = XLog.getLog(InventoryServlet.class);
  private static final String START = "start";
  // Actions
  private static final String ACTION_GETINVENTORYBATCHES = "gibtchs";

  // Params.
  private static final String MATERIAL_ID = "mid";

  MobileTransactionsBuilder mobileTransactionsBuilder = StaticApplicationContext.getBean(MobileTransactionsBuilder.class);

  // Get the material info. including material-id to stock-on-hand for the update-inventory return object
  @SuppressWarnings({"rawtypes", "unchecked"})
  private static List getStockOnHand(List<ITransaction> list, InventoryManagementService ims,
                                     boolean forceIntegerForStock, EntitiesService as,
                                     Locale locale, String timezone, Long domainId)
      throws ServiceException {
    Vector materials = new Vector();
    List materialList = new ArrayList();
    DomainConfig dc = DomainConfig.getInstance(domainId);
    InventoryConfig ic = dc.getInventoryConfig();
    Iterator<ITransaction> it = list.iterator();
    // Duplicate check for materials
    ArrayList<Long> uniqueIds = new ArrayList<>();
    while (it.hasNext()) {
      ITransaction in = it.next();
      Long materialId = in.getMaterialId();
      Long kioskId = in.getKioskId();
      if (!uniqueIds.contains(
          materialId)) { // if this material is not already processed, then process (there can be two transactions for the same material if it has both an issue and a receipt in the same update request)
        uniqueIds.add(materialId);
        IInvntry inventory = ims.getInventory(kioskId, materialId);
        if (inventory != null) {
          Hashtable ht = new Hashtable();
          ht.put(JsonTagsZ.MATERIAL_ID, materialId.toString());
          String stockStr;
          if (forceIntegerForStock) {
            stockStr = String.valueOf(inventory.getStock().intValue());
          } else {
            stockStr = String.valueOf(inventory.getStock());
          }
          ht.put(JsonTagsZ.QUANTITY, stockStr);
          if (dc.autoGI()) {
            ht.put(JsonTagsZ.ALLOCATED_QUANTITY,
                BigUtil.getFormattedValue(inventory.getAllocatedStock()));
            ht.put(JsonTagsZ.INTRANSIT_QUANTITY,
                BigUtil.getFormattedValue(inventory.getInTransitStock()));
            ht.put(JsonTagsZ.AVAILABLE_QUANTITY,
                BigUtil.getFormattedValue(inventory.getAvailableStock()));
          }

          BigDecimal min = inventory.getReorderLevel();
          BigDecimal max = inventory.getMaxStock();
          if (BigUtil.notEqualsZero(min)) {
            ht.put(JsonTagsZ.MIN, BigUtil.getFormattedValue(min));
          }
          if (BigUtil.notEqualsZero(max)) {
            ht.put(JsonTagsZ.MAX, BigUtil.getFormattedValue(max));
          }
          BigDecimal minDur = inventory.getMinDuration();
          BigDecimal maxDur = inventory.getMaxDuration();
          if (minDur != null && BigUtil.notEqualsZero(minDur)) {
            ht.put(JsonTagsZ.MINDUR, BigUtil.getFormattedValue(minDur));
          }
          if (maxDur != null && BigUtil.notEqualsZero(maxDur)) {
            ht.put(JsonTagsZ.MAXDUR, BigUtil.getFormattedValue(maxDur));
          }
          BigDecimal stockAvailPeriod = ims.getStockAvailabilityPeriod(inventory, dc);
          if (stockAvailPeriod != null && BigUtil.notEqualsZero(stockAvailPeriod)) {
            ht.put(JsonTagsZ.STOCK_DURATION, BigUtil.getFormattedValue(stockAvailPeriod));
          }
          // Consumption rates
          ht = RESTUtil.getConsumptionRate(ic, inventory, ht);
          IKiosk k = as.getKiosk(kioskId, false);
          Vector<Hashtable<String, String>>
              batches =
              RESTUtil.getBatchData(inventory, locale, timezone, ims, k.isBatchMgmtEnabled(),
                  dc.autoGI());
          if (batches != null && !batches.isEmpty()) {
            ht.put(JsonTagsZ.BATCHES, batches);
          }
          Vector<Hashtable<String, String>>
              expiredBatches =
              RESTUtil.getExpiredBatchData(inventory, locale, timezone, ims, k.isBatchMgmtEnabled(),
                  dc.autoGI());
          if (expiredBatches != null && !expiredBatches.isEmpty()) {
            ht.put(JsonTagsZ.EXPIRED_NONZERO_BATCHES, expiredBatches);
          }
          // Add to material list
          materials.add(ht);
          materialList.add(ht);
        }
      }
    }
    return materialList;
  }

  // Get error transactions and corresponding error messages
  @SuppressWarnings({"rawtypes", "unchecked"})
  private static Vector getErrorMessages(List<ITransaction> errors, Locale locale, String timezone,
                                         Long domainId) throws ServiceException {
    if (errors == null) {
      return null;
    }

    Vector<Hashtable> errorMsgs = new Vector<>();
    InventoryManagementService ims =
        StaticApplicationContext.getBean(InventoryManagementServiceImpl.class);
    DomainConfig dc = DomainConfig.getInstance(domainId);
    Hashtable<Long, Hashtable> midMht = new Hashtable<>();
    for (ITransaction inTrans : errors) {
      Long mid = inTrans.getMaterialId();
      Hashtable mht;
      if (inTrans.hasBatch()) {
        Vector batches;
        if (midMht.containsKey(mid)) {
          mht = midMht.get(mid);
          batches = (Vector) mht.get(JsonTagsZ.BATCHES);
        } else {
          mht = new Hashtable();
          batches = new Vector();
        }
        IInvntryBatch
            ib =
            ims.getInventoryBatch(inTrans.getKioskId(), inTrans.getMaterialId(),
                inTrans.getBatchId(), null);
        if (ib != null) {
          Hashtable bht = ib.toMapZ(locale, timezone, dc.autoGI());
          bht.put(JsonTagsZ.MESSAGE, inTrans.getMessage());
          batches.add(bht);
        }
        if (!batches.isEmpty()) {
          mht.put(JsonTagsZ.BATCHES, batches);
        }
      } else {
        mht = new Hashtable();
      }

      if (!mht.containsKey(JsonTagsZ.MATERIAL_ID)) {
        IInvntry i;
        if (inTrans.getType().equals(ITransaction.TYPE_TRANSFER)
            && inTrans.getLinkedKioskId() != null) {
          i = ims.getInventory(inTrans.getLinkedKioskId(), mid);
        } else {
          i = ims.getInventory(inTrans.getKioskId(), mid);
        }
        mht.put(JsonTagsZ.MESSAGE, inTrans.getMessage());
        mht.put(JsonTagsZ.MATERIAL_ID, Long.toString(mid));
        if (i != null) {
          mht.put(JsonTagsZ.QUANTITY, i.getStock().toString());
          if (dc.autoGI()) {
            mht.put(JsonTagsZ.ALLOCATED_QUANTITY, i.getAllocatedStock().toString());
            mht.put(JsonTagsZ.INTRANSIT_QUANTITY, i.getInTransitStock().toString());
            mht.put(JsonTagsZ.AVAILABLE_QUANTITY, i.getAvailableStock().toString());
          }
          mht.put(JsonTagsZ.TIMESTAMP, LocalDateUtil.format(i.getTimestamp(), locale, timezone));
        }
      }
      midMht.put(mid, mht);
    }
    // Iterate through midMht and create a vector and return
    Set<Long> mids = midMht.keySet();
    for (Long mid : mids) {
      errorMsgs.add(midMht.get(mid));
    }

    return errorMsgs;
  }

  public void processGet(HttpServletRequest req, HttpServletResponse resp,
                         ResourceBundle messages)
      throws ServletException, IOException, ServiceException {
    String action = req.getParameter(RestConstantsZ.ACTION);
    if (RestConstantsZ.ACTION_GETINVENTORY.equalsIgnoreCase(action)) {
      getInventory(req, resp, messages);
    } else if (RestConstantsZ.ACTION_EXPORT.equals(action)) {
      scheduleExport(req, resp, messages);
    } else if (ACTION_GETINVENTORYBATCHES.equals(action)) {
      getInventoryBatches(req, resp, messages);
    } else {
      xLogger.severe("Invalid action: " + action);
    }
  }

  // Added by arun, 01/11/09
  public void processPost(HttpServletRequest req, HttpServletResponse resp,
                          ResourceBundle messages)
      throws ServletException, IOException, ServiceException {
    // Get the post parameters
    String action = req.getParameter(RestConstantsZ.ACTION);
    if (RestConstantsZ.ACTION_GETINVENTORY.equalsIgnoreCase(action)) {
      getInventory(req, resp, messages);
    } else if (RestConstantsZ.ACTION_UPDINVENTORY.equalsIgnoreCase(action)) {
      updateInventoryTransactionsv1(req, resp, messages);
    } else if (RestConstantsZ.ACTION_UPDATEINVENTORYTRANSACTIONS.equalsIgnoreCase(action)) {
      updateInventoryTransactions(req, resp, messages);
    } else {
      throw new ServiceException("Invalid action: " + action);
    }
  }

  // Get inventory data
  @SuppressWarnings("unchecked")
  public void getInventory(HttpServletRequest req, HttpServletResponse resp,
                           ResourceBundle backendMessages)
      throws IOException, ServiceException {
    String errMessage = null;
    Long kioskId = null;
    boolean status = true;
    Vector<Hashtable<String, Object>> inventoryList = null;
    String currency = null;
    Locale locale = new Locale(Constants.LANG_DEFAULT, "");
    String timezone = null;
    String appVersion = null;
    Long domainId = null;
    int statusCode = HttpServletResponse.SC_OK;
    // Get request parameters
    String strKioskId = req.getParameter(RestConstantsZ.KIOSK_ID);
    String strUserId = req.getParameter(RestConstantsZ.USER_ID);
    String password = req.getParameter(RestConstantsZ.PASSWRD);
    String sizeStr = req.getParameter(RestConstantsZ.SIZE);
    String filter = req.getParameter(RestConstantsZ.FILTER);
    boolean onlyStock = RestConstantsZ.FILTER_ONLYSTOCK.equals(filter);
    // Get the start date and time
    String startDateStr = req.getParameter(START);
    Date start = null;
    xLogger.fine("startDateStr: " + startDateStr);
    Optional<Date> modifiedSinceDate;
    String offsetStr = req.getParameter(Constants.OFFSET);
    int offset = 0;
    if (StringUtils.isNotBlank(offsetStr)) {
      try {
        offset = Integer.parseInt(offsetStr);
      } catch (Exception e) {
        xLogger.warn("Invalid offset {0}: {1}", offsetStr, e.getMessage());
      }
    }
    PageParams pageParams = null;
    if (sizeStr != null && !sizeStr.isEmpty()) {
      try {
        int size = Integer.parseInt(sizeStr);
        pageParams = new PageParams(offset, size);
      } catch (Exception e) {
        xLogger.warn("Invalid number for size: {0}", sizeStr);
      }
    }
    // Authenticate the user - either with password or via the kioskId/session combination
    try {
      if (strKioskId != null && !strKioskId.isEmpty()) {
        try {
          kioskId = Long.valueOf(strKioskId);
        } catch (NumberFormatException e) {
          xLogger.warn("Invalid kiosk Id {0}: {1}", strKioskId, e.getMessage());
        }
      }
      if (kioskId
          == null) { // kiosk ID is mandatory, and user should have authorization on it (either domain owner, or a operator/manager of it)
        status = false;
        errMessage = backendMessages.getString("error.nokiosk");
      } else {
        // Check if password was sent or not
        boolean hasPublicStockboard = false;
        if (password == null || password.isEmpty()) {
          // Check kisok stock-board configuration
          KioskConfig kioskConfig = KioskConfig.getInstance(kioskId);
          StockboardConfig sbc = (kioskConfig != null ? kioskConfig.getStockboardConfig() : null);
          if (sbc != null && sbc.getEnabled() == StockboardConfig.PUBLIC) {
            hasPublicStockboard = true;
            EntitiesService as = StaticApplicationContext.getBean(EntitiesServiceImpl.class);
            IKiosk k = as.getKiosk(kioskId, false);
            domainId = k.getDomainId();
            DomainConfig dc = DomainConfig.getInstance(domainId);
            currency = k.getCurrency();
            if (currency == null) {
              currency = dc.getCurrency();
            }
            locale = dc.getLocale();
            if (locale == null) {
              locale = new Locale(Constants.LANG_DEFAULT, Constants.COUNTRY_DEFAULT);
            }
            timezone = dc.getTimezone();
            if (timezone == null) {
              timezone = Constants.TIMEZONE_DEFAULT;
            }
          }
        }

        if (!hasPublicStockboard) {
          // Authenticate user
          IUserAccount
              u =
              RESTUtil.authenticate(strUserId, password, kioskId, req,
                  resp); // NOTE: throws ServiceException in case of invalid credentials or no authentication
          // Get the currency from the kiosk
          EntitiesService as = StaticApplicationContext.getBean(EntitiesServiceImpl.class);
          IKiosk k = as.getKiosk(kioskId, false);
          currency = k.getCurrency();
          if (currency == null) {
            DomainConfig dc = DomainConfig.getInstance(k.getDomainId());
            currency = dc.getCurrency();
          }
          // Get user metadata
          locale = u.getLocale();
          timezone = u.getTimezone();
          appVersion = u.getAppVersion();
          domainId = u.getDomainId();
        }
      }
    } catch (ServiceException | NumberFormatException e) {
      errMessage = e.getMessage();
      status = false;
    } catch (UnauthorizedException e) {
      errMessage = e.getMessage();
      status = false;
      statusCode = HttpServletResponse.SC_UNAUTHORIZED;
    }
    int numInv = 0;
    Date lastModified = new Date();
    // Get inventory list
    if (status) {
      modifiedSinceDate = HttpUtil.getModifiedDate(req);
      try {
        // Get domain config
        DomainConfig dc = DomainConfig.getInstance(domainId);
        // FOR BACKWARD COMPATIBILITY: determine whether Integer has to be forced
        boolean forceIntegerForStock = RESTUtil.forceIntegerForStock(appVersion);
        if (startDateStr != null && !startDateStr.isEmpty()) {
          // Convert the start string to a Date format.
          try {
            start = LocalDateUtil.parseCustom(startDateStr, Constants.DATETIME_FORMAT, timezone);
          } catch (ParseException pe) {
            status = false;
            errMessage = backendMessages.getString("error.invalidstartdate");
            xLogger.severe("Exception while parsing start date. Exception: {0}, Message: {1}",
                pe.getClass().getName(), pe.getMessage());
          }
        }
        Results
            results =
            RESTUtil.getInventoryData(kioskId, locale, timezone, onlyStock, dc,
                forceIntegerForStock, start, modifiedSinceDate, pageParams);
        xLogger.fine("results: {0}", results);
        inventoryList = (Vector<Hashtable<String, Object>>) results.getResults();
        numInv = results.getNumFound();
      } catch (ServiceException e) {
        xLogger.severe("InventoryServlet Exception: {0}", e.getMessage());
        status = false;
        errMessage = backendMessages.getString("error.nomaterials");
      }
    }
    try {
      String
          jsonOutput =
          GsonUtil.getInventoryOutputToJson(status, inventoryList, currency, errMessage, numInv,
              RESTUtil.VERSION_01);
      HttpUtil.setLastModifiedHeader(resp, lastModified);
      sendJsonResponse(resp, statusCode, jsonOutput);
    } catch (Exception e1) {
      xLogger.severe("InventoryServlet Exception: {0}", e1.getMessage());
      resp.setStatus(500);
    }
  }

  // Get valid batches of inventory
  @SuppressWarnings("unchecked")
  public void getInventoryBatches(HttpServletRequest req, HttpServletResponse resp,
                                  ResourceBundle backendMessages)
      throws IOException, ServiceException {
    String errMessage = null;
    Long kioskId = null;
    Long materialId = null;
    boolean status = true;
    int statusCode = HttpServletResponse.SC_OK;
    // Get request parameters
    String strKioskId = req.getParameter(RestConstantsZ.KIOSK_ID);
    String strMaterialId = req.getParameter(MATERIAL_ID);
    String strUserId = req.getParameter(RestConstantsZ.USER_ID);
    String password = req.getParameter(RestConstantsZ.PASSWRD);
    String sizeStr = req.getParameter(RestConstantsZ.SIZE);
    String offsetStr = req.getParameter(Constants.OFFSET);
    int offset = 0;
    if (StringUtils.isNotBlank(offsetStr)) {
      try {
        offset = Integer.parseInt(offsetStr);
      } catch (Exception e) {
        xLogger.warn("Invalid offset {0}: {1}", offsetStr, e.getMessage());
      }
    }
    PageParams pageParams = null;
    if (sizeStr != null && !sizeStr.isEmpty()) {
      try {
        int size = Integer.parseInt(sizeStr);
        pageParams = new PageParams(offset, size);
      } catch (Exception e) {
        xLogger.warn("Invalid number for size: {0}", sizeStr);
      }
    }
    // Authenticate the user - either with password or via the kioskId/session combination
    try {
      if (strKioskId != null && !strKioskId.isEmpty()) {
        try {
          kioskId = Long.valueOf(strKioskId);
        } catch (NumberFormatException e) {
          xLogger.warn("Invalid kiosk Id {0}: {1}", strKioskId, e.getMessage());
        }
      }
      if (strMaterialId != null && !strMaterialId.isEmpty()) {
        try {
          materialId = Long.valueOf(strMaterialId);
        } catch (Exception e) {
          xLogger.warn("{0} when converting material Id {1} to long: {2}", e.getClass().getName(),
              strMaterialId, e.getMessage());
        }
      }
      if (kioskId
          == null) { // kiosk ID is mandatory, and user should have authorization on it (either domain owner, or a operator/manager of it)
        status = false;
        errMessage = backendMessages.getString("error.nokiosk");
      } else if (materialId == null) {
        status = false;
        errMessage = backendMessages.getString("error.nomaterials");
      } else {
        // Authenticate user (throws exception, if not authenticated)
        RESTUtil.authenticate(strUserId, password, kioskId, req,
            resp); // NOTE: throws ServiceException in case of invalid credentials or no authentication
      }
    } catch (ServiceException | NumberFormatException e) {
      errMessage = e.getMessage();
      status = false;
    } catch (UnauthorizedException e) {
      errMessage = e.getMessage();
      status = false;
      statusCode = HttpServletResponse.SC_UNAUTHORIZED;
    }
    // Get inventory list
    List<IInvntryBatch> batches = null;
    if (status) {
      try {
        // Get the batches
        InventoryManagementService ims =
            StaticApplicationContext.getBean(InventoryManagementServiceImpl.class);
        Results<IInvntryBatch> results = ims.getValidBatches(materialId, kioskId, pageParams);
        batches = results.getResults();
        if (batches == null) {
          batches = new ArrayList<>(); // empty list
        }
        // TODO: change time to locale times?
      } catch (ServiceException e) {
        xLogger.severe("InventoryServlet Exception: {0}", e.getMessage());
        status = false;
        errMessage = backendMessages.getString("error.nomaterials");
      }
    }
    // Send the response
    try {
      // Get the json return object
      GetInventoryBatchesOutput
          gibo =
          new GetInventoryBatchesOutput(JsonOutput.VERSION_DEFAULT, status, errMessage,
              strMaterialId, batches);
      Gson
          gson =
          new GsonBuilder().setDateFormat(Constants.DATE_FORMAT)
              .excludeFieldsWithoutExposeAnnotation().create();
      sendJsonResponse(resp, statusCode, gson.toJson(gibo));
    } catch (Exception e1) {
      xLogger.severe("InventoryServlet Exception: {0}", e1.getMessage());
      resp.setStatus(500);
    }
  }

  @SuppressWarnings("rawtypes")
  public void updateInventoryTransactionsv1(HttpServletRequest req, HttpServletResponse resp,
                                            ResourceBundle backendMessages)
      throws IOException {
    boolean status = true;
    String message = null;
    List materialList = new ArrayList();
    Vector errors = null; // map of material-id and error message
    InventoryManagementService ims = null;
    EntitiesService as = null;
    String trackingIdStr = null;
    String formattedTime = null;
    Locale locale = null;
    String timezone = null;
    String appVersion = null;
    Long domainId = null;
    int statusCode = HttpServletResponse.SC_OK;
    // Get request parameters
    String jsonString = req.getParameter(RestConstantsZ.JSON_STRING);
    String transType = req.getParameter(RestConstantsZ.TRANS_TYPE);
    String password = req.getParameter(RestConstantsZ.PASSWRD); // sent in case of SMS message
    boolean isReverse = (req.getParameter("reverse") != null);
    String
        domainIdStr =
        req.getParameter("domainid"); // used ONLY if present and if is Reverse transaction
    // Get timestamp
    Date now = new Date();
    try {
      if (jsonString == null || jsonString.isEmpty()) {
        status = false;
        message = "Invalid input parameters.";
      } else {
        // Parse the JSON input array, and get the list of transaction objects (issued and/or received)
        UpdateInventoryInput updInventoryJson;
        updInventoryJson = GsonUtil.updateInventoryInputFromJson(jsonString);

        // Get the user Id
        String userId = updInventoryJson.getUserId();
        // Get the kiosk Id
        String kioskIdStr = updInventoryJson.getKioskId();
        Long kioskId = null;
        if (kioskIdStr != null && !kioskIdStr.isEmpty()) {
          try {
            kioskId = Long.valueOf(kioskIdStr);
          } catch (NumberFormatException e) {
            xLogger.warn("Invalid kiosk Id {0}: {1}", kioskIdStr, e.getMessage());
          }
        }
        if (kioskId
            == null) { // kiosk ID is mandatory, and user should have authorization on it (either domain owner, or a operator/manager of it)
          status = false;
          message = backendMessages.getString("error.nokiosk");
        } else {
          // Authenticate user, if needed
          try {
            IUserAccount u = RESTUtil.authenticate(userId, password, kioskId, req, resp);
            domainId = u.getDomainId();
            if (isReverse && domainIdStr != null && !domainIdStr.isEmpty()) {
              domainId = Long.valueOf(domainIdStr);
            }
            locale = u.getLocale();
            timezone = u.getTimezone();
            appVersion = u.getAppVersion();
            // Get services
            ims = StaticApplicationContext.getBean(InventoryManagementServiceImpl.class);
            as = StaticApplicationContext.getBean(EntitiesServiceImpl.class);
          } catch (ServiceException e) {
            message = e.getMessage();
            status = false;
            xLogger.warn("Authentication failed: {0} for user {1}", message, userId);
          }
        }
        if (status) { // login is valid
          boolean
              isDuplicateUpdate =
              false; // whether an update is a duplication of an earlier update
          // Get tracking Id, if any
          trackingIdStr = updInventoryJson.getTrackingId();
          // Get macro-message, if any
          message = "";
          // Get update transactions
          List<ITransaction>
              list =
              RESTUtil.getInventoryTransactions(updInventoryJson, transType, now);
          // Update the inventory transactions
          List<ITransaction> errorTrans = null;
          if (list == null) {
            status = false;
            message = backendMessages.getString("error.unabletoupdatetrans");
          } else {
            // Update stock transactions
            try {
              if (updInventoryJson.getTimestampSaveMillis() == null) {
                errorTrans = ims.updateInventoryTransactions(domainId, list).getErrorTransactions();
              } else {
                if (TransactionUtil
                    .deduplicateBySaveTimePartial(updInventoryJson.getTimestampSaveMillis(),
                        updInventoryJson.getUserId(), updInventoryJson.getKioskId(),
                        updInventoryJson.getPartId())) {
                  xLogger.warn("DuplicationException when updating inventory transactions");
                  isDuplicateUpdate = true;
                } else {
                  errorTrans = ims.updateInventoryTransactions(domainId, list, true).getErrorTransactions();
                }
              }
            } catch (ServiceException e) {
              xLogger.severe("Unable to update inventory transactions: {0}", e.getMessage());
              status = false;
              message = backendMessages.getString("error.unabletoupdatetrans");
            } catch (DuplicationException e) {
              xLogger.warn("DuplicationException when updating inventory transactions: {0}",
                  e.getMessage());
              isDuplicateUpdate = true;
              // Act as though the transaction has succeeded and send back the same message as for a successful invne
            } catch (InvalidDataException ide) {
              xLogger.warn("Unable to update inventory transactions: {0}", ide.getMessage());
              status = false;
              message = backendMessages.getString("error.materialindestinationkioskdoesnotexist");
            }
          }
          if (status) {
            // FOR BACKWARD COMPATIBILITY: check whether integer stock has to be sent (for mobile app versions 1.2.0 onwards, float stock is sent)
            boolean forceIntegerForStock = RESTUtil.forceIntegerForStock(appVersion);
            // Get the material id and current stock on hand for the set of materials (with local format timestamp)
            materialList =
                getStockOnHand(list, ims, forceIntegerForStock, as, locale, timezone, domainId);
            // Get the error hash tables
            if (!isDuplicateUpdate) {
              errors = getErrorMessages(errorTrans, locale, timezone, domainId);
              status = !(errors != null && !errors.isEmpty());
            }
            // Get formatted time
            formattedTime = LocalDateUtil.format(now, locale, timezone);
          }
        }
      }
    } catch (ServiceException e2) {
      xLogger.severe("ServiceException: {0}", e2.getMessage());
      message =
          backendMessages.getString("error.unabletoupdatetrans")
              + " [2]"; // [2] is just a marker for this exception
      status = false;
    } catch (NumberFormatException e) {
      xLogger.warn("Invalid number passed during inventory update: {0}", e.getMessage());
      message =
          backendMessages.getString("error.systemerror")
              + " [1]"; // [1] is a marker for this exception
      status = false;
    } catch (InvalidDataException ide) {
      xLogger.warn(backendMessages.getString("error.transferfailed") + " " + backendMessages
          .getString("affectedmaterials") + ": " + ide.getMessage());
      status = false;
      message =
          backendMessages.getString("error.transferfailed") + " " + backendMessages
              .getString("affectedmaterials") + ": " + ide.getMessage();
    } catch (UnauthorizedException e) {
      message = e.getMessage();
      status = false;
      statusCode = HttpServletResponse.SC_UNAUTHORIZED;
    } catch (Exception e2) {
      xLogger.severe("Exception: {0} : {1}", e2.getClass().getName(), e2.getMessage(), e2);
      message = backendMessages.getString("error.systemerror");
      status = false;
    }
    String localeStr = Constants.LANG_DEFAULT;
    if (locale != null) {
      localeStr = locale.toString();
    }
    try {
      // Get the JSON return object
      String
          respStr =
          GsonUtil.updateInventoryOutputToJson(status, message, materialList, errors, formattedTime,
              trackingIdStr, localeStr, RESTUtil.VERSION_01);
      // Log quantity related errors, if any
      if (errors != null && !errors.isEmpty()) {
        xLogger.warn(
            "Some errors were encountered when updating inventory in domain {0} for trans-type of {1}: {2}",
            domainId, transType, respStr);
      }
      // Send response
      sendJsonResponse(resp, statusCode, respStr);
    } catch (Exception e2) {
      xLogger.severe("InventoryServlet Protocol Exception: {0}", e2.getMessage());
      resp.setStatus(500);
    }
  }

  // Schedule export of inventory data
  private void scheduleExport(HttpServletRequest req, HttpServletResponse resp,
                              ResourceBundle backendMessages) {
    xLogger.fine("Entered scheduleExport");
    int statusCode = HttpServletResponse.SC_OK;
    // Send response back to client
    try {
      String respStr = RESTUtil.scheduleKioskDataExport(req, backendMessages, resp);
      // Send response
      if (respStr.contains("Invalid token")) {
        statusCode = HttpServletResponse.SC_UNAUTHORIZED;
      }
      sendJsonResponse(resp, statusCode, respStr);
    } catch (Exception e2) {
      xLogger.severe("InventoryServlet Protocol Exception: {0}", e2.getMessage(), e2);
      resp.setStatus(500);
    }
    xLogger.fine("Exiting scheduleExport");
  }

  /**
   * Updates inventories from the list of transactions sent from the mobile.
   * @param req - Request object containing the json with the mobile inventory transactions
   * @param resp
   * @throws IOException
   */
  public void updateInventoryTransactions(HttpServletRequest req, HttpServletResponse resp, ResourceBundle backendMessages) throws IOException {
    String reqJsonStr = req.getParameter(RestConstantsZ.JSON_STRING);
    MobileUpdateInvTransRequest mobUpdateInvTransReq = null;
    int statusCode = HttpServletResponse.SC_OK;
    Long domainId = null;
    String errorMessage = null;
    Map<Long,ResponseDetailModel> midResponseDetailModelMap = null;
    boolean isDuplicate = false;
    try {
      if (StringUtils.isEmpty(reqJsonStr)) {
        throw new InvalidDataException(backendMessages.getString("error.invaliddata.frommobile"));
      }
      mobUpdateInvTransReq = mobileTransactionsBuilder.buildMobileUpdateInvTransRequest(reqJsonStr);
      validateMobileUpdateInvTransRequest(mobUpdateInvTransReq, backendMessages);
      IUserAccount u = RESTUtil.authenticate(mobUpdateInvTransReq.uid, null, mobUpdateInvTransReq.kid, req, resp);
      domainId = u.getDomainId();
      // Deduplicate by transaction send time
      if (TransactionUtil
          .deduplicateBySaveTimePartial(String.valueOf(mobUpdateInvTransReq.sntm),
              mobUpdateInvTransReq.uid, String.valueOf(mobUpdateInvTransReq.kid),
              mobUpdateInvTransReq.pid)) {
        isDuplicate = true;
      }
      if (!isDuplicate) {
        Map<Long,List<ITransaction>> materialTransactionsMap = mobileTransactionsBuilder.buildMaterialTransactionsMap(mobUpdateInvTransReq.uid, mobUpdateInvTransReq.kid,
            mobUpdateInvTransReq.trns);
        if (materialTransactionsMap == null || materialTransactionsMap.isEmpty()) {
          throw new InvalidDataException(backendMessages.getString("error.invaliddata.frommobile"));
        }
        InventoryManagementService ims =
            StaticApplicationContext.getBean(InventoryManagementServiceImpl.class);
        midResponseDetailModelMap = ims.updateMultipleInventoryTransactions(materialTransactionsMap, domainId,
            mobUpdateInvTransReq.uid);
      } else {
        Integer status = TransactionUtil.getObjectFromCache(String.valueOf(mobUpdateInvTransReq.sntm),
            mobUpdateInvTransReq.uid, mobUpdateInvTransReq.kid,
            mobUpdateInvTransReq.pid);
        if (status != null && TransactionUtil.IN_PROGRESS == status) {
            throw new LogiException(
                backendMessages.getString("transactions.processing.inprogress"));
        }
      }
    } catch (UnauthorizedException ue) {
      xLogger.warn("Exception when updating inventory transactions {0}", ue);
      statusCode = HttpServletResponse.SC_UNAUTHORIZED;
      errorMessage = ue.getMessage();
    } catch (InvalidDataException e) {
      xLogger.warn("Exception when updating inventory transactions {0}", e);
      statusCode = HttpServletResponse.SC_BAD_REQUEST;
      errorMessage = e.getMessage();
    } catch (Exception e) {
      xLogger.warn("Exception when updating inventory transactions {0}", e);
      statusCode = HttpServletResponse.SC_INTERNAL_SERVER_ERROR;
      errorMessage = backendMessages.getString("error.systemerror");
    }
    try {
      String
          mobUpdateInvTransRespJsonStr = null;
      if (statusCode != HttpServletResponse.SC_BAD_REQUEST && statusCode != HttpServletResponse.SC_UNAUTHORIZED) {
        mobUpdateInvTransRespJsonStr = createMobUpdateInvTransRespJsonStr(mobUpdateInvTransReq, errorMessage, midResponseDetailModelMap, domainId, isDuplicate);
      }
      if (mobUpdateInvTransRespJsonStr != null) {
        sendJsonResponse(resp, statusCode, mobUpdateInvTransRespJsonStr);
      } else {
        sendJsonResponse(resp, statusCode, errorMessage);
      }
    } catch (Exception e) {
      xLogger.severe("Exception when sending update inventory transactions response", e);
      resp.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
    }
  }

  private void validateMobileUpdateInvTransRequest(MobileUpdateInvTransRequest mobUpdateInvTransReq,
                                                   ResourceBundle backendMessages) {
    if (StringUtils.isEmpty(mobUpdateInvTransReq.uid) || mobUpdateInvTransReq.kid == null || mobUpdateInvTransReq.sntm == null || mobUpdateInvTransReq.trns == null || mobUpdateInvTransReq.trns.isEmpty()) {
      throw new InvalidDataException(backendMessages.getString("error.invaliddata.frommobile"));
    }
  }

  private String createMobUpdateInvTransRespJsonStr(MobileUpdateInvTransRequest mobUpdateInvTransReq, String errorMessage, Map<Long,ResponseDetailModel> midResponseDetailModelMap, Long domainId, boolean isDuplicate) {
    MobileUpdateInvTransResponse
        mobUpdateInvTransResp =
        mobileTransactionsBuilder.buildMobileUpdateInvTransResponse(
            domainId, mobUpdateInvTransReq.uid, mobUpdateInvTransReq.kid,
            mobUpdateInvTransReq.pid, errorMessage, midResponseDetailModelMap,
            mobUpdateInvTransReq.trns, isDuplicate);
    String mobUpdateInvTransRespJsonStr = new Gson().toJson(mobUpdateInvTransResp);
    if (!isDuplicate && mobUpdateInvTransResp != null) {
      TransactionUtil.setObjectInCache(String.valueOf(mobUpdateInvTransReq.sntm),
          mobUpdateInvTransReq.uid, mobUpdateInvTransReq.kid,
          mobUpdateInvTransReq.pid,TransactionUtil.COMPLETED);
    }
    return mobUpdateInvTransRespJsonStr;
  }
}
