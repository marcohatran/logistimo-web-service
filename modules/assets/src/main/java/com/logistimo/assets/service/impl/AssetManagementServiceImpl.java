/*
 * Copyright © 2017 Logistimo.
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

package com.logistimo.assets.service.impl;

import com.google.common.base.Preconditions;

import com.logistimo.assets.AssetUtil;
import com.logistimo.assets.entity.IAsset;
import com.logistimo.assets.entity.IAssetRelation;
import com.logistimo.assets.entity.IAssetStatus;
import com.logistimo.assets.models.AssetModel;
import com.logistimo.assets.models.AssetModels;
import com.logistimo.assets.service.AssetManagementService;
import com.logistimo.auth.utils.SecurityUtils;
import com.logistimo.config.models.AssetSystemConfig;
import com.logistimo.config.models.ConfigurationException;
import com.logistimo.config.models.DomainConfig;
import com.logistimo.config.models.EventSpec;
import com.logistimo.config.models.EventsConfig;
import com.logistimo.constants.CharacterConstants;
import com.logistimo.constants.Constants;
import com.logistimo.dao.JDOUtils;
import com.logistimo.domains.utils.DomainsUtil;
import com.logistimo.events.entity.IEvent;
import com.logistimo.events.processor.EventPublisher;
import com.logistimo.logger.XLog;
import com.logistimo.pagination.PageParams;
import com.logistimo.pagination.Results;
import com.logistimo.services.Resources;
import com.logistimo.services.ServiceException;
import com.logistimo.services.impl.PMF;
import com.logistimo.utils.QueryUtil;
import com.sun.rowset.CachedRowSetImpl;

import org.apache.commons.lang.StringUtils;
import org.springframework.stereotype.Service;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.ResourceBundle;

import javax.jdo.JDOObjectNotFoundException;
import javax.jdo.PersistenceManager;
import javax.jdo.Query;
import javax.jdo.Transaction;
import javax.jdo.datastore.JDOConnection;
import javax.sql.rowset.CachedRowSet;

/**
 * Created by kaniyarasu on 02/11/15.
 */

@Service
public class AssetManagementServiceImpl implements AssetManagementService {

  private static final XLog xLogger = XLog.getLog(AssetManagementServiceImpl.class);

  @Override
  public void createAsset(Long domainId, IAsset asset, final AssetModel assetModel)
      throws ServiceException {
    PersistenceManager pm = PMF.get().getPersistenceManager();
    Transaction tx = null;
    try {
      tx = pm.currentTransaction();
      tx.begin();
      createAsset(domainId, asset, pm);
      AssetUtil.registerOrUpdateDevice(assetModel, domainId);
      tx.commit();
      asset = pm.detachCopy(asset);
      try {
        EventPublisher
            .generate(domainId, IEvent.CREATED, null, JDOUtils.getImplClassName(IAsset.class),
                String.valueOf(asset.getId()), null);
      } catch (Exception e) {
        xLogger.warn("Exception when generating event for creating asset {0} in domain {1}",
            asset.getSerialId(), domainId, e);
        throw new ServiceException(e);
      }

    } catch (IllegalArgumentException e) {
      throw e;
    } catch (Exception e) {
      throw new ServiceException(e);
    } finally {
      if (tx.isActive()) {
        tx.rollback();
      }
      pm.close();
    }
  }

  @SuppressWarnings("unchecked")
  private Long createAsset(Long domainId, IAsset asset, PersistenceManager pm)
      throws ServiceException {
    if (domainId == null || asset == null
        || asset.getSerialId() == null || asset.getVendorId() == null) {
      throw new ServiceException("Invalid details for the asset");
    }
    try {
      Query query = pm.newQuery(JDOUtils.getImplClass(IAsset.class));
      query.setFilter("vId == vendorIdParam && nsId == serialIdParam");
      query.declareParameters("String vendorIdParam, String serialIdParam");
      List<IAsset> assets = null;
      try {
        assets =
            (List<IAsset>) query.execute(asset.getVendorId(), asset.getSerialId().toLowerCase());
        assets = (List<IAsset>) pm.detachCopyAll(assets);
      } finally {
        query.closeAll();
      }

      if (assets == null || assets.size() == 0) {
        Date now = new Date();
        asset.setCreatedOn(now);
        asset.setUpdatedOn(now);
        asset.setDomainId(domainId);
        asset = (IAsset) DomainsUtil.addToDomain(asset, domainId, pm);
      } else {
        final Locale locale = SecurityUtils.getLocale();
        ResourceBundle backendMessages = Resources.get().getBundle(Constants.BACKEND_MESSAGES, locale);
        throw new ServiceException(
            asset.getSerialId() + "(" + asset.getVendorId() + ") " + backendMessages
                .getString("error.alreadyexists"));
      }
    } catch (ServiceException e) {
      xLogger.warn("{0} while creating asset {1}, {2} for the domain {4}", e.getMessage(),
          asset.getSerialId(), asset.getVendorId(), domainId, e);
      throw new ServiceException(e.getMessage());
    } catch (IllegalArgumentException e) {
      throw e;
    } catch (Exception e) {
      xLogger.severe("{0} while creating asset {1}, {2} for the domain {4}", e.getMessage(),
          asset.getSerialId(), asset.getVendorId(), domainId, e);
      throw new ServiceException(e.getMessage());
    }

    return asset.getId();
  }

