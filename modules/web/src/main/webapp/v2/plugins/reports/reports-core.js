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
 * Created by Mohan Raja on 22/02/17.
 */

var reportsPluginCore = angular.module('reportsPluginCore', ['reportsServiceCore']);
var reportWidgets;
var assetWidgets = undefined;


function registerWidget(id, widget, report, subReport, helpFilePath) {
    if (checkNullEmpty(reportWidgets)) {
        reportWidgets = {};
    }
    if (checkNullEmpty(reportWidgets[report])) {
        reportWidgets[report] = [];
    }
    reportWidgets[report].push({id: id, subReport: subReport, widget: widget, helpFile: helpFilePath});
    reportWidgets = sortObject(reportWidgets);
    for (var w in reportWidgets) {
        reportWidgets[w] = sortByKey(reportWidgets[w], 'subReport');
    }
}

function getReportWidgetById(id) {
    for (let widget in reportWidgets) {
        for (let report of reportWidgets[widget]) {
            if (report.id == id) {
                return report;
            }
        }
    }
}

(function () {
    reportCoreService();
    var DEFAULT_COMPARE_LIMIT = 3;

    reportsPluginCore.controller('rptCoreController', ReportCoreController);

    ReportCoreController.$inject = ['$scope', '$timeout', 'domainCfgService'];

    function ReportCoreController($scope, $timeout, domainCfgService) {

        var configLoadCount = 0;
        $scope.loadingConfig = false;
        var userLocale = 'en';
        if (checkNotNullEmpty($scope.i18n)) {
            userLocale = $scope.i18n.language.locale;
        }
        function showLoadConfig(){
            configLoadCount++;
            $scope.loadingConfig = true;
        }
        function hideLoadConfig(){
            configLoadCount--;
            if(configLoadCount == 0) {
                $scope.loadingConfig = false;
            }
        }
        if (!assetWidgets) {
            assetWidgets = angular.copy(reportWidgets['Assets']);
        }
        $scope.getReportSubMenuLabel = function(reportId) {
            return $scope.resourceBundle['reports.submenu.' + reportId];
        };
        $scope.getReportMenuLabel = function(key) {
            return $scope.resourceBundle['reports.menu.' + key.replace(/ /g, ".").toLowerCase()];
        };
        showLoadConfig();
        domainCfgService.getAssetCfg().then(function (data) {
            switch (data.data.enable) {
                case 0:
                    delete reportWidgets['Assets'];
                    break;
                case 1:
                    for (var w in reportWidgets) {
                        switch (w) {
                            case 'Assets':
                                break;
                            case 'Activity':
                                // Remove all reports other than Domain activity
                                for (var i = reportWidgets[w].length - 1; i >= 0; i--) {
                                    if (reportWidgets[w][i].subReport !== 'Domain activity') {
                                        reportWidgets[w].splice(i, 1);
                                    }
                                }
                                break;
                            default:
                                delete reportWidgets[w];
                                break;
                        }
                    }
                    break;
                default:
                    if (checkNullEmpty(reportWidgets['Assets'])) {
                        reportWidgets['Assets'] = angular.copy(assetWidgets);
                        reportWidgets = sortObject(reportWidgets);
                    }
                    break;
            }
        }).finally(function () {
            hideLoadConfig();
        });

        showLoadConfig();
        domainCfgService.getApprovalsEnabledConfig().then(function (data) {
            if (checkNotNullEmpty(data.data)) {
                var transferApprovalsEnabled = data.data.itae;
                var purchaseApprovalsEnabled = data.data.ipae;
                var salesApprovalsEnabled = data.data.isae;
                if (!transferApprovalsEnabled && !purchaseApprovalsEnabled && !salesApprovalsEnabled) {
                    for (var w in reportWidgets) {
                        if (w == 'Order') {
                            for (var i = reportWidgets[w].length - 1; i >= 0; i--) {
                                if (reportWidgets[w][i].id === 'oart' || reportWidgets[w][i].id === 'oars') {
                                    reportWidgets[w].splice(i, 1);
                                }
                            }
                        }
                    }
                }

            }
        }).catch(function error(msg) {
            $scope.showErrorMsg(msg);
        }).finally(function () {
            hideLoadConfig();
        });
        showLoadConfig();
        domainCfgService.getDomainConfigMenuStats().then(function(data) {
            if (checkNotNullEmpty(data.data) && (!data.data.iOrdTbEn)) {
                delete reportWidgets['Order'];
            }
        }).finally(function () {
            hideLoadConfig();
        });
        $scope.$on("$routeChangeSuccess", function (event, current, previous) {
            if (current.params.rptid != previous.params.rptid) {
                $scope.tempReport = $scope.report;
                $scope.report = undefined;
                $timeout(function () {
                    $scope.rptid = current.params.rptid;
                    reloadWidget();
                    $scope.toggleMenu();
                }, 100);
            } else if (checkNotNullEmpty($scope.rptid)) {
                $scope.hideMenu = true;
            }
        });

        function reloadWidget() {
            $scope.widget = '';
            $scope.helpURL = undefined;
            if ($scope.rptid == 'cr') {
                $scope.heading = $scope.resourceBundle['report.customreports'];
                $scope.report = $scope.resourceBundle['report.customreports'];
            } else if (checkNullEmpty($scope.rptid)) {
                $scope.hideMenu = false;
            } else {
                for (var w in reportWidgets) {
                    var found = reportWidgets[w].some(function (data) {
                        if (data.id == $scope.rptid) {
                            $scope.widget = data.widget;
                            $scope.heading = $scope.getReportSubMenuLabel(data.id);
                            if (checkNotNullEmpty(data.helpFile)) {
                                $scope.helpURL = "/v2/help/report/" + userLocale + "/" + data.helpFile + ".html";
                            }
                            $scope.report = $scope.getReportMenuLabel(w);
                            return true;
                        }
                    });
                    if (found) {
                        break;
                    }
                }
            }
        }

        reloadWidget();

        function getReportDataAsCSV(data, heading) {
            var comma = ",";
            var nl = "\r\n";
            var csvData = heading.join(comma);
            csvData += nl;
            for (var i = data.length - 1; i >= 0; i--) {
                csvData += data[i][1].label;
                for (var j = 1; j < heading.length; j++) {
                    csvData += comma + data[i][j].value || 0;
                }
                csvData += nl;
            }
            return csvData;
        }

        function getReportTableDataAsCSV(data, heading, tableSeriesNo) {
            var comma = ",";
            var nl = "\r\n";
            var csvHeading = angular.copy(heading);
            var isKiosk = false;
            var isAsset = false;
            if (csvHeading[0] == $scope.resourceBundle['kiosk']) {
                csvHeading.splice(1, 0, $scope.resourceBundle['state']);
                csvHeading.splice(1, 0, $scope.resourceBundle['district']);
                csvHeading.splice(1, 0, $scope.resourceBundle['taluk']);
                csvHeading.splice(1, 0, $scope.resourceBundle['city']);
                isKiosk = true;
            } else if (csvHeading[0] == $scope.resourceBundle['asset.serial.number']) {
                isAsset = true;
            }
            if (isAsset) {
                var csvData = getAssetColumns(csvHeading);
            } else {
                csvData = csvHeading.join(comma);
            }
            csvData += nl;
            for (var key in data) {
                var location = key.split("|");
                if (isKiosk) {
                    csvData += location[0];
                    csvData += comma + location[2];
                    csvData += comma + location[3];
                    csvData += comma + location[4];
                    csvData += comma + location[5];
                } else if (isAsset) {
                    var assetMetaData = key.split("|||");
                    var vid_sid = assetMetaData[0].split('_');
                    csvData += vid_sid.splice(1).join() + comma;
                    csvData += assetMetaData[2] + comma + assetMetaData[1] + comma + assetMetaData[4];
                    csvData += comma + cleanupLocationField(assetMetaData[5]);
                    csvData += comma + cleanupLocationField(assetMetaData[6]);
                    csvData += comma + cleanupLocationField(assetMetaData[7]);
                    csvData += comma + cleanupLocationField(assetMetaData[8]);
                    csvData += comma + cleanupLocationField(assetMetaData[9]);
                } else {
                    csvData += location[0];
                }
                for (var j = 0; j < heading.length - 1; j++) {
                    csvData += comma + data[key][j][tableSeriesNo].value || 0;
                }
                csvData += nl;
            }
            return csvData;
        }

        function cleanupLocationField(location) {
            var value = ((checkNullEmpty(location) || location.trim() == ',') ? '' : location);
            if (checkNotNullEmpty(value) && value.charAt(0) == ',') {
                value = value.substring(1);
            }
            return "\"" + value.trim() + "\"";
        }

        function getAssetColumns(csvHeading) {
            return csvHeading[0] + ',' + $scope.resourceBundle['assetModel'] + ','
                + $scope.resourceBundle['manufacturer'] + ',' + $scope.resourceBundle['kiosk']
                + ',' + $scope.resourceBundle['city'] + ',' + $scope.resourceBundle['taluk'] + ','
                + $scope.resourceBundle['district'] + ',' + $scope.resourceBundle['state'] + ','
                + $scope.resourceBundle['country'] + ',' + csvHeading.splice(1).join(',');
        }

        $scope.exportAsCSV = function (data, headings, fileName, tableSeriesNo) {
            var csvData;
            if (tableSeriesNo !== undefined) {
                csvData = getReportTableDataAsCSV(data, headings, tableSeriesNo);
            } else {
                csvData = getReportDataAsCSV(data, headings);
            }
            exportCSV(csvData, fileName, $timeout);
        };

        $scope.toggleMenu = function () {
            if ($scope.report) {
                $scope.hideMenu = !$scope.hideMenu;
            }
        };
        $scope.toggleMenu();

        $scope.getReportMenu = function (column) {
            var data = {};
            var index = 1;
            for (var w in reportWidgets) {
                if (index++ == column) {
                    data[w] = reportWidgets[w];
                    column += 4;
                }
            }
            return data;
        };
    }

    reportsPluginCore.directive('rptLocationFilter', locationFilter);
    reportsPluginCore.directive('rptMaterialFilter', materialFilter);
    reportsPluginCore.directive('rptEntityFilter', entityFilter);
    reportsPluginCore.directive('rptUserFilter', userFilter);
    reportsPluginCore.directive('rptEntityRelationFilter', entityRelationFilter);
    reportsPluginCore.directive('rptTagFilter', tagFilter);
    reportsPluginCore.directive('rptDateFilter', dateFilter);
    reportsPluginCore.directive('rptPeriodicityFilter', periodicityFilter);
    reportsPluginCore.directive('rptAssetFilter', assetFilter);
    reportsPluginCore.directive('rptMonitorTypeFilter', monitorTypeFilter);
    reportsPluginCore.directive('rptManufacturerFilter', manufacturerFilter);
    reportsPluginCore.directive('rptAgeFilter', ageFilter);
    reportsPluginCore.directive('rptModelFilter', modelFilter);
    reportsPluginCore.directive('rptWidgetBase', widgetBase);
    reportsPluginCore.directive('rptLastRunTime', lastRunTime);
    reportsPluginCore.directive('rptOrderTypeFilter', orderTypeFilter);
    reportsPluginCore.directive('rptExportData', exportData);

    LocationFilterController.$inject = ['$scope', 'entityService'];
    MaterialFilterController.$inject = ['$scope', '$q', 'matService'];
    EntityFilterController.$inject = ['$scope', '$q', 'entityService'];
    EntityRelationFilterController.$inject = ['$scope', '$q', 'entityService'];
    TagFilterController.$inject = ['$scope', 'domainCfgService'];
    DateFilterController.$inject = ['$scope', '$timeout'];
    PeriodicityFilterController.$inject = ['$scope'];
    AssetFilterController.$inject = ['$scope', 'domainCfgService'];
    MonitorTypeFilterController.$inject = ['$scope'];
    ManufacturerFilterController.$inject = ['$scope', 'domainCfgService'];
    ModelFilterController.$inject = ['$scope', '$q', 'assetService'];
    AggregationLastRunTimeController.$inject = ['$scope', 'reportsServiceCore'];
    OrderTypeFilterController.$inject = ['$scope'];
    ExportController.$inject = ['$scope','$uibModal','AnalyticsService'];

    function ageFilter() {
        function getYears(offset) {
            var currentYear = new Date().getFullYear();
            var years = [];
            var i = 0;
            while ((currentYear + offset - i) >= 1980) {
                years.push(currentYear + offset - i);
                i++;
            }
            return years;
        }

        return {
            restrict: 'E',
            scope: {
                filterModel: '=',
                filterType: '@',
                preSelected: '=',
                ngDisabled: '='
            },
            link: function (scope, element, attrs) {
                scope.years = getYears(+attrs.offset);
                scope.selected = scope.years[0];
            },
            templateUrl: function () {
                return 'plugins/reports/filters/asset-age.html';
            }
        };
    }

    function lastRunTime() {
        return {
            restrict: 'E',
            scope: {
                reportType: '='
            },
            controller: AggregationLastRunTimeController,
            template: '<div ng-if="lastRuntime" class="form-label gray-text">{{asofLabel}} {{lastRuntime}}</div>'
        };
    }

    function AggregationLastRunTimeController($scope, reportCoreService) {
        $scope.asofLabel = $scope.$parent.resourceBundle.asof;
        reportCoreService.getAggregatedTime($scope.reportType).then(function (data) {
            $scope.lastRuntime = data.data;
        }).catch(function error() {
            $scope.lastRuntime = undefined;
        });
    }

    function exportData() {
        return {
            restrict: 'E',
            scope: {
                reportType: '=',
                callback: '&'
            },
            controller: ExportController,
            template: '<div><button class="btn btn-primary pull-right" ng-click="confirmExport()">{{$parent.resourceBundle.export}}</button></div>'
        };
    }

    function ExportController($scope,$uibModal,AnalyticsService) {
        $scope.mailId = $scope.$parent.$parent.$parent.$parent.mailId;
        $scope.doExport = function() {
            $scope.close();
            AnalyticsService.logEventAnalytics('Export',getReportWidgetById($scope.reportType).subReport + " Report");
            $scope.callback({reportType: $scope.reportType});
        };
        $scope.close = function () {
            $scope.modalInstance.dismiss('cancel');
        };
        const FILTER_TEMPLATE = $scope.$parent.resourceBundle['report.export.filter.template'];

        var exportModal = '<div class="modal-header ws">' +
            '<h3 class="modal-title">' + $scope.$parent.resourceBundle['export.data'] + '</h3>' +
            '</div>' +
            '<div class="modal-body ws">' +
            '<p>{{message}}</p>' +
            '<p ng-if="exportFilters" class="litetext word-wrap" style="white-space: pre-wrap"><b>' + $scope.$parent.resourceBundle['filters.uppercase'] + ':</b> {{exportFilters}}</p>' +
            '</div>' +
            '<div class="modal-footer ws">' +
            '<button class="btn btn-primary" ng-click="startExport()">' + $scope.$parent.resourceBundle['ok'] + '</button>' +
            '<button class="btn btn-default" ng-click="close()">' + $scope.$parent.resourceBundle['cancel'] + '</button>' +
            '</div>';

        function messageFormat(text) {
            var args = arguments;
            return text.replace(/\{(\d+)\}/g, function () {
                return args[arguments[1] * 1 + 1];
            });
        }

        $scope.confirmExport = function () {
            var info = $scope.callback({reportType: $scope.reportType, isInfo: true});
            $scope.exportFilters = info.filters ? info.filters : undefined;
            $scope.message = messageFormat(FILTER_TEMPLATE, info.type, $scope.mailId);

            $scope.modalInstance = $uibModal.open({
                template: exportModal,
                scope: $scope,
                controller: ['$scope', function ($scope) {
                    $scope.startExport = function(){
                        $scope.doExport();
                    }
                }]
            });
        };
    }

    function locationFilter() {
        return {
            restrict: 'E',
            scope: {
                filterModel: '=',
                placeHolder: '@',
                multiple: '@',
                limit: '@',
                filterType: '@',
                preSelected: '=',
                ngDisabled: '='
            },
            controller: LocationFilterController,
            bindToController: true,
            controllerAs: 'lf',
            templateUrl: function (elem, attr) {
                if (attr.multiple) {
                    return 'plugins/reports/filters/multiple-location-filter.html';
                } else {
                    return 'plugins/reports/filters/location-filter.html';
                }
            }
        };
    }

    // scope added just to call watch.
    function LocationFilterController($scope, entityService) {
        var lf = this;
        lf.limit = lf.limit || DEFAULT_COMPARE_LIMIT;
        lf.model = {selectModel: lf.filterModel};
        if (lf.multiple) {
            $scope.$watch('lf.filterModel', function (newValue, oldValue) {
                if (newValue != oldValue) {
                    lf.model = {selectModel: newValue};
                }
            });
            $scope.$watch('lf.model.selectModel', function (newValue) {
                lf.filterModel = newValue;
            });
        }

        lf.getData = function (text) {
            lf.loadingData = true;
            return entityService.getLocationSuggestion(text, lf.filterType).then(function (data) {
                var d = [];
                angular.forEach(data.data, function (l) {
                    if (checkNullEmpty(lf.preSelected) || l.label != lf.preSelected.label) {
                        d.push(l);
                    }
                });
                lf.filteredData = d;
                lf.loadingData = false;
                return d;
            });
        };
    }

    function userFilter() {
        return {
            restrict: 'E',
            scope: {
                filterModel: '=',
                placeHolder: '@',
                limit: '@',
                preSelected: '=',
                ngDisabled: '='
            },
            controller: UserFilterController,
            templateUrl: function (elem, attr) {
                return 'plugins/reports/filters/user-filter.html';
            }
        }
    }

    function UserFilterController($scope, $q, userService) {


        $scope.query = function (term) {
            $scope.loadingData = true;
            var deferred = $q.defer();
            userService.getDomainUsers(term, $scope.offset, $scope.size, undefined, false, true).then(function (data) {
                for (var i in data.data.results) {
                    data.data.results[i].text = data.data.results[i].fnm + ' [' + data.data.results[i].id + ']';
                }
                deferred.resolve(data.data.results);
            }).catch(function error(msg) {
                deferred.reject(msg);
            }).finally(function () {
                $scope.loadingData = false;
            });
            return deferred.promise;
        };

    }

    function materialFilter() {
        return {
            restrict: 'E',
            scope: {
                filterModel: '=',
                placeHolder: '@',
                multiple: '@',
                limit: '@',
                preSelected: '=',
                ngDisabled: '='
            },
            controller: MaterialFilterController,
            templateUrl: function (elem, attr) {
                if (attr.multiple) {
                    return 'plugins/reports/filters/multiple-material-filter.html';
                } else {
                    return 'plugins/reports/filters/material-filter.html';
                }
            }
        }
    }

    function MaterialFilterController($scope, $q, matService) {
        $scope.limit = $scope.limit || DEFAULT_COMPARE_LIMIT;
        $scope.model = {selectModel: $scope.filterModel};
        if ($scope.multiple) {
            $scope.$watch('filterModel', function (newValue, oldValue) {
                if (newValue != oldValue) {
                    $scope.model = {selectModel: newValue};
                }
            });
            $scope.$watch('model.selectModel', function (newValue) {
                $scope.filterModel = newValue;
            });
        }
        $scope.query = function (term) {
            $scope.loadingData = true;
            var deferred = $q.defer();
            matService.getDomainMaterials(term, null, 0, 10).then(function (data) {
                deferred.resolve(data.data.results);
            }).catch(function error(msg) {
                deferred.reject(msg);
            }).finally(function () {
                $scope.loadingData = false;
            });
            return deferred.promise;
        };

        function isSelected(i) {
            if ($scope.preSelected.mId == i.mId) {
                return true;
            }
            for (var m in $scope.filterModel) {
                if (i == $scope.filterModel[m]) {
                    return true;
                }
            }
            return false;
        }

        $scope.queryMultiple = function (term) {
            matService.getDomainMaterials(term, null, 0, 10).then(function (data) {
                $scope.filteredData = [];
                for (var i in data.data.results) {
                    var material = data.data.results[i];
                    if (!isSelected(material)) {
                        $scope.filteredData.push(material);
                    }
                }
            }).catch(function error(msg) {
            });
        }
    }


    function entityFilter() {
        return {
            restrict: 'E',
            scope: {
                filterModel: '=',
                placeHolder: '@',
                multiple: '@',
                limit: '@',
                preSelected: '=',
                ngDisabled: '='
            },
            controller: EntityFilterController,
            templateUrl: function (elem, attr) {
                if (attr.multiple) {
                    return 'plugins/reports/filters/multiple-entity-filter.html';
                } else {
                    return 'plugins/reports/filters/entity-filter.html';
                }
            }
        }
    }

    function EntityFilterController($scope, $q, entityService) {
        $scope.limit = $scope.limit || DEFAULT_COMPARE_LIMIT;
        $scope.model = {selectModel: $scope.filterModel};
        if ($scope.multiple) {
            $scope.$watch('filterModel', function (newValue, oldValue) {
                if (newValue != oldValue) {
                    $scope.model = {selectModel: newValue};
                }
            });
            $scope.$watch('model.selectModel', function (newValue) {
                $scope.filterModel = newValue;
            });
        }

        $scope.query = function (term) {
            $scope.loadingData = true;
            var deferred = $q.defer();
            entityService.getFilteredEntity(term.toLowerCase()).then(function (data) {
                deferred.resolve(data.data.results);
            }).catch(function error(msg) {
                deferred.reject(msg);
            }).finally(function () {
                $scope.loadingData = false;
            });
            return deferred.promise;
        };

        function isSelected(i) {
            if ($scope.preSelected.id == i.id) {
                return true;
            }
            for (var m in $scope.filterModel) {
                if (i == $scope.filterModel[m]) {
                    return true;
                }
            }
            return false;
        }

        $scope.queryMultiple = function (term) {
            entityService.getFilteredEntity(term.toLowerCase()).then(function (data) {
                $scope.filteredData = [];
                for (var i in data.data.results) {
                    var entity = data.data.results[i];
                    if (!isSelected(entity)) {
                        $scope.filteredData.push(entity);
                    }
                }
            }).catch(function error(msg) {
            });
        }
    }

    function entityRelationFilter() {
        return {
            restrict: 'E',
            scope: {
                filterModel: '=',
                filterType: '=',
                placeHolder: '@',
                linkedId: '='
            },
            controller: EntityRelationFilterController,
            templateUrl: function () {
                return 'plugins/reports/filters/entity-relation-filter.html';
            }
        }
    }

    function EntityRelationFilterController($scope, $q, entityService) {
        $scope.model = {selectModel: $scope.filterModel};

        $scope.entities = [];
        if ($scope.filterType == 'customer') {
            entityService.getCustomers($scope.linkedId.id).then(function (data) {
                $scope.entities = data.data.results || [];
            });
        } else if ($scope.filterType == 'vendor') {
            entityService.getVendors($scope.linkedId.id).then(function (data) {
                $scope.entities = data.data.results || [];
            });
        }

        $scope.$watch('linkedId', function (newValue, oldValue) {
            if (!angular.equals(newValue, oldValue)) {
                $scope.model = {selectModel: undefined};
                $scope.filterModel = undefined;
            }
        });
    }

    function tagFilter() {
        return {
            restrict: 'E',
            scope: {
                filterType: '@',
                filterModel: '=',
                placeHolder: '@',
                multiple: '@',
                limit: '@',
                preSelected: '=',
                ngDisabled: '='
            },
            controller: TagFilterController,
            templateUrl: 'plugins/reports/filters/tag-filter.html'
        }
    }

    function TagFilterController($scope, domainCfgService) {
        $scope.limit = $scope.limit || DEFAULT_COMPARE_LIMIT;
        $scope.tags = {};
        $scope.loadingData = true;
        if ($scope.filterType === "entity") {
            domainCfgService.getEntityTagsCfg().then(function (data) {
                $scope.tags = data.data.tags;
                $scope.filter('');
                $scope.loadingData = false;
            });
        } else if ($scope.filterType === "material") {
            domainCfgService.getMaterialTagsCfg().then(function (data) {
                $scope.tags = data.data.tags;
                $scope.filter('');
                $scope.loadingData = false;
            });
        } else if ($scope.filterType === "user") {
            domainCfgService.getUserTagsCfg().then(function (data) {
                $scope.tags = data.data.tags;
                $scope.filter('');
                $scope.loadingData = false;
            });
        } else if ($scope.filterType === "order") {
            domainCfgService.getOrderTagsCfg().then(function (data) {
                $scope.tags = data.data.tags;
                $scope.filter('');
                $scope.loadingData = false;
            });
        }

        $scope.model = {selectModel: $scope.filterModel};

        $scope.filteredData = [];

        $scope.filter = function (query) {
            var data = {results: []};
            var term = query.toLowerCase();
            for (var i in $scope.tags) {
                var tag = $scope.tags[i].toLowerCase();
                if (tag.indexOf(term) >= 0 && (checkNullEmpty($scope.preSelected) || $scope.preSelected.id != $scope.tags[i])) {
                    data.results.push({'text': $scope.tags[i], 'id': $scope.tags[i]});
                }
            }
            $scope.filteredData = data.results;
        };

        $scope.$watch('filterModel', function (newValue, oldValue) {
            if (newValue != oldValue) {
                $scope.model = {selectModel: newValue};
            }
        });
        $scope.$watch('model.selectModel', function (newValue) {
            $scope.filterModel = newValue;
        });
    }


    function dateFilter() {
        return {
            restrict: 'E',
            scope: {
                filterModel: '=',
                placeHolder: '@',
                minDate: '=',
                maxDate: '=',
                mode: '=',
                opened: '=',
                closeModel: '='
            },
            controller: DateFilterController,
            templateUrl: 'plugins/reports/filters/date-filter.html'
        }
    }

    function DateFilterController($scope) {
        $scope.noclear = 'noclear';
        $scope.opened = false;
        function init() {
            $scope.format = $scope.mode == 'month' ? "MMM yyyy" : "dd MMM yyyy";
            $scope.dateOptions = $scope.mode == 'month' ? {minMode: 'month'} : {};
            $scope.dateOptions.minDate = $scope.minDate;
            $scope.dateOptions.maxDate = $scope.maxDate;
            if ($scope.mode == 'week') {
                $scope.dateOptions.showWeeks = true;
                $scope.dateOptions.startingDay = 1;
                $scope.dateOptions.dateDisabled = $scope.disabled;
            }
        }

        init();
        $scope.disabled = function (model) {
            return ($scope.mode == 'week' && model.date.getDay() != 1);
        };

        $scope.open = function ($event) {
            $event.preventDefault();
            $event.stopPropagation();
            $scope.opened = true;
            $scope.closeModel = false;
            init();
        };

        $scope.$watch("mode", function (newValue, oldValue) {
            if (newValue != oldValue) {
                init();
            }
        });
        $scope.$watch("minDate", function (newValue, oldValue) {
            if (newValue != oldValue) {
                init();
            }
        });
        $scope.$watch("maxDate", function (newValue, oldValue) {
            if (newValue != oldValue) {
                init();
            }
        });
        $scope.IE = false;
        var ua = window.navigator.userAgent;
        if (ua.indexOf("MSIE ") > 0 || ua.indexOf('Trident/') > 0 || ua.indexOf('Edge/') > 0 || navigator.appVersion.indexOf("MSIE 9.0") !== -1) {
            $scope.IE = true;
        }
        if ($scope.IE) {
            $scope.prevDate = $scope.filterModel;
            $scope.validate = function () {
                if (checkNotNullEmpty($scope.filterModel)) {
                    var selDate = $scope.filterModel.getTime();
                    if (checkNotNullEmpty($scope.minDate)) {
                        var mnDate = constructDate($scope.minDate).getTime();
                        if (selDate < mnDate) {
                            $scope.filterModel = $scope.prevDate;
                            alert("Date can't be less than " + $scope.minDate);
                        }
                    }
                    if (checkNotNullEmpty($scope.maxDate)) {
                        var mxDate = constructDate($scope.maxDate).getTime();
                        if (selDate > mxDate) {
                            $scope.filterModel = $scope.prevDate;
                            alert("Date can't be greater than " + $scope.maxDate);
                        }
                    }
                    $scope.prevDate = $scope.filterModel;
                }
            }
        }
    }

    function periodicityFilter() {
        return {
            restrict: 'E',
            scope: {
                filterModel: '=',
                fromModel: '=',
                toModel: '='
            },
            controller: PeriodicityFilterController,
            templateUrl: 'plugins/reports/filters/periodicity-filter.html'
        }
    }

    function PeriodicityFilterController($scope) {
        $scope.filterModel = 'm';
        $scope.closeDates = function () {
            $scope.fromModel = false;
            $scope.toModel = false;
        }
    }

    function assetFilter() {
        return {
            restrict: 'E',
            scope: {
                filterType: '@',
                filterModel: '=',
                placeHolder: '@'
            },
            controller: AssetFilterController,
            templateUrl: 'plugins/reports/filters/asset-filter.html'
        }
    }

    function AssetFilterController($scope, domainCfgService) {

        $scope.assets = {};
        $scope.assets['all'] = [];
        $scope.loadingData = true;
        var count = 0;
        //$scope.showLoading();
        domainCfgService.getAssetSysCfg('1').then(function (data) {
            $scope.assets['1'] = data.data;
            addAllAsset(data.data);
        }).catch(function error(msg) {
        }).finally(function () {
            //$scope.hideLoading();
        });

        //$scope.showLoading();
        domainCfgService.getAssetSysCfg('2').then(function (data) {
            $scope.assets['2'] = data.data;
            addAllAsset(data.data);
        }).catch(function error(msg) {
        }).finally(function () {
            //$scope.hideLoading();
        });

        function addAllAsset(data) {
            for (var key in data) {
                $scope.assets['all'][key] = data[key];
            }
            if (++count == 2) {
                $scope.loadingData = false;
            }
        }

        $scope.model = {selectModel: $scope.filterModel};

        $scope.filteredData = [];

        $scope.filter = function (query) {
            var data = {results: []};
            var term = query.toLowerCase();
            for (var key in $scope.assets[$scope.filterType]) {
                if ($scope.assets[$scope.filterType][key].toLowerCase().indexOf(term) >= 0) {
                    data.results.push({'text': $scope.assets[$scope.filterType][key], 'id': key});
                }
            }
            $scope.filteredData = data.results;
        };

        $scope.$watch('filterModel', function (newValue, oldValue) {
            if (newValue != oldValue) {
                $scope.model = {selectModel: newValue};
            }
        });
        $scope.$watch('filterType', function (newValue, oldValue) {
            if (newValue != oldValue) {
                $scope.filter('');
            }
        });
        $scope.$watch('model.selectModel', function (newValue) {
            $scope.filterModel = newValue;
        });
    }

    function monitorTypeFilter() {
        return {
            restrict: 'E',
            scope: {
                filterModel: '=',
                ngDisabled: '='
            },
            controller: MonitorTypeFilterController,
            templateUrl: 'plugins/reports/filters/monitor-type-filter.html'
        }
    }

    function MonitorTypeFilterController($scope) {
    }

    function manufacturerFilter() {
        return {
            restrict: 'E',
            scope: {
                filterType: '=',
                filterModel: '=',
                placeHolder: '@',
                ngDisabled: '='
            },
            controller: ManufacturerFilterController,
            templateUrl: 'plugins/reports/filters/manufacturer-filter.html'
        }
    }

    function ManufacturerFilterController($scope, domainCfgService) {

        var count = 0;
        $scope.mf = {};
        $scope.mf['all'] = [];
        $scope.fType = 'all';
        $scope.loadingData = true;
        //$scope.showLoading();
        domainCfgService.getAssetManufacturerSysCfg('1').then(function (data) {
            $scope.mf['1'] = data.data;
            count++;
            addAllMf(data.data);
        }).catch(function error(msg) {
        }).finally(function () {
            //$scope.hideLoading();
        });

        //$scope.showLoading();
        domainCfgService.getAssetManufacturerSysCfg('2').then(function (data) {
            $scope.mf['2'] = data.data;
            count++;
            addAllMf(data.data);
        }).catch(function error(msg) {
        }).finally(function () {
            //$scope.hideLoading();
        });

        function addAllMf(data) {
            for (var k in data) {
                $scope.mf['all'][k] = data[k];
            }
            if (count == 2) {
                $scope.filter('');
                $scope.loadingData = false;
            }
        }

        $scope.model = {selectModel: $scope.filterModel};

        $scope.filteredData = [];

        $scope.filter = function (query) {
            var data = {results: []};
            var term = query.toLowerCase();
            for (var key in $scope.mf[$scope.fType]) {
                if ($scope.mf[$scope.fType][key].toLowerCase().indexOf(term) >= 0) {
                    data.results.push({'text': $scope.mf[$scope.fType][key], 'id': key});
                }
            }
            $scope.filteredData = data.results;
        };

        $scope.$watch('filterModel', function (newValue, oldValue) {
            if (newValue != oldValue) {
                $scope.model = {selectModel: newValue};
            }
        });
        $scope.$watch('filterType', function () {
            $scope.fType = $scope.filterType || 'all';
            $scope.filter('');
        });
        $scope.$watch('model.selectModel', function (newValue) {
            $scope.filterModel = newValue;
        });
    }

    function modelFilter() {
        return {
            restrict: 'E',
            scope: {
                filterModel: '=',
                placeHolder: '@'
            },
            controller: ModelFilterController,
            templateUrl: 'plugins/reports/filters/model-filter.html'
        }
    }

    function ModelFilterController($scope, $q, assetService) {
        $scope.model = {selectModel: $scope.filterModel};

        $scope.query = function (term) {
            var deferred = $q.defer();
            assetService.getModelSuggestions(term).then(function (data) {
                deferred.resolve(data.data);
            }).catch(function error(msg) {
                deferred.reject(msg);
            });
            return deferred.promise;
        };
    }

    function orderTypeFilter() {
        return {
            restrict: 'E',
            scope: {
                filterModel: '=',
                ngDisabled: '=',
                hideTransfer: '=',
            },
            controller: OrderTypeFilterController,
            templateUrl: 'plugins/reports/filters/order-type-filter.html'
        }
    }

    function OrderTypeFilterController($scope) {
        $scope.model = {selectModel: $scope.filterModel};
        $scope.orderTypes = {};
        $scope.orderTypes["1"] = 'ordertype.purchase';
        $scope.orderTypes["2"] = 'order.sales';
        $scope.orderTypes["0"] = 'ordertype.transfer';

        if ($scope.hideTransfer) {
            delete $scope.orderTypes["0"];
        }
        $scope.$watch('filterModel', function (newValue, oldValue) {
            if (newValue != oldValue) {
                $scope.model = {selectModel: newValue};
            }
        });
        $scope.$watch('model.selectModel', function (newValue) {
            $scope.filterModel = newValue;
        });
    }

    function widgetBase($compile) {
        return {
            restrict: 'E',
            scope: {
                widgetName: '='
            },
            link: function (scope, element) {
                element.html('<' + scope.widgetName + '/>').show();
                $compile(element.contents())(scope);
            }
        }
    }
})();


