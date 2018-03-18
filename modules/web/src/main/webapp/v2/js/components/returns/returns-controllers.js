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


logistimoApp.controller('CreateReturnsController', CreateReturnsController);
logistimoApp.controller('ReturnsDetailController', ReturnsDetailController);

CreateReturnsController.$inject = ['$scope','$location', 'returnsService','trnService'];
ReturnsDetailController.$inject = ['$scope', 'requestContext', 'returnsService'];

function CreateReturnsController($scope, $location, returnsService, trnService) {

    const SOURCE_WEB = 1;

    $scope.returnItems = returnsService.getItems();
    $scope.returnOrder = returnsService.getOrder();

    $scope.showLoading();
    trnService.getReasons('ro')
        .then(function (data) {
            $scope.reasons = [""].concat(data.data.rsns);
            $scope.defaultReason = data.data.defRsn;
        })
        .then(function () {
            angular.forEach($scope.returnItems, function (returnItem) {
                if (returnItem.materialTags) {
                    trnService.getReasons('ro', returnItem.materialTags).then(function (data) {
                        returnItem.reasons = [""].concat(data.data.rsns);
                        returnItem.returnReason = data.data.defRsn;
                        angular.forEach(returnItem.returnBatches, function (returnBatch) {
                            returnBatch.returnReason = angular.copy(returnItem.returnReason);
                        });
                    });
                } else {
                    returnItem.reasons = angular.copy($scope.reasons);
                    returnItem.returnReason = angular.copy($scope.defaultReason);
                    angular.forEach(returnItem.returnBatches, function (returnBatch) {
                        returnBatch.returnReason = angular.copy(returnItem.returnReason);
                    });
                }
            })
        }).catch(function error(msg) {
            $scope.showErrorMsg(msg);
        }).finally(function () {
            $scope.hideLoading();
        });

    $scope.cancel = function() {
        $scope.setPageSelection('orderDetail');
        $scope.enableScroll();
    };

    $scope.create = function() {
        if(isReturnValid('returnQuantity')) {
            if(isReturnValid('returnReason')) {
                if(!$scope.transConfig.rosm || isReturnValid('returnMaterialStatus')) {
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
            } else {
                $scope.showWarning("Reason is mandatory for all materials being returned")
            }
        } else {
            $scope.showWarning("Please specify return quantity for all materials")
        }
    };

    function isReturnValid(field) {
        return $scope.returnItems.every(function (returnItem) {
            if (checkNotNullEmpty(returnItem.returnBatches)) {
                return returnItem.returnBatches.some(function (returnBatch) {
                    return checkNotNullEmpty(returnBatch[field]);
                });
            } else {
                return checkNotNullEmpty(returnItem[field]);
            }
        });
    }

    function getCreateRequest() {
        var items = [];
        angular.forEach($scope.returnItems, function(returnItem) {
            var item = {
                material_id : returnItem.id,
                return_quantity : returnItem.returnQuantity,
                material_status : returnItem.returnMaterialStatus,
                reason : returnItem.returnReason
            };
            if(returnItem.returnBatches) {
                item.batches = [];
                var totalReturnQuantity = 0;
                angular.forEach(returnItem.returnBatches, function(returnBatch){
                    if(checkNotNullEmpty(returnBatch.returnQuantity)) {
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
            order_id : $scope.order.id,
            comment : $scope.comment,
            items : items,
            source : SOURCE_WEB
        }
    }
}

function ReturnsDetailController($scope, requestContext, returnsService) {
    $scope.page = 'detail';
    $scope.subPage = 'consignment';

    var returnId = requestContext.getParam("returnId");
    $scope.showLoading();
    returnsService.get(returnId).then(function(data){
        $scope.returns = data.data;
    }).catch(function error(msg) {
        $scope.showErrorMsg(msg);
    }).finally(function () {
        $scope.hideLoading();
    });
}

