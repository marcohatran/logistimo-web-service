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

package com.logistimo.deliveryrequest.fleet.model.orders;

import com.google.gson.annotations.SerializedName;

import com.logistimo.deliveryrequest.models.ExtDeliveryRequest;

import java.io.Serializable;
import java.util.Collection;
import java.util.Date;

import lombok.Data;

@Data
public class Order implements Serializable, ExtDeliveryRequest<FleetOrderStatus> {

    private String id;
    @SerializedName("cust")
    private Customer cust;
    @SerializedName("cust_shpr")
    private Boolean custShpr;
    @SerializedName("u_shpr")
    private Boolean uShpr;
    @SerializedName("u_rcvr")
    private Boolean uRcvr;
    @SerializedName("shppr")
    private Location shipper;
    @SerializedName("rcvr")
    private Location receiver;
    @SerializedName("pck")
    private String pck;
    @SerializedName("consgt")
    private Consignment consgt;
    @SerializedName("prc")
    private Pricing prc;
    @SerializedName("status")
    private FleetOrderStatus status;
    @SerializedName("o_dt")
    private Date oDt;
    @SerializedName("ept_w")
    private DateSlot eptW;
    @SerializedName("crt")
    private Date created;
    @SerializedName("upd")
    private Date lastUpdated;
    @SerializedName("crt_by")
    private String createdBy;
    @SerializedName("upd_by")
    private String updatedBy;
    @SerializedName("s_updt")
    private Date stsUpdatedOn;
    @SerializedName("ref_no")
    private String orderRefNum;
    @SerializedName("tr_id")
    private String trackingId;
    @SerializedName("e_eta")
    private Date eta; // Based on the actual Pickup time of the Order
    @SerializedName("insts")
    private Collection<ExtRemark> instructions;  // A collection of instructions - added during the order creation time

}
