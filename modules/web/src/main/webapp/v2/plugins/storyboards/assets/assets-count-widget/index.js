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
angular.module('logistimo.storyboard.assetsCountWidget', [])
    .config(function (widgetsRepositoryProvider) {
        widgetsRepositoryProvider.addWidget({
            id: "assetsCountWidget",
            name: "asset.count",
            templateUrl: "plugins/storyboards/assets/assets-count-widget/assets-count-widget.html",
            editTemplateUrl: "plugins/storyboards/assets/asset-edit-template.html",
            templateFilters: [
                {
                    nameKey: 'asset.type',
                    type: 'assetType'
                },
                {
                    nameKey: 'period.since',
                    type: 'tPeriod'
                },
                {
                    nameKey: 'exclude.temp.state',
                    type: 'exTempState'
                }
            ],
            defaultHeight: 1,
            defaultWidth: 4
        });
    })
    .controller('assetsCountWidgetController', ['$scope', 'dashboardService', function ($scope, dashboardService) {
        var filter = angular.copy($scope.widget.conf);
        var asset = '';
        $scope.totalAssets = 0;
        var fDate = (checkNotNullEmpty(filter.date) ? formatDate(filter.date) : undefined);
        $scope.showChart = false;
        $scope.wloading = true;
        $scope.showError = false;
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

        getData();

        function getData() {
            dashboardService.get(undefined, undefined, $scope.exFilter, $scope.exType, $scope.period,
                $scope.widget.conf.tPeriod, asset, constructModel(filter.entityTag), fDate,
                constructModel(filter.exEntityTag), false).then(function (data) {
                    if(!checkNullEmptyObject(data.data.tempDomain)) {
                        $scope.totalAssets = getTotalItems(data.data.tempDomain);
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
        $scope.computedHeight = parseInt($scope.widget.computedHeight, 10)-10;

    }]);

logistimoApp.requires.push('logistimo.storyboard.assetsCountWidget');
