/**
 * Created by yuvaraj on 21/11/17.
 */

function constructPie(data, color, order, invConstants, mapEvent, ets) {

    if (checkNotNullEmpty(ets)) {
        ets.forEach(function (exStatus) {
            delete data[exStatus.id];
        });
    }

    var d = [];
    var iEntEvent = false;
    var iTempEvent = false;
    var isOneDataAvailable = false;
    var INVENTORY = invConstants;
    for (var or in order) {
        var dd = order[or];

        var o = {};
        if (dd == INVENTORY.stock.STOCKOUT) {
            o.label = "Zero stock";
            o.color = color[0];
        } else if (dd == INVENTORY.stock.UNDERSTOCK) {
            o.label = "Min";
            o.color = color[1];
        } else if (dd == INVENTORY.stock.OVERSTOCK) {
            o.label = "Max";
            o.color = color[2];
        } else if (dd == "n") {
            o.label = "Normal";
            o.color = color[3];
        } else if (dd == "a") {
            o.label = "Active";
            o.color = color[0];
            iEntEvent = true;
        } else if (dd == "i") {
            o.label = "Inactive";
            o.color = color[1];
            iEntEvent = true;
        } else if (dd == "tn") {
            o.label = "Normal";
            o.color = color[0];
            iTempEvent = true;
        } else if (dd == "tl") {
            o.label = "Low";
            o.color = color[1];
            iTempEvent = true;
        } else if (dd == "th") {
            o.label = "High";
            o.color = color[2];
            iTempEvent = true;
        } else if (dd == "tu") {
            o.label = "Unknown";
            o.color = color[3];
            iTempEvent = true;
        } else {
            o.label = dd;
        }
        if (dd == mapEvent) {
            o.isSliced = 1;
        }
        o.value = data[dd] || 0;
        if (data[dd]) {
            isOneDataAvailable = true;
        }
        d.push(o);
    }
    return isOneDataAvailable ? d : [];
};

function constructPieData(data, color, order, invConstants, mapEvent, ets) {
    if (checkNotNullEmpty(data)) {
        return constructPie(data, color, order, invConstants, mapEvent, ets);
    } else {
        return null;
    }
};

function getTotalItems(data) {
    var totalInv = 0;
    for (var d in data) {
        totalInv += data[d];
    }
    return totalInv;
};

function getItemCount(data, wt){
    if (wt == 'ia') {
        return (data['n'] + data[201] + data[202]);
    } else if (wt == 'iso') {
        return (data[200] ? data[200] : 0 );
    } else if (wt == 'in') {
        return (data['n'] ? data['n'] : 0 );
    } else if (wt == 'imin') {
        return (data[201] ? data[201] : 0 );
    } else if (wt == 'imax') {
        return (data[202] ? data[202] : 0 );
    } else if (wt == 'tn') {
        return (data['tn'] ? data['tn'] : 0);
    } else if (wt == 'tl') {
        return (data['tl'] ? data['tl'] : 0);
    } else if (wt == 'th') {
        return (data['th']? data['th'] : 0);
    } else if (wt == 'tu') {
        return (data['tu']? data['tu'] : 0);
    }
}

function getPercent(data, wt) {
    // Available = ia, Stockout = iso, Normal = in, Min = imin, or Max = imax
    if (checkNotNullEmpty(wt)) {
        if (wt == 'ia') {
            return (((data['n'] + data[201] + data[202]) / getTotalItems(data) * 100).toFixed(1)) * 1 + "%";
        } else if (wt == 'iso') {
            return ((data[200] / getTotalItems(data) * 100).toFixed(1)) * 1 + "%";
        } else if (wt == 'in') {
            return ((data['n'] / getTotalItems(data) * 100).toFixed(1)) * 1 + "%";
        } else if (wt == 'imin') {
            return ((data[201]  / getTotalItems(data) * 100).toFixed(1)) * 1 + "%";
        } else if (wt == 'imax') {
            return ((data[202]  / getTotalItems(data) * 100).toFixed(1)) * 1 + "%";
        } else if (wt == 'tn') {
            return ((data['tn']  / getTotalItems(data) * 100).toFixed(1)) * 1 + "%";
        } else if (wt == 'tl') {
            return ((data['tl'] / getTotalItems(data) * 100).toFixed(1)) * 1 + "%";
        } else if (wt == 'th') {
            return ((data['th'] / getTotalItems(data) * 100).toFixed(1)) * 1 + "%";
        } else if (wt == 'tu') {
            return ((data['tu'] / getTotalItems(data) * 100).toFixed(1)) * 1 + "%";
        }
    }
    return null;
};

