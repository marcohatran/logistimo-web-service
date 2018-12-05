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

package com.logistimo.api.builders;

import com.logistimo.config.entity.IConfig;
import com.logistimo.config.models.ConfigurationException;
import com.logistimo.config.models.GeneralConfig;
import com.logistimo.config.service.ConfigurationMgmtService;
import com.logistimo.services.ServiceException;
import com.logistimo.twofactorauthentication.vo.UserDevicesVO;
import com.logistimo.utils.TwoFactorAuthenticationUtil;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.Calendar;
import java.util.Date;

/**
 * @author smriti
 */
@Component
public class UserDevicesBuilder {
  private ConfigurationMgmtService configurationMgmtService;

  @Autowired
  public void setConfigurationMgmtService(ConfigurationMgmtService configurationMgmtService) {
    this.configurationMgmtService = configurationMgmtService;
  }

  public UserDevicesVO buildUserDevicesVO(String deviceId, String userId, Integer applicationName,
                                          Date twoFactorTokenGenerationTime)
      throws ServiceException, ConfigurationException {
    IConfig c = configurationMgmtService.getConfiguration(IConfig.GENERALCONFIG);
    GeneralConfig config = new GeneralConfig(c.getConfig());
    UserDevicesVO userDevicesVO = new UserDevicesVO();
    String key = TwoFactorAuthenticationUtil.generateUserDeviceKey(userId, deviceId);
    userDevicesVO.setId(key);
    userDevicesVO.setUserId(userId);
    userDevicesVO.setApplicationName(applicationName);
    Date expiryTime = computeExpiryTime(twoFactorTokenGenerationTime, config.getRememberDeviceInMinutes());
    userDevicesVO.setExpiresOn(expiryTime);
    userDevicesVO.setCreatedOn(twoFactorTokenGenerationTime);
    return userDevicesVO;
  }

  protected Date computeExpiryTime(Date twoFactorTokenGenerationTime, Integer rememberDeviceDuration) {
    Calendar calendar = Calendar.getInstance();
    calendar.setTime(twoFactorTokenGenerationTime);
    calendar.add(Calendar.MINUTE, rememberDeviceDuration);
    return calendar.getTime();
  }
}
