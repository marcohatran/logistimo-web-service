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
 * Created by naveensnair on 15/11/17.
 */
var bulletinBoardControllers = angular.module('bulletinBoardControllers', []);
bulletinBoardControllers.controller('BulletinBoardRenderController', ['bulletinBoardRepository', 'dashboardRepository', '$scope', 'requestContext','AnalyticsService', function (bulletinBoardRepository, dashboardRepository, $scope, requestContext, analyticsService) {
    function init() {
        $scope.title = "";
        $scope.subTitle = "";
        $scope.showDomain = false;
        if(checkNotNullEmpty($scope.domainName)) {
            $scope.showDomain = true;
        }
        var dimensionDomain = $scope.domainName;
        var dimensionUser = $scope.curUserName;
        var path = window.location.pathname;
        var userTags = $scope.curUserTag;
        analyticsService.logAnalytics(path,dimensionUser,dimensionDomain,userTags);
    }

    init();

    $scope.setBBTitle = function (title, subTitle) {
        $scope.title = title;
        $scope.subTitle = subTitle;
    };

    $scope.$on("offline", function () {
        if ($scope.isUndef($scope.offLineSince)) {
            $scope.offLineSince = $scope.formatDate(new Date());
        }
        $scope.offLineMessage = $scope.resourceBundle['network.unavailable'] + " ";
    });

    $scope.$on("online", function () {
        $scope.offLineSince = "";
    });
}]);

bulletinBoardControllers.controller('BulletinBoardConfigController', ['domainCfgService', '$scope',
    function(domainCfgService, $scope) {

        $scope.saveExpiry = function() {
                $scope.showLoading();
                domainCfgService.setBulletinBoardCfg($scope.bb).then(function(data) {
                    $scope.showSuccess($scope.resourceBundle['bulletin.expiry.update.success']);
                }).catch(function error(msg) {
                    $scope.showErrorMsg(msg,true);
                }).finally(function(){
                    $scope.getBulletinBoardConfig();
                    $scope.hideLoading();
                });
        };

        $scope.getBulletinBoardConfig = function() {
            $scope.showLoading();
            domainCfgService.getBulletinBoardCfg().then(function(data) {
                if(checkNotNullEmpty(data.data)) {
                    $scope.bb = data.data;
                }
            }).catch(function error(msg) {
                $scope.showErrorMsg(msg,true);
            }).finally(function(){
                $scope.hideLoading();
            });
        };

        function init() {
            $scope.bb = {};
            $scope.getBulletinBoardConfig();
        }

        init();
    }]);