function constructMapData(event, init, scope, INVENTORY, $sce, mapRange, mapColors, invPieOrder, $timeout, isloc) {
    if (scope.mapEvent == event && !init) {
        return;
    }
    scope.mloading = true;
    var subData;
    scope.caption = '';
    scope.subCaption = '';
    var iEntEvent = false;
    var iTempEvent = false;
    if (event == INVENTORY.stock.STOCKOUT) {
        subData = scope.dashboardView.inv[INVENTORY.stock.STOCKOUT];
        scope.caption = "Inventory";
        scope.subCaption = "<b>Stock:</b> Zero";
    } else if (event == INVENTORY.stock.UNDERSTOCK) {
        subData = scope.dashboardView.inv[INVENTORY.stock.UNDERSTOCK];
        scope.caption = "Inventory";
        scope.subCaption = "<b>Stock:</b> Min";
    } else if (event == INVENTORY.stock.OVERSTOCK) {
        subData = scope.dashboardView.inv[INVENTORY.stock.OVERSTOCK];
        scope.caption = "Inventory";
        scope.subCaption = "<b>Stock:</b> Max";
    } else if (event == 'n') {
        subData = scope.dashboardView.inv["n"];
        scope.caption = "Inventory";
        scope.subCaption = "<b>Stock:</b> Normal";
    } else if (event == 'a') {
        subData = scope.dashboardView.ent["a"];
        scope.caption = "Activity";
        scope.subCaption = "<b>Status: </b> Active";
        iEntEvent = true;
    } else if (event == 'i') {
        subData = scope.dashboardView.ent["i"];
        scope.caption = "Activity";
        scope.subCaption = "<b>Status: </b> Inactive";
        iEntEvent = true;
    } else if (event == 'tn') {
        subData = scope.dashboardView.temp["tn"];
        scope.caption = "Temperature";
        scope.subCaption = "<b>Status: </b> Normal";
        iTempEvent = true;
    } else if (event == 'tl') {
        subData = scope.dashboardView.temp["tl"];
        scope.caption = "Temperature";
        scope.subCaption = "<b>Status: </b> Low";
        iTempEvent = true;
    } else if (event == 'th') {
        subData = scope.dashboardView.temp["th"];
        scope.caption = "Temperature";
        scope.subCaption = "<b>Status: </b> High";
        iTempEvent = true;
    } else if (event == 'tu') {
        subData = scope.dashboardView.temp["tu"];
        scope.caption = "Temperature";
        scope.subCaption = "<b>Status: </b> Unknown";
        iTempEvent = true;
    }
    var allSubData = iEntEvent ? scope.dashboardView.ent["i"] : (iTempEvent ? subData : scope.dashboardView.inv["n"]);
    var fPeriod;
    if (!iTempEvent) {
        fPeriod = scope.period == "0" ? (iEntEvent ? scope.aper : "0") : scope.period;
    } else {
        fPeriod = scope.tperiod;
    }
    if (checkNotNullEmpty(scope.exFilter) && !iTempEvent) {
        if (scope.exType == "mTag") {
            scope.subCaption += (scope.subCaption == '' ? '' : ', ') + "<b>Material tag(s):</b> " + scope.exFilterText;
        } else if (scope.exType == "mId") {
            scope.subCaption += (scope.subCaption == '' ? '' : ', ') + "<b>Material:</b> " + scope.exFilterText;
        }
    }
    if (checkNotNullEmpty(fPeriod)) {
        if (iTempEvent) {
            if (fPeriod != 'M_0') {
                var fp = fPeriod.substr(2);
                scope.subCaption += (scope.subCaption == '' ? '' : ', ') + "<b>Period: </b>" + (fPeriod.indexOf('M') == 0 ? fp + " Minutes(s)" : (fPeriod.indexOf('H') == 0 ? fp + " Hour(s)" : fp + " Day(s)"));
            }
        } else {
            scope.subCaption += (scope.subCaption == '' ? '' : ', ') + "<b>Period:</b> " + fPeriod + " day(s)";
        }
    }
    if (checkNotNullEmpty(scope.eTag)) {
        scope.subCaption += (scope.subCaption == '' ? '' : ', ') + "<b>" + scope.resourceBundle.kiosk + " tag(s): </b>" + scope.eTagText;
    }
    if (checkNotNullEmpty(scope.asset) && iTempEvent) {
        scope.subCaption += (scope.subCaption == '' ? '' : ', ') + "<b>Asset Type:</b> ";
        var first = true;
        scope.asset.forEach(function (data) {
            if (!first) {
                scope.subCaption += ", " + data.text;
            } else {
                scope.subCaption += data.text;
                first = false;
            }
        });
    }
    scope.subCaption = $sce.trustAsHtml(scope.subCaption);


    scope.mapOpt = {
        "nullEntityColor": "#cccccc",
        "nullEntityAlpha": "50",
        "hoverOnNull": "0",
        "showLabels": "0",
        "showCanvasBorder": "0",
        "useSNameInLabels": "0",
        "toolTipSepChar": ": ",
        "legendPosition": "BOTTOM",
        "borderColor": "FFFFFF",
        //"entityBorderHoverThickness": "2",
        "interactiveLegend": 1,
        "exportEnabled": 0,
        "baseFontColor": "#000000",
        "captionFontSize": "14",
        "captionAlignment": "left",
        "legendPosition": "bottom", // we can set only bottom or right.
        "alignCaptionWithCanvas": 1,
        "labelConnectorAlpha":0,
        "captionFontBold":1,
        "captionFont":'Helvetica Neue, Arial'

    };
    var addLink = false;
    if (!scope.showSwitch) {
        var mData = [];
        var level = undefined;
        if (scope.dashboardView.mLev == "country") {
            level = "state";
            scope.mapOpt.caption = "Availability by " + level;

        } else if (scope.dashboardView.mLev == "state") {
            level = "district";
            scope.mapOpt.caption = "Availability by " + level;
        }
        for (var n in allSubData) {
            if (checkNotNullEmpty(n)) {
                var per = 0;
                var value = 0;
                var den = 0;
                if (subData != undefined && subData[n] != undefined) {
                    per = subData[n].per;
                    value = subData[n].value;
                    den = subData[n].den;
                }
                var o = {};
                var filter;
                if (scope.dashboardView.mLev == "country") {
                    if (scope.locationMapping.data[scope.dashboardView.mTy] != undefined &&
                        scope.locationMapping.data[scope.dashboardView.mTy].states[n] != undefined) {
                        o.id = scope.locationMapping.data[scope.dashboardView.mTy].states[n].id;
                    }
                    filter = n;
                } else if (scope.dashboardView.mLev == "state") {
                    if (checkNotNullEmpty(scope.locationMapping.data[scope.dashboardView.mPTy].states[scope.dashboardView.mTyNm].districts[n])) {
                        if (scope.locationMapping.data[scope.dashboardView.mPTy] != undefined &&
                            scope.locationMapping.data[scope.dashboardView.mPTy].states[scope.dashboardView.mTyNm] != undefined &&
                            scope.locationMapping.data[scope.dashboardView.mPTy].states[scope.dashboardView.mTyNm].districts[n] != undefined) {
                            o.id = scope.locationMapping.data[scope.dashboardView.mPTy].states[scope.dashboardView.mTyNm].districts[n].id;
                        }
                        filter = scope.dashboardView.mTyNm + "_" + n;
                    }
                }
                o.label = n;
                o.value = per;
                // o.displayValue = o.label + "<br/>" + per + "%";
                o.displayValue = "";
                if (iEntEvent) {
                    o.toolText = o.label + ": " + value + " of " + den + " " + scope.resourceBundle['kiosks.lower'];
                } else if (iTempEvent) {
                    o.toolText = o.label + ": " + value + " of " + den + " " + " assets";
                } else {
                    o.toolText = o.label + ": " + value + " of " + den + " " + " inventory items";
                }
                o.showLabel = "1";
                if (checkNotNullEmpty(value)) {
                    o.link = "JavaScript: angular.element(document.getElementById('cid')).scope().addFilter('" + filter + "','" + level + "')";
                }
                mData.push(o);
            }
        }
        scope.mapData = mData;
        setMapRange(event, scope, mapRange, mapColors);
        addLink = true;
    }
    var ei = invPieOrder.indexOf(scope.mapEvent);
    if (ei != -1 && (iEntEvent || iTempEvent)) {
        if (FusionCharts("id-pie1")) {
            FusionCharts("id-pie1").slicePlotItem(ei, false);
        }
    }
    scope.mapEvent = event;
    constructBarData(subData, allSubData, event, addLink, level, scope, mapRange, mapColors);
    if (!isloc && (scope.mapEvent == 'n' || scope.mapEvent == '200' || scope.mapEvent == '201' || scope.mapEvent == '202')) {
        constructMatBarData(subData['MAT_BD'].materials, allSubData['MAT_BD'].materials, event, scope, mapRange, mapColors, INVENTORY);
    } else {
        scope.matBarData = undefined;
    }
    if (!init) {
        $timeout(function () {
            scope.mloading = false;
        }, 190);
        scope.$digest();
    } else {
        $timeout(function () {
            scope.mloading = false;
        }, 1250);
    }
};

