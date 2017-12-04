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
 * Created by naveensnair on 29/11/17.
 */

logistimoApp.controller('eventSummariesTemplateController', ['$scope', '$timeout', 'domainCfgService', function ($scope, $timeout, domainCfgService) {

    $scope.$watch('widget.conf.category', function (newVal, oldVal) {
        if (oldVal != newVal) {
            $scope.summaryEvents = [];
            $scope.widget.conf.event = "";
            $scope.summaryEvents = $scope.events[newVal];
        }
    });

    $scope.$watch('widget.conf.event', function (newVal, oldVal) {
        if (oldVal != newVal) {
            $scope.summaryThresholds = {};
            $scope.widget.threshold = "";
            if(checkNotNullEmpty(newVal) && checkNotNullEmpty($scope.thresholds[newVal])) {
                $scope.thresholds[newVal].forEach(function (data) {
                    $scope.summaryThresholds[data.id] = data.label;
                });
            }
        }
    });

    $scope.setSummaryId = function() {
        if(checkNotNullEmpty($scope.widget.conf.threshold)) {
            $scope.summaryId = $scope.widget.conf.threshold;
        }
    };

    function populateCategories(event) {
        if ($scope.categories[event.category] == undefined) {
            $scope.categories[event.category] = event.category;
        }
    }

    function populateEventTypes(event) {
        if ($scope.events[event.category] == undefined) {
            var event_type = [];
            event_type.push(event.event_type);
            $scope.events[event.category] = event_type;
        } else {
            $scope.events[event.category].push(event.event_type);
        }
    }

    function constructLabel(thresholds) {
        var label = "";
        if (checkNotNullEmpty(thresholds.conditions)) {
            thresholds.conditions.forEach(function (condition) {
                var construct = false;

                if(($scope.isDef(condition.value) && checkNotNullEmpty(condition.value)) || ($scope.isDef(condition.values) && checkNotNullEmpty(condition.values) && condition.values.length > 0)) {
                    construct = true;
                }
                if(construct){
                    if (checkNotNullEmpty(label)) {
                        label += ", ";
                    }
                    if (checkNotNullEmpty(condition.name) && (checkNotNullEmpty(condition.value)) ||
                        checkNotNullEmpty(condition.values)) {
                        label += condition.name + ":";
                    }
                    if (checkNotNullEmpty(condition.oper) && checkNotNullEmpty(condition.name)) {
                        label += " " + condition.oper;
                    }
                    if ($scope.isDef(condition.value) && checkNotNullEmpty(condition.value)) {
                        label += " " + condition.value;
                    }
                    if (checkNotNullEmpty(condition.units)) {
                        label += " " + condition.units;
                    }
                    if ($scope.isDef(condition.values) && checkNotNullEmpty(condition.values) && condition.values.length > 0) {
                        for (var i = 0; i < condition.values.length; i++) {
                            label += condition.values[i];
                            if (i != condition.values.length - 1) {
                                label += ",";
                            }
                        }
                    }
                }
            });
        }
        return label;
    }

    function constructThresholds(thresholds, event_type) {
        if (checkNotNullEmpty(thresholds)) {
            var thresholdArray = [];
            thresholds.forEach(function (th) {
                var thresholdObject = {id: "", label: ""};
                if (th.id != undefined) {
                    thresholdObject.id = th.id;
                    thresholdObject.label = constructLabel(th);
                    thresholdArray.push(thresholdObject);
                    $scope.thresholds[event_type] = thresholdArray;
                }
            });
        }
    }

    function populateThresholds(event) {
        if ($scope.thresholds[event.event_type] == undefined) {
            constructThresholds(event.thresholds, event.event_type);
        }

    }

    function getEventSummariesConfig() {
        $scope.wloading = true;
        domainCfgService.getEventSummaryConfig().then(function (data) {
            if (checkNotNullEmpty(data.data)) {
                $scope.eventSummaries = data.data;
                if (checkNotNullEmpty($scope.eventSummaries.events)) {
                    $scope.eventSummaries.events.forEach(function (d) {
                        populateCategories(d);
                        populateEventTypes(d);
                        populateThresholds(d);
                    });
                }
            }
        }).catch(function error(msg) {
            $scope.showErrorMsg(msg);
        }).finally(function () {
            $scope.hideLoading();
            $scope.loading = false;
            $scope.wloading = false;
        });
    }

    function init() {
        $scope.categories = {};
        $scope.events = {};
        $scope.thresholds = {};
        $scope.eventIds = {};
        getEventSummariesConfig();
        console.log($scope.thresholds);
    }

    init();


}]);
