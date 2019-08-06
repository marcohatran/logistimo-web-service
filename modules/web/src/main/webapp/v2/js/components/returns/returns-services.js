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
 * Created by Mohan Raja on 11/03/18.
 */

logistimoApp.service('returnsService', ['APIService', function (apiService) {
    let _items = [];
    let _order = {};

    const SOURCE_WEB = 1;

    this.setItems =  items => _items = items;

    this.getItems = () => {
        let tItems = _items;
        _items = [];
        return tItems;
    };

    this.setOrder =  order => _order = order;

    this.getOrder = () => {
        let tOrder = _order;
        _order = {};
        return tOrder;
    };

    let getTrackingDetails = returns => {
        if(returns.tracking_details) {
            let tracking_details = {};
            tracking_details.transporter = returns.tracking_details.transporter || undefined;
            tracking_details.tracking_id = returns.tracking_details.trackingId || undefined;
            tracking_details.estimated_arrival_date = formatDate(returns.tracking_details.estimatedArrivalDate) || undefined;
            tracking_details.required_on = formatDate(returns.tracking_details.requiredOn) || undefined;
            let isValid = Object.values(tracking_details).some(d => {
                return checkNotNullEmpty(d);
            });
            return isValid ? tracking_details : undefined;
        }
    };

    let getCreateRequest = returns => {
        let items = [];
        angular.forEach(returns.returnItems, returnItem => {
            let item = {
                material_id: returnItem.id,
                return_quantity: returnItem.returnQuantity,
                material_status: returnItem.returnMaterialStatus,
                reason: returnItem.returnReason
            };
            if (checkNotNullEmpty(returnItem.returnBatches)) {
                item.batches = [];
                let totalReturnQuantity = 0;
                angular.forEach(returnItem.returnBatches, returnBatch => {
                    if (checkNotNullEmpty(returnBatch.returnQuantity)) {
                        item.batches.push({
                            batch_id: returnBatch.id,
                            return_quantity: returnBatch.returnQuantity,
                            material_status: returnBatch.returnMaterialStatus,
                            reason: returnBatch.returnReason
                        });
                        totalReturnQuantity += returnBatch.returnQuantity * 1;
                    }
                });
                item.return_quantity = totalReturnQuantity;
            }
            items.push(item);
        });

        return {
            order_id: returns.order_id,
            comment: returns.comment,
            items,
            source: SOURCE_WEB,
            tracking_details: getTrackingDetails(returns)
        }
    };

    this.create = data => {
        return apiService.post(getCreateRequest(data), '/s2/api/returns');
    };

    this.get = id => {
        return apiService.get(`/s2/api/returns/${id}`);
    };

    this.getAll = filters => {
        let filterArray = [];
        for (let filter in filters) {
            if (filters.hasOwnProperty(filter) && checkNotNullEmpty(filters[filter])) {
                filterArray.push(`${filter}=${filters[filter]}`);
            }
        }
        return apiService.get(`/s2/api/returns${filterArray.length > 0 ? '?' + filterArray.join("&") : ''}`);
    };

    let getShipRequest = returns => {
        return {
            comment: returns.comment,
            tracking_details: getTrackingDetails(returns),
            updated_time: returns.updated_time
        }
    };

    this.ship = (id, data) => {
        data.source = SOURCE_WEB;
        return apiService.post(getShipRequest(data), `/s2/api/returns/${id}/ship`);
    };

    this.receive = (id, data) => {
        data.source = SOURCE_WEB;
        return apiService.post(data, `/s2/api/returns/${id}/receive`);
    };

    this.cancel = (id, data) => {
        data.source = SOURCE_WEB;
        return apiService.post(data, `/s2/api/returns/${id}/cancel`);
    };

    let getUpdateTrackingDetailsRequest = trackingDetails => {
        return {
            transporter: trackingDetails.transporter,
            tracking_id: trackingDetails.tracking_id,
            estimated_arrival_date: formatDate(trackingDetails.ead) || undefined,
            required_on: formatDate(trackingDetails.ron) || undefined
        }
    };

    this.updateTrackingDetails = (id, data) => {
        return apiService.post(getUpdateTrackingDetailsRequest(data), `/s2/api/returns/${id}/tracking-details`);
    };

    let getUpdateItemRequest = (returnItems, updated_time) => {
        angular.forEach(returnItems, returnItem => {
            if(checkNotNullEmpty(returnItem.new_return_quantity)) {
                returnItem.return_quantity = returnItem.new_return_quantity || returnItem.return_quantity;
            }
            if(checkNotNullEmpty(returnItem.batches)) {
                angular.forEach(returnItem.batches, returnBatch => {
                    if (checkNotNullEmpty(returnBatch.new_return_quantity)) {
                        returnBatch.return_quantity = returnBatch.new_return_quantity || returnBatch.return_quantity;
                    }
                });
            }
        });
        return {items: returnItems, updated_time};
    };

    this.updateItems = (id, data, updated_time) => {
        return apiService.post(getUpdateItemRequest(data, updated_time), `/s2/api/returns/${id}/update-items`);
    };

    this.getQuantityByOrder = (orderId) => {
        return apiService.get(`/s2/api/returns/order/${orderId}`);
    };

}]);
