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

package com.logistimo.config.models;

import com.google.gson.annotations.SerializedName;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by vani on 30/01/18.
 */
public class ReturnsConfig implements Serializable {
  private static final long serialVersionUID = 1L;

  @SerializedName(value = "entity_tags")
  private List<String> entityTags = new ArrayList<>(1);
  @SerializedName(value = "incoming_duration")
  private Integer incDuration;
  @SerializedName(value = "outgoing_duration")
  private Integer outDuration;

  public List<String> getEntityTags() { return entityTags; }

  public void setEntityTags(List<String> entityTags) {
    this.entityTags = entityTags;
  }

  public Integer getIncomingDuration() {
    return incDuration;
  }

  public void setIncomingDuration(Integer incDuration) {
    this.incDuration = incDuration;
  }

  public Integer getOutgoingDuration() {
    return outDuration;
  }

  public void setOutgoingDuration(Integer outDuration) {
    this.outDuration = outDuration;
  }
}
