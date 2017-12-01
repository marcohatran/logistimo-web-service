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

var matServices = angular.module('matServices', []);
matServices.factory('matService', ['APIService', function (apiService) {
    return {
        get: function (materialId) {
            return apiService.get('/s2/api/materials/material/' + materialId);
        },
        checkMaterialAvailability: function (mnm) {
            return apiService.get('/s2/api/materials/check/?mnm=' + mnm);
        },
        /*getMaterials: function (entityId, q, tag, offset, size) {
            offset = typeof offset !== 'undefined' ? offset : 0;
            size = typeof size !== 'undefined' ? size : 50;
            var urlStr = '/s2/api/materials/entity/' + entityId + "?offset=" + offset + "&size=" + size;
            if (checkNotNullEmpty(tag)) {
                urlStr = urlStr + "&tag=" + tag;
            }
            if (typeof q !== 'undefined') {
                urlStr = urlStr + "&q=" + q;
            }
         return apiService.get(urlStr);
        },*/
        getDomainMaterials: function (q, tag, offset, size, entityId, ihu) {
            offset = typeof offset !== 'undefined' ? offset : 0;
            size = typeof size !== 'undefined' ? size : 50;
            var urlStr = '/s2/api/materials/?offset=' + offset + "&size=" + size;
            if (checkNotNullEmpty(tag)) {
                urlStr = urlStr + "&tag=" + tag;
            }
            if (checkNotNullEmpty(q)) {
                urlStr = urlStr + "&q=" + q;
            }
            if (checkNotNullEmpty(entityId)) {
                urlStr = urlStr + "&entityId=" + entityId;
            }
            if (checkNotNullEmpty(ihu)) {
                urlStr = urlStr + "&ihu=" + ihu;
            }
            return apiService.get(urlStr);
        },
        deleteMaterials: function (materials) {
            return apiService.post("'" + materials + "'", '/s2/api/materials/delete/');
        },
        createMaterial: function (material) {
            return apiService.post(material, '/s2/api/materials/create');
        },
        update: function (material) {
            return apiService.post(material, "/s2/api/materials/update");
        }
    }
}]);
