/**
 * Created by yuvaraj on 21/11/17.
 */
logistimoApp.controller('assetTemplateController', ['$scope', '$timeout', 'domainCfgService', function ($scope, $timeout, domainCfgService) {
    domainCfgService.getAssetSysCfg('2').then(function (data) {
        $scope.allAssets = data.data;
    }).catch(function error(msg) {
        $scope.showErrorMsg(msg);
    }).finally(function () {
        $scope.hideLoading();
    });

    $scope.filterAssets = function (query) {
        var rData = {results: []};
        for (var key in $scope.allAssets) {
            if ($scope.allAssets[key].toLowerCase().indexOf(query.term.toLowerCase()) != -1) {
                rData.results.push({'text': $scope.allAssets[key], 'id': key});
            }
        }
        query.callback(rData);
    };

    $scope.secondaryMetric = [];
    $scope.secondaryMetric.push({name: ">= 1 hour", value: "0"});
    $scope.secondaryMetric.push({name: ">= 2 hours", value: "1"});
    $scope.secondaryMetric.push({name: ">= 3 hours", value: "2"});
    $scope.secondaryMetric.push({name: ">= 5 hours", value: "3"});
    $scope.secondaryMetric.push({name: ">= 10 hours", value: "4"});

    $scope.filterStatus = function (query) {

        var sData = {results: []};

        sData.results[0] = {id: 'tn', text: "Normal"};
        sData.results[1] = {id: 'tl', text: "Low"};
        sData.results[2] = {id: 'th', text: "High"};
        sData.results[3] = {id: 'tu', text: "Unknown"};

        query.callback(sData);
    };

    $scope.$watch('widget.conf.entityTag', function (newVal, oldVal) {
        if (oldVal != newVal) {
            if (newVal instanceof Object || newVal == undefined) {
                $scope.widget.conf.exEntityTag = "";
            }
        }
    });

    $scope.$watch('widget.conf.exEntityTag', function (newVal, oldVal) {
        if (oldVal != newVal) {
            if (newVal instanceof Object || newVal == undefined) {
                $scope.widget.conf.entityTag = "";
            }
        }
    });

    $scope.$watch('widget.conf.excursionType', function (newVal, oldVal) {
        if(oldVal != newVal) {
            $scope.widget.conf.exposureTime = "";
            $scope.secondaryMetric = [];
            if(newVal == "0") {
                $scope.secondaryMetric.push({name: ">= 1 hour", value: "0"});
                $scope.secondaryMetric.push({name: ">= 5 hours", value: "3"});
                $scope.secondaryMetric.push({name: ">= 10 hours", value: "4"});
            } else if(newVal == "1") {
                $scope.secondaryMetric.push({name: ">= 1 hour", value: "0"});
                $scope.secondaryMetric.push({name: ">= 2 hours", value: "1"});
                $scope.secondaryMetric.push({name: ">= 3 hours", value: "2"});
            }
        }
    });

}
]);