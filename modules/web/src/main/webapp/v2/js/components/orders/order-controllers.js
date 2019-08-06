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

var ordControllers = angular.module('ordControllers', []);
ordControllers.controller('OrdersCtrl', ['$scope', 'ordService', 'domainCfgService', 'entityService', 'requestContext', '$location', 'exportService',
    function ($scope, ordService, domainCfgService, entityService, requestContext, $location, exportService) {
        $scope.wparams = [["etag", "etag"], ["otag", "otag"], ["status", "status"], ["o", "offset"], ["s", "size"], ["eid", "entity.id"], ["otype", "otype", "sle"], ["from", "from", "", formatDate2Url], ["to", "to", "", formatDate2Url], ["oid", "orderId"], ["sales_ref_id", "salesReferenceId"], ["approval_status", "approval_status"], ["purchase_ref_id", "purchaseReferenceId"], ["transfer_ref_id", "transferReferenceId"]];
        $scope.orders;
        $scope.otype;
        $scope.localFilters = ['entity', 'status', 'dateModel', 'from', 'to', 'salesRefId', 'refId', 'etag', 'tpdos', 'etag', 'otag', 'approval_status', 'purchaseRefId', 'transferRefId'];
        $scope.exRow = [];
        $scope.etag;
        $scope.otag;
        $scope.tags = {};
        $scope.from;
        $scope.to;
        $scope.initLoad = true;
        $scope.today = formatDate2Url(new Date());
        $scope.type = "0";
        domainCfgService.getEntityTagsCfg().then(function (data) {
            $scope.etags = data.data.tags;
        }).catch(function error(msg) {
            $scope.showErrorMsg(msg);
        });
        domainCfgService.getOrderTagsCfg().then(function (data) {
            $scope.otags = data.data.tags;
        }).catch(function error(msg) {
            $scope.showErrorMsg(msg);
        });
        $scope.openOrder = function (di, isMap) {
            if (typeof di === 'object') {
                if (di.oty == 0) {
                    $location.path('/orders/transfers/detail/' + di.id);
                } else if (checkNotNullEmpty(isMap)) {
                    $location.path('/orders/detail/' + di.id).search({map: 'true'});
                } else {
                    $location.path('/orders/detail/' + di.id);
                }
            } else {
                if ($scope.ordType == 'trn') {
                    $location.path('/orders/transfers/detail/' + di);
                } else {
                    $location.path('/orders/detail/' + di);
                }
            }
        };
        $scope.selectAll = function (newval) {
            $scope.selectedOrderIds.splice(0, $scope.selectedOrderIds.length);
            if (newval) {
                var index = 0;
                for (var item in $scope.filtered) {
                    $scope.selectedOrderIds[index++] = $scope.filtered[item].id;
                }
            }
        };
        domainCfgService.getOrdersCfg().then(function (data) {
            $scope.oCfg = data.data;
        }).catch(function error(msg) {
            $scope.showErrorMsg(msg);
        });
        $scope.initLocalFilters = [];
        $scope.init = function (firstTimeInit) {
            var triggerFetch = true;
            $scope.selAll = false;
            $scope.showOrderDetail = false;
            $scope.search = {};
            $scope.selectedOrderIds = [];
            $scope.etag = requestContext.getParam("etag") || "";
            $scope.otag = requestContext.getParam("otag") || "";
            $scope.approval_status = requestContext.getParam("approval_status") || "";
            $scope.status = requestContext.getParam("status") || "";
            $scope.search.vnm = requestContext.getParam("vendor") || "";
            $scope.from = parseUrlDate(requestContext.getParam("from"));
            $scope.to = parseUrlDate(requestContext.getParam("to"));
            $scope.otype = requestContext.getParam("otype");
            $scope.orderId = $scope.ordId = requestContext.getParam("oid");
            $scope.hideOrder = false;
            if (firstTimeInit) {
                delete $location.$$search.sales_ref_id;
                $scope.salesReferenceId = null;
                if ($scope.ordType == 'trn') {
                    delete $location.$$search.transfer_ref_id;
                    $scope.transferReferenceId = null;
                } else {
                    delete $location.$$search.purchase_ref_id;
                    $scope.purchaseReferenceId = null;
                }
                $location.$$compose();
            } else {
                $scope.salesReferenceId = requestContext.getParam("sales_ref_id");
                if ($scope.ordType == 'trn') {
                    $scope.transferReferenceId = requestContext.getParam("transfer_ref_id");
                } else {
                    $scope.purchaseReferenceId = requestContext.getParam("purchase_ref_id");
                }
            }
            if (checkNullEmpty($scope.otype)) {
                $scope.otype = "sle";
            }
            if (typeof  $scope.showEntityFilter === 'undefined') {
                $scope.showEntityFilter = true;
            }
            if ($scope.ordType == "trn") {
                $scope.type = "2";
            }
            var defaultFetchOrder = true;
            if ($scope.showEntityFilter) {
                if (checkNotNullEmpty(requestContext.getParam("eid"))) {
                    if (checkNullEmpty($scope.entity) || $scope.entity.id != parseInt(requestContext.getParam("eid"))) {
                        $scope.entity = {id: parseInt(requestContext.getParam("eid")), nm: ""};
                        $scope.initLocalFilters.push("entity")
                    }

                } else {
                    if (firstTimeInit && checkNotNullEmpty($scope.defaultEntityId)) {
                        $location.$$search.eid = $scope.defaultEntityId;
                        $location.$$compose();
                        $scope.entity = {id: $scope.defaultEntityId, nm: ""};
                        $scope.initLocalFilters.push("entity")
                        defaultFetchOrder = false;
                    } else {
                        $scope.entity = null;
                        if (checkNotNullEmpty($scope.otype) && $scope.otype != "sle" && !$scope.iMan) {
                            $scope.otype = "sle";
                            triggerFetch = false;
                        }
                    }
                }
            }
            $scope.exRow = [];
            if (firstTimeInit && defaultFetchOrder) {
                $scope.fetch();
            }
            return triggerFetch;
        };

        function getCaption() {
            var caption = getFilterTitle($scope.entity, $scope.resourceBundle['kiosk'], 'nm');
            caption += getFilterTitle(getStatusLabel($scope.status), $scope.resourceBundle['status']);
            caption += getFilterTitle(formatDate2Url($scope.from), $scope.resourceBundle['from']);
            caption += getFilterTitle(formatDate2Url($scope.to), $scope.resourceBundle['to']);
            caption += getFilterTitle($scope.salesReferenceId, $scope.resourceBundle['sales.reference.id']);
            if ($scope.ordType == 'trn') {
                if ($scope.transRelease) {
                    caption += getFilterTitle($scope.transferReferenceId, $scope.resourceBundle['release.reference.id']);
                } else {
                    caption += getFilterTitle($scope.transferReferenceId, $scope.resourceBundle['transfer.reference.id']);
                }
            } else {
                caption += getFilterTitle($scope.purchaseReferenceId, $scope.resourceBundle['purchase.reference.id']);
            }
            caption += getFilterTitle(getApprovalStatusLabel($scope.approval_status), $scope.resourceBundle['approval.status']);
            caption += getFilterTitle(checkNullEmpty($scope.entity) ? $scope.etag : "", $scope.resourceBundle['kiosk'] + " " + $scope.resourceBundle['tag.lower']);
            caption += getFilterTitle($scope.otag, $scope.resourceBundle['order'] + " " + $scope.resourceBundle['tag.lower']);
            if(checkNotNullEmpty($scope.entity)) {
                caption += getFilterTitle(getTypeLabel($scope.otype), $scope.resourceBundle['order'] + " " + $scope.resourceBundle['type']);
            }
            return caption;
        }

        function getTypeLabel(type) {
            if (type == "sle") {
                return $scope.type == "2" ? $scope.resourceBundle['outgoing']: $scope.resourceBundle['sales.orders'];
            } else if (type == "prc") {
                return $scope.type == "2" ? $scope.resourceBundle['incoming'] : $scope.resourceBundle['purchase.orders'];
            }
        }

        function getApprovalStatusLabel(status) {
            switch (status) {
                case 'pn':
                    return $scope.resourceBundle['order.pending'];
                case 'cn':
                    return $scope.resourceBundle['approvals.cancelled'];
                case 'rj':
                    return $scope.resourceBundle['approvals.rejected'];
                case 'ap':
                    return $scope.resourceBundle['approvals.approved'];
                case 'ex':
                    return $scope.resourceBundle['approvals.expired'];
            }
        }

        function getStatusLabel(status) {
            switch (status) {
                case 'pn':
                    return $scope.resourceBundle['order.pending'];
                case 'cf':
                    return $scope.resourceBundle['order.confirmed'];
                case 'bo':
                    return $scope.resourceBundle['order.backordered'];
                case 'cm':
                    return $scope.resourceBundle['order.shipped'];
                case 'fl':
                    return $scope.resourceBundle['order.fulfilled'];
                case 'cn':
                    return $scope.resourceBundle['order.cancelled'];
            }
        }


        $scope.exportData = function (isInfo) {
            var eid, ktag = undefined;
            if (checkNotNullEmpty($scope.entity)) {
                eid = $scope.entity.id;
            } else {
                ktag = checkNotNullEmpty($scope.etag) ? $scope.etag : undefined;
            }
            var oty = $scope.type == '2' ? '0' : '1';
            var module = oty == 0 ? 'transfers' : 'orders';
            var templateId = oty == 0 ? 'transfers' : 'orders';
            if (isInfo) {
                return {
                    filters: getCaption(),
                    type: oty == 0 ? $scope.resourceBundle['exports.transfers'] : $scope.resourceBundle['exports.orders']
                };
            }
            $scope.showLoading();
            exportService.exportData({
                from_date: checkNotNullEmpty($scope.from) ? formatDate2Url($scope.from) : undefined,
                end_date: checkNotNullEmpty($scope.to) ? formatDate2Url($scope.to) : undefined,
                entity_id: eid,
                ktag: ktag,
                sales_reference_id: $scope.salesReferenceId,
                purchase_reference_id: $scope.purchaseReferenceId,
                transfer_reference_id: $scope.transferReferenceId,
                linked_kid: $scope.lEntityId,
                order_subtype: $scope.otype,
                status: $scope.status,
                approval_status: $scope.approval_status,
                otag: $scope.otag,
                tag_type: $scope.tType,
                type: oty,
                titles: {
                    filters: getCaption()
                },
                module: module,
                templateId: templateId
            }).then(function (data) {
                $scope.showSuccess(data.data);
            }).catch(function error(msg) {
                $scope.showErrorMsg(msg);
            }).finally(function(){
                $scope.hideLoading();
            });
        };
        $scope.setData = function (data) {
            if (data != null && checkNotNullEmpty(data.results) && data.results.length > 0) {
                $scope.orders = data;
                $scope.setResults($scope.orders);
                $scope.initLoad = false;
                var orderData = $scope.orders.results;
                if (orderData.length == 1 && $scope.isReferenceIdAvailable) {
                    $scope.orderId = orderData[0].id;
                }
            } else {
                $scope.orders = {results: []};
                $scope.setResults(null);
            }

            if ($scope.initLoad && (data == null || checkNullEmpty(data.results)) && $scope.iMan) {
                $scope.initLoad = false;
                if ($scope.otype == "sle") {
                    $scope.otype = "prc";
                } else if ($scope.otype == "prc") {
                    $scope.otype = "sle";
                }
            }

        };
        $scope.fetch = function () {
            $scope.orders = {};
            $scope.loading = true;
            $scope.isReferenceIdAvailable = $scope.salesReferenceId || $scope.purchaseReferenceId || $scope.transferReferenceId;
            $scope.showLoading();
            if ($scope.mxE && checkNullEmpty($scope.entity)) {
                $scope.setData(null);
                return;
            }
            if (checkNotNullEmpty($scope.entity)) {
                $scope.otag = null;
                $scope.etag = null;
            }
            if (checkNotNullEmpty($scope.otag)) {
                $scope.tType = "or";
                $scope.ft = $scope.otag;
            } else {
                $scope.tType = "en";
                $scope.ft = $scope.etag;
            }
            var oty = $scope.type == '2' ? '0' : '1';
            if (checkNotNullEmpty($scope.entity)) {
                $scope.ft = undefined;
                ordService.getEntityOrders($scope.entity.id, $scope.otype, $scope.status, $scope.tType, $scope.ft, formatDate($scope.from), formatDate($scope.to), $scope.offset, $scope.size, oty,
                    $scope.salesReferenceId, $scope.approval_status, $scope.purchaseReferenceId, $scope.transferReferenceId).then(function (data) {
                    if (checkNotNullEmpty(data.data)) {
                        $scope.setData(data.data);
                        if (checkNotNullEmpty(data.data.results) && data.data.results.length == 1 && $scope.isReferenceIdAvailable) {
                            $scope.openOrder($scope.orderId);
                        }
                    }
                }).catch(function error(msg) {
                    $scope.showErrorMsg(msg);
                }).finally(function () {
                    $scope.loading = false;
                    $scope.hideLoading();
                    setTimeout(function () {
                        fixTable();
                    }, 200);
                });
            } else {
                ordService.getOrders($scope.otype, $scope.status, $scope.tType, $scope.ft, formatDate($scope.from), formatDate($scope.to), $scope.offset, $scope.size, oty,
                    $scope.salesReferenceId, $scope.approval_status, $scope.purchaseReferenceId, $scope.transferReferenceId).then(function (data) {
                    if (checkNotNullEmpty(data.data)) {
                        $scope.setData(data.data);
                        if (checkNotNullEmpty(data.data.results) && data.data.results.length == 1 && $scope.isReferenceIdAvailable) {
                            $scope.openOrder($scope.orderId);
                        }
                    }
                }).catch(function error(msg) {
                    $scope.showErrorMsg(msg);
                }).finally(function () {
                    $scope.loading = false;
                    $scope.hideLoading();
                    setTimeout(function () {
                        fixTable();
                    }, 200);
                });
            }
        };
        $scope.getSuggestions = function (text, type) {
            if (checkNotNullEmpty(text)) {
                var oty = $scope.type == '2' ? '0' : '1';
                return ordService.getIdSuggestions(text, type, oty).then(function (data) {
                    return data.data;
                }).catch(function (errorMsg) {
                    $scope.showErrorMsg(errorMsg);
                });
            }
        };
        ListingController.call(this, $scope, requestContext, $location);
        $scope.init(true);
        $scope.select = function (index, isMap) {
            if ($scope.exRow.length == 0) {
                for (var i = 0; i < $scope.orders.results.length; i++) {
                    $scope.exRow.push(false);
                }
            }
            $scope.cur = index;
            $scope.exRow[index] = !$scope.exRow[index];
            var data = $scope.orders.results[index];
            $scope.ldisMap = isMap && !(data.lt == undefined || data.ln == undefined || (data.lt == 0 && data.ln == 0));
        };
        $scope.toggle = function (index) {
            $scope.select(index);
        };
        $scope.metadata = [{'title': 'Sl.No.', 'field': 'sno'}, {'title': 'Order', 'field': 'id'}, {
            'title': 'Items',
            'field': 'size'
        },
            {'title': 'Price', 'field': 'price'}, {'title': 'Status', 'field': 'status'},
            {'title': 'Entity', 'field': 'enm'}, {'title': 'Created on', 'field': 'cdt'},
            {'title': 'Vendor', 'field': 'vnm'}];

        $scope.orderChanged = function (index, order) {
            var localOrder = $scope.orders.results[index];
            if (checkNotNull(localOrder)) {
                localOrder.size = order.size;
                localOrder.price = order.price;
                localOrder.status = order.status;
                localOrder.vnm = order.vnm;
                localOrder.vct = order.vct;
                localOrder.vst = order.vst;
                localOrder.hva = order.hva;
            }
        };
        $scope.exportOrders = function (type, extraParams) {
            if (checkNotNullEmpty($scope.status)) {
                extraParams += "&status=" + $scope.status;
            }
            if (checkNotNullEmpty($scope.entity)) {
                if (checkNotNullEmpty($scope.entity.id)) {
                    if (checkNotNullEmpty($scope.otype)) {
                        extraParams += "&otype=" + $scope.otype;
                    }
                }
            }
            var oty = $scope.type == '2' ? '0' : '1';
            extraParams += '&orderType=' + oty;
            exportService.scheduleBatchExport(type, extraParams).then(function (data) {
                if (data != "download complete") {
                    $scope.showSuccess($scope.resourceBundle['export.success1'] + ' ' + $scope.mailId + ' ' + $scope.resourceBundle['export.success2'] + ' ' + $scope.resourceBundle['exportstatusinfo2'] + ' ' + data.data + '. ' + $scope.resourceBundle['exportstatusinfo1']);
                    $scope.selAll = false;
                    $scope.selectAll($scope.selAll);
                }

            }).catch(function error(msg) {
                $scope.showErrorMsg(msg);
            });
        };
        $scope.$watch("etag", function (newVal) {
            if (checkNotNullEmpty(newVal)) {
                $scope.otag = null;
            }
        });
        $scope.$watch("otag", function (newVal) {
            if (checkNotNullEmpty(newVal)) {
                $scope.etag = null;
            }
        });
        $scope.$watch("salesRefId", function (newVal) {
            if (checkNullEmpty(newVal)) {
                $scope.salesReferenceId = null;
            }
        });
        $scope.$watch("purchaseRefId", function (newVal) {
            if (checkNullEmpty(newVal)) {
                $scope.purchaseReferenceId = null;
            }
        });
        $scope.$watch("transferRefId", function (newVal) {
            if (checkNullEmpty(newVal)) {
                $scope.transferReferenceId = null;
            }
        });
        $scope.$watch("apStatus", function (newVal) {
            if (checkNullEmpty(newVal)) {
                $scope.approvalStatus = null;
            }
        });

        $scope.resetFilters = function () {
            if ($scope.showEntityFilter) {
                $scope.entity = null;
            }
            $scope.status = "";
            $scope.from = undefined;
            $scope.to = undefined;
            $scope.otag = null;
            $scope.etag = null;
            $scope.otype = 'sle';
            $scope.oid = undefined;
            $scope.showOrderDetail = false;
            $scope.orderId = undefined;
            $scope.order = undefined;
            $scope.hideOrder = false;
            $scope.salesRefId = undefined;
            $scope.salesReferenceId = undefined;
            $scope.purchaseRefId = undefined;
            $scope.purchaseReferenceId = undefined;
            $scope.transferRefId = undefined;
            $scope.transferReferenceId = undefined;
            $scope.approvalStatus = null;
            $scope.apStatus = null;
            $scope.type = undefined;
            $scope.showMore = undefined;
        };

        $scope.showOrder = function (orderId) {
            if (checkNotNullEmpty(orderId)) {
                $scope.orderId = orderId;
                $scope.hideOrder = true;
                $scope.openOrder(orderId);
            }
        };

        $scope.showOrderByReferenceId = function (referenceId, referenceType) {
            if (checkNotNullEmpty(referenceId)) {
                if (referenceType == "sales") {
                    $scope.salesReferenceId = referenceId;
                } else if (referenceType == "purchase") {
                    $scope.purchaseReferenceId = referenceId;
                } else if (referenceType == "transfer") {
                    $scope.transferReferenceId = referenceId;
                }
            }
        }
    }
]);

