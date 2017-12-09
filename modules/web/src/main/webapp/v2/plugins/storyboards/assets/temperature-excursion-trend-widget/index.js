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
 * Created by naveensnair on 27/11/17.
 */

angular.module('logistimo.storyboard.temperatureExcursion', [])
    .config(function (widgetsRepositoryProvider) {
        widgetsRepositoryProvider.addWidget({
            id: "temperatureExcursionWidget",
            name: "temp.excursion.trends",
            templateUrl: "plugins/storyboards/assets/temperature-excursion-trend-widget/temperature-excursion-trend-widget.html",
            editTemplateUrl: "plugins/storyboards/assets/asset-edit-template.html",
            templateFilters: [
                {
                    nameKey: 'title',
                    type: 'title'
                },
                {
                    nameKey: 'asset.type',
                    type: 'assetType'
                },
                {
                    nameKey: 'excursion.type',
                    type: 'excursionType'
                },
                {
                    nameKey: 'period',
                    type: 'periodicity'
                }
            ],
            defaultHeight: 3,
            defaultWidth: 4
        });
    })
    .controller('temperatureExcursionTrendWidgetController', ['$scope', 'reportsServiceCore', '$timeout', function ($scope, reportsServiceCore, $timeout) {
        $scope.filter = angular.copy($scope.widget.conf);
        var MAX_MONTHS = 6;
        var MAX_WEEKS = 15;
        var MAX_DAYS = 31;


        function setDefaultFilters() {
            $scope.filter.compare = "none";
            if (checkNullEmpty($scope.filter.periodicity)) {
                $scope.filter.periodicity = "m";
            }
            $scope.filter.type = "ate";
        }

        function setFilters() {

            if ($scope.isUndef($scope.widget.conf.period) || checkNullEmpty($scope.widget.conf.period)) {
                $scope.widget.conf.period = "m";
                delete $scope.filter["periodicity"];
            }

            if($scope.isUndef($scope.widget.conf.excursionType) || checkNullEmpty($scope.widget.conf.excursionType)) {
                $scope.widget.conf.excursionType = "0";
            }

            if($scope.isDef($scope.widget.conf.asset) && checkNotNullEmpty($scope.widget.conf.asset)) {
                $scope.filter.at = $scope.widget.conf.asset[0].id;
                delete $scope.filter["mt"];
            } else {
                $scope.filter.mt = "2";
                delete $scope.filter["at"];
            }

            var fromDate = new Date();
            fromDate.setHours(0, 0, 0, 0);
            if ($scope.widget.conf.period == 'm') {
                $scope.dateMode = 'month';
                fromDate.setDate(1);
                fromDate.setMonth(fromDate.getMonth() - MAX_MONTHS);
                $scope.filter.from = new Date(fromDate);
                var toDate = new Date();
                toDate.setHours(0, 0, 0, 0);
                toDate.setDate(1);
                toDate.setMonth(toDate.getMonth() - 1);
                $scope.filter.to = toDate;
                $scope.filter.periodicity = "m";
            } else if ($scope.widget.conf.period == 'w') {
                fromDate.setDate(fromDate.getDate() - MAX_WEEKS * 7);
                fromDate.setDate(fromDate.getDate() + (fromDate.getDay() == 0 ? -6 : 1 - fromDate.getDay()));
                $scope.filter.from = new Date(fromDate);
                var toDate = new Date();
                toDate.setHours(0, 0, 0, 0);
                toDate.setDate(toDate.getDate() + (toDate.getDay() == 0 ? -6 : 1 - toDate.getDay()));
                $scope.filter.to = toDate;
                $scope.dateMode = 'week';
                $scope.filter.periodicity = "w";
            } else if ($scope.widget.conf.period == 'd') {
                fromDate.setDate(fromDate.getDate() - MAX_DAYS + 1);
                $scope.filter.from = new Date(fromDate);
                $scope.filter.to = new Date();
                $scope.filter.to.setHours(0, 0, 0, 0);
                $scope.dateMode = 'day';
                $scope.filter.periodicity = "d";
            }
        }

        function setChartOptions() {
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
            $scope.cType = "mscombi2d";
        }

        function setChartData(localData, chartData, level) {
            if (!localData) {
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
            if (compareFields.length > 1) {
                if (compareFields.indexOf("") != -1) {
                    compareFields.splice(compareFields.indexOf(""), 1);
                }
            }
            var cData = [];
            if($scope.widget.conf.excursionType == "1") {
                for (var i = 0; i < compareFields.length; i++) {
                    cData[i] = getReportFCSeries(chartData, 27, compareFields[i], "line", linkDisabled, 0);
                }
                $scope.cOptions.caption = "Low excursions - % of assets with exposure (>= 1 hour)";

            } else {
                for (var i = 0; i < compareFields.length; i++) {
                    cData[i] = getReportFCSeries(chartData, 26, compareFields[i], "line", linkDisabled, 0);
                }
                $scope.cOptions.caption = "High excursions - % of assets with exposure (>= 1 hour)";
            }
            $scope.cOptions.subcaption = getReportCaption($scope.filter);
            if ($scope.filter.periodicity != "m" && cLabel.length > 10) {
                $scope.cOptions.rotateLabels = "1";
            } else {
                $scope.cOptions.rotateLabels = undefined;
            }
            $scope.cData = cData;
            $scope.cLabel = cLabel;
            $scope.chartData = chartData;
            if (checkNotNullEmpty($scope.cData)) {
                $scope.renderChart = true;
            }

            if (!localData) {
                $timeout(function () {
                    $scope.loading = false;
                }, 200);
            }
        }

        function getData() {
            $scope.wLoading = true;
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
                showError(msg, $scope);
            }).finally(function () {
                if (selectedFilters['level'] == "d") {
                    $scope.dLoading = false;
                } else {
                    $scope.loading = false;
                }
                $scope.wLoading = false;
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
            $scope.showError = false;
            setDefaultFilters();
            setFilters();
            setChartOptions();
            updateLabels();
            getData();
        }

        init();
    }]);

logistimoApp.requires.push('logistimo.storyboard.temperatureExcursion');
