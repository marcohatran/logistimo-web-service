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
 * Created by Mohan Raja on 23/02/17.
 */

registerWidget('itc', 'rpt-transaction-counts', 'Activity', 'Transaction counts','activity/transactioncounts');
(function () {
    'use strict';

    var reportType = 'itc';

    reportsPluginCore.directive('rptTransactionCounts', function () {
        return {
            restrict: 'E',
            templateUrl: 'plugins/reports/inventory/transactioncounts/transactioncounts.html'
        };
    });

    reportsPluginCore.controller('rptTransactionCountsController', ReportTransactionCountsController);

    ReportTransactionCountsController.$inject = ['$scope', '$timeout','reportsServiceCore'];

    function ReportTransactionCountsController($scope, $timeout, reportsServiceCore) {

        InventoryReportController.call(this, $scope, $timeout, getData, reportsServiceCore);

        $scope.reportType=reportType;

        $scope.cOptions.showSum = "1";
        $scope.cOptions.exportFileName = "TransactionCounts" + "_" + FormatDate_DD_MM_YYYY($scope.today);

        $scope.sCType = "pie2d";
        $scope.sum_co = {
            "exportEnabled": '1',
            "theme": "fint"
        };

        $scope.primaryMetric.push({name: $scope.resourceBundle['reports.user.activity.number.of.transactions'], value: "1"});

        function addExtraMetric() {
            if (typeof addSecondaryMetricOptions === "function") {
                addSecondaryMetricOptions($scope.primaryMetric, reportType, $scope.resourceBundle);
            }
        }
        addExtraMetric();

        $scope.secondaryMetric.push({name: $scope.resourceBundle['reports.all.transaction.types'], value: "0"});
        $scope.secondaryMetric.push({name: $scope.resourceBundle['issues'], value: "1"});
        $scope.secondaryMetric.push({name: $scope.resourceBundle['receipts'], value: "2"});
        $scope.secondaryMetric.push({name: $scope.resourceBundle['transactions.stockcounts.upper'], value: "3"});
        $scope.secondaryMetric.push({name: $scope.resourceBundle['transactions.wastage.upper'], value: "4"});
        $scope.secondaryMetric.push({name: $scope.resourceBundle['transfers'], value: "5"});
        $scope.secondaryMetric.push({name: $scope.resourceBundle['transactions.returns.incoming.upper'], value: "6"});
        $scope.secondaryMetric.push({name: $scope.resourceBundle['transactions.returns.outgoing.upper'], value: "7"});


        var seriesNames = [$scope.resourceBundle['issues'], $scope.resourceBundle['receipts'], $scope.resourceBundle['transactions.stockcounts.upper'], $scope.resourceBundle['transactions.wastage.upper'], $scope.resourceBundle['transfers'], $scope.resourceBundle['transactions.returns.incoming.upper'], $scope.resourceBundle['transactions.returns.outgoing.upper']];

        $scope.downloadAsCSV = function (daily) {
            if(daily) {
                var data = $scope.dtableData;
                var heading = $scope.dtableHeading;
            } else {
                data = $scope.tableData;
                heading = $scope.tableHeading;
            }
            var fileName = "Transaction_Counts" + formatDate2Url($scope.filter.from) +"_"+ formatDate2Url($scope.filter.to);
            $scope.exportAsCSV(data, heading, fileName);
        };

        $scope.$watch("metrics.primary",function(newValue,oldValue){
            if(newValue != oldValue) {
                if($scope.activeMetric == 'ot'){
                    setChartData();
                } else {
                    setTableMeta();
                }

            }
        });

        $scope.$watch("metrics.secondary",function(newValue,oldValue){
            if(newValue != oldValue) {
                if($scope.activeMetric == 'ot'){
                    setChartData();
                } else {
                    setTableMeta();
                }
            }
        });

        $scope.applyFilter();

        function setTableMeta() {
            $scope.tableSeriesNo = parseInt($scope.metrics.secondary) * 3 + parseInt($scope.metrics.primary);
            $scope.tableMetric = getHeading();
        }

        function getHeading() {
            return $scope.secondaryMetric[$scope.metrics.secondary].name + " - " + $scope.primaryMetric[$scope.metrics.primary - 1].name;
        }

        function getData() {
            var selectedFilters = $scope.populateFilters();
            selectedFilters['type'] = reportType;

            if(selectedFilters['level'] == "d") {
                $scope.dLoading = true;
            } else {
                $scope.loading = true;
                $scope.cData = $scope.cLabel = $scope.chartData = undefined;
            }

            if($scope.activeMetric == 'kt'){
                $scope.metrics.primary = "1";
                $scope.primaryMetric.splice(1,$scope.primaryMetric.length-1);
            }else if($scope.primaryMetric.length==1){
                addExtraMetric();
            }

            if($scope.activeMetric == 'ot') {
                $scope.showLoading();
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
                        setChartData(true, chartData, selectedFilters.level);
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
            } else {

                selectedFilters['viewtype'] = $scope.activeMetric;
                selectedFilters['s'] = $scope.size;
                selectedFilters['o'] = $scope.offset;
                $scope.noData = true;
                $scope.tableHideNext = false;
                $scope.showLoading();
                reportsServiceCore.getReportBreakdownData(angular.toJson(selectedFilters)).then(function (data) {
                    if (checkNotNullEmpty(data.data)) {
                        var breakdownData = angular.fromJson(data.data);

                        setTableMeta();
                        $scope.tableCaption = $scope.getReportCaption(true);

                        $scope.tableHeading = [];
                        $scope.tableHeading = angular.copy(breakdownData.headings);
                        $scope.updateTableHeading($scope.tableHeading,$scope.getReportDateFormat());
                        $scope.tableCSVHeading = breakdownData.headings;
                        $scope.updateTableHeading($scope.tableCSVHeading, "yyyy-MM-dd");
                        $scope.tableData = sortObject(breakdownData.table);
                        reportCoreFunction().formatReportTableData($scope.tableData);
                        $scope.tableDataLength = Object.keys($scope.tableData).length;
                        $scope.noData = false;
                    } else if ($scope.tableDataLength == $scope.size) {
                        $scope.tableHideNext = true;
                        $scope.noData = false;
                        $scope.offset -= $scope.size;
                    }
                }).catch(function error(msg) {
                }).finally(function(){
                    $scope.loading = false;
                    $scope.hideLoading();
                });
            }
        }

        function setChartData(localData, chartData, level) {
            if(!localData) {
                $scope.loading = true;
                chartData = angular.copy($scope.allChartData);
            } else if(level != "d") {
                $scope.sum_t_head = [$scope.resourceBundle['type'], $scope.resourceBundle['count']];
                $scope.sum_t = reportCoreFunction().getReportSummaryTable(chartData, seriesNames, 4, 3);
                $scope.sum_cd = getSummaryFCData($scope.sum_t);
                $scope.sum_cd_tot = getSummaryFCDataTotal($scope.sum_t);
            }

            var linkDisabled = level == "d" || $scope.filter.periodicity == "d";
            var cLabel = reportCoreFunction().getReportFCCategories(chartData,$scope.getReportDateFormat(level));
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
                if(level != "d" && localData) {
                    if($scope.metrics.secondary == "0") {
                        $scope.metrics.secondary = "1";
                    }
                    $scope.secondaryMetric[0].hideMetric = "true";
                }
                isCompare = true;
            } else if(level != "d" && localData) {
                //$scope.metrics.secondary = "0";
                $scope.secondaryMetric[0].hideMetric = undefined;
            }

            if(compareFields.length > 1 && compareFields.indexOf("") != -1) {
                compareFields.splice(compareFields.indexOf(""),1);
                filterSeriesIndex = 0;
            }
            var seriesNo = $scope.metrics.secondary * 3 + $scope.metrics.primary * 1;
            var cData = [];
            if($scope.metrics.primary == 1 && $scope.metrics.secondary == 0) {
                var ind = 0;
                $scope.cType = "stackedcolumn2d";
                for(var i = 4; i<= (seriesNames.length + 1) * 3; i+=3) {
                    cData[ind] = reportCoreFunction().getReportFCSeries(chartData, i, seriesNames[ind], "column2d", linkDisabled, filterSeriesIndex, "0");
                    ind++;
                }
            } else {
                $scope.cType = "mscombi2d";
                for (i = 0; i < compareFields.length; i++) {
                    cData[i] = reportCoreFunction().getReportFCSeries(chartData, seriesNo, compareFields[i], isCompare ? "line" : "column2d", linkDisabled, filterSeriesIndex, isCompare ? "0" : "1");
                }
            }
            $scope.cOptions.caption = getHeading();
            $scope.cOptions.subcaption = $scope.getReportCaption();
            $scope.cOptions.yAxisName = $scope.cOptions.caption;
            if($scope.filter.periodicity != "m" && cLabel.length > 10) {
                $scope.cOptions.rotateLabels = "1";
            } else {
                $scope.cOptions.rotateLabels = undefined;
            }
            if (level == "d") {
                $scope.dcData = cData;
                $scope.dcLabel = cLabel;
                $scope.dchartData = chartData;

                $scope.dtableCaption = undefined;
                $scope.dtableHeading = [];
                $scope.dtableHeading.push($scope.resourceBundle['date']);
                if(checkNotNullEmpty(compareFields[0])) {
                    angular.forEach(compareFields, function (d) {
                        $scope.dtableHeading.push(d);
                    });
                } else if($scope.metrics.primary == 1 && $scope.metrics.secondary == 0){
                    angular.forEach(seriesNames, function (d) {
                        $scope.dtableHeading.push(d);
                    });
                } else {
                    $scope.dtableHeading.push($scope.cOptions.caption);
                }
                $scope.dtableData = reportCoreFunction().getReportTableData(cData, cLabel);
                $scope.dtableDataLength = Object.keys($scope.dtableData).length;
                $scope.dcOptions = angular.copy($scope.cOptions);
                if(cLabel.length > 10) {
                    $scope.dcOptions.rotateLabels = "1";
                }
                $scope.dcOptions.rotateValues = "1";
                $scope.dcOptions.caption = $scope.getBreakDownCaption();
                $scope.dcOptions.subcaption = undefined;
                $scope.dtableMetric = $scope.dcOptions.caption;
            } else {
                $scope.cData = cData;
                $scope.cLabel = cLabel;
                $scope.chartData = chartData;
                $scope.dcData = $scope.dcLabel = $scope.dchartData = undefined;

                $scope.tableCaption = $scope.cOptions.subcaption;
                $scope.tableHeading = [];
                $scope.tableHeading.push($scope.resourceBundle['date']);
                if(checkNotNullEmpty(compareFields[0])) {
                    $scope.tableMetric = $scope.cOptions.caption;
                    angular.forEach(compareFields, function (d) {
                        $scope.tableHeading.push(d);
                    });
                } else if($scope.metrics.primary == 1 && $scope.metrics.secondary == 0){
                    angular.forEach(seriesNames, function (d) {
                        $scope.tableHeading.push(d);
                    });
                } else {
                    $scope.tableMetric = undefined;
                    $scope.tableHeading.push($scope.cOptions.caption);
                }
                $scope.tableData = reportCoreFunction().getReportTableData(cData, cLabel,$scope.getReportDateFormat());
                $scope.tableDataLength = Object.keys($scope.tableData).length;

            }

            if(!localData) {
                $timeout(function () {
                    $scope.loading = false;
                }, 200);
            }
        }
    }
})();