  @Override
  public IAsset getAsset(Long assetId) throws ServiceException {
    PersistenceManager pm = PMF.get().getPersistenceManager();
    IAsset asset = null;
    String errMsg = null;
    try {
      asset = JDOUtils.getObjectById(IAsset.class, assetId, pm);
      asset = pm.detachCopy(asset);
    } catch (JDOObjectNotFoundException e) {
      xLogger.warn("getAsset: Asset {0} does not exist", assetId);
      final Locale locale = SecurityUtils.getLocale();
      ResourceBundle messages = Resources.get().getBundle(Constants.MESSAGES, locale);
      ResourceBundle backendMessages = Resources.get().getBundle(Constants.BACKEND_MESSAGES, locale);
      errMsg =
          messages.getString("asset") + " " + assetId + " " + backendMessages
              .getString("error.notfound");
    } catch (Exception e) {
      errMsg = e.getMessage();
    } finally {
      pm.close();
    }
    if (errMsg != null) {
      throw new ServiceException(errMsg);
    }
    return asset;
  }

  @Override
  public void updateAsset(Long domainId, IAsset asset, AssetModel assetModel)
      throws ServiceException {
    PersistenceManager pm = PMF.get().getPersistenceManager();
    Transaction tx = null;
    try {
      tx = pm.currentTransaction();
      tx.begin();
      DomainsUtil.addToDomain(asset, domainId, pm);
      AssetUtil.registerOrUpdateDevice(assetModel, domainId);
      tx.commit();
      asset = pm.detachCopy(asset);
      try {
        if (assetModel.ws == null) {
          EventPublisher.generate(domainId, IEvent.MODIFIED, null,
              JDOUtils.getImplClassName(IAsset.class), String.valueOf(asset.getId()), null);
        }
      } catch (Exception e) {
        xLogger.warn("Exception when generating event for updating asset {0} in domain {1}",
            asset.getSerialId(), domainId, e);
        throw new ServiceException(e);
      }
    } finally {
      if (tx != null) {
        if (tx.isActive()) {
          tx.rollback();
        }
      }
      pm.close();
    }
  }

  @Override
  public Results getAssetsByDomain(Long domainId, Integer assetType, PageParams pageParams)
      throws ServiceException {
    if (domainId == null) {
      throw new ServiceException("Domain id is not provided");
    }

    List<IAsset> assets = null;
    int numFound = 0;
    String cursor = null;
    PersistenceManager pm = PMF.get().getPersistenceManager();
    try {
      Query query = pm.newQuery(JDOUtils.getImplClass(IAsset.class));
      if (assetType != null && assetType != 0) {
        query.setFilter("dId.contains(domainIdParam) && type == assetTypeParam");
        query.declareParameters("Long domainIdParam, Integer assetTypeParam");
      } else {
        query.setFilter("dId.contains(domainIdParam)");
        query.declareParameters("Long domainIdParam");
      }
      query.setOrdering("sId asc");
      if (pageParams != null) {
        QueryUtil.setPageParams(query, pageParams);
      }

      try {
        if (assetType != null && assetType != 0) {
          assets = (List<IAsset>) query.execute(domainId, assetType);
        } else {
          assets = (List<IAsset>) query.execute(domainId);

        }
        cursor = QueryUtil.getCursor(assets);
        assets = (List<IAsset>) pm.detachCopyAll(assets);

        //Count query
        StringBuilder sqlQuery = new StringBuilder(
            "SELECT COUNT(1) FROM ASSET WHERE ID in (SELECT ID_OID from ASSET_DOMAINS where DOMAIN_ID = ?)");
        List<Object> params = new ArrayList<>();
        params.add(domainId);
        if (assetType != null && assetType != 0) {
          sqlQuery.append(" and TYPE = ?");
          params.add(assetType);
        }

        Query cntQuery = pm.newQuery(Constants.JAVAX_JDO_QUERY_SQL, sqlQuery.toString());
        numFound = ((Long) ((List) cntQuery.executeWithArray(params.toArray())).iterator().next()).intValue();
        cntQuery.closeAll();
      } finally {
        query.closeAll();
      }
    } catch (Exception e) {
      xLogger.warn("{0} while getting assets for the domain {1}", e.getMessage(), domainId, e);
      throw new ServiceException(e.getMessage());
    } finally {
      pm.close();
    }

    return new Results(assets, cursor, numFound, pageParams == null ? 0 : pageParams.getOffset());
  }

  @Override
  public List<IAsset> getAssetsByKiosk(Long kioskId) throws ServiceException {
    if (kioskId == null) {
      throw new ServiceException("");//TODO
    }

    List<IAsset> assets = null;
    PersistenceManager pm = PMF.get().getPersistenceManager();
    try {
      Query query = pm.newQuery(JDOUtils.getImplClass(IAsset.class));
      query.setFilter("kId == kioskIdParam");
      query.declareParameters("Long kioskIdParam");
      try {
        assets = (List<IAsset>) query.execute(kioskId);
        assets = (List<IAsset>) pm.detachCopyAll(assets);
      } finally {
        query.closeAll();
      }
    } catch (Exception e) {
      xLogger.severe("{0} while getting assets for the kiosk {1}", e.getMessage(), kioskId, e);
      throw new ServiceException(e.getMessage());
    } finally {
      pm.close();
    }

    return assets;
  }

