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
logistimoApp.controller('ReceiveReturnsController', ReceiveReturnsController);
logistimoApp.controller('ListReturnsController', ListReturnsController);
logistimoApp.controller('BatchReceiveReturnsController', BatchReceiveReturnsController);
logistimoApp.controller('BatchDetailReturnsController', BatchDetailReturnsController);
logistimoApp.controller('BatchCreateReturnsController', BatchCreateReturnsController);

function ReturnsItemInitializer($scope, trnService, $q) {

    $scope.orderByMaterial = new Map($scope.orderItems.map(d=>[d.id, d]));

    let allPromiseCollection = [];

    $scope.invalidPopup = 0;

    let getReasonMandatoryPromise = () => {
        let deferred = $q.defer();
        $scope.showLoading();
        trnService.getTransactionTypesWithReasonMandatory().then(data => {
            $scope.returnOutgoingReasonMandatory = data.data.includes('ro');
            deferred.resolve();
        }).catch(msg => deferred.reject(msg))
            .finally(() => $scope.hideLoading());
        return deferred.promise;
    };

    allPromiseCollection.push(getReasonMandatoryPromise());

    let getMaterialStatusPromise = () => {
        let deferred = $q.defer();
        $scope.showLoading();
        trnService.getMatStatus("ro", false).then(data => {
            $scope.matstatus = data.data;
            deferred.resolve();
        }).catch(msg => deferred.reject(msg))
            .finally(() => $scope.hideLoading());
        return deferred.promise;
    };

    allPromiseCollection.push(getMaterialStatusPromise());

    let getTemperatureMaterialStatusPromise = () => {
        let deferred = $q.defer();
        $scope.showLoading();
        trnService.getMatStatus("ro", true).then(data => {
            $scope.tempmatstatus = data.data;
            deferred.resolve();
        }).catch(msg => deferred.reject(msg))
            .finally(() => $scope.hideLoading());
        return deferred.promise;
    };

    allPromiseCollection.push(getTemperatureMaterialStatusPromise());

    let getMaterialStatusIncomingPromise = () => {
        let deferred = $q.defer();
        $scope.showLoading();
        trnService.getMatStatus("ri", false).then(data => {
            $scope.incomingMatstatus = data.data;
            deferred.resolve();
        }).catch(msg => deferred.reject(msg))
            .finally(() => $scope.hideLoading());
        return deferred.promise;
    };

    allPromiseCollection.push(getMaterialStatusIncomingPromise());

    let getTemperatureMaterialStatusIncomingPromise = () => {
        let deferred = $q.defer();
        $scope.showLoading();
        trnService.getMatStatus("ri", true).then(data => {
            $scope.incomingTempmatstatus = data.data;
            deferred.resolve();
        }).catch(msg => deferred.reject(msg))
            .finally(() => $scope.hideLoading());
        return deferred.promise;
    };

    allPromiseCollection.push(getTemperatureMaterialStatusIncomingPromise());

    let getStatusMandatoryPromise = () => {
        let deferred = $q.defer();
        $scope.showLoading();
        trnService.getStatusMandatory().then(data => {
            $scope.statusMandatoryConfig = data.data;
            deferred.resolve();
        }).catch(msg => deferred.reject(msg))
            .finally(() => $scope.hideLoading());
        return deferred.promise;
    };

    allPromiseCollection.push(getStatusMandatoryPromise());

    //During create return, replace the return item, with the order item updated with reasons
    let updateReturnItemWithReasons = (orderItem) => {
        $scope.returnItems.some((returnItem, index) => {
            if(returnItem.nm == orderItem.nm) {
                $scope.returnItems.splice(index, 1, angular.copy(orderItem));
                return true;
            }
        });
    };

    let setCommonReasons = item => {
        item.reasons = angular.copy($scope.reasons);
        item.returnReason = angular.copy($scope.defaultReason);
        item.defaultReturnReason = angular.copy($scope.defaultReason);
        angular.forEach(item.returnBatches, returnBatch => {
            returnBatch.returnReason = angular.copy(item.returnReason);
            returnBatch.defaultReturnReason = angular.copy(item.returnReason);
        });
    };

    let initItemReasons = () => {
        let getReasonPromise = (orderItem) => {
            let reasonDeferred = $q.defer();
            $scope.showLoading();
            trnService.getReasons('ro', orderItem.materialTags).then(data => {
                if (checkNotNullEmpty(data.data) && checkNotNullEmpty(data.data.rsns)) {
                    orderItem.returnReason = angular.copy(data.data.defRsn);
                    orderItem.defaultReturnReason = angular.copy(data.data.defRsn);
                    orderItem.reasons = [""].concat(data.data.rsns);
                    angular.forEach(orderItem.returnBatches, returnBatch => {
                        returnBatch.returnReason = angular.copy(orderItem.returnReason);
                        returnBatch.defaultReturnReason = angular.copy(orderItem.returnReason);
                    });
                } else {
                    setCommonReasons(orderItem);
                }
                updateReturnItemWithReasons(orderItem);
                reasonDeferred.resolve();
            }).catch(msg => reasonDeferred.reject(msg))
                .finally(() => $scope.hideLoading());
            return reasonDeferred.promise;
        };

        let reasonPromises = [];
        angular.forEach($scope.orderItems, orderItem => {
            if (orderItem.materialTags) {
                reasonPromises.push(getReasonPromise(orderItem))
            } else {
                setCommonReasons(orderItem);
                updateReturnItemWithReasons(orderItem);
            }
        });
        return $q.all(reasonPromises)
    };

    let getReasonsPromise = () => {
        let deferred = $q.defer();
        $scope.showLoading();
        trnService.getReasons('ro').then(data => {
            $scope.defaultReason = data.data.defRsn;
            $scope.reasons = [""].concat(data.data.rsns);
        }).then(
            initItemReasons().then(() => deferred.resolve()).catch(msg => deferred.reject(msg))
        ).catch(msg => deferred.reject(msg))
            .finally(() => $scope.hideLoading());
        return deferred.promise;
    };

    allPromiseCollection.push(getReasonsPromise());

    return $q.all(allPromiseCollection);
}