function constructBarData(data, allData, event, addLink, level, scope, mapRange, mapColors) {
    var bData = [];
    for (var f in allData) {
        if (checkNotNullEmpty(f) && f != "MAT_BD") {
            bData.push({"label": f});
        }
    }
    for (var n in allData) {
        if (checkNotNullEmpty(n) && n != "MAT_BD") {
            var per = 0;
            var value = 0;
            var den = 0;
            var kid = undefined;
            if (data != undefined && data[n] != undefined) {
                per = data[n].per;
                value = data[n].value;
                den = data[n].den;
                kid = data[n].kid;
            }
            for (var i = 0; i < bData.length; i++) {
                var bd = bData[i];
                if (n == bd.label) {
                    bd.value = per;
                    bd.displayValue = bd.value + "%";
                    if (event == '200' || event == '201' || event == '202' || event == 'n') {
                        bd.toolText = value + " of " + den + (level == undefined ? " materials" : " inventory items");
                    } else if (event == 'a' || event == 'i') {
                        bd.toolText = value + " of " + den + " " + scope.resourceBundle['kiosks.lower'];
                    } else if (event == 'tu' || event == 'tl' || event == 'th' || event == 'tn') {
                        bd.toolText = value + " of " + den + " assets";
                    }
                    for (var r = 1; r < mapRange[event].length; r++) {
                        if (per <= mapRange[event][r]) {
                            bd.color = mapColors[event][r - 1];
                            break;
                        }
                    }
                    if (addLink) {
                        var filter = bd.label;
                        if (scope.dashboardView.mLev == "state") {
                            filter = scope.dashboardView.mTyNm + "_" + bd.label;
                        }
                        bd.link = "JavaScript: angular.element(document.getElementById('cid')).scope().addFilter('" + filter + "','" + level + "')";
                    } else if ((event == '200' || event == '201' || event == '202' || event == 'n')) {
                        var search = "?eid=" + kid;
                        if (checkNotNullEmpty(scope.mtag) && scope.mtag instanceof Array) {
                            search += "&mtag=" + scope.mtag.map(function (val) {
                                    return val.text;
                                }).join(',');
                        }
                        if (event != 'n') {
                            search += "&abntype=" + event;
                            if (checkNotNullEmpty(scope.period) && scope.period != '0') {
                                search += "&dur=" + scope.period;
                            }
                        }
                        bd.link = "JavaScript: angular.element(document.getElementById('cid')).scope().drillDownInventory('" + search + "')";
                    } else if (event == 'a' || event == 'i') {
                        var fromDate = angular.copy(scope.date || scope.today);
                        fromDate.setDate(fromDate.getDate() - (scope.period || scope.aper) + (scope.date ? 1 : 0));
                        bd.link = "N-#/inventory/transactions/?eid=" + kid + "&from=" + formatDate2Url(fromDate) + "&to=" + formatDate2Url(scope.date || scope.today);
                        if (checkNotNullEmpty(scope.mtag) && scope.mtag.length == 1) {
                            bd.link += "&tag=" + scope.mtag[0].text;
                        }
                    } else if (event == 'tu' || event == 'tl' || event == 'th' || event == 'tn') {
                        var at = '&at=md';
                        if (checkNotNullEmpty(scope.assetText)) {
                            at = '&at=' + scope.assetText.join(",");
                        }
                        if (!scope.iMan) {
                            bd.link = "N-#/assets/all?eid=" + kid + "&alrm=" + (event == 'tn' ? '4' : (event == 'tu' ? '3' : '1')) + at + "&awr=1&ws=1";
                        }
                    }
                    var found = false;
                    //Arrange data in descending order
                    for (var j = 0; j < bData.length; j++) {
                        if (checkNullEmpty(bData[j].value) || bd.value > bData[j].value) {
                            bData.splice(j, 0, bd);
                            found = true;
                            break;
                        }
                    }
                    if (found) {
                        bData.splice(i + 1, 1);
                    }
                    break;
                }
            }
        }
    }
    scope.barHeight = bData.length * 20 + 80;
    scope.barData = topTen(bData);
}

