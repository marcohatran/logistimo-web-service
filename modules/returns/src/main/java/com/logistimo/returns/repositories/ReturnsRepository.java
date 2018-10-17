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

package com.logistimo.returns.repositories;

import com.logistimo.returns.entity.Returns;
import com.logistimo.returns.vo.ReturnsQuantityDetailsVO;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * Created by pratheeka on 26/06/18.
 */
@Repository(value = "returns")
public interface ReturnsRepository extends
    JpaRepository<Returns, String>,
    JpaSpecificationExecutor<Returns> {

  /**
   * Query to fetch the total quantities in returns for a given order ID.
   * Returns in cancelled status is excluded.
   */

  @Query(value =
      "select new com.logistimo.returns.vo.ReturnsQuantityDetailsVO( "
          + "sum(rb.quantity) as batchQuantity,sum(ri.quantity) as itemQuantity,rb.batch.batchId as "
          + "batchId,ri.materialId as materialId,rb.batch.manufacturer as manufacturer,"
          + "rb.batch.expiryDate as expiryDate,rb.batch.manufacturedDate as manufacturedDate,"
          + "rt.status.status as status) from Returns rt left join  "
          + "ReturnsItem ri on ri.returnsId=rt.id left join ReturnsItemBatch rb on rb.itemId=ri.id "
          + "where rt.orderId=?1 and rt.id not in (?2) and rt.status.status not in ('CANCELLED') "
          + "group by rb.batch.batchId,ri.materialId,rt.status.status")
  List<ReturnsQuantityDetailsVO> findQuantityInOtherReturns(Long orderId, Long returnId);

  @Query(value = "select new com.logistimo.returns.vo.ReturnsQuantityDetailsVO( "
      + "sum(rb.quantity) as batchQuantity,sum(ri.quantity) as itemQuantity,rb.batch.batchId as "
      + "batchId,ri.materialId as materialId,rb.batch.manufacturer as manufacturer,"
      + "rb.batch.expiryDate as expiryDate,rb.batch.manufacturedDate as manufacturedDate,"
      + "rt.status.status as status) from Returns rt left join  "
      + "ReturnsItem ri on ri.returnsId=rt.id left join ReturnsItemBatch rb on rb.itemId=ri.id "
      + "where rt.orderId=?1 and rt.status.status not in ('CANCELLED') "
      + "group by ri.materialId,rb.batch.batchId,rt.status.status")
  List<ReturnsQuantityDetailsVO> findReturnQuantityByOrderId(Long orderId);

  Returns findById(Long id);

  @Modifying
  @Query(value = "DELETE FROM Returns r where r.customerId=?1")
  void deleteReturnsByCustomerId(Long customerId);
}
