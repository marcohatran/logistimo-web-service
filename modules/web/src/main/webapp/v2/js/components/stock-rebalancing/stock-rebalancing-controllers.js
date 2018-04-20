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


var stockRebalancingControllers = angular.module('stockRebalancingControllers', []);
stockRebalancingControllers.controller('RebalancingListingCtrl', ['$scope', 'domainCfgService', 'requestContext','$location','stockRebalancingService','$timeout','REBALANCING',
        function($scope, domainCfgService, requestContext, $location, stockRebalancingService, $timeout, REBALANCING) {
            $scope.wparams = [["o", "offset"], ["s", "size"], ["eid", "entity.id"],["event_type","reason"], ["mid", "material.mId"], ["etag", "etag"], ["mtag", "mtag"]];
            $scope.localFilters = ['entity', 'material', 'reason', 'etag', 'mtag'];

            $scope.init = function(firstTimeInit) {
                $scope.showRecommendations = [];
                if (checkNotNullEmpty(requestContext.getParam("mid"))) {
                    if(checkNullEmpty($scope.material) || $scope.material.mId != parseInt(requestContext.getParam("mid"))) {
                        $scope.material = {mId: parseInt(requestContext.getParam("mid")),mnm:""};
                        $scope.initLocalFilters.push("material")
                    }
                } else {
                    $scope.material = null;
                }
                if(checkNotNullEmpty(requestContext.getParam("eid"))) {
                    if(checkNullEmpty($scope.entity) || $scope.entity.id != parseInt(requestContext.getParam("eid"))) {
                        $scope.entity = {id: parseInt(requestContext.getParam("eid")), nm: ""};
                        $scope.initLocalFilters.push("entity")
                    }
                } else {
                    $scope.entity = null;
                }
                $scope.reason = requestContext.getParam("event_type") || "";
                $scope.mtag = requestContext.getParam("mtag") || "";
                $scope.etag = requestContext.getParam("etag") || "";
                $timeout(function () {
                    if(firstTimeInit) {
                        $scope.fetchRecommendedTransfers();
                    }
                }, 0);
            };
            $scope.init(true);
            ListingController.call(this, $scope, requestContext, $location);

            function constructStockLimits(data) {
                if(checkNotNullEmpty(data.min) && checkNotNullEmpty(data.max)) {
                    data.stockLimit = "(" + data.min + ", " + data.max + ")";
                }

            }

            function updateReasons() {
                if(checkNotNullEmpty($scope.filtered)) {
                    $scope.filtered.some(function (data) {
                       if(data.trigger_code == REBALANCING.reasonCodes.sdet) {
                           data.reason = $scope.resourceBundle['stock.out.likely'];
                       } else if(data.trigger_code == REBALANCING.reasonCodes.sebc) {
                           data.reason = $scope.resourceBundle['stock.expiring'];
                       } else if(data.trigger_code == REBALANCING.reasonCodes.sgtm) {
                           data.reason = $scope.resourceBundle['excess.stock'];
                       }
                        constructStockLimits(data);
                    });

                }
            }


            $scope.fetch = function() {
                var eid, mid;
                if (checkNotNullEmpty($scope.entity)) {
                    eid = $scope.entity.id;
                }
                if(checkNotNullEmpty($scope.material)){
                    mid = $scope.material.mId;
                }
                $scope.loading = true;
                $scope.showLoading();
                stockRebalancingService.getRecommendedTransfers(eid, mid,
                    $scope.reason, $scope.etag, $scope.mtag, $scope.size, $scope.offset).then(function(data) {
                        $scope.filtered = data.data.content;
                        updateReasons();
                        $scope.setPagedResults(data.data);
                    }).catch(function error(msg) {
                        $scope.showErrorMsg(msg);
                    }).finally(function() {
                        $scope.loading = false;
                        $scope.hideLoading();
                    });
            };

            $scope.fetchRecommendedTransfers = function() {
                $scope.fetch();
            };

            $scope.toggle = function (index) {
                $scope.showRecommendations[index] = !$scope.showRecommendations[index];
            };
        }]);