function ReturnsItemValidator($scope, $timeout) {

    $scope.validateQuantityReturn = (material, index, isReceive) => {
        material.returnQuantity = isReceive ? material.received.received_quantity : material.new_return_quantity;
        return $scope.validateQuantity(material, index, true, isReceive);
    };

    $scope.validateQuantityCreateReturn = (material, index) => {
        let originalQuantity = material.returnedQuantity;
        material.returnedQuantity = (material.returnedQuantity || 0) * 1 + (material.requested_return_quantity || 0) * 1;
        let status = $scope.validateQuantity(material, index, true);
        material.returnedQuantity = originalQuantity;
        return status;
    };

    $scope.validateQuantity = (material, index, isReturn, isReceive) => {
        let redraw = material.displayMeta != material.returnQuantity > 0;
        material.displayMeta = material.returnQuantity > 0;
        let isInvalid = false;
        if (material.returnQuantity > 0) {
            if (material.returnQuantity > material.fq && !isReceive) {
                showPopup($scope, material, material.id, messageFormat($scope.resourceBundle['return.quantity.cannot.exceed.received.quantity'], material.returnQuantity, material.fq), index, $timeout);
                isInvalid = true;
            } else if (material.returnQuantity > material.fq - material.returnedQuantity && !isReceive) {
                showPopup($scope, material, material.id,
                    messageFormat($scope.resourceBundle['return.quantity.cannot.exceed.remaining.return.quantity'],
                        material.returnedQuantity, material.fq, (material.fq - material.returnedQuantity)), index, $timeout);
                isInvalid = true;
            } else if (checkNotNullEmpty(material.huName) && checkNotNullEmpty(material.huQty) &&
                material.returnQuantity % material.huQty != 0) {
                showPopup($scope, material, material.id, messageFormat($scope.resourceBundle['return.quantity.handling.units.mismatch'], material.returnQuantity, material.nm, material.huName, material.huQty, material.nm),
                    index, $timeout);
                isInvalid = true;
            }
        } else {
            if (material.rinvalidPopup) {
                hidePopup($scope, material, `r${material.id}`, index, $timeout, false, false, true);
            }
            if (material.sinvalidPopup) {
                hidePopup($scope, material, `s${material.id}`, index, $timeout, false, true);
            }
            material.sinvalidPopup = material.sPopupMsg = material.rinvalidPopup = material.rPopupMsg = undefined;
            material.returnReason = angular.copy(material.defaultReturnReason);
            if (isReturn) {
                material.reason = material.returnReason;
            }
        }
        if (redraw) {
            ReturnsValidator.redrawAllPopup($scope, $timeout);
        }
        return isInvalid;
    };

    $scope.validateBatchQuantityReturn = (material, batchMaterial, index, isReceive) => {
        batchMaterial.returnQuantity = isReceive ? batchMaterial.received.received_quantity : batchMaterial.new_return_quantity;
        return $scope.validateBatchQuantity(material, batchMaterial, index, true, isReceive);
    };

    $scope.validateBatchQuantityCreateReturn = (material, batchMaterial, index) => {
        let originalQuantity = batchMaterial.returnedQuantity;
        batchMaterial.returnedQuantity = (batchMaterial.returnedQuantity || 0) * 1 + (batchMaterial.requested_return_quantity || 0) * 1;
        let status = $scope.validateBatchQuantity(material, batchMaterial, index, true);
        batchMaterial.returnedQuantity = originalQuantity;
        return status;
    };

    $scope.validateBatchQuantity = (material, batchMaterial, index, isReturn, isReceive) => {
        let redraw = batchMaterial.displayMeta != batchMaterial.returnQuantity > 0;

        batchMaterial.displayMeta = batchMaterial.returnQuantity > 0;
        let isInvalid = false;
        if (batchMaterial.returnQuantity > 0) {
            if (batchMaterial.returnQuantity > batchMaterial.fq && !isReceive) {
                showPopup($scope, batchMaterial, material.id + batchMaterial.id, messageFormat(
                        $scope.resourceBundle['return.quantity.cannot.exceed.received.quantity'], batchMaterial.returnQuantity, batchMaterial.fq),
                    index, $timeout);
                isInvalid = true;
            } else if (batchMaterial.returnQuantity > batchMaterial.fq - batchMaterial.returnedQuantity && !isReceive) {
                showPopup($scope, batchMaterial, material.id + batchMaterial.id, messageFormat(
                        $scope.resourceBundle['return.quantity.cannot.exceed.remaining.return.quantity'],
                        batchMaterial.returnedQuantity, batchMaterial.fq, (batchMaterial.fq - batchMaterial.returnedQuantity)),
                    index, $timeout);
                isInvalid = true;
            } else if (checkNotNullEmpty(material.huName) && checkNotNullEmpty(material.huQty) &&
                batchMaterial.returnQuantity % material.huQty != 0) {
                showPopup($scope, batchMaterial, material.id + batchMaterial.id, messageFormat($scope.resourceBundle['return.quantity.handling.units.mismatch'], batchMaterial.returnQuantity, material.nm, material.huName, material.huQty, material.nm),
                    index, $timeout);
                isInvalid = true;
            }
        } else {
            if (batchMaterial.rinvalidPopup) {
                hidePopup($scope, batchMaterial, `r${material.id}${batchMaterial.id}`, index, $timeout, false, false, true);
            }
            if (batchMaterial.sinvalidPopup) {
                hidePopup($scope, batchMaterial, `s${material.id}${batchMaterial.id}`, index, $timeout, false, true);
            }
            batchMaterial.sinvalidPopup = batchMaterial.sPopupMsg = batchMaterial.rinvalidPopup = batchMaterial.rPopupMsg = undefined;
            batchMaterial.returnReason = angular.copy(batchMaterial.defaultReturnReason);
            if (isReturn) {
                batchMaterial.reason = batchMaterial.returnReason;
            }
        }
        if (redraw) {
            ReturnsValidator.redrawAllPopup($scope, $timeout);
        }
        return isInvalid;
    };

    $scope.validateReasonReturn = (material, index) => {
        material.returnReason = material.reason;
        return $scope.validateReason(material, index);
    };

    $scope.validateReason = (material, index) => {
        if ($scope.returnOutgoingReasonMandatory && checkNullEmpty(material.returnReason)) {
            showPopup($scope, material, `r${material.id}`, $scope.resourceBundle['reason.required'], index, $timeout, false, false, true);
            return true;
        }
    };

    $scope.validateBatchReasonReturn = (material, batchMaterial, index) => {
        batchMaterial.returnReason = batchMaterial.reason;
        return $scope.validateBatchReason(material, batchMaterial, index);
    };

    $scope.validateBatchReason = (material, batchMaterial, index) => {
        if ($scope.returnOutgoingReasonMandatory && checkNullEmpty(batchMaterial.returnReason)) {
            showPopup($scope, batchMaterial, `r${material.id}${batchMaterial.id}`, $scope.resourceBundle['reason.required'], index, $timeout, false, false, true);
            return true;
        }
    };

    $scope.validateStatusReturn = (material, index, isTempStatus, isReceive) => {
        material.returnMaterialStatus = isReceive ? material.received.material_status : material.material_status;
        return $scope.validateStatus(material, index, isTempStatus, isReceive);
    };

    $scope.validateStatus = (material, index, isTempStatus, isIncoming) => {
        let mandatory = isIncoming ? $scope.statusMandatoryConfig.rism : $scope.statusMandatoryConfig.rosm;
        let isStatusDefined;
        if (isTempStatus) {
            isStatusDefined = checkNotNullEmpty($scope.tempmatstatus);
        } else {
            isStatusDefined = checkNotNullEmpty($scope.matstatus);
        }
        if (mandatory && isStatusDefined && checkNullEmpty(material.returnMaterialStatus)) {
            showPopup($scope, material, `s${isTempStatus ? 't' : ''}${material.id}`, $scope.resourceBundle['status.required'], index, $timeout, false, true, false);
            return true;
        }
    };

    $scope.validateBatchStatusReturn = (material, batchMaterial, index, isTempStatus, isReceive) => {
        material.id = material.material_id;
        batchMaterial.returnMaterialStatus = isReceive ? batchMaterial.received.material_status : batchMaterial.material_status;
        return $scope.validateBatchStatus(material, batchMaterial, index, isTempStatus, isReceive);
    };

    $scope.validateBatchStatus = (material, batchMaterial, index, isTempStatus, isIncoming) => {
        let mandatory = isIncoming ? $scope.statusMandatoryConfig.rism : $scope.statusMandatoryConfig.rosm;
        if (mandatory && checkNullEmpty(batchMaterial.returnMaterialStatus)) {
            showPopup($scope, batchMaterial, `s${isTempStatus ? 't' : ''}${material.id}${batchMaterial.id}`, $scope.resourceBundle['status.required'], index, $timeout, false, true, false);
            return true;
        }
    };

    $scope.closePopup = (material, index, prefix) =>
        hidePopup($scope, material, (prefix ? prefix : '') + material.id, index, $timeout, false, prefix == 's' || prefix == 'st', prefix == 'r');

    $scope.closeBatchPopup = (material, batchMaterial, index, prefix) =>
        hidePopup($scope, batchMaterial, (prefix ? prefix : '') + material.id + batchMaterial.id, index, $timeout, false, prefix == 's' || prefix == 'st', prefix == 'r');
}