ordControllers.controller('OrderDetailCtrl', ['$scope', '$sce', 'ordService', 'ORDER', 'userService', 'domainCfgService',
    'invService', 'entityService', '$timeout', 'requestContext', '$uibModal', 'trnService', 'conversationService', '$window', 'ORDERSTATUSTEXT', 'approvalService', 'RETURNS', 'returnsService', '$q',
    function ($scope, $sce, ordService, ORDER, userService, domainCfgService, invService, entityService, $timeout, requestContext, $uibModal, trnService, conversationService, $window, ORDERSTATUSTEXT, approvalService, RETURNS, returnsService, $q) {
        $scope.ORDER = ORDER;
        $scope.RETURNS = RETURNS;
        $scope.ORDERSTATUSTEXT = ORDERSTATUSTEXT;
        $scope.edit = {mat: false};
        $scope.payOpts = [];
        $scope.packSizes = [];
        $scope.masterIts = [];
        $scope.newStatus = {};
        $scope.vErrs = {};
        $scope.dop = '';
        $scope.saveDisable = false;
        $scope.saveCounter = 0;
        $scope.isShipment = false;
        $scope.isExpand = false;
        $scope.isreadMore = false;
        $scope.limitText = 300;
        $scope.message = "";
        $scope.selection = "orderDetail";
        $scope.selectedMaterialIds = [];
        $scope.shipAvailable = false;
        $scope.view = 'consignment';
        $scope.rsn = '';
        $scope.isBatchItemAvl = false;
        $scope.today = new Date();
        $scope.dates = {};
        $scope.ps = '';
        $scope.dispDeliveryStatus = false;
        const beforeShipStatuses = [ORDER.OPEN, ORDER.PENDING, ORDER.CONFIRMED, ORDER.READY_FOR_DISPATCH];

        $scope.init = function () {
            $scope.order = undefined;
            $scope.statusList = [];
            $scope.administrators = {};
            $scope.vInventory = null;
            $scope.inventory = null;
            $scope.vendor = null;
            $scope.material = undefined;
            $scope.newStatus = {};
            $scope.pmt = {};
            $scope.statusJSON = undefined;
            $scope.aCfg = undefined;
            $scope.oCfg = undefined;
            $scope.dispMap = false;
            $scope.sMTShip = [];
            $scope.oTags = {};
            $scope.approval = {orderId: "", msg: ""};
            $scope.cancelPermission = false;
            $scope.shipPermission = false;
            $scope.createShipmentPermission = false;
            $scope.editPermission = false;
            $scope.confirmPermission = false;
            $scope.allocatePermission = false;
            $scope.reOpenPermission = false;
            if ($scope.lMap) {
                $timeout(function () {
                    $scope.dispMap = true;
                }, 1000);
            }
            if (requestContext.getParam("oid")) {
                $scope.orderId = requestContext.getParam("oid");
            }
            if (requestContext.getParam("map")) {
                $timeout(function () {
                    $scope.dispMap = true;
                }, 1000);
            }
        };
        $scope.init();

        function getMessageCount() {
            $scope.showLoading();
            conversationService.getMessagesByObj('ORDER', $scope.orderId, 0, 1, true).then(function (data) {
                if (checkNotNullEmpty(data.data)) {
                    $scope.messageCnt = data.data.numFound;
                }
            }).catch(function error(msg) {
                $scope.showErrorMsg(msg);
            }).finally(function () {
                $scope.hideLoading();
            });
        }

        getMessageCount();
        $scope.setMessageCount = function (count) {
            $scope.messageCnt = count;
        };
        $scope.showLoading();
        domainCfgService.getOrdersCfg().then(function (data) {
            $scope.oCfg = data.data;
            if (checkNotNullEmpty($scope.oCfg.po)) {
                $scope.payOpts = $scope.oCfg.po.split(',');
                $scope.pmt.po = $scope.payOpts[0];
            }
            $scope.dop = $scope.oCfg.dop;
            if (checkNotNullEmpty($scope.oCfg.ps)) {
                $scope.packageSize = $scope.oCfg.ps.split(',');
                $scope.packageSize.unshift("");
            }
            $scope.newStatusLongFormVisible = (true == $scope.oCfg.tm) || (true == $scope.oCfg.eadm);
        }).catch(function error(msg) {
            $scope.showErrorMsg(msg);
        }).finally(function () {
            $scope.hideLoading();
        });
        $scope.showLoading();
        trnService.getStatusMandatory().then(function (data) {
            $scope.transConfig = data.data;
        }).catch(function error(msg) {
            $scope.showErrorMsg(msg);
        }).finally(function () {
            $scope.hideLoading();
        });
        $scope.showLoading();
        trnService.getMatStatus("i", false).then(function (data) {
            $scope.matstatus = data.data;
        }).catch(function error(msg) {
            $scope.showErrorMsg(msg);
        }).finally(function () {
            $scope.hideLoading();
        });
        $scope.showLoading();
        trnService.getMatStatus("i", true).then(function (data) {
            $scope.tempmatstatus = data.data;
        }).catch(function error(msg) {
            $scope.showErrorMsg(msg);
        }).finally(function () {
            $scope.hideLoading();
        });
        $scope.showLoading();
        domainCfgService.getAccountingCfg().then(function (data) {
            $scope.aCfg = data.data;
        }).catch(function error(msg) {
            $scope.showErrorMsg(msg);
        }).finally(function () {
            $scope.hideLoading();
        });

        $scope.showLoading();
        ordService.getOrderReasons("cor").then(function (data) {
            $scope.cancelReasons = data.data;
            if (checkNotNullEmpty($scope.cancelReasons)) {
                var rsn = "others";
                var isMatched = $scope.cancelReasons.some(function (arval) {
                    return rsn == arval.toLowerCase();
                });
                if (!isMatched) {
                    $scope.cancelReasons = $scope.cancelReasons.concat("Others");
                }
                $scope.cancelReasons.unshift("");
            }
        }).catch(function error(msg) {
            $scope.showErrorMsg(msg);
        }).finally(function () {
            $scope.hideLoading();
        });

        $scope.showLoading();
        ordService.getOrderReasons("eqr").then(function (data) {
            $scope.reasons = data.data;
            if (checkNotNullEmpty($scope.reasons)) {
                var rsn = "others";
                var isMatched = $scope.reasons.some(function (arval) {
                    return rsn == arval.toLowerCase();
                });
                if (!isMatched) {
                    $scope.reasons = $scope.reasons.concat("Others");
                }
                $scope.reasons.unshift("");
            }
        }).catch(function error(msg) {
            $scope.showErrorMsg(msg);
        }).finally(function () {
            $scope.hideLoading();
        });

        $scope.showLoading();
        ordService.getOrderReasons("orr").then(function (data) {
            $scope.oReasons = data.data;
            if (checkNotNullEmpty($scope.oReasons)) {
                var rsn = "others";
                var isMatched = $scope.oReasons.some(function (arval) {
                    return rsn == arval.toLowerCase();
                });
                if (!isMatched) {
                    $scope.oReasons = $scope.oReasons.concat("Others");
                }
                $scope.oReasons.unshift("");
            }
        }).catch(function error(msg) {
            $scope.showErrorMsg(msg);
        }).finally(function () {
            $scope.hideLoading();
        });
        $scope.$watch("edit.mat", function (newVal) {
            if (newVal) {
                $scope.edit.vend = false;
            }
        });

        $scope.setPageSelection = function (page) {
            $scope.selection = page;
        };

        $scope.openView = function (view, selRows, sn) {
            $scope.sMTShip = [];
            if (!sn) {
                $scope.selection = view;
                if (selRows) {
                    for (var i = 0; i < selRows.length; i++) {
                        $scope.sMTShip.push($scope.order.its[selRows[i]]);
                    }
                } else {
                    for (i = 0; i < $scope.order.its.length; i++) {
                        $scope.order.its[i].nq = "";
                        $scope.order.its[i].ta = false;
                    }
                }
            } else {
                var allocateComplete = true;
                $scope.sMTShip = angular.copy($scope.order.its);

                $scope.order.its.forEach(function (data) {
                    if (data.isBa == true) {
                        $scope.isBatchItemAvl = true;
                    }
                });

                $scope.sMTShip.forEach(function (data) {
                    if ($scope.isBatchItemAvl) {
                        if (data.q != data.astk) {
                            allocateComplete = false;
                        }
                    } else {
                        data.astk = data.q
                    }
                });
                if (allocateComplete) {
                    $scope.selection = view;
                } else {
                    $scope.showWarning("Please allocate to all materials before shipping the order.");
                }
            }
            $scope.sMatList = $scope.sMTShip;
        };

        function isOrderPartiallyAllocated() {
            if (!$scope.allocate) {
                return true;
            }
            var nonAllocatedItemCount = 0;
            var noOfValidItems = 0;

            $scope.sMTShip = angular.copy($scope.order.its);
            var dd = $scope.order.its.some(function (data) {
                if (data.isBa && data.q != data.astk && data.q > 0) {
                    return true;
                }
                if (data.q > 0) {
                    noOfValidItems++;
                    if (data.astk == 0) {
                        nonAllocatedItemCount++;
                    } else if (data.q != data.astk && data.astk > 0) {
                        return true;
                    }
                }
            });
            return dd || (nonAllocatedItemCount > 0 && nonAllocatedItemCount != noOfValidItems);
        }

        $scope.toggleEdit = function (field, open) {
            if (field == 'etdate' && open) {
                $scope.dates.edd = parseUrlDate($scope.order.edd);
            } else if (field == 'efdate' && open) {
                $scope.dates.efd = parseUrlDate($scope.order.efd);
            }
            $scope.edit[field] = !$scope.edit[field];
        };
        $scope.toggleBatch = function (type, index) {
            $scope.order.its[index][type] = !$scope.order.its[index][type];
        };
        $scope.expand = function () { // Messagebox expand
            $scope.isExpand = !$scope.isExpand;
            if (!$scope.isExpand) {
                $scope.isreadMore = !$scope.isreadMore;
                $scope.limitText = 300;
            }
        };
        $scope.readMore = function () { // Message body expand
            $scope.isreadMore = !$scope.isreadMore;
            $scope.limitText = $scope.message.length;
        };
        $scope.toggleDiscount = function (index) {
            if (checkNullEmpty($scope.edit.ds)) {
                $scope.edit.ds = {};
            }
            $scope.edit.ds[index] = 1;
        };
        $scope.updateStatusList = function () {
            if (checkNotNullEmpty($scope.statusList)) {
                for (var i = 0; i < $scope.statusList.length; i++) {
                    if ($scope.statusList[i] == ORDER.CANCELLED && !$scope.cancelPermission) {
                        $scope.statusList.splice(i, 1);
                    }
                    if ($scope.statusList[i] == ORDER.CONFIRMED && !$scope.confirmPermission) {
                        $scope.statusList.splice(i, 1);
                    }
                    if ($scope.statusList[i] == ORDER.PENDING && !$scope.reOpenPermission) {
                        $scope.statusList.splice(i, 1);
                    }

                }
            }
        };
        function getShipmentCountWithStatus(statuses) {
            if(checkNotNullEmptyArray($scope.shipmentList)) {
                return $scope.shipmentList.filter(function(s) {
                    return statuses.includes(ORDER[s.status]);
                }).map(function(s) {
                    return s.sId;
                });
            }
            return [];
        }

        $scope.getTrustedTrackingUrl = function() {
            return $sce.trustAsResourceUrl($scope.order.drs[0]['trURL']);
        };
        $scope.toggleDispDeliveryStatus = function() {
            $scope.dispDeliveryStatus = !$scope.dispDeliveryStatus;
        };
        $scope.checkStatusList = function () {
            if ($scope.dp.vp) {
                $scope.statusList = [];
                return;
            }
            var shipmentSize = $scope.shipmentList.length;
            switch ($scope.order.st) {
                case ORDER.PENDING:
                    $scope.statusList = $scope.order.atv ? [ORDER.CONFIRMED] : [];
                    break;
                case ORDER.CONFIRMED:
                    $scope.statusList = $scope.order.atv ? [ORDER.PENDING] : [];
                    break;
                case ORDER.COMPLETED:
                    $scope.statusList = $scope.order.atc && shipmentSize == 1 ? [ORDER.FULFILLED] : [];
                    break;
                default:
                    $scope.statusList = [];
            }


            if(checkNotNullEmpty($scope.order.its) && $scope.order.its.length > 0) {
                if (($scope.order.st == ORDER.PENDING || $scope.order.st == ORDER.CONFIRMED)) {
                    if (getShipmentCountWithStatus(beforeShipStatuses).length <= 1
                            && checkNotNullEmpty($scope.order.vid) && $scope.order.atv) {
                        $scope.statusList.push(ORDER.COMPLETED);
                        if(getShipmentCountWithStatus([ORDER.READY_FOR_DISPATCH]).length == 0) {
                            $scope.statusList.push(ORDER.READY_FOR_DISPATCH);
                        }
                    }
                    if (shipmentSize == 0 && checkNotNullEmpty($scope.order.vid) && checkNotNullEmpty($scope.oCfg) && $scope.oCfg.aof && $scope.order.atc) {
                        $scope.statusList.push(ORDER.FULFILLED);
                    }
                } else if($scope.order.st == ORDER.READY_FOR_DISPATCH
                        && checkNotNullEmpty($scope.order) && checkNotNullEmpty($scope.order.vid)) {
                    if(getShipmentCountWithStatus(beforeShipStatuses).length <= 1 && $scope.order.atv) {
                        $scope.statusList.push(ORDER.COMPLETED);
                        if($scope.oCfg.tspc && checkNullEmptyArray($scope.order.drs)) {
                            $scope.statusList.push(ORDER.REQUEST_DELIVERY);
                        }
                    } else if(checkNullEmptyArray($scope.shipmentList) && $scope.order.atc && !$scope.oCfg.disableDeliveryReqByCustomer) {
                        if($scope.oCfg.tspc && checkNullEmptyArray($scope.order.drs)) {
                            $scope.statusList.push(ORDER.REQUEST_DELIVERY);
                        }
                    }
                }
            }

            if ($scope.order.st != ORDER.CANCELLED && $scope.order.st != ORDER.FULFILLED && ($scope.order.atv || $scope.order.st == ORDER.PENDING) && $scope.order.alc) {
                $scope.statusList.push(ORDER.CANCELLED);
            }
            $scope.showStock = $scope.order.st == $scope.ORDER.PENDING || $scope.order.st == $scope.ORDER.CONFIRMED ||
                $scope.order.st == $scope.ORDER.READY_FOR_DISPATCH || $scope.order.st == $scope.ORDER.BACKORDERED;
            $scope.updateStatusList();

        };
        $scope.resetMsgUsers = function () {
            $scope.newStatus.users = [];
        };
        $scope.toggleFulfil = function (update, incMsgCount) {
            if ($scope.selection == 'orderDetail') {
                $scope.selection = "fulfilShipment";
            } else {
                $scope.selection = 'orderDetail';
                if (update) {
                    $scope.fetchOrder();
                    if (incMsgCount) {
                        $scope.setMessageCount($scope.messageCnt + 1);
                    }
                }
            }
        };
        function isBatchItemAvailable() {
            return $scope.order.its.some(function (data) {
                return data.isBa;
            });
        }

        $scope.changeStatus = function (value) {
            $scope.nStatus = value;
            $scope.newStatus = {customerPickup: false};
            $scope.newStatus.st = value;
            $scope.newStatus.ncdrsn = '';
            $scope.newStatus.cmrsn = '';
            if ($scope.nStatus == ORDER.FULFILLED && $scope.shipmentList.length > 0) {
                ordService.getShipment($scope.shipmentList[0].sId).then(function (data) {
                    $scope.shipment = data.data;
                    $scope.shipment.items.forEach(function (d) {
                        d.bts = angular.copy(d.bq);
                        d.status = d.smst;
                        if (checkNotNullEmpty($scope.matstatus) && checkNullEmpty(d.status)) {
                            d.status = $scope.matstatus[0];
                        }
                    });
                    $scope.toggleFulfil();
                }).catch(function error(msg) {
                    $scope.showErrorMsg(msg);
                }).finally(function () {
                    $scope.hideLoading();
                });
            } else {
                const sIds = getShipmentCountWithStatus([ORDER.OPEN, ORDER.PENDING, ORDER.CONFIRMED, ORDER.READY_FOR_DISPATCH]);
                if(sIds.length == 1) {
                    if($scope.newStatus.st == ORDER.COMPLETED) {
                        $window.location.href = '#/orders/shipment/detail/' + sIds[0] + '?action=ship';
                        return;
                    } else if($scope.newStatus.st == ORDER.REQUEST_DELIVERY) {
                        $window.location.href = '#/orders/shipment/detail/' + sIds[0] + '?action=delivery_request';
                        return;
                    } else if($scope.newStatus.st == ORDER.READY_FOR_DISPATCH) {
                        $window.location.href = '#/orders/shipment/detail/' + sIds[0] + '?action=ready_dispatch';
                        return;
                    }
                }
                if (($scope.newStatus.st == ORDER.COMPLETED || ($scope.order.st != $scope.ORDER.COMPLETED && $scope.newStatus.st == ORDER.FULFILLED)) && isOrderPartiallyAllocated()) {
                    if (isBatchItemAvailable()) {
                        $scope.showWarning($scope.resourceBundle['order.allocations.required.toship']);
                        return false;
                    } else {
                        confirmPartialAllocatedOrder();
                    }
                } else if ($scope.newStatus.st == ORDER.REQUEST_DELIVERY) {
                    if($scope.order.st != ORDER.READY_FOR_DISPATCH) {
                        $scope.showWarning($scope.resourceBundle['move.order.to.rd']);
                        return;
                    } else {
                        $scope.createDeliveryRequest('order');
                    }
                } else {
                    if($scope.newStatus.st == ORDER.COMPLETED) {
                        $scope.newStatus['consignment'] = {}; // create empty object for shipment consignment details
                    }
                    displayStatusChangeForm();
                }
            }
        };

        $scope.createDeliveryRequest = function (type) {
            $scope.selection = "createDelReqs";
            $scope.delReq = {};
            $scope.delReq.type = type;
            $scope.$apply();
        };

        function displayStatusChangeForm() {
            $scope.modalInstance = $uibModal.open({
                templateUrl: 'views/orders/order-status.html',
                scope: $scope,
                keyboard: false,
                backdrop: 'static'
            });
            $scope.disableScroll();
        }

        $scope.$watch("newStatus.tempTransporter", function(newVal, oldVal) {
            if(checkNotNullEmpty(newVal)) {
                if(typeof newVal === 'string') {
                    $scope.newStatus.t = newVal;
                    $scope.newStatus.transporterId = null;
                } else {
                    $scope.newStatus.transporterId = newVal.id;
                    $scope.newStatus.t = newVal.name;
                    $scope.newStatus.phone = newVal.phnm;
                    $scope.newStatus.vehicle = newVal.vhcl;
                }
            } else {
                // make transporter, transporter Id and contact number empty
                $scope.newStatus.transporterId = null;
                $scope.newStatus.t = "";
                $scope.newStatus.phone = "";
                $scope.newStatus.vehicle = "";
            }
        });

        function confirmPartialAllocatedOrder() {
            $scope.msg = $scope.resourceBundle['partial.order.ship.confirm'];
            $scope.modalInstance = $uibModal.open({
                templateUrl: 'views/orders/confirm.html',
                scope: $scope,
                keyboard: false,
                backdrop: 'static'
            });
            $scope.disableScroll();
        }

        $scope.cancel = function () {
            $scope.enableScroll();
            $scope.order.aprmsg = "";
            $scope.modalInstance.dismiss('cancel');
        };

        $scope.confirm = function () {
            $scope.cancel();
            displayStatusChangeForm();
        };
        $scope.toggleMap = function () {
            $scope.dispMap = !$scope.dispMap;
        };
        $scope.validateStatusChange = function () {
            if (checkNull($scope.aCfg)) {
                $scope.showWarning($scope.resourceBundle['order.config.dontchange']);
                return false;
            }
            var enforceCreditCheck = false;
            if (checkNullEmpty($scope.oCfg.dop) && checkNotNullEmpty($scope.aCfg.ea) && checkNotNullEmpty($scope.aCfg.en) &&
                ( ( $scope.aCfg.en == $scope.newStatus.st ) || ( $scope.aCfg.en == ORDER.CONFIRMED && $scope.newStatus.st == ORDER.COMPLETED ) ))
                enforceCreditCheck = true;
            if (enforceCreditCheck && $scope.order.tp > $scope.order.avc) {
                $scope.showWarning($scope.resourceBundle.ordercannotbe + ' ' + ORDER.statusTxt[$scope.newStatus.st].toLowerCase() + '. ' + $scope.resourceBundle.ordercostexceedscredit + ' ' + $scope.order.cur + ' ' + $scope.order.avc);
                return false;
            }
            if ($scope.oCfg.tm && checkNullEmpty($scope.newStatus.t) && ($scope.newStatus.st == $scope.ORDER.COMPLETED || $scope.newStatus.st == ORDER.FULFILLED)) {
                $scope.showWarning($scope.resourceBundle['transportermandatory']);
                return false;
            }
            if ($scope.newStatus.st == ORDER.COMPLETED) {
                var isInvalid = $scope.order.its.some(function (it) {
                    if (it.q > (it.atpstk + it.astk)) {
                        $scope.showWarning($scope.resourceBundle['less.available.stock.toship']);
                        return true;
                    }
                });
                if (isInvalid) {
                    return false;
                }
            }
            return true;
        };
        function updateOrderObj(data, skipSno) {
            if (!skipSno) {
                data.sno = $scope.order.sno;
            }
            $scope.order = data;
            if (checkNotNullEmpty($scope.dates.efd)) {
                $scope.order.isEfdValid = isNotPastDate($scope.dates.efd);
            }
            $scope.sicrsn = $scope.order.crsn;
            $scope.order.its.forEach(function (data) {
                data.nrsn = '';
                data.drsn = '';
                if (data.rq >= 0 && data.oq != data.rq) {
                    var r = '<p align="left">';
                    r += '<span class="codegray"> ' + $scope.resourceBundle['order.recommended'] + ': </span>' + data.rq + '<br/>' + '<span class="codegray"> ' + $scope.resourceBundle['orders.originally.ordered'] + ': </span>' + data.oq;
                    if (checkNotNullEmpty(data.rsn)) {
                        r += '<br/>' + '<span class="codegray"> ' + $scope.resourceBundle['orders.reason.for.ordering.discrepancy'] + ': </span> <br/>' + data.rsn;
                    }
                    data.drsn += r + '</p>';
                }
                if (data.oq > 0 && data.q != data.oq) {
                    var isShipped = $scope.order.st == $scope.ORDER.COMPLETED ||
                        $scope.order.st == $scope.ORDER.FULFILLED;
                    if (!isShipped && checkNotNullEmpty(data.drsn)) {
                        r = '<hr/><p align="left">';
                    } else {
                        r = '<p align="left"><span class="codegray"> ' + $scope.resourceBundle['orders.originally.ordered'] + ': </span>' + data.oq + '<br/>';
                    }
                    r += '<span class="codegray">' + (isShipped ? $scope.resourceBundle['shipped'] + ': ' : $scope.resourceBundle['modified']+ ': ') + '</span>' + data.q;
                    if (checkNotNullEmpty(data.sdrsn)) {
                        r += '<br/> <span class="codegray">' + (isShipped ? $scope.resourceBundle['orders.reason.for.shipping.discrepancy'] + ': ' : $scope.resourceBundle['orders.reason.for.modifying.order.quantity'] + ': ') + '</span> <br/>'
                            + data.sdrsn;
                    }
                    if (isShipped) {
                        data.osdrsn = r + '</p>';
                    } else {
                        data.drsn += r + '</p>';
                    }
                }
                if ($scope.order.st == $scope.ORDER.FULFILLED) {
                    if (data.isBa && $scope.oCfg.agi) {
                        var highlight = false;
                        if (checkNotNullEmpty(data.bts)) {
                            data.bts.forEach(function (data) {
                                var showInfo = false;
                                data.fdrsn = 'Batch Id: ' + data.id + '<br/><table class="table table-condensed table-logistimo"><tr><th>'+ $scope.resourceBundle['shipment'] + '</th><th>' + $scope.resourceBundle['shipped'] + '</th><th>' + $scope.resourceBundle['fulfilled'] + '</th><th>' + $scope.resourceBundle['reason'] + '</th></tr>';
                                data.bd.forEach(function (d) {
                                    if (d.fq != d.q) {
                                        data.fdrsn += '<tr><td>' + d.sid + '</td><td>' + d.q + '</td><td class="fc-color-red">' + d.fq + '</td><td>' + (d.frsn || '') + '</td></tr>';
                                        showInfo = true;
                                        highlight = true;
                                    } else {
                                        data.fdrsn += '<tr><td>' + d.sid + '</td><td>' + d.q + '</td><td>' + d.fq + '</td><td>' + (d.frsn || '') + '</td></tr>';
                                    }
                                });
                                data.fdrsn += '</table>';
                                if (!showInfo) {
                                    data.fdrsn = '';
                                }
                            });
                        }
                        if (highlight) {
                            data.isDisc = true;
                        }
                    } else {
                        if (checkNotNullEmpty(data.bd)) {
                            var showInfo = false;
                            data.fdrsn = '<table class="table table-condensed table-logistimo"><tr><th>' + $scope.resourceBundle['shipment'] + '</th><th>' + $scope.resourceBundle['shipped'] + '</th><th>' + $scope.resourceBundle['fulfilled'] + '</th><th>' + $scope.resourceBundle['reason'] + '</th></tr>';
                            data.bd.forEach(function (d) {
                                if (d.q != d.fq) {
                                    data.fdrsn += '<tr><td>' + d.sid + '</td><td>' + d.q + '</td><td class="fc-color-red">' + d.fq + '</td><td>' + (d.frsn || '') + '</td></tr>';
                                    showInfo = true;
                                } else {
                                    data.fdrsn += '<tr><td>' + d.sid + '</td><td>' + d.q + '</td><td>' + d.fq + '</td><td>' + (d.frsn || '') + '</td></tr>';
                                }
                            });
                            data.fdrsn += '</table>';
                            if (!showInfo) {
                                data.fdrsn = '';
                            }
                            data.isDisc = showInfo;
                        }
                    }
                }
            });
            if ($scope.returnPolicyDuration == undefined) {
                $scope.showLoading();
                domainCfgService.getReturnConfig($scope.order.vid).then(function (data) {
                    $scope.returnPolicyDuration = data.data.incDur || 0;
                }).catch(function error(msg) {
                    $scope.showErrorMsg(msg);
                }).finally(function () {
                    $scope.hideLoading();
                    $scope.checkReturn();
                });
            } else {
                $scope.checkReturn();
            }

        }

        $scope.saveStatus = function () {
            if ($scope.validateStatusChange()) {
                if ($scope.newStatus.st == ORDER.CANCELLED) {
                    if ($scope.oCfg.corm || $scope.newStatus.ncdrsn == 'Others') {
                        if ((checkNullEmpty($scope.oCfg.cor) || checkNullEmpty($scope.newStatus.ncdrsn) || $scope.newStatus.ncdrsn == 'Others')
                            && checkNullEmpty($scope.newStatus.cmrsn)) {
                            $scope.showWarning($scope.resourceBundle["provide.reason"]);
                            return;
                        }
                    }
                    if (checkNullEmpty($scope.oCfg.cor) || $scope.newStatus.ncdrsn == 'Others') {
                        $scope.newStatus.cdrsn = $scope.newStatus.cmrsn;
                    } else {
                        $scope.newStatus.cdrsn = $scope.newStatus.ncdrsn;
                    }
                } else if ($scope.newStatus.st == ORDER.COMPLETED) {
                    if ($scope.oCfg.ridm && (checkNullEmpty($scope.newStatus.salesRefId) || checkNullEmpty($scope.newStatus.salesRefId.trim()))) {
                        $scope.showWarning($scope.resourceBundle['sales.reference.id.mandatory.error']);
                        return;
                    } else if ($scope.oCfg.eadm && checkNullEmpty($scope.newStatus.efd)) {
                        $scope.showWarning($scope.resourceBundle['estimated.date.of.arrival.mandatory.error']);
                        return;
                    }
                }
                $scope.newStatus.efd = checkNotNullEmpty($scope.newStatus.efd) ? formatDate($scope.newStatus.efd) : undefined;
                $scope.newStatus.orderUpdatedAt = $scope.order.orderUpdatedAt;
                $scope.statusLoading = true;
                $scope.showLoading();
                ordService.updateOrderStatus($scope.order.id, $scope.newStatus).then(function (data) {
                    $scope.fetchOrder();
                    var upMsg = {};
                    upMsg.objId = $scope.order.id;
                    upMsg.objType = "ORDER";
                    upMsg.offset = 0;
                    getMessageCount();
                    $scope.$broadcast("updateMessage", upMsg);
                    var refreshMsg;
                    $scope.cancel();
                    $scope.newStatus = {};
                    var accountUpdatedMsg;
                    if (checkNullEmpty($scope.oCfg.dop) && $scope.aCfg && $scope.aCfg.ea && $scope.order.st == "cm" && $scope.order.tp > 0) {
                        accountUpdatedMsg = $scope.resourceBundle["accountupdatedmsg"] + " <b>" + $scope.order.pst + "</b>";
                        if (refreshMsg)
                            accountUpdatedMsg += "<br/><br/><b>" + refreshMsg + "<b>";
                    }
                    $scope.displayStatus(data, accountUpdatedMsg);
                }).catch(function error(msg) {
                    $scope.showErrorMsg(msg);
                }).finally(function () {
                    $scope.statusLoading = false;
                    $scope.hideLoading();
                });
            }
        };
        $scope.saveVendor = function (vend) {
            if (checkNotNullEmpty(vend)) {
                $scope.vendor = vend;
            }
            if (checkNullEmpty($scope.vendor) || checkNullEmpty($scope.vendor.id)) {
                $scope.showWarning($scope.resourceBundle['order.specify.vendor']);
                return;
            }
            $scope.showLoading();
            ordService.updateVendor($scope.order.id, $scope.vendor.id, $scope.order.orderUpdatedAt)
                .then(function (data) {
                    updateOrderObj(data.data.order);
                    $scope.toggleEdit('vend');
                    $scope.showSuccess($scope.resourceBundle['order.upd.vendor.success']);
                    $scope.updatePermissions();
                    $scope.checkStatusList();
                }).catch(function error(msg) {
                $scope.showErrorMsg(msg);
            }).finally(function () {
                $scope.hideLoading();
            });
        };

        $scope.updateExpectedFulfillmentDate = function () {
            if (checkNullEmpty($scope.dates.efd) && checkNullEmpty($scope.efdt)) {
                $scope.showWarning("Please select a date");
                return;
            }
            if (checkNullEmpty($scope.dates.efd)) {
                $scope.efdt = '';
            } else {
                $scope.efdt = formatDate($scope.dates.efd);
            }
            $scope.showLoading();
            ordService.updateExpectedFulfillmentDate($scope.order.id, $scope.efdt, $scope.order.orderUpdatedAt).then(function (data) {
                $scope.order.efdLabel = data.data.respData;
                $scope.order.orderUpdatedAt = data.data.order.orderUpdatedAt;
                $scope.order.efd = $scope.dates.efd;
                if (checkNotNullEmpty($scope.dates.efd)) {
                    $scope.order.isEfdValid = isNotPastDate($scope.dates.efd);
                }
                $scope.toggleEdit('efdate');
                $scope.showSuccess("<b>" + $scope.resourceBundle['eda'] + "</b> " + $scope.resourceBundle['updated.successfully.lowercase']);
            }).catch(function error(msg) {
                $scope.showErrorMsg(msg);
            }).finally(function () {
                $scope.hideLoading();
            });
        };

        $scope.updateDueDate = function () {
            if (checkNullEmpty($scope.dates.edd) && checkNullEmpty($scope.eddt)) {
                $scope.showWarning("Please select a date");
                return;
            }
            if (checkNullEmpty($scope.dates.edd)) {
                $scope.eddt = '';
            } else {
                $scope.eddt = formatDate($scope.dates.edd);
            }
            $scope.showLoading();
            ordService.updateDueDate($scope.order.id, $scope.eddt, $scope.order.orderUpdatedAt)
                .then(function (data) {
                    $scope.order.eddLabel = data.data.respData;
                    $scope.order.edd = $scope.dates.edd;
                    $scope.order.orderUpdatedAt = data.data.order.orderUpdatedAt;
                    $scope.toggleEdit('etdate');
                    $scope.showSuccess("<b>" + $scope.resourceBundle['required.bydate'] + "</b> " + $scope.resourceBundle['updated.successfully.lowercase']);
                }).catch(function error(msg) {
                $scope.showErrorMsg(msg);
            }).finally(function () {
                $scope.hideLoading();
            });
        };

        $scope.updateRefId = function (value, referenceType) {
            if (checkNullEmpty(value) && referenceType == 'purchaseRefId' && $scope.oCfg.purchase) {
                $scope.showWarning($scope.resourceBundle['purchase.reference.id.mandatory.error']);
                return;
            } else if (checkNullEmpty(value) && referenceType == 'transferRefId' && $scope.oCfg.transfer) {
                if ($scope.transRelease) {
                    $scope.showWarning($scope.resourceBundle['release.reference.id.mandatory.error']);
                } else {
                    $scope.showWarning($scope.resourceBundle['transfer.reference.id.mandatory.error']);
                }
                return;
            }

            $scope.showLoading();
            ordService.updateReferenceID($scope.order.id, value, $scope.order.orderUpdatedAt, referenceType)
                .then(function (data) {
                    $scope.order.salesRefId = data.data.order.salesRefId;
                    $scope.order.purchaseRefId = data.data.order.purchaseRefId;
                    $scope.order.transferRefId = data.data.order.transferRefId;
                    $scope.order.orderUpdatedAt = data.data.order.orderUpdatedAt;
                    $scope.showSuccess("<b>" + $scope.resourceBundle['orders.reference.id'] + " </b> " + $scope.resourceBundle['updated.successfully.lowercase']);
                }).catch(function (errorMsg) {
                $scope.showErrorMsg(errorMsg);
            }).finally(function () {
                    $scope.toggleEdit(referenceType);
                $scope.hideLoading();
            });
        };

        $scope.editPayment = function () {
            $scope.pmt.pay = $scope.order.tp - $scope.order.pd;
            $scope.toggleEdit('pmt');
        };
        $scope.validatePayment = function () {
            if (checkNullEmpty($scope.pmt.pay)) {
                $scope.showWarning($scope.resourceBundle.nopayment);
                return false;
            }
            if (( $scope.order.pd + $scope.pmt.pay ) > $scope.order.tp) {
                if (!confirm($scope.resourceBundle.paymenthigher))
                    return false;
            }
            return true;
        };
        $scope.savePayment = function () {
            if ($scope.validatePayment()) {
                $scope.showLoading();
                $scope.pmt.orderUpdatedAt = $scope.order.orderUpdatedAt;
                ordService.updatePayment($scope.order.id, $scope.pmt).then(function (data) {
                    $scope.fetchOrder();
                    $scope.toggleEdit('pmt');
                    $scope.showSuccess($scope.resourceBundle['order.upd.pay.success']);
                }).catch(function error(msg) {
                    $scope.showErrorMsg(msg);
                }).finally(function () {
                    $scope.hideLoading();
                });
            }
        };
        $scope.editRID = function (type) {
            switch (type) {
                case "salesRefId":
                    $scope.order.tempSalesRefId = $scope.order.salesRefId;
                    break;
                case "purchaseRefId":
                    $scope.order.tempPurchaseRefId = $scope.order.purchaseRefId;
                    break;
                case "transferRefId":
                    $scope.order.tempTransferRefId = $scope.order.transferRefId;
                    break;
            }
            $scope.toggleEdit(type);
        };
        $scope.editTags = function () {
            $scope.oTags.tag = [];
            $scope.order.tgs.forEach(function (data) {
                $scope.oTags.tag.push({"id": data, "text": data});
            });
            $scope.toggleEdit("otags");
        };
        $scope.saveTags = function (oTags) {
            if (oTags != $scope.order.tgs) {
                var tags = "";
                var ordTags = [];
                var i = 0;
                oTags.forEach(function (data) {
                    if (checkNotNullEmpty(tags)) {
                        tags = tags.concat(",");
                    }
                    tags = tags.concat(data.text);
                    ordTags[i++] = data.text;
                });
                $scope.showLoading();
                ordService.updateOrderTags($scope.order.id, tags, $scope.order.orderUpdatedAt).then(function (data) {
                    $scope.order.orderUpdatedAt = data.data.order.orderUpdatedAt;
                    $scope.order.tgs = ordTags;
                    $scope.toggleEdit("otags");
                    $scope.showSuccess($scope.resourceBundle['order.tags.updated.successfully']);
                }).catch(function (errorMsg) {
                    $scope.showErrorMsg(errorMsg);
                }).finally(function () {
                    $scope.hideLoading();
                });
            }
        };
        $scope.editPackage = function () {
            $scope.pkgs = $scope.order.pkgs || "";
            $scope.toggleEdit("pkgs");
        };
        $scope.savePackage = function () {
            $scope.showLoading();
            ordService.updatePackage($scope.order.id, $scope.pkgs).then(function (data) {
                $scope.order.pkgs = $scope.pkgs;
                $scope.toggleEdit('pkgs');
                $scope.showSuccess($scope.resourceBundle['order.pkg.success']);
            }).catch(function error(msg) {
                $scope.showErrorMsg(msg);
            }).finally(function () {
                $scope.hideLoading();
            });
        };

        $scope.updatePermissions = function () {
            $scope.cancelPermission = false;
            $scope.shipPermission = false;
            $scope.createShipmentPermission = false;
            $scope.editPermission = false;
            $scope.confirmPermission = false;
            $scope.allocatePermission = false;
            $scope.reOpenPermission = false;
            $scope.editMetaDataPermission = false;
            if (checkNotNullEmpty($scope.order.permissions) &&
                checkNotNullEmpty($scope.order.permissions.permissions) && !$scope.dp.vp) {
                $scope.order.permissions.permissions.forEach(function (data) {
                    if (data == 'cancel') {
                        $scope.cancelPermission = true;
                    }
                    if (data == 'confirm') {
                        $scope.confirmPermission = true;
                    }
                    if (data == 'allocate') {
                        $scope.allocatePermission = true;
                    }
                    if (data == 'ship') {
                        $scope.shipPermission = true;
                    }
                    if(data == 'create_shipment') {
                        $scope.createShipmentPermission = true;
                    }
                    if (data == 'edit') {
                        $scope.editPermission = true;
                    }
                    if (data == 'reopen') {
                        $scope.reOpenPermission = true;
                    }
                    if (data == 'edit_meta_data') {
                        $scope.editMetaDataPermission = true;
                    }
                });
            }
        };

        $scope.fetchOrder = function () {
            $scope.loading = true;
            $scope.admins = false;
            $scope.aprMsg = false;
            $scope.aprDetail = "";
            if ($scope.orderId) {
                var count = 0;
                $scope.showLoading();
                ordService.getOrder($scope.orderId).then(function (data) {
                    if (checkNotNullEmpty(data.data)) {
                        if ($scope.showOrderFilter && $scope.entityId != data.data.eid && $scope.entityId != data.data.vid) {
                            $scope.errMsg = $scope.resourceBundle['ord.invalid.entity'];
                            return;
                        }
                        updateOrderObj(data.data, true);
                        $scope.updatePermissions();
                        count += 1;
                        callCheckStatus(count);
                        $scope.order.sno = $scope.sno;
                        $scope.efdt = $scope.dates.efd = parseUrlDate($scope.order.efd);
                        if (checkNotNullEmpty($scope.dates.efd)) {
                            $scope.order.isEfdValid = isNotPastDate($scope.dates.efd);
                        }
                        $scope.eddt = $scope.dates.edd = parseUrlDate($scope.order.edd);
                        if (checkNotNullEmpty($scope.order.vid) && $scope.order.vid > 0) {
                            $scope.vendor = {id: $scope.order.vid, nm: $scope.order.vnm};
                        }
                        if ($scope.order.appr && checkNullEmpty($scope.order.pa)) {
                            $scope.aprMsg = true;
                        }
                        userService.getUsersByRole('ROLE_do').then(function (data) {
                            $scope.administrators = data.data.results;
                            if (checkNotNullEmpty($scope.administrators) && $scope.administrators.length > 0) {
                                $scope.admins = true;
                            }
                        }).catch(function error(msg) {
                            $scope.showErrorMsg(msg);
                        });
                        $scope.getStatusHistory();

                    }
                }).catch(function error(msg) {
                    if (checkNotNullEmpty(msg.data) && checkNotNullEmpty(msg.data.message) && msg.data.message.startsWith("w:")) {
                        $scope.errMsg = msg.data.message.substr(2);
                    } else {
                        $scope.showErrorMsg(msg);
                        $scope.errMsg = msg.data.message;
                    }
                    $scope.init();
                }).finally(function () {
                    $scope.loading = false;
                    $scope.hideLoading();
                });

                $scope.showLoading();
                ordService.getShipmentsByOrderId($scope.orderId).then(function (data) {
                    $scope.shipmentList = data.data;
                    if (data.data.length > 0) {
                        $scope.setShipmentAvailable(data.data.length);
                    }
                    count += 1;
                    callCheckStatus(count);
                }).catch(function error(msg) {
                    $scope.showErrorMsg(msg);
                }).finally(function () {
                    $scope.hideLoading();
                });

                $scope.showLoading();
                returnsService.getAll({orderId: $scope.orderId}).then(function (data) {
                    $scope.returnsList = data.data.returns;
                }).catch(function error(msg) {
                    $scope.showErrorMsg(msg);
                }).finally(function () {
                    $scope.hideLoading();
                });

                function callCheckStatus(count) {
                    if (count == 2) {
                        $scope.checkStatusList();
                    }
                }
            }
        };

        $scope.getStatusHistory = function () {
            $scope.showLoading();
            ordService.getStatusHistory(null, null, 'ORDER:' + $scope.orderId).then(function (data) {
                if (checkNotNullEmpty(data.data) && checkNotNullEmpty(data.data.results)) {
                    $scope.history = data.data.results;
                    var hMap = {};
                    var pVal;
                    $scope.history.forEach(function (data) {
                        if (data.objectType == 'ORDER' && checkNullEmpty(hMap[data.newValue])) {
                            hMap[data.newValue] = {
                                "status": ORDER.statusTxt[data.newValue],
                                "updatedon": data.createDate,
                                "updatedby": data.userName,
                                "updatedId": data.userId
                            };
                            if (ORDER.CANCELLED == data.newValue) {
                                pVal = data.prevValue;
                            }
                        }
                    });
                    $scope.si = [];
                    var end = false;
                    var siInd = 0;

                    function constructStatus(stCode, stText) {
                        if ($scope.order.st != ORDER.CANCELLED || !end) {
                            $scope.si[siInd] = (!end && hMap[stCode]) ? hMap[stCode] : {
                                "status": stText,
                                "updatedon": "",
                                "updatedby": ""
                            };
                            $scope.si[siInd].completed = $scope.order.st == stCode ? "end" : (end ? "false" : "true");
                            siInd += 1;
                        }
                        if (!end) {
                            end = $scope.order.st == stCode || ($scope.order.st == ORDER.CANCELLED && pVal == stCode);
                        }
                    }

                    constructStatus(ORDER.PENDING, ORDER.statusTxt[ORDER.PENDING]);
                    if ($scope.order.st == ORDER.PENDING || hMap[ORDER.CONFIRMED]) {
                        constructStatus(ORDER.CONFIRMED, ORDER.statusTxt[ORDER.CONFIRMED]);
                    }
                    if($scope.order.st == ORDER.PENDING || $scope.order.st == ORDER.CONFIRMED || hMap[ORDER.READY_FOR_DISPATCH]) {
                        constructStatus(ORDER.READY_FOR_DISPATCH, ORDER.statusTxt[ORDER.READY_FOR_DISPATCH]);
                    }
                    if (!end && hMap[ORDER.BACKORDERED]) {
                        constructStatus(ORDER.BACKORDERED, ORDER.statusTxt[ORDER.BACKORDERED]);
                    }
                    constructStatus(ORDER.COMPLETED, ORDER.statusTxt[ORDER.COMPLETED]);
                    constructStatus(ORDER.FULFILLED, ORDER.statusTxt[ORDER.FULFILLED]);
                    if ($scope.order.st == ORDER.CANCELLED) {
                        $scope.si[siInd] = hMap[ORDER.CANCELLED];
                        $scope.si[siInd].completed = "cancel";
                    }
                    if ($scope.order.st == ORDER.COMPLETED) {
                        $scope.shipDate = string2Date(hMap[ORDER.COMPLETED].updatedon, "dd/mm/yyyy", "/", true);
                    }
                }
            }).finally(function () {
                $scope.hideLoading();
            });
        };
        if (checkNullEmpty($scope.order) || $scope.order.id != $scope.orderId) {
            $scope.fetchOrder();
        }

        $scope.shouldShowStatusChange = function () {
            if (checkNotNullEmpty($scope.statusList)) {
                return $scope.statusList.some(function (status) {
                    return !$scope.checkHideStatus(status);
                })
            }
            return false;
        };

        $scope.checkHideStatus = function (status) {
            return (checkNotNullEmpty(status) && (status == 'cm' || status == 'fl'));
        };
        $scope.hasStatus = function (status) {
            return (checkNotNullEmpty($scope.statusList) && $scope.statusList.indexOf(status) > -1);
        };


        $scope.getInventory = function (mIds) {
            if (checkNotNullEmpty($scope.order.eid) && checkNotNullEmpty(mIds)) {
                $scope.showLoading();
                return invService.getInventoryByMaterial($scope.order.eid, mIds).then(function (data) {
                    var sugInvMaterial = angular.copy(data.data.results);
                    var inventory = data.data.results;
                    var diMap = [];
                    inventory.forEach(function (di) {
                        diMap[di.mId] = di;
                    });
                    $scope.order.its.forEach(function (it) {
                        var item = diMap[it.id];
                        if (checkNotNull(item)) {
                            item.hide = true;
                            it.inv = item;
                        }
                    });
                    return sugInvMaterial;
                }).catch(function error(msg) {
                    $scope.showErrorMsg(msg);
                }).finally(function () {
                    $scope.hideLoading();
                });
            }
        };
        $scope.addRow = function (material) {
            if (checkNotNull(material)) {
                if ($scope.order.atv) {
                    $scope.getInventory([material.mId]).then(function (data) {
                            if (data.length == 0) {
                                $scope.showWarning("Cannot add " + material.mnm + ". It is not available to customer.");
                                return;
                            }
                            continueAddRow(data[0]).then(function(data) {
                                var nr = $scope.order.its[$scope.order.its.length - 1];
                                nr.vmin = material.reord;
                                nr.vmax = material.max;
                                nr.vevent = material.event;
                                nr.isBa = material.be;
                                nr.itstk = material.tstk;
                            });
                        }
                    );
                    material.dInv = {"stk": material.stk};

                } else {
                    continueAddRow(material);
                }
            }
        };
        var continueAddRow = (material) =>  {
            let deferred = $q.defer();
            $scope.validBatchStock = undefined;
            if (material.be) {
                $scope.setValidBatchQuantity($scope.order.eid, material).then(function() {
                    deferred.resolve();
                });
            } else {
                addMaterial(material);
                deferred.resolve();
            }
            return deferred.promise;
        };

        function addMaterial(material) {
            var vs = checkNotNull(material.dInv) ? material.dInv.stk : 0;
            var isBn = material.b == "yes";
            var status = undefined;
            if (material.ts) {
                status = checkNotNullEmpty($scope.tempmatstatus) ? $scope.tempmatstatus[0] : undefined;
            } else {
                status = checkNotNullEmpty($scope.matstatus) ? $scope.matstatus[0] : undefined;
            }
            var newItem = {
                nm: material.mnm,
                id: material.mId,
                q: Math.max(getOptimalOrderQuantity(material), 0),
                p: material.rp,
                tx: material.tx,
                event: material.event,
                oq: 0,
                vs: vs,
                d: 0,
                isBa: material.be,
                isBn: isBn,
                added: true,
                inv: material,
                min: material.reord,
                max: material.max,
                atpstk: material.atpstk,
                itstk: material.tstk,
                csavibper: material.sap,
                dInv: {stk: "0", event: -1},
                rq: getOptimalOrderQuantity(material),
                stk: material.stk,
                nrsn: '',
                qq: Math.max(getOptimalOrderQuantity(material), 0),
                tm: material.ts,
                mst: status
            };
            $scope.order.its.push(newItem);
            if (checkNotNullEmpty($scope.order.vid)) {
                $scope.getVendInventory([material.mId]);
            }
        }

        function getOptimalOrderQuantity(material) {
            var opoq = -1;
            var stock = material.stk;
            if (checkNotNull(material.validBatchStock)) {
                stock = material.validBatchStock;
            }
            if (material.im == 'sq') {
                opoq = material.eoq;
            } else {
                if (material.max > 0 && (stock + material.tstk) >= material.max) {
                    opoq = 0;
                } else if (material.max > 0) {
                    opoq = material.max - stock - material.tstk;
                    opoq.toFixed(1);
                }
            }
            return opoq.toFixed(0);
        }

        $scope.setValidBatchQuantity = (eid, material)  => {
            let deferred = $q.defer();
            $scope.showLoading();
            invService.getBatchDetail(material.mId, eid, false).then(function (data) {
                var batchDet = data.data;
                var totalStock = 0;
                if (checkNotNullEmpty(batchDet)) {
                    for (var i = 0; i < batchDet.length; i++) {
                        if (!batchDet[i].isExp) {
                            totalStock = totalStock + batchDet[i].q;
                        }
                    }
                }
                material.validBatchStock = totalStock;
                addMaterial(material);
                deferred.resolve();
            }).catch(function error(msg) {
                $scope.showErrorMsg(msg);
            }).finally(function () {
                $scope.hideLoading();
            });
            return deferred.promise;
        };

        $scope.removeRow = function (index) {
            $scope.exRow.splice(index, 1);
            $scope.order.its.splice(index, 1);
        };
        $scope.startEditMats = function (isAllocation) {
            angular.copy($scope.order.its, $scope.masterIts);
            if (checkNotNullEmpty($scope.order.its)) {
                $scope.order.its.forEach(function (its) {
                    its.nastk = its.astk;
                    its.mrsn = null;
                    its.ts = its.tm;
                    if ($scope.order.atv) {
                        if (checkNullEmpty($scope.reasons)) {
                            its.mrsn = its.sdrsn;
                        } else {
                            if (checkNotNullEmpty(its.sdrsn) && $scope.reasons.indexOf(its.sdrsn) == -1) {
                                its.nrsn = "Others";
                                its.mrsn = its.sdrsn;
                            } else {
                                its.nrsn = its.sdrsn;
                            }
                        }
                    } else {
                        if (checkNullEmpty($scope.oReasons)) {
                            its.mrsn = its.rsn;
                        } else {
                            if (checkNotNullEmpty(its.rsn) && $scope.oReasons.indexOf(its.rsn) == -1) {
                                its.nrsn = "Others";
                                its.mrsn = its.rsn;
                            } else {
                                its.nrsn = its.rsn;
                            }
                        }
                    }
                    if (checkNullEmpty(its.mst) && !its.isBa) {
                        if (its.tm) {
                            its.mst = checkNotNullEmpty($scope.tempmatstatus) ? $scope.tempmatstatus[0] : undefined;
                        } else {
                            its.mst = checkNotNullEmpty($scope.matstatus) ? $scope.matstatus[0] : undefined;
                        }
                    }
                });
            }
            $scope.toggleEdit('mat');
            $scope.invalidPopup = 0;
            $scope.isAllocating = isAllocation;
        };
        $scope.cancelEditMats = function () {
            $scope.saveCounter = 0;
            $scope.saveDisable = false;
            angular.copy($scope.masterIts, $scope.order.its);
            for (var i in $scope.exRow) {
                $scope.exRow[i] = false;
            }
            $scope.toggleEdit('mat');
        };

        $scope.saveMaterials = function () {
            if ($scope.validateMaterials()) {
                $scope.showLoading(false, $scope.resourceBundle['order.saving.materials.message']);
                $scope.newIts = angular.copy($scope.order.its);
                $scope.newIts.forEach(function (its) {
                    if (checkNotNullEmpty(its.nrsn) && its.nrsn != "Others") {
                        its.mrsn = undefined;
                    }
                    if (its.added || !$scope.order.atv) {
                        if (its.rq >= 0 && its.rq != its.q) {
                            its.rsn = its.mrsn || its.nrsn || null;
                        } else {
                            its.rsn = null;
                        }
                    } else {
                        if (its.oq > 0 && its.q != its.oq) {
                            its.sdrsn = its.mrsn || its.nrsn || null;
                        } else {
                            its.sdrsn = null;
                        }
                    }
                    its.astk = its.nastk || 0;
                    if (!$scope.order.atv || its.q == its.sq) {
                        its.astk = undefined;
                    }
                    if (!$scope.order.atv) {
                        its.oq = its.q;
                    }
                    if (checkNotNullEmpty(its.bts)) {
                        its.bts.forEach(function (d) {
                            d.q = d.q || 0;
                            d.fq = d.fq || 0;
                            d.mst = d.q > 0 ? d.mst : undefined;
                        });
                    }
                });
                ordService.updateMaterials($scope.order.id, {
                    'items': $scope.newIts,
                    'msg': $scope.msg,
                    'orderUpdatedAt': $scope.order.orderUpdatedAt
                }).then(function (data) {
                    $scope.order.its = angular.copy($scope.newIts);
                    var prevStatus = $scope.order.st;
                    updateOrderObj(data.data.order);
                    if (prevStatus != $scope.order.st) {
                        $scope.checkStatusList();
                        $scope.getStatusHistory();
                    }
                    $scope.batchQ = {};
                    $scope.edit.ds = {};
                    $scope.toggleEdit('mat');
                    $scope.showSuccess($scope.resourceBundle['order.mat.success']);
                }).catch(function error(msg) {
                    $scope.showErrorMsg(msg);
                }).finally(function () {
                    $scope.hideLoading();
                });
            }
        };

        $scope.batchQ = {};

        $scope.saveBatchQ = function (index) {
            $scope.batchQ[index] = $scope.order.its[index].q;
        };

        $scope.validateBatchQ = function (index) {
            var it = $scope.order.its[index];
            var zeroq = false;
            if (it.q == 0) {
                zeroq = true;
            }
            if (it.isBa && $scope.batchQ[index] != it.q && checkNotNullEmpty(it.bts)) {
                if ((zeroq && confirm($scope.resourceBundle['order.batchq.changed.tozero'])) || (!zeroq && confirm($scope.resourceBundle['order.batchq.changed']))) {
                    it.bts = undefined;
                    it.oastk = 0;
                    it.nastk = 0;
                    it.astk = 0;
                    $scope.exRow[index] = true;
                } else {
                    it.q = $scope.batchQ[index];
                }
            }
            if (it.isBa && it.q < it.sq) {
                showPopup($scope, it, it.id, messageFormat($scope.resourceBundle['shipment.items.already.shipped'], it.sq, it.nm), index, $timeout);
            }
            var maxQ = it.oastk - it.astk + (it.nastk * 1) + it.sq;
            if (it.q < maxQ) {
                showPopup($scope, it, it.id, $scope.resourceBundle['shipment.quantity.cannot.be.less.than.allocated.stock'], index, $timeout);
                return false;
            }
        };

        $scope.validateMaterials = function () {
            var removeRows = [];
            var allzero = true;
            var ind = 0;
            for (var i in $scope.order.its) {
                var it = $scope.order.its[i];
                if (checkNullEmpty(it.d)) {
                    it.d = 0;
                }
                if (checkNullEmpty(it.q)) {
                    it.q = 0;
                }
                if (it.added) {
                    if (it.q == 0) {
                        removeRows.push(i);
                    }
                }
                if (allzero) {
                    allzero = (it.q == 0);
                }
                if ($scope.order.atv && it.oq != 0 && it.q != it.oq) {
                    if ($scope.oCfg.eqrm || (checkNotNullEmpty(it.nrsn) && it.nrsn.toLowerCase() == "others")) {
                        if ((checkNullEmpty($scope.oCfg.eqr) || checkNullEmpty(it.nrsn) || it.nrsn.toLowerCase() == 'others')
                            && checkNullEmpty(it.mrsn)) {
                            $scope.showWarning(messageFormat($scope.resourceBundle['provide.ordrsn'], it.nm));
                            return;
                        }
                    }
                } else if ((it.added || !$scope.order.atv) && (it.q != it.rq && it.rq != -1)) {
                    if ($scope.oCfg.orrm || (checkNotNullEmpty(it.nrsn) && it.nrsn.toLowerCase() == "others")) {
                        if ((checkNullEmpty($scope.oCfg.orr) || checkNullEmpty(it.nrsn) || it.nrsn.toLowerCase() == 'others')
                            && checkNullEmpty(it.mrsn)) {
                            $scope.showWarning(messageFormat($scope.resourceBundle['ordrsn.warning'], it.nm));
                            return;
                        }
                    }
                }
                if (!$scope.validate(it, i, 's', $scope.isAllocating)) {
                    return false;
                }
                if (!it.isBa) {
                    var status = it.tm ? $scope.tempmatstatus : $scope.matstatus;
                    it.isVisitedStatus = true;
                    if (checkNotNullEmpty(status) && $scope.transConfig.ism && !$scope.validateStatus(it, i, it.tm ? "cmt" : "cm")) {
                        ind++;
                    }
                }
            }
            if (allzero) {
                $scope.showWarning($scope.resourceBundle['allitemszeromsg']);
                return false;
            }
            for (i in removeRows) {
                $scope.removeRow(removeRows[i]);
            }
            return (!(ind > 0));
        };

        $scope.validate = function (material, index, source, allocate) {
            if (!material.isBn) {
                material.isVisited = material.isVisited || checkNotNullEmpty(source);
                if (material.isVisited) {
                    if (!allocate) {
                        if (checkNotNull(material.sno) && !(material.q >= 0)) {
                            showPopup($scope, material, material.id, messageFormat($scope.resourceBundle['transaction.invalid.quantity'], material.nm), index, $timeout);
                            return false;
                        }
                        if (material.added && material.q == 0) {
                            showPopup($scope, material, material.id, messageFormat($scope.resourceBundle['transaction.invalid.quantity'], material.nm), index, $timeout);
                            return false;
                        }
                        if (checkNotNullEmpty(material.huName) && checkNotNullEmpty(material.huQty) && checkNotNullEmpty(material.q) && material.q % material.huQty != 0) {
                            showPopup($scope, material, material.id, messageFormat($scope.resourceBundle['transaction.handling.units.mismatch'], material.q, material.nm, material.huName, material.huQty, material.nm), index, $timeout);
                            return false;
                        }
                        if (checkNotNullEmpty(material.q) && checkNotNullEmpty(material.sq)) {
                            if (material.q < material.sq) {
                                showPopup($scope, material, material.id, messageFormat($scope.resourceBundle['shipment.items.already.shipped'], material.sq, material.nm), index, $timeout, allocate);
                                return false;
                            }
                        }
                        var nastk = material.nastk ? material.nastk * 1 : 0;
                        var allocStk = material.oastk - material.astk + nastk;
                        var isq = material.isq * 1;
                        material.q = material.q * 1;
                        if (allocStk + isq > material.q) {
                            showPopup($scope, material, material.id, $scope.resourceBundle['order.ordered.quantity.cannot.be.less.than.allocated.quantity'], index, $timeout, allocate);
                            return false;
                        } else if (material.q < isq) {
                            showPopup($scope, material, material.id, $scope.resourceBundle['order.ordered.quantity.cannot.be.less.than.shipment.quantity'], index, $timeout, allocate);
                            return false;
                        }
                    } else {
                        //Max: Lowest among “Yet to allocate” and “Available stock”. For Batch, overall have to match this condition.
                        var yta = material.q - material.oastk + material.astk - material.sq;
                        var prefix = material.isBa ? 'at' : 'a';
                        if (checkNotNullEmpty(material.nastk)) {
                            if (material.nastk > yta) {
                                showPopup($scope, material, prefix + material.id, messageFormat($scope.resourceBundle['allocation.cannot.be.greater.than.ordered.or.yet.to.allocate.quantity'], yta), index, $timeout, allocate);
                                return false;
                            } else if (material.nastk > material.atpstk + material.astk) {
                                showPopup($scope, material, prefix + material.id, messageFormat($scope.resourceBundle['allocation.cannot.be.greater.than.available.stock'], (material.atpstk + material.astk)), index, $timeout, allocate);
                                return false;
                            }
                        }
                        if (checkNotNullEmpty(material.huName) && checkNotNullEmpty(material.huQty) && checkNotNullEmpty(material.nastk) && material.nastk % material.huQty != 0) {
                            showPopup($scope, material, prefix + material.id, messageFormat($scope.resourceBundle['transaction.handling.units.mismatch'], material.nastk, material.nm, material.huName, material.huQty, material.nm), index, $timeout, allocate);
                            return false;
                        }
                    }
                }
            }
            return true;
        };
        $scope.validateStatus = function (material, index, source) {
            if (material.nastk > 0 && material.isVisitedStatus && $scope.isAllocating) {
                var dispStatus = material.tm ? $scope.tempmatstatus : $scope.matstatus;
                if (checkNotNullEmpty(dispStatus) && checkNullEmpty(material.mst) && $scope.transConfig.ism) {
                    showPopup($scope, material, source + material.id, $scope.resourceBundle['status.required'], index, $timeout, false, true);
                    return false;
                }
            }
            return true;
        };

        $scope.hidePop = function (item, id, allocate, isTable, source) {
            if (!allocate) {
                hidePopup($scope, item, item.id, id, $timeout);
            } else if (checkNotNullEmpty(source)) {
                hidePopup($scope, item, source + item.id, id, $timeout, false, true);
            } else {
                hidePopup($scope, item, (isTable ? "at" : "a") + item.id, id, $timeout, allocate);
            }
        };

        $scope.getVendInventory = function (mIds) {
            if (checkNotNullEmpty($scope.order.vid) && checkNotNullEmpty(mIds)) {
                $scope.showLoading();
                invService.getInventoryByMaterial($scope.order.vid, mIds).then(function (data) {
                    var vInventory = data.data.results;
                    var diMap = [];
                    vInventory.forEach(function (di) {
                        diMap[di.mId] = di;
                    });
                    $scope.order.its.forEach(function (it) {
                        var item = diMap[it.id];
                        if (checkNotNull(item)) {
                            it.dInv = item;
                            it.vs = item.stk;
                            it.atpstk = item.atpstk;
                            it.huName = item.huName;
                            it.huQty = item.huQty;
                            it.astk = 0;
                            it.oastk = 0;
                            it.sq = 0;
                            it.isq = 0;
                        }
                    });
                }).catch(function error(msg) {
                    $scope.showErrorMsg(msg);
                }).finally(function () {
                    $scope.hideLoading();
                });
            }
        };
        $scope.getFilteredInvntry = function (text) {
            $scope.fetchingInvntry = true;
            var eid = $scope.order.eid;
            if ($scope.order.atv) {
                eid = $scope.order.vid;
            }
            return invService.getInventoryStartsWith(eid, null, text.toLowerCase(), 0, 10).then(function (data) {
                $scope.fetchingInvntry = false;
                var list = [];
                var map = {};
                for (var i in $scope.order.its) {
                    var mat = $scope.order.its[i];
                    if (checkNotNullEmpty(mat.nm)) {
                        map[mat.id] = true;
                    }
                }
                if (checkNotNullEmpty(data.data.results)) {
                    for (var j in data.data.results) {
                        var mat = data.data.results[j];
                        if (!map[mat.mId]) {
                            list.push(mat);
                        }
                    }
                }
                return list;
            }).finally(function () {
                $scope.fetchingInvntry = false;
            });
        };
        $scope.exRow = [];
        $scope.select = function (index, type) {
            var empty = '';
            if ($scope.exRow.length == 0) {
                for (var i = 0; i < $scope.order.its.length; i++) {
                    $scope.exRow.push(empty);
                }
            }
            if (type == 'show') {
                $scope.saveDisable = true;
                $scope.saveCounter++;
            }
            if (checkNullEmpty(type)) {
                $scope.saveCounter--;
                if ($scope.saveCounter == 0) {
                    $scope.saveDisable = false;
                }
            }
            $scope.exRow[index] = $scope.exRow[index] === type ? empty : type;
            redrawPopup($scope, $scope.order.its, 'hide', $timeout);
        };
        $scope.toggle = function (index) {
            $scope.select(index);
        };
        $scope.displayStatus = function (data, addMsg) {
            var msg = $scope.resourceBundle['order.upd.success'];
            if (checkNotNullEmpty(data.data.msg)) {
                msg = msg + "<br><br>" + data.data.msg;
            }
            if (checkNotNullEmpty(addMsg)) {
                msg = msg + "<br>" + addMsg;
            }
            if (data.data.invErr) {
                $scope.showWarning(msg);
            } else {
                $scope.showSuccess(msg);
            }
        };
        $scope.setShipmentAvailable = function (value) {
            $scope.shipAvailable = value;
        };
        $scope.toggleStatusHistory = function () {
            $scope.dispStatusHistory = !$scope.dispStatusHistory;
        };
        $scope.showLoading();
        ordService.getOrderReasons("eqr").then(function (data) {
            $scope.reasons = data.data;
            if (checkNotNullEmpty($scope.reasons)) {
                var rsn = "others";
                var isMatched = $scope.reasons.some(function (arval) {
                    return rsn == arval.toLowerCase();
                });
                if (!isMatched) {
                    $scope.reasons = $scope.reasons.concat("Others");
                }
                $scope.reasons.unshift("");
            }
        }).catch(function error(msg) {
            $scope.showErrorMsg(msg);
        }).finally(function () {
            $scope.hideLoading();
        });

        $scope.openShipment = function (sID) {
            $window.open("#/orders/shipment/detail/" + sID, '_blank');
        };

        $scope.openReturns = function (returnId) {
            $window.open("#/orders/returns/detail/" + returnId, '_blank');
        };

        $scope.cancelNewShipment = function () {
            $scope.openView("orderDetail");
            $scope.enableScroll();
        };

        $scope.checkReturn = function () {
            if ($scope.order.st == ORDER.FULFILLED) {
                $scope.canReturn = $scope.order.its.some(function (item) {
                    return item.fq > 0 && item.returnedQuantity < item.fq;
                });
                if ($scope.canReturn && $scope.returnPolicyDuration > 0) {
                    var fulfilDate = string2Date($scope.order.statusUpdateDate, "dd/MM/yyyy", "/", true);
                    var oneDayInMillis = 1000 * 60 * 60 * 24;
                    var days = (new Date().setHours(0, 0, 0, 0) - fulfilDate.getTime()) / oneDayInMillis;
                    $scope.returnPolicyExpired = days > $scope.returnPolicyDuration;
                }
                if($scope.canReturn && $scope.order.vbm && !$scope.order.ebm) {
                    $scope.canReturn = false;
                }
            }
        };
    }
]);
ordControllers.controller('orders.MaterialController', ['$scope',
    function ($scope) {
        $scope.$watch('mModel', function (newVal, oldVal) {
            if (checkNotNull(newVal) && checkNotNull(newVal.stk)) {
                $scope.material = newVal;
                $scope.addRow(newVal);
                $scope.material = $scope.mModel = null;
            }
        });
    }
]);
ordControllers.controller('OrdersFormCtrl', ['$scope', 'ordService', 'invService', 'domainCfgService', 'entityService', 'requestContext', '$timeout', '$uibModal', '$location',
    function ($scope, ordService, invService, domainCfgService, entityService, requestContext, $timeout, $uibModal, $location) {
        $scope.offset = 0;
        $scope.coffset = 0;
        $scope.size = 50;
        $scope.avMap = {}; //Available Inventory Mapped by material Id.
        $scope.cInvMap = {}; //Customer Inventory Mapped by material Id.
        $scope.availableInventory = [];
        $scope.custInventory = [];
        $scope.tagMaterials = [];
        $scope.stopInvFetch = false;
        $scope.invLoading = false;
        $scope.showDestInv = false;
        $scope.type = "1";
        $scope.showCustInv = false;
        $scope.dcu = false;
        $scope.invalidPopup = 0;
        $scope.ty = true;
        $scope.showMinMax = true;
        $scope.today = new Date();
        $scope.order = {};
        $scope.order.materials = [];
        if ($scope.ordType == 'trn') {
            $scope.type = "2";
            $scope.entType = "all";
        } else {
            $scope.entType = "customers";
        }
        $scope.validate = function (material, index, source) {
            material.isVisited = material.isVisited || checkNotNullEmpty(source);
            if (material.isVisited) {
                if (checkNotNull(material.ind) && (checkNullEmpty(material.quantity) || material.quantity <= 0)) {
                    showPopup($scope, material, material.name.mId, messageFormat($scope.resourceBundle['transaction.invalid.quantity'], (material.mnm || material.name.mnm)), index, $timeout);
                    return false;
                }
                if (checkNotNullEmpty(material.name.huName) && checkNotNullEmpty(material.name.huQty) && checkNotNullEmpty(material.quantity) && material.quantity % material.name.huQty != 0) {
                    showPopup($scope, material, material.name.mId, messageFormat($scope.resourceBundle['transaction.handling.units.mismatch'], material.quantity, material.name.mnm, material.name.huName, material.name.huQty, material.name.mnm), index, $timeout);
                    return false;
                }
            }
            return true;
        };

        $scope.hidePop = function (material, id) {
            hidePopup($scope, material, material.name.mId, id, $timeout);
        };

        domainCfgService.getOrdersCfg().then(function (data) {
            $scope.oCfg = data.data;
        }).catch(function error(msg) {
            $scope.showErrorMsg(msg);
        });
        domainCfgService.getInventoryCfg().then(function (data) {
            $scope.iCfg = data.data.ivc;
        }).catch(function error(msg) {
            $scope.showErrorMsg(msg);
        });

        domainCfgService.getMaterialTagsCfg().then(function (data) {
            $scope.tags = data.data.tags;
        }).catch(function error(msg) {
            $scope.showErrorMsg(msg);
        });

        $scope.showLoading();
        ordService.getOrderReasons("orr").then(function (data) {
            $scope.reasons = data.data;
            if (checkNotNullEmpty($scope.reasons)) {
                var rsn = "others";
                var isMatched = $scope.reasons.some(function (arval) {
                    return rsn == arval.toLowerCase();
                });
                if (!isMatched) {
                    $scope.reasons = $scope.reasons.concat("Others");
                }
                $scope.reasons.unshift("");
            }
        }).catch(function error(msg) {
            $scope.showErrorMsg(msg);
        }).finally(function () {
            $scope.hideLoading();
        });

        if (checkNotNullEmpty(requestContext.getParam("eid"))) {
            var eId = requestContext.getParam("eid");
            entityService.get(eId).then(function (data) {
                $scope.order.ent = data.data;
                $scope.changeEntity(true);
                $scope.type = "1";
            });
        }

        $scope.changeEntity = function () {
            var ent = $scope.order.ent;
            $scope.order.ent = ent;
            $scope.order.eent = true;
            if (checkNotNullEmpty($scope.order.ent)) {
                $scope.loadMaterials = true;
                $scope.showMaterials = true;
                $scope.getInventory();
                fetchOrderConfig();
            }
        };
        $scope.$watch("order.cent", function (newVal, oldVal) {
            if (checkNotNullEmpty(newVal) && (checkNullEmpty(oldVal) || (checkNotNullEmpty(oldVal) && newVal.id != oldVal.id))) {
                $scope.order.cent = newVal;
                $scope.dcu = true;
                $scope.loadCInventory = true;
                $scope.getCustInventory();

            }
        });
        $scope.$watch("type", function (newVal, oldVal) {
            if (checkNotNullEmpty(newVal) && newVal != oldVal) {
                if ($scope.skipCheck) {
                    $scope.skipCheck = false;
                    return;
                }
                if ($scope.reset("ty")) {
                    if ($scope.type == "0") {
                        $scope.coffset = 0;
                    } else if ($scope.type == "1") {
                        $scope.availableInventory.some(function (mat) {
                            mat.enable = true;
                            mat.hide = false;
                        });
                    }
                } else {
                    $scope.type = oldVal;
                    $scope.skipCheck = true;
                }
            }
        });
        $scope.$watch("loadMaterials", function (newVal, oldVal) {
            if (!newVal && !$scope.loadCInventory) {
                $scope.alterInventory();
            }
        });
        $scope.$watch("loadCInventory", function (newVal, oldVal) {
            if (!newVal && !$scope.loadMaterials) {
                $scope.alterInventory();
            }
        });
        $scope.alterInventory = function () {
            $scope.custInventory.forEach(function (val) {
                if ($scope.avMap[val.mId]) {
                    $scope.avMap[val.mId].enable = true;
                    $scope.avMap[val.mId].cstk = val.stk;
                }
                $scope.cInvMap[val.mId] = val;
            });
        };
        $scope.getCustInventory = function () {
            if (checkNotNullEmpty($scope.order.cent)) {
                var custId = $scope.order.cent.id;
                $scope.showLoading();
                invService.getInventory(custId, null, $scope.coffset, $scope.size).then(function (data) {
                    var custInv = data.data.results;
                    if (checkNotNullEmpty(custInv) && custInv.length > 0) {
                        $scope.custInventory = $scope.custInventory.concat(custInv);
                    }
                    if (checkNotNullEmpty(custInv) && custInv.length == $scope.size) {
                        $scope.coffset += $scope.size;
                        $scope.getCustInventory();
                    } else {
                        $scope.loadCInventory = false;
                    }

                }).catch(function error(msg) {
                    $scope.showErrorMsg(msg);
                }).finally(function () {
                    $scope.hideLoading();
                });
            }
        };

        function fetchOrderConfig() {
            if (checkNotNullEmpty($scope.order.ent)) {
                invService.getInventoryDomainConfig($scope.order.ent.id).then(function (data) {
                    $scope.invCnf = data.data;
                }).catch(function error(msg) {
                    $scope.showErrorMsg(msg);
                });
                ordService.getVendorConfig($scope.order.ent.id).then(function (data) {
                    $scope.vendorConfig = data.data;
                }).catch(function error(msg) {
                    $scope.showErrorMsg(msg);
                });
            }
        }

        $scope.getInventory = function () {
            if (checkNotNullEmpty($scope.order.ent)) {
                var entId = $scope.order.ent.id;
                invService.getInventory(entId, null, $scope.offset, $scope.size).then(function (data) {
                    if ($scope.stopInvFetch || checkNullEmpty($scope.order.ent) || entId != $scope.order.ent.id) {
                        return;
                    }
                    var inventory = data.data.results;
                    if (checkNotNullEmpty(inventory) && inventory.length > 0) {
                        $scope.availableInventory = $scope.availableInventory.concat(inventory);
                    }
                    if (!$scope.stopInvFetch && checkNotNullEmpty(inventory) && inventory.length == $scope.size) {
                        $scope.offset += $scope.size;
                        $scope.getInventory();
                    } else {
                        $scope.loadMaterials = false;
                        var mid = requestContext.getParam("mid");
                        for (i = 0; i < $scope.availableInventory.length; i++) {
                            if ($scope.availableInventory[i].mId == mid) {
                                $scope.addMaterialToList(i);
                                $scope.showMaterials = true;
                                break;
                            }
                        }
                    }
                    inventory.forEach(function (inv) {
                        inv.tgs.forEach(function (tag) {
                            if (checkNullEmpty($scope.tagMaterials[tag])) {
                                $scope.tagMaterials[tag] = [];
                            }
                            $scope.tagMaterials[tag].push(inv.mId);
                        });
                        $scope.avMap[inv.mId] = inv;
                    });
                    for (var i in $scope.order.materials) {
                        var mat = $scope.order.materials[i];
                        if (checkNotNullEmpty(mat.name) && checkNotNullEmpty($scope.avMap[mat.name.mId])) {
                            $scope.avMap[mat.name.mId].hide = true;
                        }
                    }
                    $scope.invLoading = false;
                }).catch(function error(msg) {
                    $scope.showErrorMsg(msg);
                    $scope.loadMaterials = false;
                    $scope.invLoading = false;
                });
            }
        };
        $scope.update = function (skipCheck) {
            var invalidQMats = "";
            var count = 0;
            if (!skipCheck) {
                for (var i in $scope.order.materials) {
                    var mat = $scope.order.materials[i];
                    if (checkNullEmpty(mat.name)) {
                        continue;
                    }
                    if (mat.showRecommended) {
                        count++;
                    }
                    if (!$scope.validate(mat, i, 's')) {
                        return;
                    }
                    if (checkNotNull(mat.ind) && (checkNullEmpty(mat.quantity) || mat.quantity <= 0)) {
                        invalidQMats = invalidQMats + mat.name.mnm + ", ";
                    }
                    if (mat.showRecommended && mat.recomQ != '-1' && ($scope.oCfg.orrm || (checkNotNullEmpty(mat.rsn) && mat.rsn.toLowerCase() == 'others'))) {
                        if ((checkNullEmpty($scope.oCfg.orr) || checkNullEmpty(mat.rsn) || mat.rsn.toLowerCase() == 'others')
                            && checkNullEmpty(mat.mrsn)) {
                            $scope.showWarning(messageFormat($scope.resourceBundle['provide.reason.for'], mat.name.mnm));
                            return;
                        }
                    }
                }
                if ($scope.type == -1) {
                    $scope.showWarning($scope.resourceBundle['select.type']);
                    return;
                }
                if ($scope.type == "0" && checkNullEmpty($scope.order.cent)) {
                    $scope.showWarning($scope.resourceBundle['entercustomer']);
                    return;
                }
                if ($scope.type == "2" && $scope.order.ent.id === $scope.order.cent.id) {
                    $scope.showWarning($scope.resourceBundle['form.error']);
                    return true;
                }
                if (count > 0) {
                    if (!confirm(messageFormat($scope.resourceBundle['order.quantity.recommended.quantity.mismatch'], count))) {
                        return false;
                    }
                }
                if (checkNullEmpty($scope.order.rid) && $scope.type == 1 && $scope.oCfg.purchase) {
                    $scope.showWarning($scope.resourceBundle['purchase.reference.id.mandatory.error']);
                    return;
                } else if ((checkNullEmpty($scope.order.rid) || checkNullEmpty($scope.order.rid.trim())) && $scope.type == 2 && $scope.oCfg.transfer) {
                    if ($scope.transRelease) {
                        $scope.showWarning($scope.resourceBundle['release.reference.id.mandatory.error']);
                    } else {
                        $scope.showWarning($scope.resourceBundle['transfer.reference.id.mandatory.error']);
                    }
                    return;
                }
            }
            if (checkNotNullEmpty(invalidQMats)) {
                $scope.showWarning($scope.resourceBundle['quantity.invalid'] + ' ' + invalidQMats.slice(0, -2));
            } else {
                $scope.showFullLoading();
                if ($scope.timestamp == undefined) {
                    $scope.timestamp = new Date().getTime();
                }
                var fOrder = constructFinalOrder();
                if ($scope.type != "1") {
                    skipCheck = true;
                }
                fOrder.skipCheck = skipCheck;
                ordService.createOrder(fOrder).then(function (data) {
                    if (data.data.msg) {
                        resetNoConfirm(true);
                        $scope.showSuccess(data.data.msg);

                        if (checkNotNullEmpty(data.data.orderId)) {
                            if ($scope.ordType == 'trn') {
                                $location.path('/orders/transfers/detail/' + data.data.orderId);
                            } else {
                                $location.path('/orders/detail/' + data.data.orderId);
                            }
                        }
                    } else {
                        $scope.orderTable = data.data.items;
                        $scope.modalInstance = $uibModal.open({
                            templateUrl: 'views/orders/order-exists-table.html',
                            scope: $scope,
                            keyboard: false,
                            backdrop: 'static'
                        });
                    }
                }).catch(function error(msg) {
                    if (msg.status == 504 || msg.status == 404) {
                        // Allow resubmit or cancel.
                        handleTimeout();
                    } else {
                        $scope.showErrorMsg(msg);
                    }
                }).finally(function () {
                    $scope.hideFullLoading();
                });
            }
        };

        function handleTimeout() {
            $scope.modalInstance = $uibModal.open({
                template: '<div class="modal-header ws">' +
                '<h3 class="modal-title">{{resourceBundle["connection.timedout"]}}</h3>' +
                '</div>' +
                '<div class="modal-body ws">' +
                '<p>{{resourceBundle["connection.timedout"]}}.</p>' +
                '</div>' +
                '<div class="modal-footer ws">' +
                '<button class="btn btn-primary" ng-click="update();cancel()">{{resourceBundle.resubmit}}</button>' +
                '<button class="btn btn-default" ng-click="cancel()">{{resourceBundle.cancel}}</button>' +
                '</div>',
                scope: $scope
            });
        }

        $scope.proceed = function () {
            $scope.update(true);
            $scope.cancel();
        };
        $scope.cancel = function () {
            $scope.modalInstance.dismiss('cancel');
        };
        function constructFinalOrder() {

            var ft = {};
            if ($scope.type == "1") {
                ft['kioskid'] = '' + $scope.order.ent.id;
                if (checkNotNullEmpty($scope.order.vent)) {
                    ft['vkioskid'] = '' + $scope.order.vent.id;
                }
            } else {
                if (checkNotNullEmpty($scope.order.cent)) {
                    ft['kioskid'] = '' + $scope.order.cent.id;
                    ft['vkioskid'] = '' + $scope.order.ent.id;
                }
            }
            ft['ordMsg'] = $scope.order.msg;
            ft['oTag'] = [];
            if (checkNotNullEmpty($scope.order.oTags)) {
                var i = 0;
                $scope.order.oTags.forEach(function (data) {
                    ft['oTag'][i++] = data.id;
                });
            }
            if (checkNotNullEmpty($scope.order.edd)) {
                ft['edd'] = formatDate($scope.order.edd);
            }
            if (checkNotNullEmpty($scope.order.efd)) {
                ft['efd'] = formatDate($scope.order.efd);
            }
            if ($scope.type == "0") {
                ft['orderType'] = '2';
            } else if ($scope.type == "1") {
                ft['orderType'] = '1';
            } else {
                ft['orderType'] = '0';
            }
            ft['status'] = $scope.status;
            ft['materials'] = {};
            $scope.order.materials.forEach(function (mat) {
                if (checkNotNull(mat.ind)) {
                    ft['materials'][mat.name.mId] = {q: '' + mat.quantity, r: mat.mrsn || mat.rsn || null};
                }
            });
            ft['rid'] = $scope.order.rid;
            ft['signature'] = $scope.curUser + $scope.timestamp;
            return ft;
        }

        $scope.$watch('mtag', function (name, oldVal) {
            $scope.availableInventory.forEach(function (inv) {
                inv.tHide = checkNotNullEmpty(name) && (checkNullEmpty($scope.tagMaterials[name]) || !($scope.tagMaterials[name].indexOf(inv.mId) >= 0));
            });
        });
        $scope.deleteRow = function (id) {
            var mIndex = $scope.order.materials[id].name.mId;
            if (checkNotNullEmpty(mIndex) && checkNotNullEmpty($scope.avMap[mIndex])) {
                $scope.avMap[mIndex].hide = false;
            }
            $scope.exRow.splice(id, 1);
            hidePopup($scope, $scope.order.materials[id], $scope.order.materials[id].name.mId, id, $timeout);
            $scope.order.materials.splice(id, 1);
            redrawPopup($scope, $scope.order.materials, 'hide', $timeout);
        };
        $scope.addRows = function () {
            $scope.order.materials.push({"name": "", "stock": ""});
        };
        $scope.reset = function (type) {
            if (type === "en") {
                if ($scope.order.materials.length - 1 > 0) {
                    if (window.confirm($scope.resourceBundle['entity.editconfirm'])) {
                        resetType(type);
                    }
                } else {
                    resetType(type);
                }
            } else if (type === "ty") {
                if ($scope.order.materials.length - 1 > 0) {
                    if (window.confirm($scope.resourceBundle['type.editconfirm'])) {
                        resetNoConfirm(true);
                        return true;
                    }
                } else {
                    resetNoConfirm(true);
                    return true;
                }
            } else if (type == "cu") {
                if ($scope.order.materials.length - 1 > 0) {
                    if (window.confirm($scope.resourceBundle['customer.clear'])) {
                        resetType(type);
                    }
                } else {
                    resetType(type);
                }
            }
            else {
                if (window.confirm($scope.resourceBundle['clear.all'])) {
                    resetNoConfirm();
                }
            }
            return false;
        };
        function resetType(type) {
            if (type == "en") {
                $scope.order.ent = null;
                $scope.order.eent = false;
                $scope.order.vent = null;
                $scope.availableInventory = [];
                $scope.order.oTags = undefined;
            }
            $scope.order.cent = null;
            $scope.order.materials = [];
            if (type != "en") {
                $scope.availableInventory.forEach(function (inv) {
                    inv.hide = false;
                    inv.enable = false;
                    inv.cstk = null;
                });
            }
            $scope.custInventory = [];
            $scope.cInvMap = {};
            $scope.showMaterials = true;
            $scope.loadCInventory = false;
            $scope.showCustInv = false;
            $scope.dcu = false;
            $scope.coffset = 0;
            $scope.invalidPopup = 0;
            $scope.addRows();
            $scope.offset = 0;
        }

        function resetNoConfirm(isAdd) {
            $scope.offset = 0;
            $scope.coffset = 0;
            $scope.order = {};
            $scope.order.materials = [];
            $scope.availableInventory = [];
            $scope.custInventory = [];
            $scope.avMap = {};
            $scope.cInvMap = {};
            $scope.tagMaterials = [];
            $scope.invCnf = {};
            $scope.showMaterials = false;
            $scope.showDestInv = false;
            $scope.loadMaterials = false;
            $scope.exRow = [];
            $scope.submitted = false;
            $scope.dInv = false;
            $scope.diMap = {};
            $scope.showCustInv = false;
            $scope.dcu = false;
            $scope.loadCInventory = false;
            $scope.invalidPopup = 0;
            $scope.order.oTags = undefined;
            $scope.addRows();
            if ($scope.isEnt) {
                $scope.order.ent = $scope.entity;
                $scope.getInventory();
                fetchOrderConfig();
                $scope.showMaterials = true;
            } else if (checkNotNullEmpty($scope.defaultEntityId) && isAdd) {
                $scope.showLoading();
                entityService.get($scope.defaultEntityId).then(function (data) {
                    $scope.order.ent = data.data;
                    if (checkNotNullEmpty($scope.order.ent)) {
                        $scope.order.eent = true;
                        $scope.getInventory();
                        fetchOrderConfig();
                        $scope.showMaterials = true;
                    }
                }).finally(function () {
                    $scope.hideLoading();
                });
            }
            $scope.timestamp = undefined;
        }

        resetNoConfirm(true);
        $scope.getFilteredEntity = function (text) {
            $scope.loadingEntityMaterials = true;
            return entityService.getFilteredEntity(text.toLowerCase()).then(function (data) {
                $scope.loadingEntityMaterials = false;
                return data.data.results;
            }).catch(function error(msg) {
                $scope.showErrorMsg(msg);
            });
        };
        $scope.getFilteredInvntry = function (text) {
            $scope.fetchingInvntry = true;
            return invService.getInventoryStartsWith($scope.order.ent.id, null, text.toLowerCase(), 0, 10).then(function (data) {
                $scope.fetchingInvntry = false;
                var list = [];
                var map = {};
                for (var i in $scope.order.materials) {
                    var mat = $scope.order.materials[i];
                    if (checkNotNullEmpty(mat.name)) {
                        map[mat.name.mId] = true;
                    }
                }
                if (checkNotNullEmpty(data.data.results)) {
                    for (var j in data.data.results) {
                        var mat = data.data.results[j];
                        var push = true;
                        if ($scope.type == "0" && $scope.order.cent) {
                            /*var enable = $scope.custInventory.some(function(data) {
                             return mat.mnm == data.mnm;
                             }) ;*/
                            push = checkNotNullEmpty($scope.cInvMap[mat.mId]);
                        }
                        if (!map[mat.mId] && push) {
                            list.push(mat);
                        }
                    }
                }
                return list;
            }).finally(function () {
                $scope.fetchingInvntry = false;
            });
        };
        $scope.addMaterialToList = function (index) {
            var li = $scope.order.materials.length - 1;
            var newMat = {"name": $scope.availableInventory[index]};
            if (li != $scope.order.materials.length) {
                $scope.order.materials[li] = newMat;
            } else {
                $scope.order.materials.push(newMat);
            }
            $scope.availableInventory[index].hide = true;
        };
        $scope.exRow = [];
        $scope.select = function (index, type) {
            var empty = "";
            if ($scope.exRow.length == 0) {
                for (var i = 0; i < $scope.order.materials.length; i++) {
                    $scope.exRow.push(empty);
                }
            }
            $scope.exRow[index] = $scope.exRow[index] === type ? empty : type;
        };
        $scope.toggle = function (index) {
            $scope.select(index);
        };
    }
]);
ordControllers.controller('order.MaterialController', ['$scope', 'invService',
    function ($scope, invService) {
        $scope.material.showRecommended = false;
        $scope.showRecomQuantity = function (val) {
            $scope.material.showRecommended = false;
            if ($scope.material.recomQ >= 0 && val != $scope.material.recomQ) {
                $scope.material.showRecommended = true;
            } else {
                //reset reasons
                $scope.material.rsn = null;
                $scope.material.mrsn = null;
            }
        };
        $scope.recommendedQuantity = function (name) {
            var opoq = -1;
            var stock = name.stk;
            if (checkNotNull(name.validBatchStock)) {
                stock = name.validBatchStock;
            }
            if (name.im == 'sq') {
                opoq = name.eoq;
            } else if (name.max > 0) {
                if ((stock + name.tstk) >= name.max) {
                    opoq = 0;
                } else {
                    opoq = name.max - stock - name.tstk;
                    opoq.toFixed(1);
                }
            }
            return opoq.toFixed(0);
        };

        function setValidBatchQuantity(inv, material) {
            $scope.showLoading();
            invService.getBatchDetail(inv.mId, inv.kId, false).then(function (data) {
                var batchDet = data.data;
                var totalStock = 0;
                if (checkNotNullEmpty(batchDet)) {
                    for (var i = 0; i < batchDet.length; i++) {
                        if (!batchDet[i].isExp) {
                            totalStock = totalStock + batchDet[i].q;
                        }
                    }
                }
                inv.validBatchStock = totalStock;
                updateMaterial(material);
            }).catch(function error(msg) {
                $scope.showErrorMsg(msg);
            }).finally(function () {
                $scope.hideLoading();
            });
        }

        $scope.$watch('material.name', function (newVal, oldVal) {
            if (checkNotNull(newVal) && checkNotNull(newVal.stk)) {
                if (checkNotNullEmpty($scope.avMap[newVal.mId])) {
                    newVal = $scope.avMap[newVal.mId];
                    newVal.hide = true;
                }
                var inv = $scope.type == "0" || $scope.type == "2" ? $scope.cInvMap[newVal.mId] : newVal;
                if (inv.be) {
                    setValidBatchQuantity(inv, newVal);
                } else {
                    updateMaterial(newVal);
                }
            }
        });

        function updateMaterial(name) {
            $scope.material.stock = name.stk;
            $scope.material.ind = name.sno - 1;
            $scope.material.event = name.event;
            $scope.material.rp = name.rp;
            $scope.material.mId = [];
            $scope.material.mId.push(name.mId);
            $scope.material.kId = name.kId;
            $scope.material.crd = name.crd;
            $scope.material.crw = name.crw;
            $scope.material.crm = name.crm;
            $scope.material.rvpdmd = name.rvpdmd;
            $scope.material.ordp = name.ordp;
            $scope.material.eoq = name.eoq;
            $scope.material.rsn = '';
            $scope.material.atpstk = name.atpstk;
            $scope.material.tstk = name.tstk;
            $scope.material.mm = "(" + name.reord.toFixed(0) + ',' + name.max.toFixed(0) + ")";
            if (checkNotNullEmpty($scope.order.cent)) {
                $scope.material.cstock = $scope.cInvMap[name.mId].stk;
                $scope.material.cevent = $scope.cInvMap[name.mId].event;
                $scope.material.cmm = "(" + $scope.cInvMap[name.mId].reord + "," + $scope.cInvMap[name.mId].max + ")";
                $scope.material.rp = $scope.cInvMap[name.mId].rp;
            }
            $scope.material.mrsn = null;
            if ($scope.type == "1") {
                $scope.material.recomQ = $scope.recommendedQuantity(name);
            } else if ($scope.type == "0" || $scope.type == "2") {
                if ($scope.cInvMap[name.mId]) {
                    $scope.material.recomQ = $scope.recommendedQuantity($scope.cInvMap[name.mId]);
                }
            }
            $scope.material.quantity = Math.max($scope.material.recomQ, 0);
            $scope.addRows();
        }
    }
]);
ordControllers.controller('DemandItemController', ['$scope', function ($scope) {
    if (checkNotNullEmpty($scope.item.bts) || checkNotNullEmpty($scope.item.bq)) {
        $scope.item.obts = angular.copy($scope.item.bts || $scope.item.bq);
    }
    $scope.updateBatch = function (bdata) {
        $scope.saveCounter--;
        $scope.item.bdata = bdata;
        if ($scope.saveCounter == 0) {
            $scope.saveDisable = false;
        }
        var newBatches = [];
        var allocated = 0;
        bdata.forEach(function (bItem) {
            if (checkNotNullEmpty($scope.batchOrder) || (checkNotNullEmpty(bItem.quantity) && bItem.quantity != 0)) {
                //mst is used on demand and order-detail page whereas smst is used on shipment-detail page for describing it as shipped material status
                var batchData = {};
                batchData.e = bItem.bexp;
                batchData.m = bItem.bmfnm;
                batchData.mdt = bItem.bmfdt;
                batchData.q = bItem.quantity;
                batchData.id = bItem.bid;
                batchData.fq = bItem.quantity;
                batchData.sq = bItem.sq;
                batchData.atpstk = bItem.atpstk;
                batchData.mst = bItem.mst;
                batchData.smst = bItem.mst;
                newBatches.push(batchData);
                if (checkNotNullEmpty(bItem.quantity)) {
                    allocated = parseInt(bItem.quantity) + allocated;
                }
            }
        });
        $scope.item.bts = newBatches;
        $scope.item.nastk = $scope.item.naq = $scope.item.nfq = allocated;
    }
}]);
ordControllers.controller('NewShipmentController', ['$scope', 'ordService', '$location', 'trnService', '$timeout', 'domainCfgService', '$uibModal',
    function ($scope, ordService, $location, trnService, $timeout, domainCfgService, $uibModal) {
        windowScrollTop();
        $scope.today = new Date();
        $scope.rsn = '';
        $scope.ps = '';
        $scope.consignment = {};
        $scope.showLoading();
        domainCfgService.getOrdersCfg().then(function (data) {
            $scope.oCfg = data.data;
            if (checkNotNullEmpty($scope.oCfg.ps)) {
                $scope.packageSize = $scope.oCfg.ps.split(',');
                $scope.packageSize.unshift("");
            }
        }).catch(function error(msg) {
            $scope.showErrorMsg(msg);
        }).finally(function () {
            $scope.hideLoading();
        });
        $scope.showLoading();
        ordService.getOrderReasons("psr").then(function (data) {
            $scope.reasons = data.data;
            if (checkNotNullEmpty($scope.reasons)) {
                var rsn = "others";
                var isMatched = $scope.reasons.some(function (arval) {
                    return rsn == arval.toLowerCase();
                });
                if (!isMatched) {
                    $scope.reasons = $scope.reasons.concat("Others");
                }
                $scope.reasons.unshift("");
            }
        }).catch(function error(msg) {
            $scope.showErrorMsg(msg);
        }).finally(function () {
            $scope.hideLoading();
        });
        if (checkNotNullEmpty($scope.sMTShip)) {
            $scope.sMTShip.forEach(function (item) {
                if (item.astk > 0) {
                    item.nq = Math.min(angular.copy(item.astk), angular.copy(item.ytcs));
                    item.ta = true;
                } else {
                    item.nq = angular.copy(item.ytcs);
                }
            });
        }

        $scope.create = function (shipNow) {
            var ind = 0;
            var isInvalid = $scope.sMTShip.some(function (data) {
                return !$scope.validate(data, ind++);
            });
            if (isInvalid) {
                return;
            }
            $scope.shipment = { consignment: $scope.consignment };

            if ($scope.oCfg.psrm || (checkNotNullEmpty($scope.rsn) && $scope.rsn.toLowerCase() == 'others')) {
                if ((checkNullEmpty($scope.oCfg.psr) || checkNullEmpty($scope.rsn) || $scope.rsn.toLowerCase() == 'others')
                    && checkNullEmpty($scope.mrsn)) {
                    $scope.showWarning($scope.resourceBundle['provide.reason']);
                    return;
                }
            }
            if (checkNullEmpty($scope.oCfg.psr) || $scope.rsn.toLowerCase() == 'others') {
                $scope.shipment.reason = $scope.mrsn;
            } else {
                $scope.shipment.reason = $scope.rsn;
            }
            $scope.shipment.orderId = $scope.order.id;
            $scope.shipment.customerId = $scope.order.eid;
            $scope.shipment.vendorId = $scope.order.vid;
            $scope.shipment.items = [];
            if (shipNow) {
                $scope.shipment.changeStatus = 'sp';
            }
            var hasBatchEnabledMaterials = false;
            var showDialog = true;
            $scope.sMTShip.forEach(function (data) {
                var i1 = {};
                i1.mId = data.id;
                i1.q = data.nq;
                i1.oq = data.nq;
                i1.afo = data.ta;
                i1.isBa = data.isBa;
                if (showDialog) {
                    if (i1.isBa) {
                        hasBatchEnabledMaterials = true;
                        if (data.astk < data.nq) {
                            showDialog = false;
                        }
                    } else if (!hasBatchEnabledMaterials) {
                        if (data.atpstk < data.nq) {
                            showDialog = false;
                        }
                    }
                }
                i1.smst = data.smst;

                i1.astk = data.astk;
                $scope.shipment.items.push(i1);
            });
            $scope.shipment.transporter = $scope.transporter.name;
            $scope.shipment.transporterId = $scope.transporter.id;
            $scope.shipment.phonenum = $scope.transporter.phnm;
            $scope.shipment.vehicle = $scope.transporter.vhcl;
            $scope.shipment.trackingId = $scope.trackingId;
            $scope.shipment.ps = $scope.ps;
            $scope.shipment.salesRefId = $scope.salesRefId;
            $scope.shipment.ead = formatDate($scope.ead);
            if (showDialog) {
                $scope.shipment.ship = 0;
                $scope.modalInstance = $uibModal.open({
                    templateUrl: 'views/orders/ship-shipment.html',
                    scope: $scope,
                    keyboard: false,
                    backdrop: 'static'
                });
            } else {
                createShipment($scope.shipment);
            }
        };

        $scope.$watch("trsp", function(newVal, oldVal) {
            if(checkNotNullEmpty(newVal)) {
                if(typeof newVal === 'string') {
                    $scope.transporter =  {name: newVal};
                } else {
                    $scope.transporter = newVal;
                }
            } else {
                $scope.transporter = {};
            }
        });


        $scope.shipNewShipment = function () {
            if ($scope.shipment.ship == 0) {
                if ($scope.oCfg.tm && checkNullEmpty($scope.shipment.transporter)) {
                    $scope.showWarning($scope.resourceBundle['transportermandatory']);
                    return;
                } else if ($scope.oCfg.eadm && checkNullEmpty($scope.shipment.ead)) {
                    $scope.showWarning($scope.resourceBundle['estimated.date.of.arrival.mandatory.error']);
                    return;
                } else if ($scope.oCfg.ridm && (checkNullEmpty($scope.shipment.salesRefId) || checkNullEmpty($scope.shipment.salesRefId.trim()))) {
                    $scope.showWarning($scope.resourceBundle['sales.reference.id.mandatory.error']);
                    return;
                }
                $scope.shipment.changeStatus = 'sp';
            } else {
                $scope.shipment.changeStatus = '';
            }
            $scope.dismissModel();
            createShipment($scope.shipment);
        }

        $scope.dismissModel = function () {
            $scope.modalInstance.dismiss('cancel');

        }

        function createShipment(shipment) {
            $scope.showFullLoading();
            ordService.createShipment(shipment).then(function (data) {
                $scope.showSuccess(data.data.msg);
                $location.path('/orders/shipment/detail/' + data.data.sId);
            }).catch(function error(msg) {
                $scope.showErrorMsg(msg);
            }).finally(function () {
                $scope.hideFullLoading();
            });
        };

        $scope.hidePop = function (material, index) {
            hidePopup($scope, material, material.id, index, $timeout);
        };
        $scope.validate = function (material, index) {
            if (checkNotNullEmpty(material.nq)) {
                if (material.nq > material.ytcs) {
                    var extMsg = "";
                    if (material.ytcs != material.q) {
                        extMsg = messageFormat($scope.resourceBundle['shipment.created.with.quantity'], (material.q - material.ytcs));
                    }
                    showPopup($scope, material, material.id, messageFormat($scope.resourceBundle['quantity.cannot.be.greater.than'], material.ytcs) + extMsg, index, $timeout);
                    return false;
                }
            }
            if (checkNotNullEmpty(material.huName) && checkNotNullEmpty(material.huQty) && checkNotNullEmpty(material.nq) && material.nq % material.huQty != 0) {
                showPopup($scope, material, material.id, messageFormat($scope.resourceBundle['transaction.handling.units.mismatch'], material.nq, material.nm, material.huName, material.huQty, material.nm), $timeout);
                return false;
            }
            return true;
        };
    }
]);
ordControllers.controller('ShipmentListingController', ['$scope', 'ordService', 'requestContext', '$location', '$window', function ($scope, ordService, requestContext, $location, $window) {
    $scope.wparams = [["o", "offset"], ["s", "size"], ["status", "status"], ["cid", "custId.id"], ["vid", "vendId.id"], ["from", "from", "", formatDate2Url], ["to", "to", "", formatDate2Url], ["eftf", "eftFrom", "", formatDate2Url], ["eftt", "eftTo", "", formatDate2Url], ["trans", "trans"], ["trackid", "trackId"]];
    $scope.today = new Date();
    $scope.localFilters = ['custId', 'vendId', 'status', 'sTrackId', 'from', 'to', 'eftFrom', 'eftTo', 'sTrans', 'trackId'];
    $scope.filterMethods = ['searchTID', 'updateTransporter'];
    ListingController.call(this, $scope, requestContext, $location);
    $scope.initLocalFilters = [];
    $scope.init = function () {
        $scope.status = requestContext.getParam("status") || "";
        $scope.from = parseUrlDate(requestContext.getParam("from"));
        $scope.to = parseUrlDate(requestContext.getParam("to"));
        $scope.eftFrom = parseUrlDate(requestContext.getParam("eftf"));
        $scope.eftTo = parseUrlDate(requestContext.getParam("eftt"));
        $scope.trans = $scope.sTrans = requestContext.getParam("trans");
        $scope.trackId = $scope.sTrackId = $scope.ordId = requestContext.getParam("trackid");
        if (checkNotNullEmpty(requestContext.getParam("cid"))) {
            if (checkNullEmpty($scope.custId) || $scope.custId.id != parseInt(requestContext.getParam("cid"))) {
                $scope.custId = {id: parseInt(requestContext.getParam("cid")), nm: ""};
                $scope.initLocalFilters.push("custId");
            }
        } else {
            $scope.custId = null;
        }
        if (checkNotNullEmpty(requestContext.getParam("vid"))) {
            if (checkNullEmpty($scope.vendId) || $scope.vendId.id != parseInt(requestContext.getParam("vid"))) {
                $scope.vendId = {id: parseInt(requestContext.getParam("vid")), nm: ""};
                $scope.initLocalFilters.push("vendId")
            }
        } else {
            $scope.vendId = null;
        }
    };
    $scope.init();
    $scope.fetch = function () {
        if ($scope.iMan && !$scope.custId && !$scope.vendId) {
            $scope.filtered = [];
        } else {
            $scope.loading = true;
            $scope.showLoading();
            var cust = undefined;
            if ($scope.custId) {
                cust = $scope.custId.id;
            }
            var vend = undefined;
            if ($scope.vendId) {
                vend = $scope.vendId.id;
            }
            ordService.getShipments($scope.offset, $scope.size, cust, vend, $scope.status, formatDate($scope.from), formatDate($scope.to), formatDate($scope.eftFrom), formatDate($scope.eftTo), $scope.trans, $scope.trackId).then(function (data) {
                $scope.filtered = data.data.results;
                $scope.setResults(data.data);
                fixTable();
            }).catch(function error(msg) {
                $scope.showErrorMsg(msg);
            }).finally(function () {
                $scope.hideLoading();
                $scope.loading = false;
            });
        }
    };
    $scope.reset = function () {
        $scope.custId = null;
        $scope.vendId = null;
        $scope.status = undefined;
        $scope.from = undefined;
        $scope.to = undefined;
        $scope.eftFrom = undefined;
        $scope.eftTo = undefined;
        $scope.trans = undefined;
        $scope.trackId = undefined;
    };
    $scope.getSuggestions = function (text) {
        if (checkNotNullEmpty(text)) {
            return ordService.getTransSuggestions(text).then(function (data) {
                return data.data;
            }).catch(function (errorMsg) {
                $scope.showErrorMsg(errorMsg);
            });
        }
    };
    $scope.updateTransporter = function () {
        $scope.trans = $scope.sTrans;
    };
    $scope.$watch('sTrans', function (newValue) {
        if (checkNullEmpty(newValue) && checkNotNullEmpty($scope.trans)) {
            $scope.trans = undefined;
        }
    });
    $scope.openShipment = function (shipId) {
        $window.open("#/orders/shipment/detail/" + shipId, '_blank');
    };
    $scope.fetch();

    $scope.searchTID = function () {
        $scope.trackId = $scope.sTrackId;
    };
}]);

