/**
 * Created by yuvaraj on 13/11/17.
 */
angular.module('logistimo.storyboard.topLocationsByTemperatureStatusWidget', [])
    .config(function (widgetsRepositoryProvider) {
        widgetsRepositoryProvider.addWidget({
            id: "topLocationsByTemperatureStatusWidget",
            name: "Asset top ten locations",
            templateUrl: "plugins/storyboards/assets/" +
            "top-locations-by-temperature-status-widget/top-locations-by-temperature-status-widget.html",
            editTemplateUrl: "plugins/storyboards/assets/asset-edit-template.html",
            templateFilters: [
                {
                    nameKey: 'asset.status',
                    type: 'assetStatus'
                },
                {
                    nameKey: 'asset.type',
                    type: 'assetType'
                },
                {
                    nameKey: 'period.since',
                    type: 'tPeriod'
                },
                {
                    nameKey: 'exclude.temp.state',
                    type: 'exTempState'
                },
                {
                    nameKey: 'include.entity.tag',
                    type: 'entityTag'
                },
                {
                    nameKey: 'exclude.entity.tag',
                    type: 'exEntityTag'
                }
            ],
            defaultHeight: 2,
            defaultWidth: 3
        });
    })
    .controller('topLocationsByTemperatureStatusWidgetController',
    ['$scope', '$timeout', 'dashboardService', 'domainCfgService', 'INVENTORY', '$sce',
        function ($scope, $timeout, dashboardService, domainCfgService, INVENTORY, $sce) {
            var filter = angular.copy($scope.widget.conf);
            var tempPieColors, tempPieOrder, mapRange, mapColors, asset = '', level;
            level = getLevel();
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
                tempPieOrder = domainConfig.pie.to;
                tempPieColors = domainConfig.pie.tc;
                $scope.mapEvent = tempPieOrder[0];
                $scope.init();
            });

            $scope.init = function () {
                domainCfgService.getMapLocationMapping().then(function (data) {
                    if (checkNotNullEmpty(data.data)) {
                        $scope.locationMapping = angular.fromJson(data.data);
                        setFilters();
                        loadLocationMap();
                    }
                });
            };

            function setFilters() {

                if (checkNotNullEmpty(filter.assetStatus) && checkNotNullEmpty($scope.widget.conf.assetStatus)) {
                    $scope.mapEvent = $scope.widget.conf.assetStatus;
                } else {
                    $scope.mapType = "tn";
                }

            }

            if (checkNotNullEmpty($scope.widget.conf.asset) && $scope.widget.conf.asset.length > 0) {
                var first = true;
                $scope.widget.conf.asset.forEach(function (data) {
                    if (!first) {
                        asset += "," + data.id;
                    } else {
                        asset += data.id;
                        first = false;
                    }

                });
            }
            ;

            function loadLocationMap() {
                dashboardService.get(undefined, undefined, $scope.exFilter, $scope.exType, $scope.period,
                    $scope.widget.conf.tPeriod, asset, constructModel(filter.entityTag), fDate,
                    constructModel(filter.exEntityTag), false).then(function (data) {
                        $scope.dashboardView = data.data;
                        var linkText;
                        if ($scope.dashboardView.mLev == "country") {
                            linkText = $scope.locationMapping.data[$scope.dashboardView.mTy].name;
                            $scope.mapType = "maps/" + linkText;
                        } else if ($scope.dashboardView.mLev == "state") {
                            linkText = $scope.dashboardView.mTyNm;
                            $scope.mapType = "maps/" + $scope.dashboardView.mTy;
                        } else {
                            linkText =
                                $scope.dashboardView.mTyNm.substr($scope.dashboardView.mTyNm.lastIndexOf("_") + 1);
                            $scope.showMap = false;
                            $scope.showSwitch = false;
                            $scope.mapData = [];
                        }
                        constructMapData($scope.mapEvent, true, $scope, INVENTORY, $sce, mapRange, mapColors,
                            tempPieOrder, $timeout);
                        setWidgetData();
                    }).catch(function error(msg) {
                        showError(msg, $scope);
                    }).finally(function () {
                        $scope.loading = false;
                        $scope.wloading = false;
                    });
            };

            function getLevel(){
                if(checkNullEmpty($scope.dstate)) {
                    return 'country';
                }else if(checkNullEmpty($scope.ddist)) {
                    return 'state';
                }else {
                    return 'district'
                }
            }
            if(checkNotNullEmpty(level)){
                if(level == 'country'){
                    level = 'states'
                }else if (level == 'state'){
                    level = 'districts'
                }else if (level == 'district'){
                    level = 'stores'
                }
            }

            $scope.barOpt = {
                "showAxisLines": "0",
                "valueFontColor": "#000000",
                "theme": "fint",
                "exportEnabled": 0,
                "yAxisMaxValue": 100,
                "captionFontSize": "12",
                "captionAlignment": "center",
                "caption": "Top 10 " + level,
                "chartLeftMargin": "50"
            };

            function setWidgetData() {
                $scope.topLocationsByTemperatureStatusWidget = {
                    wId: $scope.widget.id,
                    cType: 'bar2d',
                    copt: $scope.barOpt,
                    cdata: $scope.barData,
                    computedWidth: '90%',
                    computedHeight: parseInt($scope.widget.computedHeight, 10) - 10,
                    colorRange: $scope.mapRange,
                    markers: $scope.markers
                };
                $scope.wloading = false;
                $scope.showChart = true;
            }
        }]);

logistimoApp.requires.push('logistimo.storyboard.topLocationsByTemperatureStatusWidget');
