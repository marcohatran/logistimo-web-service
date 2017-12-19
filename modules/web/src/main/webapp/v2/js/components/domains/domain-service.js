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

var domainServices = angular.module('domainServices', []);
domainServices.factory('domainService', ['APIService', '$q', '$cookies', function (apiService, $q, $cookies) {
    return {
        createDomain: function (domainName, desc) {
            var param = '?domainName=' + domainName + '&desc=' + desc;
            return apiService.post(null, '/s2/api/domain/create' + param);
        },
        deleteDomain: function(dId) {
            var param = '?dId=' + dId;
            return apiService.post(null, '/s2/api/domain/delete' + param);
        },
        getCurrentDomain: function(){
            return apiService.get('/s2/api/domain/current');
        },
        switchDomain: function(domainId){
            return $q(function (resolve, reject) {
                apiService.post(domainId, '/s2/api/domain/switch').then(function (data) {
                    localStorage.setItem("domain", domainId);
                    $cookies.put("x-access-domain", domainId, {path: "/"});
                    resolve(data);
                }).catch(function (err) {
                    reject(err);
                })
            });
        },
        getCurrentUserDomain: function(){
            return apiService.get('/s2/api/domain/currentUser');
        },
        getDomainSuggestions: function (query) {
            var param = '';
            if(checkNotNullEmpty(query)){
                param = '?q='+query;
            }
            return apiService.get('/s2/api/domain/suggestions' + param);
        },
        updatedomaininfo: function (domainId, name, desc) {
            var param = '/' + domainId + '?name=' + name + '&desc=' + desc;
            return apiService.get('/s2/api/domain/updateDomain' + param);
        },
        fetchDomainById: function (data) {
            return apiService.get('/s2/api/domain/domain?domainId=' + data);
        }
    }
}]);