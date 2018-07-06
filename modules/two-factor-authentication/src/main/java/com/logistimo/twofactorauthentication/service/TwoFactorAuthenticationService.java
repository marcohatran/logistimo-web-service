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

import com.logistimo.jpa.Repository;
import com.logistimo.logger.XLog;
import com.logistimo.twofactorauthentication.entity.UserDevices;
import com.logistimo.twofactorauthentication.vo.UserDevicesVO;
import com.logistimo.utils.TwoFactorAuthenticationUtil;

import org.modelmapper.ModelMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.EnableTransactionManagement;
import org.springframework.transaction.annotation.Transactional;


@Service
@EnableTransactionManagement
public class TwoFactorAuthenticationService {

  @Autowired
  private Repository repository;

  private static final XLog xLogger = XLog.getLog(TwoFactorAuthenticationService.class);

  private ModelMapper modelMapper = new ModelMapper();

  @Transactional(transactionManager = "transactionManager")
  public void createUserDevices(UserDevicesVO userDevicesVO) {
    UserDevices userDevices = modelMapper.map(userDevicesVO, UserDevices.class);
    repository.save(userDevices);
    xLogger.info("Device information successfully added for user: {0} ",
        userDevices.getUserId());
  }

  public UserDevices getDeviceInformation(String id, String userId) {
    String key = TwoFactorAuthenticationUtil.generateUserDeviceKey(userId, id);
    return repository.findById(UserDevices.class, key);
  }


}