function ReturnsItemManager($scope, $timeout) {

    ReturnsItemValidator($scope, $timeout);

    let updateAvailableReturnItems = () => {
        $scope.availableReturnItems = [];
        if ($scope.orderItems.length == $scope.returnItems.length) {
            return;
        }
        angular.forEach($scope.orderItems, orderItem => {
            let found = $scope.returnItems.some(
                    returnItem => orderItem.id == returnItem.id || orderItem.id == returnItem.material_id
            );
            if (!found) {
                if (orderItem.fq > (orderItem.returnedQuantity || 0)) {
                    $scope.availableReturnItems.push(orderItem);
                }
            }
        });
    };
    updateAvailableReturnItems();

    $scope.deleteReturnItem = index => {
        $scope.returnItems.splice(index, 1);
        updateAvailableReturnItems();
        if ($scope.invalidPopup > 0) {
            ReturnsValidator.redrawAllPopup($scope, $timeout);
        }
    };

    $scope.addReturnItem = newItem => {
        $scope.returnItems.push(angular.copy(newItem));
        updateAvailableReturnItems();
    };
}

var ReturnsValidator = {};
ReturnsValidator.isAllValid = $scope => {
    if (ReturnsValidator.isReturnQuantityAdded($scope, false)) {
        if (ReturnsValidator.isReturnValid($scope, 'returnReason')) {
            if (!$scope.statusMandatoryConfig.rosm || ReturnsValidator.isReturnValid($scope, 'returnMaterialStatus')) {
                return true;
            } else {
                $scope.showWarning($scope.resourceBundle['return.material.status.required']);
            }
        } else {
            $scope.showWarning($scope.resourceBundle['return.reason.required']);
        }
    } else {
        $scope.showWarning($scope.resourceBundle['return.specify.quantity']);
    }
};
ReturnsValidator.isReturnQuantityAdded = ($scope, isReturn) => {
    return $scope.returnItems.every(returnItem => {
        let batches = isReturn ? returnItem.batches : returnItem.returnBatches;
        if (checkNotNullEmpty(batches)) {
            return batches.some(returnBatch =>
                (isReturn ? returnBatch.new_return_quantity : returnBatch.returnQuantity) > 0
            );
        } else {
            return (isReturn ? returnItem.new_return_quantity : returnItem.returnQuantity) > 0;
        }
    });
};
ReturnsValidator.isReturnValid = ($scope, field) => {
    return !$scope.returnItems.some((returnItem, index) => {
        if (checkNotNullEmpty(returnItem.returnBatches)) {
            return returnItem.returnBatches.some((returnBatch, index)  => {
                if (returnBatch.returnQuantity > 0) {
                    if (field == 'returnReason') {
                        return checkNotNullEmpty(returnItem.reasons) && returnItem.reasons.length > 1 &&
                            $scope.validateBatchReason(returnItem, returnBatch, index);
                    } else if (field == 'returnMaterialStatus') {
                        if (returnItem.tm) {
                            return checkNotNullEmpty($scope.tempmatstatus) &&
                                $scope.validateBatchStatus(returnItem, returnBatch, index, true);
                        } else {
                            return checkNotNullEmpty($scope.matstatus) &&
                                $scope.validateBatchStatus(returnItem, returnBatch, index);
                        }
                    }
                }
            });
        } else {
            if (field == 'returnReason') {
                return checkNotNullEmpty(returnItem.reasons) && returnItem.reasons.length > 1 &&
                    $scope.validateReason(returnItem, index);
            } else if (field == 'returnMaterialStatus') {
                if (returnItem.tm) {
                    return checkNotNullEmpty($scope.tempmatstatus) && $scope.validateStatus(returnItem, index, true);
                } else {
                    return checkNotNullEmpty($scope.matstatus) && $scope.validateStatus(returnItem, index);
                }
            }
        }
    });
};
ReturnsValidator.redrawAllPopup = ($scope, $timeout, type) => {
    $scope.returnItems.forEach((returnItem, index) => {
        if (checkNotNullEmpty(returnItem.returnBatches)) {
            returnItem.returnBatches.forEach((returnBatch, index) => {
                if (returnBatch.popupMsg) {
                    type == 'show' ?
                        $scope.validateBatchQuantity(returnItem, returnBatch, index) :
                        hidePopup($scope, returnBatch, returnItem.id + returnBatch.id, index, $timeout);
                }
                if (returnBatch.sPopupMsg) {
                    type == 'show' ?
                        $scope.validateBatchStatus(returnItem, returnBatch, index, returnItem.tm) :
                        hidePopup($scope, returnBatch, `s${returnItem.tm ? 't' : ''}${returnItem.id}${returnBatch.id}`, index, $timeout, false, true);
                }
                if (returnBatch.rPopupMsg) {
                    type == 'show' ?
                        $scope.validateBatchReason(returnItem, returnBatch, index) :
                        hidePopup($scope, returnBatch, `r${returnItem.id}${returnBatch.id}`, index, $timeout, false, false, true);
                }
            });
        } else {
            if (returnItem.popupMsg) {
                type == 'show' ?
                    $scope.validateQuantity(returnItem, index) :
                    hidePopup($scope, returnItem, returnItem.id, index, $timeout);
            }
            if (returnItem.sPopupMsg) {
                type == 'show' ?
                    $scope.validateStatus(returnItem, index, returnItem.tm) :
                    hidePopup($scope, returnItem, `s${returnItem.tm ? 't' : ''}${returnItem.id}`, index, $timeout);
            }
            if (returnItem.rPopupMsg) {
                type == 'show' ?
                    $scope.validateReason(returnItem, index) :
                    hidePopup($scope, returnItem, `r${returnItem.id}`, index, $timeout);
            }
        }
    });
    if (type != 'show') {
        $timeout(() => ReturnsValidator.redrawAllPopup($scope, $timeout, 'show'), 0);
    }
};

