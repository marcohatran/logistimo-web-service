angular.module('logistimo.storyboard.inventoryStatusDonutWidget', [])
    .config(function (widgetsRepositoryProvider) {
        widgetsRepositoryProvider.addWidget({
            id: "inventoryStatusDonutWidget",
            name: "inventory.status.donut",
            templateUrl: "plugins/storyboards/inventory/inventory-status-donut-widget/inventory-status-donut-widget.html",
            editTemplateUrl: "plugins/storyboards/inventory/edit-template.html",
            templateFilters: [
                {
                    nameKey: 'inventory.status',
                    type: 'widType'
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
            defaultHeight: 3,
            defaultWidth: 4
        });
    })
    .controller('inventoryStatusDonutWidgetController', ['$scope', 'dashboardService', 'domainCfgService', 'INVENTORY',
        function ($scope, dashboardService, domainCfgService, INVENTORY) {
            var filter = angular.copy($scope.widget.conf);
            var invPieColors, invPieOrder;
            var fDate = (checkNotNullEmpty(filter.date) ? formatDate(filter.date) : undefined);
            $scope.showChart = false;
            $scope.wloading = true;
            $scope.showError = false;
            function setFilters() {
                if (checkNotNullEmpty(filter.period)) {
                    var p = filter.period;
                    if (p == '0' || p == '1' || p == '2' || p == '3' || p == '7' || p == '30') {
                        $scope.period = p;
                    }
                } else {
                    $scope.period = "0";
                }

                if (checkNotNullEmpty(filter.widType)) {
                    var widType = filter.widType;
                        $scope.widType = widType;
                        if ($scope.widType == 'ia') {
                            invPieColors[0] = "#efefef";
                            invPieColors[1] = invPieColors[3];
                            invPieColors[2] = invPieColors[3];
                            $scope.widget.conf.title = "Available";
                        } else if($scope.widType == 'iso'){
                            invPieColors[1] = "#efefef";
                            invPieColors[2] = "#efefef";
                            invPieColors[3] = "#efefef";
                            $scope.widget.conf.title = "Stocked out";
                        } else if($scope.widType == 'in'){
                            invPieColors[0] = "#efefef";
                            invPieColors[1] = "#efefef";
                            invPieColors[2] = "#efefef";
                            $scope.widget.conf.title = "Normal";
                        }else if($scope.widType == 'imin'){
                            invPieColors[0] = "#efefef";
                            invPieColors[2] = "#efefef";
                            invPieColors[3] = "#efefef";
                            $scope.widget.conf.title = "Minimum";
                        }else if($scope.widType == 'imax'){
                            invPieColors[0] = "#efefef";
                            invPieColors[1] = "#efefef";
                            invPieColors[3] = "#efefef";
                            $scope.widget.conf.title = "Maximum";
                        }
                } else {
                    $scope.widType = "ia";
                }

                if (checkNotNullEmpty(filter.materialTag)) {
                    $scope.exFilter = constructModel(filter.materialTag);
                    $scope.exType = 'mTag';
                } else if (checkNotNullEmpty(filter.material)) {
                    $scope.exFilter = filter.material.id;
                    $scope.exType = 'mId';
                }
            }

            domainCfgService.getSystemDashboardConfig().then(function (data) {
                var domainConfig = angular.fromJson(data.data);
                invPieColors = domainConfig.pie.ic;
                invPieOrder = domainConfig.pie.io;
                $scope.init();
            });

            $scope.init = function () {
                setFilters();
                getData();
            };

            function getData() {
                var chartData = [],totalInv = 0, totalInvText = '';
                dashboardService.get(undefined, undefined, $scope.exFilter, $scope.exType, $scope.period, undefined,
                    undefined, constructModel(filter.entityTag), fDate, constructModel(filter.exEntityTag), false).then(
                    function (data) {
                        chartData = constructPieData(data.data.invDomain, invPieColors, invPieOrder, INVENTORY,
                            $scope.mapEvent);
                        var normalPercent = getPercent(data.data.invDomain, $scope.widType);
                        totalInv = getItemCount(data.data.invDomain, $scope.widType);
                        if(totalInv>1){
                            totalInvText = totalInv + " inventory items";
                        }else{
                            totalInvText = totalInv + " inventory item";
                        }
                        setWidgetData(normalPercent, chartData, totalInvText);
                    }).catch(function error(msg) {
                        showError(msg, $scope);
                    }).finally(function () {
                        $scope.loading = false;
                        $scope.wloading = false;
                    });
            }

            function setWidgetData(centerLabelPercent, chartData, totalInvText) {
                var radius = getDonutRadius($scope.widget.width,$scope.widget.height);
                $scope.inventoryStatusDonutWidget = {
                    wId: $scope.widget.id,
                    cType: "doughnut2d",
                    copt: {
                        theme: "fint",
                        doughnutRadius: radius.doughnutRadius,
                        pieRadius: radius.pieRadius,
                        showLabels: "0",
                        showPercentValues: "0",
                        showLegend: "0",
                        showValues: "0",
                        defaultCenterLabel: centerLabelPercent,
                        subCaption: totalInvText,
                        captionOnTop: "0",
                        alignCaptionWithCanvas: "1",
                        showToolTip: "1",
                        centerLabelFontSize: 19,
                        centerLabelFont : 'Helvetica Neue, Arial'
                    },
                    cdata: chartData,
                    computedWidth: '100%',
                    computedHeight: parseInt($scope.widget.computedHeight, 10) - 30
                };
                $scope.wloading = false;
                $scope.showChart = true;
            }
        }]);

logistimoApp.requires.push('logistimo.storyboard.inventoryStatusDonutWidget');
