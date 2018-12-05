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

package com.logistimo.twofactorauthentication.service;

/**
 * @author smriti
 */

import com.logistimo.communications.service.EmailManager;
import com.logistimo.communications.service.MessageService;
import com.logistimo.communications.service.TemplateEmailTask;
import com.logistimo.jpa.Repository;
import com.logistimo.logger.XLog;
import com.logistimo.twofactorauthentication.entity.UserDevices;
import com.logistimo.twofactorauthentication.vo.UserDevicesVO;
import com.logistimo.users.entity.IUserAccount;
import com.logistimo.users.service.UsersService;
import com.logistimo.utils.CommonUtils;
import com.logistimo.utils.TwoFactorAuthenticationUtil;

import org.apache.commons.lang.text.StrSubstitutor;
import org.modelmapper.ModelMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.EnableTransactionManagement;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collections;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.ResourceBundle;


@Service
@EnableTransactionManagement
public class TwoFactorAuthenticationService {

  @Autowired
  private Repository repository;

  @Autowired
  UsersService usersService;

  private static final XLog xLogger = XLog.getLog(TwoFactorAuthenticationService.class);

  private ModelMapper modelMapper = new ModelMapper();

  @Transactional(transactionManager = "transactionManager")
  public void createUserDevices(UserDevicesVO userDevicesVO)
      throws Exception {
    UserDevices userDevices = modelMapper.map(userDevicesVO, UserDevices.class);
    repository.save(userDevices);
    xLogger.info("Device information successfully added for user: {0} ",
        userDevices.getUserId());

    sendNewDeviceLoginInfo(userDevicesVO.getUserId(), userDevicesVO.getApplicationName());
  }

  public UserDevices getDeviceInformation(String id, String userId) {
    String key = TwoFactorAuthenticationUtil.generateUserDeviceKey(userId, id);
    return repository.findById(UserDevices.class, key);
  }

  private void sendNewDeviceLoginInfo(String userId, Integer applicationId)
      throws Exception {
    IUserAccount userAccount = usersService.getUserAccount(userId);
    sendMessage(userAccount, applicationId);
    sendEmail(userAccount, applicationId);
  }

  private void sendMessage(IUserAccount userAccount, Integer applicationId)
      throws Exception {

    if (userAccount == null) {
      return;
    }

    MessageService ms = MessageService.getInstance("sms", userAccount.getCountry(), true,
        userAccount.getDomainId(), userAccount.getFirstName(), null);
    String message = getMessage(applicationId, userAccount.getLocale());
    ms.send(userAccount, message, MessageService.getMessageType(message), null, null, null);
    xLogger
        .info("Login from a new device info is sent to {0} through SMS", userAccount.getUserId());
  }

  private void sendEmail(IUserAccount userAccount, Integer applicationId) {

    if (userAccount.getEmail() == null) {
      return;
    }

    ResourceBundle
        resourceBundle =
        ResourceBundle.getBundle("BackendMessages", userAccount.getLocale());
    String emailSubject = resourceBundle.getString("new.device.login.info.email.subject");

    Map<String, Object> emailBodyAttributes = new HashMap<>();
    emailBodyAttributes.put("dear", resourceBundle.getString("password.reset.info.user.name"));
    emailBodyAttributes.put("user", userAccount.getFirstName());
    emailBodyAttributes.put("body", getMessage(applicationId, userAccount.getLocale()));
    emailBodyAttributes.put("confidentialityNotice",
        resourceBundle.getString("password.reset.confidentiality.notice"));

    TemplateEmailTask emailTask =
        new TemplateEmailTask("TwoFactorAuthentication.vm", emailBodyAttributes,
            emailSubject, userAccount.getEmail(), userAccount.getDomainId());
    EmailManager.enqueueEmailTask(emailTask);

    xLogger
        .info("Login from a new device info is sent to {0} through mail", userAccount.getUserId());
  }

  private String getMessage(Integer applicationId, Locale locale) {
    ResourceBundle
        resourceBundle =
        ResourceBundle.getBundle("BackendMessages", locale);

    Map<String, String>
        messageParameters =
        Collections.singletonMap("appName",
            CommonUtils.getAppName(applicationId, locale));

    return new StrSubstitutor(messageParameters)
        .replace(resourceBundle.getString("login.from.new.device.message"));
  }


}
