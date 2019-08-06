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

package com.logistimo.api.controllers;

import com.logistimo.api.builders.ConversationBuilder;
import com.logistimo.api.models.ConversationModel;
import com.logistimo.api.request.StringRequestObj;
import com.logistimo.auth.utils.SecurityUtils;
import com.logistimo.constants.Constants;
import com.logistimo.conversations.builders.MessageBuilder;
import com.logistimo.conversations.entity.IConversation;
import com.logistimo.conversations.entity.IMessage;
import com.logistimo.conversations.models.MessageModel;
import com.logistimo.conversations.service.ConversationService;
import com.logistimo.exception.InvalidDataException;
import com.logistimo.exception.InvalidServiceException;
import com.logistimo.logger.XLog;
import com.logistimo.orders.service.OrderManagementService;
import com.logistimo.pagination.PageParams;
import com.logistimo.pagination.Results;
import com.logistimo.returns.service.ReturnsService;
import com.logistimo.security.SecureUserDetails;
import com.logistimo.services.ObjectNotFoundException;
import com.logistimo.services.Resources;
import com.logistimo.services.ServiceException;
import com.logistimo.shipments.service.impl.ShipmentService;
import com.logistimo.utils.LocalDateUtil;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

import java.text.SimpleDateFormat;
import java.util.Collections;
import java.util.Date;
import java.util.Locale;
import java.util.ResourceBundle;

/**
 * Created by kumargaurav on 04/10/16.
 */
@Controller
@RequestMapping("/conversation")
public class ConversationController {

  private static final XLog xLogger = XLog.getLog(ConversationController.class);

  private ConversationBuilder conversationBuilder;
  private MessageBuilder messageBuilder;
  private ConversationService conversationService;
  private ShipmentService shipmentService;
  private OrderManagementService orderManagementService;

  @Autowired
  private ReturnsService returnsService;

  @Autowired
  public void setConversationBuilder(ConversationBuilder conversationBuilder) {
    this.conversationBuilder = conversationBuilder;
  }

  @Autowired
  public void setMessageBuilder(MessageBuilder messageBuilder) {
    this.messageBuilder = messageBuilder;
  }

  @Autowired
  public void setConversationService(ConversationService conversationService) {
    this.conversationService = conversationService;
  }

  @Autowired
  public void setShipmentService(ShipmentService shipmentService) {
    this.shipmentService = shipmentService;
  }

  @Autowired
  public void setOrderManagementService(OrderManagementService orderManagementService) {
    this.orderManagementService = orderManagementService;
  }

  @RequestMapping(value = "/", method = RequestMethod.POST)
  public
  @ResponseBody
  ConversationModel addEditConversation(@RequestBody final ConversationModel conversation,
                                        @RequestParam(required = false, defaultValue = "false") boolean update) {
    SecureUserDetails sUser = SecurityUtils.getUserDetails();
    Locale locale = sUser.getLocale();
    ResourceBundle backendMessages = Resources.getBundle(locale);
    Long domainId = sUser.getCurrentDomainId();
    ConversationModel model;
    try {
      //setting domain id if client has not passed it
      if (null == conversation.domainId && null != domainId) {
        conversation.domainId = domainId;
      }
      IConversation conv = conversationBuilder.buildConversation(conversation, !update);
      conv = conversationService.addEditConversation(conv, !update);
      model = conversationBuilder.buildModel(conv, sUser);

    } catch (ServiceException e) {
      xLogger.warn("Error while creating conversation {0}", conversation, e);
      if (!update) {
        throw new InvalidServiceException(backendMessages.getString("conversation.create.error"));
      } else {
        throw new InvalidServiceException(backendMessages.getString("conversation.update.error"));
      }
    } catch (Exception e) {
      xLogger.severe("Error while creating conversation {0}", conversation, e);
      if (!update) {
        throw new InvalidServiceException(
            backendMessages.getString("conversation.create.error") + ": " + backendMessages
                .getString("error.systemerror"));
      } else {
        throw new InvalidServiceException(
            backendMessages.getString("conversation.update.error") + ": " + backendMessages
                .getString("error.systemerror"));
      }
    }
    return model;
  }

  @RequestMapping(value = "/", method = RequestMethod.GET)
  public
  @ResponseBody
  ConversationModel getConversation(@RequestParam(required = true) String conversationId) {
    SecureUserDetails sUser = SecurityUtils.getUserDetails();
    IConversation conv;
    try {
      conv = conversationService.getConversationById(conversationId);
    } catch (Exception e) {
      xLogger.warn("Error while creating getting conversion with id {0}", conversationId, e);
      throw new InvalidServiceException(e);
    }

    return null == conv ? null : conversationBuilder.buildModel(conv, sUser);
  }

