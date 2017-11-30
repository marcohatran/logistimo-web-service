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

package com.logistimo.exports.util;

import com.logistimo.communications.MessageHandlingException;
import com.logistimo.communications.service.EmailService;
import com.logistimo.communications.service.MessageService;
import com.logistimo.entity.IJobStatus;
import com.logistimo.services.utils.ConfigUtil;
import com.logistimo.users.entity.IUserAccount;
import com.logistimo.users.service.UsersService;
import com.logistimo.utils.StringUtil;

import org.apache.commons.lang.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * @author Mohan Raja
 */
@Component
public class EmailHelper {

  UsersService us;

  @Autowired
  private void setUserService(UsersService us) {
    this.us = us;
  }

  public void sendMail(IJobStatus jobStatus, String filePath, String fileName, String customMessage)
      throws MessageHandlingException, IOException {
    Map<String, String> model = jobStatus.getMetadataMap();
    IUserAccount u = us.getUserAccount(model.get("userId"));
    String subject = "Export Subject";
    List<String> userIds = StringUtil.getList(u.getEmail());
    List<String> addresses = new ArrayList<>();
    String[] addressArray = userIds.toArray(new String[userIds.size()]);
    Collections.addAll(addresses, addressArray);
    String message;
    if(StringUtils.isNotBlank(customMessage)){
      message = customMessage;
    } else {
      String host = ConfigUtil.get("logi.host.server");
      String path = host == null ? "http://localhost:50070/webhdfs/v1" : "/media";
      String localStr = host == null ? "?op=OPEN" : "";
      message = "<br/><br/>Please <a href=\"" + (host == null ? "" : host) + path
          + "/user/logistimoapp/dataexport/" + filePath + "/" + fileName + localStr
          + "\">click here</a> to download the file.";
    }
    MessageService ms =
        MessageService.getInstance(MessageService.EMAIL, u.getCountry(), true, u.getDomainId(),
            u.getUserId(), null);
    ((EmailService) ms).sendHTML(null, addresses, subject, message, null);
  }
}
