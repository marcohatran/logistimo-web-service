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

package com.logistimo.api.servlets.mobile.builders;

import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;

/**
 * Created by vani on 17/02/18.
 */
public class MobileReturnsDurationModel {
  public static final int DEFAULT_INCOMING_DURATION = 30;
  public static final int DEFAULT_OUTGOING_DURATION = 30;

  @Expose
  @SerializedName(value = "incoming")
  private int incoming = DEFAULT_INCOMING_DURATION;
  @Expose
  @SerializedName(value = "outgoing")
  private int outgoing = DEFAULT_OUTGOING_DURATION;

  public int getIncoming() {
    return incoming;
  }

  public void setIncoming(int incoming) {
    this.incoming = incoming;
  }

  public int getOutgoing() {
    return outgoing;
  }

  public void setOutgoing(int outgoing) {
    this.outgoing = outgoing;
  }
}