ordControllers.controller('FulfilBatchTransactionCtrl', ['$scope', '$timeout', function ($scope, $timeout) {

    $scope.newBatchDet = [];
    $scope.batchDet = [];
    $scope.quantityZero = [];
    var tempData = angular.copy($scope.bdata);

    tempData.forEach(function (bItem) {
        if (bItem.added) {
            $scope.newBatchDet.push(bItem);
        } else {
            $scope.batchDet.push(bItem);
        }
    });

    $scope.addBatch = function () {
        $scope.newBatchDet.push({"frsn": "", "fmst": ""});
    };

    $scope.deleteRow = function (index) {
        $scope.newBatchDet.splice(index, 1);
    };

    $scope.saveBatchTrans = function () {
        var tempBatchDet = angular.copy($scope.batchDet);
        var isBatchIdRepeated = false;
        var isInvalid = $scope.newBatchDet.some(function (newBItem) {

            if (checkNullEmpty(newBItem.id) || checkNullEmpty(newBItem.uie) ||
                checkNullEmpty(newBItem.bmfnm) || checkNullEmpty(newBItem.q)) {
                $scope.showWarning($scope.resourceBundle['fields.missing']);
                return true;
            }

            isBatchIdRepeated = $scope.validateBatchId(newBItem);

            if (newBItem.uie != null && newBItem.q != null) {
                newBItem.bmfdt = newBItem.uibmfdt ? formatDate(newBItem.uibmfdt) : "";
                newBItem.e = formatDate(newBItem.uie);
                newBItem.added = true;
                tempBatchDet.push(newBItem);
            }
        });

        if (isInvalid || isBatchIdRepeated) {
            return;
        }

        if ($scope.type !== 'p') {
            var index = 0;
            var invalidQuantity = tempBatchDet.some(function (det) {
                if (checkNotNullEmpty(det.q)) {
                    if ($scope.huQty && det.q % $scope.huQty != 0) {
                        return true;
                    }
                }

                if (checkNotNullEmpty(det.q) && checkNullEmpty(det.fmst)) {
                    det.isVisitedStatus = true;
                    if (index < $scope.batchDet.length) {
                        $scope.batchDet[index].isVisitedStatus = true;
                        if (!$scope.validateBatch($scope.batchDet[index], index, $scope.tm ? "smt" : "sm")) {
                            return true;
                        }
                    } else {
                        newIndex = index - $scope.batchDet.length;
                        $scope.newBatchDet[newIndex].isVisitedStatus = true;
                        if (!$scope.validateBatch($scope.newBatchDet[newIndex], newIndex, $scope.tm ? "nsmt" : "nsm")) {
                            return true;
                        }
                    }
                }
                if (index < $scope.batchDet.length) {
                    if (!$scope.validateBatch($scope.batchDet[index], index, 'r')) {
                        return true;
                    }
                } else {
                    var newIndex = index - $scope.batchDet.length;
                    if (!$scope.validateBatch($scope.newBatchDet[newIndex], newIndex, 'n')) {
                        return true;
                    }
                }
                index += 1;
            });
            if (invalidQuantity) {
                return;
            }
        }
        var newBatches = [];
        var allocated = 0;
        tempBatchDet.forEach(function (bItem) {
            var batchData = {};
            batchData.e = bItem.e;
            batchData.bmfdt = bItem.bmfdt;
            batchData.bmfnm = bItem.bmfnm;
            batchData.q = bItem.q;
            batchData.id = bItem.id;
            batchData.fq = bItem.q;
            batchData.sq = bItem.sq ? bItem.sq : 0;
            batchData.huName = bItem.huName;
            batchData.huQty = bItem.huQty;
            batchData.fmst = bItem.fmst;
            batchData.smst = bItem.smst;
            batchData.added = bItem.added;
            batchData.uie = bItem.uie;
            batchData.uibmfdt = bItem.uibmfdt;
            batchData.frsn = bItem.frsn || '';
            if (checkNullEmpty(bItem.frsn) || bItem.frsn == "Others") {
                batchData.mrsn = bItem.mrsn;
            }
            newBatches.push(batchData);
            if (checkNotNullEmpty(bItem.q)) {
                allocated = parseInt(bItem.q) + allocated;
            }
        });
        $scope.item.bts = newBatches;
        $scope.item.nastk = $scope.item.naq = $scope.item.fq = allocated;
        $scope.checkShowReason();
        $scope.toggle($scope.$index);
    };

    $scope.validateBatchId = function (item) {

        var duplicateFound = false;

        $scope.batchDet.some(function (existingItem) {
            if (existingItem.id == item.id) {
                duplicateFound = true;
                $scope.showWarning($scope.resourceBundle['duplicate.batch']);
            }
        });

        for (var j = 0; j < $scope.newBatchDet.length; j++) {
            for (var i = 0; i < $scope.newBatchDet.length; i++) {
                if (i != j && $scope.newBatchDet[i].id == $scope.newBatchDet[j].id) {
                    duplicateFound = true;
                    $scope.showWarning($scope.resourceBundle['duplicate.batch']);
                }
            }
        }

        return duplicateFound;
    }

    $scope.validateQuantity = function (item, index) {
        $scope.quantityZero[index] = false;
        if (item.q == 0) {
            $scope.quantityZero[index] = true;
            showPopup($scope, item, 'newBatchq' + $scope.mid, $scope.resourceBundle['shipment.fulfil.batch.quantity.cannot.be.zero'], index, $timeout);
        }
        if (checkNotNullEmpty($scope.huName) && checkNotNullEmpty($scope.huQty) && checkNotNullEmpty(item.q) && item.q % $scope.huQty != 0) {
            $scope.quantityZero[index] = true;
            showPopup($scope, item, 'newBatchq' + $scope.mid, messageFormat($scope.resourceBundle['transaction.handling.units.mismatch'], item.q, $scope.mnm, $scope.huName, $scope.huQty, $scope.mnm), index, $timeout);
        }
    }

    $scope.validateBatch = function (data, index, source) {
        if (source == 'r') {
            if (data.sq != data.q && ($scope.oCfg.pfrm || (checkNotNullEmpty(data.frsn) && data.frsn.toLowerCase() == 'others'))) {
                if ((checkNullEmpty($scope.oCfg.pfr) || checkNullEmpty(data.frsn) || data.frsn.toLowerCase() == 'others')
                    && checkNullEmpty(data.mrsn)) {
                    showPopup($scope, data, 'fr' + (checkNotNullEmpty($scope.oCfg.pfr) ? 'r' : '') + $scope.mid, $scope.resourceBundle['reason.required'], index, $timeout, true);
                    return false;
                } else {
                    $scope.hidePop(data, index, source);
                }
            }
        } else if (source == 'n') {
            if (data.sq != data.q && ($scope.oCfg.pfrm || (checkNotNullEmpty(data.frsn) && data.frsn.toLowerCase() == 'others'))) {
                if ((checkNullEmpty($scope.oCfg.pfr) || checkNullEmpty(data.frsn) || data.frsn.toLowerCase() == 'others') && checkNullEmpty(data.mrsn)) {
                    showPopup($scope, data, 'nr' + (checkNotNullEmpty($scope.oCfg.pfr) ? 'r' : '') + $scope.mid, $scope.resourceBundle['reason.required'], index, $timeout, true);
                    return false;
                } else {
                    $scope.hidePop(data, index, source);
                }
            }
        } else if (checkNotNullEmpty(source)) {
            if (data.q > 0 && checkNullEmpty(data.fmst) && data.isVisitedStatus) {
                var status = $scope.tm ? $scope.tempmatstatus : $scope.matstatus;
                if (checkNotNullEmpty(status) && $scope.msm) {
                    showPopup($scope, data, source + $scope.mid, $scope.resourceBundle['status.required'], index, $timeout, false, true);
                    return false;
                }
            }
        } else if (checkNotNullEmpty($scope.huName) && checkNotNullEmpty($scope.huQty) && checkNotNullEmpty(data.q) && data.q % $scope.huQty != 0) {
            showPopup($scope, data, 'f' + $scope.mid, messageFormat($scope.resourceBundle['transaction.handling.units.mismatch'], data.q, $scope.mnm, $scope.huName, $scope.huQty, $scope.mnm), index, $timeout);
            return false;
        }

        return true;
    };
    $scope.hidePopBatch = function (material, index, source) {
        if (source == 'r') {
            hidePopup($scope, material, 'fr' + (checkNotNullEmpty($scope.oCfg.pfr) ? 'r' : '') + $scope.mid, index, $timeout, true);
        } else if (source == 'n') {
            hidePopup($scope, material, 'nr' + (checkNotNullEmpty($scope.oCfg.pfr) ? 'r' : '') + $scope.mid, index, $timeout, true);
        } else if (source == 'bid') {
            hidePopup($scope, material, 'newBatch' + $scope.mid, index, $timeout, true);
        } else if (source == 'q') {
            hidePopup($scope, material, 'newBatchq' + $scope.mid, index, $timeout, true);
        } else if (checkNotNullEmpty(source)) {
            hidePopup($scope, material, source + $scope.mid, index, $timeout, false, true);
        } else {
            hidePopup($scope, material, 'f' + $scope.mid, index, $timeout);
        }
    };
}]);