  @Override
  public List<IAsset> getAssetsByKiosk(Long kioskId, Integer assetType) throws ServiceException {
    if (kioskId == null || assetType == null) {
      final Locale locale = SecurityUtils.getLocale();
      ResourceBundle backendMessages = Resources.get().getBundle(Constants.BACKEND_MESSAGES, locale);
      throw new ServiceException(
          backendMessages.getString("kiosk") + " and AssetType are mandatory");
    }
    List<IAsset> assets = null;
    PersistenceManager pm = PMF.get().getPersistenceManager();
    try {
      Query query = pm.newQuery(JDOUtils.getImplClass(IAsset.class));
      query.setFilter("kId == kioskIdParam  && type == assetTypeParam");
      query.declareParameters("Long kioskIdParam, Integer assetTypeParam");
      try {
        assets = (List<IAsset>) query.execute(kioskId, assetType);
        assets = (List<IAsset>) pm.detachCopyAll(assets);
      } finally {
        query.closeAll();
      }
    } catch (Exception e) {
      xLogger.severe("{0} while getting assets for the kiosk {1}", e.getMessage(), kioskId, e);
      throw new ServiceException(e.getMessage());
    } finally {
      pm.close();
    }

    return assets;
  }

  @Override
  public IAsset getAsset(String manufacturerId, String assetId) throws ServiceException {
    if (assetId == null || manufacturerId == null) {
      throw new ServiceException("");//TODO
    }

    PersistenceManager pm = PMF.get().getPersistenceManager();
    try {
      Query query = pm.newQuery(JDOUtils.getImplClass(IAsset.class));
      query.setFilter("vId == vendorIdParam && sId == serialIdParam");
      query.declareParameters("String vendorIdParam, String serialIdParam");
      List<IAsset> assets;
      try {
        assets = (List<IAsset>) query.execute(manufacturerId, assetId);
        assets = (List<IAsset>) pm.detachCopyAll(assets);

        if (assets != null && assets.size() == 1) {
          return assets.get(0);
        }
      } finally {
        query.closeAll();
      }
    } catch (JDOObjectNotFoundException e) {
      xLogger.warn("{0} while getting asset {1}, {2}", e.getMessage(), assetId, manufacturerId, e);
      throw new ServiceException(e.getMessage());
    } catch (Exception e) {
      xLogger
          .severe("{0} while getting asset {1}, {2}", e.getMessage(), assetId, manufacturerId, e);
      throw new ServiceException(e.getMessage());
    } finally {
      pm.close();
    }

    return null;
  }


  private IAssetStatus getAssetStatus(Long assetId, Integer mpId, String sId, Integer type,
      PersistenceManager pm) throws ServiceException {
    Preconditions.checkArgument(!(assetId == null || (mpId == null && sId == null) || type == null),
        "Illegal argument expected assetId, mpId or sId and type");
    Long id = JDOUtils.createAssetStatusKey(assetId, mpId != null ? mpId : 0, sId, type);
    try {
      return JDOUtils.getObjectById(IAssetStatus.class, id, pm);
    } catch (Exception e) {
      //No such object
      return null;
    }
  }