function constructMatBarData(data, allData, event, scope, mapRange, mapColors, INVENTORY) {
    var bData = [];
    for (var f in allData) {
        if (checkNotNullEmpty(f)) {
            bData.push({"label": f});
        }
    }
    for (var n in allData) {
        if (checkNotNullEmpty(n)) {
            var value = 0;
            var den = 0;
            if (data != undefined && data[n] != undefined) {
                value = data[n].value;
                den = data[n].den;
            }
            function getLocationParams(links) {
                var params = '';
                if (checkNotNullEmpty(links) && links.length > 0) {
                    for (var l = 0; l < links.length; l++) {
                        if (links[l].level == 'state' && checkNotNullEmpty(links[l].text)) {
                            params += '&state=' + links[l].text;
                        } else if (links[l].level == 'district' && checkNotNullEmpty(links[l].text)) {
                            params += '&district=' + links[l].text;
                        }
                    }
                }
                return params;
            }

            for (var i = 0; i < bData.length; i++) {
                var bd = bData[i];
                if (n == bd.label) {
                    bd.value = Math.round(value / den * 1000) / 10;
                    bd.displayValue = bd.value + "%";
                    bd.toolText = value + " of " + den + " " + scope.resourceBundle['kiosks.lower'];
                    for (var r = 1; r < mapRange[event].length; r++) {
                        if (bd.value <= mapRange[event][r]) {
                            bd.color = mapColors[event][r - 1];
                            break;
                        }
                    }
                    // generate link to stock views page
                    var search;
                    if (checkNotNullEmpty(data[n].mid) && (event == INVENTORY.stock.STOCKOUT ||
                        event == INVENTORY.stock.UNDERSTOCK || event == INVENTORY.stock.OVERSTOCK)
                        && data[n].value != 0) {
                        search = "?abntype=" + event + "&mid=" + data[n].mid;
                        if (checkNotNullEmpty(scope.period) && scope.period != '0') {
                            search += "&dur=" + scope.period;
                        }
                        search += getLocationParams(scope.links);
                    } else if (event == 'n' && checkNotNullEmpty(data[n].mid) && data[n].value != 0) {
                        search = "?mid=" + data[n].mid;
                        search += getLocationParams(scope.links);
                    }
                    if (checkNotNullEmpty(search) && checkNotNullEmpty(scope.eTag) && scope.eTag instanceof Array) {
                        search += "&etag=" + scope.eTag.map(function (val) {
                                return val.text;
                            }).join(',');
                    } else if (checkNotNullEmpty(search) && checkNotNullEmpty(scope.excludeTag) && scope.excludeTag instanceof Array) {
                        search += "&eetag=" + scope.excludeTag.map(function (val) {
                                return val.text;
                            }).join(',');
                    }
                    if (checkNotNullEmpty(search)) {
                        bd.link = "JavaScript: angular.element(document.getElementById('cid')).scope().drillDownInventory('" + search + "')";
                    }
                    var found = false;
                    //Arrange data in descending order
                    for (var j = 0; j < bData.length; j++) {
                        if (checkNullEmpty(bData[j].value) || bd.value > bData[j].value) {
                            bData.splice(j, 0, bd);
                            found = true;
                            break;
                        }
                    }
                    if (found) {
                        bData.splice(i + 1, 1);
                    }
                    break;
                }
            }
        }
    }
    scope.matBarHeight = bData.length * 20 + 80;
    scope.matBarData = topTen(bData);
    scope.barData = topTen(bData);
};

