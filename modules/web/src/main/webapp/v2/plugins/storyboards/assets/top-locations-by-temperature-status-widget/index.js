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
 * Created by yuvaraj on 13/11/17.
 */
angular.module('logistimo.storyboard.topLocationsByTemperatureStatusWidget', [])
    .config(function (widgetsRepositoryProvider) {
        widgetsRepositoryProvider.addWidget({
            id: "topLocationsByTemperatureStatusWidget",
            name: "asset.top.ten.locations",
            templateUrl: "plugins/storyboards/assets/" +
            "top-locations-by-temperature-status-widget/top-locations-by-temperature-status-widget.html",
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
    .controller('topLocationsByTemperatureStatusWidgetController',
    ['$scope', '$timeout', 'dashboardService', 'domainCfgService', 'INVENTORY', '$sce',
        function ($scope, $timeout, dashboardService, domainCfgService, INVENTORY, $sce) {
            var filter = angular.copy($scope.widget.conf);
            var tempPieColors, tempPieOrder, mapRange, mapColors, asset = '', level, barColor,invPieColors;
            level = getLevel();
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
                invPieColors = domainConfig.pie.ic;
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
                });
            };

            function setFilters() {

                if (checkNotNullEmpty(filter.assetStatus) && checkNotNullEmpty($scope.widget.conf.assetStatus)) {
                    $scope.mapEvent = $scope.widget.conf.assetStatus;
                    if($scope.mapEvent=='tn'){
                        barColor = invPieColors[3];
                    }else if($scope.mapEvent=='tl'){
                        barColor = invPieColors[2];
                    }else if($scope.mapEvent=='th'){
                        barColor = invPieColors[0];
                    }else if($scope.mapEvent=='tu'){
                        barColor = "#cccccc";
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
                    $scope.dashboardView = data.data;
                    var linkText;
                    if ($scope.dashboardView.mLev == "country") {
                        linkText = $scope.locationMapping.data[$scope.dashboardView.mTy].name;
                        $scope.mapType = "maps/" + linkText;
                    } else if ($scope.dashboardView.mLev == "state") {
                        linkText = $scope.dashboardView.mTyNm;
                        $scope.mapType = "maps/" + $scope.dashboardView.mTy;
                    } else {
                        linkText =
                            $scope.dashboardView.mTyNm.substr($scope.dashboardView.mTyNm.lastIndexOf("_") + 1);
                        $scope.showMap = false;
                        $scope.showSwitch = false;
                        $scope.mapData = [];
                    }
                    constructMapData($scope.mapEvent, true, $scope, INVENTORY, $sce, mapRange, mapColors,
                        tempPieOrder, $timeout, true, barColor);
                    setWidgetData();
                    }).catch(function error(msg) {
                        $scope.noDataToRender();
                        showError(msg, $scope);
                    }).finally(function () {
                        $scope.loading = false;
                        $scope.wloading = false;
                    });
            };

            function getLevel(){
                if(checkNullEmpty($scope.dstate)) {
                    return 'country';
                }else if(checkNullEmpty($scope.ddist)) {
                    return 'state';
                }else {
                    return 'district'
                }
            }
            if(checkNotNullEmpty(level)){
                if(level == 'country'){
                    level = 'states'
                }else if (level == 'state'){
                    level = 'districts'
                }else if (level == 'district'){
                    level = 'stores'
                }
            }

            $scope.barOpt = {
                "showAxisLines": "0",
                "theme": "fint",
                "exportEnabled": 0,
                "yAxisMaxValue": 100,
                "captionFontSize": "12",
                "captionAlignment": "center",
                "chartLeftMargin": "50",
                "bgColor": "#272727",
                "bgAlpha" : "100",
                "canvasBgAlpha": "100",
                "canvasBgColor" : "#272727",
                "baseFont" : "Lato",
                "baseFontColor" : "#d2d2d2"
            };
    
            $scope.chartTitle = "Top 10 " + level;

            function setWidgetData() {
                $scope.topLocationsByTemperatureStatusWidget = {
                    wId: $scope.widget.id,
                    cType: 'bar2d',
                    copt: $scope.barOpt,
                    cdata: $scope.barData,
                    computedWidth: '90%',
                    computedHeight: parseInt($scope.widget.computedHeight, 10) - 50,
                    colorRange: $scope.mapRange,
                    markers: $scope.markers
                };
                $scope.wloading = false;
                $scope.showChart = true;
                if(checkNullEmptyObject($scope.barData)){
                    $scope.noDataToRender();
                }
            }
        }]);

logistimoApp.requires.push('logistimo.storyboard.topLocationsByTemperatureStatusWidget');