  @Override
  public void updateAssetStatus(List<IAssetStatus> assetStatusModelList) throws ServiceException {
    PersistenceManager pm = PMF.get().getPersistenceManager();
    List<IAssetStatus> updated = new ArrayList<>();
    try {
      for (IAssetStatus assetStatusModel : assetStatusModelList) {
        boolean firstWorkingStateChange = false;
        try {
          IAssetStatus assetStatus =
              getAssetStatus(assetStatusModel.getAssetId(), assetStatusModel.getMpId(),
                  assetStatusModel.getsId(), assetStatusModel.getType(), pm);
          if (assetStatus == null) {
            pm.makePersistent(assetStatusModel);
            assetStatusModel = pm.detachCopy(assetStatusModel);
            if (IAssetStatus.TYPE_STATE.equals(assetStatusModel.getType())) {
              firstWorkingStateChange = true;
            }
          } else {
            if (Objects.equals(assetStatusModel.getStatus(), assetStatus.getStatus())) {
              continue;
            }
            assetStatus.setStatus(assetStatusModel.getStatus());
            assetStatus.setTmp(assetStatusModel.getTmp());
            assetStatus.setTs(assetStatusModel.getTs());
            assetStatus.setAbnStatus(assetStatusModel.getAbnStatus());
            assetStatus.setAttributes(assetStatusModel.getAttributes());
          }
          if (!firstWorkingStateChange) {
            updated.add(assetStatusModel);
          }
        } catch (Exception e) {
          xLogger.severe("Error while persisting asset status for: {0}", assetStatusModel, e);
        }
      }
    } finally {
      pm.close();
    }
    try {
      AssetSystemConfig asc = AssetSystemConfig.getInstance();
      for (IAssetStatus assetStatus : updated) {
        IAsset asset = getAsset(assetStatus.getAssetId());
        AssetSystemConfig.Asset assetData = asc.assets.get(asset.getType());
        Integer assetType = assetData.type;
        int eventType = -1;
        if (assetType == IAsset.MONITORED_ASSET) {
          if (IAssetStatus.TYPE_TEMPERATURE.equals(assetStatus.getType())) {
            //Event notification
            if (IAsset.ABNORMAL_TYPE_HIGH == assetStatus.getAbnStatus()) {
              if (assetStatus.getStatus() == IAsset.STATUS_EXCURSION) {
                eventType = IEvent.HIGH_EXCURSION;
              } else if (assetStatus.getStatus() == IAsset.STATUS_WARNING) {
                eventType = IEvent.HIGH_WARNING;
              } else if (assetStatus.getStatus() == IAsset.STATUS_ALARM) {
                eventType = IEvent.HIGH_ALARM;
              }
            } else if (IAsset.ABNORMAL_TYPE_LOW == assetStatus.getAbnStatus()) {
              if (assetStatus.getStatus() == IAsset.STATUS_EXCURSION) {
                eventType = IEvent.LOW_EXCURSION;
              } else if (assetStatus.getStatus() == IAsset.STATUS_WARNING) {
                eventType = IEvent.LOW_WARNING;
              } else if (assetStatus.getStatus() == IAsset.STATUS_ALARM) {
                eventType = IEvent.LOW_ALARM;
              }
            } else if (assetStatus.getStatus() == IAsset.STATUS_NORMAL) {
              eventType = IEvent.INCURSION;
            }
          } else if (IAssetStatus.TYPE_STATE.equals(assetStatus.getType())
              && assetStatus.getMpId() == 0) {
            eventType = IEvent.STATUS_CHANGE;
          }
        } else if (assetType == IAsset.MONITORING_ASSET) {
          if (IAssetStatus.TYPE_BATTERY.equals(assetStatus.getType())) {
            if (IAsset.STATUS_BATTERY_LOW == assetStatus.getStatus()) {
              eventType = IEvent.BATTERY_LOW;
            } else if (IAsset.STATUS_BATTERY_ALARM == assetStatus.getStatus()) {
              eventType = IEvent.BATTERY_ALARM;
            } else if (IAsset.STATUS_NORMAL == assetStatus.getStatus()) {
              eventType = IEvent.BATTERY_NORMAL;
            }
          } else if (IAssetStatus.TYPE_ACTIVITY.equals(assetStatus.getType())) {
            if (IAsset.STATUS_ASSET_INACTIVE == assetStatus.getStatus()) {
              eventType = IEvent.ASSET_INACTIVE;
            } else if (IAsset.STATUS_NORMAL == assetStatus.getStatus()) {
              eventType = IEvent.ASSET_ACTIVE;
            }
          } else if (IAssetStatus.TYPE_DEVCONN.equals(assetStatus.getType())) {
            if (IAsset.STATUS_DEVICE_DISCONNECTED == assetStatus.getStatus()) {
              eventType = IEvent.DEVICE_DISCONNECTED;
            } else if (IAsset.STATUS_NORMAL == assetStatus.getStatus()) {
              eventType = IEvent.DEVICE_CONNECTION_NORMAL;
            }
          } else if (IAssetStatus.TYPE_XSENSOR.equals(assetStatus.getType())) {
            if (IAsset.STATUS_SENSOR_DISCONNECTED == assetStatus.getStatus()) {
              eventType = IEvent.SENSOR_DISCONNECTED;
            } else if (IAsset.STATUS_NORMAL == assetStatus.getStatus()) {
              eventType = IEvent.SENSOR_CONNECTION_NORMAL;
            }
          } else if (IAssetStatus.TYPE_POWER.equals(assetStatus.getType())) {
            if (IAsset.STATUS_POWER_OUTAGE == assetStatus.getStatus()) {
              eventType = IEvent.POWER_OUTAGE;
            } else if (IAsset.STATUS_NORMAL == assetStatus.getStatus()) {
              eventType = IEvent.POWER_NORMAL;
            }
          } else if (IAssetStatus.TYPE_STATE.equals(assetStatus.getType()) &&
              StringUtils.isNotEmpty(assetStatus.getsId())) {
            eventType = IEvent.STATUS_CHANGE_TEMP;
          }
        }
        // NO_ACTIVITY event is handled during DailyEventsCreation
        // Log the high excursion, low excursion and incursion events. The NO_ACTIVITY event is handled by the DailyEventsGenerator
        if (eventType == IEvent.STATUS_CHANGE) {
          AssetUtil.generateAssetStatusEvents(asset.getDomainId(), assetStatus, asset);
        } else if (eventType != -1) {
          EventsConfig ec = DomainConfig.getInstance(asset.getDomainId()).getEventsConfig();
          EventSpec eventSpec =
              ec.getEventSpec(eventType, JDOUtils.getImplClass(IAssetStatus.class).getName());
          // After getting tempEventSpec, call a private method to generate temperature events.
          if (eventSpec != null) {
            AssetUtil.generateAssetEvents(asset.getDomainId(), eventSpec, assetStatus, asset,
                eventType);
          }
        }
      }
    } catch (Exception e) {
      xLogger.severe("Error while creating event for asset status", e);
    }
  }

  @Override
  public void deleteAsset(String manufacturerId, List<String> serialIds, Long domainId)
      throws ServiceException {
    PersistenceManager pm = PMF.get().getPersistenceManager();
    Transaction tx = null;
    try {
      tx = pm.currentTransaction();
      tx.begin();
      Query query = pm.newQuery(JDOUtils.getImplClass(IAsset.class));
      query.setFilter("vId == vIdParam && assetIdParam.contains(sId)");
      query.declareParameters("String vIdParam, java.util.Collection assetIdParam");
      List<IAsset> assets;
      try {
        assets = (List<IAsset>) query.execute(manufacturerId, serialIds);
        for (IAsset asset : assets) {
          deleteAssetRelation(asset.getId(), domainId, pm);
        }
        for (IAsset asset : assets) {
          EventPublisher
              .generate(domainId, IEvent.DELETED, null, JDOUtils.getImplClassName(IAsset.class),
                  String.valueOf(asset.getId()), null, asset);
        }
        pm.deletePersistentAll(assets);
        tx.commit();
      } finally {
        query.closeAll();
      }
    } catch (Exception e) {
      xLogger.severe("{0} while deleting assets: {1}, {2}", e.getMessage(), manufacturerId,
          serialIds.toString(), e);
      throw new ServiceException(e.getMessage());
    } finally {
      if (tx.isActive()) {
        tx.rollback();
      }
      pm.close();
    }
  }

