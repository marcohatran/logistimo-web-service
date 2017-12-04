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

package com.logistimo.social.event.processor;

import com.codahale.metrics.Meter;
import com.logistimo.collaboration.core.events.Event;
import com.logistimo.context.StaticApplicationContext;
import com.logistimo.logger.XLog;
import com.logistimo.social.event.handler.Handler;
import com.logistimo.social.event.registry.HandlerRegistry;
import com.logistimo.utils.MetricsUtil;

import java.util.List;

/**
 * Created by kumargaurav on 10/11/17.
 */
public class SocialEventProcessor<E extends Event> {

  private static final XLog log = XLog.getLog(SocialEventProcessor.class);
  private static Meter jmsMeter = MetricsUtil
      .getMeter(SocialEventProcessor.class, "socialEventProcessor");

  @org.apache.camel.Handler
  public void execute(E e) {
    jmsMeter.mark();
    log.info("processing events in logistimo {0}", e);
    HandlerRegistry
        registry =
        StaticApplicationContext.getApplicationContext().getBean(HandlerRegistry.class);
    List<Handler> handlers = registry.getEventHandlers(e);
    for (Handler handler : handlers) {
      handler.handle(e);
    }
  }

}
