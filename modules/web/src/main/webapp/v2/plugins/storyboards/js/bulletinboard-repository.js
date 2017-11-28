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
 * Created by naveensnair on 14/11/17.
 */

function BulletinBoardRepository(apiService, $q) {
    return {
        /*checkNotNullEmpty : function (argument) {
         return typeof argument !== 'undefined' && argument != null && argument != "";
         },
         checkNullEmpty : function (argument) {
         return !$scope.checkNotNullEmpty(argument);
         },
         constructDashboardsForBulletinBoard: function (bulletinBoard) {
         if(checkNotNullEmpty(bulletinBoard.dashboards)) {
         bulletinBoard.conf = [];
         bulletinBoard.dashboards.forEach(function (db) {
         bulletinBoard.conf.push(db.dbId);
         });
         }
         return bulletinBoard;
         },*/
        get: function (bulletinBoardId, $scope) {
            var deferred = $q.defer();
            $scope.showLoading();
            apiService.get("/s2/api/bulletinboard/" + bulletinBoardId).then(function (data) {
                var bulletinBoard = data.data;
                if (bulletinBoard) {
                    bulletinBoard.dashboards = [];
                    bulletinBoard.dashboards = JSON.parse(bulletinBoard.conf);
                    delete bulletinBoard["conf"];
                }
                deferred.resolve(bulletinBoard);
            }).catch(function error(msg) {
                $scope.showErrorMsg(msg);
                deferred.reject();
            }).finally(function () {
                $scope.hideLoading();
            });
            return deferred.promise;
        },
        getAll: function ($scope) {
            var deferred = $q.defer();
            $scope.showLoading();
            apiService.get("/s2/api/bulletinboard/all").then(function (data) {
                deferred.resolve(data.data);
            }).catch(function error(msg) {
                $scope.showErrorMsg(msg);
                deferred.reject();
            }).finally(function () {
                $scope.hideLoading();
            });
            return deferred.promise;
        },
        save: function (bulletinBoard, $scope) {
            if ($scope.isUndef(bulletinBoard.name)) {
                $scope.showWarning("Name is mandatory");
                return;
            } else if ($scope.isUndef(bulletinBoard.min)) {
                $scope.showWarning("Minimum scroll time is mandatory");
                return;
            } else if ($scope.isUndef(bulletinBoard.max)) {
                $scope.showWarning("Maximum scroll time is mandatory");
                return;
            } else if ($scope.isUndef(bulletinBoard.dashboards) || bulletinBoard.dashboards.length == 0) {
                $scope.showWarning("Please configure dashboards");
                return;
            }
            var deferred = $q.defer();
            $scope.showLoading();
            var conf = bulletinBoard.dashboards;
            bulletinBoard.conf = JSON.stringify(conf);
            apiService.post(bulletinBoard, "/s2/api/bulletinboard/").then(function (data) {
                deferred.resolve(data.data);
                $scope.showSuccess(data.data);
            }).catch(function error(msg) {
                $scope.showErrorMsg(msg);
                deferred.reject();
            }).finally(function () {
                $scope.hideLoading();
            });
            return deferred.promise;
        },
        delete: function (bulletinBoardId, scope) {

        },
        update: function (bulletinBoard, $scope) {
            var deferred = $q.defer();
            $scope.showLoading();
            bulletinBoard.conf = JSON.stringify(bulletinBoard.conf);
            apiService.post(bulletinBoard, "/s2/api/bulletinboard/update").then(function (data) {
                deferred.resolve(data.data);
                $scope.showSuccess(data.data);
            }).catch(function error(msg) {
                $scope.showErrorMsg(msg);
                deferred.reject();
            }).finally(function () {
                $scope.hideLoading();
            });
            return deferred.promise;
        }
    }
}
