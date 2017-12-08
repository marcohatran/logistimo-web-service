/**
 * Created by yuvaraj on 10/11/17.
 */
angular.module('logistimo.storyboard.storesActivityWidget', [])
    .config(function (widgetsRepositoryProvider) {
        widgetsRepositoryProvider.addWidget({
            id: "storesActivityWidget",
            name: "Entity activity bar",
            templateUrl: "plugins/storyboards/inventory/stores-activity-widget/stores-activity-widget.html",
            editTemplateUrl: "plugins/storyboards/inventory/edit-template.html",
            templateFilters: [
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
            defaultHeight: 1,
            defaultWidth: 4
        });
    })
    .controller('storesActivityWidgetController',
    ['$scope', 'dashboardService', 'domainCfgService', function ($scope, dashboardService, domainCfgService) {
        var filter = angular.copy($scope.widget.conf);
        var entPieColors;
        var fDate = (checkNotNullEmpty(filter.date) ? formatDate(filter.date) : undefined);
        $scope.showChart = false;
        $scope.totalInv = 0;
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
            entPieColors = domainConfig.pie.ec;
        }).then(function () {
            setFilters();
        }).then(function () {
            getData();
        });

        function getData() {
            dashboardService.get(undefined, undefined, $scope.exFilter, $scope.exType, $scope.period, undefined,
                undefined, constructModel(filter.entityTag), fDate, constructModel(filter.exEntityTag), false).then(
                function (data) {
                    constructStackData(data.data.entDomain);
                }).catch(function error(msg) {
                    showError(msg, $scope);
                }).finally(function () {
                    $scope.loading = false;
                    $scope.wloading = false;
                });
        }

        function constructStackData(data) {
            $scope.entDomainTotal = (data.a || 0) + (data.i || 0);
            var stackData = [];
            $scope.activeData = data.a ? data.a : 0;
            $scope.activePercent = getNormalPercent($scope.entDomainTotal, $scope.activeData);
            var sData = constructStack(data.a, "Active", entPieColors[0], $scope.entDomainTotal, "a");
            if (!checkNullEmptyObject(sData)) {
                stackData.push(sData);
            }
            sData = constructStack(data.i, "Inactive", entPieColors[1], $scope.entDomainTotal, "i");
            if (!checkNullEmptyObject(sData)) {
                stackData.push(sData);
            }
            setWidgetData(stackData);
        }

        function constructStack(data, seriesName, barColor, denominator) {
            var stackSeries = {};
            if (denominator > 0) {
                stackSeries.seriesname = seriesName;
                stackSeries.data = [{value: data, color: barColor}];
            }
            return stackSeries;
        }

        function getNormalPercent(total, data) {
            return (data / total * 100).toFixed(0);
        }

        $scope.pOpt = {
            "showLabels": 0,
            "showLegend": 1,
            "showPercentValues": 1,
            "showPercentInToolTip": 0,
            "enableSmartLabels": 1,
            "toolTipSepChar": ": ",
            "theme": "fint",
            "enableRotation": 0,
            "enableMultiSlicing": 0,
            "labelFontSize": 12,
            "slicingDistance": 15,
            "useDataPlotColorForLabels": 1,
            "subCaptionFontSize": 10,
            "interactiveLegend": 0,
            "exportEnabled": 1,
            "captionOnTop": 0
        };
        $scope.pieOpt = [];
        $scope.pieOpt[0] = angular.copy($scope.pOpt);
        $scope.stackBarOptions = angular.copy($scope.pOpt);
        $scope.stackBarOptions.stack100Percent = 1;
        $scope.stackBarOptions.exportEnabled = 0;
        $scope.stackBarOptions.showLegend = 0;
        $scope.stackBarOptions.showValues = 0;
        $scope.stackBarOptions.showLabels = 0;
        $scope.stackBarOptions.showXAxisLine = 0;
        $scope.stackBarOptions.showYAxisLine = 0;
        $scope.stackBarOptions.maxBarHeight = 3;
        $scope.stackBarOptions.valueFontColor = '#ffffff';

        function setWidgetData(stackData) {


            $scope.wloading = false;

            $scope.storesActivityWidget = {
                wId: $scope.widget.id,
                cType: "stackedbar2d",
                copt: $scope.stackBarOptions,
                cLabel: [{label: 'Inv status'}],
                cdata: stackData,
                computedWidth: '100%',
                computedHeight: parseInt($scope.widget.computedHeight, 10) - 30
            };
            $scope.showChart = true;
        }
    }]);

logistimoApp.requires.push('logistimo.storyboard.storesActivityWidget');
