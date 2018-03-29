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

angular.module('logistimo.storyboard.temperatureStatusDonutWidget', [])
    .config(function (widgetsRepositoryProvider) {
        widgetsRepositoryProvider.addWidget({
            id: "temperatureStatusDonutWidget",
            name: "asset.status.donut",
            templateUrl: "plugins/storyboards/assets/temperature-status-donut-widget/temperature-status-donut-widget.html",
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
                    nameKey: 'include.entity.tag',
                    type: 'entityTag'
                },
                {
                    nameKey: 'exclude.entity.tag',
                    type: 'exEntityTag'
                }
            ],
            defaultHeight: 3,
            defaultWidth: 4
        });
    })
    .controller('temperatureStatusDonutWidgetController',
    ['$scope', 'dashboardService', 'domainCfgService', 'INVENTORY', function ($scope, dashboardService,
                                                                              domainCfgService, INVENTORY) {
        var filter = angular.copy($scope.widget.conf);
        var tempPieColors, tempPieOrder, asset = '';
        var fDate = (checkNotNullEmpty(filter.date) ? formatDate(filter.date) : undefined);
        $scope.showChart = false;
        $scope.wloading = true;
        $scope.showError = false;
        $scope.totalAssetsText = '';
        function setFilters() {

            if (checkNotNullEmpty(filter.assetStatus)) {
                var assetStatus = $scope.assetStatus = filter.assetStatus;
                if (assetStatus == 'tn') {
                    tempPieColors[1] = tempPieColors[3];
                    tempPieColors[2] = tempPieColors[3];
                    $scope.widget.conf.title = "Normal";
                } else if (assetStatus == 'tl') {
                    tempPieColors[0] = tempPieColors[3];
                    tempPieColors[2] = tempPieColors[3];
                    $scope.widget.conf.title = "Freezing";
                } else if (assetStatus == 'th') {
                    tempPieColors[0] = tempPieColors[3];
                    tempPieColors[1] = tempPieColors[3];
                    $scope.widget.conf.title = "Heating";
                } else {
                    tempPieColors[0] = tempPieColors[3];
                    tempPieColors[1] = tempPieColors[3];
                    tempPieColors[2] = tempPieColors[3];
                    tempPieColors[3] = "#5F5F5F";
                    $scope.widget.conf.title = "Unknown";
                }
            }

            if(checkNotNullEmpty($scope.widget.conf.title)) {
                $scope.widget.conf.title = $scope.widget.conf.title?$scope.widget.conf.title.toUpperCase():"";
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


        domainCfgService.getSystemDashboardConfig().then(function (data) {
            var domainConfig = angular.fromJson(data.data);
            tempPieColors = domainConfig.pie.tc;
            tempPieOrder = domainConfig.pie.to;
        }).then(function () {
            setFilters();
        }).then(function () {
            getData();
        });

        function getData() {
            var chartData = [], totalAssets = 0;
            dashboardService.get(undefined, undefined, $scope.exFilter, $scope.exType, $scope.period,
                $scope.widget.conf.tPeriod, asset, constructModel(filter.entityTag), fDate,
                constructModel(filter.exEntityTag), false).then(function (data) {
                if(!checkNullEmptyObject(data.data.tempDomain)) {
                    chartData = constructPieData(data.data.tempDomain, tempPieColors, tempPieOrder, INVENTORY,
                        $scope.mapEvent, undefined);
                    var normalPercent = getPercent(data.data.tempDomain, $scope.assetStatus);
                    normalPercent = formatDecimal(normalPercent);
                    totalAssets = getItemCount(data.data.tempDomain, $scope.assetStatus);
    
                    if (totalAssets > 1) {
                        $scope.totalAssetsText = totalAssets + " assets";
                    } else {
                        $scope.totalAssetsText = totalAssets + " asset";
                    }
                    setWidgetData(normalPercent, chartData);
                }else{
                    $scope.noDataToRender();
                }
                }).catch(function error(msg) {
                    $scope.noDataToRender();
                    showError(msg, $scope);
                }).finally(function () {
                    $scope.loading = false;
                    $scope.wloading = false;
                });
        }

        function setWidgetData(centerLabelPercent, chartData) {
            var radius = getDonutRadius($scope.widget.width,$scope.widget.height);
            $scope.temperatureStatusDonutWidget = {
                wId: $scope.widget.id,
                cType: "doughnut2d",
                copt: {
                    theme: "fint",
                    doughnutRadius: radius.doughnutRadius,
                    pieRadius: radius.pieRadius,
                    showLabels: "0",
                    showPercentValues: "0",
                    showLegend: "0",
                    showValues: "0",
                    defaultCenterLabel: centerLabelPercent,
                    alignCaptionWithCanvas: "1",
                    showToolTip: "0",
                    centerLabelFontSize: 19,
                    centerLabelFont : 'Lato',
                    centerLabelFontColor: "#d2d2d2",
                    chartTopMargin: -30,
                    bgColor: "#272727",
                    bgAlpha : 100,
                    baseFontColor:"#d2d2d2",
                    baseFont : "Lato"
    
                },
                cdata: chartData,
                computedWidth: '100%',
                computedHeight: parseInt($scope.widget.computedHeight, 10) - 30
            };
            $scope.wloading = false;
            $scope.showChart = true;
        }
    }]);

logistimoApp.requires.push('logistimo.storyboard.temperatureStatusDonutWidget');
