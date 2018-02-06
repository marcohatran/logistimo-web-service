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

logistimoApp.factory('APIService', function ($http, $q, $rootScope) {
    var checkDomainAndReject = function () {
        var localDomain = localStorage.getItem("domain");
        if (checkNotNullEmpty($rootScope.currentDomain) && checkNotNullEmpty(localDomain)) {
            if (localDomain != $rootScope.currentDomain) {
                $rootScope.reloadPage();
                return true;
            }
        }
        return false;
    };
    var cached = {};
    var get = function (urlStr) {
        if (checkDomainAndReject()) {
            return $q(function (resolve, reject) {
                reject();
            });
        }
        if (!$rootScope.isBulletinBoard) {
            return $http({method: 'GET', url: urlStr});
        } else if ($rootScope.networkAvailable === false) {
            return $q(function (resolve, reject) {
                var cache = localStorage.getItem(urlStr);
                if (checkNotNullEmpty(cache)) {
                    resolve(JSON.parse(cache).data);
                } else {
                    reject("Network is not available");
                }
            });
        } else {
            $rootScope.systemError = false;
            var promise = $http({method: 'GET', url: urlStr});
            return $q(function (resolve, reject) {
                promise.then(function (data) {
                    localStorage.setItem(urlStr, JSON.stringify({
                        ts: new Date(),
                        data: data
                    }));
                    resolve(data);
                }).catch(function (error) {
                    $rootScope.systemError = true;
                    reject(error);
                });
            });
        }
    };
    return {
        getCached: function(cacheKey, urlStr){
            return $q(function (resolve, reject) {
                if(cached[cacheKey]){
                    resolve(cached[cacheKey]);
                    return;
                }
                get(urlStr).then(function(data){
                    cached[cacheKey] = data;
                    resolve(data);
                }).catch(function(err){
                    reject(err);
                })
            });
        },
        get: get,
        post: function (data, urlStr) {
            if (checkDomainAndReject()) {
                return $q(function (resolve, reject) {
                    reject();
                });
            }
            return $http({method: 'POST', data: data, url: urlStr});
        },
        put: function (data, urlStr) {
            if (checkDomainAndReject()) {
                return $q(function (resolve, reject) {
                    reject();
                });
            }
            return $http({method: 'PUT', data: data, url: urlStr})
        },
        delete: function (urlStr) {
            if (checkDomainAndReject()) {
                return $q(function (resolve, reject) {
                    reject();
                });
            }
            return $http({method: 'DELETE', url: urlStr})
        },
        serveFile: function (url, defaultFileName) {
            if (!checkDomainAndReject()) {
                $http({method: 'GET', url: url}).
                    success(function (data,status, headers, config) {
                        var fileName=headers()['content-disposition'];
                        if(fileName) {
                            fileName = fileName.substring(fileName.indexOf("=")+1);
                        }else {
                            fileName = defaultFileName;
                        }
                        if (window.navigator.msSaveOrOpenBlob) {
                            var blob = new Blob([data]);  //csv data string as an array.
                            // IE hack; see http://msdn.microsoft.com/en-us/library/ie/hh779016.aspx
                            window.navigator.msSaveBlob(blob, fileName);
                        } else {
                            var anchor = angular.element('<a/>');
                            anchor.css({display: 'none'}); // Make sure it's not visible
                            angular.element(document.body).append(anchor); // Attach to document for FireFox

                            anchor.attr({
                                href: 'data:attachment/csv;charset=utf-8,' + encodeURI(data),
                                target: '_blank',
                                download: fileName
                            })[0].click();
                            anchor.remove();
                        }
                    }).
                    error(function (data,status, headers, config) {
                        $rootScope.showError(data.data);
                    });
            }
        }
    }
});