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

package com.logistimo.social.event.registry;

import com.logistimo.collaboration.core.events.Event;
import com.logistimo.social.event.handler.Handler;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.context.ApplicationListener;
import org.springframework.context.event.ContextRefreshedEvent;
import org.springframework.stereotype.Component;

import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Created by kumargaurav on 10/11/17.
 */
@Component
public class HandlerRegistry implements ApplicationListener<ContextRefreshedEvent> {


  private static final Logger log = LoggerFactory.getLogger(HandlerRegistry.class);

  @Autowired
  private ConfigurableListableBeanFactory beanFactory;

  private Map<String, Handler> eventHandlers = new HashMap<>();

  @SuppressWarnings({"unchecked", "rawtypes"})
  public List<Handler> getEventHandlers(Event event) {
    List<Handler> handlers = new ArrayList<>();
    for (Handler handler : this.eventHandlers.values()) {
      if (getHandledEventType(handler) != null
          && getHandledEventType(handler).isAssignableFrom(event.getClass())) {
        log.info("Queueing handler {} for {}", handler.getClass().getSimpleName(),
            event.getClass().getSimpleName());
        handlers.add(handler);
      }
    }
    return handlers;
  }

  @Override
  public void onApplicationEvent(ContextRefreshedEvent event) {
    eventHandlers.clear();
    eventHandlers.putAll(beanFactory.getBeansOfType(Handler.class));
  }

  private Class<?> getHandledEventType(Handler handler) {
    Type genericSuperclass = handler.getClass().getGenericSuperclass();
    if (genericSuperclass instanceof ParameterizedType) {
      Class<?> genericType = getGenericType((ParameterizedType) genericSuperclass);
      if (genericType != null && Event.class.isAssignableFrom(genericType)) {
        return genericType;
      }
    }
    for (Type genericInterface : handler.getClass().getGenericInterfaces()) {
      if (genericInterface instanceof ParameterizedType) {
        Class<?> genericType = getGenericType((ParameterizedType) genericInterface);
        if (genericType != null && Event.class.isAssignableFrom(genericType)) {
          return genericType;
        }
      }
    }
    return null;
  }

  private Class<?> getGenericType(ParameterizedType type) {
    return (Class<?>) type.getActualTypeArguments()[0];
  }

}
