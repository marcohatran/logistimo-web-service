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

package com.logistimo.communications.utils;

import com.logistimo.config.models.ConfigurationException;
import com.logistimo.config.models.SMSConfig;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.powermock.core.classloader.annotations.PowerMockIgnore;
import org.powermock.modules.junit4.PowerMockRunner;

/** @author Mohan Raja */
@RunWith(PowerMockRunner.class)
@PowerMockIgnore("javax.net.ssl.*")
public class TwilioUtilsTest {

  @Test
  public void testSend() throws ConfigurationException {
    SMSConfig smsConfig = new SMSConfig(getConfig());
    SMSConfig.ProviderConfig providerConfig = smsConfig.getProviderConfig("twilio");
    TwilioUtils.send(
        providerConfig, "919999999999", "One more test message from MR using Twilio provider");
  }

  private String getConfig() {
    return "{"
        + "  'country-config': {"
        + "    'IN': {"
        + "      'outgoing': 'twilio',"
        + "      'incoming': 'twilio'"
        + "    }"
        + "  },"
        + "  'providers': {"
        + "    'twilio': {"
        + "      'pid': 'twilio',"
        + "      'jid-unique': 'yes',"
        + "      'account_sid': 'ACbee2b770b67fc57769fd8d08b11592b8',"
        + "      'auth_token': '9a98810ad014b64ccd0fcd276a6553b4',"
        + "      'from_phone_number': '+19794282188',"
        + "      'messaging_service_sid': 'MGa36841eda51e9d96305563b8f1e38f8c',"
        + "      'callback_host': 'https://postb.in/b/QYLxqvkJ'"
        + "    }"
        + "  }"
        + "}";
  }
}
