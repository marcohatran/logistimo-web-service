angular.module('logistimo.storyboard.inventoryStatusDonutWidget', [])
    .config(function (widgetsRepositoryProvider) {
        widgetsRepositoryProvider.addWidget({
            id: "inventoryStatusDonutWidget",
            name: "Inventory status donut",
            templateUrl: "plugins/storyboards/inventory/inventory-status-donut-widget/inventory-status-donut-widget.html",
            editTemplateUrl: "plugins/storyboards/inventory/edit-template.html",
            templateFilters: [
                {
                    nameKey: 'title',
                    type: 'title'
                },
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
            var invPieColors, invPieOrder, totalInv = 0;
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
                    if (widType == '0' || widType == '1') {
                        $scope.widType = widType;
                        if ($scope.widType == 0) {
                            invPieColors[0] = "#efefef";
                            invPieColors[1] = invPieColors[3];
                            invPieColors[2] = invPieColors[3];
                        } else {
                            invPieColors[1] = "#efefef";
                            invPieColors[2] = "#efefef";
                            invPieColors[3] = "#efefef";
                        }
                    }
                } else {
                    $scope.widType = "0";
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
                var chartData = [];
                dashboardService.get(undefined, undefined, $scope.exFilter, $scope.exType, $scope.period, undefined,
                    undefined, constructModel(filter.entityTag), fDate, constructModel(filter.exEntityTag), false).then(
                    function (data) {
                        chartData = constructPieData(data.data.invDomain, invPieColors, invPieOrder, INVENTORY,
                            $scope.mapEvent);
                        var normalPercent = getPercent(data.data.invDomain, $scope.widType);
                        totalInv = getTotalItems(data.data.invDomain);
                        setWidgetData(normalPercent, chartData);
                    }).catch(function error(msg) {
                        showError(msg, $scope);
                    }).finally(function () {
                        $scope.loading = false;
                        $scope.wloading = false;
                    });
            }

            function setWidgetData(centerLabelPercent, chartData) {
                $scope.inventoryStatusDonutWidget = {
                    wId: $scope.widget.id,
                    cType: "doughnut2d",
                    copt: {
                        theme: "fint",
                        doughnutRadius: '60',
                        pieRadius: '80',
                        showLabels: "0",
                        showPercentValues: "0",
                        showLegend: "0",
                        showValues: "0",
                        defaultCenterLabel: centerLabelPercent,
                        subCaption: totalInv + " inventory items",
                        captionOnTop: "0",
                        alignCaptionWithCanvas: "1",
                        showToolTip: "0"
                    },
                    cdata: chartData,
                    computedWidth: '100%',
                    computedHeight: parseInt($scope.widget.computedHeight, 10) - 100
                };
                $scope.wloading = false;
                $scope.showChart = true;
            }

            $scope.subCaption = undefined;

            if (checkNotNullEmpty(filter.materialTag)) {
                $scope.subCaption = "Material tag(s):" + getSubtext(filter.materialTag);
            } else if (checkNotNullEmpty(filter.material)) {
                $scope.subCaption = "Material tag(s):" + getSubtext(filter.material);
            }

            if (checkNotNullEmpty(filter.entityTag)) {
                $scope.subCaption =
                    (checkNullEmpty($scope.subCaption) ? '' : $scope.subCaption + ', ') + $scope.resourceBundle.kiosk +
                    " tag(s):" + getSubtext(filter.entityTag);
            }

            if (checkNotNullEmpty(filter.period)) {
                $scope.subCaption = (checkNullEmpty($scope.subCaption) ? '' : $scope.subCaption + ', ') + "Period: " +
                    $scope.widget.conf.period + " day(s)";
            }

        }]);

logistimoApp.requires.push('logistimo.storyboard.inventoryStatusDonutWidget');
