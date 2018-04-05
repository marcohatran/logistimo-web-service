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
 * Created by yuvaraj on 10/11/17.
 */
angular.module('logistimo.storyboard.storesCountWidget', [])
    .config(function (widgetsRepositoryProvider) {
        widgetsRepositoryProvider.addWidget({
            id: "storesCountWidget",
            name: "entity.count",
            templateUrl: "plugins/storyboards/inventory/stores-count-widget/stores-count-widget.html",
            editTemplateUrl: "plugins/storyboards/inventory/edit-template.html",
            templateFilters: [
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
            defaultHeight: 1,
            defaultWidth: 4
        });
    })
    .controller('storesCountWidgetController',
    ['$scope', 'dashboardService', 'domainCfgService', function ($scope, dashboardService, domainCfgService) {
        var filter = angular.copy($scope.widget.conf);
        var entPieColors;
        var fDate = (checkNotNullEmpty(filter.date) ? formatDate(filter.date) : undefined);
        $scope.showChart = false;
        $scope.totalInv = 0;
        $scope.wloading = true;
        $scope.showError = false;
        function setFilters() {
            if (checkNotNullEmpty(filter.period)) {
                var p = filter.period;
                if (p == '0' || p == '1' || p == '2' || p == '3' || p == '7' || p == '30') {
                    $scope.period = p;
                }
            } else {
                $scope.period = "0";
            }

            if (checkNotNullEmpty(filter.materialTag)) {
                $scope.exFilter = constructModel(filter.materialTag);
                $scope.exType = 'mTag';
            } else if (checkNotNullEmpty(filter.material)) {
                $scope.exFilter = filter.material.id;
                $scope.exType = 'mId';
            }
        }

        domainCfgService.getSystemDashboardConfig().then(function (data) {
            var domainConfig = angular.fromJson(data.data);
            entPieColors = domainConfig.pie.ec;
        }).then(function () {
            setFilters();
        }).then(function () {
            getData();
        });

        function getData() {
            dashboardService.get(undefined, undefined, $scope.exFilter, $scope.exType, $scope.period, undefined,
                undefined, constructModel(filter.entityTag), fDate, constructModel(filter.exEntityTag), false).then(
                function (data) {
                    if(checkNullEmptyObject(data.data.entDomain)) {
                        $scope.noDataToRender();
                    }
                    getTotalItems(data.data.entDomain);
                    setWidgetData();
                }).catch(function error(msg) {
                    $scope.noDataToRender();
                    showError(msg, $scope);
                }).finally(function () {
                    $scope.loading = false;
                    $scope.wloading = false;
                });
        }

        function getTotalItems(data) {
            $scope.entDomainTotal = (data.a || 0) + (data.i || 0);
        }

        function setWidgetData() {
            $scope.storesCountWidget = {
                computedWidth: '100%',
                computedHeight: parseInt($scope.widget.computedHeight, 10)-10
            };
        }

    }]);

logistimoApp.requires.push('logistimo.storyboard.storesCountWidget');
