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

package com.logistimo.orders.approvals.utils;

import com.codahale.metrics.Meter;
import com.logistimo.communications.MessageHandlingException;
import com.logistimo.communications.service.MessageService;
import com.logistimo.config.models.DomainConfig;
import com.logistimo.constants.Constants;
import com.logistimo.context.StaticApplicationContext;
import com.logistimo.entities.entity.IKiosk;
import com.logistimo.entities.service.EntitiesService;
import com.logistimo.entities.service.EntitiesServiceImpl;
import com.logistimo.logger.XLog;
import com.logistimo.orders.approvals.dao.IApprovalsDao;
import com.logistimo.orders.entity.IOrder;
import com.logistimo.orders.entity.approvals.IOrderApprovalMapping;
import com.logistimo.orders.service.OrderManagementService;
import com.logistimo.orders.service.impl.OrderManagementServiceImpl;
import com.logistimo.services.ObjectNotFoundException;
import com.logistimo.services.Resources;
import com.logistimo.services.ServiceException;
import com.logistimo.services.impl.PMF;
import com.logistimo.users.entity.IUserAccount;
import com.logistimo.users.service.UsersService;
import com.logistimo.users.service.impl.UsersServiceImpl;
import com.logistimo.utils.LocalDateUtil;
import com.logistimo.utils.MetricsUtil;

import org.apache.camel.Handler;
import org.apache.commons.lang.text.StrSubstitutor;

import java.io.IOException;
import java.util.Date;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.ResourceBundle;
import java.util.concurrent.TimeUnit;

import javax.jdo.PersistenceManager;

/**
 * Created by nitisha.khandelwal on 02/06/17.
 */

public class ApprovalStatusUpdateEventProcessor {

  private static final String ORDER = "order";
  private static final String APPROVED_STATUS = "ap";
  private static final String REJECTED_STATUS = "rj";
  private static final String CANCELLED_STATUS = "cn";
  private static final String EXPIRED_STATUS = "ex";

  private static Meter jmsMeter = MetricsUtil
      .getMeter(ApprovalStatusUpdateEventProcessor.class, "approvalStatusUpdateEventMeter");
  private static final XLog xLogger = XLog.getLog(ApprovalStatusUpdateEventProcessor.class);

  @Handler
  public void execute(ApprovalStatusUpdateEvent event) throws ServiceException {

    jmsMeter.mark();
    xLogger.info("Approval status update event received - {0}", event);

    if (ORDER.equalsIgnoreCase(event.getType())) {

      UsersService usersService = StaticApplicationContext.getBean(UsersServiceImpl.class);
      EntitiesService entitiesService = StaticApplicationContext.getBean(EntitiesServiceImpl.class);
      OrderManagementService orderManagementService = StaticApplicationContext.getBean(
          OrderManagementServiceImpl.class);
      PersistenceManager pm = PMF.get().getPersistenceManager();

      try {
        IApprovalsDao approvalsDao = StaticApplicationContext.getBean(IApprovalsDao.class);
        IOrderApprovalMapping orderApprovalMapping = approvalsDao
            .getOrderApprovalMapping(event.getApprovalId());
        IOrder order = orderManagementService.getOrder(orderApprovalMapping.getOrderId());

        if (APPROVED_STATUS.equals(event.getStatus())) {
          setApprovalResponseTime(event.getUpdatedAt(), orderApprovalMapping.getApprovalType(),
              order);
        }

        pm.makePersistent(order);
        approvalsDao.updateOrderApprovalStatus(Long.valueOf(event.getTypeId()),
            event.getApprovalId(), event.getStatus());

        IUserAccount requester = usersService.getUserAccount(event.getRequesterId());
        IUserAccount updatedBy = usersService.getUserAccount(event.getUpdatedBy());
        IKiosk kiosk = entitiesService.getKiosk(orderApprovalMapping.getKioskId());

        MessageService messageService = MessageService.getInstance(MessageService.SMS,
            requester.getCountry(), true, kiosk.getDomainId(), Constants.SYSTEM_USER_ID, null);
        String resolvedMessage =
            getMessage(event, orderApprovalMapping, requester, kiosk, updatedBy);
        messageService.send(requester, resolvedMessage,
            MessageService.getMessageType(resolvedMessage), null, null, null);
        for (String approverId : event.getApproverIds()) {
          IUserAccount approver = usersService.getUserAccount(approverId);
          messageService.send(approver, resolvedMessage,
              MessageService.getMessageType(resolvedMessage), null, null, null);
        }

      } catch (ObjectNotFoundException e) {
        xLogger.warn("Object not found - ", e);
      } catch (IOException e) {
        xLogger.warn("Error in sending message - ", e);
      } catch (MessageHandlingException e) {
        xLogger.warn("Error in building message status - ", e);
      } catch (Exception e) {
        xLogger.warn("Error in handling approval message - ", e);
      } finally {
        pm.close();
      }
    }
  }

  private void setApprovalResponseTime(Date eventUpdateTime, Integer approvalType, IOrder order) {
    if (IOrder.TRANSFER_ORDER == approvalType) {
      order.setTransferApprovalResponseTime(TimeUnit.MILLISECONDS.toSeconds(
          eventUpdateTime.getTime() - order.getCreatedOn().getTime()));
    }
    if (IOrder.PURCHASE_ORDER == approvalType) {
      order.setPurchaseApprovalResponseTime(TimeUnit.MILLISECONDS.toSeconds(
          eventUpdateTime.getTime() - order.getCustomerVisibilityTime().getTime()));
    }
    if (IOrder.SALES_ORDER == approvalType) {
      order.setSalesApprovalResponseTime(TimeUnit.MILLISECONDS.toSeconds(
          eventUpdateTime.getTime() - order.getVendorVisibilityTime().getTime()));
    }
  }

  private String getMessage(ApprovalStatusUpdateEvent event, IOrderApprovalMapping orderApproval,
      IUserAccount requester, IKiosk kiosk, IUserAccount updatedBy) {

    String message = getMessage(event.getStatus(), requester);
    Map<String, String> values = new HashMap<>();
    values.put("approvalType", ApprovalUtils.getApprovalType(orderApproval.getApprovalType()));
    values.put("requestorName", requester.getFullName());
    values.put("requestorPhone", requester.getMobilePhoneNumber());
    values.put("eName", kiosk.getName());
    values.put("eCity", kiosk.getCity());
    values.put("orderId", event.getTypeId());
    values.put("statusChangedTime", LocalDateUtil.format(event.getUpdatedAt(),
        requester.getLocale(), requester.getTimezone()));
    values.put("updatedBy", updatedBy.getFullName());
    values.put("updatedByPhone", updatedBy.getMobilePhoneNumber());

    StrSubstitutor sub = new StrSubstitutor(values);

    return sub.replace(message);
  }

  private String getMessage(String status, IUserAccount requester) {
    String message;
    DomainConfig domainConfig = DomainConfig.getInstance(requester.getDomainId());
    ResourceBundle
        messages =
        Resources.getBundle(new Locale(domainConfig.getLangPreference()));
    switch (status) {
      case APPROVED_STATUS:
        message = messages.getString("approval.approved.message");
        break;
      case REJECTED_STATUS:
        message = messages.getString("approval.rejected.message");
        break;
      case CANCELLED_STATUS:
        message = messages.getString("approval.cancelled.message");
        break;
      case EXPIRED_STATUS:
        message = messages.getString("approval.expired.message");
        break;
      default:
        message = messages.getString("approval.status.general.message");
    }
    return message;
  }
}
