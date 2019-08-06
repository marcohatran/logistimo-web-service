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

package com.logistimo.transporters.service;

import com.logistimo.pagination.Results;
import com.logistimo.services.ServiceException;
import com.logistimo.transporters.actions.AddTransporterAction;
import com.logistimo.transporters.actions.DeleteTransporterAction;
import com.logistimo.transporters.actions.GetTransporterDetailsAction;
import com.logistimo.transporters.actions.GetTransportersAction;
import com.logistimo.transporters.actions.UpdateTransporterAction;
import com.logistimo.transporters.model.TransporterDetailsModel;
import com.logistimo.transporters.model.TransporterModel;

import org.datanucleus.util.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

/**
 * Created by chandrakant on 16/07/19.
 */
@Service
public class TransporterServiceImpl implements TransporterService {

  @Autowired
  private GetTransportersAction getTransportersAction;
  @Autowired
  private AddTransporterAction addTransporterAction;
  @Autowired
  private UpdateTransporterAction updateTransporterAction;
  @Autowired
  private GetTransporterDetailsAction getTransporterDetailsAction;
  @Autowired
  private DeleteTransporterAction deleteTransporterAction;

  @Override
  public Results<TransporterModel> getTransporters(Long domainId, boolean apiEnabled,
                                                   String searchName, Integer page, Integer size) {
    Pageable pageable = null;
    if(page != null && size != null) {
      pageable = new PageRequest(page, size);
    }
    return getTransportersAction.invoke(domainId, apiEnabled, searchName, pageable);
  }

  @Override
  public void createTransporter(TransporterDetailsModel model, String userId, Long domainId) {
    addTransporterAction.invoke(model, userId, domainId);
  }

  @Override
  public void updateTransporter(Long id, TransporterDetailsModel model, String userId)
      throws ServiceException {
    updateTransporterAction.invoke(id, model, userId);
  }

  @Override
  public TransporterDetailsModel getTransporterDetails(Long id) {
    return getTransporterDetailsAction.invoke(id);
  }

  @Override
  public void deleteTransporters(String transporterIds) {
    if(StringUtils.notEmpty(transporterIds)) {
      deleteTransporterAction.invoke(transporterIds);
    }
  }
}