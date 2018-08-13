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

package com.logistimo.returns.service;

import com.logistimo.activity.entity.IActivity;
import com.logistimo.activity.models.ActivityModel;
import com.logistimo.activity.service.impl.ActivitiesServiceImpl;
import com.logistimo.returns.Status;
import com.logistimo.returns.vo.ReturnsVO;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

/**
 * @author Mohan Raja.
 */
@Service
class ReturnsStatusHistoryService {

  @Autowired
  private ActivitiesServiceImpl activityService;

  public void addStatusHistory(ReturnsVO returnVO, Status oldStatus, Status newStatus,
                               String messageId) {
    ActivityModel activityModel = new ActivityModel();
    activityModel.objectType = IActivity.TYPE.RETURNS.name();
    activityModel.objectId = String.valueOf(returnVO.getId());
    activityModel.field = "STATUS";
    activityModel.prevValue = getStatus(oldStatus);
    activityModel.newValue = getStatus(newStatus);
    activityModel.userId = returnVO.getUpdatedBy();
    activityModel.domainId = returnVO.getSourceDomain();
    activityModel.messageId = messageId;
    activityModel.tag = "RETURNS:" + returnVO.getId();
    activityService.saveActivity(activityModel);
  }

  private String getStatus(Status status) {
    if (status != null) {
      return status.toString();
    }
    return null;
  }
}
