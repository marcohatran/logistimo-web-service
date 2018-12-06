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
 * Created by Mohan Raja on 03/04/15
 */
var authControllers = angular.module('authControllers', []);
authControllers.controller('LoginController', ['$scope', 'iAuthService', 'authService', '$rootScope','$timeout', '$uibModal',
    function ($scope, iAuthService, authService, $rootScope, $timeout, $uibModal) {
        $scope.lLoading = false;
        $scope.fp = false;
        $scope.showCaptcha = true;
        $scope.captchaEnabled = true;

        $scope.init = function(){
            $scope.denied = false;
            if(checkNotNullEmpty($scope.curUser)){
                $scope.userId = $scope.curUser;
            }
            $scope.otp = undefined;
            $scope.errorMsg = undefined;
            $scope.invalid = false;
            $scope.fp = false;
            iAuthService.getCaptchaConfig().then(function (config) {
                $scope.captchaEnabled = config.data['enabled'];
            })
        };
        $scope.init();

        $scope.setCaptchaResponse = function(response) {
            $scope.captchaResponse = response;
        };

        $scope.resetCaptcha = function() {
            if($scope.captchaEnabled) {
                $scope.showCaptcha = false;
                $timeout(function(){
                    $scope.showCaptcha = true;
                },10);
                $scope.captchaResponse = undefined;
            }
        };

        $scope.captchaDisabledOrNonEmpty = function() {
            return !$scope.captchaEnabled || isCaptchaValid($scope.captchaResponse)
        }

        $scope.login = function (userId, password, otp) {
            if (checkNotNullEmpty(otp) || $scope.captchaDisabledOrNonEmpty()) {
            if (checkNullEmpty(userId) || checkNullEmpty(password)) {
                $scope.invalid = true;
                $scope.errorMsg = (checkNullEmpty(userId) ? $scope.resourceBundle['user.id'] : $scope.resourceBundle['login.password']) + " " + $scope.resourceBundle['isrequired'];
            } else {
                $scope.lLoading = true;
                iAuthService.login(userId, password, $scope.i18n.language.locale, $scope.captchaResponse, otp).then(function (data) {
                    $scope.errMsg = data.data;
                    if ($scope.errMsg.isError) {
                        $scope.invalid = true;
                        $scope.showMsg = true;
                        if($scope.errMsg.ec == 4){
                            $scope.denied = true;
                        }
                        $scope.errorMsg = $scope.errMsg.errorMsg;
                    } else if(checkNullEmpty(data.data.dmn)) {
                        $scope.mobileNo = data.data.mobileNo.substring(data.data.mobileNo.length-2);
                        $scope.userId = userId;
                        $scope.password = password;
                        $scope.modalInstance = $uibModal.open({
                            templateUrl: 'views/two-factor-authentication.html',
                            scope: $scope,
                            backdrop: 'static',
                            backdropClass: 'login-modal',
                            windowClass: 'login-modal-win',
                            keyboard: false
                        });
                    } else {
                        $scope.invalid = false;
                        $scope.errorMsg = undefined;
                        if(checkNotNullEmpty($scope.modalInstance)) {
                            $scope.modalInstance.close();
                            $scope.modalInstance = undefined;
                        }
                        $scope.setAuthentication(data);
                    }
                }).catch(function err(response){
                    $scope.invalid = true;
                    $scope.showMsg = true;
                    if(response && response.status == 403){
                        $scope.denied = true;
                        $scope.errorMsg = $scope.resourceBundle['login.unable'] + " " + $scope.resourceBundle['user.access.denied'];
                    }else {
                        $scope.errorMsg = $scope.resourceBundle['login.unable'] + " " + response ? response.data ? response.data.message : "" : "";
                    }
                }).finally(function (){
                    $scope.lLoading = false;
                    $scope.resetCaptcha();
                    $scope.otp = undefined;
                });
             }
            } else {
                $scope.showWarning($scope.resourceBundle['captcha.error.response']);
            }
        };

        $scope.cancel = function() {
            $scope.init();
            $scope.showLogin(true);
        };

        $scope.generateNewOTP = function() {
            $scope.nLoading = true;
            var mode = "0";
            iAuthService.generateAuthenticationOTP($scope.userId, mode).then(function(data){
                $scope.showSuccess(data.data.errorMsg);
            }).catch(function error(msg){
                $scope.showWarning(msg.data.message);
            }).finally(function (){
                $scope.nLoading = false;
            });
        };

        function getAuthCache(headers) {
            var keys = Object.keys(headers);
            var index = 0;
            if(keys) {
                keys.some(function (key) {
                    index += 1;
                    return key.startsWith("_di");
                });
                if(index < keys.length) {
                    return {
                        key : keys[index - 1],
                        value : headers[keys[index - 1]]
                    }
                }
            }

        }

        $scope.setAuthentication = function(data) {

            iAuthService.setAccessToken(data.headers()['x-access-token'], data.headers()['expires']);
            var cookie = getAuthCache(data.headers());
            if(cookie) {
                iAuthService.setAuthenticationHeader(cookie.key, cookie.value, data.headers()['expires']);
            }
            //log analytics
            $scope.initAnalytics(data.data.fnm,data.data.dmn.nm, data.data.tgs);

            if(checkNotNullEmpty($scope.curUser)){
                if($scope.curUser != $scope.userId){
                    authService.loginConfirmed({initApp: true}, null, true);
                    $scope.lLoading = true;
                    $scope.refreshDomainConfig().then(function(){
                        $scope.changeContext();
                    }).catch(function error(msg){
                        console.log(msg);
                    }).finally(function(){
                        $scope.hideLogin();
                        $scope.lLoading = false;
                    });
                }else{
                    authService.loginConfirmed({initApp: false}, null, false);
                    $scope.hideLogin();
                }
            }else{
                authService.loginConfirmed({initApp: true}, null, false);
                $scope.hideLogin();
                $scope.refreshDomainConfig();

                if($scope.userLoggedOut){
                    $scope.changeContext();
                    $scope.userLoggedOut = false;
                }
            }
        };

        $scope.forgotPassword = function(){
            $scope.fp = true;
            $scope.showMsg = false;
        };

        $scope.reset = function(){
            $scope.fp = false;
        }
    }]);