function topTen(bData) {
    if (Array.isArray(bData) && bData.length >= 10) {
        return bData.slice(0, 10);
    } else {
        return bData;
    }
};

function setMapRange(event, scope, mapRange, mapColors) {
    scope.mapRange = {color: []};
    for (var i = 1; i < mapRange[event].length; i++) {
        var o = {};
        o.minvalue = mapRange[event][i - 1];
        o.maxvalue = mapRange[event][i];
        o.code = mapColors[event][i - 1];
        if (i == 1) {
            o.displayValue = "<" + mapRange[event][1] + "%"
        } else if (i == mapRange[event].length - 1) {
            o.displayValue = ">" + mapRange[event][i - 1] + "%"
        } else {
            o.displayValue = mapRange[event][i - 1] + "-" + mapRange[event][i] + "%"
        }
        scope.mapRange.color.push(o);
    }
};

function getSubtext(obj) {
    var first = true, subCaption = '';
    obj.forEach(function (data) {
        if (!first) {
            subCaption += ", " + data.text;
        } else {
            subCaption += data.text;
            first = false;
        }
    });
    return subCaption;
};

function showError(msg, scope) {
    scope.showError = true;
    if (checkNotNullEmpty(msg.data)) {
        if (checkNotNullEmpty(msg.data.message)) {
            scope.errorMessage = cleanupString(msg.data.message);
        } else {
            scope.errorMessage = cleanupString($scope.resourceBundle['general.error']);
        }
    } else if (checkNotNullEmpty(msg.message)) {
        scope.errorMessage = cleanupString(msg.message);
    } else if (msg.data == '') {
        scope.showError(msg.data);
        scope.errorMessage = cleanupString(message);
    } else {
        scope.errorMessage = cleanupString(message);
    }
};

