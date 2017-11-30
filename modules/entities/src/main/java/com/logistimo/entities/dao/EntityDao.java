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

package com.logistimo.entities.dao;

import com.logistimo.constants.CharacterConstants;
import com.logistimo.constants.Constants;
import com.logistimo.constants.QueryConstants;
import com.logistimo.entities.entity.Approver;
import com.logistimo.entities.entity.IApprover;
import com.logistimo.entities.entity.IKiosk;
import com.logistimo.entities.entity.IKioskLink;
import com.logistimo.entities.entity.IPoolGroup;
import com.logistimo.entities.entity.Kiosk;
import com.logistimo.entities.entity.KioskLink;
import com.logistimo.entities.models.ApproverFilters;
import com.logistimo.entities.models.KioskLinkFilters;
import com.logistimo.logger.XLog;
import com.logistimo.pagination.PageParams;
import com.logistimo.pagination.QueryParams;
import com.logistimo.pagination.Results;
import com.logistimo.services.ServiceException;
import com.logistimo.services.impl.PMF;
import com.logistimo.tags.dao.ITagDao;
import com.logistimo.tags.dao.TagDao;
import com.logistimo.tags.entity.ITag;
import com.logistimo.utils.QueryUtil;
import com.logistimo.utils.StringUtil;

import org.apache.commons.lang.StringUtils;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.jdo.PersistenceManager;
import javax.jdo.Query;

/**
 * Created by charan on 17/02/15.
 */
public class EntityDao implements IEntityDao {

  private static final XLog xLogger = XLog.getLog(EntityDao.class);

  ITagDao tagDao = new TagDao();

  public Results getAllKiosks(Long domainId, String tag, String excludedTags,PageParams pageParams) {
    return getKiosks(domainId, tag, excludedTags, pageParams, false);
  }

  public Results getAllDomainKiosks(Long domainId, String tags, String excludedTags, PageParams pageParams) {
    return getKiosks(domainId, tags, excludedTags, pageParams, true);
  }

  private Results getKiosks(Long domainId, String tags, String excludedTags, PageParams pageParams, boolean isDomain) {
    xLogger.fine("Entering getKiosks");
    PersistenceManager pm = PMF.get().getPersistenceManager();
    List<IKiosk> kiosks = new ArrayList<>();
    String cursor = null;
    try {
      StringBuilder filter = new StringBuilder();
      if (isDomain) {
        filter.append("sdId == domainIdParam");
      } else {
        filter.append("dId.contains(domainIdParam)");
      }
      String declaration = "Long domainIdParam";
      Map<String, Object> params = new HashMap<>();
      params.put("domainIdParam", domainId);
      if (StringUtils.isNotEmpty(tags) || StringUtils.isNotEmpty(excludedTags)) {
        boolean isExcluded = StringUtils.isNotEmpty(excludedTags);
        String value = isExcluded ? excludedTags : tags;
        List<String> kTags = StringUtil.getList(value, true);
        List<ITag> tagIdList = tagDao.getTagsByNames(kTags, ITag.KIOSK_TAG);
        int i = 0;
        filter.append(" && ( ");
        for (ITag localTag : tagIdList) {
          String tgsParam = "tgsParam" + (++i);
          if (i != 1) {
            filter.append(isExcluded ? " && " : " || ");
          }
          filter.append(" ")
              .append(isExcluded ? QueryConstants.NEGATION : CharacterConstants.EMPTY);
          filter.append("tgs.contains(").append(tgsParam).append(")");
          declaration += ", Long " + tgsParam;
          params.put(tgsParam, localTag.getId());
        }
        filter.append(" ) ");
      }
      Query query = pm.newQuery(Kiosk.class);
      query.setFilter(filter.toString());
      query.declareParameters(declaration);
      query.setOrdering("nName asc");
      if (pageParams != null) {
        QueryUtil.setPageParams(query, pageParams);
      }
      try {
        kiosks = (List<IKiosk>) query.executeWithMap(params);
        kiosks
            .size(); // TODO This is to prevent datanucleus exception - "Object manager is closed"; this retrieves all objects before object manager is closed
        // Get the cursor, if any
        cursor = QueryUtil.getCursor(kiosks);
        kiosks = (List<IKiosk>) pm.detachCopyAll(kiosks);
      } finally {
        query.closeAll();
      }
    } catch (Exception e) {
      xLogger.warn("Exception: {0}", e.getMessage());
    } finally {
      // Close PM
      pm.close();
    }

    xLogger.fine("Exiting getKiosks");
    return new Results(kiosks, cursor, -1, (pageParams == null ? 0 : pageParams.getOffset()));
  }

