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
 * Created by naveensnair on 08/11/17.
 */
function DashboardRepository(apiService, $q) {
    return {
        get: function (dashboardId, $scope) {
            var deferred = $q.defer();
            $scope.showLoading();
            apiService.get("/s2/api/dashboards/" + dashboardId).then(function (data) {
                var dashboard = data.data;
                if (dashboard) {
                    dashboard.widgets = JSON.parse(dashboard.widgets);
                }
                deferred.resolve(dashboard);
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
            apiService.get("/s2/api/dashboards/all").then(function (data) {
                deferred.resolve(data.data);
            }).catch(function error(msg) {
                $scope.showErrorMsg(msg);
                deferred.reject();
            }).finally(function () {
                $scope.hideLoading();
            });
            return deferred.promise;
        },
        save: function (dashboard, $scope) {
            if ($scope.isUndef(dashboard.name)) {
                $scope.showWarning("Name is mandatory");
                return;
            } else if ($scope.isUndef(dashboard.widgets) || checkNullEmptyObject(dashboard.widgets)) {
                $scope.showWarning("Please configure widgets");
                return;
            }
            var deferred = $q.defer();
            $scope.showLoading();
            dashboard.widgets = JSON.stringify(dashboard.widgets);
            apiService.post(dashboard, "/s2/api/dashboards/").then(function (data) {
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
        delete: function (dashboardId, scope) {

        }
    }
}