ordControllers.controller('FulfilShipmentController', ['$scope', 'ordService', 'trnService', '$timeout', 'domainCfgService', function ($scope, ordService, trnService, $timeout, domainCfgService) {
    $scope.today = new Date();
    $scope.masterIts = [];
    $scope.shipData = {};
    $scope.shipData.afd = $scope.today;
    $scope.batchOrder = true;
    $scope.saveCounter = 0;
    $scope.saveDisable = false;
    var counter = 0;
    $scope.showLoading();
    ordService.getOrderReasons("pfr").then(function (data) {
        $scope.fulfilReasons = data.data;
        if (checkNotNullEmpty($scope.fulfilReasons)) {
            var rsn = "others";
            var isMatched = $scope.fulfilReasons.some(function (arval) {
                return rsn == arval.toLowerCase();
            });
            if (!isMatched) {
                $scope.fulfilReasons = $scope.fulfilReasons.concat("Others");
            }
            $scope.fulfilReasons.unshift("");
        }
        counter++;
        $scope.initShipmentItems();
    }).catch(function error(msg) {
        $scope.showErrorMsg(msg);
    }).finally(function () {
        $scope.hideLoading();
    });

    domainCfgService.getOrdersCfg().then(function (data) {
        $scope.oCfg = data.data;
    }).catch(function error(msg) {
        $scope.showErrorMsg(msg);
    });
    $scope.showLoading();
    trnService.getStatusMandatory().then(function (data) {
        $scope.transConfig = data.data;
    }).catch(function error(msg) {
        $scope.showErrorMsg(msg);
    }).finally(function () {
        $scope.hideLoading();
    });
    $scope.showLoading();
    trnService.getMatStatus("r", false).then(function (data) {
        $scope.matstatus = data.data;
        counter++;
        $scope.initShipmentItems();
    }).catch(function error(msg) {
        $scope.showErrorMsg(msg);
    }).finally(function () {
        $scope.hideLoading();
    });
    $scope.showLoading();
    trnService.getMatStatus("r", true).then(function (data) {
        $scope.tempmatstatus = data.data;
        counter++;
        $scope.initShipmentItems();
    }).catch(function error(msg) {
        $scope.showErrorMsg(msg);
    }).finally(function () {
        $scope.hideLoading();
    });

    $scope.initShipmentItems = function () {
        if (counter > 2) {
            angular.copy($scope.shipment.items, $scope.masterIts);
            if (checkNotNullEmpty($scope.shipment.items)) {
                $scope.shipment.items.forEach(function (its) {
                    its.naq = its.aq;
                    var status;
                    if (its.tm) {
                        status = checkNotNullEmpty($scope.tempmatstatus) ? $scope.tempmatstatus : undefined;
                    } else {
                        status = checkNotNullEmpty($scope.matstatus) ? $scope.matstatus : undefined;
                    }
                    if (!its.isBa) {
                        if (checkNotNullEmpty(status)) {
                            if (checkNotNullEmpty(its.smst) && status.indexOf(its.smst) != -1) {
                                its.fmst = its.smst;
                            } else {
                                its.fmst = checkNotNullEmpty(status) ? status[0] : undefined;
                            }
                        }
                        its.frsn = '';
                        its.qq = its.q;
                    }
                    its.fq = its.q;
                    if (checkNotNullEmpty(its.bts)) {
                        its.bts.forEach(function (data) {
                            data.fq = data.q;
                            data.sq = data.q;
                            data.frsn = '';
                            if (checkNotNullEmpty(status)) {
                                if (checkNotNullEmpty(data.smst) && status.indexOf(data.smst) != -1) {
                                    data.fmst = data.smst;
                                } else {
                                    data.fmst = checkNotNullEmpty(status) ? status[0] : undefined;
                                }
                            }
                        });
                    }
                });
            }
        }
    };

    $scope.toggle = function (index) {
        $scope.select(index);
    };

    $scope.exRow = [];
    $scope.select = function (index, type) {
        var empty = '';
        if ($scope.exRow.length == 0) {
            for (var i = 0; i < $scope.shipment.items.length; i++) {
                $scope.exRow.push(empty);
            }
        }
        if (type == 'show') {
            $scope.saveDisable = true;
            $scope.saveCounter++;
            $scope.shipment.items[index].hideEdit = true;
        }
        if (checkNullEmpty(type)) {
            $scope.saveCounter--;
            if ($scope.saveCounter == 0) {
                $scope.saveDisable = false;
            }
            $scope.shipment.items[index].hideEdit = false;
        }
        $scope.exRow[index] = $scope.exRow[index] === type ? empty : type;
        redrawPopup($scope, $scope.shipment.items, 'hide', $timeout);
    };

    $scope.fulfil = function (shipmentIts) {
        var ind = -1;
        var isInvalid = shipmentIts.some(function (data) {
            if (!data.isBa) {
                return !($scope.validate(data, ++ind) && $scope.validate(data, ind, 'r'));
            }
        });
        ind = 0;
        var isStatusEmpty = 0;
        shipmentIts.forEach(function (data) {
            var status = data.tm ? $scope.tempmatstatus : $scope.matstatus;
            if (!data.isBa) {
                if (data.fq > 0 && checkNullEmpty(data.fmst)) {
                    data.isVisitedStatus = true;
                    if (checkNotNullEmpty(status) && $scope.transConfig.rsm && !$scope.validate(data, ind, data.tm ? "cmt" : "cm")) {
                        isStatusEmpty += 1;
                    }
                }
            } else {
                var index = 0;
                var source = data.tm ? "smt" : "sm";
                if (data.bts.length > 0) {
                    data.bts.forEach(function (det) {
                        if (checkNotNullEmpty(status) && det.q > 0 && checkNullEmpty(det.fmst) && $scope.transConfig.rsm) {
                            $scope.showWarning($scope.resourceBundle['material.status.required']);
                            showPopup($scope, det, source + data.mId, $scope.resourceBundle['status.required'], index, $timeout, false, true);
                            isStatusEmpty += 1;
                        }
                        index += 1;
                    });
                }
            }
            ind += 1;
        });
        if (isInvalid || isStatusEmpty > 0) {
            return;
        }
        if (checkNullEmpty($scope.shipData.afd)) {
            $scope.showWarning($scope.resourceBundle['shipment.provide.date.of.actual.receipt']);
            return;
        }
        shipmentIts.forEach(function (d) {
            d.bq = angular.copy(d.bts);
            if (!$scope.allocate) {
                d.isBa = false;
            }
            d.fq = d.fq || 0;
            if (checkNotNullEmpty(d.bq)) {
                d.bq.forEach(function (dd) {
                    dd.fq = dd.fq || 0;
                    dd.q = dd.sq || 0;
                    if (dd.sq == dd.fq) {
                        dd.frsn = undefined;
                    } else {
                        if (checkNotNullEmpty(dd.frsn) && dd.frsn != "Others") {
                            dd.mrsn = undefined;
                        }
                        dd.frsn = dd.mrsn || dd.frsn || null;
                    }
                });
            }
            if (!d.isBa) {
                if (d.fq == d.qq) {
                    d.frsn = undefined;
                } else {
                    if (checkNotNullEmpty(d.frsn) && d.frsn != "Others") {
                        d.mrsn = undefined;
                    }
                    d.frsn = d.mrsn || d.frsn || null;
                }
            }
        });
        var afd = formatDate($scope.shipData.afd);
        var msg = $scope.shipData.msg;
        var shipData = {
            items: shipmentIts,
            sId: $scope.shipment.sId,
            kid: $scope.shipment.customerId,
            afd: afd,
            msg: msg,
            isFulfil: true,
            isOrderFulfil: $scope.isOrderFulfil,
            orderUpdatedAt: $scope.order ? $scope.order.orderUpdatedAt : $scope.shipment.orderUpdatedAt
        };
        $scope.showLoading(false, $scope.resourceBundle['order.fulfilling.message']);
        ordService.updateShipment(shipData).then(function (data) {
            $scope.$emit("updateFulifilmentdata", data);
            $scope.showPartialSuccessMsg(data);
            shipmentIts.forEach(function (d) {
                d.bts = angular.copy(d.bq);
                if (!$scope.isOrderFulfil) {
                    $scope.constructDiscrepancyData(d);
                }
            });
            $scope.toggleFulfil(true, checkNotNullEmpty(msg));
        }).catch(function error(msg) {
            $scope.showErrorMsg(msg);
        }).finally(function () {
            $scope.hideLoading();
        });
    };

    $scope.hidePop = function (material, index, source) {
        if (source == 'r') {
            hidePopup($scope, material, 'fr' + (checkNotNullEmpty($scope.oCfg.pfr) ? 'r' : '') + material.mId, index, $timeout, true);
        } else if (checkNotNullEmpty(source)) {
            hidePopup($scope, material, source + material.mId, index, $timeout, false, true);
        } else {
            hidePopup($scope, material, 'f' + material.mId, index, $timeout);
        }
    };

    $scope.validate = function (material, index, source) {
        if (source == 'r') {
            if (material.fq != material.qq && ($scope.oCfg.pfrm || (checkNotNullEmpty(material.frsn) && material.frsn.toLowerCase() == 'others'))) {
                if ((checkNullEmpty($scope.oCfg.pfr) || checkNullEmpty(material.frsn) || material.frsn.toLowerCase() == 'others')
                    && checkNullEmpty(material.mrsn)) {
                    showPopup($scope, material, 'fr' + (checkNotNullEmpty($scope.oCfg.pfr) ? 'r' : '') + material.mId, $scope.resourceBundle['reason.required'], index, $timeout, true);
                    return false;
                } else {
                    $scope.hidePop(material, index, source);
                }
            }
        } else if (checkNotNullEmpty(source)) {
            if (!material.isBa && material.fq > 0 && checkNullEmpty(material.fmst) && material.isVisitedStatus) {
                var status = material.tm ? $scope.tempmatstatus : $scope.matstatus;
                if (checkNotNullEmpty(status) && $scope.transConfig.rsm) {
                    showPopup($scope, material, source + material.mId, $scope.resourceBundle['status.required'], index, $timeout, false, true);
                    return false;
                }
            }
        } else if (checkNotNullEmpty(material.huName) && checkNotNullEmpty(material.huQty) && checkNotNullEmpty(material.fq) && material.fq % material.huQty != 0) {
            showPopup($scope, material, 'f' + material.mId, messageFormat($scope.resourceBundle['transaction.handling.units.mismatch'], material.fq, material.mnm, material.huName, material.huQty, material.mnm), index, $timeout);
            return false;
        }
        return true;
    };

    $scope.checkShowReason = function () {
        $scope.showReason = $scope.shipment.items.some(function (data) {
            if (data.fq != data.q) {
                return true;
            }
        });
    };
}]);

