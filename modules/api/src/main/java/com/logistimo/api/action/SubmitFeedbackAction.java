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

package com.logistimo.api.action;

import com.logistimo.communications.service.EmailManager;
import com.logistimo.communications.service.TemplateEmailTask;
import com.logistimo.api.models.FeedbackModel;
import com.logistimo.config.models.ConfigurationException;
import com.logistimo.config.models.GeneralConfig;
import com.logistimo.constants.Constants;
import com.logistimo.domains.entity.IDomain;
import com.logistimo.domains.service.DomainsService;
import com.logistimo.entity.Feedback;
import com.logistimo.exception.ValidationException;
import com.logistimo.jpa.Repository;
import com.logistimo.logger.XLog;
import com.logistimo.services.ServiceException;
import com.logistimo.users.entity.IUserAccount;
import com.logistimo.users.service.UsersService;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;

/**
 * Created by kumargaurav on 19/07/18.
 */
@Component
public class SubmitFeedbackAction {

  private static final XLog xLogger = XLog.getLog(SubmitFeedbackAction.class);

  private static final String VELOCITY_TEMPLATE_PATH = "velocity/Feedback.vm";

  private UsersService usersService;

  private Repository repository;

  private DomainsService domainsService;

  @Autowired
  public void setUsersService(UsersService usersService) {
    this.usersService = usersService;
  }

  @Autowired
  public void setDomainsService(DomainsService domainsService) {
    this.domainsService = domainsService;
  }


  @Autowired
  public void setRepository(Repository repository) {
    this.repository = repository;
  }

  @Transactional
  public void invoke(FeedbackModel feedbackModel) throws ServiceException,ConfigurationException {

    validate(feedbackModel);
    setDomain(feedbackModel);
    saveFeedback(feedbackModel);
    sendEmail(feedbackModel);
    xLogger.info("Feedback submitted successfully with data {0}",feedbackModel);
  }

  protected void validate(FeedbackModel model) {
    if (StringUtils.isEmpty(model.getApp()) || StringUtils.isEmpty(model.getText())) {
      throw new ValidationException("Required feedback param missing");
    }
  }

  protected void setDomain(FeedbackModel model) throws ServiceException {
    IUserAccount account = usersService.getUserAccount(model.getUserId());
    model.setDomainId(account.getDomainId());
    IDomain domain = domainsService.getDomain(model.getDomainId());
    model.setDomain(domain.getName());
  }

  protected String getFeedbackAddress() throws ConfigurationException {
    //feedback receiver's email address
    GeneralConfig gc = GeneralConfig.getInstance();
    return gc.getFeedbackEmail();
  }

  protected String getEmailSubject(FeedbackModel model) throws ServiceException,ConfigurationException {
    return "[".concat(getAppName(model.getApp())).concat(" - feedback]");
  }

  protected void saveFeedback(FeedbackModel model) {
    Feedback feedback = new Feedback();
    feedback.setUserId(model.getUserId());
    feedback.setDomainId(model.getDomainId());
    feedback.setApp(model.getApp());
    feedback.setAppVersion(model.getAppVersion());
    feedback.setTitle(model.getTitle());
    feedback.setText(model.getText());
    feedback.setCreateDate(new Date());
    repository.save(feedback);
  }

  protected Map<String,Object> convertToMap(FeedbackModel model) throws ServiceException,ConfigurationException {

    Map<String,Object> attributes = new HashMap<>();
    IUserAccount account = usersService.getUserAccount(model.getUserId());
    attributes.put("userId", account.getUserId());
    attributes.put("domain", model.getDomain());
    attributes.put("userName", account.getFullName());
    attributes.put("message", model.getText());
    attributes.put("mPhone", account.getMobilePhoneNumber());
    String eMail = account.getEmail();
    if (eMail != null && !eMail.isEmpty()) {
      attributes.put("eMail", eMail);
    }
    attributes.put("appName", getAppName(model.getApp()));
    return attributes;
  }

  protected void sendEmail(FeedbackModel model) throws ServiceException, ConfigurationException {
    //feedback receiver's email address
    String address = getFeedbackAddress();
    //email subject
    String subject = getEmailSubject(model);
    //building email's message body
    Map<String, Object> attributes = convertToMap(model);
    //template email task
    TemplateEmailTask
        emailTask =
        new TemplateEmailTask(VELOCITY_TEMPLATE_PATH, attributes, subject, address,
            model.getDomainId());
    EmailManager.enqueueEmailTask(emailTask);
  }

  protected String getAppName(String appId) throws ConfigurationException {
    String appNameDisplay;
    GeneralConfig gc = GeneralConfig.getInstance();
    if(Constants.MMA_NAME.equals(appId)) {
      appNameDisplay = gc.getMonitoringAppFeedbackText();
    } else {
      appNameDisplay = gc.getStoreAppFeedbackText();
    }
    return appNameDisplay;
  }

}
