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

angular.module('logistimo.storyboard', ['logistimo.storyboard.bulletinboards']);

/**
 * Created by naveensnair on 15/11/17.
 */

angular.module('logistimo.storyboard.bulletinboards', ['logistimo.storyboard.dashboards'])
    .provider('bulletinBoardRepository', function () {
        return {
            $get: ['$q', function ($q) {
                return new LocalBBStorageRepository($q);
            }]
        };
    })
    .controller('BulletinBoardController', ['bulletinBoardRepository', '$scope', function (bulletinBoardRepository, $scope) {
        $scope.init = function () {
            if ($scope.bulletinBoardId != undefined) {
                bulletinBoardRepository.get($scope.bulletinBoardId, $scope).then(function (bulletinBoard) {
                    $scope.bulletinBoard = bulletinBoard;
                });
            } else {
                $scope.bulletinBoard = {dashboards: []};
            }
            $scope.selectedRow = null;
        };
        $scope.init();

        $scope.checkNotNullEmpty = function (argument) {
            return typeof argument !== 'undefined' && argument != null && argument != "";
        };

        $scope.checkNullEmpty = function (argument) {
            return !$scope.checkNotNullEmpty(argument);
        };

        $scope.addDashboard = function (dashboard) {
            var db = {id: dashboard.dbId, name: dashboard.name};
            $scope.bulletinBoard.dashboards.push(db);
        };

        $scope.setClickedRow = function (index) {
            $scope.selectedRow = index;
            $scope.bulletinBoard.dashboards[index].isChecked = true;
        };

        $scope.moveUp = function (num) {
            if (num > 0) {
                tmp = $scope.bulletinBoard.dashboards[num - 1];
                $scope.bulletinBoard.dashboards[num - 1] = $scope.bulletinBoard.dashboards[num];
                $scope.bulletinBoard.dashboards[num] = tmp;
                $scope.selectedRow--;
                $scope.bulletinBoard.dashboards[num - 1].isChecked = false;
            }
        };

        $scope.moveDown = function (num) {
            if (num < $scope.bulletinBoard.dashboards.length - 1) {
                tmp = $scope.bulletinBoard.dashboards[num + 1];
                $scope.bulletinBoard.dashboards[num + 1] = $scope.bulletinBoard.dashboards[num];
                $scope.bulletinBoard.dashboards[num] = tmp;
                $scope.selectedRow++;
                $scope.bulletinBoard.dashboards[num + 1].isChecked = false;
            }
        };

        $scope.save = function () {
            if ($scope.checkNotNullEmpty($scope.bulletinBoard)) {
                bulletinBoardRepository.save($scope.bulletinBoard, $scope);
            }
        };

        $scope.removeDashboardFromBulletinBoard = function (index) {
            $scope.bulletinBoard.dashboards.splice(index, 1);
        }
    }])
    .controller('BulletinBoardsListingController', ['bulletinBoardRepository', '$scope', function (bulletinBoardRepository, $scope) {
        $scope.init = function () {
            $scope.bulletinBoards = {};
        };
        $scope.init();

        bulletinBoardRepository.getAll($scope).then(function (bulletinBoards) {
            $scope.bulletinBoards = bulletinBoards;
        })
    }])
    .controller('BulletinBoardViewController', ['bulletinBoardRepository', '$scope','$timeout', function (bulletinBoardRepository, $scope, $timeout) {
        $scope.init = function () {
            $scope.renderDashboardsPage = false;
            if($scope.showTitle == undefined) {
                $scope.showTitle = true;
            }
            if ($scope.bulletinBoardId != undefined) {
                bulletinBoardRepository.get($scope.bulletinBoardId, $scope).then(function (bulletinBoard) {
                    $scope.bulletinBoard = bulletinBoard;
                    renderDashboards();
                });
            } else {
                $scope.bulletinBoard = {dashboards: []};
            }
            $scope.myInterval = 1000;
            $scope.noWrapSlides = false;
            $scope.activeSlide = 0;
            $scope.count = 0;
        };
        $scope.init();
        function renderDashboards() {
            $scope.dashboardId = $scope.bulletinBoard.dashboards[$scope.count].id;
            $scope.renderDashboardsPage = true;
            $timeout(function () {
                $scope.count = $scope.count + 1;
                $scope.renderDashboardsPage = false;
                if ($scope.count == $scope.bulletinBoard.dashboards.length) {
                    $scope.count = 0;
                }
                $timeout(function () {
                    renderDashboards();
                }, 10);
            }, $scope.bulletinBoard.max * 1000);
        }

        $scope.setBulletinBoardTitle = function (title, subTitle) {
            $scope.title = title;
            $scope.subTitle = subTitle;
        };

        $scope.setTitles = function(title, info) {
            $scope.setBulletinBoardTitle(title, info);
        }

    }]);




/**
 * Created by naveensnair on 18/10/17.
 */