ordControllers.controller('ShipmentDetailCtrl', ['$scope', '$sce', 'ordService', 'transporterServices','requestContext', 'ORDER', '$uibModal', 'userService', '$timeout', 'conversationService', 'trnService', 'domainCfgService', 'ORDERSTATUSTEXT', 'PATCH_OPERATION_TYPES', 'PATCH_PATHS', 'DELIVERY_REQUEST_STATUSES', '$location',
    function ($scope, $sce, ordService, transporterServices, requestContext, ORDER, $uibModal, userService, $timeout, conversationService, trnService, domainCfgService, ORDERSTATUSTEXT, PATCH_OPERATION_TYPES, PATCH_PATHS, DELIVERY_REQUEST_STATUSES, $location) {
        windowScrollTop();
        $scope.ORDER = ORDER;
        $scope.ORDERSTATUSTEXT = ORDERSTATUSTEXT;
        $scope.statusList = [];
        $scope.sid = requestContext.getParam("sid");
        $scope.selection = "shipDetail";
        $scope.newStatus = {};
        $scope.edit = {};
        $scope.view = 'consignment';
        $scope.batchOrder = true;
        $scope.masterIts = [];
        $scope.today = new Date();
        $scope.saveCounter = 0;
        $scope.deliveryRequest = null;
        $scope.dispDeliveryStatus = false;
        var action = requestContext.getParam("action");
        var counter = 0;
        const beforeShipStatuses = [ORDER.OPEN, ORDER.PENDING, ORDER.READY_FOR_DISPATCH];
        $scope.resetMsgUsers = function () {
            $scope.newStatus.users = [];
        };
        $scope.showLoading();
        ordService.getOrderReasons("cor").then(function (data) {
            $scope.cancelReasons = data.data;
            if (checkNotNullEmpty($scope.cancelReasons)) {
                var rsn = "others";
                var isMatched = $scope.cancelReasons.some(function (arval) {
                    return rsn == arval.toLowerCase();
                });
                if (!isMatched) {
                    $scope.cancelReasons = $scope.cancelReasons.concat("Others");
                }
                $scope.cancelReasons.unshift("");
            }
        }).catch(function error(msg) {
            $scope.showErrorMsg(msg);
        }).finally(function () {
            $scope.hideLoading();
        });
        $scope.checkStatusList = function () {
            if ($scope.dp.vp) {
                $scope.statusList = [];
                return;
            }

            switch ($scope.shipment.statusCode) {
                case ORDER.OPEN:
                    if ($scope.shipment.atv) {
                        $scope.statusList.push(ORDER.READY_FOR_DISPATCH, ORDER.SHIPPED, ORDER.CANCELLED);
                    }
                    break;
                case ORDER.READY_FOR_DISPATCH:
                    if ($scope.shipment.atv) {
                        $scope.statusList = [ORDER.SHIPPED, ORDER.CANCELLED];
                    }
                    break;
                case ORDER.SHIPPED:
                    $scope.statusList = [];
                    if ($scope.shipment.atv) {
                        $scope.statusList.push(ORDER.CANCELLED);
                    }
                    if ($scope.shipment.atc) {
                        $scope.statusList.push(ORDER.FULFILLED);
                    }
                    break;
                default:
                    $scope.statusList = [];
            }
        };
        $scope.getShipmentItems = function () {
            if (counter > 1) {
                $scope.shipment.items.forEach(function (d) {
                    d.bts = angular.copy(d.bq);
                    d.ts = d.tm;
                    if (checkNullEmpty(d.smst)) {
                        if (d.tm) {
                            d.smst = checkNotNullEmpty($scope.tempmatstatus) ? $scope.tempmatstatus[0] : "";
                        } else {
                            d.smst = checkNotNullEmpty($scope.matstatus) ? $scope.matstatus[0] : "";
                        }
                    }
                    $scope.constructDiscrepancyData(d);
                });
            }
        };
        $scope.constructDiscrepancyData = function (material) {
            if (material.isBa) {
                material.bts.forEach(function (data) {
                    data.fdrsn = '';
                    if (data.q != data.fq) {
                        data.fdrsn += '<p align="left">' + '<span class="codegray"> ' + $scope.resourceBundle['shipped'] + ': </span> ' + data.q + '<br/>' + '<span class="codegray"> ' + $scope.resourceBundle['fulfilled'] + ': </span>' + data.fq;
                        if (checkNotNullEmpty(data.frsn)) {
                            data.fdrsn += '<br/> <span class="codegray"> ' + $scope.resourceBundle['shipment.reason.for.fulfilment.discrepancy'] + ': </span> <br/> ' + data.frsn;
                        }
                        data.fdrsn += '</p>';
                    }
                });
            } else {
                if (material.q != material.fq) {
                    material.fdrsn += '<p align="left">' + '<span class="codegray"> ' + $scope.resourceBundle['shipped'] + ': </span> ' + material.q + '<br/>' + '<span class="codegray"> '+ $scope.resourceBundle['fulfilled'] + ': </span>' + material.fq;
                    if (checkNotNullEmpty(material.frsn)) {
                        material.fdrsn = '<br/> <span class="codegray"> ' + $scope.resourceBundle['shipment.reason.for.fulfilment.discrepancy'] + ': </span> <br/> ' + material.frsn;
                    }
                    material.fdrsn += '</p>';
                }
            }
        };
        $scope.getShipment = function () {
            $scope.showLoading();
            ordService.getShipment($scope.sid).then(function (data) {
                $scope.shipment = data.data;
                $scope.shipment.trackingId = $scope.shipment.trackingId || '';
                $scope.rsn = $scope.shipment.reason;
                $scope.sicrsn = $scope.shipment.cdrsn;
                $scope.shipment.tead = parseUrlDate($scope.shipment.ead);
                $scope.beforeShip = $scope.shipment.statusCode == ORDER.OPEN
                    || $scope.shipment.statusCode == ORDER.READY_FOR_DISPATCH;
                $scope.showLoading();
                var configPromise = domainCfgService.getOrdersCfg().then(function (data) {
                    $scope.oCfg = data.data;
                    if (checkNotNullEmpty($scope.oCfg.ps)) {
                        $scope.packageSize = $scope.oCfg.ps.split(',');
                        $scope.packageSize.unshift("");
                        if (checkNullEmpty($scope.ps)) {
                            $scope.ps = $scope.packageSize[0];
                        }
                    }
                    $scope.isTMand = $scope.oCfg.tm;
                }).catch(function error(msg) {
                    $scope.showErrorMsg(msg);
                }).finally(function () {
                    $scope.hideLoading();
                });
                if (checkNotNullEmpty($scope.shipment.items)) {
                    $scope.showLoading();
                    trnService.getMatStatus("i", false).then(function (data) {
                        $scope.matstatus = data.data;
                        counter++;
                        $scope.getShipmentItems();
                    }).catch(function error(msg) {
                        $scope.showErrorMsg(msg);
                    }).finally(function () {
                        $scope.hideLoading();
                    });
                    $scope.showLoading();
                    trnService.getMatStatus("i", true).then(function (data) {
                        $scope.tempmatstatus = data.data;
                        counter++;
                        $scope.getShipmentItems();
                    }).catch(function error(msg) {
                        $scope.showErrorMsg(msg);
                    }).finally(function () {
                        $scope.hideLoading();
                    });
                    $scope.showLoading();
                    trnService.getStatusMandatory().then(function (data) {
                        $scope.transConfig = data.data;
                    }).catch(function error(msg) {
                        $scope.showErrorMsg(msg);
                    }).finally(function () {
                        $scope.hideLoading();
                    });
                    /*userService.getUsers($scope.shipment.customerId).then(function (data) {
                     $scope.entityUsers = data.data.results;
                     }).catch(function error(msg) {
                     $scope.showErrorMsg(msg);
                     });*/
                    /*if (checkNotNullEmpty($scope.shipment.vendorId)) {
                     userService.getUsers($scope.shipment.vendorId, $scope.shipment.customerId).then(function (data) {
                     $scope.vendorUsers = data.data.results;
                     }).catch(function error(msg) {
                     $scope.showErrorMsg(msg);
                     });
                     }*/
                    var userPromise = userService.getUsersByRole('ROLE_do').then(function (data) {
                        $scope.administrators = data.data.results;
                        if (checkNotNullEmpty($scope.administrators) && $scope.administrators.length > 0) {
                            $scope.admins = true;
                        }
                        $scope.checkStatusList();
                        $scope.getStatusHistory();
                    }).catch(function error(msg) {
                        $scope.showErrorMsg(msg);
                    }).finally(function () {
                        $scope.hideLoading();
                    });
                    Promise.all([userPromise, configPromise]).then(function() {
                        return getDeliveryRequests();
                    }).then(function() {
                        performAction();
                    })
                }

            }).catch(function error(msg) {
                $scope.showErrorMsg(msg);
            }).finally(function () {
                $scope.hideLoading();
            });
        };

        function performAction() {
            if(action == 'ship') {
                $scope.changeStatus(ORDER.SHIPPED);
            } else if(action == 'delivery_request') {
                $scope.changeStatus(ORDER.REQUEST_DELIVERY);
            } else if(action == 'ready_dispatch') {
                $scope.changeStatus(ORDER.READY_FOR_DISPATCH);
            }
            $location.search('action', null);
            action = "";
        }
        $scope.getShipment();
        $scope.getStatusHistory = function () {
            $scope.showLoading();
            ordService.getStatusHistory($scope.sid, 'SHIPMENT', null).then(function (data) {
                if (checkNotNullEmpty(data.data) && checkNotNullEmpty(data.data.results)) {
                    $scope.history = data.data.results;
                    var hMap = {};
                    var pVal;
                    $scope.history.forEach(function (data) {
                        if (checkNullEmpty(hMap[data.newValue])) {
                            hMap[data.newValue] = {
                                "status": ORDER.statusTxt[data.newValue],
                                "updatedon": data.createDate,
                                "updatedby": data.userName,
                                "updatedId": data.userId
                            };
                            if (ORDER.CANCELLED == data.newValue) {
                                pVal = data.prevValue;
                            }
                        }
                    });
                    $scope.si = [];
                    var end = false;
                    var siInd = 0;

                    function constructStatus(stCode, stText) {
                        if ($scope.shipment.statusCode != ORDER.CANCELLED || !end) {
                            $scope.si[siInd] = (!end && hMap[stCode]) ? hMap[stCode] : {
                                "status": stText,
                                "updatedon": "",
                                "updatedby": ""
                            };
                            $scope.si[siInd].completed = $scope.shipment.statusCode == stCode ? "end" : (end ? "false" : "true");
                            siInd += 1;
                        }
                        if (!end) {
                            end = $scope.shipment.statusCode == stCode || ($scope.shipment.statusCode == ORDER.CANCELLED && pVal == stCode);
                        }
                    }

                    constructStatus(ORDER.OPEN, ORDER.statusTxt[ORDER.OPEN]);
                    if($scope.shipment.statusCode == ORDER.OPEN || hMap[ORDER.READY_FOR_DISPATCH]) {
                        constructStatus(ORDER.READY_FOR_DISPATCH, ORDER.statusTxt[ORDER.READY_FOR_DISPATCH]);
                    }
                    constructStatus(ORDER.SHIPPED, ORDER.statusTxt[ORDER.SHIPPED]);
                    constructStatus(ORDER.FULFILLED, ORDER.statusTxt[ORDER.FULFILLED]);
                    if ($scope.shipment.statusCode == ORDER.CANCELLED) {
                        $scope.si[siInd] = hMap[ORDER.CANCELLED];
                        $scope.si[siInd].completed = "cancel";
                    }
                    if ($scope.shipment.statusCode == ORDER.SHIPPED) {
                        $scope.shipDate = string2Date(hMap[ORDER.SHIPPED].updatedon, "dd/mm/yyyy", "/", true);
                    }
                }
            }).finally(function () {
                $scope.hideLoading();
            });
        };

        function getMessageCount() {
            conversationService.getMessagesByObj('SHIPMENT', $scope.sid, 0, 1, true).then(function (data) {
                if (checkNotNullEmpty(data.data)) {
                    $scope.messageCnt = data.data.numFound;
                }
            }).catch(function error(msg) {
                $scope.showErrorMsg(msg);
            }).finally(function () {
                $scope.mLoading = false;
                if (offset == 0) {
                    $scope.hideLoading();
                }
            });
        }

        getMessageCount();

        function filterActiveDeliveryRequest(deliveryRequests) {
            $scope.deliveryRequest = null;
            if(deliveryRequests != null) {
                deliveryRequests.forEach(function(dr) {
                    if(dr.st != 'dr_cn') { // delivery request is not cancelled
                        $scope.deliveryRequest = dr;
                    }
                });
                if($scope.deliveryRequest == null &&
                    (($scope.shipment.statusCode == ORDER.READY_FOR_DISPATCH
                    && ($scope.shipment.atv || ($scope.shipment.atc && !$scope.oCfg.disableDeliveryReqByCustomer))))) {
                    populateRequestForDeliveryAction();
                }
            }
        }

        function populateRequestForDeliveryAction() {
            if($scope.statusList.indexOf(ORDER.REQUEST_DELIVERY) == -1) {
                $scope.statusList.unshift(ORDER.REQUEST_DELIVERY);
            }
        }

        function getDeliveryRequests() {
            $scope.showLoading();
            return ordService.fetchDeliveryRequests($scope.sid, false).then(function (data) {
                if(checkNotNullEmpty(data.data)) {
                    $scope.drCnt = data.data.numFound;
                    filterActiveDeliveryRequest(data.data.results); //deliveryRequest array
                }
            }).catch(function error(msg) {
                $scope.showErrorMsg(msg);
            }).finally(function () {
                $scope.hideLoading();
            });
        }

        $scope.toggleDispDeliveryStatus = function() {
            $scope.dispDeliveryStatus = !$scope.dispDeliveryStatus;
        };

        $scope.getTrustedTrackingUrl = function() {
            return $sce.trustAsResourceUrl($scope.deliveryRequest['trURL']);
        };

        $scope.setMessageCount = function (count) {
            $scope.messageCnt = count;
        };
        $scope.toggleFulfil = function (update, data) {
            if ($scope.selection == 'shipDetail') {
                $scope.selection = "fulfilShipment";
            } else {
                $scope.selection = "shipDetail";
                if (update) {
                    $scope.shipment.statusCode = $scope.ORDER.FULFILLED;
                    $scope.checkStatusList();
                    $scope.getStatusHistory();
                    getMessageCount();
                } else {
                    angular.copy(data, $scope.shipment.items);
                }
            }
        };

        $scope.changeStatus = function (value) {
            $scope.nStatus = value;
            $scope.newStatus = {};
            $scope.newStatus.st = value;
            $scope.newStatus.ncdrsn = '';
            $scope.newStatus.cmrsn = '';
            if (value == $scope.ORDER.FULFILLED) {
                $scope.toggleFulfil();
                return;
            }
            if (value == $scope.ORDER.SHIPPED) {
                if ($scope.allocate) {
                    var isInvalid = $scope.shipment.items.some(function (d) {
                        return d.aq != d.q;
                    });
                    if (isInvalid) {
                        $scope.showWarning($scope.resourceBundle['order.allocations.required.toship']);
                        return;
                    }
                }
                if ($scope.oCfg.tm && checkNullEmpty($scope.shipment.transporter)) {
                    $scope.showWarning($scope.resourceBundle['transportermandatory']);
                    return;
                }
                if ($scope.oCfg.ridm && (checkNullEmpty($scope.shipment.salesRefId) || checkNullEmpty($scope.shipment.salesRefId.trim()))) {
                    $scope.showWarning($scope.resourceBundle['sales.reference.id.mandatory.error']);
                    return;
                } else if ($scope.oCfg.eadm && checkNullEmpty($scope.shipment.ead)) {
                    $scope.showWarning($scope.resourceBundle['estimated.date.of.arrival.mandatory.error']);
                    return;
                }
            }
            if(value == $scope.ORDER.REQUEST_DELIVERY) {
                if(checkNotNullEmpty($scope.deliveryRequest)) {
                    $scope.showWarning($scope.resourceBundle['active.dr.exists']);
                } else if($scope.shipment.statusCode == ORDER.READY_FOR_DISPATCH) {
                    $scope.createDeliveryRequest('shipment');
                } else if(beforeShipStatuses.indexOf($scope.shipment.statusCode) > -1) {
                    $scope.showWarning($scope.resourceBundle['move.shipment.to.rd']);
                }
                return;
            }
            $scope.modalInstance = $uibModal.open({
                templateUrl: 'views/orders/order-status.html',
                scope: $scope,
                keyboard: false,
                backdrop: 'static'
            });
        };

        $scope.createDeliveryRequest = function (type) {
            $scope.selection = "createDelReqs";
            $scope.delReq = {'type': type};
            $scope.$apply();
        };

        $scope.openView = function(view) {
            $scope.selection = view;
        };

        $scope.$on("updateFulifilmentdata", function (evt, data) {
            $scope.shipment.afd = data.config.data.afd;
        });

        $scope.cancel = function () {
            $scope.modalInstance.dismiss('cancel');
        };

        $scope.saveStatus = function () {
            if ($scope.newStatus.st == ORDER.CANCELLED) {
                if ($scope.oCfg.corm || $scope.newStatus.ncdrsn == 'Others') {
                    if ((checkNullEmpty($scope.oCfg.cor) || checkNullEmpty($scope.newStatus.ncdrsn) || $scope.newStatus.ncdrsn == 'Others')
                        && checkNullEmpty($scope.newStatus.cmrsn)) {
                        $scope.showWarning($scope.resourceBundle["provide.reason"]);
                        return;
                    }
                }
                if (checkNullEmpty($scope.oCfg.cor) || $scope.newStatus.ncdrsn == 'Others') {
                    $scope.newStatus.cdrsn = $scope.newStatus.cmrsn;
                } else {
                    $scope.newStatus.cdrsn = $scope.newStatus.ncdrsn;
                }
            }
            $scope.newStatus.efd = checkNotNullEmpty($scope.newStatus.efd) ? $scope.newStatus.efd : undefined;
            $scope.newStatus.orderUpdatedAt = $scope.shipment.orderUpdatedAt;
            $scope.statusLoading = true;
            $scope.showLoading();
            ordService.updateShipmentStatus($scope.sid, $scope.newStatus).then(function (data) {
                $scope.showPartialSuccessMsg(data);
                $scope.getShipment();
                getMessageCount();
                $scope.cancel();
                $scope.shipment.statusCode = $scope.newStatus.st;
                $scope.checkStatusList();
                var upMsg = {};
                upMsg.objId = $scope.sid;
                upMsg.objType = "SHIPMENT";
                upMsg.offset = 0;
                $scope.$broadcast("updateMessage", upMsg);
                $scope.sicrsn = $scope.newStatus.cdrsn;
                $scope.newStatus = {};
            }).catch(function error(msg) {
                $scope.showErrorMsg(msg);
            }).finally(function () {
                $scope.statusLoading = false;
                $scope.hideLoading();
            });
        };

        $scope.updateShipmentField = function (field, content) {
            var patchPath = '';
            switch (field) {
                case 'trackid' :
                    $scope.showLoading();
                    ordService.updateShipmentTrackingId(content, $scope.sid, $scope.shipment.orderUpdatedAt).then(function (data) {
                        $scope.shipment.trackingId = $scope.shipment.tempTrackingId;
                        $scope.showSuccess(data.data.respMsg);
                        $scope.shipment.orderUpdatedAt = data.data.orderUpdatedAt;
                        $scope.toggleEdit(field);
                    }).catch(function error(msg) {
                        $scope.showErrorMsg(msg);
                    }).finally(function () {
                        $scope.hideLoading();
                    });
                    break;
                case 'sefd':
                    if (checkNullEmpty($scope.shipment.ead) && checkNullEmpty(content)) {
                        $scope.showWarning($scope.resourceBundle['shipment.enter.estimated.arrival.date']);
                        return;
                    }
                    if (checkNullEmpty($scope.shipment.ead) || checkNotNullEmpty($scope.shipment.ead)) {
                        content = formatDate(content);
                    }
                    $scope.showLoading();
                    ordService.updateShipmentDate(content, $scope.sid, $scope.shipment.orderUpdatedAt).then(function (data) {
                        $scope.shipment.eadLabel = cleanupString(data.data.respMsg);
                        $scope.shipment.ead = $scope.shipment.tead;
                        $scope.showSuccess($scope.resourceBundle['ead.updated']);
                        $scope.shipment.orderUpdatedAt = data.data.orderUpdatedAt;
                        $scope.toggleEdit(field);
                    }).catch(function error(msg) {
                        $scope.showErrorMsg(msg);
                    }).finally(function () {
                        $scope.hideLoading();
                    });
                    break;
                case 'ps':
                    $scope.showLoading();
                    ordService.updateShipmentPackageSize(content, $scope.sid, $scope.shipment.orderUpdatedAt).then(function (data) {
                        $scope.shipment.ps = $scope.shipment.tempPS;
                        $scope.showSuccess(data.data.respMsg);
                        $scope.shipment.orderUpdatedAt = data.data.orderUpdatedAt;
                        $scope.toggleEdit(field);
                    }).catch(function error(msg) {
                        $scope.showErrorMsg(msg);
                    }).finally(function () {
                        $scope.hideLoading();
                    });
                    break;
                case 'phnm':
                    $scope.showLoading();
                    ordService.updateTrackingPhoneNumber(content, $scope.sid, $scope.shipment.orderUpdatedAt).then(function (data) {
                        $scope.shipment.phonenum = $scope.shipment.tempPhonenum;
                        $scope.showSuccess(data.data.respMsg);
                        $scope.shipment.orderUpdatedAt = data.data.orderUpdatedAt;
                        $scope.toggleEdit(field);
                    }).catch(function error(msg) {
                        $scope.showErrorMsg(msg);
                    }).finally(function () {
                        $scope.hideLoading();
                    });
                    break;
                case 'salesRefId':
                    $scope.showLoading();
                    ordService.updateShipmentReferenceId(content, $scope.sid, $scope.shipment.orderUpdatedAt)
                    .then(function (data) {
                        $scope.shipment.salesRefId = $scope.shipment.tempSalesRefId;
                        $scope.showSuccess(data.data.respMsg);
                        $scope.shipment.orderUpdatedAt = data.data.orderUpdatedAt;
                        $scope.toggleEdit(field);
                    }).catch(function error(msg) {
                        $scope.showErrorMsg(msg);
                    }).finally(function () {
                        $scope.hideLoading();
                    });
                    break;
                case 'numpckg':
                    patchPath = PATCH_PATHS.orders['CONSIGNMENT_PACKAGE_CNT'];
                    patchShipmentConsignmentDetails(content, field, 'pkgCnt', patchPath);
                    break;
                case 'wt':
                    patchPath = PATCH_PATHS.orders['CONSIGNMENT_WEIGHT'];
                    patchShipmentConsignmentDetails(content, field, 'wt', patchPath);
                    break;
                case 'vol':
                    patchPath = PATCH_PATHS.orders['CONSIGNMENT_DIMENSIONS'];
                    patchShipmentConsignmentDetails(content, field, 'dimension', patchPath);
                    break;
                case 'declaration':
                    patchPath = PATCH_PATHS.orders['CONSIGNMENT_DECLARATION'];
                    patchShipmentConsignmentDetails(content, field, 'declaration', patchPath);
                    break;
                case 'value':
                    patchPath = PATCH_PATHS.orders['CONSIGNMENT_VALUE'];
                    patchShipmentConsignmentDetails(content, field, 'value', patchPath);
                    break;
                case 'vehicle':
                    patchPath = PATCH_PATHS.orders['SHIPMENT_VEHICLE'];
                    patchShipmentDetails(content, field, 'vehicle', patchPath);
                    break;
            }
        };

        $scope.updateTransporterDetails = function(transporter) {
            const patches = [];
            if(typeof transporter === 'string') {
                patches.push({val: transporter, path: PATCH_PATHS.orders['SHIPMENT_TRANSPORTER_NAME'], op:PATCH_OPERATION_TYPES['REPLACE']},
                    {val: "", path: PATCH_PATHS.orders['SHIPMENT_TRANSPORTER_ID'], op:PATCH_OPERATION_TYPES['REPLACE']})
            } else if(checkNotNullEmpty(transporter.id)) {
                patches.push({val: transporter.id, path: PATCH_PATHS.orders['SHIPMENT_TRANSPORTER_ID'], op:PATCH_OPERATION_TYPES['REPLACE']},
                    {val: transporter.name, path: PATCH_PATHS.orders['SHIPMENT_TRANSPORTER_NAME'], op:PATCH_OPERATION_TYPES['REPLACE']},
                    {val: transporter.phnm, path: PATCH_PATHS.orders['SHIPMENT_PHONE_NUM'], op:PATCH_OPERATION_TYPES['REPLACE']})
            }
            $scope.showLoading();
            updateShipmentDetails($scope.sid, patches, $scope.shipment.orderUpdatedAt).then(function (data) {
                if(typeof transporter === 'string') {
                    $scope.shipment.transporter = transporter;
                } else {
                    $scope.shipment.transporter = transporter.name;
                    $scope.shipment.phonenum = transporter.phnm;
                }
                $scope.toggleEdit('transp');
            }).catch(function error(msg) {
                $scope.showErrorMsg(msg);
            }).finally(function () {
                $scope.hideLoading();
            });
        };

        function patchShipmentConsignmentDetails(updValue, editField, consignmentFieldName, patchPath) {
            $scope.showLoading();
            updateShipmentDetails($scope.sid, [{val: updValue, path: patchPath, op: PATCH_OPERATION_TYPES['REPLACE']}], $scope.shipment.orderUpdatedAt).then(function (data) {
                $scope.shipment.consignment[consignmentFieldName] = updValue;
                $scope.toggleEdit(editField);
            }).catch(function error(msg) {
                $scope.showErrorMsg(msg);
            }).finally(function () {
                $scope.hideLoading();
            });
        }

        function patchShipmentDetails(updValue, editField, fieldName, patchPath) {
            $scope.showLoading();
            updateShipmentDetails($scope.sid, [{val: updValue, path: patchPath, op: PATCH_OPERATION_TYPES['REPLACE']}], $scope.shipment.orderUpdatedAt)
              .then(function () {
                $scope.shipment[fieldName] = updValue;
                $scope.toggleEdit(editField);
            }).catch(function error(msg) {
                $scope.showErrorMsg(msg);
            }).finally(function () {
                $scope.hideLoading();
            });
        }

        function updateShipmentDetails(sId, patches, orderUpdatedAt) {
            return ordService.updateShipmentDetails(sId, patches, orderUpdatedAt)
                .then(function (data) {
                    $scope.showSuccess(data.data.respMsg);
                    $scope.shipment.orderUpdatedAt = data.data.orderUpdatedAt;
                });
        }

        $scope.saveShipment = function () {
            var ind = 0;
            var isInvalid = $scope.shipment.items.some(function (data) {
                return !$scope.validate(data, ind++, 's');
            });
            if (isInvalid) {
                return;
            }
            ind = 0;
            var isStatusEmpty = 0;
            $scope.shipment.items.forEach(function (data) {
                if (!data.isBa && data.naq > 0 && checkNullEmpty(data.smst)) {
                    var status = data.tm ? $scope.tempmatstatus : $scope.matstatus;
                    data.isVisitedStatus = true;
                    if (checkNotNullEmpty(status) && $scope.transConfig.ism && !$scope.validateStatus(data, ind, data.tm ? "cmt" : "cm")) {
                        isStatusEmpty += 1;
                    }
                }
                ind += 1;
            });
            if (isStatusEmpty > 0) {
                return;
            }
            var shipment = {};
            shipment.sId = $scope.sid;
            shipment.kid = $scope.shipment.vendorId;
            shipment.orderUpdatedAt = $scope.shipment.orderUpdatedAt;
            shipment.items = angular.copy($scope.shipment.items);
            shipment.items.forEach(function (d) {
                if (d.isBa) {
                    var oaq = {};
                    d.bq.forEach(function (dd) {
                        oaq[dd.id] = dd.q;
                    });
                    d.bq = [];
                    d.bts.forEach(function (dd) {
                        if (checkNotNullEmpty(oaq[dd.id]) || checkNotNullEmpty(dd.q)) {
                            d.bq.push({id: dd.id, q: dd.q || 0, smst: dd.mst});
                        }
                    });
                }
                d.aq = d.naq || 0;
            });
            $scope.showLoading();
            ordService.updateShipment(shipment).then(function (data) {
                $scope.showSuccess(data.data);
                $scope.toggleEdit('ship');
                //To fetch updated stocks of vendor due to allocations.
                $scope.getShipment();
            }).catch(function error(msg) {
                $scope.showErrorMsg(msg);
            }).finally(function () {
                $scope.hideLoading();
            });
        };

        $scope.cancelShipment = function () {
            var ind = 0;
            $scope.shipment.items.forEach(function (data) {
                $scope.hidePop(data, ind++);
            });
            angular.copy($scope.masterIts, $scope.shipment.items);
            $scope.toggleEdit('ship');
        };

        $scope.toggle = function (index) {
            $scope.select(index);
        };

        $scope.exRow = [];
        $scope.select = function (index, type) {
            var empty = '';
            if ($scope.exRow.length == 0) {
                for (var i = 0; i < $scope.shipment.items.length; i++) {
                    $scope.exRow.push(empty);
                }
            }
            if (type == 'show') {
                $scope.saveDisable = true;
                $scope.saveCounter++;
            }
            if (checkNullEmpty(type)) {
                $scope.saveCounter--;
                if ($scope.saveCounter == 0) {
                    $scope.saveDisable = false;
                }
            }
            $scope.exRow[index] = $scope.exRow[index] === type ? empty : type;
            redrawPopup($scope, $scope.shipment.items, 'hide', $timeout);
        };


        $scope.toggleEdit = function (field, close) {
            if (field == 'sefd' && close) {
                $scope.shipment.tead = parseUrlDate($scope.shipment.ead);
            } else if (field == 'transp') {
                $scope.shipment.tempTransporter = $scope.shipment.transporter ?
                {id: $scope.shipment.transporterId, name: $scope.shipment.transporter} : null;
            } else if (field == 'trackid') {
                $scope.shipment.tempTrackingId = $scope.shipment.trackingId;
            } else if (field == 'vehicle') {
                $scope.shipment.tempVehicle = $scope.shipment.vehicle;
            } else if (field == 'ps') {
                $scope.shipment.tempPS = $scope.shipment.ps;
            } else if (field == 'phnm') {
                $scope.shipment.tempPhonenum = $scope.shipment.phonenum;
            } else if (field == 'salesRefId') {
                $scope.shipment.tempSalesRefId = $scope.shipment.salesRefId;
            } else if (field == 'numpckg') {
                $scope.shipment.consignment['temp_pkgCnt'] = $scope.shipment.consignment['pkgCnt'];
            } else if (field == 'wt') {
                $scope.shipment.consignment['temp_wt'] = $scope.shipment.consignment['wt'];
            } else if(field == 'vol') {
                const dim = $scope.shipment.consignment['dimension'];
                $scope.shipment.consignment.temp_dimension = { l: dim.l, w: dim.w, h: dim.h };
            } else if(field == 'category') {
                $scope.shipment.consignment['temp_ctg'] = $scope.shipment.consignment['ctg'];
            } else if(field == 'declaration') {
                $scope.shipment.consignment['temp_declaration'] = $scope.shipment.consignment['declaration'];
            } else if(field == 'value') {
                $scope.shipment.consignment['temp_val'] = $scope.shipment.consignment['value'];
            }
            $scope.edit[field] = !$scope.edit[field];
        };

        $scope.toggleBatch = function (type, index) {
            $scope.shipment.items[index][type] = !$scope.shipment.items[index][type];
        };

        $scope.startEditShipment = function () {
            angular.copy($scope.shipment.items, $scope.masterIts);
            if (checkNotNullEmpty($scope.shipment.items)) {
                $scope.shipment.items.forEach(function (its) {
                    its.naq = its.aq;
                });
            }
            $scope.toggleEdit('ship');
        };

        $scope.toggleStatusHistory = function () {
            $scope.dispStatusHistory = !$scope.dispStatusHistory;
        };

        $scope.hidePop = function (material, index, source, isStatus) {
            if (checkNullEmpty(isStatus)) {
                var prefix = source == 's' ? 'at' : '';
                hidePopup($scope, material, prefix + material.mId, index, $timeout);
            } else {
                hidePopup($scope, material, source + material.mId, index, $timeout, false, true);
            }
        };
        $scope.validate = function (material, index, source) {
            var prefix = source == 's' ? 'at' : '';
            if (checkNotNullEmpty(material.naq)) {
                if (material.naq > material.q) {
                    showPopup($scope, material, prefix + material.mId, messageFormat($scope.resourceBundle['allocation.cannot.be.greater.than.ordered.quantity'], material.q), index, $timeout);
                    return false;
                }
            }
            if (checkNotNullEmpty(material.vs)) {
                if (material.naq > material.vs) {
                    showPopup($scope, material, prefix + material.mId, messageFormat($scope.resourceBundle['allocation.cannot.be.greater.than.available.stock'],  material.vs), index, $timeout);
                    return false;
                }
            }
            if (material.isBa) {
                var alStk = 0;
                var oAlStk = {};
                material.bq.forEach(function (d) {
                    oAlStk[d.id] = d.q;
                });
                if (checkNotNullEmpty(material.bdata)) {
                    material.bdata.forEach(function (d) {
                        var diff = (d.quantity || 0) - (oAlStk[d.bid] || 0);
                        if (diff < 0) {
                            alStk += diff;
                        } else if (diff > (d.oastk || 0)) {
                            alStk += diff - (d.oastk || 0);
                        }
                    });
                    if (alStk > material.aaq) {
                        showPopup($scope, material, prefix + material.mId, $scope.resourceBundle['allocation.use.allocations.from.order'], index, $timeout);
                        return false;
                    }
                }
            }
            if (checkNotNullEmpty(material.huName) && checkNotNullEmpty(material.huQty) && checkNotNullEmpty(material.naq) && material.naq % material.huQty != 0) {
                showPopup($scope, material, prefix + material.mId, messageFormat($scope.resourceBundle['handling.units.mismatch.message'], material.naq, material.mnm, material.huName, material.huQty, material.mnm), index, $timeout);
                return false;
            }
            return true;
        };

        $scope.checkHideStatus = function (status) {
            return (checkNotNullEmpty(status) && status == 'sp' || status == 'fl');
        };

        $scope.hasStatus = function (status) {
            return (checkNotNullEmpty($scope.statusList) && $scope.statusList.indexOf(status) > -1);
        };


        $scope.validateStatus = function (material, index, source) {
            if (material.naq > 0 && checkNullEmpty(material.smst) && material.isVisitedStatus) {
                var status = material.tm ? $scope.tempmatstatus : $scope.matstatus;
                if (checkNotNullEmpty(status) && $scope.transConfig.ism) {
                    showPopup($scope, material, source + material.mId, $scope.resourceBundle['status.required'], index, $timeout, false, true);
                    return false;
                }
            }
            return true;
        };
    }
]);

