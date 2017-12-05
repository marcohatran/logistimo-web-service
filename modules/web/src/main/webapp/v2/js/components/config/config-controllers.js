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

var systemCfgControllers = angular.module('systemCfgControllers', []);
systemCfgControllers.controller('SystemConfigController', ['$scope', 'configService', 'requestContext',
    function ($scope, configService, requestContext) {
        var renderContext = requestContext.getRenderContext("manage.system-config", "config_type");
        $scope.configTypes = ["generalconfig", "dashboardconfig", "maplocationconfig", "temperaturesysconfig",
            "currencies", "smsconfig", "languages", "languages_mobile", "locations", "optimization", "reports"];
        if (checkNotNullEmpty(requestContext.getParam("config_type"))) {
            $scope.configType = requestContext.getParam("config_type");
        } else {
            $scope.configType = "generalconfig";
        }

        $scope.config = "";

        $scope.fetchJson = function () {
            $scope.loading = true;
            $scope.config = "";
            configService.getConfigJson($scope.configType).then(function (data) {
                $scope.config = data.data;
            }).catch(function (err) {
                $scope.showError(err);
            }).finally(function () {
                $scope.loading = false;
            })
        };

        $scope.fetchJson();

        $scope.saveConfig = function () {
            $scope.loading = true;
            try {
                a = JSON.parse($scope.config);
            } catch (e) {
                $scope.showError("Oops !! Looks like an invalid json, please check.");
            }
            configService.updateConfigJson($scope.configType, $scope.config).then(function (data) {
                $scope.showSuccess(data.data);
                $scope.fetchJson();
            }).catch(function (err) {
                $scope.showError(err);
            }).finally(function () {
                $scope.loading = false;
            })
        };

        $scope.$on("requestContextChanged", function () {
            if (!renderContext.isChangeRelevant()) {
                return;
            }
            if ($scope.configType != requestContext.getParam("config_type")) {
                $scope.config = "";
                $scope.configType = requestContext.getParam("config_type");
                $scope.fetchJson();
            }

        });

        $scope.$watch("configType", function (newVal, oldVal) {
            if (newVal != oldVal) {
                $scope.fetchJson();
            }
        });


    }
]);

systemCfgControllers.controller('SimulateDataController', ['$scope', 'configService',
    function ($scope, configService) {
        $scope.model = {
            startDate: "01/01/2017",
            stockOnHand: "0",
            duration: 90,
            periodicity: "d",
            issueMean: "50",
            issueStdDev: "25",
            receiptMean: "150",
            receiptStdDev: "40",
            zeroStockDaysLow: "1",
            zeroStockDaysHigh: "2"
        };
        $scope.entity = {};

        function isValid() {
            if (checkNullEmptyObject($scope.entity)) {
                $scope.showError("Please choose entity");
                return false;
            }
            if ($scope.model.periodicity != "d" && $scope.model.periodicity != "w") {
                $scope.showError("Periodicity should be d or w");
                return false;
            }
            return true;
        }

        $scope.simulateData = function () {
            $scope.loading = true;
            if (isValid()) {
                $scope.model.entityId = $scope.entity.id;
                configService.simulateData($scope.model).then(function (data) {
                    $scope.showSuccess(data.data);
                }).catch(function () {
                    $scope.showError("Error occurred while simulating data");
                }).finally(function () {
                    $scope.loading = false;
                })
            }
        };
    }
]);
