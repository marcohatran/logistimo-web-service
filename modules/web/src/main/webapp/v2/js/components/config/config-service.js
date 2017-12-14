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

var configServices = angular.module('configServices', []);
configServices.factory('configService', ['APIService', function (apiService) {
    return {
        getLocations: function () {
            return apiService.getCached('config_locations','/s2/api/config/locations');
        },
        getCurrentDomainLocations: function () {
            return apiService.get('/s2/api/config/locations/currentdomain');
        },
        getCurrencies: function () {
            return apiService.getCached('config_currencies','/s2/api/config/currencies');
        },
        getLanguages: function () {
            return apiService.getCached('config_languages','/s2/api/config/languages');
        },
        getMobileLanguages: function () {
            return apiService.get('/s2/api/config/languages?type=mobile');
        },
        getTimezones: function () {
            return apiService.get('/s2/api/config/timezones');
        },
        getTimezonesWithOffset: function () {
            return apiService.get('/s2/api/config/timezones/offset');
        },
        getGeneralConfig: function() {
            return apiService.getCached('config_general','/s2/api/config/generalconfig');
        },
        getTimezonesKVReversed: function () {
            return apiService.get('/s2/api/config/timezoneskvreversed');
        },
        getConfigJson: function (configType) {
            return apiService.get('/s2/api/config/json?config_type=' + configType)
        },
        updateConfigJson: function (configType, configJson) {
            return apiService.post({type: configType, configJson: configJson}, '/s2/api/config/json');
        },
        simulateData: function (simulateDataRequest) {
            return apiService.post(simulateDataRequest, '/s2/api/admin/simulate-data');
        },
        updateLocationsMasterData: function () {
            return apiService.post({}, '/s2/api/admin/update-locations-data');
        }
    }
}]);