ordControllers.controller('ConsignmentController', ['$scope', 'returnsService', function ($scope, returnsService) {
    $scope.batchOrder = true;
    $scope.selectedMaterialIds = [];
    $scope.sel = {};
    $scope.sel.selectedRows = [];
    $scope.selectAll = function (newval) {
        $scope.sel.selectedRows = [];
        if (newval) {
            for (var i = 0; i < $scope.order.its.length; i++) {
                if ($scope.order.its[i].q != $scope.order.its[i].isq || ($scope.canReturn && $scope.order.its[i].fq > 0
                    && $scope.order.its[i].fq != $scope.order.its[i].returnedQuantity)) {
                    $scope.sel.selectedRows.push(i);
                }
            }
        }
    };
    $scope.createShipment = function () {
        if ($scope.sel.selectedRows.length > 0) {
            $scope.order.ead = parseUrlDate($scope.order.efd);
            $scope.openView("createShipment", $scope.sel.selectedRows);
        } else {
            $scope.showWarning($scope.resourceBundle['shipment.create.select.material']);
        }
    };

    $scope.doReturn = function () {
        if ($scope.sel.selectedRows.length > 0) {
            var selectedItems = (function () {
                var items = [];
                for (var i = 0; i < $scope.sel.selectedRows.length; i++) {
                    items.push(angular.copy($scope.order.its[$scope.sel.selectedRows[i]]));
                }
                return items;
            })();
            if (isReturnValid(selectedItems)) {
                returnsService.setItems(selectedItems);
                returnsService.setOrder(angular.copy($scope.order));
                $scope.setPageSelection('createReturns');
            } else {
                $scope.showWarning($scope.resourceBundle['returns.create.select.partially.fully.fulfilled.material']);
            }
        } else {
            $scope.showWarning($scope.resourceBundle['returns.create.select.material']);
        }
    };

    function isReturnValid(selectedItems) {
        return !selectedItems.some(function (item) {
            return item.fq == 0
        })
    }

}]);