function getReportFCCategories(data, format) {
    if (checkNotNullEmpty(data)) {
        var category = [];
        var labels = [];
        var lIndex = 0;
        var ind = 0;
        format = format || "mmm dd, yyyy";
        for (var i = data.length - 1; i >= 0; i--) {
            if (labels.indexOf(data[i].label) == -1) {
                var t = {};
                t.label = formatLabel(data[i].label, format);
                t.csvLabel = data[i].label;
                labels[lIndex++] = data[i].label;
                category[ind++] = t;
            }
        }
        return category;
    }
};

function getReportCaption(filter) {
    return "From: " + formatReportDate(filter.from, filter) + "   To: " + formatReportDate(filter.to, filter) + "   " + getFilterLabel()
};

function formatReportDate(date, filter) {
    if (filter.periodicity == "m") {
        return FormatDate_MMM_YYYY(date);
    } else {
        return FormatDate_MMM_DD_YYYY(date);
    }
};

function getFilterLabel(filterLabels) {
    var label = '';
    for (var i in filterLabels) {
        if (checkNotNullEmpty(filterLabels[i]) && i != "Periodicity") {
            label += i + ": " + filterLabels[i] + "   ";
        }
    }
    return label;
};

function getReportDateFormat(level, filter) {
    return (filter.periodicity == "m" && level == undefined) ? "mmm yyyy" : undefined;
};

