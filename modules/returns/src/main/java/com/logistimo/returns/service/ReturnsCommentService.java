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
import com.logistimo.conversations.service.impl.ConversationsServiceImpl;

import org.apache.commons.lang.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.EnableTransactionManagement;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collections;

/**
 * Created by pratheeka on 25/06/18.
 */
@Service
@EnableTransactionManagement
class ReturnsCommentService {

  @Autowired
  private ConversationsServiceImpl conversationService;

  public String addComment(Long returnId, String message, String userId, Long domainId) {
    if (StringUtils.isNotBlank(message)) {
      return conversationService
          .addMessageToConversation(IActivity.TYPE.RETURNS.name(), String.valueOf(returnId),
              message, userId, Collections.singleton("RETURNS:" + returnId), domainId, null);
    }
    return null;
  }

  @Transactional(transactionManager = "transactionManager")
  public String postComment(Long returnId, String message, String userId, Long domainId) {
    return addComment(returnId, message, userId, domainId);
  }

}
