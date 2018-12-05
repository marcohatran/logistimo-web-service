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

package com.logistimo.entities.utils;

import com.logistimo.assets.entity.IAsset;
import com.logistimo.assets.entity.IAssetRelation;
import com.logistimo.assets.service.AssetManagementService;
import com.logistimo.auth.utils.SecurityUtils;
import com.logistimo.constants.CharacterConstants;
import com.logistimo.context.StaticApplicationContext;
import com.logistimo.entities.entity.IKiosk;
import com.logistimo.entities.service.EntitiesService;
import com.logistimo.entities.service.EntitiesServiceImpl;
import com.logistimo.services.Resources;
import com.logistimo.services.ServiceException;
import com.logistimo.users.entity.IUserAccount;
import com.logistimo.utils.MsgUtil;


import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang.StringUtils;

import java.util.*;
import java.util.stream.Collectors;

/**
 * The {@code EntityMoveHelper} class is a helper class for {@link EntityMover}.
 *
 * @author Mohan Raja
 * @see EntityMover
 */
@SuppressWarnings("unchecked")
public class EntityMoveHelper {

  /**
   * Validates whether any user {@code userIds} is associated with any entity other than {@code kIds},
   * which is selected to move to another domain.
   *
   * @param userIds set of user ids selected to move to another domain.
   * @param kIds    set of entity ids selected to move to another domain.
   * @return list of string with user name and associated entity names of user if that entity is
   * selected to move to another domain.
   * @throws ServiceException when there is error in fetching object.
   */
  public static List<String> validateUsers(Set<String> userIds, List<Long> kIds)
      throws ServiceException {
    List<String> userErrors = new ArrayList<>();
    EntitiesService as = StaticApplicationContext.getBean(EntitiesServiceImpl.class);
    StringBuilder missedKiosks = new StringBuilder();
    for (String userId : userIds) {
      List<Long> userKIds = as.getKioskIdsForUser(userId, null, null).getResults();
      if (userKIds!=null && !kIds.containsAll(userKIds)) {
        missedKiosks.append(MsgUtil.newLine()).append(MsgUtil.newLine());
        missedKiosks.append("User id: ").append(userId);
        missedKiosks.append(MsgUtil.newLine()).append("Missed entity:");
        for (Long userKId : userKIds) {
          if (!kIds.contains(userKId)) {
            IKiosk kiosk = as.getKiosk(userKId, false);
            missedKiosks.append(kiosk.getName()).append(CharacterConstants.COMMA);
          }
        }
        missedKiosks.setLength(missedKiosks.length() - 1);
        userErrors.add(missedKiosks.toString());
        missedKiosks.setLength(0);
      }
    }
    return userErrors;
  }

  /**
   * Extracts and returns set of unique user ids from list of {@code kiosks}
   *
   * @param kiosks list of kiosk object from which user id need to be extracted
   * @return set of unique user ids from list of {@code kiosks}
   */
  public static Set<String> extractUserIds(List<IKiosk> kiosks) {
    Set<String> userIds = new HashSet<>();
    for (IKiosk kiosk : kiosks) {
      userIds.addAll(
          kiosk.getUsers().stream().map(IUserAccount::getUserId).collect(Collectors.toList()));
    }
    return userIds;
  }

  public static List<String> isAssetsMovePossible(List<IKiosk> kioskList) throws ServiceException {
    List<String> errors = new ArrayList<>();
    AssetManagementService assetManagementService = StaticApplicationContext.getBean(AssetManagementService.class);
    for (IKiosk kiosk : kioskList) {
      List<IAsset> kioskAssets = assetManagementService.getAssetsByKiosk(kiosk.getKioskId());
      if (CollectionUtils.isNotEmpty(kioskAssets)) {
        for (IAsset asset : kioskAssets) {
          IAssetRelation assetRelation;
          Long relatedAssetId = null;
          if (IAsset.MONITORING_ASSET == asset.getType()) {
            assetRelation = assetManagementService.getAssetRelationByRelatedAsset(asset.getId());
            if(assetRelation != null) {
              relatedAssetId = assetRelation.getAssetId();
            }
          } else {
            assetRelation = assetManagementService.getAssetRelationByAsset(asset.getId());
            if(assetRelation != null) {
              relatedAssetId = assetRelation.getRelatedAssetId();
            }
          }
         if(assetRelation != null) {
           String errorMsg = validateRelatedAssetsBelongsToMovedEntities(assetManagementService, relatedAssetId, kiosk, asset.getSerialId());
           if(StringUtils.isNotBlank(errorMsg)) {
             errors.add(errorMsg);
           }
         }
        }
      }
    }
    return errors;
  }

  private static String validateRelatedAssetsBelongsToMovedEntities(AssetManagementService assetManagementService, Long relatedAssetId, IKiosk kiosk, String assetSerialNumber)
      throws ServiceException {
    StringBuilder errorMsg = new StringBuilder();
    IAsset relatedAsset = assetManagementService.getAsset(relatedAssetId);
    if (relatedAsset.getKioskId() != null && !kiosk.getKioskId().equals(relatedAsset.getKioskId())) {
      ResourceBundle
          backendMessages =
          Resources.get().getBundle("BackendMessages", SecurityUtils.getLocale());
      errorMsg.append(MsgUtil.newLine()).append(backendMessages.getString("kiosk"))
          .append(": ")
          .append(kiosk.getName()).append(MsgUtil.newLine())
          .append(backendMessages.getString("assets")).append(": ").append(assetSerialNumber);

    }
    return errorMsg.toString();
  }
}
