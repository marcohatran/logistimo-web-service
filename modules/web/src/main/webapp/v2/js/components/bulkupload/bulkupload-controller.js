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

/**
 * Created by mohan raja on 08/01/15.
 */

var blkUpControllers = angular.module('blkUpControllers', []);

blkUpControllers.controller('BulkUploadController',['$scope','blkUpService','APIService','$timeout',
    function($scope,blkUpService,apiService,$timeout){
        $scope.fileData = '';
        if($scope.uploadType == 'users'){
            $scope.helpFile = 'uploadusersinbulk.htm';
        }else if($scope.uploadType == 'kiosks'){
            $scope.helpFile = 'uploadentitiesinbulk.htm';
        }else if($scope.uploadType == 'materials'){
            $scope.helpFile = 'uploadmaterialsinbulk.htm';
        }
        $scope.uploadURL = function() {
            $scope.uploading = true;
            blkUpService.uploadURL($scope.us.ty).then(function(data) {
                $scope.actionURL = cleanupString(data.data.toString().replace(/"/g,''));
                $scope.uploading = false;
                $scope.urlLoaded = true;
            }).catch(function error(msg) {
                $scope.showErrorMsg(msg);
            });
        };

        $scope.uploadStatus = function(initCall) {
            $scope.loading = true;
            $scope.serr = false;
            $scope.showLoading();
            blkUpService.uploadStatus($scope.uploadType).then(function (data) {
                $scope.us = data.data;
                if(initCall){
                    $scope.uploadURL();
                }
                $scope.fileData = '';
            }).catch(function error(msg) {
                $scope.showErrorMsg(msg);
            }).finally(function(){
                $scope.loading = false;
                $scope.hideLoading();
            });
        };
        $scope.uploadStatus(true);

        $scope.viewErrors = function() {
            $scope.serr = !$scope.serr;
        };

        $scope.uploadPostUrl = function(){
            if($scope.fileData.size > 20 * 1024 * 1024 ) {
                $scope.showWarning($scope.resourceBundle["bulkupload.maximum.file.size.warning"]);
                return;
            }
            var uploadName = "";
            $scope.showLoading();
            blkUpService.uploadPostUrl($scope.actionURL,$scope.fileData, $scope.us.ty).then(function(data){
                $scope.uploadStatus();
                $scope.uploadURL();
                if($scope.uploadType == 'kiosks') {
                    uploadName = $scope.resourceBundle['kiosks.lowercase'];
                } else {
                    uploadName = $scope.uploadType;
                }
                $scope.showSuccess(messageFormat($scope.resourceBundle['bulkupload.successfullyscheduled'], uploadName));
            }).catch(function error(msg) {
                $scope.showErrorMsg(msg);
            }).finally(function(){
                $scope.hideLoading();
            });
        };

        $scope.validate = function(){
            if(checkNullEmpty($scope.fileData)) {
                $scope.showWarning($scope.resourceBundle['upload.csvdata']);
                return false;
            }
            var filename = $scope.fileData.name.split(".");
            var ext = filename[filename.length - 1];
            if(ext != 'csv'){
                $scope.showWarning($scope.resourceBundle['upload.csvdata']);
                return false;
            }
            $scope.uploadTypeMessage = "";
            if($scope.uploadType == 'kiosks'){
                $scope.uploadTypeMessage = $scope.resourceBundle['kiosks.lowercase'];
            }else{
                $scope.uploadTypeMessage = $scope.uploadType;
            }
            if (!confirm(messageFormat($scope.resourceBundle['bulkupload.containcsvdata'], $scope.uploadTypeMessage))) {
                return;
            }
            return true;
        };

        $scope.downloadAssetModels = function () {
            if (!checkNullEmptyObject($scope.assetConfig) && !checkNullEmptyObject($scope.assetConfig.assets)) {
                let assetModelData = '';
                Object.values($scope.assetConfig.assets).forEach(asset => {
                    if (asset.id != 1 && !checkNullEmptyObject(asset.mcs)) {
                        Object.values(asset.mcs).forEach(manufacturer => {
                            if (!checkNullEmptyObject(manufacturer.model)) {
                                Object.values(manufacturer.model).forEach(model => {
                                    assetModelData += `${asset.an},${manufacturer.name},${model.name},${model.capacity}\r\n`;
                                });
                            }
                        });
                    }
                });
                assetModelData = `${$scope.resourceBundle['asset.type']}, ${$scope.resourceBundle['manufacturer']} ${$scope.resourceBundle['name.lower']}` +
                `, ${$scope.resourceBundle['model.name']}, ${$scope.resourceBundle['capacity']}(${$scope.resourceBundle['model.capacity.metric']}) \r\n ${assetModelData}`;
                exportCSV(assetModelData, "Asset_Model", $timeout);
            }
        }

    }
]);

blkUpControllers.controller('CheckUploadController',['$scope','blkUpService',function($scope,blkUpService){
    blkUpService.getManualUploadStatus().then(function(data){
        $scope.manualUpload = data.data;
    }).catch(function error(msg) {
        $scope.showErrorMsg(msg);
    });
}]);

blkUpControllers.controller('ViewBulkUploadController', ['$scope','requestContext','$location','blkUpService','exportService',
    function ($scope,requestContext,$location,blkUpService,exportService) {
        $scope.wparams = [
            ["from", "from","",formatDate2Url],
            ["to", "to","",formatDate2Url],
            ["eid", "entity.id"],
            ["o", "offset"],
            ["s", "size"]
        ];
        $scope.localFilters = ['entity', 'from', 'to'];
        $scope.init = function () {
            $scope.from = parseUrlDate(requestContext.getParam("from")) || "";
            $scope.to = parseUrlDate(requestContext.getParam("to")) || "";
            if (checkNotNullEmpty(requestContext.getParam("eid"))) {
                if(checkNullEmpty($scope.entity) || $scope.entity.id != parseInt(requestContext.getParam("eid"))) {
                    $scope.entity = {id: parseInt(requestContext.getParam("eid")), nm: ""};
                }
            }else{
                $scope.entity = null;
            }
        };
        $scope.init();
        ListingController.call(this, $scope, requestContext, $location);
        $scope.fetch = function () {
            $scope.loading = true;
            $scope.showLoading();
            blkUpService.getUploadTransactions(checkNotNullEmpty($scope.entity)?$scope.entity.id:"", formatDate($scope.from), formatDate($scope.to), $scope.offset, $scope.size).then(function (data) {
                $scope.filtered = data.data.results;
                $scope.setResults(data.data);
            }).catch(function error(msg) {
                $scope.showErrorMsg(msg);
                $scope.filtered = null;
                $scope.setResults(null);
            }).finally(function (){
                $scope.loading = false;
                $scope.hideLoading();
            });
        };
        $scope.fetch();
        $scope.resetFilters = function(){
            $location.$$search = {};
            $location.$$compose();
        };

        $scope.exportData = function (isInfo) {
            if (isInfo) {
                return {
                    filters: getCaption(),
                    type: $scope.resourceBundle['exports.manualtransactions']
                };
            }
            var eid;
            if (checkNotNullEmpty($scope.entity)) {
                eid = $scope.entity.id;
            }
            $scope.showLoading();
            exportService.exportData({
                entity_id: eid,
                from_date: formatDate2Url($scope.from) || undefined,
                end_date: formatDate2Url($scope.to) || undefined,
                titles: {
                    filters: getCaption()
                },
                module: "manualtransactions",
                templateId: "i_manual_transaction"
            }).then(function (data) {
                $scope.showSuccess(data.data);
            }).catch(function error(msg) {
                $scope.showErrorMsg(msg);
            }).finally(function() {
                $scope.hideLoading();
            });
        };

        function getCaption() {
            var caption = getFilterTitle($scope.entity, $scope.resourceBundle['kiosk'], 'nm');
            caption += getFilterTitle(formatDate2Url($scope.from), $scope.resourceBundle['from']);
            caption += getFilterTitle(formatDate2Url($scope.to), $scope.resourceBundle['to']);
            return caption;
        }
    }
]);