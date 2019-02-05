/*
 * Copyright © 2019 Logistimo.
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

package com.logistimo.shipments.action;

import com.logistimo.activity.entity.IActivity;
import com.logistimo.activity.service.ActivityService;
import com.logistimo.conversations.entity.IMessage;
import com.logistimo.conversations.service.ConversationService;
import com.logistimo.conversations.service.impl.ConversationServiceImpl;
import com.logistimo.dao.JDOUtils;
import com.logistimo.events.entity.IEvent;
import com.logistimo.orders.service.OrderManagementService;
import com.logistimo.services.ServiceException;
import com.logistimo.shipments.ShipmentStatus;
import com.logistimo.shipments.entity.IShipment;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.Date;

import javax.jdo.PersistenceManager;

@Component
public class ShipmentActivty {

  @Autowired
  private ActivityService activityService;

  @Autowired
  private ConversationService conversationService;

  @Autowired
  private OrderManagementService orderManagementService;

  public void addActivity(String shipmentId, String userId, Long orderId, Long domainId,
                     ShipmentStatus prevStatus, ShipmentStatus newStatus, Date createDate,
                     PersistenceManager pm, IMessage iMessage) {
    activityService.createActivity(IActivity.TYPE.SHIPMENT.name(), shipmentId, "STATUS",
        prevStatus != null ? prevStatus.toString() : null,
        newStatus.toString(), userId, domainId, iMessage != null ? iMessage.getMessageId() : null,
        "ORDER:" + orderId, createDate, pm);
  }



  public IMessage addMessage(String shipmentId, String message, String userId,
                         Long orderId, Long domainId
      , Date createDate, PersistenceManager pm)
      throws ServiceException {
    IMessage iMessage = null;
    if (message != null) {
      iMessage = conversationService
          .addMsgToConversation(ConversationServiceImpl.ObjectTypeShipment, shipmentId, message,
              userId,
              Collections.singleton("ORDER:" + orderId), domainId, createDate, pm);
      orderManagementService.generateOrderCommentEvent(domainId, IEvent.COMMENTED,
          JDOUtils.getImplClassName(IShipment.class), shipmentId, null,
          null);
    }
    return iMessage;
  }

  public void updateMessageAndActivity(String shipmentId, String message, String userId,
                                       Long orderId, Long domainId
      , ShipmentStatus prevStatus, ShipmentStatus newStatus, Date createDate, PersistenceManager pm)
      throws ServiceException {
    IMessage iMessage = addMessage(shipmentId, message, userId, orderId, domainId, createDate, pm);
    addActivity(shipmentId, userId, orderId, domainId, prevStatus, newStatus, createDate, pm,
        iMessage);
  }

}