function CreateReturnsController($scope, $location, $timeout, $q, returnsService, trnService) {

    $scope.returnItems = returnsService.getItems();
    $scope.returnOrder = returnsService.getOrder();
    $scope.orderItems = angular.copy($scope.returnOrder.its);
    $scope.trackingDetails = {};

    let isReturnBatchAvailable = item =>
        item.returnBatches && item.returnBatches.length > 0 && item.returnBatches.some(b => b.returnQuantity > 0);

    let updateReturnItems = () => {
        let orderItemByMaterial = new Map($scope.orderItems.map(d=>[d.id, d]));
        $scope.returnItems = $scope.returnItems.map(i => orderItemByMaterial.get(i.id));
        $scope.returnItems.forEach(returnItem => returnItem.returnBatchAvailable = isReturnBatchAvailable(returnItem));
    };

    returnsService.getQuantityByOrder($scope.returnOrder.id).then(data=> {
        let quantityByMaterial = new Map(data.data.map(d=>[d.material_id, d]));
        angular.forEach($scope.orderItems, function (orderItem) {
            let materialQuantity = quantityByMaterial.get(orderItem.id);
            orderItem.requested_return_quantity = materialQuantity.requested_return_quantity;
            if (checkNotNullEmpty(materialQuantity.batches)) {
                let quantityByBatch = new Map(materialQuantity.batches.map(b=>[b.batch_id, b]));
                angular.forEach(orderItem.returnBatches, orderBatch => {
                    let batchQuantity = quantityByBatch.get(orderBatch.id);
                    orderBatch.requested_return_quantity = batchQuantity.requested_return_quantity;
                });
            }
        });
        updateReturnItems();
        ReturnsItemInitializer($scope, trnService, $q).catch(msg => $scope.showErrorMsg(msg));
        ReturnsItemManager($scope, $timeout);
    });

    $scope.cancel = () => {
        $scope.setPageSelection('orderDetail');
        $scope.enableScroll();
    };

    $scope.create = () => {
        if (ReturnsValidator.isAllValid($scope)) {
            $scope.showLoading();
            let request = {
                returnItems: $scope.returnItems,
                order_id: $scope.returnOrder.id,
                comment: $scope.comment,
                tracking_details: $scope.trackingDetails
            };
            returnsService.create(request).then(data => {
                $scope.showSuccess($scope.resourceBundle['return.creation.success']);
                $location.path(`/orders/returns/detail/${data.data.return_id}`);
            }).catch(msg => $scope.showErrorMsg(msg))
                .finally(() => $scope.hideLoading());
        }
    };

    $scope.toggleBatchItems = item => {
        item.additionalRows = !item.additionalRows;
        if (!item.additionalRows) {
            item.returnBatchAvailable = isReturnBatchAvailable(item)
        }
    };
}

function BatchCreateReturnsController($scope) {
    $scope.returnBatches = angular.copy($scope.item.returnBatches);
    sortByKey($scope.returnBatches, 'id');
    let isAllValid = () => {
        let isInvalid = $scope.returnBatches.some((batchItem, index) => {
            if (batchItem.returnQuantity > 0) {
                if ($scope.validateBatchQuantity($scope.item, batchItem, index)) {
                    return true;
                }
                if ($scope.item.tm) {
                    if (checkNotNullEmpty($scope.tempmatstatus)) {
                        return $scope.validateBatchStatus($scope.item, batchItem, index, true);
                    }
                } else {
                    if (checkNotNullEmpty($scope.matstatus)) {
                        return $scope.validateBatchStatus($scope.item, batchItem, index);
                    }
                }
            }
        });
        return !isInvalid;
    };

    $scope.save = () => {
        if (!isAllValid()) {
            return;
        }
        $scope.item.returnBatches = $scope.returnBatches;
        $scope.toggleBatchItems($scope.item);
    };

    $scope.cancel = () => {
        $scope.toggleBatchItems($scope.item);
    }
}

