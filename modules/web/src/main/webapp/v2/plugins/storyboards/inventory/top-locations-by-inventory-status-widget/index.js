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
angular.module('logistimo.storyboard.topLocationsByInventoryStatusWidget', [])
    .config(function (widgetsRepositoryProvider) {
        widgetsRepositoryProvider.addWidget({
            id: "topLocationsByInventoryStatusWidget",
            name: "inventory.top.ten.locations",
            templateUrl: "plugins/storyboards/inventory/" +
            "top-locations-by-inventory-status-widget/top-locations-by-inventory-status-widget.html",
            editTemplateUrl: "plugins/storyboards/inventory/edit-template.html",
            templateFilters: [
                {
                    nameKey: 'inventory.status',
                    type: 'topTenLocType'
                },
                {
                    nameKey: 'material.upper',
                    type: 'material'
                },
                {
                    nameKey: 'filter.material.tag',
                    type: 'materialTag'
                },
                {
                    nameKey: 'include.entity.tag',
                    type: 'entityTag'
                },
                {
                    nameKey: 'exclude.entity.tag',
                    type: 'exEntityTag'
                },
                {
                    nameKey: 'date',
                    type: 'date'
                },
                {
                    nameKey: 'period.since',
                    type: 'period'
                }
            ],
            defaultHeight: 4,
            defaultWidth: 3
        });
    })
    .controller('topLocationsByInventoryStatusWidgetController',
    ['$scope', '$timeout', 'dashboardService', 'domainCfgService', 'INVENTORY', '$sce',
        function ($scope, $timeout, dashboardService, domainCfgService, INVENTORY, $sce) {
            var filter = angular.copy($scope.widget.conf);
            var invPieColors, invPieOrder, mapRange, mapColors, level, barColor;
            level = getLevel();
            var fDate = (checkNotNullEmpty(filter.date) ? formatDate(filter.date) : undefined);
            $scope.showChart = false;
            $scope.wloading = true;
            $scope.showError = false;
            domainCfgService.getSystemDashboardConfig().then(function (data) {
                var domainConfig = angular.fromJson(data.data);
                mapColors = domainConfig.mc;
                mapRange = domainConfig.mr;
                mapRange['avlbl'] = mapRange['n'];
                mapColors['avlbl'] = mapColors['n'];
                $scope.mr = mapRange;
                $scope.mc = mapColors;
                invPieColors = domainConfig.pie.ic;
                invPieOrder = domainConfig.pie.io;
                $scope.mapEvent = invPieOrder[0];
                $scope.init();
            });

            $scope.init = function () {
                domainCfgService.getMapLocationMapping().then(function (data) {
                    if (checkNotNullEmpty(data.data)) {
                        $scope.locationMapping = angular.fromJson(data.data);
                        setFilters();
                        loadLocationMap();
                    }else{
                        $scope.noDataToRender();
                    }
                });
            };

            function setFilters() {
                if (checkNotNullEmpty(filter.period)) {
                    var p = filter.period;
                    if (p == '0' || p == '1' || p == '2' || p == '3' || p == '7' || p == '30') {
                        $scope.period = p;
                    }
                } else {
                    $scope.period = "0";
                }

                if (checkNotNullEmpty(filter.topTenLocType)) {
                    var topTenLocType = filter.topTenLocType;
                        $scope.topTenLocType = topTenLocType;
                        if ($scope.topTenLocType == 0) {
                            $scope.mapEvent = invPieOrder[0];
                            barColor = invPieColors[3];
                        } else if ($scope.topTenLocType == 1) {
                            $scope.mapEvent = invPieOrder[1];
                            barColor = invPieColors[0];
                        } else if ($scope.topTenLocType == 2) {
                            $scope.mapEvent = invPieOrder[2];
                            barColor = invPieColors[1];
                        } else if ($scope.topTenLocType == 3){
                            $scope.mapEvent = invPieOrder[3];
                            barColor = invPieColors[2];
                        } else {
                            $scope.mapEvent = 'avlbl';
                            barColor = invPieColors[3];
                        }
                } else {
                    $scope.topTenLocType = "0";
                }

                if (checkNotNullEmpty(filter.materialTag)) {
                    $scope.exFilter = constructModel(filter.materialTag);
                    $scope.exType = 'mTag';
                } else if (checkNotNullEmpty(filter.material)) {
                    $scope.exFilter = filter.material.id;
                    $scope.exType = 'mId';
                }
            }

            function loadLocationMap() {
                dashboardService.get(undefined, undefined, $scope.exFilter, $scope.exType, $scope.period, undefined,
                    undefined, constructModel(filter.entityTag), fDate, constructModel(filter.exEntityTag),
                    false).then(function (data) {
                    if(checkNullEmptyObject(data.data)) {
                        $scope.noDataToRender();
                    }
                    $scope.dashboardView = data.data;
                    if ($scope.mapEvent == 'avlbl') {
                        $scope.dashboardView.inv['avlbl'] = getAvailable($scope.dashboardView.inv);
                    }
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
                        invPieOrder, $timeout, true, barColor);
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
                "valueFontColor": "#000000",
                "theme": "fint",
                "exportEnabled": 0,
                "yAxisMaxValue": 100,
                "captionFontSize": "12",
                "captionAlignment": "center",
                "chartLeftMargin": "50",
                "captionFontSize": "14",
                "captionFontBold":1,
                "bgColor": "#272727",
                "bgAlpha" : "100",
                "canvasBgAlpha": "100",
                "canvasBgColor" : "#272727",
                "baseFont" : "Lato"
            };
            
            $scope.chartTitle = "Top 10 " + level;

            function setWidgetData() {
                $scope.topLocationsByInventoryStatusWidget = {
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
            }
        }]);

logistimoApp.requires.push('logistimo.storyboard.topLocationsByInventoryStatusWidget');
