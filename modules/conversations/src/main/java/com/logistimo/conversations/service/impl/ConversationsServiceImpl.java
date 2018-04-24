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

package com.logistimo.conversations.service.impl;

import com.logistimo.conversations.entity.jpa.Conversation;
import com.logistimo.conversations.entity.jpa.ConversationTag;
import com.logistimo.conversations.entity.jpa.Message;
import com.logistimo.jpa.Repository;
import org.apache.commons.collections.CollectionUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

/**
 * Created by pratheeka on 24/04/18.
 */

@Service
public class ConversationsServiceImpl {


  @Autowired
  private Repository repository;


  public String addMessageToConversation(String objectType, String objectId, String msg,
                                         String updatingUserId,
                                         Set<String> tags, Long domainId, Date date){
    Map<String, Object> filters = new HashMap<>(2);
    filters.put("objectType", objectType);
    filters.put("objectId", objectId);
    Conversation conversation = repository.find("Conversation.getId", filters);
    Date createdDate = date;
    if (null == createdDate) {
      createdDate = new Date();
    }
    if (null == conversation) {

      conversation =
          saveConversation(objectType, objectId, updatingUserId, tags, domainId, createdDate);
      String conversationId = conversation.getId();
      if (CollectionUtils.isNotEmpty(tags)) {
        tags.forEach(tag -> saveConversationTag(conversationId, tag));
      }
    }

    Message message = saveMessage(msg, updatingUserId, conversation, createdDate);

    return message.getMessageId();

  }

  public Conversation saveConversation(String objectType, String objectId, String updatingUserId,
                                        Set<String> tags, Long domainId, Date createdDate) {
    Conversation conversation = new Conversation();
    conversation.setObjectId(objectId);
    conversation.setObjectType(objectType);
    conversation.setDomainId(domainId);
    conversation.setTags(tags);
    conversation.setCreateDate(createdDate);
    conversation.setUserId(updatingUserId);
    repository.save(conversation);
    return conversation;
  }

  public void saveConversationTag(String conversationId, String tag) {
    ConversationTag conversationTag = new ConversationTag();
    conversationTag.setConversationId(conversationId);
    conversationTag.setTag(tag);
    repository.save(conversationTag);
  }

  public Message saveMessage(String msg, String updatingUserId, Conversation conversation,
                              Date createdDate) {
    Message message = new Message();
    message.setConversationId(conversation.getId());
    message.setCreateDate(createdDate);
    message.setMessage(msg);
    message.setUserId(updatingUserId);
    message.setConversationId(conversation.getId());
    repository.save(message);
    return message;
  }
}
