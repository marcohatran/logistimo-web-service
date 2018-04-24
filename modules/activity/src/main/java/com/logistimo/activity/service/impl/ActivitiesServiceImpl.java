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

package com.logistimo.activity.service.impl;

import com.logistimo.activity.entity.jpa.Activity;
import com.logistimo.activity.models.ActivityModel;
import com.logistimo.jpa.Repository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Date;

/**
 * Created by pratheeka on 24/04/18.
 */

@Service
public class ActivitiesServiceImpl {

  @Autowired
  private Repository repository;

  public Activity saveActivity(ActivityModel activityModel) {
    Activity activity = new Activity();
    activity.setObjectType(activityModel.objectType);
    activity.setObjectId(activityModel.objectId);
    activity.setField(activityModel.field);
    activity.setNewValue(activityModel.newValue);
    activity.setPrevValue(activityModel.prevValue);
    activity.setCreateDate(new Date());
    activity.setTag(activityModel.tag);
    activity.setUserId(activityModel.userId);
    activity.setDomainId(activityModel.domainId);
    activity.setMessageId(activityModel.messageId);
    repository.save(activity);
    return activity;

  }
}