  @Override
  public IAssetRelation createOrUpdateAssetRelation(Long domainId, IAssetRelation assetRelation)
      throws ServiceException {
    PersistenceManager pm = PMF.get().getPersistenceManager();
    try {
      Query query = pm.newQuery(JDOUtils.getImplClass(IAssetRelation.class));
      query.setFilter("assetId == assetIdParam");
      query.declareParameters("Long assetIdParam");
      List<IAssetRelation> assetRelations = null;
      try {
        assetRelations = (List<IAssetRelation>) query.execute(assetRelation.getAssetId());
        assetRelations = (List<IAssetRelation>) pm.detachCopyAll(assetRelations);
      } catch (Exception ignored) {
        //do nothing
      } finally {
        query.closeAll();
      }

      if (assetRelations != null && assetRelations.size() == 1) {
        IAssetRelation assetRelationTmp = assetRelations.get(0);
        assetRelationTmp.setRelatedAssetId(assetRelation.getRelatedAssetId());
        assetRelationTmp.setType(assetRelation.getType());
        pm.makePersistent(assetRelationTmp);
        return assetRelationTmp;
      } else {
        pm.makePersistent(assetRelation);
        EventPublisher.generate(domainId, IEvent.CREATED, null,
            JDOUtils.getImplClassName(IAssetRelation.class), String.valueOf(assetRelation.getId()),
            null);
        return assetRelation;
      }
    } catch (Exception e) {
      xLogger.warn("{0} while updating asset relationship for the asset {1}", e.getMessage(),
          assetRelation.getAssetId(), e);
      throw new ServiceException(e.getMessage());
    } finally {
      pm.close();
    }
  }

  @Override
  public void deleteAssetRelation(Long assetId, Long domainId, IAsset asset)
      throws ServiceException {
    PersistenceManager pm = PMF.get().getPersistenceManager();
    try {
      deleteAssetRelation(assetId, domainId, pm);
    } finally {
      pm.close();
    }
  }

  public void deleteAssetRelation(Long assetId, Long domainId, PersistenceManager pm)
      throws ServiceException {
    try {
      Query query = pm.newQuery(JDOUtils.getImplClass(IAssetRelation.class));
      query.setFilter("assetId == assetIdParam");
      query.declareParameters("Long assetIdParam");
      List<IAssetRelation> assetRelations = null;
      try {
        assetRelations = (List<IAssetRelation>) query.execute(assetId);
        assetRelations = (List<IAssetRelation>) pm.detachCopyAll(assetRelations);
      } catch (Exception ignored) {
        //do nothing
      } finally {
        query.closeAll();
      }

      if (assetRelations != null && assetRelations.size() == 1) {
        IAssetRelation assetRelationTmp = assetRelations.get(0);
        for (IAssetRelation assetRelation : assetRelations) {
          EventPublisher.generate(domainId, IEvent.DELETED, null,
              JDOUtils.getImplClassName(IAssetRelation.class),
              String.valueOf(assetRelation.getId()), null, assetRelation);
        }
        pm.deletePersistent(assetRelationTmp);
      }
    } catch (Exception e) {
      xLogger
          .warn("{0} while deleting asset relationship for the asset {1}", e.getMessage(), assetId,
              e);
      throw new ServiceException(e.getMessage());
    }
  }

  public void deleteAssetRelationByRelatedAsset(Long relatedAssetId) throws ServiceException {
    PersistenceManager pm = PMF.get().getPersistenceManager();
    try {
      Query query = pm.newQuery(JDOUtils.getImplClass(IAssetRelation.class));
      query.setFilter("relatedAssetId == relatedAssetIdParam");
      query.declareParameters("Long relatedAssetIdParam");
      List<IAssetRelation> assetRelations = null;
      try {
        assetRelations = (List<IAssetRelation>) query.execute(relatedAssetId);
        assetRelations = (List<IAssetRelation>) pm.detachCopyAll(assetRelations);
      } catch (Exception ignored) {
        //do nothing
      } finally {
        query.closeAll();
      }

      if (assetRelations != null && assetRelations.size() == 1) {
        IAssetRelation assetRelationTmp = assetRelations.get(0);
        pm.deletePersistent(assetRelationTmp);
      }
    } catch (Exception e) {
      xLogger.warn("{0} while deleting asset relationship for the asset {1}", e.getMessage(),
          relatedAssetId, e);
      throw new ServiceException(e.getMessage());
    } finally {
      pm.close();
    }
  }

  @Override
  public IAssetRelation getAssetRelationByRelatedAsset(Long relatedAssetId)
      throws ServiceException {
    PersistenceManager pm = PMF.get().getPersistenceManager();
    try {
      Query query = pm.newQuery(JDOUtils.getImplClass(IAssetRelation.class));
      query.setFilter("relatedAssetId == relatedAssetIdParam");
      query.declareParameters("Long relatedAssetIdParam");
      List<IAssetRelation> assetRelations = null;
      try {
        assetRelations = (List<IAssetRelation>) query.execute(relatedAssetId);
        assetRelations = (List<IAssetRelation>) pm.detachCopyAll(assetRelations);
      } catch (Exception ignored) {
        //do nothing
      } finally {
        query.closeAll();
      }

      if (assetRelations != null && assetRelations.size() == 1) {
        return assetRelations.get(0);
      }
    } catch (Exception e) {
      xLogger.warn("{0} while deleting asset relationship for the asset {1}", e.getMessage(),
          relatedAssetId, e);
      throw new ServiceException(e.getMessage());
    } finally {
      pm.close();
    }

    return null;
  }