authControllers.controller('BulletinBoardAuthController', ['$scope', 'iAuthService', function ($scope, iAuthService) {
    function init() {
        $scope.authKey = "";
    }

    init();

    $scope.authorizeBulletinBoard = function () {
        if (checkNotNullEmpty($scope.authKey)) {
            $scope.showLoading();
            iAuthService.authorizeBulletinBoard($scope.authKey).then(function (data) {
                $scope.showSuccess($scope.resourceBundle['authorized.success']);
            }).catch(function err(msg) {
                $scope.showErrorMsg(msg);
            }).finally(function () {
                init();
                $scope.hideLoading();
            })
        } else {
            $scope.showWarning($scope.resourceBundle['enter.key']);
        }
    }
}]);

authControllers.controller('BulletinBoardLoginController', ['$scope', 'iAuthService', 'authService', '$rootScope', '$timeout',
    function ($scope, iAuthService, authService, $rootScope, $timeout) {
        $scope.lLoading = false;
        $scope.accessKey = "";

        $scope.init = function () {
            $scope.lLoading = true;
            iAuthService.requestAccessKey().then(function (data) {
                $scope.accessKey = data.data;
                $timeout($scope.checkAccessKey, 5000);
            }).catch(function error(msg) {
                $scope.errorMsg = $scope.resourceBundle['login.unable'] + " " + msg;
            }).finally(function () {
                $scope.lLoading = false;
            });
        };
        $scope.init();

        $scope.checkAccessKey = function () {
            $scope.lLoading = true;
            iAuthService.checkAccessKey($scope.accessKey).then(function (data) {
                if (!checkNullEmptyObject(data.data)) {
                    iAuthService.setAccessToken(data.data["x-access-token"], data.data.expires);
                    authService.loginConfirmed(null, null, false);
                    $scope.hideLogin();
                } else {
                    $timeout($scope.checkAccessKey, 5000);
                }
            }).catch(function err(msg) {
                $scope.errorMsg = "Unable to check authorisation: " + msg;
                $timeout($scope.checkAccessKey, 5000);
            }).finally(function () {
                $scope.lLoading = false;
            })
        };
    }]);

