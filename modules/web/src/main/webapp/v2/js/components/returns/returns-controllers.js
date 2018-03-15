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

logistimoApp.controller('CreateReturnsController', ['$scope', 'returnsService',
    function ($scope, returnsService) {

        $scope.returnItems = returnsService.getItems();
        $scope.returnOrder = returnsService.getOrder();

        $scope.toggleBatch = function (type, item) {
            item[type] = !item[type];
        };

        $scope.cancel = function() {
            $scope.setPageSelection('orderDetail');
            $scope.enableScroll();
        };

        $scope.create = function() {
            if(isReturnValid()) {
                $scope.showLoading();
                returnsService.create(getCreateRequest()).then(function (data) {
                    $scope.showSuccess("Returns created successfully");
                    $location.path('/orders/returns/detail/' + data.data.return_id);
                }).catch(function error(msg) {
                    $scope.showErrorMsg(msg);
                }).finally(function () {
                    $scope.hideLoading();
                });
            }
        };

        function isReturnValid() {
            return $scope.returnItems.every(function (returnItem) {
                if (returnItem.returnBatches) {
                    return $scope.returnItems.returnBatches.some(function (returnBatch) {
                        return returnBatch.returnQuantity > 0;
                    });
                } else {
                    return returnItem.returnQuantity > 0;
                }
            });
        }

        function getCreateRequest() {
            var items = [];
            angular.forEach($scope.returnItems, function(returnItem) {
                var item = {
                    material_id : returnItem.id,
                    return_quantity : returnItem.returnQuantity,
                    material_status : returnItem.newStataus,
                    reason : returnItem.reason
                };
                if(returnItem.returnBatches) {
                    item.batches = [];
                    var totalReturnQuantity = 0;
                    angular.forEach(returnItem.returnBatches, function(returnBatch){
                        item.batches.push({
                            batch_id : returnBatch.id,
                            return_quantity : returnBatch.returnQuantity,
                            material_status : returnBatch.newStataus,
                            reason : returnBatch.reason
                        });
                        totalReturnQuantity += returnBatch.returnQuantity;
                    });
                    item.return_quantity = totalReturnQuantity;
                }
                items.push(item);
            });

            return {
                order_id : $scope.order.id,
                comment : $scope.comment,
                items : items,
                source : "web"
            }
        }
    }
]);

