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

import com.logistimo.transporters.repo.TransporterRepository;

import org.datanucleus.util.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

@Component
public class DeleteTransporterAction {
  private TransporterRepository repository;

  @Autowired
  public void setRepository(TransporterRepository repository) {
    this.repository = repository;
  }

  @Transactional
  public void invoke(String transporterIdsCsv) {
    List<Long> transporterIdList = Arrays.asList(StringUtils.split(transporterIdsCsv, ","))
        .stream()
        .map(Long::parseLong)
        .collect(Collectors.toList());
    if(transporterIdList != null && !transporterIdList.isEmpty()) {
      repository.deleteByIds(transporterIdList);
    }
  }

}