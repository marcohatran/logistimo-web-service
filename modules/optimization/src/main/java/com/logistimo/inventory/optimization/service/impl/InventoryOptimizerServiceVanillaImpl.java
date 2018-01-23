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

/**
 *
 */
package com.logistimo.inventory.optimization.service.impl;

import com.logistimo.config.models.DomainConfig;
import com.logistimo.inventory.entity.IInvntry;
import com.logistimo.inventory.optimization.service.InventoryOptimizerService;
import com.logistimo.services.ServiceException;

import org.springframework.stereotype.Service;

import java.util.Date;
import java.util.List;
import java.util.Locale;

import javax.jdo.PersistenceManager;

/**
 * @author arun
 *         Implementation of an inventory optimizer
 */
@Service
public class InventoryOptimizerServiceVanillaImpl implements InventoryOptimizerService {

  /**
   * Optimize for the list of inventories specified
   */
  public void optimize(Long domainId, List<IInvntry> inventories, DomainConfig dc,
                       boolean isPAndSRequired, boolean isDAndQRequired, Locale locale,
                       PersistenceManager pm) throws ServiceException {
    // TODO: Default implementation
  }



    // Get the most recent optimizer log
  @SuppressWarnings("unchecked")
  public Date getLastRunDate(String runType) {
    return new Date();
  }

}
