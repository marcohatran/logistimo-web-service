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
 * Created by naveensnair on 01/12/17.
 */
angular.module('logistimo.storyboard.eventSummariesUserProfileWidget', [])
    .config(function (widgetsRepositoryProvider) {
        widgetsRepositoryProvider.addWidget({
            id: "eventSummariesUserProfileWidget",
            name: "event.summaries.user.profile",
            templateUrl: "plugins/storyboards/event-summaries/event-summaries-user-profile-widget/event-summaries-user-profile-widget.html",
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
                nameKey: "profile.number",
                type: "locations"
            }],
            defaultHeight: 2,
            defaultWidth: 4
        });
    })
    .controller('eventSummaryUserProfileWidgetController', ['$scope', 'eventSummaryService', function ($scope, eventSummaryService) {

        function populateUserProfiles(summaries) {
            summaries.forEach(function (data) {
                var profile = {name: "", title: "", user: "", image_url: ""};
                profile.name = data.name;
                profile.title = data.title;
                profile.subtitle = data.subtitle;
                profile.show_image = false;
                profile.type = data.type;
                if(checkNotNullEmpty(data.contacts)) {
                    if(checkNotNullEmpty(data.contacts[0].user)) {
                        profile.user = data.contacts[0].user;
                    }
                    if(checkNotNullEmpty(data.contacts[0].photo)) {
                        profile.image_url = data.contacts[0].photo[0].serving_url;
                        if(checkNotNullEmpty(profile.image_url)) {
                            profile.show_image = true;
                        }
                    }
                }
                $scope.profiles.push(profile);
            })
        }
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
                    populateUserProfiles(data.data.summaries);
                    $scope.heading = getHeading(data.data.summaries, $scope);
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
            $scope.profiles = [];
            getData();
        }
        init();
    }]);

logistimoApp.requires.push('logistimo.storyboard.eventSummariesUserProfileWidget');