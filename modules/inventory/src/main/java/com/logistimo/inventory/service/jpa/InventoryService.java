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

package com.logistimo.inventory.service.jpa;

import com.logistimo.inventory.entity.jpa.Inventory;
import com.logistimo.inventory.entity.jpa.InventoryBatch;
import com.logistimo.inventory.repositories.InventoryBatchRepository;
import com.logistimo.inventory.repositories.InventoryRepository;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * Created by pratheeka on 30/07/18.
 */
@Service
public class InventoryService {
  @Autowired
  private InventoryRepository inventoryRepository;

  @Autowired
  private InventoryBatchRepository inventoryBatchRepository;

  public List<Inventory> getInventoriesByKioskId(Long kioskId, List<Long> materialIds){
    return inventoryRepository.getInventories(kioskId,materialIds);
  }

  public List<InventoryBatch> getInventoryBatchesByKioskId(Long kioskId, List<Long> materialIds){
    return inventoryBatchRepository.getInventoryBatches(kioskId,materialIds);
  }
}
