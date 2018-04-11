/*
 * Copyright © 2018 Logistimo.
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
 * Created by naveensnair on 22/03/18.
 */

var stockRebalancingServices = angular.module('stockRebalancingServices', []);
stockRebalancingServices.factory('stockRebalancingService', ['APIService', 'ordService', function (apiService, orderService) {
    return {
        getRecommendedTransfers: function(entityId, materialId, reason, entityTag, materialTag, size, offset) {
            offset = typeof offset !== 'undefined' ? offset : 0;
            size = typeof size !== 'undefined' ? size : 50;
            var url = '/s2/api/stock-rebalancing/events?offset=' + offset + '&size=' + size;
            if (checkNotNullEmpty(entityId)) {
                url += '&entity_id=' + entityId;
            }
            if (checkNotNullEmpty(materialId)) {
                url += '&material_id=' + materialId;
            }
            if (checkNotNullEmpty(reason)) {
                url += '&event_type=' + reason;
            }
            if (checkNotNullEmpty(entityTag)) {
                url += '&entity_tag=' + entityTag;
            }
            if (checkNotNullEmpty(materialTag)) {
                url += '&material_tag=' + materialTag;
            }


            return apiService.get(url);
        },
        getRecommendations: function(eventId) {
                return apiService.get('/s2/api/stock-rebalancing/events/' + eventId + '/recommendations');
        },

        createTransfer: function (recommendationId) {
            return apiService.post({"recommendation_id" : recommendationId}, '/s2/api/stock-rebalancing/create-transfer');
        },
        addToExistingTransfer: function(recommendationId, transferId) {
            return apiService.post({"recommendation_id": recommendationId, "transfer_id": transferId}, '/s2/api/stock-rebalancing/add-to-transfer');
        }
    }

}]);