function LocalBBStorageRepository(promise){
    var BULLETINBOARDS = "bulletinBoard";
    function readLocal(){
        if(localStorage.getItem(BULLETINBOARDS) == undefined){
            return {}
        }
        return JSON.parse(localStorage.getItem(BULLETINBOARDS));
    }
    function saveLocal(bulletinBoard){
        localStorage.setItem(BULLETINBOARDS,JSON.stringify(bulletinBoard));
    }
    return {
        get: function (bulletinBoardId) {
            return promise(function(resolve, reject){
                var bulletinBoards = readLocal();
                resolve(bulletinBoards[bulletinBoardId]);
            });

        },
        getAll: function(){
            return promise(function(resolve, reject){
                resolve(readLocal());
            });
        },
        save: function (bulletinBoard) {
            return promise(function(resolve, reject){
                if(bulletinBoard.dbId == undefined) {
                    bulletinBoard.dbId = new Date().getTime();
                }
                var bulletinBoards = readLocal();
                bulletinBoards[bulletinBoard.dbId] = bulletinBoard;
                saveLocal(bulletinBoards);
                resolve();
            });
        },
        delete: function (bulletinBoardId) {
            return promise(function(resolve, reject){
                var bulletinBoards = readLocal();
                delete bulletinBoards[bulletinBoardId];
                saveLocal(bulletinBoards);
                resolve();
            });
        }
    }
}
function DashboardLayoutHandler(containerId, $timeout, $window) {
    function getViewportHeight() {
        if ($window) {
            return $window.innerHeight;
        }
        return -1;
    }

    var ul = document.getElementById(containerId);
    var MAX_COLS_WIDTH = parseInt(getComputedStyle(ul).width, 10);
    var MAX_HEIGHT = getViewportHeight();
    var sw = MAX_COLS_WIDTH / 12;
    var sh = 110;
    var PAD = 5; // Dont Change: From UI
    var PAD2 = PAD * 2;
    var PAD3 = PAD * 3;
    var maxHeightNo = 0;
    var widgets = [];
    var MIN_WIDTH = sw * 2;
    var MIN_HEIGHT = sh * 2;
    var clicked = null;
    var onRightEdge, onBottomEdge, onLeftEdge;
    var b, x, y;
    var panel, ghostPanel;
    var ghostPanels = [], panels = [];
    var redraw = false;
    var e;
    var dProcessedPanels = [];
    var removeProcessedPanes = [];
    var processedPanels = [];

    function checkNotNullEmpty(argument) {
        return typeof argument !== 'undefined' && argument != null && argument != "";
    }

    function getLeft(newVal){
        if(newVal < PAD){
            return PAD + 'px';
        }else {
            return newVal + PAD + 'px';
        }
    }

    function getGhostLeft(newVal) {
        if (newVal < PAD) {
            return PAD + 'px';
        } else {
            return Math.round(newVal / sw) * sw + PAD + 'px';
        }
    }

    function getWidth(newVal) {
        return Math.round(newVal / sw) * sw - PAD2 + 'px';
    }


    function onMouseDown(e) {
        onDown(e);
    }

    function onDown(e) {
        calc(e);
        var isResizing = onRightEdge || onBottomEdge || onLeftEdge;
        ghostPanel.style.opacity = '0.1';
        clicked = {
            x: x,
            y: y,
            cx: e.clientX,
            cy: e.clientY,
            w: b.width,
            h: b.height,
            l: parseInt(panel.style.left, 10),
            t: parseInt(panel.style.top, 10),
            isResizing: isResizing,
            isMoving: !isResizing && canMove(),
            onRightEdge: onRightEdge,
            onLeftEdge: onLeftEdge,
            onBottomEdge: onBottomEdge
        };
        if (isResizing || clicked.isMoving) {
            document.addEventListener('mousemove', onMove);
            panel.classList.remove('dummy');
            panel.style.zIndex = '3';
            getInnerPanel(panel).classList.remove('dummy');
            getInnerMostPanel(panel).classList.remove('dummy');
        }
    }

    function onUp(e) {
        if (checkNotNullEmpty(panel)) {
            calc(e);
            if (clicked && (clicked.isResizing || clicked.isMoving)) {
                document.removeEventListener('mousemove', onMove);
            }
            panel.classList.add('dummy');
            panel.style.zIndex = '1';
            getInnerPanel(panel).classList.add('dummy');
            getInnerMostPanel(panel).classList.add('dummy');

            ghostPanel.style.opacity = '0';
            var st = {};
            st.left = ghostPanel.style.left;
            st.top = ghostPanel.style.top;
            st.width = ghostPanel.style.width;
            st.height = ghostPanel.style.height;
            getInnerPanel(panel).style.height = parseInt(st.height, 10) + 'px';
            getInnerMostPanel(panel).style.lineHeight = parseInt(st.height, 10) - 45 + 'px';
            setBounds(panel, parseInt(st.left, 10) - PAD, parseInt(st.top, 10) - PAD, parseInt(st.width, 10) + PAD2, parseInt(st.height, 10) + PAD2);
            setMaxHeight();
            clicked = null;
        }
    }

    function canMove() {
        return x > PAD && x < b.width && y > PAD && y < b.height && y < 50;
    }

    function increaseRow(wid, count) {
        wid.computedY = (wid.y + count) * sh + PAD;
        wid.y += count;
        panels[wid.wid].style.top = wid.computedY + 'px';
        ghostPanels[wid.wid].style.top = wid.computedY + PAD + 'px';
    }

    function decreaseRow(wid, count) {
        increaseRow(wid, -count);
    }

    function isIncrease(wid, t, h, l, w, recall) {
        if ((t + h > wid.computedY - PAD && t + h <= wid.computedY + wid.computedHeight + PAD) ||
            (t > wid.computedY - PAD && t < wid.computedY + wid.computedHeight + PAD && recall)) { // h increase move down - recall: because of width increase
            if (l <= wid.computedX && l + w >= wid.computedX + wid.computedWidth) { // 1 & 3 / 2 & 4,5 // check for eq in 1st cond
                return true;
            } else if ((l + w - PAD > wid.computedX && l + w < wid.computedX + wid.computedWidth) ||
                (l > wid.computedX && l < wid.computedX + wid.computedWidth - PAD)) { // 1 & 5
                return true;
            }
        }
        return false;
    }

    function addPaneEventListener(wid, time) {
        $timeout(function () {
            var pp = document.getElementById('panel_' + wid);
            pp.addEventListener('mousedown', onMouseDown);
            pp.addEventListener('mousemove', onMove);
            panels[wid] = pp;
            ghostPanels[wid] = document.getElementById('ghost_' + wid);
        }, time ? time : 1000); // Let the page render this elements to add listeners
    }

    function isBlockedPanel(sc, sx, dc, dx, isBig) {
        return (sc <= dc && sc + sx <= dc + dx && sc + sx > dc) ||
            (sc >= dc && sc + sx >= dc + dx && sc < dc + dx) ||
            (sc == dc && sc + sx == dc + dx) ||
            (dc < sc && dc + dx > sc + sx) ||
            (!isBig && (dc > sc && dc + dx < sc + sx));
    }

    function getInnerPanel(p) {
        return p.children[0].children[0].children[0];
    }

    function getInnerMostPanel(p) {
        return p.children[0].children[0].children[0].children[1].children[0].children[0];
    }

    function movePanesUp() {
        for (var d in widgets) {
            if (widgets.hasOwnProperty(d)) {
                var dd = widgets[d];
                if (removeProcessedPanes.indexOf(dd.wid) == -1) {
                    var cnt = 1;
                    while (dd.y - cnt >= 0 && !isBlocked(dd, cnt)) {
                        cnt++;
                    }
                    cnt -= 1;
                    if (cnt > 0) {
                        decreaseRow(dd, cnt);
                        removeProcessedPanes.push(dd.wid);
                        movePanesUp();
                    }
                }
            }
        }
    }

    function recalculateEdges(wid, l, t, w, h) {
        for (var x in widgets) {
            if (widgets.hasOwnProperty(x)) {
                var d = widgets[x];
                if (d.wid == wid) {
                    d.computedWidth = w;
                    d.computedHeight = h - PAD2;
                    d.computedX = l;
                    d.computedY = t + PAD;
                    d.width = d.computedWidth / sw;
                    d.height = (d.computedHeight + PAD2) / sh;
                    d.x = d.computedX / sw;
                    d.y = (d.computedY - PAD) / sh;
                    return true;
                }
            }
        }
    }


    function isBlocked(wid, cnt, skipWid) {
        for (var x in widgets) {
            if (widgets.hasOwnProperty(x)) {
                var dd = widgets[x];
                if (dd.wid != wid.wid && dd.wid != skipWid &&
                    isBlockedPanel(dd.computedX, dd.computedWidth, wid.computedX, wid.computedWidth) &&
                    ((wid.y - cnt) * sh + PAD) < dd.computedY + dd.computedHeight &&
                    ((wid.y - cnt) * sh + PAD) >= dd.computedY) {
                    return true;
                }
            }
        }
    }

    function getWidgetId(id, asInt) {
        return asInt ? parseInt(id.replace('panel_', ''), 10) : id.replace('panel_', '');
    }

    function movePanesDown(np, ngp, recall) {
        var nw = parseInt(ngp.style.width, 10) + PAD2;
        var nh = parseInt(ngp.style.height, 10) + PAD2;
        var nt = parseInt(ngp.style.top, 10) - PAD2;
        var nl = parseInt(ngp.style.left, 10) - PAD;
        var curId = getWidgetId(np.id);
        dProcessedPanels.push(curId);
        recalculateEdges(curId, nl, nt, nw, nh);
        for (var x in widgets) {
            if (widgets.hasOwnProperty(x)) {
                var d = widgets[x];
                if (dProcessedPanels.indexOf(d.wid.toString()) == -1) {
                    if (isIncrease(d, nt, nh, nl, nw, recall)) {
                        increaseRow(d, 1);
                        movePanesDown(panels[d.wid], ghostPanels[d.wid], true);
                    }
                }
            }
        }
    }

    function reArrangePanes(np, ngp, oWid, oHt, recall, rht) {
        var nw = parseInt(ngp.style.width, 10) + PAD2;
        var nh = parseInt(ngp.style.height, 10) + PAD2;
        var nt = parseInt(ngp.style.top, 10) - PAD2;
        var nl = parseInt(ngp.style.left, 10) - PAD;
        var curId = np.id.replace('panel_', '');
        processedPanels.push(curId);
        recalculateEdges(curId, nl, nt, nw, nh);
        for (var x in widgets) {
            if (widgets.hasOwnProperty(x)) {
                var d = widgets[x];
                if (processedPanels.indexOf(d.wid.toString()) == -1) {
                    var inc = false, dec = false;
                    var wd = false;
                    if (nw / sw < oWid / sw || nh / sh < oHt / sh) { // is decrease
                        dec = true;
                    } else {
                        if ((nw / sw > oWid / sw || recall) && nl + nw > d.computedX && nl < d.computedX + d.computedWidth) { // w increase move down
                            if (nt <= d.computedY && nt + nh >= d.computedY + d.computedHeight - PAD) { // inside / equal
                                inc = true;
                                wd = true;
                            } else if ((nt + nh > d.computedY && nt + nh < d.computedY + d.computedHeight - PAD) ||
                                (nt > d.computedY + PAD && nt < d.computedY + d.computedHeight + PAD)) { // outside
                                inc = true;
                                wd = true;
                            }
                        }
                        if ((nh / sh > oHt / sh || recall) && isIncrease(d, nt, nh, nl, nw, recall)) {
                            inc = true;
                        }
                        if ((nw / sw < oWid / sw || recall) && nt + nh <= d.computedY - PAD) { // w decrease move up
                            if (nt + nh == d.computedY - PAD && (nl + nw < d.computedX + PAD || nl > d.computedX + d.computedWidth - PAD)) {
                                dec = true;
                            }
                        }
                        if ((nh / sh < oHt / sh || recall) && nt + nh < d.computedY - PAD) { // h decrease move up
                            if (nl <= d.computedX && nl + nw >= d.computedX + d.computedWidth) { // 1 & 3 / 2 & 4,5
                                dec = true;
                            } else if ((nl + nw - PAD > d.computedX && nl + nw < d.computedX + d.computedWidth) ||
                                (nl > d.computedX && nl < d.computedX + d.computedWidth - PAD)) { // 1 & 5
                                dec = true;
                            }
                        }
                    }
                    if (dec) {
                        var cnt = 1;
                        while (d.y - cnt >= 0 && !isBlocked(d, cnt)) {
                            cnt++;
                        }
                        cnt -= 1;
                        if (cnt > 0) {
                            decreaseRow(d, cnt);
                            reArrangePanes(panels[d.wid], ghostPanels[d.wid], parseInt(panels[d.wid].style.width, 10),
                                parseInt(getInnerPanel(panels[d.wid]).style.height, 10) + PAD2, true, cnt);
                        }
                    } else if (inc) {
                        var ic = rht ? rht : (wd ? (nt + nh - d.computedY + PAD) / sh : 1);
                        if (ic > 1) {
                            while (ic > 0) {
                                increaseRow(d, 1);
                                dProcessedPanels = [];
                                dProcessedPanels.push(processedPanels[0]);
                                movePanesDown(panels[d.wid], ghostPanels[d.wid]);
                                ic--;
                            }
                        } else {
                            increaseRow(d, ic);
                            reArrangePanes(panels[d.wid], ghostPanels[d.wid], parseInt(panels[d.wid].style.width, 10),
                                parseInt(getInnerPanel(panels[d.wid]).style.height, 10) + PAD2, true, ic);
                        }
                    }
                }
            }
        }
    }

    function setBounds(element, x, y, w, h) {
        element.style.left = x + 'px';
        element.style.top = y + 'px';
        element.style.width = w + 'px';
        element.style.height = h + 'px';
    }

    function calc(e) {
        b = panel.getBoundingClientRect();
        x = e.clientX - b.left;
        y = e.clientY - b.top;
        onLeftEdge = x >= PAD - 3 && x <= PAD + 1;
        onRightEdge = x >= (b.width - PAD - 3) && x <= (b.width - PAD + 1);
        onBottomEdge = y >= (b.height - PAD - 3) && y <= (b.height - PAD + 1);
        /*console.log(e);
         console.log(b);*/

    }


    function onMove(ee) {
        if (ee.currentTarget.id && !clicked) {
            panel = ee.currentTarget;
            ghostPanel = ghostPanels[getWidgetId(ee.currentTarget.id)];
        }
        calc(ee);
        e = ee;
        redraw = true;
    }

    function setComputeRow(wid, row) {
        for (var d in widgets) {
            if (widgets.hasOwnProperty(d)) {
                var dw = widgets[d];
                if (dw.wid == wid) {
                    dw.y = row;
                    dw.computedY = dw.y * sh + PAD;
                    break;
                }
            }
        }
    }

    function animate() {

        requestAnimationFrame(animate);

        if (!redraw) return;

        redraw = false;
        var rearrange = false;

        if (clicked && clicked.isResizing) {
            if (clicked.onRightEdge) {
                if (clicked.l + clicked.w + e.clientX - clicked.cx <= MAX_COLS_WIDTH) { // Right most edge
                    panel.style.width = Math.max(x + PAD, MIN_WIDTH) + 'px';
                }
            }
            if (clicked.onBottomEdge) {
                getInnerPanel(panel).style.height = Math.max(y - PAD2, MIN_HEIGHT - PAD2) + 'px';
                getInnerMostPanel(panel).style.lineHeight = Math.max(y - PAD2 - 45, MIN_HEIGHT - PAD2 - 45) + 'px';
                panel.style.height = Math.max(y + PAD2, MIN_HEIGHT + PAD2) + 'px';
            }
            if (clicked.onLeftEdge) {
                if (clicked.l + e.clientX - clicked.cx >= 0) { // Left most edge
                    var currentWidth = Math.max(clicked.cx - e.clientX + clicked.w, MIN_WIDTH);
                    if (currentWidth > MIN_WIDTH) {
                        panel.style.width = currentWidth + 'px';
                        panel.style.left = getLeft(clicked.l + e.clientX - clicked.cx);
                    }
                }
            }

            var curWid = parseInt(ghostPanel.style.width, 10) + PAD2;
            var curHt = parseInt(ghostPanel.style.height, 10) + PAD2;
            var gw = parseInt(ghostPanel.style.width, 10);
            var pw = parseInt(panel.style.width, 10);

            if (gw - pw >= sw * 0.65) { // w dec
                ghostPanel.style.width = getWidth(gw - sw);
                if (clicked.onLeftEdge) {
                    ghostPanel.style.left = getGhostLeft(parseInt(ghostPanel.style.left, 10) + sw);
                }
                rearrange = true;
            } else if (pw - gw >= sw * 0.35) { // w inc
                ghostPanel.style.width = getWidth(gw + sw);
                if (clicked.onLeftEdge) {
                    ghostPanel.style.left = getGhostLeft(parseInt(ghostPanel.style.left, 10) - sw);
                }
                rearrange = true;
            }
            var gh = parseInt(ghostPanel.style.height, 10);
            var ph = parseInt(panel.style.height, 10);
            if (gh - ph >= sh * 0.65) { // h dec
                ghostPanel.style.height = gh - sh + 'px';
                rearrange = true;
            } else if (ph - gh >= sh * 0.35) { // h inc
                ghostPanel.style.height = gh + sh + 'px';
                rearrange = true;
            }
        }
        if (clicked && clicked.isMoving) {
            if (clicked.t + e.clientY - clicked.cy >= 0) {
                panel.style.top = (clicked.t + e.clientY - clicked.cy) + 'px';
            }
            if (clicked.l + e.clientX - clicked.cx >= 0 &&
                clicked.l + e.clientX - clicked.cx + clicked.w <= MAX_COLS_WIDTH) {
                panel.style.left = getLeft(clicked.l + e.clientX - clicked.cx);
            }

            curWid = parseInt(ghostPanel.style.width, 10);
            curHt = parseInt(ghostPanel.style.height, 10);
            var gl = parseInt(ghostPanel.style.left, 10);
            var pl = parseInt(panel.style.left, 10);
            if (gl - pl >= sw * 0.3) { // moving left
                ghostPanel.style.left = getGhostLeft(gl - sw);
                panel.style.width = curWid + 'px';
                rearrange = true;
            } else if (pl - gl >= sw * 0.3) { // moving right
                ghostPanel.style.left = getGhostLeft(gl + sw);
                panel.style.left = getLeft(parseInt(panel.style.left, 10));
                rearrange = true;
            }
            var gt = parseInt(ghostPanel.style.top, 10);
            var pt = parseInt(panel.style.top, 10);
            pw = parseInt(panel.style.width, 10);
            if (pt <= sh * 0.3) {
                ghostPanel.style.top = PAD2 + 'px';
                panel.style.height = parseInt(clicked.h, 10) + PAD + 'px';
                rearrange = true;
            } else if (gt - pt > 0) { //moving up
                for (var widget in widgets) {
                    if (widgets.hasOwnProperty(widget)) {
                        var dd = widgets[widget];
                        if (dd.wid.toString() != getWidgetId(panel.id) &&
                            pt < clicked.t - sh && pt - (dd.computedY + dd.computedHeight) > 0 && pt - (dd.computedY + dd.computedHeight) <= sh * 0.3 &&
                            isBlockedPanel(dd.computedX, dd.computedWidth, clicked.l, pw)) {
                            ghostPanel.style.top = dd.computedY + dd.computedHeight + PAD3 + 'px';
                            panel.style.height = parseInt(clicked.h, 10) + PAD + 'px';
                            rearrange = true;
                            break;
                        }
                    }
                }
            } else if (pt - gt > 0) { // moving down
                var curPid = getWidgetId(panel.id);
                for (widget in widgets) {
                    if (widgets.hasOwnProperty(widget)) {
                        dd = widgets[widget];
                        if (dd.wid.toString() != curPid &&
                            gt + curHt - PAD == dd.computedY && pt - dd.computedY > sh * 0.3 && pt - dd.computedY < sh) {
                            if (dd.computedX >= clicked.l && dd.computedX + dd.computedWidth <= clicked.l + pw && pt - clicked.t > dd.computedHeight) { //Top: big, Bottom: small
                                decreaseRow(dd, clicked.h / sh);
                                ghostPanel.style.top = dd.computedY + dd.computedHeight + PAD3 + 'px';
                                setComputeRow(parseInt(curPid, 10), (dd.computedY + dd.computedHeight + PAD) / sh);
                                rearrange = true;
                            } else if (gl >= dd.computedX && gl + curWid <= dd.computedX + dd.computedWidth) { //Top: small, Bottom: big / both Equal
                                cnt = 1;
                                while (dd.y - cnt >= 0 && !isBlocked(dd, cnt, parseInt(curPid, 10))) {
                                    cnt++;
                                }
                                cnt -= 1;
                                if (cnt > 0) {
                                    decreaseRow(dd, cnt);
                                    reArrangePanes(panels[dd.wid], ghostPanels[dd.wid], dd.computedWidth, dd.computedHeight, false, cnt);
                                }
                                ghostPanel.style.top = dd.computedY + dd.computedHeight + PAD3 + 'px';
                                setComputeRow(parseInt(curPid, 10), (dd.computedY + dd.computedHeight + PAD) / sh);
                            } else if (isBlockedPanel(dd.computedX, dd.computedWidth, gl, curWid, true)) { // Top: big, Bottom: small
                                cnt = 1;
                                while (dd.y - cnt >= 0 && !isBlocked(dd, cnt, parseInt(curPid, 10))) {
                                    cnt++;
                                }
                                cnt -= 1;
                                if (cnt >= dd.height) {
                                    decreaseRow(dd, cnt);
                                    if (dd.computedY + dd.computedHeight > gt) {
                                        ghostPanel.style.top = dd.computedY + dd.computedHeight + PAD3 + 'px';
                                        setComputeRow(parseInt(curPid, 10), (dd.computedY + dd.computedHeight + PAD) / sh);
                                    }
                                } else if (gt + curHt - PAD > dd.computedY) {
                                    var ic = clicked.h / sh;
                                    while (ic > 0) {
                                        increaseRow(dd, 1);
                                        dProcessedPanels = [];
                                        dProcessedPanels.push(parseInt(curPid, 10));
                                        movePanesDown(panels[dd.wid], ghostPanels[dd.wid]);
                                        ic--;
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        if (rearrange) {
            processedPanels = [];
            reArrangePanes(panel, ghostPanel, curWid, curHt, clicked.isMoving);
            var d = undefined;
            var curId = getWidgetId(panel.id);

            for (widget in widgets) {
                if (widgets.hasOwnProperty(widget)) {
                    var dw = widgets[widget];
                    if (dw.wid.toString() == curId) {
                        d = dw;
                        break;
                    }
                }
            }
            if (d != undefined) {
                var cnt = 1;
                while (d.y - cnt >= 0 && !isBlocked(d, cnt)) {
                    cnt++;
                }
                cnt -= 1;
                if (cnt > 0) {
                    decreaseRow(d, cnt);
                    reArrangePanes(panel, ghostPanel, parseInt(panel.style.width, 10),
                        parseInt(getInnerPanel(panel).style.height, 10) + PAD2, false, cnt);
                }
            }
            return;
        }

        if (onRightEdge && onBottomEdge) {
            panel.style.cursor = 'nwse-resize';
        } else if (onBottomEdge && onLeftEdge) {
            panel.style.cursor = 'nesw-resize';
        } else if (onRightEdge || onLeftEdge) {
            panel.style.cursor = 'ew-resize';
        } else if (onBottomEdge) {
            panel.style.cursor = 'ns-resize';
        } else if (canMove()) {
            panel.style.cursor = 'move';
        } else {
            panel.style.cursor = 'default';
        }
    }

    function getMaxHeightGridNumber() {
        var maxHeightNo = 0;
        for (var k in widgets) {
            if (widgets.hasOwnProperty(k)) {
                var dw = widgets[k];
                if (maxHeightNo < dw.y + dw.height) {
                    maxHeightNo = dw.y + dw.height;
                }
            }
        }
        return maxHeightNo;
    }

    function computeSh() {
        if (MAX_HEIGHT > 360 && getMaxHeightGridNumber() > 4) {
            sh = (MAX_HEIGHT - 80) / getMaxHeightGridNumber();
        }
    }

    function setMaxHeight() {
        var maxHeight = 100;
        maxHeightNo = 0;
        for (var k in widgets) {
            if (widgets.hasOwnProperty(k)) {
                var dw = widgets[k];
                if (maxHeight < dw.computedY + dw.computedHeight) {
                    maxHeight = dw.computedY + dw.computedHeight;
                    maxHeightNo = dw.y + dw.height;
                }
            }
        }
        ul.style.minHeight = maxHeight - PAD + 'px';
    }


    return {
        setWidgets: function (newWidgets) {
            widgets = newWidgets;
        },
        addWidget: function(widget) {
            var newWid = {};
            newWid.id = widget.name.replace(/\s/g,'')+new Date().getMilliseconds();
            newWid.name = widget.name;
            newWid.wid = newWid.id;
            newWid.widgetTemplateId = widget.id;
            newWid.x = 0;
            newWid.width = checkNotNullEmpty(widget.defaultWidth) ? widget.defaultWidth : 4;
            newWid.height = checkNotNullEmpty(widget.defaultHeight) ? widget.defaultHeight : 3;
            newWid.y = maxHeightNo;
            this.addComputeData(newWid);
            widgets[newWid.id] = newWid;
            this.setMaxHeight();
            addPaneEventListener(newWid.id);
            movePanesUp();
        },
        setMaxHeight: setMaxHeight,
        addComputeData: function (wid) {
            var maxHeight = 100;
            if (wid) {
                wid.computedWidth = wid.width * sw;
                wid.computedX = wid.x * sw;
                wid.computedHeight = wid.height * sh - PAD2;
                wid.computedY = wid.y * sh + PAD;
            } else {
                computeSh();
                for (var k in widgets) {
                    if (widgets.hasOwnProperty(k)) {
                        wid = widgets[k];
                        wid.computedWidth = wid.width * sw;
                        wid.computedX = wid.x * sw;
                        wid.computedHeight = wid.height * sh - PAD2;
                        wid.computedY = wid.y * sh + PAD;
                        if (maxHeight < wid.computedY + wid.computedHeight) {
                            maxHeight = wid.computedY + wid.computedHeight;
                        }
                    }
                }
            }
            return maxHeight - PAD + 'px';
        },
        initEventListeners: function () {
            $timeout(function () {
                for (var k in widgets) {
                    if (widgets.hasOwnProperty(k)) {
                        addPaneEventListener(widgets[k].id, 1);
                    }
                }
                document.addEventListener('mouseup', onUp);
                animate();
            }, 1000); // Let the page render all elements to add listeners
            $timeout(function () {
                setMaxHeight();
            },1000);
        },
        removeWidget: function(widgetId) {
            delete widgets[widgetId];
            movePanesUp();
            setMaxHeight();
        }
    }

}
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

/**
 * Created by naveensnair on 15/11/17.
 */

angular.module('logistimo.storyboard.dashboards', ['logistimo.storyboard.widgets'])
    .provider('dashboardRepository', function () {
        return {
            $get: ['$q', function ($q) {
                return new LocalStorageRepository($q);
            }]
        };
    })
    .controller('DashboardViewController', ['dashboardRepository', '$scope', '$window', function (dashboardRepository, $scope, $window) {

        function init() {
            function initDashboardLayout() {
                var dashboardLayoutHandler = new DashboardLayoutHandler("allWid", null, $window);
                dashboardLayoutHandler.setWidgets($scope.db.widgets);
                $scope.maxHeight = dashboardLayoutHandler.addComputeData();
                dashboardLayoutHandler.setMaxHeight();
                $scope.renderingStatus = {};
            }

            if (!$scope.noInit) {
                dashboardRepository.get($scope.dashboardId, $scope).then(function (dashboard) {
                    $scope.db = dashboard;
                    if (!$scope.showTitle) {
                        $scope.setTitles($scope.db.title, $scope.db.info);
                    }
                    if ($scope.db == null || $scope.db == "") {
                        $scope.db = {
                            id: $scope.dashboardId,
                            widgets: {}
                        };
                    }
                    initDashboardLayout();
                });
            } else {
                initDashboardLayout();
            }
        }

        init();

        $scope.completeRendering = function (widgetId) {
            $scope.renderingStatus[widgetId] = true;
            //check if all widgets have signalled completion and then emit so bulletin board can switch dashboard.
        }

    }])
    .controller('DashboardConfigureController', ['dashboardRepository', '$scope', '$timeout', function (dashboardRepository, $scope, $timeout) {

        var dashboardLayoutHandler = new DashboardLayoutHandler("allWid", $timeout);
        $scope.init = function () {
            if ($scope.dashboard != undefined) {
                $scope.widgets = $scope.dashboard.widgets;
            } else {
                $scope.widgets = {};
            }
            dashboardLayoutHandler.setWidgets($scope.widgets);
            dashboardLayoutHandler.addComputeData();
            dashboardLayoutHandler.initEventListeners();
            dashboardLayoutHandler.setMaxHeight();
        };
        $scope.init();

        $scope.checkNotNullEmpty = function (argument) {
            return typeof argument !== 'undefined' && argument != null && argument != "";
        };

        $scope.checkNullEmpty = function (argument) {
            return !$scope.checkNotNullEmpty(argument);
        };


        $scope.addWidget = function (widget) {
            if ($scope.checkNotNullEmpty(widget)) {
                dashboardLayoutHandler.addWidget(widget);
            }
        };

        $scope.removeWidget = function (wid) {
            if (!confirm("Do you want to remove this widget from dashboard?")) {
                return;
            }
            dashboardLayoutHandler.removeWidget(wid.id);
        };
        $scope.completeRendering = function (widgetId) {
            console.log("rendered widget" + widgetId);
        };

        $scope.saveWidgetConf = function (widgetId, widgetConfig) {
            if ($scope.widgets[widgetId] == null) {
                $scope.widgets[widgetId] = {};
            }
            $scope.widgets[widgetId].conf = widgetConfig;
        };

        $scope.save = function () {
            $scope.saveDashboard($scope.widgets);
        };

        $scope.previewChange = function () {
            $scope.previewDashboard($scope.widgets);
        }

    }])
    .controller('DashboardListingController', ['dashboardRepository', '$scope', function (dashboardRepository, $scope) {
        $scope.dashboards = {};

        dashboardRepository.getAll($scope).then(function (dashboards) {
            $scope.dashboards = dashboards;
        });

        $scope.checkNotNullEmpty = function (argument) {
            return typeof argument !== 'undefined' && argument != null && argument != "";
        };

        $scope.checkNullEmpty = function (argument) {
            return !$scope.checkNotNullEmpty(argument);
        };
    }])
    .controller('DashboardController', ['dashboardRepository', '$scope', function (dashboardRepository, $scope) {
        $scope.init = function () {
            $scope.renderDashboardConfigurePage = false;
            if ($scope.dashboardId != undefined) {
                dashboardRepository.get($scope.dashboardId, $scope).then(function (dashboard) {
                    $scope.dashboard = dashboard;
                    $scope.renderDashboardConfigurePage = true;
                });
            } else {
                $scope.dashboard = {widgets: {}};
                $scope.renderDashboardConfigurePage = true;
            }
            $scope.preview = false;
        };
        $scope.init();

        $scope.isUndef = function (value) {
            return (value == undefined || value == '');
        };

        $scope.isDef = function (value) {
            return !$scope.isUndef(value);
        };
        $scope.saveDashboard = function (widgets) {
            $scope.dashboard.widgets = widgets;
            dashboardRepository.save($scope.dashboard, $scope).then(function (dashboard) {
                $scope.widgets = {};
                $scope.dashboard = {};
            });
        };

        $scope.previewDashboard = function (widgets) {
            $scope.preview = !$scope.preview;
            $scope.temp_dashboard = angular.copy($scope.dashboard);
            if ($scope.temp_dashboard == undefined) {
                $scope.temp_dashboard = {};
            }
            $scope.temp_dashboard.widgets = widgets;
        }

    }]);





/**
 * Created by naveensnair on 18/10/17.
 */
function LocalStorageRepository(promise){
    var DASHBOARDS = "dashboards";
    function readLocal(){
        if(localStorage.getItem(DASHBOARDS) == undefined){
            return {}
        }
        return JSON.parse(localStorage.getItem(DASHBOARDS));
    }
    function saveLocal(dashboards){
        localStorage.setItem(DASHBOARDS,JSON.stringify(dashboards));
    }
    return {
        get: function (dashboardId) {
            return promise(function(resolve, reject){
                var dashboards = readLocal();
                resolve(dashboards[dashboardId]);
            });

        },
        getAll: function(){
            return promise(function(resolve, reject){
                resolve(readLocal());
            });
        },
        save: function (dashboard) {
            return promise(function(resolve, reject){
                if(dashboard.dbId == undefined) {
                    dashboard.dbId = new Date().getTime();
                }
                var dashboards = readLocal();
                dashboards[dashboard.dbId] = dashboard;
                saveLocal(dashboards);
                resolve();
            });
        },
        delete: function (dashboardId) {
            return promise(function(resolve, reject){
                var dashboards = readLocal();
                delete dashboards[dashboardId];
                saveLocal(dashboards);
                resolve();
            });
        }
    }
}
/**
 * Created by naveensnair on 18/10/17.
 */
angular.module('logistimo.storyboard.widgets', [])
    .provider('widgetsRepository', function () {
        return {
            repository: new WidgetRegistry(),
            addWidget: function (widget) {
                this.repository.register(widget);
            },
            $get: function () {
                return this.repository;
            }
        };
    })
    .controller('WidgetsViewController', ['widgetsRepository', '$scope', function (widgetsRepository, $scope) {
        $scope.widgetTemplate = widgetsRepository.get($scope.widget.widgetTemplateId);
    }])
    .controller('WidgetsConfigureController', ['widgetsRepository', '$scope', function (widgetsRepository, $scope) {
        $scope.widgetTemplate = widgetsRepository.get($scope.widget.widgetTemplateId);

        $scope.checkNotNullEmpty = function (argument) {
            return typeof argument !== 'undefined' && argument != null && argument != "";
        };

        $scope.checkNullEmpty = function (argument) {
            return !$scope.checkNotNullEmpty(argument);
        };

        if(!$scope.checkNotNullEmpty($scope.widget.conf)){
            $scope.widget.conf = {};
        }
        $scope.save = function () {
            $scope.saveWidgetConf($scope.id, $scope.widget.conf);
            $scope.isEdit = false;
        };
        $scope.cancel = function(){
            $scope.isEdit = false;
        };
    }])
    .controller('WidgetScopeController', [function () {
        //Changes scope
    }])
    .controller('WidgetsListingController', ['widgetsRepository', '$scope', function (widgetRepository, $scope) {
        $scope.widgets = widgetRepository.getAll();
    }])
    .controller('StaticWidgetController', ['$scope', function($scope) {
        //$scope.completeRendering($scope.id);
    }]);




/**
 * Created by naveensnair on 18/10/17.
 */
function WidgetRegistry(){
    var widget = "widget";
    return {
        widgets : {},
        get: function (widgetId) {
            return this.widgets[widgetId];
        },
        getAll: function(){
            return this.widgets;
        },
        register: function (widget) {
            this.widgets[widget.id] = widget;
        }
    }
}
angular.module('logistimo.storyboard').run(['$templateCache', function($templateCache) {  'use strict';

    $templateCache.put('/angular-storyboards/src/bulletinboard/templates/create-bulletin-board.html',
        "<div ng-controller=\"BulletinBoardController\">\n" +
        "    <div class=\"box topbox\">\n" +
        "        <div class=\"bizinfo bizinfo-last\">\n" +
        "            <form class=\"form-horizontal\" name=\"Bulletinboard\">\n" +
        "                <div class=\"bgr\">\n" +
        "                    <div class=\"title-heading\">\n" +
        "                        {{resourceBundle['bulletinboard']}}\n" +
        "                    </div>\n" +
        "                    <div class=\"row\">\n" +
        "                        <div class=\"col-sm-6\">\n" +
        "                            <div class=\"form-group\">\n" +
        "                                <label class=\"col-sm-4 control-label required\">{{resourceBundle['name']}}</label>\n" +
        "                                <div class=\"col-sm-6\">\n" +
        "                                    <input type=\"text\" class=\"form-control\" placeholder=\"{{resourceBundle['name']}}\" ng-model=\"bulletinBoard.name\" maxlength=\"50\">\n" +
        "                                </div>\n" +
        "                            </div>\n" +
        "                        </div>\n" +
        "                    </div>\n" +
        "                    <div class=\"row\">\n" +
        "                        <div class=\"col-sm-6\">\n" +
        "                            <div class=\"form-group\">\n" +
        "                                <label class=\"col-sm-4 control-label\">{{resourceBundle['description']}}</label>\n" +
        "                                <div class=\"col-sm-6\">\n" +
        "                                    <input type=\"text\" class=\"form-control\" placeholder=\"{{resourceBundle['description']}}\" ng-model=\"bulletinBoard.desc\">\n" +
        "                                </div>\n" +
        "                            </div>\n" +
        "                        </div>\n" +
        "                    </div>\n" +
        "                    <div class=\"row\">\n" +
        "                        <div class=\"col-sm-6\">\n" +
        "                            <div class=\"form-group\">\n" +
        "                                <label class=\"col-sm-4 control-label required\">{{resourceBundle['min.display.time']}}</label>\n" +
        "                                <div class=\"col-sm-6\">\n" +
        "                                    <input type=\"text\" class=\"form-control\" placeholder=\"{{resourceBundle['time.in.seconds']}}\" ng-model=\"bulletinBoard.min\">\n" +
        "                                </div>\n" +
        "                            </div>\n" +
        "                        </div>\n" +
        "                    </div>\n" +
        "                    <div class=\"row\">\n" +
        "                        <div class=\"col-sm-6\">\n" +
        "                            <div class=\"form-group\">\n" +
        "                                <label class=\"col-sm-4 control-label required\">{{resourceBundle['max.display.time']}}</label>\n" +
        "                                <div class=\"col-sm-6\">\n" +
        "                                    <input type=\"text\" class=\"form-control\" placeholder=\"{{resourceBundle['time.in.seconds']}}\" ng-model=\"bulletinBoard.max\">\n" +
        "                                </div>\n" +
        "                            </div>\n" +
        "                        </div>\n" +
        "                    </div>\n" +
        "                </div>\n" +
        "                <div class=\"bgr\">\n" +
        "                    <div class=\"title-heading\">\n" +
        "                        {{resourceBundle['dashboards']}}\n" +
        "                    </div>\n" +
        "                    <div class=\"row\">\n" +
        "                        <div class=\"col-sm-6\"></div>\n" +
        "                        <div class=\"col-sm-6\">\n" +
        "                            <div ng-include=\" '/angular-storyboards/src/bulletinboard/templates/dashboards.html' \"></div>\n" +
        "                        </div>\n" +
        "                    </div>\n" +
        "                    <div class=\"row\">\n" +
        "                        <div class=\"col-sm-6\" ng-show=\"bulletinBoard.dashboards.length > 0\">\n" +
        "                            <table class=\"table table-bordered\" ng-keydown=\"key($event)\">\n" +
        "                                <thead>\n" +
        "                                <tr>\n" +
        "                                    <th class=\"col-sm-1\">{{resourceBundle['s.no']}}</th>\n" +
        "                                    <th class=\"col-sm-2\">{{resourceBundle['dashboard']}}</th>\n" +
        "                                    <th class=\"col-sm-1\">{{resourceBundle['action']}}</th>\n" +
        "                                </tr>\n" +
        "                                </thead>\n" +
        "                                <tbody>\n" +
        "                                <tr ng-repeat=\"(id, dashboard) in bulletinBoard.dashboards track by $index\"\n" +
        "                                    ng-class=\"{'selected':$index == selectedRow}\">\n" +
        "                                    <td><input type=\"checkbox\" ng-click=\"setClickedRow($index)\"\n" +
        "                                               ng-model=\"dashboard.isChecked\"></td>\n" +
        "                                    <td>{{dashboard.name}}</td>\n" +
        "                                    <td>\n" +
        "                                        <span class=\"glyphicon glyphicon-trash\" ng-click=\"removeDashboardFromBulletinBoard($index)\" title=\"{{resourceBundle['remove']}}\"></span>\n" +
        "                                    </td>\n" +
        "                                </tr>\n" +
        "                                </tbody>\n" +
        "                            </table>\n" +
        "                            <button type=\"button\" ng-click=\"moveUp(selectedRow)\" class=\"btn btn-sm btn-primary\"\n" +
        "                                    ng-show=\"checkNotNullEmpty(bulletinBoard.dashboards)\">{{resourceBundle['move.up']}}\n" +
        "                            </button>\n" +
        "                            <button type=\"button\" ng-click=\"moveDown(selectedRow)\" class=\"btn btn-sm btn-primary\"\n" +
        "                                    ng-show=\"checkNotNullEmpty(bulletinBoard.dashboards)\">{{resourceBundle['move.down']}}\n" +
        "                            </button>\n" +
        "                        </div>\n" +
        "                    </div>\n" +
        "                </div>\n" +
        "                <div class=\"row\" id=\"sbt\">\n" +
        "                    <div class=\"col-sm-6\">\n" +
        "                        <div class=\"pull-right\">\n" +
        "                            <button type=\"button\" ng-click=\"save()\" class=\"btn btn-primary\">{{resourceBundle['save']}}</button>\n" +
        "                        </div>\n" +
        "                    </div>\n" +
        "                </div>\n" +
        "            </form>\n" +
        "        </div>\n" +
        "    </div>\n" +
        "</div>"
    );


    $templateCache.put('/angular-storyboards/src/bulletinboard/templates/dashboards-render.html',
        "<div ng-controller=\"RenderDashboardsController\">\n" +
        "    <div class=\"row mt18\">\n" +
        "        <div class=\"col-sm-12\">\n" +
        "            <h2 style=\"text-align: center;\">{{dashboard.title}}</h2>\n" +
        "        </div>\n" +
        "    </div>\n" +
        "    <div class=\"row mt18\">\n" +
        "        <h3 style=\"text-align: center;\">{{dashboard.info}}</h3>\n" +
        "    </div>\n" +
        "    <div class=\"row padding5\" id=\"viewDash\">\n" +
        "        <ul\n" +
        "                style=\"list-style-type: none; padding: 0; display: block; margin: 0; position: relative; min-height: {{maxHeight}}\"\n" +
        "                id=\"allWid\">\n" +
        "            <li class=\"dummy noLRpad\"\n" +
        "                style=\"z-index: 1; position: absolute; padding:5px; display:list-item; width: {{widget.computedWidth}}px; left: {{widget.computedX}}px; top: {{widget.computedY}}px; height: {{widget.computedHeight + 10}}px;\"\n" +
        "                ng-controller=\"WidgetScopeController\" ng-repeat=\"(id,widget) in dashboard.widgets\">\n" +
        "                <span ng-include=\"'/angular-storyboards/src/widget/templates/view-widget.html'\"></span>\n" +
        "            </li>\n" +
        "        </ul>\n" +
        "    </div>\n" +
        "</div>"
    );


    $templateCache.put('/angular-storyboards/src/bulletinboard/templates/dashboards.html',
        "<div ng-controller=\"DashboardListingController\">\n" +
        "    <div class=\"row\">\n" +
        "        <div class=\"col-sm-12\">\n" +
        "            <button type=\"button\" class=\"btn btn-primary pull-right\" data-toggle=\"modal\" data-target=\"#myModal\">{{resourceBundle['prm.add']}}\n" +
        "                {{resourceBundle['dashboards.lower']}}\n" +
        "            </button>\n" +
        "        </div>\n" +
        "    </div>\n" +
        "    <div class=\"modal fade\" id=\"myModal\" role=\"dialog\">\n" +
        "        <div class=\"modal-dialog\">\n" +
        "            <!-- Modal content-->\n" +
        "            <div class=\"modal-content\">\n" +
        "                <div class=\"modal-header ws\">\n" +
        "                    <button type=\"button\" class=\"close\" data-dismiss=\"modal\">&times;</button>\n" +
        "                    <h4 class=\"modal-title\">{{resourceBundle['dashboards']}}</h4>\n" +
        "                </div>\n" +
        "                <div class=\"modal-body ws\">\n" +
        "                    <ul class=\"list-group\">\n" +
        "                        <li class=\"list-group-item\" ng-repeat=\"(id, dashboard) in dashboards\">\n" +
        "                            <span>{{dashboard.name}}</span>\n" +
        "                            <span class=\"btn btn-sm btn-primary pull-right\"\n" +
        "                                  ng-click=\"addDashboard(dashboard)\" style=\"position:relative;bottom:5px;\">{{resourceBundle['prm.add']}}</span>\n" +
        "                        </li>\n" +
        "                    </ul>\n" +
        "                </div>\n" +
        "                <div class=\"modal-footer ws\">\n" +
        "                    <button type=\"button\" class=\"btn btn-default\" data-dismiss=\"modal\">{{resourceBundle['close']}}</button>\n" +
        "                </div>\n" +
        "            </div>\n" +
        "        </div>\n" +
        "    </div>\n" +
        "</div>"
    );


    $templateCache.put('/angular-storyboards/src/bulletinboard/templates/list-bulletin-boards.html',
        "<div class=\"tab pane\">\n" +
        "    <div class=\"box topbox\">\n" +
        "        <div ng-controller=\"BulletinBoardsListingController\">\n" +
        "            <div>\n" +
        "                <div class=\"row\">\n" +
        "                    <div class=\"col-sm-8\">\n" +
        "                        <table class=\"table table-bordered\">\n" +
        "                            <thead>\n" +
        "                            <tr>\n" +
        "                                <th class=\"col-sm-1\">{{resourceBundle['s.no']}}</th>\n" +
        "                                <th class=\"col-sm-4\">{{resourceBundle['bulletinboard']}}</th>\n" +
        "                                <th class=\"col-sm-1\">{{resourceBundle['action']}}</th>\n" +
        "                            </tr>\n" +
        "                            </thead>\n" +
        "                            <tbody>\n" +
        "                            <tr ng-repeat=\"(id, bulletinBoard) in bulletinBoards\">\n" +
        "                                <td>{{$index + 1}}</td>\n" +
        "                                <td>{{bulletinBoard.name}}</td>\n" +
        "                                <td>\n" +
        "                                    <span class=\"glyphicon glyphicon-th-large\" ng-click=\"viewBulletinBoard(id)\" title=\"{{resourceBundle['view']}}\"></span>&nbsp;&nbsp;\n" +
        "                                    <span class=\"glyphicon glyphicon-cog\" ng-click=\"editBulletinBoard(id)\" title=\"{{resourceBundle['configure']}}\"></span>\n" +
        "                                </td>\n" +
        "                            </tr>\n" +
        "                            </tbody>\n" +
        "                        </table>\n" +
        "                    </div>\n" +
        "                </div>\n" +
        "            </div>\n" +
        "        </div>\n" +
        "    </div>\n" +
        "</div>"
    );


    $templateCache.put('/angular-storyboards/src/bulletinboard/templates/view-bulletin-board.html',
        "<div class=\"row\">\n" +
        "    <div class=\"col-sm-12\">\n" +
        "        <div ng-controller=\"BulletinBoardViewController\">\n" +
        "            <div ng-if=\"renderDashboardsPage == true\">\n" +
        "                <div ng-include=\"'/angular-storyboards/src/dashboard/templates/view-dashboard.html'\"\n" +
        "                     ng-init=\"showTitle = showTitle\"></div>\n" +
        "            </div>\n" +
        "        </div>\n" +
        "    </div>\n" +
        "</div>\n"
    );


    $templateCache.put('/angular-storyboards/src/bulletinboard/templates/view-bulletinboard.html',
        ""
    );


    $templateCache.put('/angular-storyboards/src/dashboard/templates/configure-dashboard.html',
        "<div ng-controller=\"DashboardConfigureController\">\n" +
        "    <div class=\"bgr\">\n" +
        "        <div class=\"box topbox\" style=\"min-height:500px;\">\n" +
        "            <div class=\"title-heading\">\n" +
        "                Widgets\n" +
        "            </div>\n" +
        "            <div class=\"row\">\n" +
        "                <div class=\"col-sm-6\">\n" +
        "                    <div class=\"col-sm-4\" ng-show=\"checkNullEmpty(widgets)\">\n" +
        "                        <div class=\"form-group\">\n" +
        "                            <label class=\"cbx control-label\">No widgets configured</label>\n" +
        "                        </div>\n" +
        "                    </div>\n" +
        "                </div>\n" +
        "                <div class=\"col-sm-6\">\n" +
        "                    <div ng-include=\"'/angular-storyboards/src/widget/templates/list-widgets.html'\"></div>\n" +
        "                </div>\n" +
        "            </div>\n" +
        "            <div>\n" +
        "                <div class=\"row mt18 padding5\">\n" +
        "                    <ul style=\"list-style-type: none; padding: 0; display: block; margin: 0; position: relative; min-height: {{maxHeight}}\"\n" +
        "                        id=\"allWid\">\n" +
        "                        <li class=\"dummy noLRpad\" id=\"panel_{{widget.id}}\"\n" +
        "                            style=\"z-index: 1; position: absolute; padding:5px; display:list-item; width: {{widget.computedWidth}}px; left: {{widget.computedX}}px; top: {{widget.computedY}}px;\"\n" +
        "                            ng-repeat=\"(id,widget) in widgets\">\n" +
        "                            <span ng-include=\"'/angular-storyboards/src/widget/templates/view-widget-template.html'\"></span>\n" +
        "                        </li>\n" +
        "                <span class=\"noLRpad\" id=\"ghost_{{widget.id}}\"\n" +
        "                      style=\"z-index: 0; position:absolute; padding:5px; width: {{widget.computedWidth - 10}}px; left: {{widget.computedX + 5}}px; top: {{widget.computedY + 5}}px; height: {{widget.computedHeight}}px;\"\n" +
        "                      ng-repeat=\"(id,widget) in widgets\">\n" +
        "                </span>\n" +
        "                    </ul>\n" +
        "                    <div class=\"modal fade\" id=\"editWidgetModal{{widget.id}}\" role=\"dialog\" ng-repeat=\"(id,widget) in widgets\">\n" +
        "                        <div class=\"modal-dialog\">\n" +
        "                            <!-- Modal content-->\n" +
        "                            <div class=\"modal-content\" ng-controller=\"WidgetsConfigureController\">\n" +
        "                                <div class=\"modal-header ws\">\n" +
        "                                    <button type=\"button\" class=\"close\" data-dismiss=\"modal\">&times;</button>\n" +
        "                                    <h4 class=\"modal-title\">{{resourceBundle['widgets.upper']}}</h4>\n" +
        "                                </div>\n" +
        "                                <div class=\"modal-body\" style=\"background-color: white;\">\n" +
        "                                    <div ng-include=\"widgetTemplate.editTemplateUrl\"></div>\n" +
        "                                </div>\n" +
        "                                <div class=\"modal-footer ws\">\n" +
        "\n" +
        "                                    <button class=\"btn btn-primary\" ng-click=\"save()\" data-dismiss=\"modal\">{{resourceBundle['save']}}</button>\n" +
        "                                    <button class=\"btn btn-default\" ng-click=\"cancel()\" data-dismiss=\"modal\">{{resourceBundle['cancel']}}</button>\n" +
        "\n" +
        "                                </div>\n" +
        "                            </div>\n" +
        "                        </div>\n" +
        "                    </div>\n" +
        "                </div>\n" +
        "            </div>\n" +
        "        </div>\n" +
        "    </div>\n" +
        "    <div class=\"row\" id=\"sbt\">\n" +
        "        <div class=\"col-sm-6\">\n" +
        "            <div class=\"pull-right\" ng-show=\"!edit\">\n" +
        "                <button type=\"button\" ng-click=\"save()\" class=\"btn btn-primary\">Save</button>\n" +
        "            </div>\n" +
        "            <div class=\"pull-right\" ng-show=\"edit\">\n" +
        "                <button type=\"button\" ng-click=\"save()\" class=\"btn btn-primary\">Update</button>\n" +
        "            </div>\n" +
        "            <div class=\"pull-right\" style=\"padding-right:10px;\" ng-show=\"!preview\">\n" +
        "                <button type=\"button\" ng-click=\"previewChange()\" class=\"btn btn-primary\">Preview</button>\n" +
        "            </div>\n" +
        "            <div class=\"pull-right\" style=\"padding-right:10px;\" ng-show=\"preview\">\n" +
        "                <button type=\"button\" ng-click=\"previewChange()\" class=\"btn btn-primary\">Exit preview</button>\n" +
        "            </div>\n" +
        "        </div>\n" +
        "    </div>\n" +
        "</div>"
    );


    $templateCache.put('/angular-storyboards/src/dashboard/templates/create-dashboard.html',
        "<div ng-controller=\"DashboardController\">\n" +
        "    <div class=\"box topbox\">\n" +
        "        <div class=\"bizinfo bizinfo-last\" ng-show=\"!preview\">\n" +
        "            <form class=\"form-horizontal\" name=\"createDashboardForm\">\n" +
        "                <div class=\"bgr\">\n" +
        "                    <div class=\"title-heading\">\n" +
        "                        {{resourceBundle['dashboard']}}\n" +
        "                    </div>\n" +
        "                    <div class=\"row\">\n" +
        "                        <div class=\"col-sm-6\">\n" +
        "                            <div class=\"form-group\">\n" +
        "                                <label class=\"col-sm-4 control-label required\">{{resourceBundle['name']}}</label>\n" +
        "                                <div class=\"col-sm-6\">\n" +
        "                                    <input type=\"text\" class=\"form-control\" placeholder=\"{{resourceBundle['name']}}\" ng-model=\"dashboard.name\"\n" +
        "                                           maxlength=\"50\">\n" +
        "                                </div>\n" +
        "                            </div>\n" +
        "                        </div>\n" +
        "                    </div>\n" +
        "                    <div class=\"row\">\n" +
        "                        <div class=\"col-sm-6\">\n" +
        "                            <div class=\"form-group\">\n" +
        "                                <label class=\"col-sm-4 control-label\">{{resourceBundle['description']}}</label>\n" +
        "\n" +
        "                                <div class=\"col-sm-6\">\n" +
        "                                    <input type=\"text\" class=\"form-control\" placeholder=\"{{resourceBundle['description']}}\" ng-model=\"dashboard.desc\"\n" +
        "                                           maxlength=\"50\">\n" +
        "                                </div>\n" +
        "                            </div>\n" +
        "                        </div>\n" +
        "                    </div>\n" +
        "                    <div class=\"row\">\n" +
        "                        <div class=\"col-sm-6\">\n" +
        "                            <div class=\"form-group\">\n" +
        "                                <label class=\"col-sm-4 control-label\">{{resourceBundle['title']}}</label>\n" +
        "\n" +
        "                                <div class=\"col-sm-6\">\n" +
        "                                    <input type=\"text\" class=\"form-control\" placeholder=\"{{resourceBundle['title']}}\" ng-model=\"dashboard.title\"\n" +
        "                                           maxlength=\"50\">\n" +
        "                                </div>\n" +
        "                            </div>\n" +
        "                        </div>\n" +
        "                    </div>\n" +
        "                    <div class=\"row\">\n" +
        "                        <div class=\"col-sm-6\">\n" +
        "                            <div class=\"form-group\">\n" +
        "                                <label class=\"col-sm-4 control-label\">{{resourceBundle['sub.title']}}</label>\n" +
        "\n" +
        "                                <div class=\"col-sm-6\">\n" +
        "                                    <input type=\"text\" class=\"form-control\" placeholder=\"{{resourceBundle['sub.title']}}\" ng-model=\"dashboard.info\">\n" +
        "                                </div>\n" +
        "                            </div>\n" +
        "                        </div>\n" +
        "                    </div>\n" +
        "                </div>\n" +
        "                <div ng-if=\"renderDashboardConfigurePage == true;\" ng-include=\" '/angular-storyboards/src/dashboard/templates/configure-dashboard.html' \" ng-init=\"edit = edit\"></div>\n" +
        "            </form>\n" +
        "        </div>\n" +
        "        <div ng-if=\"preview == true\">\n" +
        "            <div ng-include=\"'/angular-storyboards/src/dashboard/templates/view-dashboard.html'\" ng-init=\"db = temp_dashboard; noInit = true; preview = preview; showTitle = true\"></div>\n" +
        "        </div>\n" +
        "    </div>\n" +
        "</div>"
    );


    $templateCache.put('/angular-storyboards/src/dashboard/templates/list-dashboards.html',
        "<div class=\"tab pane\">\n" +
        "    <div class=\"box topbox\">\n" +
        "        <div ng-controller=\"DashboardListingController\">\n" +
        "            <div>\n" +
        "                <div class=\"row\">\n" +
        "                    <div class=\"col-sm-8\">\n" +
        "                        <table class=\"table table-bordered\">\n" +
        "                            <thead>\n" +
        "                            <tr>\n" +
        "                                <th class=\"col-sm-1\">{{resourceBundle['s.no']}}</th>\n" +
        "                                <th class=\"col-sm-4\">{{resourceBundle['dashboard']}}</th>\n" +
        "                                <th class=\"col-sm-1\">{{resourceBundle['action']}}</th>\n" +
        "                            </tr>\n" +
        "                            </thead>\n" +
        "                            <tbody>\n" +
        "                            <tr ng-repeat=\"(id, dashboard) in dashboards\">\n" +
        "                                <td>{{$index + 1}}</td>\n" +
        "                                <td>{{dashboard.name}}</td>\n" +
        "                                <td>\n" +
        "                                    <span class=\"glyphicon glyphicon-th-large\" ng-click=\"viewDashboard(id)\" title=\"{{resourceBundle['view']}}\"></span>&nbsp;&nbsp;\n" +
        "                                    <span class=\"glyphicon glyphicon-cog\" ng-click=\"editDashboard(id)\" title=\"{{resourceBundle['configure']}}\"></span>\n" +
        "                                </td>\n" +
        "                            </tr>\n" +
        "                            </tbody>\n" +
        "                        </table>\n" +
        "                    </div>\n" +
        "                </div>\n" +
        "            </div>\n" +
        "        </div>\n" +
        "    </div>\n" +
        "</div>"
    );


    $templateCache.put('/angular-storyboards/src/dashboard/templates/view-dashboard.html',
        "<div class=\"row\">\n" +
        "    <div class=\"col-sm-12\">\n" +
        "        <div ng-controller=\"DashboardViewController\">\n" +
        "            <div ng-show=\"preview\" class=\"row pull-right\">\n" +
        "                <div class=\"col-sm-12\">\n" +
        "                    <button type=\"button\" ng-click=\"previewDashboard()\" class=\"btn btn-primary\">\n" +
        "                        {{resourceBundle['exit.preview']}}\n" +
        "                    </button>\n" +
        "                </div>\n" +
        "            </div>\n" +
        "            <div>\n" +
        "                <div class=\"row\" ng-if=\"showTitle\">\n" +
        "                    <div class=\"col-sm-12 pt10\">\n" +
        "                        <h2 style=\"text-align: center;margin: 0px\">{{db.title}}</h2>\n" +
        "                    </div>\n" +
        "                </div>\n" +
        "                <div class=\"row\" ng-if=\"showTitle\">\n" +
        "                    <div class=\"col-sm-12 pt10 pb10\">\n" +
        "                        <h3 style=\"text-align: center;margin: 0px;color: #777;\">{{db.info}}</h3>\n" +
        "                    </div>\n" +
        "                </div>\n" +
        "                <div class=\"row padding5\" id=\"viewDash\">\n" +
        "                    <ul\n" +
        "                            style=\"list-style-type: none; padding: 0; display: block; margin: 0; position: relative; min-height: {{maxHeight}}\"\n" +
        "                            id=\"allWid\">\n" +
        "                        <li class=\"dummy noLRpad\"\n" +
        "                            style=\"z-index: 1; position: absolute; padding:5px; display:list-item; width: {{widget.computedWidth}}px; left: {{widget.computedX}}px; top: {{widget.computedY}}px; height: {{widget.computedHeight}}px;max-height: {{widget.computedHeight}}px;\"\n" +
        "                            ng-controller=\"WidgetScopeController\"\n" +
        "                            ng-repeat=\"(id,widget) in db.widgets track by $index\">\n" +
        "                            <span ng-include=\"'/angular-storyboards/src/widget/templates/view-widget.html'\"></span>\n" +
        "                        </li>\n" +
        "                    </ul>\n" +
        "                </div>\n" +
        "            </div>\n" +
        "        </div>\n" +
        "    </div>\n" +
        "</div>"
    );


    $templateCache.put('/angular-storyboards/src/widget/templates/list-widgets.html',
        "<div ng-controller=\"WidgetsListingController\">\n" +
        "    <div class=\"row\">\n" +
        "        <div class=\"col-sm-12\">\n" +
        "            <button type=\"button\" class=\"btn btn-primary pull-right\" data-toggle=\"modal\" data-target=\"#myModal\">{{resourceBundle['add.widgets']}}</button>\n" +
        "        </div>\n" +
        "    </div>\n" +
        "    <div class=\"modal fade\" id=\"myModal\" role=\"dialog\">\n" +
        "        <div class=\"modal-dialog\">\n" +
        "            <!-- Modal content-->\n" +
        "            <div class=\"modal-content\">\n" +
        "                <div class=\"modal-header ws\">\n" +
        "                    <button type=\"button\" class=\"close\" data-dismiss=\"modal\">&times;</button>\n" +
        "                    <h4 class=\"modal-title\">{{resourceBundle['widgets.upper']}}</h4>\n" +
        "                </div>\n" +
        "                <div class=\"modal-body ws\" style=\"max-height: 350px; overflow-y: scroll; display: block;\">\n" +
        "                    <ul class=\"list-group\">\n" +
        "                        <li class=\"list-group-item\" ng-repeat=\"widget in widgets\">\n" +
        "                            <span>{{widget.name}}</span>\n" +
        "                            <span class=\"btn btn-sm btn-primary pull-right\" ng-click=\"addWidget(widget)\"  style=\"position:relative;bottom:5px;\">{{resourceBundle['prm.add']}}</span>\n" +
        "                        </li>\n" +
        "                    </ul>\n" +
        "                </div>\n" +
        "                <div class=\"modal-footer ws\">\n" +
        "                    <button type=\"button\" class=\"btn btn-default\" data-dismiss=\"modal\">{{resourceBundle['close']}}</button>\n" +
        "                </div>\n" +
        "            </div>\n" +
        "        </div>\n" +
        "    </div>\n" +
        "</div>"
    );


    $templateCache.put('/angular-storyboards/src/widget/templates/view-widget-template.html',
        "<widget-panel>\n" +
        "    <div class=\"panel panel-default dummy\" style=\"height: {{widget.computedHeight}}px; overflow: hidden\">\n" +
        "        <div class=\"panel-heading\" style=\"display: flex\">\n" +
        "            <span class=\"panel-title\" style=\"flex: 1 1 0\">{{widget.name}}</span>\n" +
        "            <div class=\"btn-group\">\n" +
        "                <a data-toggle=\"modal\" data-target=\"#editWidgetModal{{widget.id}}\" uib-tooltip=\"Configure\">\n" +
        "                    <span class=\"glyphicon glyphicon-asterisk\"></span>\n" +
        "                </a>\n" +
        "                <span class=\"lPad10\"></span>\n" +
        "                <a ng-click=\"removeWidget(widget)\" uib-tooltip=\"{{resourceBundle['remove']}}\">\n" +
        "                    <span class=\"glyphicon glyphicon-trash\"></span>\n" +
        "                </a>\n" +
        "            </div>\n" +
        "        </div>\n" +
        "        <div class=\"row\" style=\"margin: 0\">\n" +
        "            <div class=\"col-sm-12 text-center noLRpad\">\n" +
        "                <span class=\"dummy widget-watermark\" style=\"line-height: {{item.computedHeight - 45}}px\">{{resourceBundle['widget']}}</span>\n" +
        "            </div>\n" +
        "        </div>\n" +
        "    </div>\n" +
        "</widget-panel>"
    );


    $templateCache.put('/angular-storyboards/src/widget/templates/view-widget.html',
        "<div ng-controller=\"WidgetsViewController\">\n" +
        "    <div style=\"height: {{widget.computedHeight}}px; min-height: {{widget.computedHeight}}px;max-height: {{widget.computedHeight}}px; overflow: hidden; border-radius: 5px; border: 1px solid #dddddd; padding: 2px\">\n" +
        "        <div ng-include=\"widgetTemplate.templateUrl\"></div>\n" +
        "    </div>\n" +
        "</div>"
    );
}]);