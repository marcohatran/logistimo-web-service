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

package com.logistimo.social.util;

import com.google.gson.GsonBuilder;

import com.logistimo.config.models.DomainConfig;
import com.logistimo.config.models.EventSummaryConfigModel;
import com.logistimo.collaboration.core.models.ContextModel;

import java.util.List;
import java.util.Optional;

/**
 * Created by kumargaurav on 27/11/17.
 */
public class CollaborationDomainUtil {

  private CollaborationDomainUtil(){

  }

  public static ContextModel getEventContext(String eventId, Long domainId) {

    ContextModel cmodel = new ContextModel();
    cmodel.setEventId(eventId);
    DomainConfig dc = DomainConfig.getInstance(domainId);
    EventSummaryConfigModel config = dc.getEventSummaryConfig();
    if (config != null && config.getEvents() !=null) {
      Optional<EventSummaryConfigModel.Events> events = getEventByEventId(config.getEvents(), eventId);
      if (events.isPresent()) {
        cmodel.setCategory(events.get().getCategory());
        cmodel.setEventType(events.get().getType());
        Optional<EventSummaryConfigModel.Threshold>
            threshold =
            getThresholdByEventId(events.get().getThresholds(), eventId);
        if (threshold.isPresent()) {
          cmodel.setAttribute(new GsonBuilder().create().toJson(threshold.get()));
        }
      }
    }
    return cmodel;
  }

  private static Optional<EventSummaryConfigModel.Events> getEventByEventId(List<EventSummaryConfigModel.Events> events,
                                                                            String eventId) {
    return events.stream().filter(e -> e.getThresholds().stream()
        .filter(t -> t.getId().equalsIgnoreCase(eventId)).findFirst().isPresent()).findFirst();
  }

  private static Optional<EventSummaryConfigModel.Threshold> getThresholdByEventId(List<EventSummaryConfigModel.Threshold> thresholds,
                                                                                   String eventId) {
    return thresholds.stream().
        filter(threshold -> threshold.getId().equalsIgnoreCase(eventId)).findFirst();
  }

}