  @Override
  public IAssetRelation getAssetRelationByAsset(Long assetId) throws ServiceException {
    PersistenceManager pm = PMF.get().getPersistenceManager();
    try {
      Query query = pm.newQuery(JDOUtils.getImplClass(IAssetRelation.class));
      query.setFilter("assetId == assetIdParam");
      query.declareParameters("Long assetIdParam");
      List<IAssetRelation> assetRelations = null;
      try {
        assetRelations = (List<IAssetRelation>) query.execute(assetId);
        assetRelations = (List<IAssetRelation>) pm.detachCopyAll(assetRelations);
      } catch (Exception ignored) {
        //do nothing
      } finally {
        query.closeAll();
      }

      if (assetRelations != null && assetRelations.size() == 1) {
        return assetRelations.get(0);
      }
    } catch (Exception e) {
      xLogger
          .warn("{0} while getting asset relationship for the asset {1}", e.getMessage(), assetId,
              e);
      throw new ServiceException(e.getMessage());
    } finally {
      pm.close();
    }

    return null;
  }

  public List<IAsset> getAssets(Long domainId, Long kId, String q, String assetType, Boolean all)
      throws ServiceException {
    PersistenceManager pm = PMF.get().getPersistenceManager();
    List<IAsset> assets = new ArrayList<>();
    List<Object> params = new ArrayList<>();
    try {
      StringBuilder sqlQuery = new StringBuilder("SELECT * FROM ASSET WHERE");

      if (all) {
        sqlQuery.append(" ID IN (SELECT ID_OID FROM ASSET_DOMAINS WHERE DOMAIN_ID = ?)");
        params.add(domainId);
      } else {
        sqlQuery.append(
            " ID NOT IN (SELECT RELATEDASSETID FROM ASSETRELATION where RELATEDASSETID is not NULL) AND SDID = ?");
        params.add(domainId);
      }

      if (kId != null) {
        sqlQuery.append(" AND KID = ?");
        params.add(kId);
      }

      if (!q.isEmpty()) {
        sqlQuery.append(" AND nsId like ?");
        params.add(q + "%");
      }

      if (assetType != null) {
        sqlQuery.append(" AND type = ? ");
        params.add(assetType);
      }

      sqlQuery.append(" ORDER BY nsId asc LIMIT 0, 10");
      Query query = pm.newQuery(Constants.JAVAX_JDO_QUERY_SQL, sqlQuery.toString());
      query.setClass(JDOUtils.getImplClass(IAsset.class));
      try {
        assets = (List<IAsset>) query.executeWithArray(params.toArray());
        assets = (List<IAsset>) pm.detachCopyAll(assets);
      } finally {
        query.closeAll();
      }
    } catch (Exception e) {
      throw new ServiceException(e.getMessage());
    } finally {
      pm.close();
    }

    return assets;
  }

  @Override
  public Map<String, Integer> getTemperatureStatus(Long entityId) {
    PersistenceManager pm = null;
    JDOConnection conn = null;
    PreparedStatement statement = null;
    CachedRowSet rowSet = null;
    try {
      AssetSystemConfig config = AssetSystemConfig.getInstance();
      Map<Integer, AssetSystemConfig.Asset>
          monitoredAssets =
          config.getAssetsByType(IAsset.MONITORED_ASSET);
      String csv = monitoredAssets.keySet().toString();
      csv = csv.substring(1, csv.length() - 1);

      pm = PMF.get().getPersistenceManager();
      conn = pm.getDataStoreConnection();
      java.sql.Connection sqlConn = (java.sql.Connection) conn;
      rowSet = new CachedRowSetImpl();
      String query =
          "SELECT STAT, COUNT(1) COUNT FROM (SELECT ID, IF(ASF.STAT = 'tu', 'tu',(SELECT IF(MAX(ABNSTATUS) = 2, 'th', "
              + "IF(MAX(ABNSTATUS) = 1, 'tl', 'tn')) FROM ASSETSTATUS AO WHERE AO.ASSETID = ASF.ASSETID AND AO.TYPE = 1 AND AO.STATUS = 3 "
              + ")) STAT FROM (SELECT A.ID FROM ASSET A WHERE TYPE IN (TOKEN_TYPE) AND KID = ? AND "
              + "EXISTS(SELECT 1 FROM ASSETRELATION R WHERE A.ID = R.ASSETID AND R.TYPE = 2) AND EXISTS"
              + "(SELECT 1 FROM ASSETSTATUS S WHERE S.ASSETID = A.ID AND S.TYPE = 7 AND S.STATUS = 0)"
              + ") A "
              + "LEFT JOIN (SELECT ASSETID, IF(MIN(STATUS) = 0, 'tk', 'tu') STAT FROM ASSETSTATUS ASI WHERE ASI.TYPE = 3 AND "
              + "ASI.ASSETID IN (SELECT ID FROM ASSET WHERE TYPE IN (TOKEN_TYPE) AND KID = ?) GROUP BY ASI.ASSETID) ASF "
              + "ON A.ID = ASF.ASSETID) T GROUP BY T.STAT";
      query = query.replace("TOKEN_TYPE", csv);
      statement = sqlConn.prepareStatement(query);
      statement.setLong(1,entityId);
      statement.setLong(2,entityId);
      rowSet.populate(statement.executeQuery(query));
      Map<String, Integer> stats = new HashMap<>(4);
      while (rowSet.next()) {
        stats.put(rowSet.getString("STAT"), rowSet.getInt("COUNT"));
      }
      return stats;
    } catch (Exception e) {
      xLogger.severe("Error in fetching Temperature status for assets of entity {0}", entityId, e);
    } finally {

      try{
        if (rowSet != null) {
          rowSet.close();
        }
      }catch(Exception ignored) {
        xLogger.warn("Exception while closing rowSet", ignored);
      }
      try {
        if (statement != null) {
          statement.close();
        }
      } catch (Exception ignored) {
        xLogger.warn("Exception while closing statement", ignored);
      }

      try {
        if (conn != null) {
          conn.close();
        }
      } catch (Exception ignored) {
        xLogger.warn("Exception while closing connection", ignored);
      }

      try {
        if (pm != null) {
          pm.close();
        }
      } catch (Exception ignored) {
        xLogger.warn("Exception while closing pm", ignored);
      }
    }
    return null;
  }

