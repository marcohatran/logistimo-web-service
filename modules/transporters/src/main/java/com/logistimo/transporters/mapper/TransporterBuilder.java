/*
 * Copyright © 2019 Logistimo.
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

package com.logistimo.transporters.mapper;

import com.google.common.reflect.TypeToken;
import com.google.gson.Gson;

import com.logistimo.auth.utils.SecurityUtils;
import com.logistimo.config.entity.IConfig;
import com.logistimo.config.models.TransportersSystemConfig;
import com.logistimo.config.service.impl.ConfigurationMgmtServiceImpl;
import com.logistimo.logger.XLog;
import com.logistimo.security.SecureUserDetails;
import com.logistimo.services.ServiceException;
import com.logistimo.transporters.entity.Transporter;
import com.logistimo.transporters.entity.TransporterApiMetadata;
import com.logistimo.transporters.model.ConsignmentCategoryModel;
import com.logistimo.transporters.model.TransporterDetailsModel;
import com.logistimo.transporters.model.TransporterModel;
import com.logistimo.users.entity.IUserAccount;
import com.logistimo.users.service.UsersService;
import com.logistimo.utils.LocalDateUtil;

import org.apache.commons.collections.CollectionUtils;
import org.modelmapper.ModelMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.convert.converter.Converter;
import org.springframework.stereotype.Component;

import java.lang.reflect.Type;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Component
public class TransporterBuilder {

  private static final XLog xLogger = XLog.getLog(TransporterBuilder.class);

  private ModelMapper modelMapper = new ModelMapper();

  @Autowired
  private UsersService userService;
  @Autowired
  private ConfigurationMgmtServiceImpl configurationMgmtService;

  public Converter<Transporter, TransporterModel> mapToModel() {
    return transporter -> {
      if(transporter == null) {
        return new TransporterModel();
      }
      TransporterModel transporterModel = new TransporterModel();
      transporterModel.setId(transporter.getId());
      transporterModel.setSourceDomainId(transporter.getSourceDomainId());
      transporterModel.setName(transporter.getName());
      transporterModel.setType(transporter.getType());
      transporterModel.setDefaultCategoryId(transporter.getDefaultCategoryId());
      transporterModel.setPhoneNumber(transporter.getPhoneNumber());
      SecureUserDetails sUser = SecurityUtils.getUserDetails();
      transporterModel.setUpdatedAt(LocalDateUtil.format(transporter.getUpdatedAt(),
          sUser.getLocale(), sUser.getTimezone()));
      transporterModel.setUpdatedBy(transporter.getUpdatedBy());
      transporterModel.setUpdatedByName(getUserFullName(transporter.getUpdatedBy()));
      transporterModel.setTspId(transporter.getTransporterApiMetadata() != null ? transporter
          .getTransporterApiMetadata().getTspId() : null);
      transporterModel.setVehicle(transporter.getVehicle());
      if(Boolean.TRUE.equals(transporter.getIsApiSupported())) {
        populateServiceProviderMetadata(transporterModel);
      }
      return transporterModel;
    };
  }

  private void populateServiceProviderMetadata(TransporterModel transporterModel) {
    try {
      IConfig config = configurationMgmtService.getConfiguration(IConfig.TRANSPORTER_CONFIG);
      TransportersSystemConfig tConfig =
          new Gson().fromJson(config.getConfig(), TransportersSystemConfig.class);
      Map<String, TransportersSystemConfig.TransporterConfig> configMap = new HashMap<>();
      if (CollectionUtils.isNotEmpty(tConfig.getTransporters())) {
        tConfig.getTransporters().forEach(c -> configMap.put(c.getId(), c));
        TransportersSystemConfig.TransporterConfig c = configMap.get(transporterModel.getTspId());
        if (c != null) {
          Type targetType = new TypeToken<List<ConsignmentCategoryModel>>() {
          }.getType();
          transporterModel.setCategories(modelMapper.map(c.getCategories(), targetType));
          transporterModel.setTspName(c.getName());
          transporterModel.setPriceEnabled(c.isPricingEnabled());
          transporterModel.setAttributes(c.getAttributes());
        }
      }
    } catch (ServiceException e) {
      xLogger.warn("Error while populating transporter categories", e);
    }
  }

  public TransporterDetailsModel mapToDetailedModel(Transporter transporter) {
    if(transporter == null) {
      return new TransporterDetailsModel();
    }
    TransporterDetailsModel transporterDetailsModel =
        new TransporterDetailsModel(mapToModel().convert(transporter));
    transporterDetailsModel.setCountry(transporter.getCountry());
    transporterDetailsModel.setState(transporter.getState());
    transporterDetailsModel.setDistrict(transporter.getDistrict());
    transporterDetailsModel.setTaluk(transporter.getTaluk());
    transporterDetailsModel.setCity(transporter.getCity());
    transporterDetailsModel.setStreetAddress(transporter.getStreetAddress());
    transporterDetailsModel.setPinCode(transporter.getPinCode());
    transporterDetailsModel.setApiEnabled(transporter.getIsApiSupported());
    if(transporter.getTransporterApiMetadata() != null) {
      transporterDetailsModel.setTspId(transporter.getTransporterApiMetadata().getTspId());
      transporterDetailsModel.setUrl(transporter.getTransporterApiMetadata().getUrl());
      transporterDetailsModel.setAccountId(transporter.getTransporterApiMetadata().getAccountId());
      transporterDetailsModel.setSecretToken(transporter.getTransporterApiMetadata().getSecret());
    }
    transporterDetailsModel.setCreatedBy(transporter.getCreatedBy());
    transporterDetailsModel.setCreatedByName(getUserFullName(transporter.getCreatedBy()));
    transporterDetailsModel.setDescription(transporter.getDescription());
    SecureUserDetails sUser = SecurityUtils.getUserDetails();
    transporterDetailsModel.setCreatedAt(LocalDateUtil.format(transporter.getCreatedAt(),
        sUser.getLocale(), sUser.getTimezone()));
    return transporterDetailsModel;
  }

  public Transporter mapToEntity(TransporterDetailsModel model) {
    Transporter transporter = new Transporter();
    populateEntity(transporter, model);
    return transporter;
  }

  public void populateEntity(Transporter transporter ,TransporterDetailsModel model) {
    transporter.setName(model.getName());
    transporter.setSourceDomainId(model.getSourceDomainId());
    transporter.setDescription(model.getDescription());
    transporter.setCountry(model.getCountry());
    transporter.setState(model.getState());
    transporter.setDistrict(model.getDistrict());
    transporter.setTaluk(model.getTaluk());
    transporter.setCity(model.getCity());
    transporter.setStreetAddress(model.getStreetAddress());
    transporter.setPinCode(model.getPinCode());
    transporter.setType(model.getType());
    transporter.setDefaultCategoryId(model.getDefaultCategoryId());
    transporter.setPhoneNumber(model.getPhoneNumber());
    transporter.setVehicle(model.getVehicle());
    transporter.setIsApiSupported(model.isApiEnabled());
    if(model.isApiEnabled()) {
      TransporterApiMetadata transporterApiMetadata = transporter.getTransporterApiMetadata();
      if(transporterApiMetadata == null) {
        transporterApiMetadata = new TransporterApiMetadata();
      }
      transporterApiMetadata.setTspId(model.getTspId());
      transporterApiMetadata.setUrl(model.getUrl());
      transporterApiMetadata.setAccountId(model.getAccountId());
      if(model.isSecretUpdated()) {
        transporterApiMetadata.setSecret(model.getSecretToken());
      }
      transporter.setTransporterApiMetadata(transporterApiMetadata);
    } else {
      transporter.setTransporterApiMetadata(null);
    }
  }

  private String getUserFullName(String userId) {
    IUserAccount userAccount = userService.getUserAccount(userId);
    if(userAccount != null) {
      return userAccount.getFullName();
    }
    return userId;
  }

}