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
 * Created by yuvaraj on 10/11/17.
 */
angular.module('logistimo.storyboard.temperatureStatusMapWidget', [])
    .config(function (widgetsRepositoryProvider) {
        widgetsRepositoryProvider.addWidget({
            id: "temperatureStatusMapWidget",
            name: "asset.status.map",
            templateUrl: "plugins/storyboards/assets/temperature-status-map-widget/temperature-status-map-widget.html",
            editTemplateUrl: "plugins/storyboards/assets/asset-edit-template.html",
            templateFilters: [
                {
                    nameKey: 'asset.status',
                    type: 'assetStatus'
                },
                {
                    nameKey: 'asset.type',
                    type: 'assetType'
                },
                {
                    nameKey: 'period.since',
                    type: 'tPeriod'
                },
                {
                    nameKey: 'exclude.temp.state',
                    type: 'exTempState'
                },
                {
                    nameKey: 'include.entity.tag',
                    type: 'entityTag'
                },
                {
                    nameKey: 'exclude.entity.tag',
                    type: 'exEntityTag'
                }
            ],
            defaultHeight: 2,
            defaultWidth: 3
        });
    })
    .controller('temperatureStatusMapWidgetController',
    ['$scope', '$timeout', 'dashboardService', 'domainCfgService', 'INVENTORY', '$sce',
        function ($scope, $timeout, dashboardService, domainCfgService, INVENTORY, $sce) {
            var filter = angular.copy($scope.widget.conf);
            var tempPieColors, tempPieOrder, mapRange, mapColors, asset = '',level='',mapName='';
            var fDate = (checkNotNullEmpty(filter.date) ? formatDate(filter.date) : undefined);
            $scope.showChart = false;
            $scope.wloading = true;
            $scope.showError = false;
            domainCfgService.getSystemDashboardConfig().then(function (data) {
                var domainConfig = angular.fromJson(data.data);
                mapColors = domainConfig.mc;
                $scope.mc = mapColors;
                mapRange = domainConfig.mr;
                $scope.mr = mapRange;
                tempPieOrder = domainConfig.pie.to;
                tempPieColors = domainConfig.pie.tc;
                $scope.mapEvent = tempPieOrder[0];
                $scope.init();
            });

            $scope.init = function () {
                domainCfgService.getMapLocationMapping().then(function (data) {
                    if (checkNotNullEmpty(data.data)) {
                        $scope.locationMapping = angular.fromJson(data.data);
                        setFilters();
                        loadLocationMap();
                    }
                })
            };

            function setFilters() {

                if (checkNotNullEmpty(filter.assetStatus) && checkNotNullEmpty($scope.widget.conf.assetStatus)) {
                    $scope.mapEvent = $scope.widget.conf.assetStatus;
                    if($scope.widget.conf.assetStatus == 'tn'){
                        mapName = 'Normality'
                    }else if($scope.widget.conf.assetStatus == 'tl'){
                        mapName = 'Assets freezing'
                    }else if($scope.widget.conf.assetStatus == 'th'){
                        mapName = 'Assets heating'
                    }else if($scope.widget.conf.assetStatus == 'tu'){
                        mapName = 'Assets unknown'
                    }
                } else {
                    $scope.mapType = "tn";
                }
            }

            if (checkNotNullEmpty($scope.widget.conf.asset) && $scope.widget.conf.asset.length > 0) {
                var first = true;
                $scope.widget.conf.asset.forEach(function (data) {
                    if (!first) {
                        asset += "," + data.id;
                    } else {
                        asset += data.id;
                        first = false;
                    }

                });
            }
            ;

            function loadLocationMap() {
                dashboardService.get(undefined, undefined, $scope.exFilter, $scope.exType, $scope.period,
                    $scope.widget.conf.tPeriod, asset, constructModel(filter.entityTag), fDate,
                    constructModel(filter.exEntityTag), false).then(function (data) {
                    if(checkNullEmptyObject(data.data)) {
                        $scope.noDataToRender();
                    }
                    $scope.dashboardView = data.data;
                    var linkText;
                    if ($scope.dashboardView.mLev == "country") {
                        linkText = $scope.locationMapping.data[$scope.dashboardView.mTy].name;
                        $scope.mapType = linkText;
                    } else if ($scope.dashboardView.mLev == "state") {
                        linkText = $scope.dashboardView.mTyNm;
                        $scope.mapType = $scope.dashboardView.mTy;
                    }
                    constructMapData($scope.mapEvent, true, $scope, INVENTORY, $sce, mapRange, mapColors,
                        tempPieOrder, $timeout);
                    setTitle();
                    setWidgetData();
                    }).catch(function error(msg) {
                        $scope.noDataToRender();
                        showError(msg, $scope);
                    }).finally(function () {
                        $scope.loading = false;
                        $scope.wloading = false;
                    });
            };

            function setTitle() {
                if ($scope.dashboardView.mLev == "country") {
                    level = "state";
                } else if ($scope.dashboardView.mLev == "state") {
                    level = "district";
                }
                $scope.mapTitle = mapName + " by " + level;
            }

            function setWidgetData() {
                $scope.temperatureStatusMapWidget = {
                    wId: $scope.widget.id,
                    cType: $scope.mapType,
                    copt: $scope.mapOpt,
                    cdata: $scope.mapData,
                    computedWidth: '100%',
                    computedHeight: parseInt($scope.widget.computedHeight, 10) - 30,
                    colorRange: $scope.mapRange,
                    markers: $scope.markers
                };
                $scope.wloading = false;
                $scope.showChart = true;
            };
        }]);

logistimoApp.requires.push('logistimo.storyboard.temperatureStatusMapWidget');
