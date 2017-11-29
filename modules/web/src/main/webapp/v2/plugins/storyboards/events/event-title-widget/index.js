/**
 * Created by yuvaraj on 28/11/17.
 */
angular.module('logistimo.storyboard.eventTitleWidget', [])
    .config(function (widgetsRepositoryProvider) {
        widgetsRepositoryProvider.addWidget({
            id: "eventTitleWidget",
            name: "Event title ",
            templateUrl: "plugins/storyboards/events/event-title-widget/event-title-widget.html",
            editTemplateUrl: "plugins/storyboards/inventory/edit-template.html",
            templateFilters: [
                {
                    nameKey: 'title',
                    type: 'title'
                }],
            defaultHeight: 1,
            defaultWidth: 4
        });
    })
    .controller('eventTitleWidgetController',
        ['$scope', function ($scope) {
            $scope.title = "400 stock outs";
            $scope.subTitle = "In 50 stores across 12 districts";
            $scope.eventText = "IPV (dose) and 6 others stocked out over 15 days";

        }]);

logistimoApp.requires.push('logistimo.storyboard.eventTitleWidget');
