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

package com.logistimo.social.provider;

import org.junit.Assert;
import org.junit.Before;

import java.util.List;

public class SubscriberProviderTest {

  SubscriberProvider subscriberProvider;

  @Before
  public void setUp () {
    subscriberProvider = new SubscriberProvider();
  }

  @org.junit.Test
  public void testGetStoreUserTagForEventForNull () {
    String context = "{\"eventId\":\"120548104\",\"attribute\":\"{\\\"id\\\":\\\"120548104\\\",\\\"conditions\\\":[{\\\"name\\\":\\\"percentage_time_in_stock\\\",\\\"value\\\":\\\"30\\\",\\\"units\\\":\\\"%\\\",\\\"oper\\\":\\\"\\\\u003e\\\\u003d\\\"},{\\\"name\\\":\\\"percentage_time_no_excess_stock\\\",\\\"value\\\":\\\"30\\\",\\\"units\\\":\\\"%\\\",\\\"oper\\\":\\\"\\\\u003e\\\\u003d\\\"},{\\\"name\\\":\\\"percentage_of_materials\\\",\\\"value\\\":\\\"40\\\",\\\"units\\\":\\\"%\\\",\\\"oper\\\":\\\"\\\\u003e\\\\u003d\\\"},{\\\"name\\\":\\\"duration\\\",\\\"value\\\":\\\"1\\\",\\\"units\\\":\\\"months\\\",\\\"oper\\\":\\\"\\\"},{\\\"name\\\":\\\"include_entity_tags\\\",\\\"units\\\":\\\"\\\",\\\"values\\\":[\\\"GTAG\\\"],\\\"oper\\\":\\\"\\\"},{\\\"name\\\":\\\"exclude_entity_tags\\\",\\\"units\\\":\\\"\\\",\\\"values\\\":[\\\"SVS\\\"],\\\"oper\\\":\\\"\\\"},{\\\"name\\\":\\\"users_tags_responsible\\\",\\\"units\\\":\\\"\\\",\\\"values\\\":[\\\"CMO\\\"],\\\"oper\\\":\\\"\\\"}]}\",\"category\":\"inventory\",\"eventType\":\"inventory_performance_by_entity\"}";
    List<String> usertags = subscriberProvider.getStoreUserTagForEvent(context);
    Assert.assertEquals(0,usertags.size());
  }


  @org.junit.Test
  public void testGetStoreUserTagForEventForNotNull () {
    String context = "{\"eventId\":\"120548104\",\"attribute\":\"{\\\"id\\\":\\\"120548104\\\",\\\"conditions\\\":[{\\\"name\\\":\\\"percentage_time_in_stock\\\",\\\"value\\\":\\\"30\\\",\\\"units\\\":\\\"%\\\",\\\"oper\\\":\\\"\\\\u003e\\\\u003d\\\"},{\\\"name\\\":\\\"percentage_time_no_excess_stock\\\",\\\"value\\\":\\\"30\\\",\\\"units\\\":\\\"%\\\",\\\"oper\\\":\\\"\\\\u003e\\\\u003d\\\"},{\\\"name\\\":\\\"percentage_of_materials\\\",\\\"value\\\":\\\"40\\\",\\\"units\\\":\\\"%\\\",\\\"oper\\\":\\\"\\\\u003e\\\\u003d\\\"},{\\\"name\\\":\\\"duration\\\",\\\"value\\\":\\\"1\\\",\\\"units\\\":\\\"months\\\",\\\"oper\\\":\\\"\\\"},{\\\"name\\\":\\\"include_entity_tags\\\",\\\"units\\\":\\\"\\\",\\\"values\\\":[\\\"GTAG\\\"],\\\"oper\\\":\\\"\\\"},{\\\"name\\\":\\\"exclude_entity_tags\\\",\\\"units\\\":\\\"\\\",\\\"values\\\":[\\\"SVS\\\"],\\\"oper\\\":\\\"\\\"},{\\\"name\\\":\\\"user_tags_responsible\\\",\\\"units\\\":\\\"\\\",\\\"values\\\":[\\\"CMO\\\"],\\\"oper\\\":\\\"\\\"}]}\",\"category\":\"inventory\",\"eventType\":\"inventory_performance_by_entity\"}";
    List<String> usertags = subscriberProvider.getStoreUserTagForEvent(context);
    Assert.assertNotEquals(0,usertags.size());
  }

}
