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

var ordServices = angular.module('ordServices', []);
ordServices.factory('ordService', ['APIService', function (apiService) {
    return {
        getEntityOrders: function (entityId, orderType, status, tgType, tag, from, to, offset, size, oType, rid, approvalStatus) {
            offset = typeof offset !== 'undefined' ? offset : 0;
            size = typeof size !== 'undefined' ? size : 50;
            var urlStr = '/s2/api/orders/entity/' + entityId + "?offset=" + offset + "&size=" + size;
            if (checkNotNullEmpty(orderType)) {
                urlStr = urlStr + "&otype=" + orderType;
            }
            if (checkNotNullEmpty(status)) {
                urlStr = urlStr + "&status=" + status;
            }
            if (checkNotNullEmpty(tgType)) {
                urlStr = urlStr + "&tgType=" + tgType;
            }
            if (checkNotNullEmpty(tag)) {
                urlStr = urlStr + "&tag=" + tag;
            }
            if (checkNotNullEmpty(from)) {
                urlStr = urlStr + "&from=" + from;
            }
            if (checkNotNullEmpty(to)) {
                urlStr = urlStr + "&until=" + to;
            }
            if (checkNotNullEmpty(oType)) {
                urlStr = urlStr + "&oty=" + oType;
            }
            if (checkNotNullEmpty(rid)) {
                urlStr = urlStr + "&rid=" + encodeURIComponent(rid);
            }
            if (checkNotNullEmpty(approvalStatus)) {
                urlStr = urlStr + "&approval_status=" + approvalStatus;
            }
            return apiService.get(urlStr);
        },
        getOrder: function (orderId) {
            return apiService.get('/s2/api/orders/order/' + orderId + '?embed=permissions');
        },
        getOrders: function (orderType, status, tgType, tag, from, to, offset, size, oType, rid, approvalStatus) {
            offset = typeof offset !== 'undefined' ? offset : 0;
            size = typeof size !== 'undefined' ? size : 50;
            var urlStr = '/s2/api/orders/?offset=' + offset + "&size=" + size;
            if (checkNotNullEmpty(orderType)) {
                urlStr = urlStr + "&otype=" + orderType;
            }
            if (checkNotNullEmpty(status)) {
                urlStr = urlStr + "&status=" + status;
            }
            if (checkNotNullEmpty(tgType)) {
                urlStr = urlStr + "&tgType=" + tgType;
            }
            if (checkNotNullEmpty(tag)) {
                urlStr = urlStr + "&tag=" + tag;
            }
            if (checkNotNullEmpty(from)) {
                urlStr = urlStr + "&from=" + from;
            }
            if (checkNotNullEmpty(to)) {
                urlStr = urlStr + "&until=" + to;
            }
            if (checkNotNullEmpty(oType) || oType == 0) {
                urlStr = urlStr + "&oty=" + oType;
            }
            if (checkNotNullEmpty(rid)) {
                urlStr = urlStr + "&rid=" + encodeURIComponent(rid);
            }
            if (checkNotNullEmpty(approvalStatus)) {
                urlStr = urlStr + "&approval_status=" + approvalStatus;
            }
            return apiService.get(urlStr);
        },
        updateOrderStatus: function (orderId, orderStaus) {
            return apiService.post(orderStaus, '/s2/api/orders/order/' + orderId + "/status");
        },
        updateVendor: function (orderId, vendorId, orderUpdatedAt) {
            return apiService.post({
                updateValue: vendorId,
                orderUpdatedAt: orderUpdatedAt
            }, '/s2/api/orders/order/' + orderId + "/vendor");
        },
        getVendorConfig: function (kioskId) {
            return apiService.get('/s2/api/transactions/transconfig/?kioskId=' + kioskId);
        },
        createOrder: function (data) {
            return apiService.post(data, '/s2/api/orders/add/');
        },
        createShipment: function (data) {
            return apiService.post(data, '/s2/api/shipment/add/');
        },
        getShipments: function (offset, size, custId,vendId,status, from, to, eftFrom,eftTo, trans, trackId) {
            var urlStr = '/s2/api/shipment/?offset=' + offset + "&size=" + size;
            if (checkNotNullEmpty(vendId)) {
                urlStr = urlStr + "&vendId=" + vendId;
            }
            if (checkNotNullEmpty(custId)) {
                urlStr = urlStr + "&custId=" + custId;
            }
            if (checkNotNullEmpty(status)) {
                urlStr = urlStr + "&status=" + status;
            }
            if (checkNotNullEmpty(from)) {
                urlStr = urlStr + "&from=" + from;
            }
            if (checkNotNullEmpty(to)) {
                urlStr = urlStr + "&to=" + to;
            }
            if (checkNotNullEmpty(eftFrom)) {
                urlStr = urlStr + "&eftFrom=" + eftFrom;
            }
            if (checkNotNullEmpty(eftTo)) {
                urlStr = urlStr + "&eftTo=" + eftTo;
            }
            if (checkNotNullEmpty(trans)) {
                urlStr = urlStr + "&trans=" + trans;
            }
            if (checkNotNullEmpty(trackId)) {
                urlStr = urlStr + "&trackId=" + trackId;
            }
            return apiService.get(urlStr);
        },
        updateShipment: function (data) {
            return apiService.post(data, '/s2/api/shipment/update/sitems');
        },
        updatePayment: function (orderId, payment) {
            return apiService.post(payment, '/s2/api/orders/order/' + orderId + "/payment");
        },
        updateTransporter: function (orderId, transporter, orderUpdatedAt) {
            return apiService.post({
                updateValue: transporter,
                orderUpdatedAt: orderUpdatedAt
            }, '/s2/api/orders/order/' + orderId + "/transporter");
        },
        updatePackage: function (orderId, pkg) {
            return apiService.post("'" + pkg + "'", '/s2/api/orders/order/' + orderId + "/package");
        },
        updateFulfillmentTime: function (orderId, fulfillmentTime, orderUpdatedAt) {
            return apiService.post({
                updateValue: fulfillmentTime,
                orderUpdatedAt: orderUpdatedAt
            }, '/s2/api/orders/order/' + orderId + "/fulfillmenttime");
        },
        updateExpectedFulfillmentDate: function (orderId, efd, orderUpdatedAt) {
            return apiService.post({
                updateValue: efd,
                orderUpdatedAt: orderUpdatedAt
            }, '/s2/api/orders/order/' + orderId + "/efd");
        },
        updateDueDate: function (orderId, edd, orderUpdatedAt) {
            return apiService.post({
                updateValue: edd,
                orderUpdatedAt: orderUpdatedAt
            }, '/s2/api/orders/order/' + orderId + "/edd");
        },
        updateMaterials: function (orderId, demandItems) {
            return apiService.post(demandItems, '/s2/api/orders/order/' + orderId + "/items");
        },
        getOrderStatusJSON: function (orderId) {
            return apiService.get('/s2/api/orders/order/' + orderId + '/statusJSON');
        },
        getOrderReasons: function(type) {
            return apiService.get('/s2/api/orders/order/reasons/' + type);
        },
        updateOrderTags: function(orderId,oTags,orderUpdatedAt) {
            return apiService.post({
                updateValue: oTags,
                orderUpdatedAt: orderUpdatedAt
            }, '/s2/api/orders/order/' + orderId + '/tags');
        },
        updateReferenceID: function(orderId,referenceID, orderUpdatedAt) {
            return apiService.post({
                updateValue: referenceID,
                orderUpdatedAt: orderUpdatedAt
            }, '/s2/api/orders/order/' + orderId + '/referenceid');
        },
        getIdSuggestions: function(id, type, oty) {
            var urlStr = '/s2/api/orders/filter/?id=' + encodeURIComponent(id) + "&type="+type;
            if (checkNotNullEmpty(oty)) {
                urlStr += "&oty=" + oty;
            }
            return apiService.get(urlStr);
        },
        getTransSuggestions: function(text) {
            var urlStr = '/s2/api/shipment/transfilter/?text=' + encodeURIComponent(text);
            return apiService.get(urlStr);
        },
        updateOrder: function(orderId, details) {
            return apiService.post(details, '/s2/api/orders/' + orderId + '/update/items');
        },
        getShipmentsByOrderId: function (orderId) {
            return apiService.get('/s2/api/shipment/' + orderId);
        },
        getShipment:function(sID){
            return apiService.get('/s2/api/shipment/detail/' + sID);
        },
        updateShipmentInfo: function (updValue, sID, orderUpdatedAt) {
            return apiService.post("'" + updValue + "'", '/s2/api/shipment/update/' + sID + '/transporter?orderUpdatedAt=' + orderUpdatedAt);
        },
        updateShipmentTrackingId: function (updValue, sID, orderUpdatedAt) {
            return apiService.post("'" + updValue + "'", '/s2/api/shipment/update/' + sID + '/trackingID?orderUpdatedAt=' + orderUpdatedAt);
        },
        updateShipmentReason: function (updValue, sID, orderUpdatedAt) {
            return apiService.post("'" + updValue + "'", '/s2/api/shipment/update/' + sID + '/rfs?orderUpdatedAt=' + orderUpdatedAt);
        },
        updateShipmentDate: function (updValue, sID, orderUpdatedAt) {
            return apiService.post("'" + updValue + "'", '/s2/api/shipment/update/' + sID + '/date?orderUpdatedAt=' + orderUpdatedAt);
        },
        getStatusHistory: function(id, type, tag) {
            var urlStr = '/s2/api/activity/?';
            if(checkNotNullEmpty(id)) {
                urlStr += '&objectId=' + id;
            }
            if(checkNotNullEmpty(type)) {
                urlStr += '&objectType=' + type;
            }
            if(checkNotNullEmpty(tag)) {
                urlStr += '&tag=' + tag;
            }
            return apiService.get(urlStr);
        },
        updateShipmentStatus: function(shipId, status) {
            return apiService.post(status, '/s2/api/shipment/update/' + shipId + "/status");
        },
        updateShipmentPackageSize: function (updValue, sID) {
            return apiService.post("'" + updValue + "'", '/s2/api/shipment/update/' + sID + '/ps');
        },
        fetchRequesters: function(text) {
            return apiService.get("/s2/api/order-approvals-meta/requesters?q=" + text);
        },
        fetchApprovers: function(text) {
            return apiService.get("/s2/api/order-approvals-meta/approvers?q=" + text);
        },
        fetchPrimaryApprovers: function(orderId) {
            return apiService.get("/s2/api/orders/order/" + orderId + "/approvers");
        },
        updateShipmentReferenceId: function(updValue, sId, orderUpdatedAt) {
            return apiService.post("'" + updValue + "'", '/s2/api/shipment/update/' + sId + '/referenceId?orderUpdatedAt=' + orderUpdatedAt);
        }
    }
}]);
