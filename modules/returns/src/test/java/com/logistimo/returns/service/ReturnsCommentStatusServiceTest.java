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

import com.logistimo.activity.service.impl.ActivitiesServiceImpl;
import com.logistimo.conversations.service.impl.ConversationsServiceImpl;
import com.logistimo.returns.Status;
import com.logistimo.returns.utility.ReturnsTestUtility;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.powermock.modules.junit4.PowerMockRunner;

import java.io.IOException;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

/**
 * Created by pratheeka on 11/07/18.
 */
@RunWith(PowerMockRunner.class)
public class ReturnsCommentStatusServiceTest {

  @InjectMocks
  private ReturnsCommentService returnsCommentStatusService;

  @InjectMocks
  private ReturnsStatusHistoryService returnsStatusHistoryService;

  @Mock
  private ConversationsServiceImpl conversationService;

  @Mock
  private ActivitiesServiceImpl activityService;

  @Test
  public void testPostComment(){
    when(conversationService.addMessageToConversation(any(),any(),any(),any(),any(),any(),any())).thenReturn(null);
    returnsCommentStatusService.postComment(1L,"Comment","TEST_USER",1L);
  }

  @Test
  public void testPostCommentForNull(){
    when(conversationService.addMessageToConversation(any(),any(),any(),any(),any(),any(),any())).thenReturn(null);
    returnsCommentStatusService.postComment(1L,null,"TEST_USER",1L);
  }

  @Test
  public void testAddStatusHistory() throws IOException {
    when(activityService.saveActivity(any())).thenReturn(null);
    returnsStatusHistoryService.addStatusHistory(ReturnsTestUtility.getReturnsVO(),null, Status.RECEIVED,"Message");
  }
}