  @Override
  public List<String> getModelSuggestion(Long domainId, String term) {
    PersistenceManager pm = PMF.get().getPersistenceManager();
    Query query = null;
    try {
      List<Object> params = new ArrayList<>();
      StringBuilder sqlQuery = new StringBuilder("SELECT DISTINCT MODEL FROM ASSET WHERE");
      sqlQuery.append(" ID IN (SELECT ID_OID FROM ASSET_DOMAINS WHERE DOMAIN_ID = ?)");
      params.add(domainId);
      if (StringUtils.isNotEmpty(term)) {
        sqlQuery.append(" AND lower(MODEL) like ? ");
        params.add("%" + term.toLowerCase() + "%");
      }
      sqlQuery.append(" LIMIT 0, 10");
      query = pm.newQuery(Constants.JAVAX_JDO_QUERY_SQL, sqlQuery.toString());
      List modelList = (List) query.executeWithArray(params.toArray());
      List<String> models = new ArrayList<>(modelList.size());
      for (Object o : modelList) {
        models.add((String) o);
      }
      return models;
    } catch (Exception e) {
      xLogger.warn("Error while fetching suggestions for asset models", e);
    } finally {
      if (query != null) {
        query.closeAll();
      }
      pm.close();
    }
    return null;
  }

  @Override
  public String getMonitoredAssetIdsForReport(Map<String, String> filters) {
    PersistenceManager pm = PMF.get().getPersistenceManager();
    Connection conn = (java.sql.Connection) pm.getDataStoreConnection();
    PreparedStatement statement = null;
    StringBuilder assetQuery = new StringBuilder("SELECT SID,VID FROM ASSET WHERE TYPE != 1 ");
    List<Object> params = new ArrayList<>();
    if (filters.containsKey("TOKEN_CN")
        || filters.containsKey("TOKEN_ST")
        || filters.containsKey("TOKEN_DIS")
        || filters.containsKey("TOKEN_TALUK")
        || filters.containsKey("TOKEN_KID")
        || filters.containsKey("TOKEN_KTAG")) {
      assetQuery.append("AND KID IN (");
      StringBuilder kioskQuery =
          new StringBuilder("SELECT KIOSKID FROM KIOSK K ,KIOSK_DOMAINS KD WHERE ");
      kioskQuery.append(" K.KIOSKID = KD.KIOSKID_OID AND KD.DOMAIN_ID = ?");
      params.add(filters.get("TOKEN_DID"));
      if (filters.containsKey("TOKEN_CN")) {
        kioskQuery.append(" AND K.COUNTRY = ?");
        params.add(filters.get("TOKEN_CN"));
      }
      if (filters.containsKey("TOKEN_ST")) {
        kioskQuery.append(" AND K.STATE = ?");
        params.add(filters.get("TOKEN_ST"));
      }
      if (filters.containsKey("TOKEN_DIS")) {
        kioskQuery.append(" AND K.DISTRICT = ?");
        params.add(filters.get("TOKEN_DIS"));
      }
      if (filters.containsKey("TOKEN_TALUK")) {
        kioskQuery.append(" AND K.TALUK = ?");
        params.add(filters.get("TOKEN_TALUK"));
      }
      if (filters.containsKey("TOKEN_KID")) {
        kioskQuery.append(" AND K.KIOSKID = ?");
        params.add(filters.get("TOKEN_KID"));
      } else if (filters.containsKey("TOKEN_KTAG")) {
        kioskQuery.append(" AND K.KIOSKID IN (SELECT KT.KIOSKID FROM KIOSK_TAGS KT,TAG T WHERE T.ID = KT.ID AND T.NAME = ?)");
        params.add(filters.get("TOKEN_KTAG"));
      }
      assetQuery.append(kioskQuery);
      assetQuery.append(")");
    } else {
      assetQuery.append("AND KID IN (SELECT KIOSKID_OID FROM KIOSK_DOMAINS WHERE DOMAIN_ID = ?)");
      params.add(filters.get("TOKEN_DID"));
    }
    if (filters.containsKey("TOKEN_ATYPE")) {
      assetQuery.append(" AND type = ?");
      params.add(filters.get("TOKEN_ATYPE"));
    }
    if (filters.containsKey("TOKEN_VID")) {
      assetQuery.append(" AND VID = ?");
      params.add(filters.get("TOKEN_VID"));
    }
    if (filters.containsKey("TOKEN_DMODEL")) {
      assetQuery.append(" AND model = ?");
      params.add(filters.get("TOKEN_DMODEL"));
    }
    if (filters.containsKey("TOKEN_MYEAR")) {
      assetQuery.append(" AND yom = ?");
      params.add(filters.get("TOKEN_MYEAR"));
    }
    if (filters.containsKey("TOKEN_SIZE") && filters.containsKey("TOKEN_OFFSET")) {
      assetQuery.append(" LIMIT ? , ?");
      params.add(filters.get("TOKEN_OFFSET"));
      params.add(filters.get("TOKEN_SIZE"));
    }
    ResultSet rs = null;
    try {
      statement = conn.prepareStatement(assetQuery.toString());
      int i = 1;
      for (Object param : params) {
        statement.setObject(i++, param);
      }
      rs = statement.executeQuery();
      StringBuilder assetIds = new StringBuilder();
      while (rs.next()) {
        assetIds.append(CharacterConstants.S_QUOTE).append(rs.getString("VID"))
            .append(CharacterConstants.UNDERSCORE).append(rs.getString("SID"))
            .append(CharacterConstants.S_QUOTE).append(CharacterConstants.COMMA);
      }
      if(assetIds.length() > 0) {
        assetIds.setLength(assetIds.length() - 1);
      }
      return assetIds.toString();
    } catch (Exception e) {
      xLogger.warn("Error while fetching asset ids for reports", e);
    } finally {
      if (rs != null) {
        try {
          rs.close();
        } catch (SQLException e) {
          xLogger.warn("Exception while closing resultset", e);
        }
      }
      try {
        if (statement != null) {
          statement.close();
        }
      } catch (Exception e) {
        xLogger.warn("Exception while closing connection", e);
      }
      try {
        conn.close();
      } catch (SQLException e) {
        xLogger.warn("Exception while closing connection", e);
      }
      pm.close();
    }
    return null;
  }