function DetailReturnsController($scope, $uibModal, $timeout, $q, requestContext, RETURNS, returnsService, conversationService,
                                 activityService, trnService, ordService, DATEFORMAT) {
    $scope.RETURNS = RETURNS;
    $scope.page = 'detail';
    $scope.subPage = 'consignment';
    $scope.today = new Date();

    const RETURN_ID = requestContext.getParam("returnId");

    let updateDateModels = () => {
        if (checkNotNullEmpty($scope.returns.tracking_details)) {
            $scope.returns.tracking_details.ead = string2Date($scope.returns.tracking_details.estimated_arrival_date, DATEFORMAT.DATE_FORMAT, '/');
        }
    };

    let getMessageCount = () => {
        $scope.showLoading();
        conversationService.getMessagesByObj('RETURNS', RETURN_ID, 0, 1, true).then(data => {
            if (checkNotNullEmpty(data.data)) {
                $scope.messageCount = data.data.numFound;
            }
        }).catch(msg => $scope.showErrorMsg(msg))
            .finally(() => $scope.hideLoading());
    };

    let getStatusHistory = () => {
        $scope.showLoading();
        activityService.getStatusHistory(RETURN_ID, 'RETURNS', null).then(data => {
            if (checkNotNullEmpty(data.data) && checkNotNullEmpty(data.data.results)) {
                $scope.history = data.data.results;
                let hMap = {};
                let pVal;
                $scope.history.forEach(data => {
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
                let end = false;
                let siInd = 0;

                let constructStatus = (stCode, stText) => {
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
                };

                constructStatus(RETURNS.status.OPEN, RETURNS.statusLabel[RETURNS.status.OPEN]);
                constructStatus(RETURNS.status.SHIPPED, RETURNS.statusLabel[RETURNS.status.SHIPPED]);
                constructStatus(RETURNS.status.RECEIVED, RETURNS.statusLabel[RETURNS.status.RECEIVED]);
                if ($scope.returns.status.status == RETURNS.status.CANCELLED) {
                    $scope.si[siInd] = hMap[RETURNS.status.CANCELLED];
                    $scope.si[siInd].completed = "cancel";
                }
            }
        }).catch(msg => $scope.showErrorMsg(msg))
            .finally(()  => $scope.hideLoading())
    };

    let checkStatusList = () => {
        if ($scope.dp.vp) {
            $scope.statusList = [];
            return;
        }
        switch ($scope.returns.status.status) {
            case RETURNS.status.OPEN:
                $scope.statusList = [];
                if ($scope.returns.customer.has_access) {
                    $scope.statusList.push(RETURNS.status.SHIPPED);
                }
                $scope.statusList.push(RETURNS.status.CANCELLED);
                break;
            case RETURNS.status.SHIPPED:
                $scope.statusList = [];
                if ($scope.returns.vendor.has_access) {
                    $scope.statusList.push(RETURNS.status.RECEIVED);
                }
                if ($scope.returns.customer.has_access) {
                    $scope.statusList.push(RETURNS.status.CANCELLED);
                }
                break;
            default:
                $scope.statusList = [];
        }
    };

    let getReturn = () => {
        $scope.showLoading();
        returnsService.get(RETURN_ID)
            .then(data => {
                $scope.returns = data.data;
                $scope.returnItems = $scope.returns.items;
                if (checkNullEmpty($scope.returns.tracking_details)) {
                    $scope.returns.tracking_details = {};
                }
                $scope.isItemInitialised = false;
                $scope.orderItems = undefined;
            })
            .then(() => {
                updateDateModels();
                getMessageCount();
                getStatusHistory();
                checkStatusList();
            })
            .catch(msg => $scope.showErrorMsg(msg))
            .finally(() => $scope.hideLoading());
    };
    getReturn();

    $scope.initOrderItems = () => {
        let deferred = $q.defer();
        if (checkNullEmpty($scope.orderItems)) {
            $scope.showLoading();
            ordService.getOrder($scope.returns.order_id).then(data => {
                $scope.orderItems = data.data.its;
                returnsService.getQuantityByOrder($scope.returns.order_id).then(data=> {
                    let quantityByMaterial = new Map(data.data.map(d=>[d.material_id, d]));
                    angular.forEach($scope.orderItems, orderItem => {
                        orderItem.isBatch = checkNotNullEmpty(orderItem.returnBatches);
                        const materialQuantity = quantityByMaterial.get(orderItem.id);
                        if (checkNotNullEmpty(materialQuantity)) {
                            orderItem.returnedQuantity = materialQuantity.returned_quantity;
                            orderItem.dispReturnedQuantity = angular.copy(materialQuantity.returned_quantity);
                            orderItem.total_return_quantity = materialQuantity.total_return_quantity;
                            orderItem.requested_return_quantity = materialQuantity.requested_return_quantity;
                            if (checkNotNullEmpty(materialQuantity.batches)) {
                                let quantityByBatch = new Map(materialQuantity.batches.map(b=>[b.batch_id, b]));
                                angular.forEach(orderItem.returnBatches, orderBatch => {
                                    const batchQuantity = quantityByBatch.get(orderBatch.id);
                                    orderBatch.returnedQuantity = batchQuantity.returned_quantity;
                                    orderBatch.dispReturnedQuantity = angular.copy(batchQuantity.returned_quantity);
                                    orderBatch.total_return_quantity = batchQuantity.total_return_quantity;
                                    orderBatch.requested_return_quantity = batchQuantity.requested_return_quantity;
                                    orderBatch.disp_requested_return_quantity = angular.copy(batchQuantity.requested_return_quantity);
                                });
                            }
                        }
                    });
                    deferred.resolve();
                });
            }).catch(msg => deferred.reject(msg))
                .finally(() => $scope.hideLoading());
        } else {
            deferred.resolve();
        }
        return deferred.promise;
    };

    let updateBatchWithOrder = (returnBatches, orderBatches) => {
        let return_orderBatches = new Map(orderBatches.map(o => [o.id,o]));
        angular.forEach(returnBatches, returnBatch => {
            let return_orderBatch = return_orderBatches.get(returnBatch.batch_id);
            returnBatch.dispReturnedQuantity = return_orderBatch.dispReturnedQuantity;
            returnBatch.return_quantity = returnBatch.return_quantity || 0;
            returnBatch.requested_return_quantity = return_orderBatch.requested_return_quantity - returnBatch.return_quantity;
            return_orderBatch.returnedQuantity = return_orderBatch.total_return_quantity - returnBatch.return_quantity;
            return_orderBatch.disp_requested_return_quantity = returnBatch.requested_return_quantity;
            returnBatch.returnedQuantity = return_orderBatch.returnedQuantity;
            returnBatch.returnReason = return_orderBatch.returnReason;
            returnBatch.defaultReturnReason = return_orderBatch.defaultReturnReason;
            returnBatch.fq = return_orderBatch.fq;
            returnBatch.displayMeta = returnBatch.return_quantity > 0;
        });
    };

    let updateReturnItemWithOrder = returnItem => {
        let return_orderItem = $scope.orderByMaterial.get(returnItem.material_id);
        if (checkNotNullEmpty(returnItem.batches)) {
            updateBatchWithOrder(returnItem.batches, return_orderItem.returnBatches);
            returnItem.isBatch = true;
        }
        returnItem.dispReturnedQuantity = return_orderItem.dispReturnedQuantity;
        returnItem.return_quantity = returnItem.return_quantity || 0;
        returnItem.requested_return_quantity = return_orderItem.requested_return_quantity - returnItem.return_quantity;
        return_orderItem.returnedQuantity = return_orderItem.total_return_quantity - returnItem.return_quantity;
        returnItem.returnedQuantity = return_orderItem.returnedQuantity;
        returnItem.reasons = return_orderItem.reasons;
        returnItem.returnReason = return_orderItem.returnReason;
        returnItem.defaultReturnReason = return_orderItem.defaultReturnReason;
        returnItem.fq = return_orderItem.fq;
        returnItem.id = return_orderItem.id;
        returnItem.nm = return_orderItem.nm;
        returnItem.huName = return_orderItem.huName;
        returnItem.huQty = return_orderItem.huQty;
        returnItem.tm = return_orderItem.tm;
        returnItem.displayMeta = returnItem.return_quantity > 0;
    };

    let updateReturnWithOrder = returnItems => {
        angular.forEach(returnItems, returnItem => {
            updateReturnItemWithOrder(returnItem);
        });
    };
    $scope.updateReturnWithOrder = updateReturnWithOrder;

    $scope.detailAddReturnItem = newItem => {
        newItem.material_id = newItem.id;
        newItem.material_name = newItem.nm;
        newItem.reason = newItem.defaultReturnReason;
        newItem.batches = [];
        newItem.isNewItem = true;
        $scope.addReturnItem(newItem);
        updateReturnItemWithOrder($scope.returnItems[$scope.returnItems.length - 1]);
    };

    $scope.detailDeleteReturnItem = index => {
        var returnItem = $scope.returnItems[index];
        if(!returnItem.isNewItem) {
            let return_orderItem = $scope.orderByMaterial.get(returnItem.material_id);
            return_orderItem.total_return_quantity -= returnItem.return_quantity;
            return_orderItem.requested_return_quantity -= returnItem.return_quantity;
            return_orderItem.returnedQuantity = return_orderItem.dispReturnedQuantity;
            if(returnItem.isBatch) {
                let return_orderBatches = new Map(return_orderItem.returnBatches.map(o => [o.id,o]));
                angular.forEach(returnItem.batches, returnBatch => {
                    let return_orderBatch =  return_orderBatches.get(returnBatch.batch_id);
                    return_orderBatch.total_return_quantity -= returnBatch.return_quantity;
                    return_orderBatch.requested_return_quantity -= returnBatch.return_quantity;
                    //return_orderBatch.returnedQuantity = return_orderBatch.dispReturnedQuantity;
                    return_orderBatch.disp_requested_return_quantity = angular.copy(return_orderBatch.requested_return_quantity);
                });
            }
        }
        $scope.deleteReturnItem(index);
    };

    $scope.initialiseEditing = () => {
        let deferred = $q.defer();
        if (!$scope.isItemInitialised && ($scope.returns.status.status == RETURNS.status.OPEN || $scope.returns.status.status == RETURNS.status.SHIPPED)) {
            $scope.initOrderItems().then(() => {
                ReturnsItemInitializer($scope, trnService, $q).then(() => {
                    updateReturnWithOrder($scope.returnItems);
                    ReturnsItemManager($scope, $timeout);
                    $scope.isItemInitialised = true;
                    deferred.resolve();
                }).catch(msg => deferred.reject(msg));
            }).catch(msg => deferred.reject(msg));
        } else {
            ReturnsItemManager($scope, $timeout);
            deferred.resolve();
        }
        return deferred.promise;
    };

    $scope.setMessageCount = count => $scope.messageCount = count;

    $scope.toggleStatusHistory = () => $scope.displayStatusHistory = !$scope.displayStatusHistory;

    $scope.changeStatus = value => {
        $scope.new_status = value;
        $scope.newStatus = {trackingDetails: {}};
        if (value == RETURNS.status.RECEIVED) {
            $scope.toggleReceive();
            return;
        }
        $scope.modalInstance = $uibModal.open({
            templateUrl: 'views/returns/returns-status.html',
            scope: $scope,
            keyboard: false,
            backdrop: 'static'
        });
    };

    $scope.toggleReceive = update => {
        if ($scope.page == 'detail') {
            $scope.page = "receive";
        } else {
            $scope.page = 'detail';
            if (update) {
                getReturn();
            }
        }
    };

    let closeStatusModal = () => $scope.modalInstance.dismiss('cancel');

    $scope.doShip = () => {
        $scope.showLoading();
        let request = {
            comment: $scope.newStatus.comment,
            tracking_details: $scope.newStatus.trackingDetails
        };
        returnsService.ship(RETURN_ID, request).then(() => {
            closeStatusModal();
            getReturn();
        }).catch(msg =>
            $scope.showErrorMsg(msg)
        )
            .finally(() => $scope.hideLoading());
    };

    $scope.doCancel = () => {
        $scope.showLoading();
        returnsService.cancel(RETURN_ID, {comment: $scope.newStatus.comment}).then(() => {
            closeStatusModal();
            getReturn();
        }).catch(msg => $scope.showErrorMsg(msg))
            .finally(() => $scope.hideLoading());
    };

    $scope.closeStatus = () => closeStatusModal();

    $scope.hasStatus = status => {
        return (checkNotNullEmpty($scope.statusList) && $scope.statusList.includes(status));
    };

    $scope.edit = {};
    $scope.toggleEdit = (field, close) => {
        if (close) {
            if (field == 'estimatedArrivalDate') {
                updateDateModels();
            }
        }
        $scope.edit[field] = !$scope.edit[field];
    };

    $scope.updateTrackingDetails = field => {
        returnsService.updateTrackingDetails(RETURN_ID, $scope.returns.tracking_details).then(data => {
            $scope.returns.tracking_details = data.data;
            updateDateModels();
            $scope.toggleEdit(field);
        }).catch(msg => $scope.showErrorMsg(msg));
    };

    let initReturnQuantity = () => {
        angular.forEach($scope.returnItems, returnItem => {
            returnItem.new_return_quantity = returnItem.return_quantity;
            if (returnItem.batches) {
                angular.forEach(returnItem.batches, returnBatch =>
                        returnBatch.new_return_quantity = returnBatch.return_quantity
                );
            }
        })
    };

    let toggleEdit = () => $scope.editMode = !$scope.editMode;

    $scope.editItems = () => {
        $scope.initialiseEditing().then(() => {
            $scope.originalReturnItems = angular.copy($scope.returnItems);
            $scope.originalOrderItems = angular.copy($scope.orderItems);
            initReturnQuantity();
            toggleEdit();
        }).catch(msg => $scope.showErrorMsg(msg));
    };

    $scope.doEdit = () => {
        if (ReturnsValidator.isReturnQuantityAdded($scope, true)) {
            let isInvalid = $scope.returnItems.some((returnItem, index) =>
                checkNullEmpty(returnItem.batches) && $scope.validateQuantityReturn(returnItem, index)
            );
            if (!isInvalid) {
                isInvalid = $scope.returnItems.some((returnItem, index) =>
                    checkNullEmpty(returnItem.batches) && $scope.validateStatusReturn(returnItem, index, returnItem.tm)
                );
            }
            if (!isInvalid) {
                isInvalid = $scope.returnItems.some((returnItem, index) =>
                    checkNullEmpty(returnItem.batches) && $scope.validateReasonReturn(returnItem, index)
                );
            }
            if (!isInvalid) {
                $scope.showLoading();
                returnsService.updateItems(RETURN_ID, angular.copy($scope.returnItems))
                    .then(()=>getReturn())
                    .catch(msg => $scope.showErrorMsg(msg))
                    .finally(() => $scope.hideLoading());
                toggleEdit();
            }
        } else {
            $scope.showWarning($scope.resourceBundle['return.specify.quantity']);
        }
    };

    $scope.cancelEdit = () => {
        $scope.returnItems = $scope.originalReturnItems;
        $scope.orderItems = $scope.originalOrderItems;
        toggleEdit();
    };

    $scope.additionalRows = [];
    $scope.editCount = 0;
    $scope.editBatchItems = item => {
        item.additionalRows = !item.additionalRows;
        $scope.editCount += (item.additionalRows ? 1 : -1);
    };
}

/**
 * This is child controller of DetailReturnsController
 *
 * All variables from DetailReturnsController are available to use directly
 */
function ReceiveReturnsController($scope, returnsService, requestContext) {

    const RETURN_ID = requestContext.getParam("returnId");
    $scope.comment = undefined;
    $scope.additionalRows = [];
    $scope.returnItems = angular.copy($scope.returns.items);

    let fillReceiveItem = () => {

        let getReceiveData = (item) => {
            let received = {received_quantity: item.return_quantity};
            let statuses = item.tm ? $scope.incomingTempmatstatus : $scope.incomingMatstatus;
            if (statuses.length > 0 && statuses.includes(item.material_status)) {
                received.material_status = item.material_status;
            }
            return received;
        };

        angular.forEach($scope.returnItems, returnItem => {
            if (checkNotNullEmpty(returnItem.batches)) {
                let totalReceivedQuantity = returnItem.batches.reduce((currentTotal, returnBatch) => {
                    returnBatch.received = getReceiveData(returnBatch);
                    return currentTotal + returnBatch.received.received_quantity * 1;
                }, 0);
                returnItem.received = {received_quantity: totalReceivedQuantity};
            } else {
                returnItem.received = getReceiveData(returnItem);
                returnItem.displayMeta = true;
            }
        });
    };

    $scope.initialiseEditing().then(() => {
        $scope.updateReturnWithOrder($scope.returnItems);
        fillReceiveItem()
    }).catch(msg => $scope.showErrorMsg(msg));

    let isBatchesInvalid = (returnItem) => {
        if (returnItem.tm) {
            if (checkNotNullEmpty($scope.incomingTempmatstatus)) {
                return returnItem.batches.some((returnItemBatch, index) =>
                    $scope.validateBatchStatusReturn(returnItem, returnItemBatch, index, true, true)
                );
            }
        } else {
            if (checkNotNullEmpty($scope.incomingMatstatus)) {
                return returnItem.batches.some((returnItemBatch, index) =>
                    $scope.validateBatchStatusReturn(returnItem, returnItemBatch, index, false, true)
                );
            }
        }
    };

    let isAllValid = () => {
        let isInvalid = $scope.returnItems.some((returnItem, index) => {
            if (returnItem.received.received_quantity > 0) {
                let isBatch = checkNotNullEmpty(returnItem.batches);
                if (!isBatch && $scope.validateQuantityReturn(returnItem, index, true)) {
                    return true;
                }
                if (!isBatch) {
                    if (returnItem.tm) {
                        return checkNotNullEmpty($scope.incomingTempmatstatus) &&
                            $scope.validateStatusReturn(returnItem, index, true, true);
                    } else {
                        return checkNotNullEmpty($scope.incomingMatstatus) &&
                            $scope.validateStatusReturn(returnItem, index, false, true);
                    }
                } else {
                    if (isBatchesInvalid(returnItem)) {
                        $scope.showWarning("Material status is mandatory. Please specify for all batches as well.");
                        return true;
                    }
                }
            }
        });
        return !isInvalid;
    };

    $scope.doReceive = () => {
        if (isAllValid()) {
            $scope.showLoading();
            returnsService.receive(RETURN_ID, {
                comment: $scope.comment,
                items: $scope.returnItems
            }).then(() => $scope.toggleReceive(true))
                .catch(msg => $scope.showErrorMsg(msg))
                .finally(() => $scope.hideLoading());
        }
    };

    $scope.editCount = 0;
    $scope.editBatchItems = item => {
        item.additionalRows = !item.additionalRows;
        $scope.editCount += (item.additionalRows ? 1 : -1);
    };
}

function BatchDetailReturnsController($scope) {
    $scope.returnItem = angular.copy($scope.item);
    let orderItem = $scope.orderByMaterial.get($scope.returnItem.material_id);
    $scope.originalOrderReturnBatches = angular.copy(orderItem.returnBatches);
    $scope.orderReturnBatches = orderItem.returnBatches;
    let orderReturnBatchMap = new Map($scope.orderReturnBatches.map(b => {
        b.reason = b.defaultReturnReason;
        return [b.id, b];
    }));
    angular.forEach($scope.returnItem.batches, returnBatch => {
        let orderReturnBatch = orderReturnBatchMap.get(returnBatch.batch_id);
        orderReturnBatch.new_return_quantity = returnBatch.return_quantity;
        orderReturnBatch.return_quantity = returnBatch.return_quantity;
        orderReturnBatch.material_status = returnBatch.material_status;
        orderReturnBatch.reason = returnBatch.reason;
        orderReturnBatch.displayMeta = true;
    });
    sortByKey($scope.orderReturnBatches, 'id');

    let isAllValid = () => {
        let isInvalid = $scope.orderReturnBatches.some((batchItem, index) => {
            if (batchItem.return_quantity > 0) {
                if ($scope.validateBatchQuantityReturn($scope.returnItem, batchItem, index)) {
                    return true;
                }
                if ($scope.returnItem.tm) {
                    return checkNotNullEmpty($scope.tempmatstatus) &&
                        $scope.validateBatchStatusReturn($scope.returnItem, batchItem, index, true);
                } else {
                    return checkNotNullEmpty($scope.matstatus) &&
                        $scope.validateBatchStatusReturn($scope.returnItem, batchItem, index);
                }
            }
        });
        return !isInvalid;
    };

    $scope.save = () => {
        let returnItemBatches = angular.copy($scope.returnItem.batches);
        let totalReturned = 0;
        angular.forEach($scope.orderReturnBatches, batchItem => {
            batchItem.return_quantity = batchItem.return_quantity * 1 || 0;
            let found = returnItemBatches.some((returnBatch, index) => {
                if (returnBatch.batch_id == batchItem.id) {
                    if (checkNullEmpty(batchItem.new_return_quantity * 1)) {
                        returnItemBatches.splice(index, 1);
                    } else {
                        returnBatch.returnedQuantity += (batchItem.new_return_quantity * 1 - batchItem.return_quantity * 1);
                        returnBatch.new_return_quantity = batchItem.new_return_quantity;
                        returnBatch.return_quantity = batchItem.new_return_quantity;
                        returnBatch.material_status = batchItem.material_status;
                        returnBatch.reason = batchItem.reason;
                        totalReturned += returnBatch.return_quantity * 1;
                    }
                    return true;
                }
            });

            if (!found && checkNotNullEmpty(batchItem.new_return_quantity)) {
                returnItemBatches.push({
                    batch_id: batchItem.id,
                    expiry: batchItem.e,
                    manufacturer: batchItem.bmfnm,
                    manufactured_date: batchItem.bmfdt,
                    return_quantity: batchItem.new_return_quantity,
                    returnedQuantity: batchItem.new_return_quantity,
                    material_status: batchItem.material_status,
                    reason: batchItem.reason,
                    fq: batchItem.fq,
                    new_return_quantity: batchItem.new_return_quantity
                });
                totalReturned += batchItem.new_return_quantity * 1;
            }
        });
        if (!isAllValid()) {
            return;
        }
        $scope.item.batches = returnItemBatches;
        $scope.item.return_quantity = $scope.item.new_return_quantity = totalReturned;
        orderItem.returnBatches = angular.copy($scope.originalOrderReturnBatches);

        $scope.editBatchItems($scope.item);
    };

    $scope.cancel = () => {
        orderItem.returnBatches = $scope.originalOrderReturnBatches;
        $scope.editBatchItems($scope.item);
    }
}

function BatchReceiveReturnsController($scope) {
    $scope.returnItem = angular.copy($scope.item);
    $scope.orderItems.some(orderItem => {
        if (orderItem.id == $scope.returnItem.material_id) {
            $scope.orderReturnBatches = angular.copy(orderItem.returnBatches);
            let orderBatchById = new Map($scope.orderReturnBatches.map(b => {
                b.received = {};
                return [b.id, b]
            }));
            angular.forEach($scope.returnItem.batches, returnBatch => {
                let orderBatch = orderBatchById.get(returnBatch.batch_id);
                orderBatch.received = returnBatch.received;
                orderBatch.return_quantity = returnBatch.return_quantity;
                orderBatch.material_status = returnBatch.material_status;
                orderBatch.reason = returnBatch.reason;
                orderBatch.displayMeta = true;
            });
            sortByKey($scope.orderReturnBatches, 'id');
            return true;
        }
    });

    let isAllValid = () => {
        let isInvalid = $scope.orderReturnBatches.some((batchItem, index) => {
            if (batchItem.received.received_quantity > 0) {
                if ($scope.validateBatchQuantityReturn($scope.returnItem, batchItem, index, true)) {
                    return true;
                }
                if ($scope.returnItem.tm) {
                    return checkNotNullEmpty($scope.tempmatstatus) &&
                        $scope.validateBatchStatusReturn($scope.returnItem, batchItem, index, true, true);
                } else {
                    return checkNotNullEmpty($scope.matstatus) &&
                        $scope.validateBatchStatusReturn($scope.returnItem, batchItem, index, false, true);
                }
            }
        });
        return !isInvalid;
    };

    $scope.save = () => {
        if (!isAllValid()) {
            return;
        }
        let returnItemBatches = $scope.item.batches;
        let totalReceived = 0;
        angular.forEach($scope.orderReturnBatches, batchItem => {
            batchItem.received.received_quantity = batchItem.received.received_quantity * 1 || 0;
            let found = returnItemBatches.some((returnBatch, index) => {
                if (returnBatch.batch_id == batchItem.id) {
                    if (checkNullEmpty(returnBatch.return_quantity) && checkNullEmpty(batchItem.received.received_quantity)) {
                        returnItemBatches.splice(index, 1);
                    } else {
                        returnBatch.received = batchItem.received;
                        totalReceived += returnBatch.received.received_quantity;
                    }
                    return true;
                }
            });

            if (!found && checkNotNullEmpty(batchItem.received) && checkNotNullEmpty(batchItem.received.received_quantity)) {
                returnItemBatches.push({
                    batch_id: batchItem.id,
                    expiry: batchItem.e,
                    manufacturer: batchItem.bmfnm,
                    manufactured_date: batchItem.bmfdt,
                    return_quantity: 0,
                    received: batchItem.received
                });
                totalReceived += batchItem.received.received_quantity;
            }
        });
        $scope.item.received.received_quantity = totalReceived;
        $scope.editBatchItems($scope.item);
    };

    $scope.cancel = () => $scope.editBatchItems($scope.itemIndex);
}

function ListReturnsController($scope, $location, requestContext, RETURNS, returnsService, ordService, exportService) {

    const OUTGOING = 'outgoing';
    const INCOMING = 'incoming';

    $scope.RETURNS = RETURNS;
    $scope.wparams = [["eid", "entity.id"], ["status", "status"], ["from", "from", "", formatDate2Url],
        ["type", "returnsType", OUTGOING], ["to", "to", "", formatDate2Url], ["oid", "orderId"], ["o", "offset"], ["s", "size"]];

    ListingController.call(this, $scope, requestContext, $location);

    $scope.localFilters = ['entity', 'status', 'from', 'to', 'orderId'];
    $scope.initLocalFilters = [];
    $scope.returnsType = OUTGOING;
    $scope.today = new Date();

    let getCustomerVendor = () => {
        let customer = undefined;
        let vendor = undefined;
        if (checkNotNullEmpty($scope.entity)) {
            if ($scope.returnsType == OUTGOING) {
                customer = $scope.entity.id;
            } else if ($scope.returnsType == INCOMING) {
                vendor = $scope.entity.id;
            }
        }
        return {customer, vendor};
    };

    $scope.fetch = () => {
        let {customer, vendor} = getCustomerVendor();
        $scope.showLoading();
        returnsService.getAll({
            customerId: customer,
            vendorId: vendor,
            status: $scope.status,
            startDate: formatDate($scope.from),
            endDate: formatDate($scope.to),
            orderId: $scope.orderId,
            offset: $scope.offset,
            size: $scope.size
        }).then(data => {
            $scope.filtered = data.data.returns;
            $scope.setResults({
                results: data.data.returns,
                numFound: data.data.total_count
            });
        }).catch(msg => $scope.showErrorMsg(msg))
            .finally(() => {
                $scope.loading = false;
                $scope.hideLoading();
                setTimeout(() => fixTable(), 200);
            });
    };

    $scope.init = () => {
        let entityId = requestContext.getParam("eid");
        if (checkNotNullEmpty(entityId)) {
            if (checkNullEmpty($scope.entity) || $scope.entity.id != parseInt(entityId)) {
                $scope.entity = {id: parseInt(entityId), nm: ""};
                $scope.initLocalFilters.push("entity")
            }
        }
        $scope.status = requestContext.getParam("status") || "";
        $scope.from = parseUrlDate(requestContext.getParam("from"));
        $scope.to = parseUrlDate(requestContext.getParam("to"));
        $scope.orderId = requestContext.getParam("oid");
        $scope.fetch();
    };
    $scope.init();

    $scope.goToReturn = returnId => $location.path(`/orders/returns/detail/${returnId}`);

    $scope.getSuggestions = text => {
        if (checkNotNullEmpty(text)) {
            return ordService.getIdSuggestions(text, 'oid').then(data => {
                return data.data;
            }).catch(errorMsg => $scope.showErrorMsg(errorMsg));
        }
    };

    $scope.resetFilters = () => {
        $scope.entity = null;
        $scope.status = "";
        $scope.from = undefined;
        $scope.to = undefined;
        $scope.orderId = undefined;
        $scope.returnsType = OUTGOING;
        $scope.showMore = undefined;
    };

    //This is not used/implemented in export service
    $scope.exportData = () => {
        let {customer, vendor} = getCustomerVendor();
        $scope.showLoading();
        exportService.exportData({
            customer_id: customer,
            vendor_id: vendor,
            status: $scope.status,
            from_date: checkNotNullEmpty($scope.from) ? formatDate2Url($scope.from) : undefined,
            end_date: checkNotNullEmpty($scope.to) ? formatDate2Url($scope.to) : undefined,
            order_id: $scope.orderId,
            titles: {
                filters: getCaption()
            },
            module: '',
            templateId: ''
        }).then(data => $scope.showSuccess(data.data))
            .catch(msg => $scope.showErrorMsg(msg))
            .finally(() => $scope.hideLoading());
    }
}