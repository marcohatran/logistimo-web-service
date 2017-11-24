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
bulletinBoardControllers.controller('BulletinBoardController', ['bulletinBoardRepository', 'dashboardRepository', '$scope', 'requestContext', function (bulletinBoardRepository, dashboardRepository, $scope, requestContext) {
    $scope.init = function () {
        $scope.bulletinBoardId = requestContext.getParam("bulletinBoardId");
        if ($scope.bulletinBoardId != undefined) {
            bulletinBoardRepository.get($scope.bulletinBoardId, $scope).then(function (bulletinBoard) {
                $scope.bulletinBoard = bulletinBoard;
                buildBulletinBoardDashboards(JSON.parse($scope.bulletinBoard.conf));
            });
        } else {
            $scope.bulletinBoard = {dashboards: []};
        }
        $scope.selectedRow = null;
    };
    $scope.init();

    function buildBulletinBoardDashboards(conf) {
        $scope.bulletinBoard.dashboards = [];
        if (checkNotNullEmpty(conf)) {
            conf.forEach(function (c) {
                dashboardRepository.get(c, $scope).then(function(data) {
                    $scope.bulletinBoard.dashboards.push(data);
                })
            });
        }
    };

    $scope.checkNotNullEmpty = function (argument) {
        return typeof argument !== 'undefined' && argument != null && argument != "";
    };

    $scope.checkNullEmpty = function (argument) {
        return !$scope.checkNotNullEmpty(argument);
    };

    $scope.addDashboard = function (dashboard) {
        $scope.bulletinBoard.dashboards.push(dashboard);
    };

    $scope.setClickedRow = function (index) {
        $scope.selectedRow = index;
        $scope.bulletinBoard.dashboards[index].isChecked = true;
    };

    $scope.moveUp = function (num) {
        if (num > 0) {
            tmp = $scope.bulletinBoard.dashboards[num - 1];
            $scope.bulletinBoard.dashboards[num - 1] = $scope.bulletinBoard.dashboards[num];
            $scope.bulletinBoard.dashboards[num] = tmp;
            $scope.selectedRow--;
            $scope.bulletinBoard.dashboards[num - 1].isChecked = false;
        }
    };

    $scope.moveDown = function (num) {
        if (num < $scope.bulletinBoard.dashboards.length - 1) {
            tmp = $scope.bulletinBoard.dashboards[num + 1];
            $scope.bulletinBoard.dashboards[num + 1] = $scope.bulletinBoard.dashboards[num];
            $scope.bulletinBoard.dashboards[num] = tmp;
            $scope.selectedRow++;
            $scope.bulletinBoard.dashboards[num + 1].isChecked = false;
        }
    };

    $scope.save = function () {
        if ($scope.checkNotNullEmpty($scope.bulletinBoard)) {
            bulletinBoardRepository.save($scope.constructBulletinBoardModel($scope.bulletinBoard, false), $scope);
        }
    };

    $scope.update = function () {
        delete $scope.bulletinBoard["conf"];
        if ($scope.checkNotNullEmpty($scope.bulletinBoard)) {
            var bb = $scope.constructBulletinBoardModel($scope.bulletinBoard, true);
            bulletinBoardRepository.update(bb, $scope);
        }
    };

    $scope.constructBulletinBoardModel = function (bulletinBoard, update) {
        if ($scope.checkNotNullEmpty(bulletinBoard)) {
            var bb = {conf: []};
            bb.name = bulletinBoard.name;
            bb.desc = bulletinBoard.desc;
            bb.min = bulletinBoard.min;
            bb.max = bulletinBoard.max;
            if (update) {
                bb.bbId = bulletinBoard.bbId;
            }
            bulletinBoard.dashboards.forEach(function (data, i) {
                var dbId = undefined;
                if (checkNotNullEmpty(data.dbId)) {
                    dbId = data.dbId;
                } else if (checkNotNullEmpty(data.id)) {
                    dbId = data.id;
                }
                bb.conf.push(dbId);
            });
            return bb;
        }
    };

    $scope.removeDashboardFromBulletinBoard = function (index) {
        $scope.bulletinBoard.dashboards.splice(index, 1);
    };
    $scope.buildBulletinBoardConf = function (data, dbId) {
        if (checkNotNullEmpty(data)) {
            return {"name": data.name, "id": dbId};
        }
    }
}]);
bulletinBoardControllers.controller('BulletinBoardsListingController', ['bulletinBoardRepository', '$scope', function (bulletinBoardRepository, $scope) {
    $scope.init = function () {
        $scope.bulletinBoards = {};
    };
    $scope.init();

    bulletinBoardRepository.getAll($scope).then(function (bulletinBoards) {
        $scope.bulletinBoards = bulletinBoards;
    })
}]);
bulletinBoardControllers.controller('BulletinBoardViewController', ['bulletinBoardRepository', 'dashboardRepository',
    '$scope', 'requestContext', '$timeout',
    function (bulletinBoardRepository, dashboardRepository, $scope, requestContext, $timeout) {
    $scope.init = function () {
        $scope.renderDashboardsPage = false;
        $scope.bulletinBoardId = requestContext.getParam("bulletinBoardId");
        if ($scope.bulletinBoardId != undefined) {
            bulletinBoardRepository.get($scope.bulletinBoardId, $scope).then(function (bulletinBoard) {
                $scope.bulletinBoard = bulletinBoard;
                if (checkNotNullEmpty($scope.bulletinBoard)) {
                    $scope.bulletinBoard.conf = JSON.parse(bulletinBoard.conf);
                    renderDashboards();
                }
            });
        } else {
            $scope.bulletinBoard = {dashboards: []};
        }
        $scope.myInterval = 1000;
        $scope.noWrapSlides = false;
        $scope.activeSlide = 0;
        $scope.count = 0;
    };
    $scope.init();
        //var index = 0;

    function renderDashboards() {
        $scope.dashboardId = $scope.bulletinBoard.conf[$scope.count];
        $scope.renderDashboardsPage = true;
        $timeout(function () {
            $scope.count = $scope.count + 1;
            $scope.renderDashboardsPage = false;
            if($scope.count == $scope.bulletinBoard.conf.length) {
                $scope.count = 0;
            }
            $timeout(function () {
                renderDashboards();
            }, 10);
        }, $scope.bulletinBoard.max * 1000);
    }

}]);