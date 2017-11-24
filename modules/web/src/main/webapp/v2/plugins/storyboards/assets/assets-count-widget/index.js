/**
 * Created by yuvaraj on 10/11/17.
 */
angular.module('logistimo.storyboard.assetsCountWidget', [])
    .config(function (widgetsRepositoryProvider) {
        widgetsRepositoryProvider.addWidget({
            id: "assetsCountWidget",
            name: "Asset count",
            templateUrl: "plugins/storyboards/assets/assets-count-widget/assets-count-widget.html",
            editTemplateUrl: "plugins/storyboards/assets/asset-edit-template.html",
            templateFilters: [
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
                }
            ],
            defaultHeight: 1,
            defaultWidth: 2
        });
    })
    .controller('assetsCountWidgetController', ['$scope', 'dashboardService', function ($scope, dashboardService) {
        var filter = angular.copy($scope.widget.conf);
        var asset = '';
        $scope.totalAssets = 0;
        var fDate = (checkNotNullEmpty(filter.date) ? formatDate(filter.date) : undefined);
        $scope.showChart = false;
        $scope.wloading = true;
        $scope.showError = false;
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
        
        getData();
        
        function getData() {
            dashboardService.get(undefined, undefined, $scope.exFilter, $scope.exType, $scope.period,
                                 $scope.widget.conf.tPeriod, asset, constructModel(filter.entityTag), fDate,
                                 constructModel(filter.exEntityTag), false).then(function (data) {
                $scope.totalAssets = getTotalItems(data.data.tempDomain);
            }).catch(function error(msg) {
                showError(msg,$scope);
            }).finally(function () {
                $scope.loading = false;
                $scope.wloading = false;
            });
        }
        
    }]);

logistimoApp.requires.push('logistimo.storyboard.assetsCountWidget');