  public String getKeyString(IKiosk kiosk) {
    return String.valueOf(kiosk.getKioskId());
  }


  public String getKeyString(IPoolGroup group) {
    return String.valueOf(group.getGroupId());
  }

  public List<IApprover> getApprovers(ApproverFilters filters) {
    PersistenceManager pm = PMF.get().getPersistenceManager();
    Query query = null;
    List<IApprover> approvers = null;
    try {
      QueryParams
          sqlQueryModel = buildApproversQuery(filters);
      String executeQuery = sqlQueryModel.query;
      query = pm.newQuery("javax.jdo.query.SQL", executeQuery);
      query.setClass(Approver.class);
      approvers = (List<IApprover>) query.executeWithArray(
          sqlQueryModel.listParams.toArray());
      approvers = (List<IApprover>) pm.detachCopyAll(approvers);
    } catch (Exception e) {
      xLogger.severe("Error while getting approvers", e);
    } finally {
      if (query != null) {
        query.closeAll();
      }
      pm.close();
    }
    return approvers;
  }

  public Results getKioskLinks(KioskLinkFilters filters, PageParams pageParams, boolean countOnly)
      throws
      ServiceException {
    PersistenceManager pm = PMF.get().getPersistenceManager();
    Query query = null;
    Query cntQuery = null;
    List<IKioskLink> kioskLinks = null;
    int count = 0;
    try {
      QueryParams
          sqlQueryModel = buildKioskLinksQuery(filters, pageParams, false);
      if (!countOnly) {
        String sqlQuery = sqlQueryModel.query;
        query = pm.newQuery("javax.jdo.query.SQL", sqlQuery);
        query.setClass(KioskLink.class);
        kioskLinks = (List<IKioskLink>) query.executeWithArray(
            sqlQueryModel.listParams.toArray());
        kioskLinks = (List<IKioskLink>) pm.detachCopyAll(kioskLinks);
      }
      filters.withModifiedSince(null);
      QueryParams cntSqlQueryModel = buildKioskLinksQuery(filters, null, true);
      cntQuery = pm.newQuery("javax.jdo.query.SQL", cntSqlQueryModel.query);
      cntQuery.setUnique(true);
      count =
          ((Long) (cntQuery.executeWithArray(cntSqlQueryModel.listParams.toArray()))).intValue();
    } catch (Exception e) {
      xLogger.severe("Error while getting kiosklinks", e);
      throw new ServiceException(e);
    } finally {
      if (query != null) {
        try {
          query.closeAll();
        } catch (Exception e) {
          // ignore
        }
      }
      if (cntQuery != null) {
        try {
          cntQuery.closeAll();
        } catch (Exception e) {
          // ignore
        }
      }
      pm.close();
    }
    return new Results(kioskLinks, null, count,
        pageParams == null ? 0 : pageParams.getOffset());
  }