ordControllers.controller('DeliveryRequestController', ['$scope', 'ordService', 'transporterServices','$location','$window', function ($scope, ordService, transporterServices, $location, $window) {

    $scope.pricingEnabled = false;
    function init () {
        $scope.transporterId = "";
        $scope.transporters = [];
        $scope.offset = 0;
        $scope.size = 100;
        $scope.pricingEnabled = false;
        $scope.dr = {shipper:{ kid: parseInt($scope.vendor.id) },
            receiver:{ kid: parseInt($scope.cust.id) },
            consignment: { dimension:{} }, trckdtls: {}};
        if(checkNotNullEmpty($scope.consignment)) {
            $scope.dr.consignment = $scope.consignment;
        }
        getTransporters();
    }
    init();
    function getTransporters() {
        $scope.loading = true;
        $scope.showLoading();
        transporterServices.getDomainTransporters(null, $scope.offset, $scope.size, true).then(function (data) {
            $scope.transporters = data.data['results'];
            selectDefaultTransporter($scope.transporters);
        }).catch(function err(msg) {
            $scope.showErrorMsg(msg);
        }).finally(function (){
            $scope.loading = false;
            $scope.hideLoading();
        });
    }

    $scope.cancelNewDeliveryRequest = function () {
        if($scope.delReq.type == 'order') {
            $scope.openView("orderDetail");
        } else if($scope.delReq.type == 'shipment') {
            $scope.openView("shipDetail");
        }
        $scope.enableScroll();
    };

    function verifyMandatoryFields() {
        if(checkNullEmpty($scope.dr.consignment.wt)) {
            throw $scope.resourceBundle['dr.wt.warning'];
        } else if(checkNullEmpty($scope.dr.consignment.pkgCnt)) {
            throw $scope.resourceBundle['dr.nmpkg.warning'];
        } else if(checkNullEmpty($scope.dr.consignment.declaration)) {
            throw $scope.resourceBundle['dr.dec.warning'];
        } else if(checkNullEmpty($scope.dr.consignment['value'])) {
            throw $scope.resourceBundle['dr.val.warning'];
        } else if(checkNullEmpty($scope.dr.trckdtls.tid)) {
            throw $scope.resourceBundle['dr.trnsp.warning'];
        } else if($scope.dr.ctgId != "0" && checkNullEmpty($scope.dr.ctgId)) {
            throw $scope.resourceBundle['dr.cat.warning'];
        }
    }

    function selectDefaultTransporter(transporters) {
        if(checkNotNullEmptyArray(transporters) && transporters.length == 1) {
            $scope.transporterId = transporters[0].id;
            $scope.onTransporterChange(transporters[0].id);
        }
    }

    function getTransporterWithId(id) {
        if(checkNotNullEmptyArray($scope.transporters)) {
            return $scope.transporters.filter(function(t) {
                return t.id == id;
            })[0];
        }
    }

    function validateTransporterAndCategoryFields() {
        return checkNotNullEmpty($scope.dr.trckdtls.tid) && checkNotNullEmpty($scope.transporterId)
            && checkNotNullEmpty($scope.transporterId) && checkNotNullEmpty($scope.goodsCtgId);
    }

    $scope.fetchShippingOptions = function () {
        if(!validateTransporterAndCategoryFields()) {
            return;
        }
        try{
            verifyMandatoryFields();
        } catch(err) {
            $scope.showErrorMsg(err);
            return;
        }
        $scope.loading = true;
        $scope.showLoading();
        ordService.fetchShippingOptions($scope.dr).then(function (data) {
            $scope.shippingOptions = data.data;
        }).catch(function err(msg) {
            $scope.showErrorMsg(msg);
        }).finally(function () {
            $scope.loading = false;
            $scope.hideLoading();
        })
    };

    $scope.newDeliveryRequest = function () {
        try{
            verifyMandatoryFields();
        } catch(err) {
            $scope.showErrorMsg(err);
            return;
        }
        if($scope.delReq.type == 'order') {
            $scope.dr.oId = $scope.drOrderId;
        } else if($scope.delReq.type == 'shipment') {
            $scope.dr.oId = $scope.drOrderId;
            $scope.dr.shipment_id = $scope.drShipmentId;
        }
        $scope.dr.dId = $scope.currentDomain;
        $scope.showLoading();
        ordService.createDeliveryRequest($scope.dr, $scope.drOrderId, $scope.drShipmentId).then(function(data) {
            $scope.showSuccess('Delivery request created successfully!');
            $window.location.reload();
        }).catch(function(err){
            $scope.showErrorMsg(err, true);
        }).finally(function () {
            $scope.hideLoading();
        });
    };

    $scope.onTransporterChange = function(id) {
        if(checkNullEmpty(id)) {
            $scope.transporterGoodsCategories = [];
            $scope.goodsCtgId = "";
            $scope.dr.trckdtls.tid = "";
            resetShippingOptions();
            return;
        }
        const transporter = getTransporterWithId(id);
        $scope.dr.trckdtls.tid = transporter.id;
        $scope.dr.trckdtls.transporter = transporter.name;
        $scope.dr.trckdtls.phone = transporter.phnm;
        $scope.pricingEnabled = transporter['price_en'];
        $scope.transporterGoodsCategories = transporter.categories;
        $scope.goodsCtgId = transporter.dfCtgId;
        resetShippingOptions();
        $scope.onCategoryChange();
    };

    function resetShippingOptions() {
        $scope.shippingOptions = null;
        $scope.dr.trckdtls.type = null;
    }

    $scope.onCategoryChange = function() {
        const categoryJson = $scope.transporterGoodsCategories.filter(function(cat) {
            return cat.id == $scope.goodsCtgId;
        })[0];
        $scope.dr.consignment.ctg = categoryJson.name;
        $scope.dr.ctgId = categoryJson.id;
    }
}]);

