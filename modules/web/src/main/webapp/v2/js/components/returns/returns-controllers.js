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

CreateReturnsController.$inject = ['$scope', '$location', '$timeout', 'returnsService', 'trnService'];
DetailReturnsController.$inject = ['$scope', '$uibModal', 'requestContext', 'RETURNS', 'returnsService', 'conversationService', 'activityService'];
ListReturnsController.$inject = ['$scope', '$location', 'requestContext', 'RETURNS', 'returnsService', 'ordService', 'exportService'];

function CreateReturnsController($scope, $location, $timeout, returnsService, trnService) {

    $scope.returnItems = returnsService.getItems();
    $scope.returnOrder = returnsService.getOrder();
    $scope.invalidPopup = 0;

    $scope.showLoading();
    trnService.getReasons('ro')
        .then(function (data) {
            $scope.defaultReason = data.data.defRsn;
            $scope.reasons = [""].concat(data.data.rsns);
        })
        .then(function () {
            angular.forEach($scope.returnItems, function (returnItem) {
                if (returnItem.materialTags) {
                    trnService.getReasons('ro', returnItem.materialTags).then(function (data) {
                        if (checkNotNullEmpty(data.data) && checkNotNullEmpty(data.data.rsns)) {
                            returnItem.returnReason = angular.copy(data.data.defRsn);
                            returnItem.defaultReturnReason = angular.copy(data.data.defRsn);
                            returnItem.reasons = [""].concat(data.data.rsns);
                            angular.forEach(returnItem.returnBatches, function (returnBatch) {
                                returnBatch.returnReason = angular.copy(returnItem.returnReason);
                                returnBatch.defaultReturnReason = angular.copy(returnItem.returnReason);
                            });
                        } else {
                            setCommonReasons(returnItem);
                        }
                    });
                } else {
                    setCommonReasons(returnItem);
                }
            })
        }).catch(function error(msg) {
            $scope.showErrorMsg(msg);
        }).finally(function () {
            $scope.hideLoading();
        });

    $scope.showLoading();
    trnService.getTransactionTypesWithReasonMandatory().then(function(data){
        $scope.returnOutgoingReasonMandatory = data.data.indexOf('ro') != -1;
    }).catch(function error(msg) {
        $scope.showErrorMsg(msg);
    }).finally(function () {
        $scope.hideLoading();
    });

    $scope.showLoading();
    trnService.getMatStatus("ro", false).then(function (data) {
        $scope.matstatus = data.data;
    }).catch(function error(msg) {
        $scope.showErrorMsg(msg);
    }).finally(function () {
        $scope.hideLoading();
    });

    $scope.showLoading();
    trnService.getMatStatus("ro", true).then(function (data) {
        $scope.tempmatstatus = data.data;
    }).catch(function error(msg) {
        $scope.showErrorMsg(msg);
    }).finally(function () {
        $scope.hideLoading();
    });

    $scope.showLoading();
    trnService.getStatusMandatory().then(function (data) {
        $scope.statusMandatoryConfig = data.data;
    }).catch(function error(msg) {
        $scope.showErrorMsg(msg);
    }).finally(function () {
        $scope.hideLoading();
    });

    function setCommonReasons(item) {
        item.reasons = angular.copy($scope.reasons);
        item.returnReason = angular.copy($scope.defaultReason);
        item.defaultReturnReason = angular.copy($scope.defaultReason);
        angular.forEach(item.returnBatches, function (returnBatch) {
            returnBatch.returnReason = angular.copy(item.returnReason);
            returnBatch.defaultReturnReason = angular.copy(item.returnReason);
        });
    }

    $scope.cancel = function () {
        $scope.setPageSelection('orderDetail');
        $scope.enableScroll();
    };

    $scope.create = function () {
        if (isAllValid()) {
            $scope.showLoading();
            var request = {
                returnItems: $scope.returnItems,
                order_id: $scope.order.id,
                comment: $scope.comment
            };
            returnsService.create(request).then(function (data) {
                $scope.showSuccess("Returns created successfully");
                $location.path('/orders/returns/detail/' + data.data.return_id);
            }).catch(function error(msg) {
                $scope.showErrorMsg(msg);
            }).finally(function () {
                $scope.hideLoading();
            });
        }
    };

    function isAllValid() {
        if (isReturnQuantityAdded()) {
            if (isReturnValid('returnReason')) {
                if (!$scope.statusMandatoryConfig.rosm || isReturnValid('returnMaterialStatus')) {
                    return true;
                } else {
                    $scope.showWarning("Material status is mandatory for all materials being returned");
                }
            } else {
                $scope.showWarning("Reason is mandatory for all materials being returned")
            }
        } else {
            $scope.showWarning("Please specify return quantity for all materials")
        }
    }

    function isReturnQuantityAdded() {
        return $scope.returnItems.every(function (returnItem) {
            if (checkNotNullEmpty(returnItem.returnBatches)) {
                return returnItem.returnBatches.some(function (returnBatch) {
                    return checkNotNullEmpty(returnBatch.returnQuantity) && returnBatch.returnQuantity > 0;
                });
            } else {
                return checkNotNullEmpty(returnItem.returnQuantity) && returnItem.returnQuantity > 0;
            }
        });
    }

    function redrawAllPopup(type) {
        var index = -1;
        $scope.returnItems.forEach(function (returnItem) {
            index += 1;
            if (checkNotNullEmpty(returnItem.returnBatches)) {
                var batchIndex = -1;
                returnItem.returnBatches.forEach(function (returnBatch) {
                    batchIndex += 1;
                    if (returnBatch.popupMsg) {
                        type == 'show' ?
                            $scope.validateBatchQuantity(returnItem, returnBatch, batchIndex) :
                            hidePopup($scope, returnBatch, returnItem.id + returnBatch.id, batchIndex, $timeout);
                    }
                    if (returnBatch.sPopupMsg) {
                        type == 'show' ?
                            $scope.validateBatchStatus(returnItem, returnBatch, batchIndex, returnItem.tm) :
                            hidePopup($scope, returnBatch, 's' + (returnItem.tm ? 't' : '') + returnItem.id + returnBatch.id, batchIndex, $timeout);
                    }
                    if (returnBatch.rPopupMsg) {
                        type == 'show' ?
                            $scope.validateBatchReason(returnItem, returnBatch, batchIndex) :
                            hidePopup($scope, returnBatch, 'r' + returnItem.id + returnBatch.id, batchIndex, $timeout);
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
                        hidePopup($scope, returnItem, 's' + (returnItem.tm ? 't' : '') + returnItem.id, index, $timeout);
                }
                if (returnItem.rPopupMsg) {
                    type == 'show' ?
                        $scope.validateReason(returnItem, index) :
                        hidePopup($scope, returnItem, 'r' + returnItem.id, index, $timeout);
                }
            }
        });
        if (type != 'show') {
            $timeout(function () {
                redrawAllPopup('show');
            }, 0);
        }
    }

    $scope.validateQuantity = function (material, index) {
        var redraw = false;
        if (material.displayMeta != material.returnQuantity > 0) {
            redraw = true;
        }
        material.displayMeta = material.returnQuantity > 0;
        var isInvalid = false;
        if (checkNotNullEmpty(material.returnQuantity)) {
            if (material.returnQuantity > material.fq) {
                showPopup($scope, material, material.id,
                    "The quantity to be returned " + material.returnQuantity + " cannot exceed the original receipt quantity " + material.fq + ".",
                    index, $timeout);
                isInvalid = true;
            } else if (material.returnQuantity > material.fq - material.returnedQuantity) {
                showPopup($scope, material, material.id,
                    "The quantity to be returned " + material.returnQuantity + " cannot exceed the remaining return quantity " + (material.fq - material.returnedQuantity) + ".",
                    index, $timeout);
                isInvalid = true;
            } else if (checkNotNullEmpty(material.huName) && checkNotNullEmpty(material.huQty) &&
                material.returnQuantity % material.huQty != 0) {
                showPopup($scope, material, material.id,
                    material.returnQuantity + " of " + material.nm + " does not match the multiples of units expected in " +
                    material.huName + ". It should be in multiples of " + material.huQty + " " + material.nm + ".",
                    index, $timeout);
                isInvalid = true;
            } else if (material.returnQuantity > material.atpstk) {
                showPopup($scope, material, material.id,
                    "The quantity to be returned " + material.returnQuantity+ " cannot exceed the available stock "+ material.atpstk + ".",
                    index, $timeout);
                isInvalid = true;
            }
        } else {
            material.sinvalidPopup = material.sPopupMsg = material.rinvalidPopup = material.rPopupMsg = undefined;
            material.returnReason = angular.copy(material.defaultReturnReason);
        }
        if (redraw) {
            redrawAllPopup();
        }
        return isInvalid;
    };

    $scope.validateBatchQuantity = function (material, batchMaterial, index) {

        var redraw = false;
        if (batchMaterial.displayMeta != batchMaterial.returnQuantity > 0) {
            redraw = true;
        }
        batchMaterial.displayMeta = batchMaterial.returnQuantity > 0;
        var isInvalid = false;
        if (checkNotNullEmpty(batchMaterial.returnQuantity)) {
            if (batchMaterial.returnQuantity > batchMaterial.fq) {
                showPopup($scope, batchMaterial, material.id + batchMaterial.id,
                    "The quantity to be returned " + batchMaterial.returnQuantity + " cannot exceed the original receipt quantity " + batchMaterial.fq + ".",
                    index, $timeout);
                isInvalid = true;
            } else if (batchMaterial.returnQuantity > batchMaterial.fq - batchMaterial.returnedQuantity) {
                showPopup($scope, batchMaterial, material.id + batchMaterial.id,
                    "The quantity to be returned " + batchMaterial.returnQuantity + " cannot exceed the remaining return quantity " + (batchMaterial.fq - batchMaterial.returnedQuantity) + ".",
                    index, $timeout);
                isInvalid = true;
            } else if (checkNotNullEmpty(material.huName) && checkNotNullEmpty(material.huQty) &&
                batchMaterial.returnQuantity % material.huQty != 0) {
                showPopup($scope, batchMaterial, material.id + batchMaterial.id,
                    batchMaterial.returnQuantity + " of " + material.nm + " does not match the multiples of units expected in " +
                    material.huName + ". It should be in multiples of " + material.huQty + " " + material.nm + ".",
                    index, $timeout);
                isInvalid = true;
            } else if (batchMaterial.returnQuantity > batchMaterial.atpstk) {
                showPopup($scope, batchMaterial, material.id + batchMaterial.id,
                    "The quantity to be returned " + batchMaterial.returnQuantity + " cannot exceed the available stock " + batchMaterial.atpstk + ".",
                    index, $timeout);
                isInvalid = true;
            }
        } else {
            batchMaterial.sinvalidPopup = batchMaterial.sPopupMsg = batchMaterial.rinvalidPopup = batchMaterial.rPopupMsg = undefined;
            batchMaterial.returnReason = angular.copy(batchMaterial.defaultReturnReason);
        }
        if (redraw) {
            redrawAllPopup();
        }
        return isInvalid;
    };

    $scope.validateReason = function (material, index) {
        if (checkNullEmpty(material.returnReason)) {
            showPopup($scope, material, 'r' + material.id, "Reason is mandatory", index, $timeout, false, false, true);
            return true;
        }
    };

    $scope.validateBatchReason = function (material, batchMaterial, index) {
        if (checkNullEmpty(batchMaterial.returnReason)) {
            showPopup($scope, batchMaterial, 'r' + material.id + batchMaterial.id, "Reason is mandatory", index, $timeout, false, false, true);
            return true;
        }
    };

    $scope.validateStatus = function (material, index, isTempStatus) {
        if ($scope.statusMandatoryConfig.rosm && checkNullEmpty(material.returnMaterialStatus)) {
            showPopup($scope, material, 's' + (isTempStatus ? 't' : '') + material.id, "Material status is mandatory", index, $timeout,false,true,false);
            return true;
        }
    };

    $scope.validateBatchStatus = function (material, batchMaterial, index, isTempStatus) {
        if ($scope.statusMandatoryConfig.rosm && checkNullEmpty(batchMaterial.returnMaterialStatus)) {
            showPopup($scope, batchMaterial, 's' + (isTempStatus ? 't' : '') + material.id + batchMaterial.id, "Material status is mandatory", index, $timeout,false,true,false);
            return true;
        }
    };

    $scope.closePopup = function (material, index, prefix) {
        hidePopup($scope, material, (prefix ? prefix : '') + material.id, index, $timeout, false, prefix == 's' || prefix == 'st', prefix == 'r');
    };

    $scope.closeBatchPopup = function (material, batchMaterial, index, prefix) {
        hidePopup($scope, batchMaterial, (prefix ? prefix : '') + material.id + batchMaterial.id, index, $timeout, false, prefix == 's' || prefix == 'st', prefix == 'r');
    };

    function isReturnValid(field) {
        var index = -1;
        return !$scope.returnItems.some(function (returnItem) {
            index += 1;
            var batchIndex = -1;
            if (checkNotNullEmpty(returnItem.returnBatches)) {
                return returnItem.returnBatches.some(function (returnBatch) {
                    batchIndex += 1;
                    if (checkNotNullEmpty(returnBatch.returnQuantity)) {
                        if (field == 'returnReason') {
                            if(checkNotNullEmpty(returnItem.reasons) && returnItem.reasons.length > 1) {
                                return $scope.validateBatchReason(returnItem, returnBatch, batchIndex);
                            }
                        } else if (field == 'returnMaterialStatus') {
                            if (returnItem.tm) {
                                if (checkNotNullEmpty($scope.tempmatstatus)) {
                                    return $scope.validateBatchStatus(returnItem, returnBatch, batchIndex, true);
                                }
                            } else {
                                if (checkNotNullEmpty($scope.matstatus)) {
                                    return $scope.validateBatchStatus(returnItem, returnBatch, batchIndex);
                                }
                            }
                        }
                    }
                });
            } else {
                if (field == 'returnReason') {
                    if(checkNotNullEmpty(returnItem.reasons) && returnItem.reasons.length > 1) {
                        return $scope.validateReason(returnItem, index);
                    }
                } else if (field == 'returnMaterialStatus') {
                    if (returnItem.tm) {
                        if (checkNotNullEmpty($scope.tempmatstatus)) {
                            return $scope.validateStatus(returnItem, index, true);
                        }
                    } else {
                        if (checkNotNullEmpty($scope.matstatus)) {
                            return $scope.validateStatus(returnItem, index);
                        }
                    }
                }
            }
        });
    }
}

function DetailReturnsController($scope, $uibModal, requestContext, RETURNS, returnsService, conversationService, activityService) {
    $scope.RETURNS = RETURNS;
    $scope.page = 'detail';
    $scope.subPage = 'consignment';

    var returnId = requestContext.getParam("returnId");

    function getReturn() {
        $scope.showLoading();
        returnsService.get(returnId)
            .then(function (data) {
                $scope.returns = data.data;
            })
            .then(function () {
                getMessageCount();
                getStatusHistory();
                checkStatusList();
            })
            .catch(function error(msg) {
                $scope.showErrorMsg(msg);
            }).finally(function () {
                $scope.hideLoading();
            });
    }

    getReturn();

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
                    $scope.statusList.push(RETURNS.status.CANCELLED);
                }
                break;
            default:
                $scope.statusList = [];
        }
    }

    $scope.changeStatus = function (value) {
        $scope.new_status = value;
        $scope.newStatus = {};
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

    $scope.toggleReceive = function (update) {
        if ($scope.page == 'detail') {
            $scope.page = "receive";
        } else {
            $scope.page = 'detail';
            if (update) {
                getReturn();
            }
        }
    };

    $scope.doShip = function () {
        $scope.showLoading();
        returnsService.ship(returnId, {comment: $scope.newStatus.comment}).then(function (data) {
            closeStatusModal();
            getReturn();
        }).catch(function error(msg) {
            $scope.showErrorMsg(msg);
        }).finally(function () {
            $scope.hideLoading();
        });
    };

    $scope.doCancel = function () {
        $scope.showLoading();
        returnsService.cancel(returnId, {comment: $scope.newStatus.comment}).then(function (data) {
            closeStatusModal();
            getReturn();
        }).catch(function error(msg) {
            $scope.showErrorMsg(msg);
        }).finally(function () {
            $scope.hideLoading();
        });
    };

    function closeStatusModal() {
        $scope.modalInstance.dismiss('cancel');
    }

    $scope.closeStatus = function () {
        closeStatusModal();
    };

    $scope.hasStatus = function (status) {
        return (checkNotNullEmpty($scope.statusList) && $scope.statusList.indexOf(status) > -1);
    };
}

