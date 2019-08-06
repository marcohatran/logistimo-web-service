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

package com.logistimo.transporters.actions;

import com.google.common.reflect.TypeToken;
import com.google.gson.Gson;

import com.logistimo.auth.utils.SecurityUtils;
import com.logistimo.config.entity.IConfig;
import com.logistimo.config.models.DomainConfig;
import com.logistimo.config.models.TransportersConfig;
import com.logistimo.config.models.TransportersSystemConfig;
import com.logistimo.config.service.impl.ConfigurationMgmtServiceImpl;
import com.logistimo.logger.XLog;
import com.logistimo.pagination.Results;
import com.logistimo.services.ServiceException;
import com.logistimo.transporters.entity.Transporter;
import com.logistimo.transporters.mapper.TransporterBuilder;
import com.logistimo.transporters.model.ConsignmentCategoryModel;
import com.logistimo.transporters.model.TransporterModel;
import com.logistimo.transporters.repo.TransporterRepository;

import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang.StringUtils;
import org.modelmapper.ModelMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Component;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

@Component
public class GetTransportersAction {

  private TransporterBuilder mapper;
  private ConfigurationMgmtServiceImpl configurationMgmtService;
  private TransporterRepository repository;
  private ModelMapper modelMapper = new ModelMapper();

  private static final XLog xLogger = XLog.getLog(GetTransportersAction.class);

  @Autowired
  public void setRepository(TransporterRepository repository) {
    this.repository = repository;
  }

  @Autowired
  public void setTransporterMapper(TransporterBuilder transporterBuilder) {
    this.mapper = transporterBuilder;
  }

  @Autowired
  public void setConfigurationMgmtService(ConfigurationMgmtServiceImpl configurationMgmtService) {
    this.configurationMgmtService = configurationMgmtService;
  }

  private Page<Transporter> find(long domainId, boolean isApiEnabled,String searchName,
                                Pageable pageable) {
    if(StringUtils.isNotEmpty(searchName)) {
      return repository.findBySourceDomainIdAndNameContaining(domainId, searchName, pageable);
    } else {
      if(isApiEnabled) {
        DomainConfig dc = DomainConfig.getInstance(SecurityUtils.getCurrentDomainId());
        List<TransportersConfig.TSPConfig> enabledTSPs = dc.getTransportersConfig()
            .getEnabledTransporters();
        Page<Transporter> transporters =
            repository.findBySourceDomainIdAndIsApiSupportedTrue(domainId, pageable);
        List<Transporter> filteredTransporters = transporters.getContent()
            .stream()
            .filter(t -> enabledTSPs.stream()
                .anyMatch(s -> {
                  boolean isTspEnabled =
                      t.getTransporterApiMetadata().getTspId().equals(s.getTspId());
                  if(isTspEnabled) {
                    t.setDefaultCategoryId(s.getDefaultCategoryId());
                    return true;
                  }
                  return false;
                }))
            .collect(Collectors.toList());
        return new PageImpl<>(filteredTransporters);
      } else {
        return repository.findBySourceDomainId(domainId, pageable);
      }
    }
  }

  public Results<TransporterModel> invoke(long domainId, boolean apiEnabled,
                                       String searchName,
                                       Pageable pageable) {
    Page<Transporter> transporters = find(domainId, apiEnabled, searchName, pageable);
    List<TransporterModel> transporterModels = new ArrayList<>();
    if(transporters.hasContent()) {
      transporters.map(mapper.mapToModel()).forEach(transporterModels::add);
      if(pageable != null) {
        IntStream.range(0, transporterModels.size())
            .forEach(i -> transporterModels.get(i).setSerialNo(i + pageable.getOffset() + 1));
      }
    }
    if(apiEnabled) {
      populateServiceProviderMetadata(transporterModels);
    }
    return new Results<>(transporterModels, (int) transporters.getTotalElements(),
        transporters.getNumber()*transporters.getSize());
  }

  private void populateServiceProviderMetadata(List<TransporterModel> transporterModels) {
    try {
      IConfig config = configurationMgmtService.getConfiguration(IConfig.TRANSPORTER_CONFIG);
      TransportersSystemConfig tConfig = new Gson().fromJson(config.getConfig(), TransportersSystemConfig
          .class);
      Map<String, TransportersSystemConfig.TransporterConfig> configMap = new HashMap<>();
      if(CollectionUtils.isNotEmpty(tConfig.getTransporters())) {
        tConfig.getTransporters().forEach(c -> configMap.put(c.getId(), c));
      }
      transporterModels.forEach(model -> {
        TransportersSystemConfig.TransporterConfig c = configMap.get(model.getTspId());
        if(c != null) {
          Type targetType = new TypeToken<List<ConsignmentCategoryModel>>() {}.getType();
          model.setCategories(modelMapper.map(c.getCategories(), targetType));
          model.setPriceEnabled(c.isPricingEnabled());
          model.setAttributes(c.getAttributes());
        }
      });
    } catch (ServiceException e) {
      xLogger.severe("Error while populating transporter categories", e);
    }
  }
}