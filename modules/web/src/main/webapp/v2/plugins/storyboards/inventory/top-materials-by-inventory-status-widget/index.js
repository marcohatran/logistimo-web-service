/**
 * Created by yuvaraj on 10/11/17.
 */
angular.module('logistimo.storyboard.topMaterialsByInventoryStatusWidget', [])
    .config(function (widgetsRepositoryProvider) {
        widgetsRepositoryProvider.addWidget({
            id: "topMaterialsByInventoryStatusWidget",
            name: "inventory.top.ten.materials",
            templateUrl: "plugins/storyboards/inventory/" +
            "top-materials-by-inventory-status-widget/top-materials-by-inventory-status-widget.html",
            editTemplateUrl: "plugins/storyboards/inventory/edit-template.html",
            templateFilters: [
                {
                    nameKey: 'inventory.status',
                    type: 'topTenMatType',
                },
                {
                    nameKey: 'material.upper',
                    type: 'material'
                },
                {
                    nameKey: 'filter.material.tag',
                    type: 'materialTag'
                },
                {
                    nameKey: 'include.entity.tag',
                    type: 'entityTag'
                },
                {
                    nameKey: 'exclude.entity.tag',
                    type: 'exEntityTag'
                },
                {
                    nameKey: 'date',
                    type: 'date'
                },
                {
                    nameKey: 'period.since',
                    type: 'period'
                }
            ],
            defaultHeight: 4,
            defaultWidth: 3
        });
    })
    .controller('topMaterialsByInventoryStatusWidgetController',
    ['$scope', '$timeout', 'dashboardService', 'domainCfgService', 'INVENTORY', '$sce',
        function ($scope, $timeout, dashboardService, domainCfgService, INVENTORY, $sce) {
            var filter = angular.copy($scope.widget.conf);
            var invPieColors, invPieOrder, mapColors, mapRange,barColor;
            var fDate = (checkNotNullEmpty(filter.date) ? formatDate(filter.date) : undefined);
            $scope.showChart = false;
            $scope.wloading = true;
            $scope.showError = false;
            domainCfgService.getSystemDashboardConfig().then(function (data) {
                var domainConfig = angular.fromJson(data.data);
                mapColors = domainConfig.mc;
                mapRange = domainConfig.mr;
                mapRange['avlbl'] = mapRange['n'];
                mapColors['avlbl'] = mapColors['n'];
                $scope.mc = mapColors;
                $scope.mr = mapRange;
                invPieColors = domainConfig.pie.ic;
                invPieOrder = domainConfig.pie.io;
                $scope.mapEvent = invPieOrder[0];
                $scope.init()
            });

            $scope.init = function () {
                domainCfgService.getMapLocationMapping().then(function (data) {
                    if (checkNotNullEmpty(data.data)) {
                        $scope.locationMapping = angular.fromJson(data.data);
                        setFilters();
                        getData();
                    }
                });
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

                if (checkNotNullEmpty(filter.topTenMatType)) {
                    var topTenMatType = filter.topTenMatType;
                    $scope.topTenMatType = topTenMatType;
                    if ($scope.topTenMatType == 0) {
                        $scope.mapEvent = invPieOrder[0];
                        barColor = invPieColors[3];
                    } else if ($scope.topTenMatType == 1) {
                        $scope.mapEvent = invPieOrder[1];
                        barColor = invPieColors[0];
                    } else if ($scope.topTenMatType == 2) {
                        $scope.mapEvent = invPieOrder[2];
                        barColor = invPieColors[1];
                    } else if($scope.topTenMatType == 3){
                        $scope.mapEvent = invPieOrder[3];
                        barColor = invPieColors[2];
                    }else{
                        $scope.mapEvent = 'avlbl';
                        barColor = invPieColors[3];
                    }
                } else {
                    $scope.topTenMatType = "0";
                }

                if (checkNotNullEmpty(filter.materialTag)) {
                    $scope.exFilter = constructModel(filter.materialTag);
                    $scope.exType = 'mTag';
                } else if (checkNotNullEmpty(filter.material)) {
                    $scope.exFilter = filter.material.id;
                    $scope.exType = 'mId';
                }
            };


            function getData() {
                dashboardService.get(undefined, undefined, $scope.exFilter, $scope.exType, $scope.period, undefined,
                    undefined, constructModel(filter.entityTag), fDate, constructModel(filter.exEntityTag),
                    false).then(function (data) {
                        $scope.dashboardView = data.data;
                        if($scope.mapEvent == 'avlbl') {
                            $scope.dashboardView.inv['avlbl'] = getAvailable($scope.dashboardView.inv);
                        }
                        var linkText;
                        if ($scope.dashboardView.mLev == "country") {
                            linkText = $scope.locationMapping.data[$scope.dashboardView.mTy].name;
                            $scope.mapType = "maps/" + linkText;
                        } else if ($scope.dashboardView.mLev == "state") {
                            $scope.mapType = "maps/" + $scope.dashboardView.mTy;
                        } else {
                            $scope.showMap = false;
                            $scope.showSwitch = false;
                            $scope.mapData = [];
                        }
                        constructMapData($scope.mapEvent, true, $scope, INVENTORY, $sce, mapRange, mapColors,
                            invPieOrder, $timeout,false, barColor);
                        setWidgetData();
                    }).catch(function error(msg) {
                        showError(msg, $scope);
                    }).finally(function () {
                        $scope.loading = false;
                        $scope.wloading = false;
                    });
            }

            $scope.barOpt = {
                "showAxisLines": "0",
                "valueFontColor": "#000000",
                "theme": "fint",
                "exportEnabled": 0,
                "yAxisMaxValue": 100,
                "captionFontSize": "12",
                "captionAlignment": "center",
                "chartLeftMargin": "50",
                "captionFontSize": "14",
                "captionFontBold":1,
                "captionFont":'Helvetica Neue", Arial',
                "bgColor": "#272727",
                "bgAlpha" : "100",
                "canvasBgAlpha": "100",
                "canvasBgColor" : "#272727",
                "baseFont" : "Lato"
    
            };

            $scope.chartTitle = "Top 10 materials";

            function setWidgetData() {
                $scope.topMaterialsByInventoryStatus = {
                    wId: $scope.widget.id,
                    cType: 'bar2d',
                    copt: $scope.barOpt,
                    cdata: $scope.matBarData,
                    computedWidth: '90%',
                    computedHeight: parseInt($scope.widget.computedHeight, 10) - 30,
                    colorRange: $scope.mapRange,
                    markers: $scope.markers
                };
                $scope.wloading = false;
                $scope.showChart = true;
            }
        }]);

logistimoApp.requires.push('logistimo.storyboard.topMaterialsByInventoryStatusWidget');