  @Override
  public String getVendorIdsForReports(String did) {
    PersistenceManager pm = PMF.get().getPersistenceManager();
    Query query;
    StringBuilder vidQuery = new StringBuilder();
    StringBuilder vids = new StringBuilder("");
    if (StringUtils.isNotEmpty(did)) {
      vidQuery
          .append("SELECT DISTINCT VID FROM ASSET WHERE KID IN (")
          .append("SELECT KIOSKID FROM KIOSK K ,KIOSK_DOMAINS KD WHERE ")
          .append("K.KIOSKID = KD.KIOSKID_OID AND KD.DOMAIN_ID = ?)");
    }
    query = pm.newQuery(Constants.JAVAX_JDO_QUERY_SQL, vidQuery.toString());
    try {
      List<String> result = (List) query.execute(did);
      for (int i = 0; i < result.size(); i++) {
        if (i != 0) {
          vids.append(CharacterConstants.COMMA);
        }
        vids.append(CharacterConstants.S_QUOTE).append(result.get(i))
            .append(CharacterConstants.S_QUOTE);
      }
      return vids.toString();
    } catch (Exception e) {
      xLogger.warn("Error while fetching vendor ids for reports", e);
    } finally {
      if (query != null) {
        query.closeAll();
      }
      pm.close();
    }
    return null;
  }

  @Override
  public String getAssetTypesForReports(String did, String exclude) {
    PersistenceManager pm = PMF.get().getPersistenceManager();
    Query query;
    StringBuilder q = new StringBuilder();
    StringBuilder types = new StringBuilder();
    List<String> params = new ArrayList<>();
    if (StringUtils.isNotEmpty(did)) {
      q.append("SELECT DISTINCT TYPE FROM ASSET WHERE KID IN (")
          .append("SELECT KIOSKID FROM KIOSK K ,KIOSK_DOMAINS KD WHERE ")
          .append("K.KIOSKID = KD.KIOSKID_OID AND KD.DOMAIN_ID = ?)");
      params.add(did);
    }
    if (StringUtils.isNotEmpty(exclude)) {
      q.append(" AND TYPE != ?");
      params.add(exclude);
    }
    query = pm.newQuery(Constants.JAVAX_JDO_QUERY_SQL, q.toString());
    try {
      List<Integer> result = (List) query.executeWithArray(params.toArray());
      for (int i = 0; i < result.size(); i++) {
        if (i != 0) {
          types.append(CharacterConstants.COMMA);
        }
        types.append(CharacterConstants.S_QUOTE).append(result.get(i).toString())
            .append(CharacterConstants.S_QUOTE);
      }
      return types.toString();
    } catch (Exception e) {
      xLogger.warn("Error while fetching asset types for reports", e);
    } finally {
      if (query != null) {
        query.closeAll();
      }
      pm.close();
    }
    return null;
  }

  @Override
  public void updateWorkingStatus(IAsset asset, AssetModels.AssetStatus assetStatusModel)
      throws ServiceException {
    PersistenceManager pm = PMF.get().getPersistenceManager();
    try {
      AssetUtil.updateDeviceWorkingStatus(assetStatusModel, asset);
      asset.setUpdatedOn(new Date());
      asset.setUpdatedBy(assetStatusModel.stub);
      pm.makePersistent(asset);
    } finally{
      pm.close();
    }
  }
}