function ReceiveReturnsController($scope, returnsService, requestContext) {

    var returnId = requestContext.getParam("returnId");
    $scope.comment = undefined;

    $scope.doReceive = function () {
        $scope.showLoading();
        returnsService.receive(returnId, {comment: $scope.comment}).then(function (data) {
            $scope.toggleReceive(true);
        }).catch(function error(msg) {
            $scope.showErrorMsg(msg);
        }).finally(function () {
            $scope.hideLoading();
        });
    }
}

function ListReturnsController($scope, $location, requestContext, RETURNS, returnsService, orderService, exportService) {

    const OUTGOING = 'outgoing';
    const INCOMING = 'incoming';

    $scope.RETURNS = RETURNS;
    $scope.wparams = [["eid", "entity.id"], ["status", "status"], ["from", "from", "", formatDate2Url], ["type", "returnsType", OUTGOING], ["to", "to", "", formatDate2Url], ["oid", "orderId"], ["o", "offset"], ["s", "size"]];

    ListingController.call(this, $scope, requestContext, $location);

    $scope.localFilters = ['entity', 'status', 'from', 'to', 'orderId'];
    $scope.initLocalFilters = [];
    $scope.returnsType = OUTGOING;
    $scope.today = new Date();

    function getCustomerVendor() {
        var customer = undefined;
        var vendor = undefined;
        if (checkNotNullEmpty($scope.entity)) {
            if ($scope.returnsType == OUTGOING) {
                customer = $scope.entity.id;
            } else if ($scope.returnsType == INCOMING) {
                vendor = $scope.entity.id;
            }
        }
        return {customer: customer, vendor: vendor};
    }

    $scope.fetch = function () {
        var kiosks = getCustomerVendor();
        $scope.showLoading();
        returnsService.getAll({
            customerId: kiosks.customer,
            vendorId: kiosks.vendor,
            status: $scope.status,
            startDate: formatDate($scope.from),
            endDate: formatDate($scope.to),
            orderId: $scope.orderId,
            offset: $scope.offset,
            size: $scope.size
        }).then(function (data) {
            $scope.filtered = data.data.returns;
            $scope.setResults({
                results: data.data.returns,
                numFound: data.data.total_count
            });
        }).catch(function error(msg) {
            $scope.showErrorMsg(msg);
        }).finally(function () {
            $scope.loading = false;
            $scope.hideLoading();
            setTimeout(function () {
                fixTable();
            }, 200);
        });
    };

    $scope.init = function () {
        var entityId = requestContext.getParam("eid");
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

    $scope.goToReturn = function (returnId) {
        $location.path('/orders/returns/detail/' + returnId);
    };

    $scope.getSuggestions = function (text) {
        if (checkNotNullEmpty(text)) {
            return orderService.getIdSuggestions(text, 'oid').then(function (data) {
                return data.data;
            }).catch(function (errorMsg) {
                $scope.showErrorMsg(errorMsg);
            });
        }
    };

    $scope.resetFilters = function () {
        $scope.entity = null;
        $scope.status = "";
        $scope.from = undefined;
        $scope.to = undefined;
        $scope.orderId = undefined;
        $scope.returnsType = OUTGOING;
        $scope.showMore = undefined;
    };

    $scope.exportData = function () {
        var kiosks = getCustomerVendor();
        $scope.showLoading();
        exportService.exportData({
            customer_id: kiosks.customer,
            vendor_id: kiosks.vendor,
            status: $scope.status,
            from_date: checkNotNullEmpty($scope.from) ? formatDate2Url($scope.from) : undefined,
            end_date: checkNotNullEmpty($scope.to) ? formatDate2Url($scope.to) : undefined,
            order_id: $scope.orderId,
            titles: {
                filters: getCaption()
            },
            module: '',
            templateId: ''
        }).then(function (data) {
            $scope.showSuccess(data.data);
        }).catch(function error(msg) {
            $scope.showErrorMsg(msg);
        }).finally(function () {
            $scope.hideLoading();
        });
    }
}