ordControllers.controller('DeliveryRequestListingController', ['$scope', 'ordService', 'ORDER', 'DELIVERY_REQUEST_STATUSES', function ($scope, ordService, ORDER, DELIVERY_REQUEST_STATUSES) {

    function init () {
        $scope.delivery_requests = [];
        fetchDeliveryRequests();
    }
    init();

    function fetchDeliveryRequests() {
        if($scope.objType != 'SHIPMENT' || checkNullEmpty($scope.objId)) {
            return;
        }
        $scope.loading = true;
        $scope.showLoading();
        ordService.fetchDeliveryRequests($scope.objId, false).then(function (data) {
            $scope.delivery_requests = data.data['results'];
            if(checkNotNullEmptyArray($scope.delivery_requests)) {
                $scope.delivery_requests.forEach(function(dr) {
                    populateDisplayFields(dr);
                })
            }
        }).catch(function err(msg) {
            $scope.showErrorMsg(msg);
        }).finally(function () {
            $scope.loading = false;
            $scope.hideLoading();
        })
    }

    function populateDisplayFields(dr) {
        dr.st_display = $scope.resourceBundle[DELIVERY_REQUEST_STATUSES[dr.st]];
        dr.trckdtls['tr_id_display'] = checkNotNullEmpty(dr.trckdtls['tr_id']) ? dr.trckdtls['tr_id'] : '-';
        dr.trackingIdPresent = checkNotNullEmpty(dr.trckdtls['tr_id']);
        dr.showCancel = $scope.shipment.statusCode != ORDER.FULFILLED
            && dr.st != DELIVERY_REQUEST_STATUSES.codes.DELIVERED && dr.st != DELIVERY_REQUEST_STATUSES.codes.CANCELLED;
    }

    $scope.onDeliveryRequestSync = function(deliveryRequestId, index) {
        if(checkNullEmpty(deliveryRequestId)) {
            $scope.showErrorMsg("Tracking Id is not present.");
            return;
        }
        $scope.loading = true;
        $scope.showLoading();
        ordService.syncDeliveryRequest(deliveryRequestId).then(function (data) {
            $scope.delivery_requests[index] = data.data;
            populateDisplayFields($scope.delivery_requests[index]);
        }).catch(function err(msg) {
            $scope.showErrorMsg(msg);
        }).finally(function () {
            $scope.loading = false;
            $scope.hideLoading();
        })
    };

    $scope.onDeliveryRequestCancel = function(deliveryRequestId, index) {
        if(checkNullEmpty(deliveryRequestId)) {
            $scope.showErrorMsg("Tracking Id is not present.");
            return;
        }
        $scope.loading = true;
        $scope.showLoading();
        ordService.cancelDeliveryRequest(deliveryRequestId).then(function (data) {
            $scope.delivery_requests[index] = data.data;
            populateDisplayFields($scope.delivery_requests[index]);
        }).catch(function err(msg) {
            $scope.showErrorMsg(msg);
        }).finally(function () {
            $scope.loading = false;
            $scope.hideLoading();
        })
    }

}]);