  private QueryParams buildApproversQuery(ApproverFilters filters) {
    StringBuilder
        queryBuilder =
        new StringBuilder("SELECT * FROM APPROVERS");
    if (filters.getKioskId() == null) {
      throw new IllegalArgumentException("Invalid kiosk ID");
    }
    List<String> params = new ArrayList<>(1);
    queryBuilder.append(" WHERE KID = ?");
    params.add(String.valueOf(filters.getKioskId()));

    if (filters.getOrderType() != null) {
      queryBuilder.append(" AND OTYPE = ?");
      params.add(String.valueOf(filters.getOrderType()));
    }
    if (filters.getType() != null) {
      queryBuilder.append(" AND TYPE = ?");
      params.add(String.valueOf(filters.getType()));
    }
    return new QueryParams(queryBuilder.toString(), params, QueryParams.QTYPE.SQL,
        IApprover.class);
  }

  private QueryParams buildKioskLinksQuery(KioskLinkFilters filters, PageParams pageParams,
                                           boolean buildCountQuery) {
    StringBuilder
        sqlQuery =
        new StringBuilder("SELECT KL.`KEY` AS `KEY`, KL.* FROM KIOSKLINK KL,KIOSK K");
    String orderBy = CharacterConstants.EMPTY;
    List<String> params = new ArrayList<>(1);
    if (filters.getKioskId() == null) {
      throw new IllegalArgumentException("Invalid kiosk ID");
    }
    sqlQuery.append(" WHERE KL.KIOSKID = ?");
    params.add(String.valueOf(filters.getKioskId()));
    if (filters.getLinkType() != null) {
      sqlQuery.append(" AND KL.LINKTYPE = ?");
      params.add(filters.getLinkType());
    }
    sqlQuery.append(" AND KL.LINKEDKIOSKID = K.KIOSKID");
    if (filters.getRouteEnabled() || StringUtils.isNotEmpty(filters.getRouteTag())) {
      sqlQuery.append(" AND KL.RTG = ?");
      params.add(filters.getRouteTag());
      if (filters.getRouteEnabled()) {
        orderBy = " ORDER BY KL.RI";
      }
    } else {
      orderBy = " ORDER BY K.NAME";
    }
    if (StringUtils.isNotEmpty(filters.getStartsWith())) {
      sqlQuery.append(" AND K.NNAME like ?");
      params.add(filters.getStartsWith().toLowerCase() + CharacterConstants.PERCENT);
    } else if (filters.getLinkedKioskId() != null) {
      sqlQuery.append(" AND KL.LINKEDKIOSKID = ?");
      params.add(String.valueOf(filters.getLinkedKioskId()));
    } else if (StringUtils.isNotEmpty(filters.getEntityTag())) {
      sqlQuery.append(
          " AND KL.LINKEDKIOSKID IN (SELECT KIOSKID FROM KIOSK_TAGS WHERE ID IN (SELECT ID FROM TAG WHERE NAME = ? AND TYPE = "
              + ITag.KIOSK_TAG + "))");
      params.add(filters.getEntityTag());
    }
    if (filters.getModifiedSince() != null) {
      SimpleDateFormat sdf = new SimpleDateFormat(Constants.DATETIME_CSV_FORMAT);
      sqlQuery.append(" AND CREATEDON >= ? ");
      params.add(sdf.format(filters.getModifiedSince()));
    }
    if (buildCountQuery) {
      String
          cntQueryStr =
          sqlQuery.toString().replace("KL.`KEY` AS `KEY`, KL.*", QueryConstants.ROW_COUNT);
      return new QueryParams(cntQueryStr, params, QueryParams.QTYPE.SQL,
          IKioskLink.class);
    }
    sqlQuery.append(orderBy);
    addLimitToQuery(sqlQuery, pageParams);
    return new QueryParams(sqlQuery.toString(), params, QueryParams.QTYPE.SQL,
        IKioskLink.class);
  }

  private void addLimitToQuery(StringBuilder sqlQuery, PageParams pageParams) {
    if (pageParams != null) {
      sqlQuery.append(" LIMIT ").append(pageParams.getOffset()).append(CharacterConstants.COMMA)
          .append(pageParams.getSize());
    }
  }
}
