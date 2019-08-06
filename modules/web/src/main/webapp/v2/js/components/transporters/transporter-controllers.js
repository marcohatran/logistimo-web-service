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

var transporterControllers = angular.module('transporterControllers', []);
transporterControllers.controller('TransporterListController', ['$scope', 'transporterServices','requestContext', '$location', 'TRANSPORTER_TYPES',
    function ($scope, transporterServices, requestContext, $location, TRANSPORTER_TYPES) {
        $scope.wparams = [["tname", "tname"]];
        $scope.localFilters = ['tname'];

        $scope.selectAll = function (newval) {
            for (var item in $scope.filtered) {
                $scope.filtered[item]['selected'] = newval;
            }
        };

        $scope.init = function () {
            $scope.selAll = false;
            $scope.tname = requestContext.getParam("tname") || "";
        };
        $scope.init();
        ListingController.call(this, $scope, requestContext, $location);
        $scope.fetch = function () {
            $scope.loading = true;
            $scope.showLoading();
            transporterServices.getDomainTransporters($scope.tname, $scope.offset, $scope.size).then(function (data) {
                $scope.transporters = data.data['results'];
                $scope.setResults(data.data);
                $scope.loading = false;
                $scope.hideLoading();
                fixTable();
                $scope.noTransportersFound = checkNullEmpty($scope.transporters);
            }).catch(function error(msg) {
                $scope.setResults(null);
                $scope.showErrorMsg(msg);
            }).finally(function (){
                $scope.loading = false;
                $scope.hideLoading();
            });
        };
        $scope.fetch();

        $scope.deleteTransporters = function () {
            var tids = "";
            for (var item in $scope.filtered) {
                if ($scope.filtered[item].selected) {
                    if($scope.currentDomain == $scope.filtered[item].sdid) {
                        if(checkNotNullEmpty(tids)) {
                            tids += ',';
                        }
                        tids += $scope.filtered[item].id;
                    }
                }
            }
            if (!confirm($scope.resourceBundle.removetransporterconfirmmsg)) {
                return;
            }
            transporterServices.deleteTransporters(tids).then(function(data) {
                $scope.showSuccess(data.data['message']);
                $scope.fetch();
            }).catch(function error(msg) {
                $scope.showErrorMsg(msg);
            }).finally(function (){
                $scope.loading = false;
                $scope.hideLoading();
            })
        };

        $scope.getTransporterTypeDisplayName = function(type) {
            for(var key in TRANSPORTER_TYPES) {
                if(type == key && TRANSPORTER_TYPES.hasOwnProperty(key)) {
                    return $scope.resourceBundle[TRANSPORTER_TYPES[key]['name']];
                }
            }
        };

        $scope.reset = function() {
            $scope.tname = "";
        };
    }
]);
transporterControllers.controller('TransporterDetailMenuController', ['$scope', 'transporterServices','requestContext', 'configService', '$location',
    function ($scope, transporterServices, requestContext, configService, $location) {

        $scope.init = function() {
            $scope.transporter = {};
        };
        $scope.init();
        LocationController.call(this, $scope, configService);
        $scope.transporterId = requestContext.getParam("transporterId");
        function getTransporterDetails() {
            if(checkNotNullEmpty($scope.transporterId)) {
                $scope.showLoading();
                $scope.loading = true;
                transporterServices.getTransporterDetails($scope.transporterId).then(function (data) {
                    $scope.transporter = data.data;
                    $scope.transporter.cnt = $scope.getCountryNameByCode($scope.transporter.cnt);
                }).catch(function error(msg) {
                    $scope.showErrorMsg(msg);
                }).finally(function () {
                    $scope.hideLoading();
                    $scope.loading = false;
                });
            }
        }

        $scope.$watch('countries', function(countries) {
            if(!checkNullEmptyObject(countries)) {
                getTransporterDetails();
            }
        });
    }
]);
transporterControllers.controller('AddTransporterController', ['$scope', 'transporterServices','configService','requestContext', '$location',
    function ($scope, transporterServices, configService, requestContext, $location) {

        $scope.init = function() {
            $scope.invalidPhnm = false;
            $scope.transporter = {type: 'owned'};
            $scope.secret_readonly = true;
            $scope.tspSysConfigs = [];
            $scope.edit = false;
            fetchTspConfig();
        };
        $scope.init();
        LocationController.call(this, $scope, configService);

        $scope.defineWatchers = function () {
            $scope.$watch("transporter.cnt", function (newval, oldval) {
                if (newval != oldval && !$scope.skipWatch) {
                    $scope.transporter.st = "";
                    $scope.transporter.ds = "";
                    $scope.transporter.tlk = "";
                }
            });
            $scope.$watch("transporter.st", function (newval, oldval) {
                if (newval != oldval &&  !$scope.skipWatch) {
                    $scope.transporter.ds = "";
                    $scope.transporter.tlk = "";
                }
            });
            $scope.$watch("transporter.ds", function (newval, oldval) {
                if (newval != oldval &&  !$scope.skipWatch) {
                    $scope.transporter.tlk = "";
                }
            });
        };

        $scope.editTransporterId = requestContext.getParam("tid");
        if(checkNotNullEmpty($scope.editTransporterId)) {
            $scope.edit = true;
            $scope.showLoading();
            $scope.loading = true;
            transporterServices.getTransporterDetails($scope.editTransporterId).then(function (data) {
                $scope.transporter = data.data;
                initTransporterFields($scope.transporter);
                $scope.setCountry($scope.transporter.cnt);
                $scope.setState($scope.transporter.st);
                $scope.setDistrict($scope.transporter.ds);
                $scope.defineWatchers();
            }).catch(function error(msg) {
                $scope.showErrorMsg(msg);
            }).finally(function () {
                $scope.hideLoading();
                $scope.loading = false;
            });
        }
        $scope.validateMobilePhone = function () {
            $scope.invalidPhnm = validateMobile($scope.transporter.phnm);
            return !$scope.invalidPhnm;
        };

        function initTransporterFields(transporter) {
            $scope.onEnableApiFlagChange(transporter['is_api_enabled']);
            transporter['secret_updated'] = false;
        }

        $scope.resetTransporter = function() {
            $scope.transporter = {type: 'owned'};
            $scope.uVisited.tname = false;
            $scope.uVisited.phnm = false;
            $scope.uVisited.cnt = false;
            $scope.uVisited.st = false;
            $scope.secret_readonly = true;
        };

        $scope.createTransporter = function() {
            $scope.loading = true;
            $scope.showLoading();
            transporterServices.createTransporter($scope.transporter).then(function (data) {
                $scope.resetTransporter();
                $scope.showSuccess(data.data['message']);
            }).catch(function error(msg) {
                $scope.showErrorMsg(msg);
            }).finally(function () {
                $scope.loading = false;
                $scope.hideLoading();
            });
        };

        $scope.updateTransporter = function() {
            if($scope.edit && checkNotNullEmpty($scope.editTransporterId)) {
                $scope.loading = true;
                $scope.showLoading();
                transporterServices.updateTransporter($scope.editTransporterId, $scope.transporter).then(function (data) {
                    $scope.resetTransporter();
                    $scope.showSuccess(data.data['message']);
                    $scope.$back();
                }).catch(function error(msg) {
                    $scope.showErrorMsg(msg);
                }).finally(function () {
                    $scope.hideLoading();
                    $scope.loading = false;
                });
                return true;
            }
        };

        $scope.onEnableApiFlagChange = function(val) {
            if(!val) {
                clearTspConfigDetails();
            }
        };

        $scope.onTspChange = function() {
            var config = getTspConfigById($scope.transporter.tsp_id);
            if(checkNotNullEmpty(config)) {
                $scope.transporter.ac_id = config['account'];
                $scope.transporter.url = config['url'];
            } else {
                clearTspConfigDetails();
            }
        };

        function clearTspConfigDetails() {
            $scope.transporter.ac_id = '';
            $scope.transporter.url = '';
            $scope.transporter.secret = '';
            $scope.transporter.tsp_id = '';
        }

        function fetchTspConfig() {
            $scope.loading = true;
            $scope.showLoading();
            configService.getTransporterConfig(true).then(function (data) {
                $scope.tspSysConfigs = data.data['transporters'];
            }).catch(function error(msg) {
                $scope.showErrorMsg(msg);
            }).finally(function () {
                $scope.hideLoading();
                $scope.loading = false;
            });
        }

        function getTspConfigById(tspId) {
            var filtered = $scope.tspSysConfigs.filter(function(c) {
                return c.id == tspId;
            });
            if(filtered.length > 0) {
                return filtered[0];
            }
        }
    }
]);
