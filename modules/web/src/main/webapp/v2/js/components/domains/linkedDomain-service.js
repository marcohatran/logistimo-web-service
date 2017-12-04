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

var domainServices = angular.module('linkedDomainServices', []);
domainServices.factory('linkedDomainService', ['APIService', function (apiService) {
    return {
        getChildSuggestions: function (text,domainId) {
            var eParam = '';
            if(domainId){
                eParam = '&reqDomainId='+domainId;
            }
            return apiService.get('/s2/api/linked/domain/unlinked?q=' + text + eParam);
        },
        addChildren: function (data) {
            return apiService.post({domainModel: data}, '/s2/api/linked/domain/add');
        },
        addChildrenTodomain: function (data,domainId) {
            return apiService.post({domainModel: data, domainId: domainId}, '/s2/api/linked/domain/add');
        },
        fetchLinkedDomains: function (domainId) {
            var params = '';
            if (domainId != null) {
                params = '?domainId=' + domainId;
            }
            return apiService.get('/s2/api/linked/domain/' + params);
        },
        deleteDomainLink: function (domainId) {
            return apiService.get('/s2/api/linked/domain/delete?domainId=' + domainId);
        },
        getLinkedDomainSuggestion: function(query) {
            return apiService.get('/s2/api/linked/domain/suggestions?q=' + query);
        },
        getDomainPermission: function(action,domainId){
            var params = "";
            if(typeof action !== 'undefined'){
                params += "?action=" + action;
            }
            if(typeof domainId !== 'undefined'){
                if(params == ''){
                    params +="?";
                }else{
                    params +="&";
                }
                params += "domainId=" + domainId;
            }
            return apiService.get('/s2/api/linked/domain/permission' + params);
        },
        getLinkedDomains: function (type) {
            return apiService.get('/s2/api/linked/domain/parents?domainType=' + type);
        },
        getParentsByDomainId : function(domainId, type){
            var param = "?domainId=" + domainId + "&domainType=" + type;
            return apiService.get('/s2/api/linked/domain/parents' + param);
        },

        updateChildDomainPermissions: function (data) {
            return apiService.post(data, '/s2/api/linked/domain/updatepermission');
        },
        pushConfiguration: function () {
            return apiService.get('/s2/api/linked/domain/push');
        }

    }
}]);