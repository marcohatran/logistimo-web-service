/**
 * Created by yuvaraj on 10/11/17.
 */
angular.module('logistimo.storyboard.temperatureStatusMapWidget', [])
    .config(function (widgetsRepositoryProvider) {
        widgetsRepositoryProvider.addWidget({
            id: "temperatureStatusMapWidget",
            name: "Asset map distribution",
            templateUrl: "plugins/storyboards/assets/temperature-status-map-widget/temperature-status-map-widget.html",
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
    .controller('temperatureStatusMapWidgetController',
    ['$scope', '$timeout', 'dashboardService', 'domainCfgService', 'INVENTORY', '$sce',
        function ($scope, $timeout, dashboardService, domainCfgService, INVENTORY, $sce) {
            var filter = angular.copy($scope.widget.conf);
            var tempPieColors, tempPieOrder, mapRange, mapColors, asset = '';
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
                })
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
                            $scope.mapType = linkText;
                        } else if ($scope.dashboardView.mLev == "state") {
                            linkText = $scope.dashboardView.mTyNm;
                            $scope.mapType = $scope.dashboardView.mTy;
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

            function setWidgetData() {
                $scope.temperatureStatusMapWidget = {
                    wId: $scope.widget.id,
                    cType: $scope.mapType,
                    copt: $scope.mapOpt,
                    cdata: $scope.mapData,
                    computedWidth: '100%',
                    computedHeight: parseInt($scope.widget.computedHeight, 10) - 10,
                    colorRange: $scope.mapRange,
                    markers: $scope.markers
                };
                $scope.wloading = false;
                $scope.showChart = true;
            };
        }]);

logistimoApp.requires.push('logistimo.storyboard.temperatureStatusMapWidget');
