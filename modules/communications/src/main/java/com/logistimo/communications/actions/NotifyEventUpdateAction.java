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

package com.logistimo.communications.actions;

import com.logistimo.AppFactory;
import com.logistimo.communication.common.model.MetaTag;
import com.logistimo.communication.common.model.NotificationContent;
import com.logistimo.communication.common.model.NotificationRequest;
import com.logistimo.communication.common.model.NotificationType;
import com.logistimo.communication.common.model.Subscriber;
import com.logistimo.communications.commands.GetEventSummaryCommand;
import com.logistimo.communications.models.EventSummary;
import com.logistimo.communications.models.EventSummaryResponse;
import com.logistimo.config.models.DomainConfig;
import com.logistimo.config.models.EventSummaryConfigModel;
import com.logistimo.constants.CharacterConstants;
import com.logistimo.logger.XLog;
import com.logistimo.services.Resources;
import com.logistimo.users.models.ExtUserAccount;
import com.logistimo.users.service.UsersService;

import org.apache.camel.ExchangePattern;
import org.apache.camel.ProducerTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestTemplate;

import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.ResourceBundle;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

/**
 * Created by kumargaurav on 05/09/18.
 */
@Component
public class NotifyEventUpdateAction {

  private static final XLog log = XLog.getLog(NotifyEventUpdateAction.class);

  private static final String SYSTEM_USER = "system";

  private static final String CRITICAL_INDICATORS = "critical";

  private static final String PERFORMANCE_INDICATORS = "performance";

  private RestTemplate restTemplate;

  private UsersService usersService;

  @Autowired
  @Qualifier("commRestTemplate")
  public void setRestTemplate(RestTemplate restTemplate) {
    this.restTemplate = restTemplate;
  }

  @Autowired
  public void setUsersService(UsersService usersService) {
    this.usersService = usersService;
  }

  public void invoke(Long domainId) {
    List<ExtUserAccount> users = eligibleRecipients(domainId);
    if(users == null || users.isEmpty()) {
      log.warn("No eligible user for event notifications for domain: {0}",domainId);
      return;
    }
    List<String> allowedEvents = applicableEvents(domainId);
    if(allowedEvents.isEmpty()) {
      log.warn("No critical or good event configured for domain: {0}",domainId);
      return;
    }
    EventSummaryResponse response = eventSummaries(domainId);
    if(response.getSummaries().isEmpty()) {
      log.warn("No event summaries for domain: {0}",domainId);
      return;
    }
    Locale locale = getLocale(getDomainLang(domainId));
    Date today = todayDate();
    List<EventSummary> summaries = response.getSummaries().stream()
        .filter(summary1 -> isNewEvent(eventDate(summary1.getTimestamp()), today, summary1.getType()))
        .filter(summary -> allowedEvents.contains(summary.getEventId()))
        .collect(Collectors.toList());
    enqueueNotification(summaries,users,locale);
  }


  private EventSummaryResponse eventSummaries(Long domainId) {
    return new GetEventSummaryCommand(restTemplate,domainId, SYSTEM_USER, getDomainLang(domainId)).execute();
  }

  protected boolean isNewEvent(Date dateOfEvent, Date today, String type) {
    if(dateOfEvent == null) {
      return Boolean.FALSE;
    }
    long diffInMillis = Math.abs(today.getTime() - dateOfEvent.getTime());
    long diff = TimeUnit.DAYS.convert(diffInMillis, TimeUnit.MILLISECONDS);
    if(type.equals(CRITICAL_INDICATORS) && diff <= 1l) {
      return Boolean.TRUE;
    }
    if(type.equals(PERFORMANCE_INDICATORS) && diff <= 2l) {
      return Boolean.TRUE;
    }
    return Boolean.FALSE;
  }

  protected Date todayDate() {
    Calendar c = Calendar.getInstance();
    c.set(Calendar.HOUR_OF_DAY,0);
    c.set(Calendar.MINUTE,0);
    c.set(Calendar.SECOND,0);
    c.set(Calendar.MILLISECOND,0);
    return c.getTime();
  }

  protected Date eventDate(String timestamp) {
    DateFormat df = new SimpleDateFormat("yyyy-MM-dd");
    try {
      return df.parse(timestamp);
    } catch (ParseException ex) {
      return null;
    }
  }


  private List<String> applicableEvents(Long domainId) {
    EventSummaryConfigModel eventsConfig = domainConfig(domainId).getEventSummaryConfig();
    if(eventsConfig.getEvents() == null || eventsConfig.getEvents().isEmpty()) {
      return Collections.EMPTY_LIST;
    }
    return eventsConfig.getEvents().stream()
        .filter(event -> (event.getNotification() != null && event.getNotification()))
        .flatMap(ev -> ev.getThresholds().stream())
        .map(threshold -> threshold.getId())
        .collect(Collectors.toList());
  }

  private String getDomainLang(Long domainId) {
    return domainConfig(domainId).getLangPreference();
  }

  protected DomainConfig domainConfig(Long domainId) {
    return DomainConfig.getInstance(domainId);
  }

  private Locale getLocale(String lang) {
    Locale locale;
    if (StringUtils.isEmpty(lang)) {
      locale = Locale.ENGLISH;
    } else {
      locale = new Locale(lang);
    }
    return locale;
  }

  private List<ExtUserAccount> eligibleRecipients(Long domainId) {
    return usersService.eligibleUsersForEventNotification(domainId);
  }

  protected void enqueueNotification (List<EventSummary> summaries, List<ExtUserAccount> users, Locale locale) {

    ProducerTemplate camelTemplate =
        AppFactory.get().getTaskService().getContext()
            .getBean("camel-client", ProducerTemplate.class);

    for (EventSummary summary : summaries) {
      for (ExtUserAccount user: users) {
        camelTemplate.sendBody("direct:notification", ExchangePattern.InOnly, notificationPayload(summary,user,locale));
      }
    }
  }

  public NotificationRequest notificationPayload(EventSummary summary, ExtUserAccount user,Locale locale) {
    NotificationRequest request = new NotificationRequest();
    request.setApp("logistimo");
    request.setNotificationType(NotificationType.PUSH);
    Subscriber subscriber = new Subscriber();
    subscriber.setUserId(user.getUserId());
    subscriber.setPtoken(user.getToken());
    subscriber.setEmail(user.getEmail());
    subscriber.setPhone(user.getMobilePhoneNumber());
    request.setSubscriber(subscriber);
    NotificationContent content = new NotificationContent();
    content.setType(getResourceBundleMessage("event.summary",locale));
    content.setSubtype(summary.getType() + CharacterConstants.HASH + summary.getCategory() + CharacterConstants.HASH + summary.getEventType());
    String groupTitle;
    if(summary.getType().equals("critical")) {
      groupTitle = getResourceBundleMessage("critical.events",locale);
    } else {
      groupTitle = getResourceBundleMessage("good.indicators",locale);
    }
    content.setGroupTitle(groupTitle);
    content.setTitle(summary.getTitle());
    content.setText(summary.getText());
    content.setAction("INFO");
    request.setContent(content);
    MetaTag tag = new MetaTag();
    tag.setRetry(3);
    request.setTag(tag);
    return request;
  }

  private String getResourceBundleMessage(String key, Locale locale) {
    ResourceBundle resourceBundle = Resources.get().getBundle("Messages", locale);
    return resourceBundle.getString(key);
  }

}


