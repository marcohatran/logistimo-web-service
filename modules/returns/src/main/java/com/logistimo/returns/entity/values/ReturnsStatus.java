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

package com.logistimo.returns.entity.values;

import com.logistimo.returns.Status;

import java.util.Date;

import javax.persistence.Column;
import javax.persistence.Embeddable;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.Temporal;
import javax.persistence.TemporalType;

/**
 * @author Mohan Raja
 */
@Embeddable
public class ReturnsStatus {

  @Enumerated(EnumType.STRING)
  @Column(name="status")
  private Status status;

  @Column(name="cancel_reason")
  private String cancelReason;

  @Temporal(TemporalType.TIMESTAMP)
  @Column(name="updated_at")
  private Date updatedAt;

  @Column(name="updated_by")
  private String updatedBy;

  @SuppressWarnings("unused")
  private ReturnsStatus() {
    //Added to support JPA
  }

  public ReturnsStatus(Status status, String cancelReason, Date updatedAt, String updatedBy) {
    this.status = status;
    this.cancelReason = cancelReason;
    this.updatedAt = updatedAt;
    this.updatedBy = updatedBy;
  }

  public Status getStatus() {
    return status;
  }

  public String getCancelReason() {
    return cancelReason;
  }

  public Date getUpdatedAt() {
    return updatedAt;
  }

  public String getUpdatedBy() {
    return updatedBy;
  }

}
