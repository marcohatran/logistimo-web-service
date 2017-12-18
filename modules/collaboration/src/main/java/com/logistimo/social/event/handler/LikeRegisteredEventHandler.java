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

package com.logistimo.social.event.handler;

import com.logistimo.collaboration.core.events.LikeRegisteredEvent;
import com.logistimo.exception.SystemException;
import com.logistimo.logger.XLog;
import com.logistimo.services.ServiceException;
import com.logistimo.social.model.ContentQuerySpecs;
import com.logistimo.social.provider.ContentProvider;
import com.logistimo.social.provider.SubscriberProvider;
import com.logistimo.social.util.CollaborationNotificationUtil;
import com.logistimo.users.entity.IUserAccount;
import com.logistimo.users.service.impl.UsersServiceImpl;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.util.List;

/**
 * Created by kumargaurav on 13/11/17.
 */
@Component
public class LikeRegisteredEventHandler implements Handler<LikeRegisteredEvent> {

  private static final XLog log = XLog.getLog(LikeRegisteredEventHandler.class);

  SubscriberProvider subscriberProvider;

  ContentProvider contentProvider;

  UsersServiceImpl usersService;

  @Autowired
  public void setSubscriberProvider(SubscriberProvider subscriberProvider) {
    this.subscriberProvider = subscriberProvider;
  }

  @Autowired
  public void setContentProvider(ContentProvider contentProvider) {
    this.contentProvider = contentProvider;
  }

  @Autowired
  public void setUsersService(UsersServiceImpl usersService) {
    this.usersService = usersService;
  }

  @Override
  public void handle(LikeRegisteredEvent event) {
    log.info("LikeRegisteredEvent {0} being handled", event);
    List<IUserAccount> users = null;
    try {
      users = getSubscribers(event);
      log.info("total users {} to be notified for event {}",users != null?users.size():0,event);
    } catch (ServiceException e) {
      log.severe("Error occured during getting subscriber for event {0}", event, e);
      throw new SystemException(e, "CL001", event);
    }
    String content = null;
    try {
      content = getNotificationContent(event);
    } catch (ServiceException e) {
      log.warn("Error occured during content generation for event {0}", event, e);
      throw new SystemException(e, "CL002", event);
    }
    if (!StringUtils.isEmpty(content) && users != null && users.size() > 0) {
      try {
        CollaborationNotificationUtil
            .sendSMS(users, content, usersService.getUserAccount(event.getUser()));
      } catch (Exception e) {
        log.severe("Error occured while notifying users for collaboration event {0}", event, e);
        throw new SystemException(e, "CL003", event);
      }
    } else {
      log.warn("No users found for notification for event {0}", event);
    }
  }

  private List<IUserAccount> getSubscribers(LikeRegisteredEvent event) throws ServiceException {
    return subscriberProvider.getSubscriber(event);
  }

  private String getNotificationContent(LikeRegisteredEvent event) throws ServiceException {
    ContentQuerySpecs querySpecs = new ContentQuerySpecs();
    querySpecs.setObjectId(event.getObjectId());
    querySpecs.setObjectType(event.getObjectType());
    querySpecs.setContextId(event.getContextId());
    querySpecs.setContextType(event.getContextType());
    querySpecs.setContextAttribute(event.getContextAttributes());
    querySpecs.setUser(event.getUser());
    return contentProvider.generateContent(querySpecs);
  }
}
