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

package com.logistimo.api.controllers;

import com.logistimo.auth.utils.SecurityUtils;
import com.logistimo.communications.actions.NotifyEventUpdateAction;
import com.logistimo.communications.actions.ScheduleEventUpdateAction;
import com.logistimo.services.ServiceException;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

/**
 * Created by kumargaurav on 05/09/18.
 */
@Controller
@RequestMapping("/notifications")
public class NotifierController {

  private ScheduleEventUpdateAction scheduleEventUpdateAction;

  private NotifyEventUpdateAction notifyEventUpdateAction;

  @Autowired
  public void setScheduleEventUpdateAction(
      ScheduleEventUpdateAction scheduleEventUpdateAction) {
    this.scheduleEventUpdateAction = scheduleEventUpdateAction;
  }

  @Autowired
  public void setNotifyEventUpdateAction(
      NotifyEventUpdateAction notifyEventUpdateAction) {
    this.notifyEventUpdateAction = notifyEventUpdateAction;
  }

  @RequestMapping(value = "/schedule-eventupdate", method = RequestMethod.GET)
  public
  @ResponseBody
  void scheduleEventUpdate() throws ServiceException {
    scheduleEventUpdateAction.invoke();
  }

  @RequestMapping(value = "/execute-eventupdate", method = RequestMethod.GET)
  public
  @ResponseBody
  void executeEventUpdate(@RequestParam(value = "domain_id") Long domainId) throws ServiceException {
    SecurityUtils.authorizeAdminTask();
    notifyEventUpdateAction.invoke(domainId);
  }

}