function reportCoreService() {
    var reportCoreService = angular.module('reportsServiceCore', []);
    reportCoreService.factory('reportsServiceCore', ['APIService', function (apiService) {
        return {
            getReportData: function (json) {
                return apiService.get('/s2/api/plugins/report/?json=' + encodeURIComponent(json));
            },
            getReportBreakdownData: function (json) {
                return apiService.get('/s2/api/plugins/report/breakdown?json=' + encodeURIComponent(json));
            },
            getAggregatedTime: function (reportType) {
                return apiService.get('/s2/api/plugins/report/last-run-time?reportType=' + reportType);
            },
            exportData: function (json) {
                return apiService.post(json, '/s2/api/plugins/report/export');
            },
            getMinMaxHistoryData: function (json) {
                return apiService.get('/s2/api/plugins/report/min-max-history?json=' + encodeURIComponent(json));
            },
        }
    }]);
}

function reportCoreFunction() {
    return {
        getReportFCSeries: function (data, seriesno, name, type, isLinkDisabled, filterSeriesIndex, showvalue, color, noAnchor, zeroWithEmpty, forceSum, skipSeriesInLabel, isSecValue, isTotalValue, dontNegateSecValue) {
            if (checkNotNullEmpty(data) && data[0]) {
                if (data[0].value.length > seriesno) {
                    var series = {};
                    series.seriesName = name;
                    series.renderAs = type;
                    series.showValues = showvalue ? showvalue : "1";
                    series.drawAnchors = noAnchor ? "0" : "1";
                    series.data = [];
                    var ind = 0;
                    var prevLabel = undefined;
                    var curLabel;
                    var found = false;
                    for (var i = data.length - 1; i >= 0; i--) {
                        var lData = data[i];
                        if (isSecValue) {
                            lData = angular.copy(lData);
                            angular.forEach(lData.value, function (v) {
                                if(checkNotNullEmpty(v.secValue)) {
                                    v.value = -1 * v.secValue * (dontNegateSecValue?-1:1) + "";
                                }
                                if (checkNotNullEmpty(v.secNum) * (dontNegateSecValue?-1:1)) {
                                    v.num = -1 * v.secNum;
                                }
                                if (checkNotNullEmpty(v.secDen)) {
                                    v.den = v.secDen;
                                }
                            });
                        }
                        if (isTotalValue) {
                            lData = angular.copy(lData);
                            angular.forEach(lData.value, function (v) {
                                if(checkNotNullEmpty(v.value) && checkNotNullEmpty(v.secValue)) {
                                    v.value = (parseFloat(v.value) + (-1 * parseFloat(v.secValue))) + "";
                                } else if (checkNotNullEmpty(v.value)) {
                                    v.value = parseFloat(v.value) + "";
                                }
                                v.num = v.den =undefined;
                            });
                        }
                        if (filterSeriesIndex >= 0) {
                            curLabel = lData.label;
                            if (found) {
                                if (curLabel == prevLabel) {
                                    continue;
                                }
                                found = false;
                                prevLabel = undefined;
                            }

                            if (!found && ((i == 0 && lData.value[filterSeriesIndex].value != name) || (prevLabel != undefined && curLabel != prevLabel))) {
                                var dummy = {};
                                dummy.value = [];
                                dummy.value[seriesno] = {};
                                dummy.value[seriesno].value = "0";
                                dummy.label = prevLabel;
                                lData = dummy;
                                if (prevLabel != undefined) {
                                    i++;
                                }
                                prevLabel = undefined;
                                found = true;
                            } else if (lData.value[filterSeriesIndex].value == name) {
                                found = true;
                                prevLabel = curLabel;
                            } else {
                                prevLabel = curLabel;
                            }
                            if (!found) {
                                continue;
                            }
                        }
                        var t = {};
                        t.value = lData.value[seriesno].value || "0";
                        if (zeroWithEmpty && (t.value == "0" || t.value == "0.0")) {
                            t.value = "";
                        }
                        var dec = checkNotNullEmpty(t.value) ? t.value.indexOf(".") : -1;
                        if (dec >= 0) {
                            t.displayValue = reportCoreFunction().roundNumber(t.value);
                        }
                        if (!isLinkDisabled && !(t.value == "0" || t.value == "0.0")) {
                            t.link = "JavaScript: callFunctionByName('getDFChartData','" + lData.label + "')";
                        }
                        if (color) {
                            t.color = color;
                        }
                        if (name && !skipSeriesInLabel) {
                            t.toolText = "$seriesName, ";
                        }
                        if (forceSum || (checkNotNullEmpty(type) && (type.indexOf("Pie") == 0 || type.indexOf("Doughnut") == 0))) {
                            t.toolText = (t.toolText ? t.toolText : "" ) + "$label: $value of $unformattedSum";
                        } else {
                            t.toolText = (t.toolText ? t.toolText : "" ) + "$label: " + reportCoreFunction().roundNumber(lData.value[seriesno].value, 2);
                            if (lData.value[seriesno].num) {
                                t.toolText += " (" + reportCoreFunction().roundNumber(lData.value[seriesno].num, 2) + " / " + reportCoreFunction().roundNumber(lData.value[seriesno].den, 2) + ")";
                                t.tableTooltip = reportCoreFunction().roundNumber(lData.value[seriesno].num, 2) + " / " + reportCoreFunction().roundNumber(lData.value[seriesno].den, 2);
                            }
                        }
                        series.data[ind++] = t;
                    }
                    return series;
                }
            }
        },
        roundNumber: function (value, digits, forceRound) {
            if (checkNotNullEmpty(value)) {
                digits = digits || 0;
                if (value * 1 < 1 && !forceRound && digits == 0) {
                    digits = 2;
                }
                value = parseFloat(value).toFixed(digits);
                var dec = checkNotNullEmpty(value) ? value.indexOf(".") : -1;
                if (parseFloat(value.substr(dec + 1)) == 0) {
                    value = value.substr(0, dec);
                }
            }
            return value || 0;
        },
        getReportFCCategories: function (data, format) {
            if (checkNotNullEmpty(data)) {
                var category = [];
                var labels = [];
                var lIndex = 0;
                var ind = 0;
                format = format || "mmm dd, yyyy";
                for (var i = data.length - 1; i >= 0; i--) {
                    if (labels.indexOf(data[i].label) == -1) {
                        var t = {};
                        t.label = formatLabel(data[i].label, format);
                        t.csvLabel = data[i].label;
                        labels[lIndex++] = data[i].label;
                        category[ind++] = t;
                    }
                }
                return category;
            }
        },
        getReportTableData: function (data, labels, format) {
            var tData = [];
            format = format || "mmm dd, yyyy";
            for (var i = labels.length - 1; i >= 0; i--) {
                var tRow = [];
                var label = reportCoreFunction().formatDateLabel(labels[i].csvLabel, "yyyy-MM-dd");
                tRow.push(labels[i].label);
                for (var j = 0; j < data.length; j++) {
                    tRow.push(
                        {
                            label: label,
                            value: reportCoreFunction().roundNumber(data[j].data[i].value, 2),
                            tooltip: data[j].data[i].tableTooltip
                        });
                }
                tData.push(tRow);
            }
            return tData;
        },
        formatDateLabel: function (label, format) {
            switch (format) {
                case "mmm yyyy":
                    return FormatDate_MMM_YYYY(constructDate(label));
                case "mmm dd, yyyy":
                    return FormatDate_MMM_DD_YYYY(constructDate(label));
                case "yyyy-MM-dd":
                    return FormatDate_YYYY_MM_DD(constructDate(label));
                default:
                    return FormatDate_DD_MM_YYYY(constructDate(label));
            }
        },
        getReportSummaryTable: function (data, labels, init, size) {
            var sumTable = [];
            var sumRow = [];
            for (var i = 0; i < labels.length + 1; i++) {
                sumRow[i] = 0;
            }
            size = size || 1;
            for (i = 0; i < data.length; i++) {
                for (var j = 0; j < labels.length; j++) {
                    sumRow[j] += parseInt(data[i].value[j * size + init].value || 0)
                }
            }
            var total = 0;
            for (i = 0; i < labels.length; i++) {
                sumTable[i] = [labels[i], sumRow[i]];
                total += sumRow[i];
            }
            sumTable[labels.length] = ["Total", total];
            return sumTable;
        },
        getPeriodicityDisplay: function (periodicity, periodicityText) {
            var display = '';
            switch (periodicity) {

                case 'd': {
                    display = periodicityText.daily;
                    break;
                }
                case 'w': {
                    display = periodicityText.weekly;
                    break;
                }
                default: {
                    display = periodicityText.monthly;
                    break;
                }
            }
            return display;
    },
        formatReportTableData: function (data) {
            for (var key in data) {
                angular.forEach(data[key], function (v) {
                    angular.forEach(v, function (d) {
                        var dec = checkNotNullEmpty(d.value) ? d.value.indexOf(".") : -1;
                        if (dec >= 0) {
                            d.value = reportCoreFunction().roundNumber(d.value, 2);
                        }
                        var secDec = checkNotNullEmpty(d.secValue) ? d.secValue.indexOf(".") : -1;
                        if (secDec >= 0) {
                            d.secValue = reportCoreFunction().roundNumber(d.secValue, 2);
                        }
                        if (checkNotNullEmpty(d.num)) {
                            d.num = reportCoreFunction().roundNumber(d.num, 2);
                        }
                        if (checkNotNullEmpty(d.den)) {
                            d.den = reportCoreFunction().roundNumber(d.den, 2);
                        }
                    });
                });
            }
        }
    }
}
