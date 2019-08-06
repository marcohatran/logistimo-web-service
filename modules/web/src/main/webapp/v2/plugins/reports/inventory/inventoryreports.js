/*
 * Copyright © 2017 Logistimo.
 *
 * This file is part of Logistimo.
 *
 * Logistimo software is a mobile & web platform for supply chain management and remote temperature monitoring in
 * low-resource settings, made available under the terms of the GNU Affero General Public License (AGPL).
 *
 * This program is free software: you can redistribute it and/or modify it under the terms of the GNU Affero General
 * Public License as published by the Free Software Foundation, either version 3 of the License, or (at your option) any
 * later version.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied
 * warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU Affero General Public License
 * for more details.
 *
 * You should have received a copy of the GNU Affero General Public License along with this program.  If not, see
 * <http://www.gnu.org/licenses/>.
 *
 * You can be released from the requirements of the license by purchasing a commercial license. To know more about
 * the commercial license, please contact us at opensource@logistimo.com
 */

function InventoryReportController(s, timeout, getData, reportsServiceCore) {
    s.resourceBundle = s.$parent.$parent.resourceBundle;
    s.ddist = s.$parent.$parent.ddist;
    s.dstate = s.$parent.$parent.dstate;
    s.showLoading = s.$parent.$parent.showLoading;
    s.hideLoading = s.$parent.$parent.hideLoading;
    s.exportAsCSV = s.$parent.$parent.exportAsCSV;
    s.showSuccess = s.$parent.$parent.showSuccess;
    s.showErrorMsg = s.$parent.$parent.showErrorMsg;
    s.MAX_MONTHS = 11;
    s.MAX_WEEKS = 15;
    s.MAX_DAYS = 31;

    s.PAGE_SIZE = 50;

    s.today = new Date();
    s.today.setHours(0, 0, 0, 0);

    s.cHeight = "400";
    s.cWidth = "800";

    s.mandatoryFilters = {};
    s.metricHeadings = [];
    s.primaryMetric = [];
    s.secondaryMetric = [];
    s.tertiaryMetric = [];
    s.filterLabels = {};

    s.activeMetric = "ot";
    s.metricHeadings.push({name: s.resourceBundle['overview'], code: "ot"});
    s.metricHeadings.push({name: s.resourceBundle['reports.by.materials'], code: "mt"});
    s.metricHeadings.push({name: s.resourceBundle['reports.by.entities'], code: "kt"});
    s.metricHeadings.push({name: s.resourceBundle['reports.by.locations'], code: "rt"});

    s.locationOptions = [
        {text: s.resourceBundle['show.by'] + ' ' + s.resourceBundle['district'], value: "dis"},
        {text: s.resourceBundle['show.by'] + ' ' + s.resourceBundle['taluk'], value: "tlk"},
        {text: s.resourceBundle['show.by'] + ' ' + s.resourceBundle['city'], value: "cty"}
    ];
    s.vw = "c";

    s.hideFilter = false;
    s.noData = true;

    s.cOptions = {
        "exportEnabled": "1",
        "theme": "fint",
        "subCaptionFontSize": 10,
        "yAxisNamePadding": 20,
        "rotateValues": "0",
        "placevaluesInside": 0,
        "valueFontColor": "#000000"
    };

    s.updateTableHeading = function (tableHeading, dateFormat) {
        dateFormat = dateFormat || "mmm dd, yyyy";
        if (checkNotNullEmpty(tableHeading) && tableHeading.length > 0) {
            if (tableHeading[0] == 'kid') {
                tableHeading[0] = s.resourceBundle['kiosk'];
            } else if (tableHeading[0] == 'mid') {
                tableHeading[0] = s.resourceBundle['material'];
            } else if (tableHeading[0].toLowerCase() == 'ktag') {
                tableHeading[0] = s.resourceBundle['kiosk'] + s.resourceBundle['tags'];
            } else {
                tableHeading[0] = tableHeading[0].charAt(0).toUpperCase() + tableHeading[0].slice(1);
            }
            for (var i = 1; i < tableHeading.length; i++) {
                tableHeading[i] = reportCoreFunction().formatDateLabel(tableHeading[i], dateFormat);
            }
        }
    };

    s.setMetric = function (metric) {
        if (s.activeMetric != metric) {
            s.oldMetric = s.activeMetric;
            s.activeMetric = metric;
            s.qFilter = angular.copy(s.filter);
            s.size = s.PAGE_SIZE;
            s.offset = 0;
            if (metric == 'ot') {
                checkFromDate();
            } else {
                s.filter.fromCopy = undefined;
            }
            s.tableDataLength = 0;
            s.validateAndGetData();
        }
    };

    s.openFilters = function () {
        s.hideFilter = !s.hideFilter;
        s.noResetMetric = false;
        copyFilters();
    };

    s.cancel = function () {
        restoreFilter();
        s.isCancel = true;
        s.hideFilter = true;
        timeout(function () {
            s.isCancel = false;
        }, 500);
    };

    function resetCards() {
        s.cards = {};
        if (s.hideMaterialTagFilter) {
            s.cards.mc = 'i';
        }
    }

    s.resetFilters = function () {
        s.filter = {};
        s.compare = {type: undefined};
        s.metrics = {};
        resetMetrics();
        resetCards();
        s.filter.periodicity = "m";
        s.dateMode = 'month';
        var year = new Date();
        year.setHours(0, 0, 0, 0);
        year.setDate(1);
        s.filter.to = new Date(year);
        year.setMonth(year.getMonth() - s.MAX_MONTHS);
        s.filter.from = new Date(year);
        s.skipDateWarn = true;
        s.maxDate = new Date();
        s.size = s.PAGE_SIZE;
        s.offset = 0;
    };
    s.resetFilters(true);

    s.changeSubHeading = function (group, heading) {
        if (s.cards.mc != heading && group == 'm') {
            if (s.compare.type == group) {
                s.compare.type = undefined;
            }
            s.cards.mc = heading;
            s.filter.mat = s.filter.mmat = s.filter.mtag = s.filter.mmtag = undefined;
        } else if (s.cards.ec != heading && group == 'e') {
            if (s.compare.type == group) {
                s.compare.type = undefined;
            }
            s.cards.ec = heading;
            s.filter.entity = s.filter.mentity = s.filter.etag = s.filter.metag = s.filter.lkid = undefined;
        } else if (s.cards.lc != heading && group == 'l') {
            if (s.compare.type == group) {
                s.compare.type = undefined;
            }
            s.cards.lc = heading;
            s.filter.st = s.filter.mst = s.filter.dis = s.filter.mdis = s.filter.tlk = s.filter.mtlk = s.filter.cty = s.filter.mcty = undefined;
        }
    };

    s.fetchNext = function () {
        s.offset += s.size;
        getData();
    };

    s.fetchPrev = function () {
        s.offset -= s.size;
        getData();
    };

    s.fetchFirst = function () {
        s.offset = 0;
        getData();
    };

    s.populateFilters = function () {
        var selectedFilters = angular.copy(s.qFilter);
        for (var filter in selectedFilters) {
            if (checkNullEmpty(selectedFilters[filter])) {
                delete selectedFilters[filter];
            }
        }
        angular.forEach(selectedFilters, function (value, key) {
            if (checkNotNullEmpty(value)) {
                if (key == "from" || key == "to") {
                    selectedFilters[key] = formatDate2Url(value);
                    //console.log(value);
                } else if (key == "mat") {
                    selectedFilters[key] = value.mId;
                } else if (key == "mtag" || key == "etag") {
                    selectedFilters[key] = value.id;
                } else if (key == "entity") {
                    selectedFilters[key] = value.id
                } else if (key == "lkid") {
                    selectedFilters[key] = value.id
                } else if (key == "st") {
                    selectedFilters["cn"] = value.country;
                    selectedFilters["st"] = value.label;
                } else if (key == "dis") {
                    selectedFilters["cn"] = value.country;
                    selectedFilters["st"] = value.state;
                    selectedFilters["dis"] = value.label;
                } else if (key == "tlk") {
                    selectedFilters["cn"] = value.country;
                    selectedFilters["st"] = value.state;
                    selectedFilters["dis"] = value.district;
                    selectedFilters["tlk"] = value.label;
                } else if (key == "cty") {
                    selectedFilters["cn"] = value.country;
                    selectedFilters["st"] = value.state;
                    selectedFilters["dis"] = value.district;
                    selectedFilters["tlk"] = value.taluk;
                    selectedFilters["cty"] = value.label;
                }
            }
        });
        selectedFilters['compare'] = 'none';

        //concatenate the compare fields
        if (checkNotNullEmpty(s.compare.type)) {
            if (s.compare.type == 'm') {
                if (s.cards.mc == 'i') {
                    selectedFilters['mat'] += getCompareStringAsCsv(selectedFilters['mmat'], 'mId');
                    selectedFilters['mmat'] = undefined;
                    selectedFilters['compare'] = 'mat';
                } else if (s.cards.mc == undefined) {
                    selectedFilters['mtag'] += getCompareStringAsCsv(selectedFilters['mmtag'], 'id');
                    selectedFilters['mmtag'] = undefined;
                    selectedFilters['compare'] = 'mtag';
                }
            } else if (s.compare.type == 'e') {
                if (s.cards.ec == 'i') {
                    selectedFilters['entity'] += getCompareStringAsCsv(selectedFilters['mentity'], 'id');
                    selectedFilters['mentity'] = undefined;
                    selectedFilters['compare'] = 'entity';
                } else if (s.cards.ec == undefined) {
                    selectedFilters['etag'] += getCompareStringAsCsv(selectedFilters['metag'], 'id');
                    selectedFilters['metag'] = undefined;
                    selectedFilters['compare'] = 'etag';
                }
            } else if (s.compare.type == 'l') {
                if (s.cards.lc == undefined) {
                    selectedFilters["cn"] += getCompareStringAsCsv(selectedFilters['mst'], 'country');
                    selectedFilters["st"] += getCompareStringAsCsv(selectedFilters['mst'], 'label');
                    selectedFilters['mst'] = undefined;
                    selectedFilters['compare'] = 'st';
                } else if (s.cards.lc == 'd') {
                    selectedFilters["cn"] += getCompareStringAsCsv(selectedFilters['mdis'], 'country');
                    selectedFilters["st"] += getCompareStringAsCsv(selectedFilters['mdis'], 'state');
                    selectedFilters['dis'] += getCompareStringAsCsv(selectedFilters['mdis'], 'label');
                    selectedFilters['mdis'] = undefined;
                    selectedFilters['compare'] = 'dis';
                } else if (s.cards.lc == 't') {
                    selectedFilters["cn"] += getCompareStringAsCsv(selectedFilters['mtlk'], 'country');
                    selectedFilters["st"] += getCompareStringAsCsv(selectedFilters['mtlk'], 'state');
                    selectedFilters['dis'] += getCompareStringAsCsv(selectedFilters['mtlk'], 'district');
                    selectedFilters['tlk'] += getCompareStringAsCsv(selectedFilters['mtlk'], 'label');
                    selectedFilters['mtlk'] = undefined;
                    selectedFilters['compare'] = 'tlk';
                } else if (s.cards.lc == 'c') {
                    selectedFilters["cn"] += getCompareStringAsCsv(selectedFilters['mcty'], 'country');
                    selectedFilters["st"] += getCompareStringAsCsv(selectedFilters['mcty'], 'state');
                    selectedFilters['dis'] += getCompareStringAsCsv(selectedFilters['mcty'], 'district');
                    selectedFilters['tlk'] += getCompareStringAsCsv(selectedFilters['mcty'], 'taluk');
                    selectedFilters['cty'] += getCompareStringAsCsv(selectedFilters['mcty'], 'label');
                    selectedFilters['mcty'] = undefined;
                    selectedFilters['compare'] = 'cty';
                }
            }
        }

        function getCompareStringAsCsv(values, property) {
            var v = '';
            angular.forEach(values, function (value) {
                v = v + ',' + (property ? value[property] : value);
            });
            return v;
        }

        return selectedFilters;
    };
    function reorderHeading(headingArr) {
        var headingName = "";
        if (s.compare.type == 'm') {
            if (s.cards.mc == 'i') {
                headingName = s.filter.mat.mnm;
            } else {
                headingName = s.filter.mtag.text;
            }
        } else if (s.compare.type == 'e') {
            if (s.cards.ec == 'i') {
                headingName = s.filter.entity.nm;
            } else {
                headingName = s.filter.etag.text;
            }
        } else if (s.compare.type == 'l') {
            if (s.cards.lc == undefined) {
                headingName = s.filter.st.label;
            } else if (s.cards.lc == 'd') {
                headingName = s.filter.dis.label;
            } else if (s.cards.lc == 't') {
                headingName = s.filter.tlk.label;
            } else if (s.cards.lc == 'c') {
                headingName = s.filter.cty.label;
            }
        }
        for (var index = 0; index < headingArr.length; index++) {
            var headingObj = headingArr[index];
            if (headingObj.name == headingName) {
                headingArr.splice(index, 1);
                headingArr.unshift(headingObj);
                break;
            }
        }
        return headingArr;
    }

    s.swapData = function (headings, consolidatedData) {
        var headingsArray = [];
        for (var i = 1; i < headings.length; i++) {
            headingsArray.push({name: headings[i], index: i});
        }
        headingsArray = sortByKey(headingsArray, 'name');
        headingsArray = reorderHeading(headingsArray);
        var indexArray = [];
        for (var j = 0; j < headingsArray.length; j++) {
            var headingObj = headingsArray[j];
            headings[j + 1] = headingObj.name;
            indexArray.push(headingObj.index);
        }
        reorderData(consolidatedData, indexArray);
    }

    function reorderData(consolidatedData, indexArr) {
        var dataCopy = angular.copy(consolidatedData);
        for (var k = 0; k < dataCopy.length; k++) {
            var rowData = dataCopy[k];
            var reorderedData = [];
            reorderedData[0] = rowData[0];
            for (var i = 0; i < indexArr.length; i++) {
                reorderedData[i + 1] = rowData[indexArr[i]];
            }
            consolidatedData[k] = reorderedData;
        }
    }

    s.applyFilter = function () {
        if (s.validate) {
            if (!s.validate()) {
                return;
            }
        }
        if (s.noResetMetric && s.setMetrics) {
            s.setMetrics();
        } else {
            resetMetrics();
        }
        filterHeadings();
        s.qFilter = angular.copy(s.filter);
        s.tableData = undefined;
        s.tableDataLength = undefined;
        s.offset = 0;
        getData();
        updateLabels();
        getDataNotAvailableText();
        s.openFilters();
        setHeight();
    };

    s.validateAndGetData = function () {
        if (s.validate) {
            if (!s.validate()) {
                return;
            }
        }
        getData();
    };

    s.getFilterLabel = function () {
        var label = '';
        for (var i in s.filterLabels) {
            if (checkNotNullEmpty(s.filterLabels[i]) && i != "Periodicity") {
                label += i + ": " + s.filterLabels[i] + "   ";
            }
        }
        return label;
    };

    // Current custom fusion chart directive will call only this method in case of drilling down to daily from monthly.
    s.getDFChartData = function (label) {
        s.qFilter = angular.copy(s.filter);
        s.qFilter.level = "d";
        s.qFilter.levelPeriodicity = angular.copy(s.qFilter.periodicity);
        s.qFilter.periodicity = "d";
        s.qFilter.from = parseUrlDate(label);
        getData();
    };

    s.isUndef = function (value) {
        return (value == undefined || value == '');
    };

    s.formatReportDate = function (date) {
        if (s.filter.periodicity == "m") {
            return FormatDate_MMM_YYYY(date);
        } else {
            return FormatDate_MMM_DD_YYYY(date);
        }
    };

    s.getReportDateFormat = function (level) {
        return (s.filter.periodicity == "m" && level == undefined) ? "mmm yyyy" : undefined;
    };

    s.getBreakDownCaption = function () {
        if (s.filter.periodicity == "m") {
            return s.resourceBundle['reports.daily.trends.monthly.caption'] + ' ' + FormatDate_MMM_YYYY(s.qFilter.from)
        } else {
            return s.resourceBundle['reports.daily.trends.weekly.caption'] + ' ' + FormatDate_MMM_DD_YYYY(s.qFilter.from)
        }
    };

    s.getReportCaption = function (isTable) {
        if (isTable) {
            var fromDate = new Date(s.filter.to);
            if (s.qFilter.periodicity == "m") {
                fromDate.setMonth(fromDate.getMonth() - 2);
            } else if (s.qFilter.periodicity == "w") {
                fromDate.setDate(fromDate.getDate() - 3 * 7);
            } else {
                fromDate.setDate(fromDate.getDate() - 6);
            }
            return s.resourceBundle['from.upper'] + ": " + s.formatReportDate(fromDate) + "   " + s.resourceBundle['to.upper'] + ": " + s.formatReportDate(s.filter.to) + "   " + s.getFilterLabel()
        } else {
            return s.resourceBundle['from.upper'] + ": " + s.formatReportDate(s.filter.from) + "   " + s.resourceBundle['to.upper'] + ": " + s.formatReportDate(s.filter.to) + "   " + s.getFilterLabel()
        }
    };

    function getReportType(reportType) {
        switch (reportType) {
            case 'ias':
                return s.resourceBundle['report.abnormal.stock'];
            case 'ic':
                return s.resourceBundle['report.consumption'];
            case 'id':
                return s.resourceBundle['report.discards'];
            case 'ir':
                return s.resourceBundle['report.replenishment.response.time'];
            case 'isa':
                return s.resourceBundle['report.stock.availability'];
            case 'ist':
                return s.resourceBundle['report.stock.trends'];
            case 'is':
                return s.resourceBundle['report.supply'];
            case 'iu':
                return s.resourceBundle['report.utilization'];
            case 'itc':
                return s.resourceBundle['report.transaction.counts'];
            default:
                return undefined;
        }
    }

    s.exportData = function (reportType, isInfo) {
        var selectedFilters = s.populateFilters();
        selectedFilters['type'] = reportType;
        selectedFilters['viewtype'] = s.activeMetric;
        selectedFilters['titles'] = {};
        var periodicityDisplays = {
            daily: s.resourceBundle['daily'],
            weekly: s.resourceBundle['weekly'],
            monthly: s.resourceBundle['monthly']
        };
        var filterTitles = s.resourceBundle['periodicity'] + ": " + reportCoreFunction().getPeriodicityDisplay(s.filter.periodicity, periodicityDisplays) + "   " + s.tableCaption;
        selectedFilters['titles']['filters'] = filterTitles;
        selectedFilters['titles']['metrics'] = s.tableMetric;
        selectedFilters['titles']['viewtype'] = s.activeMetric;
        selectedFilters['primaryMetricIndex'] = s.metrics.primary;
        selectedFilters['secondaryMetricIndex'] = s.metrics.secondary;
        selectedFilters['tertiaryMetricIndex'] = s.metrics.tertiary;
        if (isInfo) {
            return {
                filters: filterTitles,
                type: getReportType(reportType)
            };
        }
        s.showLoading();
        reportsServiceCore.exportData(JSON.stringify(angular.toJson(selectedFilters))).then(function (data) {
            s.showSuccess(data.data);
        }).catch(function error(msg) {
            s.showErrorMsg(msg);
        }).finally(function () {
            s.hideLoading();
        });
    };

    s.$watch("filter.to", function (newValue, oldValue) {
        if (newValue.getTime() != oldValue.getTime()) {
            if (s.hideFilter && s.tempFilters['filter']['to'].getTime() != newValue.getTime()) {
                s.hideFilter = false;
                copyFilters();
                s.tempFilters['filter']['to'] = oldValue;
            }
        }
    });

    s.$watch("filter.periodicity", function (newValue, oldValue) {
        if (newValue != oldValue) {
            if (s.hideFilter && s.tempFilters['filter']['periodicity'] != newValue) {
                s.hideFilter = false;
                copyFilters();
                s.tempFilters['filter']['periodicity'] = oldValue;
            }
            if (!s.isCancel) {
                var fromDate = new Date();
                fromDate.setHours(0, 0, 0, 0);
                if (newValue == "m") {
                    s.dateMode = 'month';
                    fromDate.setDate(1);
                    fromDate.setMonth(fromDate.getMonth() - s.MAX_MONTHS);
                    s.filter.from = new Date(fromDate);
                    var toDate = new Date();
                    toDate.setHours(0, 0, 0, 0);
                    toDate.setDate(1);
                    s.filter.to = toDate;
                } else if (newValue == 'w') {
                    fromDate.setDate(fromDate.getDate() - (s.MAX_WEEKS - 1) * 7);
                    fromDate.setDate(fromDate.getDate() + (fromDate.getDay() == 0 ? -6 : 1 - fromDate.getDay()));
                    s.filter.from = new Date(fromDate);
                    toDate = new Date();
                    toDate.setHours(0, 0, 0, 0);
                    toDate.setDate(toDate.getDate() + (toDate.getDay() == 0 ? -6 : 1 - toDate.getDay()));
                    s.filter.to = toDate;
                    s.dateMode = 'week';
                } else {
                    fromDate.setDate(fromDate.getDate() - s.MAX_DAYS + 1);
                    s.filter.from = new Date(fromDate);
                    s.filter.to = new Date();
                    s.filter.to.setHours(0, 0, 0, 0);
                    s.dateMode = 'day';
                }
                s.skipDateWarn = true;
            } else {
                if (newValue == "m") {
                    s.dateMode = 'month';
                } else if (newValue == 'w') {
                    s.dateMode = 'week';
                } else {
                    s.dateMode = 'day';
                }
            }
        }
    });

    s.$watch("filter.from", function (newValue, oldValue) {
        if (s.activeMetric == 'ot') {
            s.filter.fromCopy = newValue;
        }
        if (newValue.getTime() != oldValue.getTime()) {
            if (s.hideFilter && s.tempFilters['filter']['from'].getTime() != newValue.getTime()) {
                s.hideFilter = false;
                copyFilters();
                if (s.oldMetric != undefined && s.oldMetric != 'ot' && s.activeMetric == 'ot') {
                    s.tempFilters['filter']['from'] = newValue;
                    s.oldMetric = undefined;
                } else {
                    s.tempFilters['filter']['from'] = oldValue;
                }
            }
            var toDate = new Date(newValue);
            //if to date is beyond limit, reset to limit
            if (s.filter.periodicity == 'm') {
                toDate.setMonth(toDate.getMonth() + s.MAX_MONTHS);
                if (toDate.getTime() > s.today.getTime()) {
                    toDate = new Date(s.today);
                    toDate.setDate(1);
                }
            } else if (s.filter.periodicity == 'w') {
                toDate.setDate(toDate.getDate() + s.MAX_WEEKS * 7);
                if (toDate.getTime() > s.today.getTime()) {
                    toDate = new Date(s.today);
                    toDate.setDate(toDate.getDate() + (toDate.getDay() == 0 ? -6 : 1 - toDate.getDay()));
                }
            } else {
                toDate.setDate(toDate.getDate() + s.MAX_DAYS - 1);
                if (toDate.getTime() > s.today.getTime()) {
                    toDate = new Date(s.today);
                }
            }
            s.maxDate = new Date(toDate);
            setToDate(toDate);
        }
        s.skipDateWarn = false;
    });

    s.$watchGroup(["filter.st", "filter.dis"], function (newVal) {
        let isState = s.cards.lc == undefined;
        let value = isState ? newVal[0] : newVal[1];
        if (value == undefined) {
            s.filter.location_by = undefined;
        } else {
            if (isState) {
                s.filter.location_by = s.locationOptions[0].value;
            } else {
                s.filter.location_by = s.locationOptions[1].value;
            }
        }
    });
    function copyFilters() {
        s.tempFilters = {};
        s.tempFilters['filter'] = angular.copy(s.filter);
        s.tempFilters['compare'] = angular.copy(s.compare);
        s.tempFilters['cards'] = angular.copy(s.cards);
    }

    function restoreFilter() {
        if (checkNotNullEmpty(s.tempFilters)) {
            s.filter = angular.copy(s.tempFilters['filter']);
            s.compare = angular.copy(s.tempFilters['compare']);
            s.cards = angular.copy(s.tempFilters['cards']);
        }
    }

    function checkFromDate() {
        if (s.filter.from.getTime() > s.filter.to.getTime()) {
            s.filter.from = angular.copy(s.filter.to);
            if (s.filter.periodicity == 'm') {
                s.filter.from.setMonth(s.filter.from.getMonth() - s.MAX_MONTHS);
            } else if (s.filter.periodicity == 'w') {
                s.filter.from.setDate(s.filter.from.getDate() - s.MAX_WEEKS * 7);
            } else {
                s.filter.from.setDate(s.filter.from.getDate() - s.MAX_DAYS);
            }
        }
    }

    function filterHeadings() {
        angular.forEach(s.metricHeadings, function (h) {
            h.hide = undefined;
        });
        if (checkNotNullEmpty(s.filter['entity'])) {
            s.metricHeadings[2].hide = "true";
            s.metricHeadings[3].hide = "true";
            if (s.activeMetric == s.metricHeadings[2].code || s.activeMetric == s.metricHeadings[3].code) {
                s.activeMetric = s.metricHeadings[0].code;
            }
        }
        if (checkNotNullEmpty(s.filter['mat'])) {
            s.metricHeadings[1].hide = "true";
            if (s.activeMetric == s.metricHeadings[1].code) {
                s.activeMetric = s.metricHeadings[0].code;
            }
        }
        if (checkNotNullEmpty(s.filter['tlk']) || checkNotNullEmpty(s.filter['cty'])) {
            s.metricHeadings[3].hide = "true";
            if (s.activeMetric == s.metricHeadings[3].code) {
                s.activeMetric = s.metricHeadings[0].code;
            }
        }
        if (checkNotNullEmpty(s.filter['mentity']) || checkNotNullEmpty(s.filter['metag']) ||
            checkNotNullEmpty(s.filter['mmat']) || checkNotNullEmpty(s.filter['mmtag']) ||
            checkNotNullEmpty(s.filter['mst']) || checkNotNullEmpty(s.filter['mdis']) ||
            checkNotNullEmpty(s.filter['mtlk']) || checkNotNullEmpty(s.filter['mcty'])) {
            s.metricHeadings[1].hide = "true";
            s.metricHeadings[2].hide = "true";
            s.metricHeadings[3].hide = "true";
        }
    }

    function setHeight() {
        timeout(function () {
            var top = $("#fixedMenu").height() + $("#fixedfilter").height();
            $('.report-data-pane').css("margin-top", top + 40);
        }, 300);
    }

    setHeight();

    function resetMetrics() {
        s.metrics.primary = "1";
        s.metrics.secondary = "0";
        s.metrics.tertiary = "0";
        s.tableSeriesNo = 1;
    }

    function setToDate(date) {
        if (s.filter.from.getTime() > s.filter.to.getTime()) {
            s.filter.to = new Date(s.filter.from);
        } else if (date.getTime() < s.filter.to.getTime()) {
            if (s.filter.to.getTime() != date.getTime()) {
                s.filter.to = new Date(date);
                if (!s.skipDateWarn) {
                    var period = ' days';
                    var maxPeriod = s.MAX_DAYS;
                    if (s.filter.periodicity == 'm') {
                        period = ' months';
                        maxPeriod = s.MAX_MONTHS + 1;
                    } else if (s.filter.periodicity == 'w') {
                        period = ' weeks';
                        maxPeriod = s.MAX_WEEKS;
                    }
                    toastr.warning(messageFormat(s.resourceBundle['reports.filters.from.to.range'], maxPeriod, period));
                }
            }
        }
    }

    function updateLabels() {
        var labels = {m: "monthly", w: "weekly", d: "daily"};
        s.filterLabels[s.resourceBundle['periodicity']] =
            labels[s.filter.periodicity] ? s.resourceBundle[labels[s.filter.periodicity]] : undefined;
        //s.filterLabels[s.resourceBundle['from']] = FormatDate_MMM_YYYY(s.filter.from);
        //s.filterLabels[s.resourceBundle['to']] = FormatDate_MMM_YYYY(s.filter.to);
        s.filterLabels[s.resourceBundle['material']] = s.filter.mat ? s.filter.mat.mnm : undefined;
        s.filterLabels[s.resourceBundle['tagmaterial']] = s.filter.mtag ? s.filter.mtag.text : undefined;
        s.filterLabels[s.resourceBundle['kiosk']] = s.filter.entity ? s.filter.entity.nm : undefined;
        s.filterLabels[s.resourceBundle['tagentity']] = s.filter.etag ? s.filter.etag.text : undefined;
        s.filterLabels[s.resourceBundle['state']] = s.filter.st ? s.filter.st.label : undefined;
        s.filterLabels[s.resourceBundle['district']] = s.filter.dis ? s.filter.dis.label + ", " + s.filter.dis.subLabel : undefined;
        s.filterLabels[s.resourceBundle['taluk']] = s.filter.tlk ? s.filter.tlk.label + ", " + s.filter.tlk.subLabel : undefined;
        s.filterLabels[s.resourceBundle['city']] = s.filter.cty ? s.filter.cty.label + ", " + s.filter.cty.subLabel : undefined;
        if (s.showRelationFilter) {
            s.filterLabels[s.resourceBundle[s.relationFilterType]] = s.filter.lkid ? s.filter.lkid.nm : undefined;
        }
    }

    updateLabels();

    function getDataNotAvailableText() {
        if(!s.filter.st && !s.filter.dis) {
            if (checkNullEmpty(s.dstate)) {
                s.noDataText = s.resourceBundle['filter.state.missing'];
            } else if (checkNullEmpty(s.ddist)) {
                s.noDataText = s.resourceBundle['filter.district.missing'];
            } else {
                s.noDataText = s.resourceBundle['filter.taluk.missing'];
            }
        } else {
            if(s.filter.dis) {
                s.noDataText = s.resourceBundle['filter.taluk.missing'];
            } else if(s.filter.st) {
                if(s.filter.location_by == 'dis') {
                    s.noDataText = s.resourceBundle['filter.district.missing'];
                } else if (s.filter.location_by == 'tlk') {
                    s.noDataText = s.resourceBundle['filter.taluk.missing'];
                } else {
                    s.noDataText = s.resourceBundle['filter.city.missing'];
                }
            }
        }
    }

    getDataNotAvailableText();

    s.isText = function (value) {
        return typeof value === 'string' || value instanceof String;
    };

}