  @RequestMapping(value = "/message", method = RequestMethod.POST)
  public
  @ResponseBody
  MessageModel addEditMessage(@RequestBody MessageModel model,
                              @RequestParam(required = false, defaultValue = "false") boolean update) {
    SecureUserDetails sUser = SecurityUtils.getUserDetails();
    Locale locale = sUser.getLocale();
    ResourceBundle backendMessages = Resources.getBundle(locale);
    MessageModel retmodel;
    try {
      IMessage message = messageBuilder.buildMessage(model, sUser.getUsername(), !update);
      message = conversationService.addEditMessage(message, !update);
      retmodel = messageBuilder.buildModel(message);

    } catch (ServiceException e) {
      xLogger.warn("Error while creating message {0}", model, e);
      if (!update) {
        throw new InvalidServiceException(
            backendMessages.getString("conversation.message.create.error"));
      } else {
        throw new InvalidServiceException(
            backendMessages.getString("conversation.message.update.error"));
      }
    } catch (Exception e) {
      xLogger.severe("Error while creating message {0}", model, e);
      if (!update) {
        throw new InvalidServiceException(
            backendMessages.getString("conversation.message.create.error") + ": " + backendMessages
                .getString("error.systemerror"));
      } else {
        throw new InvalidServiceException(
            backendMessages.getString("conversation.message.update.error") + ": " + backendMessages
                .getString("error.systemerror"));
      }
    }
    return retmodel;
  }

  @RequestMapping(value = "/messages", method = RequestMethod.GET)
  public
  @ResponseBody
  Results getMessages(@RequestParam(required = false) String conversationId,
                      @RequestParam(required = false) String objType,
                      @RequestParam(required = false) String objId,
                      @RequestParam(required = false) boolean cnt,
                      @RequestParam(defaultValue = PageParams.DEFAULT_SIZE_STR) int size,
                      @RequestParam(defaultValue = PageParams.DEFAULT_OFFSET_STR) int offset) {
    SecureUserDetails sUser = SecurityUtils.getUserDetails();
    Results res;
    try {
      PageParams pageParams = new PageParams(offset, size);
      if (cnt) {
        res = conversationService.getMessagesCount(conversationId, objType, objId, pageParams);
      } else {
        res = conversationService.getMessages(conversationId, objType, objId, pageParams);
      }
      if (res != null && res.getResults() != null) {
        SimpleDateFormat sdf = new SimpleDateFormat(Constants.DATETIME_FORMAT);
        for (Object o : res.getResults()) {
          MessageModel model = (MessageModel) o;
          if (model.createDate != null) {
            Date cd = sdf.parse(model.createDate);
            model.createDate = LocalDateUtil.format(cd, sUser.getLocale(), sUser.getTimezone());
          }
          if (model.updateDate != null) {
            Date ud = sdf.parse(model.updateDate);
            model.updateDate = LocalDateUtil.format(ud, sUser.getLocale(), sUser.getTimezone());
          }
        }
      }
    } catch (Exception e) {
      xLogger.severe("Error while getting message for conversation id {0}", conversationId, e);
      throw new InvalidServiceException(e);
    }
    return res;
  }

  @RequestMapping(value = "/messages/tag", method = RequestMethod.GET)
  public
  @ResponseBody
  Results getMessagesByTag(@RequestParam(required = true) String tag,
                           @RequestParam(defaultValue = PageParams.DEFAULT_SIZE_STR) int size,
                           @RequestParam(defaultValue = PageParams.DEFAULT_OFFSET_STR) int offset) {
    try {
      PageParams pageParams = new PageParams(offset, size);
      return conversationService.getMessagesByTags(tag, pageParams);
    } catch (Exception e) {
      xLogger.severe("Error while getting message for TAGS {0}", tag, e);
      throw new InvalidServiceException(e);
    }
  }

  @RequestMapping(value = "/message/{objType}/{objId}", method = RequestMethod.POST)
  public
  @ResponseBody
  MessageModel addMessage(@PathVariable String objType, @PathVariable String objId,
                          @RequestBody StringRequestObj message) {
    SecureUserDetails user = SecurityUtils.getUserDetails();
    try {
      IMessage iMessage;
      if ("ORDER".equals(objType)) {
        iMessage = orderManagementService.addMessageToOrder(Long.valueOf(objId), message.data, user.getUsername());
      } else if ("SHIPMENT".equals(objType)) {
        iMessage = shipmentService.addMessage(objId, message.data, user.getUsername());
      } else if ("APPROVAL".equals(objType)) {
        iMessage = conversationService.addMsgToConversation(objType, objId, message.data, user.getUsername(),
            Collections.singleton(objType + objId), user.getDomainId(), null);
      } else if ("RETURNS".equals(objType)) {
        String messageId =
            returnsService.addComment(Long.parseLong(objId), message.data, user.getUsername(),
                user.getCurrentDomainId());
        iMessage=conversationService.getMessageById(messageId);
      } else {
        throw new InvalidDataException("Unrecognised object type " + objType);
      }
      return new MessageBuilder().buildModel(iMessage);
    } catch (ObjectNotFoundException e) {
      xLogger.warn("Failed to find {1} Id {0}", objId, objType, e);
      throw new InvalidDataException(objType + " : " + objId + " does not exist");
    } catch (Exception e) {
      xLogger.severe("Failed to add message to object", e);
      throw new InvalidServiceException("Failed to add message to object");
    }
  }

  @ResponseBody
  @RequestMapping(value = "/add_message/{objectType}/{objectId}", method = RequestMethod.POST)
  public MessageModel addMessageWithUserID(@PathVariable String objectType,
                                           @PathVariable String objectId,
                                           @RequestBody StringRequestObj message) {
    try {
      IMessage iMessage = conversationService.addMsgToConversation(objectType, objectId, message.data,
          message.userId, Collections.singleton(objectType + objectId), message.domainId, null);
      return new MessageBuilder().buildModel(iMessage);
    } catch (Exception e) {
      xLogger.severe("Failed to add message to object", e);
      throw new InvalidServiceException("Failed to add message to object");
    }
  }

}
