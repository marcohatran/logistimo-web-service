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

var tempServices = angular.module('tempServices', []);
tempServices.factory('tempService', ['APIService', function (apiService) {
    return {
        getMaterialDevices: function (entityId, materialId) {
            return apiService.get('/s2/api/temperature/devices/' + entityId + '/' + materialId);
        },
        saveInvntryItems: function (entityId, materialId, invItem) {
            return apiService.post(invItem, '/s2/api/temperature/devices/' + entityId + '/' + materialId);
        },
        saveInvntryItem: function (entityId, materialId, invItem, pushConfig) {
            return apiService.post(invItem, '/s2/api/temperature/device/' + entityId + '/' + materialId + '?pushconfig=' + pushConfig);
        },
        getTempDetails: function (entityId, materialId) {
            return apiService.get('/s2/api/temperature/monitor/' + entityId + '/' + materialId);
        },
        getTagSummary: function (domainId) {
            return apiService.get('/s2/api/temperature/tags?tagid=' + domainId);
        },

        getChildTagSummary: function (domainId) {
            return apiService.get('/s2/api/temperature/tags/child?tagid=' + domainId);
        },

        getTagAbnormalDevices: function (domainId) {
            return apiService.get('/s2/api/temperature/tags/abnormal?tagid=' + domainId);
        },

        getAssets: function (kioskId, deviceId, vendorId, filter, duration, location, locType, offset, size) {
            return apiService.get('/s2/api/temperature/assets?kioskid=' + kioskId + '&deviceid=' + deviceId + '&vendorid=' + vendorId + '&filter=' + filter + '&duration=' + duration + '&location=' + location + '&loctype=' + locType + '&offset=' + offset + '&size=' + size);
        },

        getInvntryItemsByDomain: function (domainId, deviceId) {
            return apiService.get('/s2/api/temperature/assets/' + deviceId);
        },

        getDeviceInfo: function (vendorId, deviceId) {
            return apiService.get('/s2/api/temperature/device/' + vendorId + '/' + deviceId);
        },

        getDeviceConfig: function (vendorId, deviceId) {
            return apiService.get('/s2/api/temperature/device/config/' + vendorId + '/' + deviceId);
        },

        getRecentAlerts: function (vendorId, deviceId, page, size) {
            return apiService.get('/s2/api/temperature/device/alerts/recent/' + vendorId + '/' + deviceId + '?page=' + page + '&size=' + size);
        },

        getTemperatures: function (vendorId, deviceId, size, sint, tdate) {
            var url = '/s2/api/temperature/' + vendorId + '/' + deviceId + '?size=' + size + '&sint=' + sint;
            if(checkNotNullEmpty(tdate)){
                url += '&edate=' + formatDate2Url(tdate);
            }
            return apiService.get(url);
        },

        getCurrentTemp: function (vendorId, deviceId) {
            return apiService.get('/s2/api/temperature/current/' + vendorId + '/' + deviceId);
        },

        getDeviceStats: function (vendorId, deviceId, from, to) {
            return apiService.get('/s2/api/temperature/stats/' + vendorId + '/' + deviceId + '?from=' + from + '&to=' + to);
        },

        getVendorMapping: function () {
            return apiService.get('/s2/api/temperature/vendors');
        },

        getDomainVendorMapping: function () {
            return apiService.get('/s2/api/temperature/domain/vendors');
        },

        pushPullConfig: function (requestData, domainId) {
            return apiService.post(requestData, '/s2/api/temperature/device/config');
        },

        updateDeviceInfo: function (deviceInfo, domainId) {
            return apiService.post(deviceInfo, '/s2/api/temperature/device');
        },

        updateDeviceConfig: function (deviceConfig, domainId, pushConfig) {
            return apiService.post(deviceConfig, '/s2/api/temperature/config?pushConfig=' + pushConfig);
        },

        /*updateInvntryItems: function (kioskId, materialId, deviceInfo) {
         return apiService.post(deviceInfo, '/i?a=arsni&kid=' + kioskId + '&mid=' + materialId);
        },*/

        /*createDevice: function (deviceInfo, domainId, kioskId, materialId) {
         return apiService.post("", '/tempmonitoring?a=register&domainid=' + domainId + '&kioskid=' + kioskId + '&materialid=' + materialId + '&devicestoaddjson=' + JSON.stringify(deviceInfo));
        },*/

        getEntityInformation: function (kioskId) {
            return apiService.get('/s2/api/entities/entity/' + kioskId);
        },

        getMaterialInformation: function (materialId) {
            return apiService.get('/s2/api/materials/material/' + materialId);
        },

        getDevices: function (entityId) {
            if(!checkNotNullEmpty(entityId)){
                entityId = -1;
            }
            return apiService.get('/s2/api/temperature/devices/' + entityId);
        },

        getDomainLocation: function () {
            return apiService.get('/s2/api/temperature/domain/location');
        }
    }}]);