function getReportFCSeries(data, seriesno, name, type, isLinkDisabled, filterSeriesIndex, showvalue, color, noAnchor, zeroWithEmpty, forceSum, skipSeriesInLabel) {
    if (checkNotNullEmpty(data) && data[0]) {
        if (data[0].value.length > seriesno) {
            var series = {};
            series.seriesName = name;
            series.renderAs = type;
            series.drawAnchors = noAnchor ? "0" : "1";
            series.anchorRadius = 0;
            series.data = [];
            var ind = 0;
            var prevLabel = undefined;
            var curLabel;
            var found = false;
            for (var i = data.length - 1; i >= 0; i--) {
                var lData = data[i];
                if (filterSeriesIndex >= 0) {
                    curLabel = lData.label;
                    if (found) {
                        if (curLabel == prevLabel) {
                            continue;
                        }
                        found = false;
                        prevLabel = undefined;
                    }

                    if (!found && ((i == 0 && lData.value[filterSeriesIndex].value != name) || (prevLabel != undefined && curLabel != prevLabel))) {
                        var dummy = {};
                        dummy.value = [];
                        dummy.value[seriesno] = {};
                        dummy.value[seriesno].value = "0";
                        dummy.label = prevLabel;
                        lData = dummy;
                        if (prevLabel != undefined) {
                            i++;
                        }
                        prevLabel = undefined;
                        found = true;
                    } else if (lData.value[filterSeriesIndex].value == name) {
                        found = true;
                        prevLabel = curLabel;
                    } else {
                        prevLabel = curLabel;
                    }
                    if (!found) {
                        continue;
                    }
                }
                var t = {};
                t.value = lData.value[seriesno].value || "0";
                if (zeroWithEmpty && (t.value == "0" || t.value == "0.0")) {
                    t.value = "";
                }
                var dec = checkNotNullEmpty(t.value) ? t.value.indexOf(".") : -1;
                if (dec >= 0) {
                    t.displayValue = roundNumber(t.value);
                }
                if (!isLinkDisabled && !(t.value == "0" || t.value == "0.0")) {
                    t.link = "JavaScript: angular.element(document.getElementById('cid')).scope().getDFChartData('" + lData.label + "');";
                }
                if (color) {
                    t.color = color;
                }
                if (name && !skipSeriesInLabel) {
                    t.toolText = "$seriesName, ";
                }
                if (forceSum || (checkNotNullEmpty(type) && (type.indexOf("Pie") == 0 || type.indexOf("Doughnut") == 0))) {
                    t.toolText = (t.toolText ? t.toolText : "" ) + "$label: $value of $unformattedSum";
                } else {
                    t.toolText = (t.toolText ? t.toolText : "" ) + "$label: " + roundNumber(lData.value[seriesno].value, 2);
                    if (lData.value[seriesno].num) {
                        t.toolText += " (" + roundNumber(lData.value[seriesno].num, 2) + " / " + roundNumber(lData.value[seriesno].den, 2) + ")";
                        t.tableTooltip = roundNumber(lData.value[seriesno].num, 2) + " / " + roundNumber(lData.value[seriesno].den, 2);
                    }
                }
                series.data[ind++] = t;
            }
            return series;
        }
    }
}

function getDonutRadius(width, height) {
    var minSide = Math.min(width, height);
    if (minSide == 2 && height == 2) {
        minSide = 1.5;
    }
    return {
        doughnutRadius: (30 * minSide),
        pieRadius: (40 * minSide)
    };
}