authControllers.controller('ForgotPasswordController', ['$scope', 'iAuthService',
    function($scope, iAuthService){
        function init(){
            $scope.fpw = {mode:"0"};
            $scope.otp = true;
            $scope.openOtp = false;
            $scope.invalid = false;
            $scope.showMsg = false;
            $scope.errorMsg = "";
            $scope.fLoading = false;
            $scope.nLoading = false;
            $scope.resetPwd = false;
        }

        init();

        $scope.toggleOTP = function(){
            $scope.openOtp = !$scope.openOtp;
        };

        $scope.cancelFP = function(){
            init();
            $scope.reset();
        };

        $scope.generateNewOtp = function(){
            $scope.fpw.otp = "";
            $scope.isCaptchaValidated = true;
            $scope.resetPwd = false;
            $scope.otp = true;
            $scope.toggleOTP();
        };

        $scope.generateOtp = function(){
            if(checkNotNullEmpty($scope.fpw)) {
                if (!$scope.isCaptchaValidated || $scope.captchaDisabledOrNonEmpty()) {
                    $scope.fpw.captcha = $scope.$parent.captchaResponse;
                    if (checkNullEmpty($scope.fpw.uid)) {
                        $scope.showWarning($scope.resourceBundle['pwd.user.id.required']);
                        return;
                    }

                    $scope.fLoading = true;

                    iAuthService.generateOtp($scope.fpw).then(function (data) {
                        $scope.fpResponse = data.data;
                        $scope.invalid = false;
                        $scope.errorMsg = undefined;
                        $scope.showSuccess($scope.fpResponse.errorMsg);
                        $scope.resetPwd = true;
                        if (!$scope.newotp && $scope.fpw.mode == '0') {
                            $scope.otp = false;
                            $scope.toggleOTP();
                        }
                    }).catch(function error(msg) {
                        $scope.invalid = true;
                        $scope.showMsg = true;
                        $scope.errorMsg = msg.data.message;
                    }).finally(function () {
                        $scope.fLoading = false;
                        $scope.newotp = false;
                        if ($scope.fpw.mode == '1' && !$scope.invalid) {
                            $scope.cancelFP();
                        }
                        $scope.resetCaptcha();
                    });
                } else {
                    $scope.showWarning($scope.resourceBundle['captcha.error.response']);
                }
            } else{
                $scope.showWarning($scope.resourceBundle['pwd.user.id.required']);
            }
        };

        $scope.resetPassword = function(){
            if($scope.fpw.mode != '1' && checkNullEmpty($scope.fpw.otp)){
                $scope.showWarning($scope.resourceBundle['pwd.otp.required']);
                return;
            }
            $scope.fLoading = true;
            iAuthService.generatePassword($scope.fpw, null).then(function(data){
                $scope.fpResponse = data.data;
                $scope.invalid = false;
                $scope.errorMsg = undefined;
                $scope.showSuccess($scope.fpResponse.errorMsg);
                $scope.cancelFP();
            }).catch(function error(msg){
                $scope.invalid = true;
                $scope.showMsg = true;
                $scope.errorMsg = msg.data.message;
            }).finally(function (){
                $scope.fLoading = false;
            });
        };

    }]);
