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

/**
 * Created by naveensnair on 24/11/17.
 */

angular.module('logistimo.storyboard.abnormalStockTrend', [])
    .config(function (widgetsRepositoryProvider) {
        widgetsRepositoryProvider.addWidget({
            id: "abnormalStockTrendWidget",
            name: "Abnormal stock trend",
            templateUrl: "plugins/storyboards/inventory/abnormal-stock-trend-widget/abnormal-stock-trend-widget.html",
            editTemplateUrl: "plugins/storyboards/inventory/edit-template.html",
            templateFilters: [
                {
                    nameKey:'filter.material.tag',
                    type: 'materialTag'
                },
                {
                    nameKey: 'material.upper',
                    type: 'material'
                },
                {
                    nameKey: 'tagentity',
                    type: 'entityTag'
                },
                {
                    nameKey: 'kiosk',
                    type: 'entity'
                },
                {
                    nameKey: 'period',
                    type: 'periodicity'
                }
            ],
            defaultHeight: 6,
            defaultWidth: 8
        });
    })
    .controller('stockAvailabilityTrendWidgetController', ['$scope', 'reportsServiceCore', '$timeout', function ($scope, reportsServiceCore, $timeout) {
        $scope.filter = angular.copy($scope.widget.conf);
        var MAX_MONTHS = 11;
        var MAX_WEEKS = 15;
        var MAX_DAYS = 31;

        function setDefaultFilters(){
            $scope.filter.compare = "none";
            if(checkNullEmpty($scope.filter.periodicity)) {
                $scope.filter.periodicity = "m";
            }
            $scope.filter.type = "ias";
        }

        function setFilters(){
            if($scope.isDef($scope.widget.conf.period) && checkNotNullEmpty($scope.widget.conf.period)) {
                $scope.filter.periodicity = $scope.widget.conf.period;
            } else {
                $scope.widget.conf.period = "m";
            }
            if($scope.isDef($scope.widget.conf.materialTag) && checkNotNullEmpty($scope.widget.conf.materialTag)) {
                $scope.filter.mtag = $scope.widget.conf.materialTag[0].id;
            } else if(checkNullEmpty($scope.widget.conf.materialTag)) {
                delete $scope.filter['mtag'];
            }

            if($scope.isDef($scope.widget.conf.entityTag) && checkNotNullEmpty($scope.widget.conf.entityTag)) {
                $scope.filter.etag = $scope.widget.conf.entityTag[0].id;
            } else if(checkNullEmpty($scope.widget.conf.entityTag)) {
                delete $scope.filter['etag']
            }

            if($scope.isDef($scope.widget.conf.entity) && checkNotNullEmpty($scope.widget.conf.entity)) {
                $scope.filter.entity = $scope.widget.conf.entity.id;
            } else if(checkNullEmpty($scope.widget.conf.entity)) {
                delete $scope.filter['entity']
            }

            if($scope.isDef($scope.widget.conf.material) && checkNotNullEmpty($scope.widget.conf.material)) {
                $scope.filter.mat = $scope.widget.conf.material.id;
            } else if(checkNullEmpty($scope.widget.conf.material)) {
                delete $scope.filter['mat']
            }

            var fromDate = new Date();
            fromDate.setHours(0, 0, 0, 0);
            if($scope.widget.conf.period == 'm') {
                $scope.dateMode = 'month';
                fromDate.setDate(1);
                fromDate.setMonth(fromDate.getMonth() - MAX_MONTHS);
                $scope.filter.from = new Date(fromDate);
                var toDate = new Date();
                toDate.setHours(0, 0, 0, 0);
                toDate.setDate(1);
                $scope.filter.to = toDate;
            } else if($scope.widget.conf.period == 'w'){
                fromDate.setDate(fromDate.getDate() - (MAX_WEEKS - 1) * 7);
                fromDate.setDate(fromDate.getDate() + (fromDate.getDay() == 0 ? -6 : 1 - fromDate.getDay()));
                $scope.filter.from = new Date(fromDate);
                var toDate = new Date();
                toDate.setHours(0, 0, 0, 0);
                toDate.setDate(toDate.getDate() + (toDate.getDay() == 0 ? -6 : 1 - toDate.getDay()));
                $scope.filter.to = toDate;
                $scope.dateMode = 'week';
            } else if($scope.widget.conf.period == 'd') {
                fromDate.setDate(fromDate.getDate() - MAX_DAYS + 1);
                $scope.filter.from = new Date(fromDate);
                $scope.filter.to = new Date();
                $scope.filter.to.setHours(0, 0, 0, 0);
                $scope.dateMode = 'day';
            }
        }

        function setChartOptions(){
            $scope.cOptions = {
                "theme": "fint",
                "subCaptionFontSize": 10,
                "yAxisNamePadding": 20,
                "rotateValues": "0",
                "placevaluesInside": 0,
                "valueFontColor": "#000000"
            };
            $scope.cHeight = $scope.widget.computedHeight;
            $scope.cWidth = "90%";
            $scope.cType="mscombi2d";
        }

        function setChartData(localData, chartData, level) {
            if(!localData) {
                $scope.loading = true;
                chartData = angular.copy($scope.allChartData);
            }
            var linkDisabled = level == "d" || $scope.filter.periodicity == "d";
            var cLabel = getReportFCCategories(chartData, getReportDateFormat(level, $scope.filter));
            var compareFields = [];
            angular.forEach(chartData, function (d) {
                if (compareFields.indexOf(d.value[0].value) == -1) {
                    compareFields.push(d.value[0].value);
                }
            });
            var filterSeriesIndex = undefined;
            var isCompare = false;
            if(compareFields.length > 1) {
                if(compareFields.indexOf("") != -1) {
                    compareFields.splice(compareFields.indexOf(""), 1);
                }
                filterSeriesIndex = 0;
                isCompare = true;
            }
            var cData = [];
            for (var i = 0; i < compareFields.length; i++) {
                cData[i] = getReportFCSeries(chartData, parseInt("0")*1 +
                    parseInt("2"), compareFields[i],
                    "line", linkDisabled, filterSeriesIndex, isCompare ? "0" : "1");
            }
            $scope.cOptions.caption = "% of inventory items available (100% availability)";
            $scope.cOptions.subcaption = getReportCaption($scope.filter);
            $scope.cOptions.yAxisName = $scope.cOptions.caption;
            if($scope.filter.periodicity != "m" && cLabel.length > 10) {
                $scope.cOptions.rotateLabels = "1";
            } else {
                $scope.cOptions.rotateLabels = undefined;
            }
            $scope.cData = cData;
            $scope.cLabel = cLabel;
            $scope.chartData = chartData;
            if(checkNotNullEmpty($scope.cData)) {
                $scope.renderChart = true;
            }

            if(!localData) {
                $timeout(function () {
                    $scope.loading = false;
                }, 200);
            }
        }

        function getData(){
            var selectedFilters = angular.copy($scope.filter);
            selectedFilters.from = formatDate2Url(selectedFilters.from);
            selectedFilters.to = formatDate2Url(selectedFilters.to);
            reportsServiceCore.getReportData(angular.toJson(selectedFilters)).then(function (data) {
                $scope.noData = true;
                if (checkNotNullEmpty(data.data)) {
                    var chartData = angular.fromJson(data.data);
                    chartData = sortByKeyDesc(chartData, 'label');
                    if (selectedFilters['level'] == "d") {
                        $scope.allDChartData = chartData;
                    } else {
                        $scope.allChartData = chartData;
                    }
                    setChartData(false, chartData, selectedFilters.level);
                    $scope.noData = false;
                }
            }).catch(function error(msg) {
            }).finally(function () {
                if (selectedFilters['level'] == "d") {
                    $scope.dLoading = false;
                } else {
                    $scope.loading = false;
                }
                $scope.hideLoading();
            });
        }

        function updateLabels() {
            var labels = {m: "monthly", w: "weekly", d: "daily"};
            $scope.filterLabels[$scope.resourceBundle['periodicity']] =
                labels[$scope.filter.periodicity] ? $scope.resourceBundle[labels[$scope.filter.periodicity]] : undefined;
        }

        function init() {
            $scope.renderChart = false;
            $scope.filterLabels = {};
            setDefaultFilters();
            setFilters();
            setChartOptions();
            updateLabels();
            getData();
        }

        init();
    }]);

logistimoApp.requires.push('logistimo.storyboard.abnormalStockTrend');