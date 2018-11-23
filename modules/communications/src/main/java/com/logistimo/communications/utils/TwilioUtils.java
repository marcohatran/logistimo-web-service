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

import com.logistimo.config.models.SMSConfig;
import com.logistimo.constants.CharacterConstants;
import com.logistimo.logger.XLog;
import com.twilio.Twilio;
import com.twilio.rest.api.v2010.account.Message;
import com.twilio.rest.api.v2010.account.MessageCreator;
import com.twilio.type.PhoneNumber;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import org.springframework.util.StringUtils;

import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/** @author Mohan Raja */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class TwilioUtils {
  private static final XLog xLogger = XLog.getLog(TwilioUtils.class);
  public static final String TWILIO = "twilio";
  private static final String TWILIO_CALLBACK_API = "/s2/api/sms/twilio/update-status";

  public static void send(
      SMSConfig.ProviderConfig providerConfig, String phoneNumber, String message) {
    send(providerConfig, Collections.singletonList(phoneNumber), message);
  }

  public static List<Message> send(
      SMSConfig.ProviderConfig providerConfig, List<String> phoneNumbers, String message) {
    if (providerConfig == null) {
      xLogger.warn("Provider config is missing for Twilio message service");
      return null;
    }
    String providerId = providerConfig.getString(SMSConfig.ProviderConfig.PROVIDER_ID);
    if (!TWILIO.equals(providerId)) {
      xLogger.warn("Invalid provider config {0} found. Expected provider is twilio.", providerId);
      return null;
    }
    Twilio.init(
        providerConfig.getString(SMSConfig.ProviderConfig.ACCOUNT_SID),
        providerConfig.getString(SMSConfig.ProviderConfig.AUTH_TOKEN));
    String callbackHost = providerConfig.getString(SMSConfig.ProviderConfig.CALLBACK_HOST);
    return cleansePhoneNumbers(phoneNumbers)
        .stream()
        .map(phoneNumber -> doSend(message, providerConfig, callbackHost, phoneNumber))
        .collect(Collectors.toList());
  }

  private static Message doSend(
      String message,
      SMSConfig.ProviderConfig providerConfig,
      String callbackHost,
      String phoneNumber) {
    MessageCreator creator =
        Message.creator(
            new PhoneNumber(phoneNumber),
            providerConfig.getString(SMSConfig.ProviderConfig.MESSAGING_SERVICE_SID),
            message);
    if (!StringUtils.isEmpty(callbackHost)) {
      creator.setStatusCallback(callbackHost.concat(TWILIO_CALLBACK_API));
    }
    return creator.create();
  }

  private static Set<String> cleansePhoneNumbers(List<String> phoneNumbers) {
    return phoneNumbers.stream().map(TwilioUtils::cleansePhoneNumber).collect(Collectors.toSet());
  }

  private static String cleansePhoneNumber(String phoneNumber) {
    String cleansedPhoneNumber = StringUtils.trimAllWhitespace(phoneNumber);
    if (!cleansedPhoneNumber.startsWith(CharacterConstants.PLUS)) {
      return CharacterConstants.PLUS + cleansedPhoneNumber;
    }
    return cleansedPhoneNumber;
  }
}
