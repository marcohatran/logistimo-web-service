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

var assetServices = angular.module('assetServices', []);
assetServices.factory('assetService', ['APIService', function (apiService) {
    return {
        createAsset: function(data){
            return apiService.post(data, '/s2/api/assets/');
        },
        getAssetsByKeyword: function(keyword, at, size, offset){
            offset = typeof offset !== 'undefined' ? offset : 0;
            size = typeof size !== 'undefined' ? size : 50;
            var urlStr = "/s2/api/assets/?offset=" + offset + "&size=" + size;
            if(checkNotNullEmpty(keyword)){
                urlStr += '&q=' + keyword;
            }else if(checkNotNullEmpty(at)){
                urlStr += '&at=' + at;
            }
            return apiService.get(urlStr);
        },
        getAssetsInDetail: function(eid, at, ws, alrmType, dur, location, offset, size, awr){
            offset = typeof offset !== 'undefined' ? offset : 0;
            size = typeof size !== 'undefined' ? size : 50;
            var urlStr = "/s2/api/assets/details/?offset=" + offset + "&size=" + size;
            if(checkNotNullEmpty(eid)){
                urlStr += "&eid=" + eid
            }
            if(checkNotNullEmpty(location)){
                urlStr += "&loc=" + location;
            }

            if(checkNotNullEmpty(at)){
                urlStr += "&at=" + at;
            }

            if(checkNotNullEmpty(ws)){
                urlStr += "&ws=" + ws;
            }

            if(checkNotNullEmpty(alrmType)){
                urlStr += "&alrmtype=" + alrmType;

                if(checkNotNullEmpty(dur)){
                    urlStr += "&dur=" + dur;
                }
            }
            if(checkNotNullEmpty(awr)){
                urlStr += "&awr=" + awr;
            }
            return apiService.get(urlStr);
        },
        exportData: function(json) {
            return apiService.post(json, '/s2/api/assets/export');
        },
        getAssetDetails: function(manufactureId, assetId){
            return apiService.get("/s2/api/assets/" + manufactureId + "/" + encodeURL(assetId));
        },
        getAssetRelations: function(manufactureId, assetId){
            return apiService.get("/s2/api/assets/relation/" + manufactureId + "/" + encodeURL(assetId));
        },
        getFilteredAssets: function(text, entityId, at, all, ns){
            var urlStr = "/s2/api/assets/filter?q=" + text;
            if(checkNotNullEmpty(at)){
                urlStr += "&at=" + at;
            }

            if(checkNotNullEmpty(entityId)){
                urlStr += "&eid=" + entityId;
            }

            if(all != undefined){
                urlStr += "&all=" + all;
            }

            if(checkNotNullEmpty(ns)){
                urlStr += "&ns=" + ns;
            }
            return apiService.get(urlStr);
        },
        createAssetRelationships: function(data, deleteR){
            var urlStr = "/s2/api/assets/relations";

            if(deleteR != undefined){
                urlStr += "?delete=" + deleteR;
            }

            return apiService.post(data, urlStr);
        },
        getTemperatures: function (vendorId, deviceId, mpId, at, size, sint, tdate) {
            var url = '/s2/api/assets/temperature/' + vendorId + '/' + encodeURL(deviceId) + '/' + mpId + '?size=' + size + '&sint=' + sint + '&at=' + at;
            if(checkNotNullEmpty(tdate)){
                url += '&edate=' + formatDate2Url(tdate);
            }
            return apiService.get(url);
        },
        getRecentAlerts: function (vendorId, deviceId, page, size) {
            return apiService.get('/s2/api/assets/alerts/recent/' + vendorId + '/' + encodeURL(deviceId) + '?page=' + page + '&size=' + size);
        },
        getAssetConfig: function (vendorId, deviceId) {
            return apiService.get('/s2/api/assets/config/' + vendorId + '/' + encodeURL(deviceId));
        },
        updateDeviceConfig: function (deviceConfig, domainId, pushConfig) {
            return apiService.post(deviceConfig, '/s2/api/assets/config?pushConfig=' + pushConfig);
        },
        getAssetStats: function (vendorId, deviceId, from, to) {
            return apiService.get('/s2/api/assets/stats/' + vendorId + '/' + encodeURL(deviceId) + '?from=' + from + '&to=' + to);
        },
        getChildTagSummary: function (domainId) {
            return apiService.get('/s2/api/assets/tags/child?tagid=' + domainId);
        },
        getTagAbnormalDevices: function (domainId) {
            return apiService.get('/s2/api/assets/tags/abnormal?tagid=' + domainId);
        },
        getDomainLocation: function () {
            return apiService.get('/s2/api/assets/domain/location');
        },
        getTagSummary: function (domainId, at) {
            return apiService.get('/s2/api/assets/tags?tagid=' + domainId + '&at=' + at);
        },
        updateAsset: function (deviceDetails) {
            return apiService.post(deviceDetails, '/s2/api/assets/?update=true');
        },
        pushPullConfig: function (requestData) {
            return apiService.post(requestData, '/s2/api/assets/device/config');
        },
        getAsset: function(assetId){
            return apiService.get('/s2/api/assets/' + assetId);
        },
        deleteAsset: function(requestData){
            return apiService.post(requestData, '/s2/api/assets/delete');
        },
        getModelSuggestions: function(query) {
            var url = '/s2/api/assets/model';
            if(checkNotNullEmpty(url)) {
                url += '?query=' + query;
            }
            return apiService.get(url);
        }
    }
}]);