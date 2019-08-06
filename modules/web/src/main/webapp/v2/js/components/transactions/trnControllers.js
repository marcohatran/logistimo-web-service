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

var trnControllers = angular.module('trnControllers', []);
trnControllers.controller('TransactionsCtrl', ['$scope', 'trnService', 'domainCfgService', 'entityService', 'requestContext', '$location', 'exportService','$timeout',
    function ($scope, trnService, domainCfgService, entityService, requestContext, $location, exportService,$timeout) {
        $scope.wparams = [
            ["tag", "tag"], ["etag", "etag"], ["type", "type"], ["from", "from", "", formatDate2Url],
            ["to", "to", "", formatDate2Url], ["o", "offset"], ["s", "size"],["batchnm","bid"],
            ["lceid","cust.id"],["lveid","vend.id"],["mid", "material.mId"],["atd","atd"],["rsn","reason"]];
        $scope.today = formatDate2Url(new Date());
        $scope.localFilters = ['entity', 'material', 'type', 'batchId', 'from', 'to', 'cust', 'vend', 'etag', 'tag','atd'];
        $scope.localFilterWatches= { material: watchMaterial};
        $scope.filterMethods = ['updateFilters','searchBatch'];
        $scope.atd = false;

        function watchMaterial(newValue, oldValue, callback) {
            if (newValue != oldValue && ((checkNotNullEmpty(newValue) && oldValue==undefined) || (checkNotNullEmpty(oldValue) && newValue==undefined) || (newValue.mId!=oldValue.mId))) {
                if (callback) {
                    callback('batchId', null);
                }
            }
        }

        $scope.initLocalFilters = [];
        $scope.init = function (firstTimeInit) {
        if (typeof  $scope.showEntityFilter === 'undefined') {
            $scope.showEntityFilter = true;
                if(firstTimeInit){
                    $scope.wparams.push(["eid", "entity.id"]);
                }
            }
            if (checkNotNullEmpty(requestContext.getParam("mid"))) {
                if(checkNullEmpty($scope.material) || $scope.material.mId != parseInt(requestContext.getParam("mid"))) {
                    $scope.material = {mId: parseInt(requestContext.getParam("mid")),mnm:""};
                    $scope.initLocalFilters.push("material")
                }
            } else {
                $scope.material = null;
            }
            $scope.type = requestContext.getParam("type") || "";
            $scope.tag = requestContext.getParam("tag") || "";
            $scope.etag = requestContext.getParam("etag") || "";
            if(firstTimeInit) {
                if(!requestContext.hasParam()) {
                    $timeout(function () {
                        var d = new Date();
                        d.setDate(d.getDate() - 30);
                        $scope.from = d;
                    }, 0);
                } else {
                    $scope.from = parseUrlDate(requestContext.getParam("from")) || "";
                    $timeout(function () {
                        $scope.fetch();
                    }, 0);
                }
            }
            $scope.to = parseUrlDate(requestContext.getParam("to")) || "";
            $scope.lEntityId = requestContext.getParam("lceid") || "";
            $scope.batchId = $scope.bid = requestContext.getParam("batchnm") || "";
            $scope.reason = requestContext.getParam("rsn") || "";
            if(checkNotNullEmpty($scope.from) || checkNotNullEmpty($scope.to)) {
                $scope.showMore = true;
            }
            if(checkNullEmpty($scope.lEntityId)) {
                $scope.lEntityId = requestContext.getParam("lveid") || "";
            }
            //$scope.to = parseUrlDate(requestContext.getParam("to")) || "";
            if (checkNotNullEmpty(requestContext.getParam("eid"))) {
                if(checkNullEmpty($scope.entity) || $scope.entity.id != parseInt(requestContext.getParam("eid"))) {
                    $scope.entity = {id: parseInt(requestContext.getParam("eid")), nm: ""};
                    $scope.initLocalFilters.push("entity")
                }
                if(checkNotNullEmpty(requestContext.getParam("lceid"))) {
                    if (checkNullEmpty($scope.cust) || $scope.cust.id != parseInt(requestContext.getParam("lceid"))) {
                        $scope.cust = {id: parseInt(requestContext.getParam("lceid")), nm: ""};
                        $scope.initLocalFilters.push("cust")
                    }
                }
                if (checkNotNullEmpty(requestContext.getParam("lveid"))) {
                    if (checkNullEmpty($scope.vend) || $scope.vend.id != parseInt(requestContext.getParam("lveid"))) {
                        $scope.vend = {id: parseInt(requestContext.getParam("lveid")), nm: ""};
                        $scope.initLocalFilters.push("vend")
                    }
                }
                entityService.getLinksCount(requestContext.getParam("eid")).then(function (data) {
                    var counts = data.data.replace(/"/g, "").split(",");
                    $scope.customerCount = counts[0];
                    $scope.vendorCount = counts[1];
                }).catch(function error(msg) {
                    $scope.showErrorMsg(msg);
                });
            } else if(checkNotNullEmpty(requestContext.getParam("mid"))){
                if(checkNotNullEmpty($scope.material) || $scope.material.mId != parseInt(requestContext.getParam("mid"))) {
                    $scope.material = {mId: parseInt(requestContext.getParam("mid")), mnm: "",b:$scope.material.b}  ;
                    $scope.initLocalFilters.push("material")
                }
            } else if($scope.showEntityFilter){
                if(firstTimeInit && checkNotNullEmpty($scope.defaultEntityId)){
                    $location.$$search.eid = $scope.defaultEntityId;
                    $location.$$compose();
                    $scope.entity = {id: $scope.defaultEntityId, nm: ""};
                    $scope.initLocalFilters.push("entity")
                }else{
                    $scope.entity = null;
                }
            }

        };
        $scope.init(true);
        ListingController.call(this, $scope, requestContext, $location);
        $scope.setData = function (data) {
            if (data != null) {
                $scope.transactions = data;
                $scope.setResults($scope.transactions);
            } else {
                $scope.transactions = {results:[]};
                $scope.setResults(null);
            }
            $scope.loading = false;
            $scope.hideLoading();
            fixTable();
        };
        function getCaption() {
            var caption = getFilterTitle($scope.entity, $scope.resourceBundle['kiosk'], 'nm');
            caption += getFilterTitle($scope.material, $scope.resourceBundle['material'], 'mnm');
            caption += getFilterTitle(getTransactionTypeLabel($scope.type), $scope.resourceBundle['type']);
            caption += getFilterTitle(formatDate2Url($scope.from), $scope.resourceBundle['from']);
            caption += getFilterTitle($scope.atd ? $scope.resourceBundle['yes'] : $scope.resourceBundle['no'], $scope.resourceBundle.filterby + " " + $scope.resourceBundle['date.actual.transaction.small']);
            caption += getFilterTitle(formatDate2Url($scope.to), $scope.resourceBundle['to']);
            caption += getFilterTitle($scope.cust, $scope.resourceBundle['customer'], 'nm');
            caption += getFilterTitle($scope.vend, $scope.resourceBundle['vendor'], 'nm');
            caption += getFilterTitle(checkNullEmpty($scope.entity) ? $scope.etag : "", $scope.resourceBundle['kiosk'] + " " + $scope.resourceBundle['tag.lower']);
            caption += getFilterTitle(checkNullEmpty($scope.material) ? $scope.tag : "", $scope.resourceBundle['material'] + " " + $scope.resourceBundle['tag.lower']);
            caption += getFilterTitle($scope.reason, $scope.resourceBundle['transaction'] + " " + $scope.resourceBundle['reason.lowercase']);
            caption += getFilterTitle($scope.bid, $scope.resourceBundle['batch']);

            return caption;
        }

        function getTransactionTypeLabel(type) {
            switch (type) {
                case 'i': return $scope.resourceBundle['issues'];
                case 'r': return $scope.resourceBundle['receipts'];
                case 'p': return $scope.resourceBundle['transactions.stockcount.upper'];
                case 'w': return $scope.resourceBundle['transactions.wastage.upper'];
                case 't': return $scope.resourceBundle['transfers'];
                case 'ro':
                    return $scope.resourceBundle['transactions.returns.outgoing.upper'];
                case 'ri':
                    return $scope.resourceBundle['transactions.returns.incoming.upper'];
            }
        }

        $scope.exportData = function (isInfo) {
            if (isInfo) {
                return {
                    filters: getCaption(),
                    type: $scope.resourceBundle['exports.transactions']
                };
            }
            var eid,mid,ktag,mtag=undefined;
            if (checkNotNullEmpty($scope.entity)) {
                eid = $scope.entity.id;
            }else{
                ktag=checkNotNullEmpty($scope.etag)?$scope.etag:undefined;
            }
            if(checkNotNullEmpty($scope.material)){
                mid = $scope.material.mId;
            }else{
                mtag=checkNotNullEmpty($scope.tag)?$scope.tag:undefined;
            }
            $scope.showLoading();
            exportService.exportData({
                from_date: formatDate2Url($scope.from) || undefined,
                end_date: formatDate2Url($scope.to) || undefined,
                entity_id: eid,
                material_id: mid,
                ktag: ktag,
                mtag: mtag,
                batch_id: $scope.bid,
                linked_kid: $scope.lEntityId,
                reason: $scope.reason,
                atd: $scope.atd,
                type: $scope.type,
                titles: {
                    filters: getCaption()
                },
                module: "transactions",
                templateId: "transactions"
            }).then(function (data) {
                $scope.showSuccess(data.data);
            }).catch(function error(msg) {
                $scope.showErrorMsg(msg);
            }).finally(function(){
                $scope.hideLoading();
            });
        };
        $scope.fetch = function () {
            $scope.transactions = {results:[]};
            $scope.exRow = [];
            $scope.loading = true;
            $scope.showLoading();
            if($scope.mxE && checkNullEmpty($scope.entity) ){
                $scope.setData(null);
                return;
            }
            var eid, mid;
            if (checkNotNullEmpty($scope.entity)) {
                eid = $scope.entity.id;
            }
            if(checkNotNullEmpty($scope.material)){
                mid = $scope.material.mId;
            }
            trnService.getTransactions(checkNullEmpty(eid)?$scope.etag:"", checkNullEmpty(mid)?$scope.tag:"", formatDate($scope.from), formatDate($scope.to),
                $scope.type, $scope.offset, $scope.size, $scope.bid, $scope.atd, eid, $scope.lEntityId,
                mid,$scope.reason).then(function (data) {
                $scope.setData(data.data);
            }).catch(function error(msg) {
                $scope.setData(null);
                $scope.showErrorMsg(msg);
            });
        };
        $scope.selectAll = function (newval) {
            for (var item in $scope.filtered) {
                var ty = $scope.filtered[item]['ty'];
                if (ty == 'i' || ty == 'r' || ty == 'w' || ty == 'rt') {
                    $scope.filtered[item]['selected'] = newval;
                }
            }
        };
        $scope.searchBatch = function () {
            if($scope.bid != $scope.batchId){
                $scope.bid = $scope.batchId;
            }
        };
        $scope.undoTransactions = function () {
            var trans = [];
            for (var item in $scope.filtered) {
                if ($scope.filtered[item].selected) {
                    if($scope.currentDomain == $scope.filtered[item].sdid){
                        trans.push($scope.filtered[item].id);
                    }
                }
            }
            if (checkNullEmpty(trans)) {
                $scope.showWarning($scope.resourceBundle['selecttransactionmsg']);
                return;
            }
            if (!confirm($scope.resourceBundle['confirmundotransactionmsg'])) {
                return;
            }
            $scope.showLoading();
            trnService.undoTransactions(trans).then(function (data) {
                $scope.fetch();
                if(data.data.indexOf("Partially Successful") > 0){
                    $scope.showWarning(data.data);
                } else {
                    $scope.showSuccess(data.data)
                }
            }).catch(function error(msg) {
                $scope.showErrorMsg(msg);
            }).finally(function(){
                $scope.hideLoading();
            });
        };

        domainCfgService.getActualTransDateCheck().then(function(data){
            $scope.hasAtd = data.data;
        });

        domainCfgService.getUniqueTransReasons().then(function(data){
            $scope.reasons = data.data;
        });

        $scope.metadata = [{'title': 'serialnum', 'field': 'sno'}, {'title': 'material', 'field': 'mnm'},
            {'title': 'kiosk', 'field': 'enm'},{'title': 'openingstock', 'field': 'os'}, {'title': 'operation', 'field': 'type'},
            {'title': 'custvend', 'field': 'lknm'}, {'title': 'quantity', 'field': 'q'},
            {'title': 'closingstock', 'field': 'cs'}, {'title': 'updatedon', 'field': 'ts'}, {'title': 'updatedby', 'field': 'unm'}];

        $scope.exRow = [];
        $scope.select = function (index, type) {
            var empty = '';
            if ($scope.exRow.length == 0) {
                for (var i = 0; i < $scope.transactions.results.length; i++) {
                    $scope.exRow.push(empty);
                }
            }
            $scope.exRow[index] = $scope.exRow[index] === type ? empty : type;
        };
        $scope.toggle = function (index) {
            $scope.select(index);
        };

        $scope.updateReason = function(reason){
            $scope.tempReason = reason;
        };

        $scope.updateFilters = function(){
            $scope.reason = angular.copy($scope.tempReason);
        }

        $scope.resetFilters = function() {
            if($scope.showEntityFilter) {
                $scope.entity = undefined;
            }
            $scope.material = undefined;
            $scope.type = undefined;
            $scope.from = undefined;
            $scope.to = undefined;
            $scope.etag = undefined;
            $scope.tag = undefined;
            $scope.cust = undefined;
            $scope.vend = undefined;
            $scope.bid = undefined;
            $scope.batchId=$scope.bid;
            $scope.atd=false;
            $scope.reason = undefined;
            $scope.tempReason = undefined;
        };
    }
]);
trnControllers.controller('TransMapCtrl', ['$scope','mapService','uiGmapGoogleMapApi',
    function ($scope,mapService,uiGmapGoogleMapApi) {
        $scope.lmap = angular.copy($scope.map);
        $scope.lmap.options = {scrollwheel: false};
        $scope.lmap.control = {};
        $scope.isToggleMap = checkNotNull($scope.toggleMap);
        if($scope.enMap){
            $scope.ltype = 'e'
        } else if ($scope.type === 'i' || $scope.type === 't' || $scope.type === 'c') {
            $scope.ltype = 'c'
        } else if ($scope.type === 'r') {
            $scope.ltype = 'v'
        }
        $scope.address = '';
        $scope.distance = '';
        $scope.markers = [];
        $scope.loading = true;
        var lCount = 0;
        var latLng;
        // Get the geocodes for the linked kiosk, only if the linked kiosk is geocoded.
        var lklatLng;
        $scope.setMarkers = function () {
            var geocoder = new google.maps.Geocoder();
            $scope.markers = [
                {latitude: $scope.lt, longitude: $scope.ln, id: '0', show: true, add: ''}]; // Add the geo codes of the transaction to markers.
            // Add the lklatLng marker only if it not undefined
            if ( lklatLng ) {
                var lkMarker = {
                    latitude: $scope.lklt,
                    longitude: $scope.lkln,
                    id: '1',
                    show: true,
                    add: '',
                    icon: 'https://www.google.com/intl/en_us/mapfiles/ms/micons/green-dot.png'
                };
                $scope.markers.push(lkMarker);
                mapService.convertLnLt($scope.markers, $scope.lmap);
            }
            function formatAddress(type, add, lt, ln) {
                var address = "<html><b>";
                if (type == "t") {
                    address += $scope.resourceBundle['transaction.at'] + ": " + add;
                } else if ($scope.enMap) {
                    address += $scope.resourceBundle['entity.at'] + ": " + add;
                } else {
                    address += ($scope.ltype == 'c' ? $scope.resourceBundle['customer.at'] + ": " : $scope.resourceBundle['vendor.at'] + ": ") + add;
                }
                address += "</b><br>";
                address += lt + "," + ln;
                address += "</html>";
                return address;
            }
            geocoder.geocode({'latLng': latLng}, function (results, status) {
                if (status == google.maps.GeocoderStatus.OK) {
                    if (results[1]) {
                        $scope.address = results[1].formatted_address;
                        $scope.markers[0] = {latitude:$scope.lt,longitude:$scope.ln,id:'0',show:true,add: formatAddress('t',$scope.address,$scope.lt,$scope.ln)};
                    } else if (results[0]) {
                        $scope.address = results[0].formatted_address;
                        $scope.markers[0] = {latitude:$scope.lt,longitude:$scope.ln,id:'0',show:true,add: formatAddress('t',$scope.address,$scope.lt,$scope.ln)};
                    }
                }else if(status == google.maps.GeocoderStatus.ZERO_RESULTS) {
                    $scope.markers[0].show=false;
                }
                mapService.convertLnLt($scope.markers, $scope.lmap);
                loadingCounter();
            });
            // Call geocoder only if lkLatLng is not undefined.
            if ( lklatLng ) {
                geocoder.geocode({'latLng': lklatLng}, function (results, status) {
                    if (status == google.maps.GeocoderStatus.OK) {
                        if (results[1]) {
                            $scope.cAddress = results[1].formatted_address;
                            $scope.markers[1] = {
                                latitude: $scope.lklt,
                                longitude: $scope.lkln,
                                id: '1',
                                show: true,
                                add: formatAddress('c', $scope.cAddress, $scope.lklt, $scope.lkln),
                                icon: 'https://www.google.com/intl/en_us/mapfiles/ms/micons/green-dot.png'
                            };
                        } else if (results[0]) {
                            $scope.cAddress = results[0].formatted_address;
                            $scope.markers[1] = {
                                latitude: $scope.lklt,
                                longitude: $scope.lkln,
                                id: '1',
                                show: true,
                                add: formatAddress('c', $scope.cAddress, $scope.lklt, $scope.lkln),
                                icon: 'https://www.google.com/intl/en_us/mapfiles/ms/micons/green-dot.png'
                            };
                        }
                    } else if (status == google.maps.GeocoderStatus.ZERO_RESULTS) {
                        $scope.markers[1].show = false;
                    }
                    mapService.convertLnLt($scope.markers, $scope.lmap);
                    loadingCounter();
                });
            } else {
                loadingCounter();
            }
        };
        uiGmapGoogleMapApi.then(function(){
            latLng = new google.maps.LatLng($scope.lt, $scope.ln);
            // Get the geocodes for the linked kiosk, only if the linked kiosk is geocoded.
            lklatLng = ( $scope.lklt == 0 && $scope.lkln == 0 ) ? undefined : new google.maps.LatLng($scope.lklt, $scope.lkln);
            $scope.setMarkers();
        });
        $scope.drawRoute = function () {
            // Draw the route only if the lklatLng is not undefined.
            if ( lklatLng ) {
                var directionsDisplay = new google.maps.DirectionsRenderer({suppressMarkers: true});
                directionsDisplay.setMap($scope.lmap.control.getGMap());
                var directionsService = new google.maps.DirectionsService();
                var request = {
                    origin: latLng,
                    destination: lklatLng,
                    travelMode: google.maps.TravelMode.DRIVING
                };
                directionsService.route(request, function (response, status) {
                    if (status == google.maps.DirectionsStatus.OK) {
                        directionsDisplay.setDirections(response);
                        $scope.distance = response.routes[0].legs[0].distance.text;
                    }
                    if (response.routes.length == 0) {
                        var bounds = new google.maps.LatLngBounds();
                        bounds.extend(new google.maps.LatLng($scope.lt, $scope.ln));
                        bounds.extend(new google.maps.LatLng($scope.lklt, $scope.lkln));
                        $scope.lmap.zoom = mapService.getBoundsZoomLevel(bounds, {height: 500, width: 900});
                    }
                    $scope.lmap.zoom -= 1;
                    loadingCounter();
                });
            } else{
                loadingCounter();
            }
        };

        function loadingCounter(){
            if(++lCount == 3){
                $scope.loading = false;
            }
        }
        $scope.$watch('lmap.control.getGMap', function (newVal) {
            if (checkNotNull(newVal)) {
                $scope.drawRoute();
            }
        });
    }
]);
trnControllers.controller('TransactionsFormCtrl', ['$rootScope','$scope', '$uibModal','trnService', 'invService', 'domainCfgService', 'entityService','$timeout',
    function ($rootScope,$scope, $uibModal, trnService, invService, domainCfgService, entityService,$timeout) {
        $scope.invalidPopup = 0;
        $scope.openFormCount = {};
        $scope.openFormCount.returns = 0;
        $scope.validate = function (material, index, source) {
            if(!material.isBatch) {
                material.isVisited = material.isVisited || checkNotNullEmpty(source);
                if(material.isVisited) {
                    if (checkNotNull(material.ind) && checkNullEmpty(material.quantity) || ($scope.transaction.type != 'p' && material.quantity <= 0)) {
                        showPopUP(material, messageFormat($scope.resourceBundle['transaction.invalid.quantity'], (material.mnm || material.name.mnm)), index);
                        return false;
                    }
                    if (checkNotNullEmpty(material.name.huName) && checkNotNullEmpty(material.name.huQty) && checkNotNullEmpty(material.quantity) && material.quantity % material.name.huQty != 0) {
                        showPopUP(material, messageFormat($scope.resourceBundle['transaction.handling.units.mismatch'], material.quantity, material.name.mnm, material.name.huName, material.name.huQty, material.name.mnm), index);
                        return false;
                    }
                }
                if (($scope.transaction.type == 'i' || $scope.transaction.type == 't' || $scope.transaction.type == 'w' ) && material.quantity > material.atpstock) {
                    showPopUP(material, (material.mnm || material.name.mnm) + ' ' + $scope.resourceBundle['quantity'] + ' (' + material.quantity + ') ' + $scope.resourceBundle['cannotexceedstock'] + ' (' + material.atpstock + ')', index);
                    return false;
                }

                if(material.isVisitedStatus) {
                    var status = material.ts ? $scope.tempmatstatus : $scope.matstatus;
                    if(checkNotNullEmpty(status) && checkNullEmpty(material.mst) && $scope.msm) {
                        showPopUP(material, $scope.resourceBundle['status.required'], index, material.ts ? "mt" : "m");
                        return false;
                    }
                }
            }

            if(!$scope.isTransactionTypeReturn() && checkNotNull(material.ind) && checkNullEmpty(material.reason) && $scope.reasonMandatory) {
                showPopUP(material, $scope.resourceBundle['reason.required'], index, 'r');
                return false;
            }

            return true;
        };

        function showPopUP(mat, msg, index, source) {
            if(checkNullEmpty(source)) {
                $timeout(function () {
                    mat.popupMsg = msg;
                    if (!mat.invalidPopup) {
                        mat.invalidPopup = true;
                        $scope.invalidPopup += 1;
                    }
                    $timeout(function () {
                        source = !mat.isBatch ? "" : "b";
                        $("[id='"+ source + mat.name.mId + index + "']").trigger('showpopup');
                    }, 0);
                }, 0);
            } else {
                $timeout(function () {
                    if (source == 'r') {
                        mat.rPopupMsg = msg;
                        if (!mat.rinvalidPopup) {
                            mat.rinvalidPopup = true;
                            $scope.invalidPopup += 1;
                        }
                    } else {
                        mat.aPopupMsg = msg;
                        if(!mat.ainvalidPopup) {
                            mat.ainvalidPopup = true;
                            $scope.invalidPopup += 1;
                        }
                    }

                    $timeout(function () {
                        $("[id='"+ source + mat.name.mId + index + "']").trigger('showpopup');
                    }, 0);
                }, 0);
            }
        }

        $scope.hidePopup = function(material,index, source){
            if (material.invalidPopup || material.ainvalidPopup || material.rinvalidPopup) {
                 $scope.invalidPopup = $scope.invalidPopup <= 0 ? 0 : $scope.invalidPopup - 1;
            }
            if(checkNullEmpty(source)) {
                material.invalidPopup = false;
                $timeout(function () {
                    $("[id='"+ material.name.mId + index + "']").trigger('hidepopup');
                }, 0);
            } else if(source == 'm' || source == 'mt') {
                hidePopup($scope, material, source + material.name.mId, index, $timeout, true);
            } else if(source == 'r') {
                hidePopup($scope, material, source + material.name.mId, index, $timeout, false, false, true);
            }
        };

        $scope.transactions_arr = [{value: 'z', displayName: '-- ' + $scope.resourceBundle['select.transaction.type'] + ' --', capabilityName: ''},
            {value: 'i', displayName: $scope.resourceBundle['issues'], capabilityName: 'es'},
            {value: 'r', displayName: $scope.resourceBundle['receipts'], capabilityName: 'er'},
            {value: 'p', displayName: $scope.resourceBundle['transactions.stockcount.upper'], capabilityName: 'sc'},
            {value: 'w', displayName: $scope.resourceBundle['transactions.wastage.upper'], capabilityName: 'wa'},
            {value: 't', displayName: $scope.resourceBundle['transfers'], capabilityName: 'ts'},
            {
                value: 'ri',
                displayName: $scope.resourceBundle['transactions.returns.incoming.upper'],
                capabilityName: 'eri'
            },
            {
                value: 'ro',
                displayName: $scope.resourceBundle['transactions.returns.outgoing.upper'],
                capabilityName: 'ero'
            }
        ];

        $scope.getAllCapabilities = function () {
            var capabName;
            var i=1;
            if(checkNotNullEmpty($scope.cnff) && checkNotNullEmpty($scope.cnff.tm)){
                while(i<$scope.transactions_arr.length){
                    capabName = $scope.transactions_arr[i].capabilityName;
                    if($scope.cnff.tm.indexOf(capabName) == -1){
                        i++;
                    }else{
                        $scope.transactions_arr.splice(i,1);
                    }
                }
            }
        };

        $scope.getAllCapabilities();

        $scope.offset = 0;
        $scope.size = 50;
        $scope.avMap = {}; //Available Inventory Mapped by material Id.
        $scope.diMap = {};
        $scope.availableInventory = [];
        $scope.tagMaterials = [];
        $scope.stopInvFetch = false;
        $scope.invLoading = false;
        $scope.showDestInv = false;
        $scope.showMinMax = true;
        $scope.today = formatDate2Url(new Date());
        $scope.minDate = new Date();
        $scope.minDate.setMonth($scope.minDate.getMonth() - 3);

        $scope.changeEntity = function(){
            var ent = $scope.transaction.ent;
            resetNoConfirm();
            $scope.transaction.ent = ent;
            $scope.transaction.eent = true;

            if (checkNotNullEmpty($scope.transaction.ent)){
                $scope.loadMaterials = true;
                $scope.getInventory();
                fetchTransConfig();
                $scope.transaction.type = $scope.transactions_arr[0].value;
            }
        };

        $scope.$watch("showDestInv",function(newVal,oldVal){
            if(newVal){
                $scope.getDestInventory();
            }
        });

        $scope.checkPermission = function(kiosk) {
            $scope.showLoading();
            trnService.getPermission($scope.curUser,kiosk.eid,$scope.transactionType).then(function(data) {
                $scope.showDestInvPermission = data.data;
            }).catch(function error(msg){
                $scope.showErrorMsg(msg);
            }).finally(function(){
                $scope.hideLoading();
            });
        };

        function fetchTransConfig() {
            if (checkNotNullEmpty($scope.transaction.ent)) {
                $scope.showLoading();
                trnService.getTransDomainConfig($scope.transaction.ent.id).then(function (data) {
                    $scope.tranDomainConfig = data.data;
                    $scope.trans = $scope.tranDomainConfig.dest;
                }).catch(function error(msg) {
                    $scope.showErrorMsg(msg);
                }).finally(function(){
                    $scope.hideLoading();
                });
            }
        }
        $scope.showLoading();
        trnService.getStatusMandatory().then(function(data) {
            $scope.statusData = data.data;
        }).catch(function error(msg) {
            $scope.showErrorMsg(msg);
        }).finally(function() {
            $scope.hideLoading();
        });
        $scope.getInventory = function () {
            if (checkNotNullEmpty($scope.transaction.ent)) {
                $scope.showLoading();
                var entId = $scope.transaction.ent.id;
                invService.getInventory(entId, null, $scope.offset, $scope.size).then(function (data) {
                    if ($scope.stopInvFetch || checkNullEmpty($scope.transaction.ent) || entId != $scope.transaction.ent.id) {
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
                    }
                    if (checkNotNullEmpty(inventory)) {
                        inventory.forEach(function (inv) {
                            inv.tgs.forEach(function (tag) {
                                if (checkNullEmpty($scope.tagMaterials[tag])) {
                                    $scope.tagMaterials[tag] = [];
                                }
                                $scope.tagMaterials[tag].push(inv.mId);
                            });

                            $scope.avMap[inv.mId] = inv;

                        });

                        for (var i in $scope.transaction.materials) {
                            var mat = $scope.transaction.materials[i];
                            if (checkNotNullEmpty(mat.name) && checkNotNullEmpty($scope.avMap[mat.name.mId])) {
                                $scope.avMap[mat.name.mId].hide = true;
                            }
                        }
                        $scope.loadDInventory(inventory);
                    }
                    $scope.invLoading = false;

                }).catch(function error(msg) {
                    $scope.showErrorMsg(msg);
                    $scope.loadMaterials = false;
                    $scope.invLoading = false;
                }).finally(function(){
                    $scope.hideLoading();
                });
            }
        };
        $scope.hasStock = function(data, type) {
            if(checkNotNullEmpty(data)) {
                return !(data.stk == 0 && (type == 'i' || type == 'w' || type == 't'));
            }
        };
        $scope.setTrans = function(){
            if(checkNullEmpty($scope.transaction.entityType)){
                $scope.trans = $scope.tranDomainConfig.dest;
            }else if($scope.transaction.entityType == 'c'){
                $scope.trans = $scope.tranDomainConfig.customers;
            }else if($scope.transaction.entityType == 'v'){
                $scope.trans = $scope.tranDomainConfig.vendors;
            }
        };
        $scope.entityType = "";
        $scope.update = function () {
            createReturnTransactions();
            if ($scope.transaction.type == 'ri' || $scope.transaction.type == 'ro') {
                if (!validateReturnTransactions()) {
                    return true;
                }
            }
            if ($scope.timestamp == undefined) {
                $scope.timestamp = new Date().getTime();
            }
            if($scope.atd == '2' && checkNullEmpty($scope.transaction.date)){
                $scope.showWarning($scope.resourceBundle['trn.atd.mandatory']);
                return true;
            }
            var index = 0;
            var invalidQuantity = $scope.transaction.modifiedmaterials.some(function (mat) {
                if(checkNotNullEmpty($scope.transaction.dent) && $scope.transaction.ent.nm === $scope.transaction.dent.enm){
                    $scope.showWarning($scope.resourceBundle['form.error']);
                    return true;
                }
                if(checkNotNullEmpty($scope.exRow[index])){
                    $scope.showWarning(messageFormat($scope.resourceBundle['transaction.batch.form.open'], $scope.transaction.modifiedmaterials[index].name.mnm));
                    return true;
                }
                if (!mat.isBatch) {
                    if(!$scope.validate(mat,index,'b')) {
                        return true;
                    }
                }else if(mat.isBatch){
                    var val = mat.bquantity.some(function(bmat){
                        if(checkNotNullEmpty(bmat.quantity)){
                            return true;
                        }
                    });
                    if(!val) {
                        $scope.showWarning(messageFormat($scope.resourceBundle['transaction.invalid.quantity'], mat.mnm || mat.name.mnm));
                        return true;
                    }
                    if (checkNullEmpty(mat.reason) && $scope.reasonMandatory && !$scope.isTransactionTypeReturn()) {
                        $scope.showWarning(messageFormat($scope.resourceBundle['transaction.reason.required'] , mat.mnm || mat.name.mnm));
                        return true;
                    }
                }
                index+=1;
            });
            var isStatusEmpty = false;
            index = 0;
            $scope.transaction.modifiedmaterials.forEach(function (mat) {
                if(checkNotNullEmpty(mat.name) && !mat.isBatch && mat.quantity != "0") {
                    var status = mat.ts ? $scope.tempmatstatus : $scope.matstatus;
                    if (checkNotNullEmpty(status) && checkNullEmpty(mat.mst) && $scope.msm) {
                        mat.isVisitedStatus = true;
                        !$scope.validate(mat, index, 's');
                        isStatusEmpty = true;
                    }
                }
                index+=1;
            });
            if (!invalidQuantity && !isStatusEmpty) {
                var fTransaction = constructFinalTransaction();
                $scope.showFullLoading();

                trnService.updateTransaction(fTransaction).then(function (data) {
                    resetNoConfirm(true);
                    if (data.data.indexOf("One or more errors") == 1) {
                        $scope.showWarning(data.data);

                    } else {
                        $scope.showSuccess(data.data);
                    }
                }).catch(function error(msg) {
                    if(msg.status == 504 || msg.status == 404) {
                        // Allow resubmit or cancel.
                        handleTimeout();
                    } else {
                        resetNoConfirm(true);
                        $scope.showErrorMsg(msg);
                    }
                }).finally(function(){
                    $scope.hideFullLoading();
                });
            }
        };

        function createReturnTransactions() {
            if ($scope.transaction.type == 'ri' || $scope.transaction.type == 'ro') {
                var transactionmaterials = [];
                angular.forEach($scope.transaction.materials, function (material) {
                    var returnitems = material.returnitems;
                    angular.forEach(returnitems, function (returnitem) {
                        var materialcopy = angular.copy(material);
                        materialcopy.quantity = returnitem.rq;
                        materialcopy.mst = returnitem.rmst;
                        materialcopy.rsn = returnitem.rrsn;
                        materialcopy.trkid = returnitem.id;
                        materialcopy.atd = returnitem.atd;
                        if (checkNotNullEmpty(returnitem.bid)) {
                            var batch = {};
                            batch.mId = returnitem.mid;
                            batch.bid = returnitem.bid;
                            batch.quantity = returnitem.rq;
                            batch.bexp = returnitem.bexp;
                            batch.bmfdt = returnitem.bmfdt;
                            batch.bmfnm = returnitem.bmfnm;
                            batch.mst = returnitem.rmst;
                            batch.trkid = returnitem.id;
                            batch.rsn = returnitem.rrsn;
                            materialcopy.bquantity.push(batch);
                        }
                        materialcopy.returnitems = undefined;
                        transactionmaterials.push(materialcopy);
                    });
                });
                $scope.transaction.modifiedmaterials = transactionmaterials;
            } else {
                $scope.transaction.modifiedmaterials = $scope.transaction.materials;
            }
        }

        $scope.isReturnTransactionsAtdValid = function(transaction) {
            if (checkNotNullEmpty($scope.transaction.date) && checkNotNullEmpty(transaction.atd)) {
                var transAtd = string2Date(transaction.atd, 'dd/mm/yyyy', '/');
                return ($scope.transaction.date >= transAtd);
            }
            return true;
        };

        function validateReturnTransactions() {
            if (checkNullEmpty($scope.transaction.modifiedmaterials)) {
                $scope.showWarning(messageFormat($scope.resourceBundle['transaction.invalid.return.quantity'], $scope.transaction.materials[0].name.mnm));
                return false;
            }
            // Check if all the selected materials selected have quantities
            var valid = $scope.transaction.materials.every(function (material) {
                if (checkNotNullEmpty(material.name)) {
                    if (!isMaterialInModifiedMaterials(material)) {
                        $scope.showWarning(messageFormat($scope.resourceBundle['transaction.invalid.return.quantity'], material.name.mnm));
                        return false;
                    } else if ($scope.transaction.type == 'ro' && isTotalReturnQuantityGreaterThanAtpStock(material)) {
                        if (material.isBatch) {
                            $scope.showWarning(messageFormat($scope.resourceBundle['transaction.total.return.quantity.greater.than.available.stock.batch'], $scope.totalReturnQuantity, material.bidbatchDetMap[$scope.errorBid].atpstk, $scope.errorBid, material.name.mnm));
                        } else {
                            $scope.showWarning(messageFormat($scope.resourceBundle['transaction.total.return.quantity.greater.than.available.stock'], $scope.totalReturnQuantity, material.atpstock, material.name.mnm));
                        }
                        return false;
                    } else {
                        return true;
                    }
                } else {
                    return true;
                }
            });
            if (!valid) {
                return false;
            }

            valid = !$scope.transaction.modifiedmaterials.some(function(material) {
                if (!$scope.isReturnTransactionsAtdValid(material)) {
                    var type = $scope.transaction.type == 'ri' ? $scope.resourceBundle['transactions.issue'] : $scope.resourceBundle['transactions.receipt'];
                    $scope.showWarning(messageFormat($scope.resourceBundle['transaction.return.atd.message'], type, material.name.mnm));
                    return true;
                }
            });
            return valid;
        }

        function isMaterialInModifiedMaterials(material) {
            return $scope.transaction.modifiedmaterials.some(function (mat) {
                return (mat.name.mId == material.name.mId);
            });
        }

        function isTotalReturnQuantityGreaterThanAtpStock(material) {
            var totalReturnQuantity = 0;
            $scope.errorBid = undefined;
            $scope.totalReturnQuantity = 0;

            var returnValue = $scope.transaction.modifiedmaterials.some(function (mat) {
                if (material.name.mId == mat.name.mId) {
                    var atpstock = 0;
                    if (material.isBatch) {
                        totalReturnQuantity = getTotalReturnQuantityForBatch(mat.bquantity[0].bid);
                        atpstock = material.bidbatchDetMap[mat.bquantity[0].bid].atpstk;
                    } else {
                        totalReturnQuantity = getTotalReturnQuantityForMaterial(material);
                        atpstock = material.atpstock;
                    }
                    if (totalReturnQuantity > atpstock) {
                        if (material.isBatch) {
                            $scope.errorBid = mat.bquantity[0].bid;
                        }
                        $scope.totalReturnQuantity = totalReturnQuantity;
                        return true;
                    }
                }
            });
            return returnValue;
        }

        function getTotalReturnQuantityForBatch(bid) {
            var totalReturnQuantityForBatch = 0;
            angular.forEach($scope.transaction.modifiedmaterials, function (mat) {
                if (checkNotNullEmpty(mat.bquantity) && mat.bquantity[0].bid == bid) {
                    totalReturnQuantityForBatch += parseInt(mat.bquantity[0].quantity);
                }
            });
            return totalReturnQuantityForBatch;
        }

        function getTotalReturnQuantityForMaterial(material) {
            var totalReturnQuantity = 0;
            angular.forEach($scope.transaction.modifiedmaterials, function (mat) {
                if (material.name.mId == mat.name.mId) {
                    totalReturnQuantity += parseInt(mat.quantity);
                }
            });
            return totalReturnQuantity;
        }

        function handleTimeout() {
            $scope.modalInstance = $uibModal.open({
                template: '<div class="modal-header ws">' +
                '<h3 class="modal-title">{{resourceBundle["connection.timedout"]}}</h3>' +
                '</div>' +
                '<div class="modal-body ws">' +
                '<p>{{resourceBundle["connection.timedout"]}}.</p>' +
                '</div>' +
                '<div class="modal-footer ws">' +
                '<button class="btn btn-primary" ng-click="update();cancel(false)">{{resourceBundle.resubmit}}</button>' +
                '<button class="btn btn-default" ng-click="cancel(true)">{{resourceBundle.cancel}}</button>' +
                '</div>',
                scope: $scope
            });
        }
        $scope.cancel = function (callReset) {
            if(callReset) {
                resetNoConfirm();
            }
            $scope.modalInstance.dismiss('cancel');
        };

        function constructFinalTransaction() {
            var ft = {};
            ft['kioskid'] = '' + $scope.transaction.ent.id;
            ft['transtype'] = $scope.transaction.type;
            if(checkNotNullEmpty($scope.transaction.dent)) {
                ft['lkioskid'] = '' + $scope.transaction.dent.eid;
            }
            //ft['reason'] = $scope.transaction.reason;
            if(checkNotNullEmpty($scope.transaction.date)) {
                ft['transactual'] = '' + formatDate($scope.transaction.date);
            }
            ft['materials'] = [];
            ft['bmaterials'] = [];
            $scope.transaction.modifiedmaterials.forEach(function (mat) {
                if (mat.isBatch) {
                    mat.bquantity.forEach(function (m) {
                        var reason = ($scope.transaction.type == 'ri' || $scope.transaction.type == 'ro' ? m.rsn : mat.reason);
                        var trackingid = checkNotNullEmpty(mat.trkid) ? mat.trkid : undefined;
                        var items = {};
                        if (checkNotNullEmpty(m.quantity)) {
                            items[m.mId + "\t" + m.bid] = {
                                q: '' + m.quantity, e: m.bexp, mr: m.bmfnm, md: m.bmfdt,
                                r: reason, mst: m.mst, trkid: trackingid
                            };
                            ft['bmaterials'].push(items);
                        }
                    });
                } else if (checkNotNull(mat.ind)) {
                    var items = {};
                    var trackingid = checkNotNullEmpty(mat.trkid) ? mat.trkid : undefined;
                    var reason = ($scope.transaction.type == 'ri' || $scope.transaction.type == 'ro' ? mat.rsn : mat.reason);
                    items[mat.name.mId] = {
                        q: '' + mat.quantity,
                        r: reason, mst: mat.mst, trkid: trackingid
                    };
                    ft['materials'].push(items);
                }
            });
            ft['signature'] = $scope.curUser + $scope.timestamp;
            return ft;
        }
        $scope.$watch('transaction.type', function (newVal) {
            if(newVal==$scope.transactions_arr[0].value){return;}
            $scope.entityType = "";
            $scope.validType = checkNotNullEmpty(newVal);
            $scope.reasons = [];
            $scope.defaultReason = undefined;
            if (checkNotNullEmpty($scope.transaction.type)) {
                if(checkNotNullEmpty($scope.tranDomainConfig.reasons)) {
                    $scope.tranDomainConfig.reasons.some(function(reason){
                        if (reason.type == $scope.transaction.type) {
                            $scope.reasons = reason.reasonConfigModel.rsns;
                            $scope.defaultReason = reason.reasonConfigModel.defRsn;
                            return;
                        }
                    });

                    if(checkNotNullEmpty($scope.reasons)) {
                        if($scope.reasons.length>0)
                            $scope.showReason=true;
                    }
                }
            }
            if ((newVal == 'i' || newVal == 'ri') && $scope.tranDomainConfig.noc > 0) {
                $scope.entityType = $scope.resourceBundle['customer'];
            } else if ((newVal == 'r' || newVal == 'ro') && $scope.tranDomainConfig.nov > 0) {
                $scope.entityType = $scope.resourceBundle['vendor'];
            } else if (newVal == 't') {
                $scope.entityType = "Transfer to";
            }
            if(newVal == 'i'){
                $scope.atd = $scope.tranDomainConfig.atdi;
                $scope.msm = $scope.statusData.ism;
            }else if(newVal =='r'){
                $scope.atd = $scope.tranDomainConfig.atdr;
                $scope.msm = $scope.statusData.rsm;
            }else if(newVal =='p'){
                $scope.atd = $scope.tranDomainConfig.atdp;
                $scope.msm = $scope.statusData.psm;
            }else if(newVal =='w'){
                $scope.atd = $scope.tranDomainConfig.atdw;
                $scope.msm = $scope.statusData.wsm;
            }else if(newVal =='t'){
                $scope.atd = $scope.tranDomainConfig.atdt;
                $scope.msm = $scope.statusData.tsm;
            } else if (newVal == 'ri') {
                $scope.atd = $scope.tranDomainConfig.atdri;
                $scope.msm = $scope.statusData.rism;
            } else if (newVal == 'ro') {
                $scope.atd = $scope.tranDomainConfig.atdro;
                $scope.msm = $scope.statusData.rosm;
            }
            $scope.reasonMandatory = $scope.tranDomainConfig.transactionTypesWithReasonMandatory.indexOf(newVal) != -1;
            $scope.showMaterials = true;
            $scope.showLoading();
            trnService.getMatStatus($scope.transaction.type, false).then(function (data) {
                $scope.matstatus = data.data;
            }).catch(function error(msg) {
                $scope.showErrorMsg(msg);
            }).finally(function() {
                $scope.hideLoading();
            });
            $scope.showLoading();
            trnService.getMatStatus($scope.transaction.type, true).then(function(data) {
                $scope.tempmatstatus = data.data;
            }).catch(function error(msg) {
                $scope.showErrorMsg(msg);
            }).finally(function() {
                $scope.hideLoading();
            });
        });
        $scope.$watch('mtag', function (name, oldVal) {
            $scope.availableInventory.forEach(function (inv) {
                inv.tHide = checkNotNullEmpty(name) && (checkNullEmpty($scope.tagMaterials[name]) || !($scope.tagMaterials[name].indexOf(inv.mId) >= 0));
            });
        });
        $scope.deleteRow = function (id) {
            var mIndex = $scope.transaction.materials[id].name.mId;
            if (checkNotNullEmpty(mIndex) && checkNotNullEmpty($scope.avMap[mIndex])) {
                $scope.avMap[mIndex].hide = false;
            }
            $scope.exRow.splice(id,1);
            $scope.hidePopup($scope.transaction.materials[id],id);
            $scope.transaction.materials.splice(id, 1);
            redrawPopup('hide');
        };
        $scope.addRows = function () {
            $scope.transaction.materials.push({"name": "", "stock": ""});
        };
        function resetMaterials() {
            $scope.transaction.materials = [];
            $scope.exRow = [];
            $scope.availableInventory.forEach(function (inv) {
                inv.hide = false;
            });
            $scope.dInv = false;
            $scope.addRows();
        }
        $scope.reset = function (type) {
            var proceed = false;
            if (type === "en") {
                if ($scope.transaction.materials.length == 1 || window.confirm($scope.resourceBundle['entity.editconfirm'])) {
                    proceed = true;
                }
            } else if (type == "ty") {
                if ($scope.transaction.materials.length == 1 || window.confirm($scope.resourceBundle['material.editconfirm'])) {
                    $scope.validType = false;
                    resetMaterials();
                    $scope.transaction.type = $scope.transactions_arr[0].value;
                    $scope.entityType = "";
                    $scope.mtag = "";
                    $scope.atd=undefined;
                    $scope.showReason = false;
                    $scope.invalidPopup = 0;
                }
                return;
            } else {
                if (window.confirm($scope.resourceBundle['clear.all'])) {
                    proceed = true;
                }
            }
            if (proceed) {
                resetNoConfirm();
            }
        };
        function resetNoConfirm(isAdd) {
            $scope.offset = 0;
            $scope.transaction = {};
            $scope.transaction.materials = [];
            $scope.availableInventory = [];
            $scope.avMap = {};
            $scope.tagMaterials = [];
            $scope.entityType = "";
            $scope.showMaterials = false;
            $scope.showDestInv = false;
            $scope.loadMaterials = false;
            $scope.validType = false;
            $scope.exRow = [];
            $scope.submitted = false;
            $scope.dInv = false;
            $scope.diMap = {};
            $scope.tranDomainConfig={};
            $scope.atd=undefined;
            $scope.mtag = "";
            $scope.showReason = false;
            $scope.timestamp = undefined;
            $scope.invalidPopup = 0;
            $scope.transaction.type = $scope.transactions_arr[0].value;
            $scope.openFormCount = {};
            $scope.openFormCount.returns = 0;
            $scope.addRows();
            if($scope.isEnt) {
                $scope.transaction.ent = $scope.entity;
                if (checkNotNullEmpty($scope.transaction.ent)){
                    $scope.transaction.eent = true;
                    $scope.loadMaterials = true;
                    $scope.getInventory();
                    fetchTransConfig();
                }
            } else if (checkNotNullEmpty($scope.defaultEntityId) && isAdd) {
                entityService.get($scope.defaultEntityId).then(function(data){
                    $scope.transaction.ent = data.data;
                    if (checkNotNullEmpty($scope.transaction.ent)){
                        $scope.transaction.eent = true;
                        $scope.loadMaterials = true;
                        $scope.getInventory();
                        fetchTransConfig();
                    }
                });
            }
        }
        resetNoConfirm(true);
        $scope.getAllCapabilities();

        $scope.getFilteredEntity = function (text) {
            $scope.loadingEntity = true;
            return entityService.getFilteredEntity(text.toLowerCase()).then(function (data) {
                $scope.loadingEntity = false;
                return data.data.results;
            }).finally(function(){
                $scope.loadingEntity = false;
            });
        };
        $scope.getFilteredInvntry = function (text) {
            $scope.fetchingInvntry = true;
            return invService.getInventoryStartsWith($scope.transaction.ent.id,null,text.toLowerCase(),0,10).then(function (data) {
                $scope.fetchingInvntry = false;
                var list = [];
                var map = {};
                for (var i in $scope.transaction.materials) {
                    var mat = $scope.transaction.materials[i];
                    if(checkNotNullEmpty(mat.name)){
                        map[mat.name.mId] = true;
                    }
                }
                if(checkNotNullEmpty(data.data.results)){
                    var trntype = false;
                    if($scope.transaction.type == 'i' || $scope.transaction.type == 'w' || $scope.transaction.type == 't') {
                        trntype = true;
                    }
                    for(var j in data.data.results) {
                        var mat = data.data.results[j];
                        if(!map[mat.mId] && !(trntype && mat.atpstk == 0)){
                            list.push(mat);
                        }
                    }
                }
                return list;
            }).finally(function(){
                $scope.fetchingInvntry = false;
            });
        };
        $scope.addMaterialToList = function (index) {
            if ($scope.validType) {
                var li = $scope.transaction.materials.length - 1;
                var newMat = {
                    "name": $scope.availableInventory[index]
                };
                if (li != $scope.transaction.materials.length) {
                    $scope.transaction.materials[li] = newMat;
                } else {
                    $scope.transaction.materials.push(newMat);
                }
                $scope.availableInventory[index].hide = true;
            }
        };
        $scope.getDestInventory = function () {
            if (checkNullEmpty($scope.dInventory)) {
                $scope.dInvLoading = true;
                $scope.showLoading();
                invService.getInventory($scope.transaction.dent.eid, null,0,999).then(function (data) {
                    $scope.dInventory = data.data.results;
                    $scope.dInv = !$scope.dInv;
                    $scope.diMap = [];
                    $scope.dInventory.forEach(function (di) {
                        $scope.diMap[di.mId] = di;
                    });
                    $scope.loadDInventory($scope.availableInventory);
                    $scope.loadDInventory($scope.transaction.materials,true);
                }).catch(function error(msg) {
                    $scope.showErrorMsg(msg);
                }).finally(function(){
                    $scope.hideLoading();
                });
            }
            if ($scope.dInvLoading == false) {
                $scope.dInv = !$scope.dInv;
            }
            $scope.dInvLoading = false;
        };
        $scope.loadDInventory = function (inventory,isTransMat) {
            inventory.forEach(function (inv) {
                var mid = isTransMat ? inv.name.mId : inv.mId;
                if (!isTransMat || checkNotNullEmpty(inv.name)) {
                    if (checkNotNullEmpty($scope.diMap[mid])) {
                        inv.dInv = $scope.diMap[mid];
                    } else {
                        inv.dInv = {};
                        inv.dInv.stk = "N/A";
                        inv.dInv.event = "-1";
                    }
                    inv.dStock = inv.dInv.stk;
                    inv.devent = inv.dInv.event;
                    if (inv.dInv.stk === "N/A") {
                        inv.dmm = "";
                    } else {
                        inv.dmm = "(" + inv.dInv.reord + ',' + inv.dInv.max + ")";
                    }
                }
            });
        };
        $scope.getAvailableInventoryID = function () {
            var mIds = [];
            $scope.availableInventory.forEach(function (inv) {
                mIds.push(inv.mId);
            });
            return mIds;
        };
        $scope.exRow = [];
        $scope.select = function (index, type) {
            var empty = "";
            if ($scope.exRow.length == 0) {
                for (var i = 0; i < $scope.transaction.materials.length; i++) {
                    $scope.exRow.push(empty);
                }
            }
            $scope.exRow[index] = $scope.exRow[index] === type ? empty : type;
            redrawPopup('hide');
        };

        function redrawPopup(type, source) {
            if (type == 'hide') {
                for (var i = 0; i < $scope.transaction.materials.length - 1; i++) {
                    $scope.hidePopup($scope.transaction.materials[i], i);
                }
                $timeout(function () {
                    redrawPopup('show',source);
                }, 40);
            } else {
                for (i = 0; i < $scope.transaction.materials.length - 1; i++) {
                    $scope.validate($scope.transaction.materials[i], i,source);
                }
            }
        }

        $scope.selectOnType = function (index) {
            var type;
            if($scope.transaction.type === 'p') {
                type = 'stock';
            } else if($scope.transaction.type === 'r') {
                type = 'add';
            }else if($scope.transaction.type === 'i' || $scope.transaction.type === 'w' || $scope.transaction.type === 't') {
                type = 'show';
            } else if ($scope.transaction.type === 'ri' || $scope.transaction.type === 'ro') {
                type = 'return';
            }
            $scope.select(index,type);
        };
        $scope.toggle = function (index) {
            $scope.select(index);
        };
        domainCfgService.getMaterialTagsCfg().then(function (data) {
            $scope.tags = data.data.tags;
        }).catch(function error(msg) {
            $scope.showErrorMsg(msg);
        });
        $scope.$on('$destroy', function cleanup() {
            $scope.stopInvFetch = true;
        });
        $scope.isTransactionTypeReturn = function () {
            return checkNotNullEmpty($scope.transaction.type) && ($scope.transaction.type == 'ri' || $scope.transaction.type == 'ro');
        };
        $scope.disableButtonsAndIncrementCount = function(material) {
            material.disableButtons = true;
            $scope.openFormCount.returns = $scope.openFormCount.returns + 1;
        };
        $scope.showCustomerInventory = function(transaction) {
            return $scope.tranDomainConfig.showCInv && (transaction.type == 'i' || transaction.type == 't') && checkNotNullEmpty(transaction.dent) && checkNotNullEmpty(transaction.dent.enm) && transaction.dent.enm.length > 0 && transaction.ent.nm !== transaction.dent.enm && $scope.showDestInvPermission > 0;
        };
    }
]);
trnControllers.controller('transactions.MaterialController', ['$scope', 'trnService', 'invService',
    function ($scope, trnService, invService) {
        $scope.updateBatch = function (batchDet) {
            $scope.material.bquantity = batchDet;
            countTotalBatchQuantities(batchDet);
        };
        $scope.updateNewBatch = function (batchNewDet) {
            $scope.material.bnquantity = batchNewDet;
        };
        function countTotalBatchQuantities(batchDet) {
            var count = 0;
            if(checkNotNullEmpty(batchDet)) {
                batchDet.forEach(function (m) {
                    if (checkNotNullEmpty(m.quantity)) {
                        count = count + parseInt(m.quantity);
                    }
                });
            }
            $scope.material.bquantities = count;
        }
        $scope.$watch('material.name', function (name, oldVal) {
            if (checkNotNull(name) && checkNotNull(name.atpstk)) {
                if(checkNotNullEmpty($scope.avMap[name.mId])){
                    name = $scope.avMap[name.mId];
                    name.hide = true;
                }
                $scope.material.stock = name.stk;
                $scope.material.atpstock = name.atpstk;
                $scope.material.tstk = name.tstk;
                $scope.material.ind = name.sno - 1;
                $scope.material.event = name.event;
                $scope.material.ts = name.ts;
                if (checkNotNull(name.dInv)) {
                    $scope.material.dStock = name.dInv.stk;
                    $scope.material.devent = name.dInv.event;
                    if (name.dInv.stk === "N/A") {
                        $scope.material.dmm = "";
                    } else {
                        $scope.material.dmm = "(" + name.dInv.reord + ',' + name.dInv.max + ")";
                    }
                }
                $scope.material.isBatch = name.be;
                if (name.be) {
                    $scope.material.bquantity = [];
                    $scope.material.bidbatchDetMap = {};
                    if ($scope.transaction.type == 'ri' || $scope.transaction.type == 'ro') {
                        fetchBatchDetails();
                    }
                }
                $scope.material.mm = "(" + name.reord.toFixed(0) + ',' + name.max.toFixed(0) + ")";
                $scope.material.reason = '';
                if(checkNotNullEmpty($scope.transaction.type)){
                    if(checkNotNullEmpty(name.tgs)) {
                        trnService.getReasons($scope.transaction.type, name.tgs).then(function (data) {
                            $scope.material.rsns = data.data.rsns;
                            $scope.material.reason = data.data.defRsn;
                            if (checkNotNullEmpty($scope.material.rsns) && $scope.material.rsns.length > 0) {
                                if ($scope.material.rsns.indexOf("") == -1) {
                                    $scope.material.rsns.splice(0, 0, "");
                                }
                                $scope.$parent.showReason = true;
                            } else if (checkNotNullEmpty($scope.reasons) && $scope.reasons.length > 0) {
                                if ($scope.reasons.indexOf("") == -1) {
                                    $scope.reasons.splice(0, 0, "");
                                }
                                $scope.material.rsns = $scope.reasons;
                                $scope.material.reason = $scope.defaultReason;
                                $scope.$parent.showReason = true;
                            }
                        }).catch(function error(msg) {
                            $scope.showErrorMsg(msg);
                        });
                    } else if (checkNotNullEmpty($scope.reasons) &&  $scope.reasons.length > 0) {
                        if ($scope.reasons.indexOf("") == -1) {
                            $scope.reasons.splice(0, 0, "");
                        }
                        $scope.material.reason = $scope.defaultReason;
                        $scope.material.rsns = $scope.reasons;
                    }
                }

                if(checkNotNullEmpty($scope.transaction.type)) {
                    if(name.ts) {
                        $scope.material.mst = checkNotNullEmpty($scope.tempmatstatus) ? $scope.tempmatstatus[0] : undefined;
                    } else {
                        $scope.material.mst = checkNotNullEmpty($scope.matstatus) ? $scope.matstatus[0] : undefined;
                    }
                }
                $scope.addRows();
            }
        });
        $scope.filterBatchMat = function(bmat){
            var fBatMat = [];
            if(checkNotNullEmpty(bmat)) {
                bmat.forEach(function (m) {
                    if (m.quantity !== undefined && m.quantity !== '') {
                        fBatMat.push(m);
                    }
                });
            }
            return fBatMat;
        }

        function fetchBatchDetails() {
            $scope.showLoading();
            invService.getBatchDetail($scope.material.name.mId, $scope.transaction.ent.id, true, undefined).then(function (data) {
                var batchDet = data.data;
                if (checkNotNullEmpty(batchDet)) {
                    batchDet.forEach(function (det) {
                        det.bexp = formatDate(parseUrlDate(det.bexp, true));
                        if (checkNotNullEmpty(det.bmfdt)) {
                            det.bmfdt = formatDate(parseUrlDate(det.bmfdt, true));
                        }
                        det.mId = $scope.material.name.mId;
                        $scope.material.bidbatchDetMap[det.bid] = det;
                    });
                }
                $scope.loading = false;
                $scope.hideLoading();
            }).catch(function error(msg) {
                $scope.loading = false;
                $scope.hideLoading();
                $scope.showErrorMsg(msg);
            });
        }
    }
]);
trnControllers.controller('BatchTransactionCtrl', ['$scope', 'invService','$timeout',
    function ($scope, invService,$timeout) {
        $scope.invalidPopup = 0;
        var displayStatus;
        $scope.togglePopup = function (batch, index, forceEvent, source) {
            $timeout(function () {
                var skipTrigger = false;
                if (forceEvent) {
                    var eventName = forceEvent
                } else {
                    var avStock = batch.atpstk + (batch.oastk || 0);
                    if(checkNotNullEmpty($scope.obts)) {
                        $scope.obts.some(function (obts) {
                            if (obts.id == batch.bid) {
                                avStock += obts.q * 1;
                                return true;
                            }
                        });
                    }
                    if (checkNotNullEmpty(batch.bid) && (batch.quantity === "0" || batch.quantity > avStock)) {
                        eventName = 'showpopup';
                        if (batch.quantity > avStock) {
                            showPopUP(batch, $scope.resourceBundle['quantity.exceed'], index);
                        } else {
                            showPopUP(batch, messageFormat($scope.resourceBundle['transaction.invalid.quantity'], batch.bid), index);
                        }
                        skipTrigger = true;
                    } else if (checkNotNullEmpty($scope.huName) && checkNotNullEmpty($scope.huQty) && checkNotNullEmpty(batch.bid) && checkNotNullEmpty(batch.quantity) && batch.quantity % $scope.huQty != 0) {
                        eventName = 'showpopup';
                        showPopUP(batch, messageFormat($scope.resourceBundle['transaction.handling.units.mismatch'], batch.quantity, batch.bid, $scope.huName, $scope.huQty, $scope.mnm), index);
                        skipTrigger = true;
                    } else {
                        if (batch.quantity > 0 && batch.isVisitedStatus && checkNotNullEmpty(displayStatus) && checkNullEmpty(batch.mst) && $scope.msm) {
                            showPopUP(batch, $scope.resourceBundle['status.required'], index, $scope.material.ts ? "btmt" : "btm");
                            skipTrigger = true;
                        } else {
                            batch.ainvalidPopup = false;
                        }
                        eventName = 'hidepopup';
                        if (!skipTrigger || (batch.invalidPopup == undefined || batch.invalidPopup == true)) {
                            $scope.invalidPopup = $scope.invalidPopup <= 0 ? 0 : $scope.invalidPopup - 1;
                        }
                        batch.invalidPopup = false;
                    }
                }
                if(!skipTrigger) {
                    source = source || "b";
                    $timeout(function () {
                        $("[id='"+ source + $scope.mid + index + "']").trigger(eventName);
                    }, 0);
                }
            }, 0);
        };

        function showPopUP(mat, msg, index, source) {
            $timeout(function () {
                if(checkNullEmpty(source)) {
                    mat.popupMsg = msg;
                    if (!mat.invalidPopup) {
                        mat.invalidPopup = true;
                        $scope.invalidPopup += 1;
                    }
                    source = "b";
                } else {
                    mat.aPopupMsg = msg;
                    if (!mat.ainvalidPopup) {
                        mat.ainvalidPopup = true;
                        $scope.invalidPopup += 1;
                    }
                }
                $timeout(function () {
                    $("[id='"+ source + $scope.mid + index + "']").trigger('showpopup');
                }, 0);
            }, 0);
        }

        $scope.loading = true;
        $scope.updateQuantity = function () {
            var remq = $scope.allocq || 0;
            if (checkNotNullEmpty($scope.exBatches)) {
                var batMap = {};
                $scope.batchDet.forEach(function (batch) {
                    batMap[batch.bid] = batch;
                });
                $scope.exBatches.forEach(function (exBatch) {
                    var batch = batMap[exBatch.id];
                    if (checkNotNull(batch)) {
                        batch.quantity = exBatch.q;
                        remq = remq - batch.quantity;
                    }
                });
            }
            if (checkNotNullEmpty($scope.allocq)) {
                $scope.batchDet.some(function (batch) {
                    if (checkNotNullEmpty(batch.quantity)) {
                        return true;
                    }
                    batch.quantity = Math.min(batch.atpstk, remq);
                    remq = remq - batch.quantity;
                    return remq <= 0;
                });
            }
        };
        $scope.updateMaterialStatus = function() {
            $scope.batchDet.forEach(function(det) {
                var setStatus = $scope.exBatches.some(function (data) {
                    if(data.id == det.bid) {
                        det.mst = data.mst || data.smst || status;
                        return true;
                    }
                });
                if(!setStatus) {
                    det.mst = status;
                }
            });
        };
        var status;
        if($scope.material.ts) {
            status = checkNotNullEmpty($scope.tempmatstatus) ? $scope.tempmatstatus[0] : undefined;
            displayStatus = $scope.tempmatstatus;
        } else {
            status = checkNotNullEmpty($scope.matstatus) ? $scope.matstatus[0] : undefined;
            displayStatus = $scope.matstatus;
        }
        if (checkNullEmpty($scope.bdata)) {
            $scope.batchDet = [];
            if(checkNotNullEmpty($scope.kid)) {
                $scope.showLoading();
                invService.getBatchDetail($scope.mid, $scope.kid, undefined,$scope.shipOrderId).then(function (data) {
                    $scope.batchDet = data.data;
                    if (checkNotNullEmpty($scope.batchDet)) {
                        $scope.updateQuantity();
                        $scope.batchDet.forEach(function (det) {
                            det.bexp = formatDate(parseUrlDate(det.bexp, true));
                            if (checkNotNullEmpty(det.bmfdt)) {
                                det.bmfdt = formatDate(parseUrlDate(det.bmfdt, true));
                            }
                            det.mId = $scope.mid;
                        });
                    }
                    if(checkNotNullEmpty($scope.exBatches)) {
                        $scope.updateMaterialStatus();
                    } else {
                        $scope.batchDet.forEach(function (data) {
                            data.mst = status;
                        });
                    }
                    $scope.loading = false;
                    $scope.hideLoading();
                }).catch(function error(msg) {
                    $scope.loading = false;
                    $scope.hideLoading();
                    $scope.showErrorMsg(msg);
                });
            }
        } else {
            $scope.batchDet = angular.copy($scope.bdata);
            $scope.updateQuantity();
            $scope.loading = false;
            $scope.hideLoading();
        }
        $scope.saveBatchTrans = function () {
            if($scope.type !== 'p') {
                var index = 0;
                var isValidQuantity = false;
                var invalidQuantity = $scope.batchDet.some(function (det) {
                    if(checkNotNullEmpty(det.quantity) && det.quantity > 0) {
                        if (det.q < det.quantity) {
                            showPopUP(det, $scope.resourceBundle['quantity.exceed'], index);
                            return true;
                        }
                        if ($scope.huQty && det.quantity % $scope.huQty != 0) {
                            det.isVisitedQuantity = true;
                            $scope.togglePopup(det, index);
                            return true;
                        }
                        isValidQuantity = true;
                    }
                    index+=1;
                });
                /*if(!isValidQuantity) {
                    $scope.showWarning($scope.resourceBundle['valid.quantity'] +" " + $scope.mnm);
                    return;
                }*/
                var isStatusEmpty = false;
                index = 0;
                $scope.batchDet.forEach(function (data) {
                    if(data.quantity > 0 && checkNotNullEmpty(displayStatus) && checkNullEmpty(data.mst) && $scope.msm) {
                        data.isVisitedStatus = true;
                        $scope.togglePopup(data,index);
                        isStatusEmpty = true;
                    }
                    index+=1;
                });
                if (invalidQuantity || isStatusEmpty) {
                    return;
                }
                var foundLast = false;
                var isInvalid = $scope.batchDet.some(function (det) {
                    if (foundLast && det.quantity > 0) {
                        return true;
                    } else if (det.atpstk > 0 && (checkNullEmpty(det.quantity) || det.quantity < det.atpstk)) {
                        foundLast = true;
                    }
                });
                var proceed = true;
                if (isInvalid && $scope.type != 'w') {
                    if (!confirm($scope.resourceBundle['batches.notallocated'])) {
                        proceed = false;
                    }
                }
                /*if (proceed && checkNotNull($scope.allocq)) {
                    var allocated = 0;
                    $scope.batchDet.forEach(function (bItem) {
                        if (checkNotNullEmpty(bItem.quantity)) {
                            allocated = allocated + parseInt(bItem.quantity);
                        }
                    });
                    if (allocated != $scope.allocq) {
                        if (!confirm($scope.resourceBundle['quantity.allocated'] + " '" + allocated + "'  " + $scope.resourceBundle['quantity.unmatch'] + " '" + $scope.item.q + "' " + "." + $scope.resourceBundle['proceed'])) {
                            proceed = false;
                        }
                    }
                }*/
            }
            if (proceed || $scope.type == 'p') {
                $scope.updateBatch(angular.copy($scope.batchDet));
                $scope.toggle($scope.$index);
            }
        };
    }
]);
trnControllers.controller('AddBatchTransactionCtrl', ['$scope','$timeout',
    function ($scope,$timeout) {

        $scope.invalidPopup = 0;
        var displayStatus;
        $scope.togglePopup = function (batch, index, forceEvent, source) {
            $timeout(function () {
                var skipTrigger = false;
                if (forceEvent) {
                    var eventName = forceEvent
                } else if (checkNotNullEmpty($scope.huName) && checkNotNullEmpty($scope.huQty) && checkNotNullEmpty(batch.bid) && checkNotNullEmpty(batch.quantity) && batch.quantity % $scope.huQty != 0) {
                    eventName = 'showpopup';
                    showPopUP(batch, messageFormat($scope.resourceBundle['transaction.handling.units.mismatch'], batch.quantity, batch.bid, $scope.huName, $scope.huQty, $scope.mnm), index);
                    skipTrigger = true;
                } else {
                    if (batch.quantity > 0 && batch.isVisitedStatus && checkNotNullEmpty(displayStatus) && checkNullEmpty(batch.mst) && $scope.msm) {
                        showPopUP(batch, $scope.resourceBundle['status.required'], index, $scope.material.ts ? "abtmt" : "abtm");
                        skipTrigger = true;
                    } else {
                        batch.ainvalidPopup = false;
                    }
                    eventName = 'hidepopup';
                    batch.invalidPopup = false;
                    if(!skipTrigger) {
                        $scope.invalidPopup = $scope.invalidPopup <= 0 ? 0 : $scope.invalidPopup - 1;
                    }
                }
                if(!skipTrigger) {
                    source = source || "b";
                    $timeout(function () {
                        $("[id='" + source + $scope.mid + index + "']").trigger(eventName);
                    }, 0);
                }
            }, 0);
        };

        function showPopUP(mat, msg, index, source) {
            $timeout(function () {
                if(checkNullEmpty(source)) {
                    mat.popupMsg = msg;
                    if (!mat.invalidPopup) {
                        mat.invalidPopup = true;
                        $scope.invalidPopup += 1;
                    }
                    source = "b";
                } else {
                    mat.aPopupMsg = msg;
                    if (!mat.ainvalidPopup) {
                        mat.ainvalidPopup = true;
                        $scope.invalidPopup += 1;
                    }
                }
                $timeout(function () {
                    $("[id='"+ source + $scope.mid + index + "']").trigger('showpopup');
                }, 0);
            }, 0);
        }

        $scope.exists = [];
        $scope.currentBatches = [];
        var status = "";
        if($scope.material.ts) {
            status = checkNotNullEmpty($scope.tempmatstatus) ? $scope.tempmatstatus[0] : undefined;
            displayStatus = $scope.tempmatstatus;
        } else {
            status = checkNotNullEmpty($scope.matstatus) ? $scope.matstatus[0] : undefined;
            displayStatus = $scope.matstatus;
        }
        $scope.addRow = function () {
            $scope.batchDet.push({mst: status});
        };
        if (checkNotNullEmpty($scope.bdata)) {
            $scope.batchDet = angular.copy($scope.bdata);
            var index=0;
            $scope.batchDet.forEach(function (det) {
                if(checkNotNullEmpty(det)){
                    if(!angular.isDate(det.bexp)){
                        det.bexp = string2Date(det.bexp, 'dd/mm/yyyy', '/');
                    }
                    if(!angular.isDate(det.bmfdt)) {
                        det.bmfdt = string2Date(det.bmfdt, 'dd/mm/yyyy', '/');
                    }
                }
                $scope.currentBatches[index++] = det.bid;
            });
        } else {
            $scope.batchDet = [];
            $scope.addRow();
        }


        $scope.checkCurrentBatches = function(data, index) {
            $scope.exists[index] = false;
            if($scope.currentBatches.length > 0) {
                for( var i=0; i<$scope.currentBatches.length; i++) {
                    if(i != index && $scope.currentBatches[i] == data) {
                        $scope.exists[index] = true;
                    }
                }
            }
            $scope.currentBatches[index] = data;
        };
        $scope.checkBatchExists = function(data, index) {
            if(checkNotNullEmpty(data)) {
                $scope.checkCurrentBatches(data, index);
            }
        };


        $scope.saveBatchTrans = function () {
            var isDup = $scope.exists.some(function (val){
               if(val){
                   $scope.showWarning($scope.resourceBundle['bid.dup']);
                   return true;
               }
            });
            if(isDup){
                return false;
            }
            var index = 0;
            var isMissing = $scope.batchDet.some(function (det) {
                if (checkNullEmpty(det.bid) || checkNullEmpty(det.bexp) ||
                    checkNullEmpty(det.bmfnm) || checkNullEmpty(det.quantity)) {
                    $scope.showWarning($scope.resourceBundle['fields.missing']);
                    return true;
                }
                if ($scope.transaction.type != 'p' && checkNotNullEmpty(det.quantity) && det.quantity <= 0) {
                    showPopUP(det,$scope.resourceBundle['invalid.quantity'],index);
                    return true;
                }
                if($scope.huQty && det.quantity % $scope.huQty != 0) {
                    $scope.togglePopup(det,index);
                    return true;
                }
                index+=1;
            });
            var isStatusEmpty = false;
            index = 0;
            $scope.batchDet.forEach(function(data) {
                if(data.quantity > 0 && checkNotNullEmpty(displayStatus) && checkNullEmpty(data.mst) && $scope.msm) {
                    data.isVisitedStatus = true;
                    isStatusEmpty = true;
                    $scope.togglePopup(data,index);
                }
                index+=1;
            });
            if (!isMissing && !isStatusEmpty) {
                $scope.batchDet.forEach(function (det) {
                    det.bexp = formatDate(det.bexp);
                    det.bmfdt = formatDate(det.bmfdt);
                    det.mId = $scope.mid;
                });
                $scope.$parent.material.bquantity = angular.copy($scope.batchDet);
                $scope.toggle($scope.$index);
            }
        };
        $scope.deleteRow = function (index) {
            $scope.exists.splice(index, 1);
            $scope.batchDet.splice(index, 1);
            $scope.currentBatches.splice(index, 1);
            for(var i=0;i<$scope.currentBatches.length;i++){
                $scope.checkCurrentBatches($scope.currentBatches[i],i);
            }
        };
        $scope.today = formatDate2Url(new Date());
    }]);

trnControllers.controller('StockBatchTransactionCtrl',['$scope','invService','$timeout',
    function($scope,invService,$timeout) {

        $scope.invalidPopup = 0;
        var status = "";
        var displayStatus;
        $scope.togglePopup = function (batch, index, forceEvent, source) {
            $timeout(function () {
                var skipTrigger = false;
                if (forceEvent) {
                    var eventName = forceEvent
                } else if (checkNotNullEmpty($scope.huName) && checkNotNullEmpty($scope.huQty) && checkNotNullEmpty(batch.bid) && checkNotNullEmpty(batch.quantity) && batch.quantity % $scope.huQty != 0) {
                    eventName = 'showpopup';
                    showPopUP(batch, messageFormat($scope.resourceBundle['transaction.handling.units.mismatch'], batch.quantity, batch.bid, $scope.huName, $scope.huQty, $scope.mnm), index);
                    skipTrigger = true;
                } else {
                    if (batch.quantity > 0 && batch.isVisitedStatus && checkNotNullEmpty(displayStatus) && checkNullEmpty(batch.mst) && $scope.msm) {
                        showPopUP(batch, $scope.resourceBundle['status.required'], index, $scope.material.ts ? "sbtmt" : "sbtm");
                        skipTrigger = true;
                    } else {
                        batch.ainvalidPopup = false;
                    }
                    eventName = 'hidepopup';
                    batch.invalidPopup = false;
                    if(!skipTrigger) {
                        $scope.invalidPopup = $scope.invalidPopup <= 0 ? 0 : $scope.invalidPopup - 1;
                    }
                }
                if(!skipTrigger) {
                    source = source || "b";
                    $timeout(function () {
                        $("[id='"+ source + $scope.mid + index + "']").trigger(eventName);
                    }, 0);
                }
            }, 0);
        };

        function showPopUP(mat, msg, index, source) {
            $timeout(function () {
                if(checkNullEmpty(source)) {
                    mat.popupMsg = msg;
                    if (!mat.invalidPopup) {
                        mat.invalidPopup = true;
                        $scope.invalidPopup += 1;
                    }
                    source = "b";
                } else {
                    mat.aPopupMsg = msg;
                    if(!mat.ainvalidPopup) {
                        mat.ainvalidPopup = true;
                        $scope.invalidPopup +=1;
                    }
                }
                $timeout(function () {
                    $("[id='"+ source + $scope.mid + index + "']").trigger('showpopup');
                }, 0);
            }, 0);
        }

        $scope.loading = true;
        $scope.exists = [];

        $scope.checkCurrentBatches = function(data, index) {
            $scope.exists[index] = false;
            if($scope.transaction.type == 'p' && $scope.batchDet.length > 0) {
                $scope.batchDet.some(function (det) {
                    if(data == det.bid) {
                        $scope.exists[index] = true;
                        return true;
                    }
                });
            }
            if($scope.batchNewDet.length > 0) {
                for( var i=0; i<$scope.batchNewDet.length; i++) {
                    if(i != index && $scope.batchNewDet[i].bid == data) {
                        $scope.exists[index] = true;
                    }
                }
            }
        };
        $scope.checkBatchExists = function(data, index) {
            if(checkNotNullEmpty(data)) {
                $scope.checkCurrentBatches(data, index);
                if($scope.exists[index]){
                    return false;
                }
            }
        };
        $scope.updateQuantity = function () {
            var remq = $scope.allocq || 0;
            if (checkNotNullEmpty($scope.exBatches)) {
                var batMap = {};
                $scope.batchDet.forEach(function (batch) {
                    batMap[batch.bid] = batch;
                });
                $scope.exBatches.forEach(function (exBatch) {
                    var batch = batMap[exBatch.id];
                    if (checkNotNull(batch)) {
                        batch.quantity = exBatch.atpstk;
                        remq = remq - batch.quantity;
                    }
                });
            }
            if (checkNotNull($scope.allocq)) {
                $scope.batchDet.some(function (batch) {
                    if (checkNotNullEmpty(batch.quantity)) {
                        return true;
                    }
                    batch.quantity = Math.min(batch.atpstk, remq);
                    remq = remq - batch.quantity;
                    return remq <= 0;
                });
            }
        };
        if (checkNullEmpty($scope.bdata)) {
            $scope.batchDet = [];
            if (checkNotNullEmpty($scope.kid)) {
                $scope.showLoading();
                invService.getBatchDetail($scope.mid, $scope.kid).then(function (data) {
                    $scope.batchDet = data.data;
                    if (checkNotNullEmpty($scope.batchDet)) {
                        $scope.updateQuantity();
                        $scope.batchDet.forEach(function (det) {
                            det.bexp = formatDate(parseUrlDate(det.bexp, true));
                            if (checkNotNullEmpty(det.bmfdt)) {
                                det.bmfdt = formatDate(parseUrlDate(det.bmfdt, true));
                            }
                            det.mId = $scope.mid;
                        });
                    }
                    if($scope.material.ts) {
                        status = checkNotNullEmpty($scope.tempmatstatus) ? $scope.tempmatstatus[0] : undefined;
                        displayStatus = $scope.tempmatstatus;
                    } else {
                        status = checkNotNullEmpty($scope.matstatus) ? $scope.matstatus[0] : undefined;
                        displayStatus = $scope.matstatus;
                    }
                    $scope.batchDet.forEach(function(data) {
                        data.mst = status;
                    });
                    $scope.loading = false;
                    $scope.hideLoading();
                }).catch(function error(msg) {
                    $scope.loading = false;
                    $scope.hideLoading();
                    $scope.showErrorMsg(msg);
                });
            }
        } else {
            $scope.batchDet = angular.copy($scope.bdata);
            $scope.updateQuantity();
            $scope.loading = false;
            $scope.hideLoading();
        }
        $scope.clearBatchTrans = function() {
            if (confirm($scope.resourceBundle['clearbatch.confirm'])) {
                $scope.batchDet.forEach(function (det) {
                    det.quantity = '0';
                });
            }
        };

        /*Add batch controller*/
        $scope.addRow = function () {
            $scope.batchNewDet.push({mst: status});
        };
        if (checkNotNullEmpty($scope.bndata)) {
            $scope.batchNewDet = angular.copy($scope.bndata);
            $scope.batchNewDet.forEach(function (det) {
                if (checkNotNullEmpty(det)) {
                    if (!angular.isDate(det.bexp)) {
                        det.bexp = string2Date(det.bexp, 'dd/mm/yyyy', '/');
                    }
                    if (!angular.isDate(det.bmfdt)) {
                        det.bmfdt = string2Date(det.bmfdt, 'dd/mm/yyyy', '/');
                    }
                }
            });
        } else {
            $scope.batchNewDet = [];
        }
        $scope.saveStockBatchTrans = function () {
            var isDup = $scope.exists.some(function (val){
                if(val) {
                    $scope.showWarning($scope.resourceBundle['bid.dup']);
                    return true;
                }
            });
            if(isDup) {
                return false;
            }
            var index = 0;
            var errorList = 0;
            $scope.batchNewDet.forEach(function (det) {
                if (checkNullEmpty(det.bid) || checkNullEmpty(det.bexp) ||
                    checkNullEmpty(det.bmfnm) || checkNullEmpty(det.quantity)) {
                    $scope.showWarning($scope.resourceBundle['fields.missing']);
                    errorList++ ;
                }
                if($scope.huQty && det.quantity % $scope.huQty != 0) {
                    $scope.togglePopup(det,index);
                    errorList++ ;
                }
                if(checkNotNullEmpty(displayStatus) && checkNullEmpty(det.mst) && $scope.msm) {
                    det.isVisitedStatus = true;
                    $scope.togglePopup(det,index);
                    errorList++ ;
                }
                index+=1;
            });
            index = 0;
            $scope.batchDet.forEach(function(data) {
                if(data.quantity > 0 && checkNotNullEmpty(displayStatus) && checkNullEmpty(data.mst) && $scope.msm) {
                    data.isVisitedStatus = true;
                    $scope.togglePopup(data, data.bid);
                    errorList ++;
                }
                index += 1;
            });
            if(errorList > 0) {
                return true;
            }
            if (errorList == 0) {
                $scope.batchNewDet.forEach(function (det) {
                    det.bexp = formatDate(det.bexp);
                    det.bmfdt = formatDate(det.bmfdt);
                    det.mId = $scope.mid;
                });
                $scope.updateNewBatch(angular.copy($scope.batchNewDet));
                $scope.$parent.material.bnquantity = angular.copy($scope.batchNewDet);
                $scope.updateBatch(angular.copy($scope.batchDet));
                $scope.$parent.material.oquantity = angular.copy($scope.batchDet);
                $scope.$parent.material.bquantity = $scope.$parent.material.oquantity.concat($scope.$parent.material.bnquantity);
                $scope.toggle($scope.$index);
            } else {
                return true;
            }

        };
        $scope.deleteRow = function (index) {
            $scope.batchNewDet.splice(index, 1);
            $scope.exists.splice(index, 1);
        };
        $scope.today = formatDate2Url(new Date());
    }
]);

trnControllers.controller('ReturnTransactionCtrl', ['$scope', '$timeout', 'requestContext', '$location', 'domainCfgService', 'trnService',
    function ($scope, $timeout, requestContext, $location, domainCfgService, trnService) {
        $scope.noWatch = true;
        ListingController.call(this, $scope, requestContext, $location);
        $scope.size = 10;
        $scope.fromDate = new Date();
        $scope.toDate = undefined;
        $scope.returnAgainstType = undefined;
        if ($scope.transaction.type == 'ri') {
            $scope.returnAgainstType = $scope.resourceBundle['transactions.issue.upper'];
        } else {
            $scope.returnAgainstType = $scope.resourceBundle['transactions.receipt.upper'];
        }
        $scope.returnitems = angular.copy($scope.material.returnitems);
        domainCfgService.getReturnConfig($scope.transaction.ent.id).then(function (data) {
            $scope.rc = data.data;
            $scope.fetch(true);
        }).catch(function error(msg) {
            $scope.showErrorMsg(msg, true);
        });
        $scope.fetch = function (firstTime) {
            if (firstTime) {
                if (checkNotNullEmpty($scope.rc)) {
                    if ($scope.type == 'ri' && checkNotNullEmpty($scope.rc.incDur)) {
                        $scope.fromDate.setDate($scope.fromDate.getDate() - $scope.rc.incDur);
                    } else if ($scope.type == 'ro' && checkNotNullEmpty($scope.rc.outDur)) {
                        $scope.fromDate.setDate($scope.fromDate.getDate() - $scope.rc.outDur);
                    } else {
                        $scope.fromDate = undefined;
                    }
                } else {
                    $scope.fromDate = undefined;
                }
                $scope.minDate = $scope.fromDate;
            }
            $scope.exRow = [];
            $scope.loading = true;
            $scope.showLoading();
            if (checkNullEmpty($scope.transaction.ent)) {
                $scope.setData(null);
                return;
            }
            var eid, mid;
            if (checkNotNullEmpty($scope.transaction.ent)) {
                eid = $scope.transaction.ent.id;
            }
            if (checkNotNullEmpty($scope.mid)) {
                mid = $scope.mid;
            }
            var type = undefined;
            var leid = undefined;
            if ($scope.type == 'ri') {
                type = 'i';
                if (checkNotNullEmpty($scope.transaction.dent) && checkNotNullEmpty($scope.transaction.dent.eid)) {
                    leid = $scope.transaction.dent.eid;
                }
            } else if ($scope.type == 'ro') {
                type = 'r';
                if (checkNotNullEmpty($scope.transaction.dent) && checkNotNullEmpty($scope.transaction.dent.eid)) {
                    leid = $scope.transaction.dent.eid;
                }
            }
            trnService.getTransactions(null, null, checkNotNullEmpty($scope.fromDate) ? formatDate($scope.fromDate) : $scope.fromDate, checkNotNullEmpty($scope.toDate) ? formatDate($scope.toDate) : $scope.toDate,
                type, $scope.offset, $scope.size, null, null, eid, leid,
                mid, null, true).then(function (data) {
                    $scope.setData(data.data);
                }).catch(function error(msg) {
                    $scope.setData(null);
                    $scope.showErrorMsg(msg);
                });
        };
        $scope.setData = function (data) {
            if (data != null) {
                var transactions = data;
                if ($scope.offset == 0) {
                    $scope.transactions = {results: []};
                    $scope.transactions.numFound = data.numFound;
                    $scope.transactions.results = transactions.results;
                } else {
                    $scope.transactions.results = $scope.transactions.results.concat(transactions.results);
                }
                $scope.setResults($scope.transactions);
                $scope.setFiltered($scope.transactions.results);
                setDefaultReason();
            } else {
                $scope.transactions = {results: []};
                $scope.setResults(null);
            }
            $scope.loading = false;
            $scope.hideLoading();
            fixTable();

        };
        function setDefaultReason() {
            angular.forEach($scope.transactions.results, function (transaction) {
                transaction.rrsn = $scope.defReason;
            });
        }

        $scope.applyFilter = function () {
            $scope.fromDate = $scope.from;
            $scope.toDate = $scope.to;
            $scope.offset = 0;
            $scope.fetch();
        };

        $scope.saveReturnTransactions = function (index) {
            if ($scope.atd == '2' && checkNullEmpty($scope.transaction.date)) {
                $scope.showWarning($scope.resourceBundle['trn.atd.mandatory']);
                return;
            }
            if (!isReturnTransactionsValid()) {
                return;
            }
            angular.forEach($scope.transactions.results, function (transaction) {
                if (transaction.rq > 0) {
                    if (!$scope.returnitems) {
                        $scope.returnitems = [];
                    }
                    $scope.returnitems.push(angular.copy(transaction));
                    transaction.rq = undefined;
                }
            });
            $scope.material.returnitems = $scope.returnitems;
            $scope.op = undefined;
            $scope.toggle(index);
        };

        $scope.toggle = function (index) {
            $scope.material.op = undefined;
            $scope.material.disableButtons = undefined;
            $scope.openFormCount.returns -= 1;
            $scope.select(index);
        };

        function isReturnTransactionsValid() {
            return ($scope.transactions.results.every(isTransactionValid));
        }

        $scope.saveEditedReturnTransactions = function (index) {
            if (!isReturnItemsValid()) {
                return;
            }
            if ($scope.atd == '2' && checkNullEmpty($scope.transaction.date)) {
                $scope.showWarning($scope.resourceBundle['trn.atd.mandatory']);
                return;
            }
            $scope.material.returnitems = $scope.returnitems;
            if ($scope.returnitems.length > 0 && checkNotNullEmpty($scope.returnitems[0].lkId)) {
                var dent = {eid: $scope.returnitems[0].lkId, enm: $scope.returnitems[0].lknm};
                $scope.transaction.dent = dent;
            }
            $scope.op = undefined;
            $scope.toggle(index);
        };

        function isReturnItemsValid() {
            return ($scope.returnitems.every(isReturnQuantityValid) && $scope.returnitems.every(isTransactionValid));
        }

        function isReturnQuantityValid(transaction) {
            if (checkNullEmpty(transaction.rq) || transaction.rq == 0) {
                $scope.showWarning(messageFormat($scope.resourceBundle['transaction.invalid.return.quantity'], transaction.mnm));
                return false;
            }
            return true;
        }

        function isTransactionValid(transaction) {
            if (checkNotNullEmpty(transaction.rq)) {
                if (transaction.rq > transaction.q) {
                    if ($scope.transaction.type == 'ri') {
                        $scope.showWarning(messageFormat($scope.resourceBundle['transaction.return.quantity.greater.than.issued.quantity'], transaction.rq, transaction.q, $scope.mnm));
                        return false;
                    } else if ($scope.transaction.type == 'ro') {
                        $scope.showWarning(messageFormat($scope.resourceBundle['transaction.return.quantity.greater.than.received.quantity'], transaction.rq, transaction.q, $scope.mnm));
                        return false;
                    }
                }
                if ($scope.transaction.type == 'ro') {
                    if (transaction.rq > 0 && checkNotNullEmpty(transaction.bid)) {
                        if (checkNullEmpty($scope.material.bidbatchDetMap[transaction.bid])) {
                            $scope.showWarning(messageFormat($scope.resourceBundle['transaction.return.batch.does.not.exist'], transaction.bid, $scope.mnm));
                            return false;
                        } else if (transaction.rq > $scope.material.bidbatchDetMap[transaction.bid].atpstk) {
                            $scope.showWarning(messageFormat($scope.resourceBundle['transaction.return.quantity.greater.than.available.stock.batch'], transaction.rq, $scope.material.bidbatchDetMap[transaction.bid].atpstk, transaction.bid, $scope.mnm));
                            return false;
                        }
                    } else if (transaction.rq > $scope.material.atpstock) {
                        $scope.showWarning(messageFormat($scope.resourceBundle['transaction.return.quantity.greater.than.available.stock'], transaction.rq, $scope.material.atpstock, $scope.mnm));
                        return false;
                    }
                }
                if (checkNotNullEmpty($scope.material.name.huName) && checkNotNullEmpty($scope.material.name.huQty) && checkNotNullEmpty(transaction.rq) && transaction.rq % $scope.material.name.huQty != 0) {
                    $scope.showWarning(messageFormat($scope.resourceBundle['transaction.handling.units.mismatch'], transaction.rq, $scope.material.name.mnm, $scope.material.name.huName, $scope.material.name.huQty, $scope.material.name.mnm));
                    return false;
                }
                var status = $scope.material.ts ? $scope.tempmatstatus : $scope.matstatus;
                if (checkNotNullEmpty(status) && checkNullEmpty(transaction.rmst) && $scope.msm) {
                    $scope.showWarning(messageFormat($scope.resourceBundle['transaction.status.required'], $scope.mnm));
                    return false;
                }
                if (checkNullEmpty(transaction.rrsn) && $scope.reasonMandatory) {
                    $scope.showWarning(messageFormat($scope.resourceBundle['transaction.reason.required'], $scope.mnm));
                    return false;
                }

                if (!$scope.isReturnTransactionsAtdValid(transaction)) {
                    var type = $scope.transaction.type == 'ri' ? $scope.resourceBundle['transactions.issue'] : $scope.resourceBundle['transactions.receipt'];
                    $scope.showWarning(messageFormat($scope.resourceBundle['transaction.return.atd.message'], type, $scope.mnm));
                    return false;
                }
            }
            return true;
        }

        $scope.$watch("offset", function () {
            $scope.fetch();
        });

        $scope.validate = function (transaction, index, idPrefix, type) {
            var redraw = false;
            var material = $scope.material;
            var isInvalid = false;
            if (type == undefined || type == 'q') {
                if (transaction.displayMeta != transaction.rq > 0) {
                    redraw = true;
                }
                transaction.displayMeta = transaction.rq > 0;
                if (checkNotNullEmpty(transaction.rq) && transaction.rq > transaction.q) {
                    if ($scope.transaction.type == 'ri') {
                        showPopup($scope, transaction, idPrefix + transaction.id,
                            messageFormat($scope.resourceBundle['transaction.return.quantity.greater.than.issued.quantity'], transaction.rq, transaction.q, $scope.mnm),
                            index, $timeout);
                        isInvalid = true;
                    } else if ($scope.transaction.type == 'ro') {
                        showPopup($scope, transaction, idPrefix + transaction.id, messageFormat($scope.resourceBundle['transaction.return.quantity.greater.than.received.quantity'], transaction.rq, transaction.q, $scope.mnm),
                            index, $timeout);
                        isInvalid = true;
                    }
                }

                if (!isInvalid && $scope.transaction.type == 'ro') {
                    if (transaction.rq > 0 && checkNotNullEmpty(transaction.bid)) {
                        if (checkNullEmpty($scope.material.bidbatchDetMap[transaction.bid])) {
                            showPopup($scope, transaction, idPrefix + transaction.id,
                                messageFormat($scope.resourceBundle['transaction.return.batch.does.not.exist'], transaction.bid, $scope.mnm),
                                index, $timeout);
                            isInvalid = true;
                        } else if (transaction.rq > $scope.material.bidbatchDetMap[transaction.bid].atpstk) {
                            showPopup($scope, transaction, idPrefix + transaction.id, messageFormat($scope.resourceBundle['transaction.return.quantity.greater.than.available.stock.batch'], transaction.rq, $scope.material.bidbatchDetMap[transaction.bid].atpstk, transaction.bid, $scope.mnm),
                                index, $timeout);
                            isInvalid = true;
                        }
                    } else if (transaction.rq > $scope.material.atpstock) {
                        showPopup($scope, transaction, idPrefix + transaction.id, messageFormat($scope.resourceBundle['transaction.return.quantity.greater.than.available.stock'], transaction.rq, material.atpstock, $scope.mnm),
                            index, $timeout);
                        isInvalid = true;
                    }
                }

                if (!isInvalid && checkNotNullEmpty(material.name.huName) && checkNotNullEmpty(material.name.huQty) && checkNotNullEmpty(transaction.rq) && transaction.rq % material.name.huQty != 0) {
                    showPopup($scope, transaction, idPrefix + transaction.id, messageFormat($scope.resourceBundle['transaction.handling.units.mismatch'], transaction.rq, material.name.mnm, material.name.huName, material.name.huQty, material.name.mnm),
                        index, $timeout);
                    isInvalid = true;
                }
            }

            if (type == undefined || type == 's') {
                var status = material.ts ? $scope.tempmatstatus : $scope.matstatus;
                if (checkNotNullEmpty(status) && checkNullEmpty(transaction.rmst) && $scope.msm) {
                    showPopup($scope, transaction, idPrefix + transaction.id, $scope.resourceBundle['status.required'],
                        index, $timeout, false, true);
                    isInvalid = true;
                }
            }

            if (type == undefined || type == 'r') {
                if ($scope.reasonMandatory && checkNullEmpty(transaction.rrsn)) {
                    showPopup($scope, transaction, idPrefix + transaction.id, $scope.resourceBundle['reason.required'],
                        index, $timeout, false, false, true);
                    isInvalid = true;
                }
            }
            if (redraw) {
                redrawAllPopup();
            }
            return !isInvalid;
        };


        function redrawAllPopup(type) {
            var index = -1;
            var items = $scope.op == 'edit' ? $scope.returnItems : $scope.transactions.results;
            angular.forEach(items, function (item) {
                index += 1;
                if (item.popupMsg) {
                    type == 'show' ?
                        $scope.validate(item, index, $scope.op == 'edit' ? 'er' : 'r', 'q') :
                        $scope.hidePopup(item, index, $scope.op == 'edit' ? 'er' : 'r', 'q');
                }
                if (item.sPopupMsg) {
                    type == 'show' ?
                        $scope.validate(item, index, ($scope.op == 'edit' ? 'ers' : 'rs') + (item.tm ? 't' : ''), 's') :
                        $scope.hidePopup(item, index, ($scope.op == 'edit' ? 'ers' : 'rs') + (item.tm ? 't' : ''), 's');
                }
                if (item.rPopupMsg) {
                    type == 'show' ?
                        $scope.validate(item, index, $scope.op == 'edit' ? 'err' : 'rr', 'r') :
                        $scope.hidePopup(item, index, $scope.op == 'edit' ? 'err' : 'rr', 'r');
                }

            });
            if (type != 'show') {
                $timeout(function () {
                    redrawAllPopup('show');
                }, 0);
            }
        }

        $scope.hidePopup = function (transaction, index, idPrefix, type) {
            hidePopup($scope, transaction, (idPrefix ? idPrefix : '') + transaction.id, index, $timeout, false, type == 's', type == 'r');
        };

        $scope.isReturnEnteredForTransaction = function (id) {
            if (checkNotNullEmpty($scope.returnitems)) {
                return $scope.returnitems.some(function (returnitem) {
                    return (returnitem.sno == id);
                });
            }
            return false;
        };

        $scope.removeFromReturnItems = function (id) {
            if (checkNotNullEmpty($scope.returnitems)) {
                for (var i = 0; i < $scope.returnitems.length; i++) {
                    if (id == $scope.returnitems[i].sno) {
                        $scope.returnitems.splice(i, 1);
                    }
                }
            }
        };
    }]);

