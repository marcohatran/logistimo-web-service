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
 * Created by naveensnair on 30/11/17.
 */

angular.module('logistimo.storyboard.eventSummariesTopTenWidget', [])
    .config(function (widgetsRepositoryProvider) {
        widgetsRepositoryProvider.addWidget({
            id: "eventSummariesTopTenWidget",
            name: "Event summaries top ten",
            templateUrl: "plugins/storyboards/event-summaries/event-summaries-top-ten-widget/event-summaries-top-ten-widget.html",
            editTemplateUrl: "plugins/storyboards/event-summaries/event-summaries-edit-template.html",
            templateFilters: [{
                nameKey: "event.category",
                type: "category"
            }, {
                nameKey: "event",
                type: "event"
            }, {
                nameKey: "threshold",
                type: "threshold"
            }, {
                nameKey: "locations.number",
                type: "locations"
            }],
            defaultHeight: 2,
            defaultWidth: 4
        });
    })
    .controller('eventSummaryTopTenWidgetController', ['$scope', 'eventSummaryService', function ($scope, eventSummaryService) {

        function getData() {
            $scope.wloading = true;
            $scope.showError = false;
            $scope.noData = false;
            $scope.offset = 0;
            if(checkNotNullEmpty($scope.widget.conf.locations)) {
                $scope.size = $scope.widget.conf.locations;
            }
            eventSummaryService.getEventSummariesByEventId($scope.currentDomain, $scope.curUser, $scope.widget.conf.threshold, $scope.offset, $scope.size).then(function (data) {
                if(checkNotNullEmpty(data.data) && checkNotNullEmpty(data.data.summaries)) {
                    $scope.eventSummaries = data.data.summaries;
                    $scope.heading = getHeading($scope.eventSummaries, $scope.dstate, $scope.ddist);
                } else {
                    $scope.noData = true;
                }
            }).catch(function error(msg) {
                $scope.showError = true;
                $scope.showErrorMsg(msg);
            }).finally(function () {
                $scope.wloading = false;
            })
        }

        function init() {
            $scope.size = 10;
            getData();
        }
        init();
    }]);

logistimoApp.requires.push('logistimo.storyboard.eventSummariesTopTenWidget');
