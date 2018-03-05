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

package com.logistimo.api.action;

import com.logistimo.api.builders.ConfigurationModelBuilder;
import com.logistimo.api.models.configuration.AssetConfigModel;
import com.logistimo.api.models.configuration.AssetSystemConfigModel;
import com.logistimo.config.models.AssetSystemConfig;
import com.logistimo.config.models.DomainConfig;
import com.logistimo.logger.XLog;
import com.logistimo.services.ServiceException;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.Locale;

/**
 * Created by naveensnair on 27/02/18.
 */
@Component
public class AssetConfigurationAction {

  private static final XLog xLogger = XLog.getLog(AssetConfigurationAction.class);

  private ConfigurationModelBuilder configurationModelBuilder;

  @Autowired
  public void setConfigurationModelBuilder(ConfigurationModelBuilder configurationModelBuilder) {
    this.configurationModelBuilder = configurationModelBuilder;
  }

  public AssetSystemConfigModel invoke(String src, Long domainId, Locale locale, String timezone)
      throws ServiceException {
    try {
      AssetSystemConfig asc = AssetSystemConfig.getInstance();
      AssetConfigModel assetConfigModel = null;
      if(domainId != null) {
        DomainConfig dc = DomainConfig.getInstance(domainId);
        assetConfigModel = configurationModelBuilder.buildAssetConfigModel(dc, locale, timezone);
      }
      return configurationModelBuilder.buildAssetSystemConfigModel(asc, assetConfigModel);
    } catch (Exception e) {
      xLogger.warn("Unable to fetch the domain details", e);
    }

    return null;
  }
}
