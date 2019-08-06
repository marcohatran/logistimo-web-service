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

package com.logistimo.transporters.repo;

import com.logistimo.transporters.entity.Transporter;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface TransporterRepository extends JpaRepository<Transporter, Long> {

  Page<Transporter> findBySourceDomainIdAndNameContaining(Long sourceDomainId, String name,
                                                          Pageable pageable);

  Page<Transporter> findBySourceDomainId(Long sourceDomainId, Pageable pageable);

  Page<Transporter> findBySourceDomainIdAndIsApiSupportedTrue(Long sourceDomainId, Pageable pageable);

  Page<Transporter> findBySourceDomainIdAndIsApiSupportedFalse(Long sourceDomainId, Pageable pageable);

  Integer countBySourceDomainId(Long sourceDomainId);

  @Modifying
  @Query("delete from Transporter t WHERE t.id in ?1")
  void deleteByIds(@Param("ids") List<Long> ids);
}