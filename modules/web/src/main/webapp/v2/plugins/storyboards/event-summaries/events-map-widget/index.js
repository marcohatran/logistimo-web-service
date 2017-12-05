/**
 * Created by yuvaraj on 10/11/17.
 */
angular.module('logistimo.storyboard.eventMapWidget', [])
    .config(function (widgetsRepositoryProvider) {
        widgetsRepositoryProvider.addWidget({
            id: "eventMapWidget",
            name: "Event map",
            templateUrl: "plugins/storyboards/event-summaries/events-map-widget/event-map-widget.html",
            editTemplateUrl: "plugins/storyboards/event-summaries/event-summaries-edit-template.html",
            templateFilters: [
                {
                    nameKey: 'event.category',
                    type: 'category'
                },
                {
                    nameKey: 'event',
                    type: 'event'
                },
                {
                    nameKey: 'threshold',
                    type: 'threshold'
                }
            ],
            defaultHeight: 4,
            defaultWidth: 5
        });
    })
    .controller('eventMapWidgetController',
    ['$scope', '$timeout', 'dashboardService', 'domainCfgService', 'INVENTORY', '$sce','eventSummaryService',
        function ($scope, $timeout, dashboardService, domainCfgService, INVENTORY, $sce,eventSummaryService) {
            var filter = angular.copy($scope.widget.conf);
            var invPieOrder, mapRange, maxValue = 0;
            var fDate = (checkNotNullEmpty(filter.date) ? formatDate(filter.date) : undefined);
            $scope.showChart = false;
            $scope.wloading = true;
            $scope.showError = false;
            domainCfgService.getSystemDashboardConfig().then(function (data) {
                var domainConfig = angular.fromJson(data.data);
                mapColors = domainConfig.mc;
                $scope.mc = mapColors;
                mapRange = domainConfig.mr;
                $scope.mr = mapRange;
                invPieOrder = domainConfig.pie.io;
                $scope.mapEvent = 'es';
                $scope.eventId = $scope.widget.conf.event;
                $scope.categoryId = $scope.widget.conf.category;
                $scope.thresholdId = $scope.widget.conf.threshold;
                $scope.init();
            });

            $scope.init = function () {
                domainCfgService.getMapLocationMapping().then(function (data) {
                    if (checkNotNullEmpty(data.data)) {
                        $scope.locationMapping = angular.fromJson(data.data);
                        setFilters();
                        loadLocationMap();
                    }
                })
            };

            function setFilters() {
                if (checkNotNullEmpty(filter.period)) {
                    var p = filter.period;
                    if (p == '0' || p == '1' || p == '2' || p == '3' || p == '7' || p == '30') {
                        $scope.period = p;
                    }
                } else {
                    $scope.period = "0";
                }

                if (checkNotNullEmpty(filter.mapType)) {
                    var mapType = filter.mapType;
                    if (mapType == '0' || mapType == '1') {
                        $scope.mapType = mapType;
                        if ($scope.mapType == 0) {
                            $scope.mapEvent = invPieOrder[0];
                        } else {
                            $scope.mapEvent = invPieOrder[1];
                        }
                    }
                } else {
                    $scope.mapType = "0";
                }

                if (checkNotNullEmpty(filter.materialTag)) {
                    $scope.exFilter = constructModel(filter.materialTag);
                    $scope.exType = 'mTag';
                } else if (checkNotNullEmpty(filter.material)) {
                    $scope.exFilter = filter.material.id;
                    $scope.exType = 'mId';
                }
            }
            
            Array.prototype.insert = function (index) {
                this.splice.apply(this, [index, 0].concat(
                    Array.prototype.slice.call(arguments, 1)));
                return this;
            };

            function loadLocationMap() {
                var eventId = $scope.widget.conf.threshold, countDistribution =[];
                eventSummaryService.getEventSummariesDistribution($scope.currentDomain,$scope.curUser,eventId,true).then(function(data) {
                    $scope.dashboardView = {};
                    if (checkNotNullEmpty($scope.ddist)) {
                        $scope.mapType = $scope.ddist;
                        $scope.dashboardView.mTy = $scope.ddist.replace(' ', '');
                        $scope.dashboardView.mTyNm = $scope.dstate;
                        $scope.mapData = [];
                    } else if (checkNotNullEmpty($scope.dstate)) {
                        $scope.dashboardView.mTy = $scope.dstate.replace(' ', '');
                        $scope.mapType = $scope.dashboardView.mTy;
                        $scope.dashboardView.mTyNm = $scope.dstate;
                    } else if (checkNotNullEmpty($scope.dcntry)) {
                        $scope.dashboardView.mTy = $scope.dcntry.replace(' ', '');
                        $scope.mapType = $scope.locationMapping.data[$scope.dashboardView.mTy].name;
                        $scope.dashboardView.mTy = $scope.dcntry;
                    }
                    $scope.dashboardView.mLev = getLevel();
                    data.data.summaries = [];
                    if (checkNotNullEmpty(data.data.summaries)) {
                        $scope.dashboardView.distribution = data.data.summaries[0].distribution;
                        $scope.dashboardView.eventType = data.data.summaries[0].type;
                        countDistribution = getCounts($scope.dashboardView.distribution);
                        maxValue = getMax(countDistribution);
                        $scope.dashboardView.event = {};
                        $scope.dashboardView.event['es'] = {};
                        $scope.dashboardView.event['es'] = getEventLocationData($scope.dashboardView.distribution);
                        
                        if ($scope.dashboardView.eventType == 'performance') {
                            mapColors['es'] = mapColors['n'];
                            mapColors['es'].insert(0, '#cccccc');
                            mapRange['es'] = mapRange['n'];
                            mapRange['es'].insert(0, 0);
                            mapRange['es'][1] = 0.1;

                        } else {
                            mapColors['es'] = mapColors[200];
                            mapColors['es'].insert(0, '#cccccc');
                            mapRange['es'] = mapRange[200];
                            mapRange['es'].insert(0, 0);
                            mapRange['es'][1] = 0.1;
                        }
                        constructMapData($scope.mapEvent, true, $scope, INVENTORY, $sce, mapRange, mapColors,
                            invPieOrder, $timeout);
                    }
                    setWidgetData();
                }).catch(function error(msg) {
                    showError(msg, $scope);
                }).finally(function () {
                    $scope.loading = false;
                    $scope.wloading = false;
                });

            };

            function getLevel(){
                $scope.dashboardView.mPTy = $scope.dcntry;
                if(checkNullEmpty($scope.dstate)) {
                    return 'country';
                }else if(checkNullEmpty($scope.ddist)) {
                    return 'state';
                }else {
                    return 'district'
                }
            }

            function getEventLocationData(obj){
                var locData = obj;
                var eventLocationData = {};
                for (var i = 0; i < locData.length; i++) {
                    if($scope.dashboardView.mLev == 'country'){
                        eventLocationData[locData[i].location.state] = {};
                        eventLocationData[locData[i].location.state].value= locData[i].count;
                        eventLocationData[locData[i].location.state].per= getPercentile(locData[i].count);
                    } else if($scope.dashboardView.mLev == 'state'){
                        eventLocationData[locData[i].location.district] = {};
                        eventLocationData[locData[i].location.district].value= locData[i].count;
                        eventLocationData[locData[i].location.district].per= getPercentile(locData[i].count);
                    }
                }
                return eventLocationData;
            }

            function getCounts(obj){
                var locData = obj;
                var locCounts = [];
                for (var i = 0; i < locData.length; i++) {
                    locCounts.push(locData[i].count);
                }
                return locCounts;
            }

            function getMax(arr){
                return Math.max.apply(null, arr);
            }

            function getPercentile(n) {
                return (n/maxValue) * 100;
            }

            function setWidgetData() {
                $scope.eventMapWidget = {
                    wId: $scope.widget.id,
                    cType: $scope.mapType,
                    copt: $scope.mapOpt,
                    cdata: $scope.mapData,
                    computedWidth: '100%',
                    computedHeight: parseInt($scope.widget.computedHeight, 10) - 30,
                    colorRange: $scope.mapRange,
                    markers: $scope.markers
                };
                $scope.wloading = false;
                $scope.showChart = true;
            }
        }]);

logistimoApp.requires.push('logistimo.storyboard.eventMapWidget');