stockRebalancingControllers.controller('RebalancingRecommendationsCtrl', ['$scope', 'stockRebalancingService','$uibModal','REBALANCING',
        function($scope, stockRebalancingService, $uibModal, REBALANCING) {

            function fetchStockColor(currentStock, min, max) {
                if(currentStock == 0) {
                    return "#a94442";
                } else if(currentStock > max) {
                    return "#31708f";
                } else if(currentStock < min) {
                    return "#8a6d3b";
                }
            };

            function constructStockLimits(data) {
                if(checkNotNullEmpty(data.min) && checkNotNullEmpty(data.max)) {
                    return "(" + data.min + ", " + data.max + ")";
                }

            }

            function updateReason(triggerCode) {
                if(checkNotNullEmpty(triggerCode)) {
                    if ( triggerCode == REBALANCING.reasonCodes.sdet) {
                        return $scope.resourceBundle['stock.out.likely'];
                    } else if( triggerCode == REBALANCING.reasonCodes.sebc) {
                        return $scope.resourceBundle['stock.expiring'];
                    } else if( triggerCode == REBALANCING.reasonCodes.sgtm) {
                        return $scope.resourceBundle['excess.stock'];
                    }
                }
            }


            function setData() {
                if(checkNotNullEmpty($scope.recommendations)) {
                    $scope.recommendations.some(function(data) {
                        data.color = fetchStockColor(data.current_stock, data.min, data.max);
                        var calculated_stock = 0;
                        if($scope.type == "SOURCE") {
                            calculated_stock = data.current_stock + data.quantity;
                        } else if($scope.type == "DESTINATION") {
                            calculated_stock = data.current_stock - data.quantity;
                        }
                        data.stock_after_transfer =  calculated_stock < 0 ? 0 : calculated_stock;
                        var triggerCode = $scope.type == 'SOURCE' ? data.destination_short_code : data.source_short_code;
                        data.reason = updateReason(triggerCode);
                        data.stockLimit = constructStockLimits(data);
                        data.count = checkNullEmpty(data.open_transfers) ? 0 : data.open_transfers.length;
                    });
                }
            };

            $scope.createTransfer = function (data) {
                $scope.recommendation = data;
                $scope.modalInstance = $uibModal.open({
                    templateUrl: 'views/stock-rebalancing/create-transfer.html',
                    scope: $scope,
                    keyboard: false,
                    backdrop: 'static'
                });
            };

            $scope.openTransfers = function (data) {
                $scope.recommendation = data;
                $scope.modalInstance = $uibModal.open({
                    templateUrl: 'views/stock-rebalancing/transfers-list.html',
                    scope: $scope,
                    keyboard: false,
                    backdrop: 'static'
                });
            };

            $scope.addToTransfer = function() {
              if(checkNotNullEmpty($scope.recommendation.open_transfers)) {
                  $scope.recommendation.open_transfers.some(function (data) {
                     if(data.selected) {
                         $scope.showLoading();
                         stockRebalancingService.addToExistingTransfer($scope.recommendation.id, data.orderId).then(function (data) {
                             $scope.showSuccess($scope.resourceBundle['order.transfer'] + " " + data.data.orderId + " " + $scope.resourceBundle['update.success']);
                         }).catch(function error(msg) {
                             $scope.showErrorMsg(msg);
                         }).finally(function() {
                             $scope.enableScroll();
                             $scope.modalInstance.dismiss('cancel');
                             $scope.hideLoading();
                             $scope.fetchRecommendedTransfers();
                         });
                     }
                  });
              }
            };

            $scope.cancel = function () {
                $scope.recommendation = {};
                $scope.enableScroll();
                $scope.modalInstance.dismiss('cancel');
            };

            $scope.proceed = function (recommendationId) {
                if(checkNotNullEmpty(recommendationId)) {
                    $scope.showLoading();
                    stockRebalancingService.createTransfer(recommendationId).then(function(data) {
                        $scope.showSuccess($scope.resourceBundle['order.transfer'] + " " + data.data.orderId + " " + $scope.resourceBundle['create.success']);
                    }).catch(function error(msg) {
                        $scope.showErrorMsg(msg);
                    }).finally (function() {
                        $scope.enableScroll();
                        $scope.modalInstance.dismiss('cancel');
                        $scope.hideLoading();
                        $scope.fetchRecommendedTransfers();
                    })
                }
                $scope.enableScroll();
                $scope.modalInstance.dismiss('cancel');
            };

            $scope.fetch = function() {
                $scope.showLoading();
                stockRebalancingService.getRecommendations($scope.eventId).then(function (data) {
                    $scope.recommendations = data.data.content;
                    setData();
                }).catch(function error(msg) {
                    $scope.showErrorMsg(msg);
                }).finally(function() {
                    $scope.hideLoading();
                });
            };

            function init() {
                $scope.recommendations = {};
                $scope.fetch();
                $scope.viewTransfer = angular.copy($scope.dp.vp);
            }

            init();

        }]);