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
 * Created by Mohan Raja on 03/04/15.
 */
var authServices = angular.module('authServices', []);
authServices.factory('iAuthService', ['APIService', '$rootScope', '$q', '$cookies', function (apiService, $rootScope, $q, $cookies) {
    return {
        login: function (userId,password,lang) {
            var urlStr = '/s2/api/mauth/login';
            var credentials = {};
            if (checkNotNullEmpty(userId)) {
                credentials.userId = userId;
            }
            if (checkNotNullEmpty(password)) {
                credentials.password = password;
            }
            if (checkNotNullEmpty(lang)) {
                credentials.language = lang;
            }
            return apiService.post(credentials, urlStr);
        },
        logout: function () {
            var service = this;
            return $q(function (resolve, reject) {
                apiService.get('/s2/api/auth/logout').then(function () {
                    service.removeAccessToken();
                    resolve();
                }).catch(function (err) {
                    reject(err);
                })
            });
        },
        generatePassword: function(data){
            return apiService.post(data, '/s2/api/auth/resetpassword/');
        },
        generateOtp: function(data){
            return apiService.post(data, '/s2/api/auth/generateOtp');
        },
        getAccessToken: function () {
            var token = localStorage.getItem("x-access-token");
            var expires = localStorage.getItem("expires");
            if (checkNotNullEmpty(token)) {
                if (checkNullEmpty(expires) || expires > new Date().getTime()) {
                    return token;
                } else {
                    this.removeAccessToken();
                }
            }
            return null;
        },
        setAccessToken: function (accessToken, expires) {
            localStorage.setItem("x-access-token", accessToken);
            $cookies.put("x-access-token", accessToken, {path: "/"});
            if (checkNotNullEmpty(expires)) {
                localStorage.setItem("expires", expires);
            }
        },
        removeAccessToken: function () {
            localStorage.removeItem('x-access-token');
            localStorage.removeItem('expires');
            localStorage.removeItem("domain");
            $cookies.remove("x-access-token", {path: "/"});
            $cookies.remove("x-access-domain", {path: "/"});
        },
        authorizeBulletinBoard: function (authKey) {
            return apiService.post(authKey, "/s2/api/auth/authorise-access-key");
        },
        requestAccessKey: function () {
            localStorage.removeItem("domain");
            $cookies.remove("x-access-domain", {path: "/"});
            return apiService.get("/s2/api/auth/request-access-key");
        },
        checkAccessKey: function (authKey) {
            return apiService.post(authKey, "/s2/api/auth/check-access-key");
        }

    }
}
]);