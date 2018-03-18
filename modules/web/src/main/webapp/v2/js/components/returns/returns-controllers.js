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
logistimoApp.controller('DetailReturnsController', DetailReturnsController);

CreateReturnsController.$inject = ['$scope','$location', 'returnsService','trnService'];
DetailReturnsController.$inject = ['$scope', 'requestContext', 'RETURNS', 'returnsService', 'conversationService', 'activityService'];

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
                } else {
                    $scope.showWarning("Material status is mandatory for all materials being returned")
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
            if(checkNotNullEmpty(returnItem.returnBatches)) {
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

function DetailReturnsController($scope, requestContext, RETURNS, returnsService, conversationService, activityService) {
    $scope.RETURNS = RETURNS;
    $scope.page = 'detail';
    $scope.subPage = 'consignment';

    var returnId = requestContext.getParam("returnId");

    $scope.showLoading();
    returnsService.get(returnId)
        .then(function (data) {
            $scope.returns = data.data;
        })
        .then(getMessageCount)
        .then(getStatusHistory)
        .then(checkStatusList)
        .catch(function error(msg) {
            $scope.showErrorMsg(msg);
        }).finally(function () {
            $scope.hideLoading();
        });

    function getMessageCount() {
        conversationService.getMessagesByObj('RETURNS', returnId, 0, 1, true).then(function (data) {
            if (checkNotNullEmpty(data.data)) {
                $scope.messageCount = data.data.numFound;
            }
        })
    }

    $scope.setMessageCount = function (count) {
        $scope.messageCount = count;
    };

    function getStatusHistory() {
        activityService.getStatusHistory(returnId, 'RETURNS', null).then(function (data) {
            if (checkNotNullEmpty(data.data) && checkNotNullEmpty(data.data.results)) {
                $scope.history = data.data.results;
                var hMap = {};
                var pVal;
                $scope.history.forEach(function (data) {
                    if (checkNullEmpty(hMap[data.newValue])) {
                        hMap[data.newValue] = {
                            "status": RETURNS.statusLabel[data.newValue],
                            "updatedon": data.createDate,
                            "updatedby": data.userName,
                            "updatedId": data.userId
                        };
                        if (RETURNS.status.CANCELLED == data.newValue) {
                            pVal = data.prevValue;
                        }
                    }
                });
                $scope.si = [];
                var end = false;
                var siInd = 0;

                function constructStatus(stCode, stText) {
                    if ($scope.returns.status.status != RETURNS.status.CANCELLED || !end) {
                        $scope.si[siInd] = (!end && hMap[stCode]) ? hMap[stCode] : {
                            "status": stText,
                            "updatedon": "",
                            "updatedby": ""
                        };
                        $scope.si[siInd].completed = $scope.returns.status.status == stCode ? "end" : (end ? "false" : "true");
                        siInd += 1;
                    }
                    if (!end) {
                        end = $scope.returns.status.status == stCode || ($scope.returns.status.status == RETURNS.status.CANCELLED && pVal == stCode);
                    }
                }

                constructStatus(RETURNS.status.OPEN, RETURNS.statusLabel[RETURNS.status.OPEN]);
                constructStatus(RETURNS.status.SHIPPED, RETURNS.statusLabel[RETURNS.status.SHIPPED]);
                constructStatus(RETURNS.status.RECEIVED, RETURNS.statusLabel[RETURNS.status.RECEIVED]);
                if ($scope.returns.status.status == RETURNS.status.CANCELLED) {
                    $scope.si[siInd] = hMap[RETURNS.status.CANCELLED];
                    $scope.si[siInd].completed = "cancel";
                }
            }
        });
    }

    $scope.toggleStatusHistory = function () {
        $scope.displayStatusHistory = !$scope.displayStatusHistory;
    };

    function checkStatusList() {
        switch ($scope.returns.status.status) {
            case RETURNS.status.OPEN:
                if ($scope.returns.customer.has_access) {
                    $scope.statusList = [RETURNS.status.SHIPPED];
                }
                $scope.statusList.push(RETURNS.status.CANCELLED);
                break;
            case RETURNS.status.SHIPPED:
                $scope.statusList = [];
                if ($scope.returns.vendor.has_access) {
                    $scope.statusList.push(RETURNS.status.RECEIVED);
                }
                $scope.statusList.push(RETURNS.status.CANCELLED);
                break;
            default:
                $scope.statusList = [];
        }
    }

}

