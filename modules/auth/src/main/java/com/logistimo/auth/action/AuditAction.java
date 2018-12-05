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

package com.logistimo.auth.action;

import com.logistimo.AppFactory;
import com.logistimo.auth.entity.UserAuth;
import com.logistimo.auth.repository.UserAuthRepository;
import com.logistimo.logger.XLog;
import com.logistimo.models.AuthRequest;
import com.logistimo.models.users.LoginStatus;
import com.logistimo.models.users.UserLoginHistoryModel;
import com.logistimo.services.taskqueue.ITaskService;
import com.logistimo.utils.GsonUtils;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.Date;

/**
 * Created by kumargaurav on 14/08/18.
 */
@Component
public class AuditAction {

  private static final String LOGUSER_TASK_URL = "/s2/api/users/update/loginhistory";
  private static final XLog xLogger = XLog.getLog(AuditAction.class);

  @Autowired
  public UserAuthRepository userAuthRepository;

  public void invoke(AuthRequest auditModel, LoginStatus loginStatus) {
    if(LoginStatus.SUCCESS.equals(loginStatus)) {
      updateLoginActivity(auditModel);
    }
    raiseAuditEvent(auditModel, loginStatus);
  }

  private void raiseAuditEvent(AuthRequest auditModel, LoginStatus loginStatus) {

    UserLoginHistoryModel
        ulh =
        new UserLoginHistoryModel(auditModel.getUserId(), auditModel.getLoginSource(),
            auditModel.getUserAgent(),
            auditModel.getIpAddress(), auditModel.getLoginTime(), auditModel.getSourceVersion(),
            loginStatus, auditModel.getReferer());
    try {
      AppFactory.get().getTaskService()
          .schedule(ITaskService.QUEUE_DEFAULT, LOGUSER_TASK_URL, GsonUtils.toJson(ulh));
    } catch (Exception e) {
      xLogger.warn("Failed to schedule job for creating user audit history ", e);
    }
  }

  private void updateLoginActivity(AuthRequest auditModel) {

    UserAuth userAuth = userAuthRepository.findOne(auditModel.getUserId());
    userAuth.setPrevUserAgent(auditModel.getPrevUserAgent());
    userAuth.setLastLogin(auditModel.getLoginTime());
    userAuth.setUserAgent(auditModel.getUserAgent());
    userAuth.setLoginSource(auditModel.getLoginSource());
    userAuth.setIpAddress(auditModel.getIpAddress());
    userAuth.setVersion(auditModel.getSourceVersion());
    userAuthRepository.save(userAuth);
  }
}
