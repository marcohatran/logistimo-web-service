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

var userServices = angular.module('userServices', []);
userServices.factory('userService', ['APIService','iAuthService','$q', function (apiService, iAuthService, $q) {
    return {
        getUsers: function (entityId, srcEntityId) {
            var urlStr = '/s2/api/entities/entity/' + entityId + "/users";
            if (typeof srcEntityId !== 'undefined') {
                urlStr = urlStr + "?srcEntityId=" + srcEntityId;
            }
            return apiService.get(urlStr);
        },
        getUsersByRole: function (role, q, offset, size) {
            offset = typeof offset !== 'undefined' ? offset : 0;
            size = typeof size !== 'undefined' ? size : 50;
            var urlStr = '/s2/api/users/role/' + role + "?offset="
                + offset + "&size=" + size;
            if (typeof q !== 'undefined') {
                urlStr = urlStr + "&q=" + q;
            }
            return apiService.get(urlStr);
        },
        getDomainUsers: function (q, offset, size, utype, includeSuperusers, includeChildDomainUsers) {
            offset = typeof offset !== 'undefined' ? offset : 0;
            size = typeof size !== 'undefined' ? size : 50;
            var urlStr = '/s2/api/users/?offset=' + offset + "&size="
                + size;
            if (typeof q !== 'undefined') {
                urlStr = urlStr + "&q=" + q;
            }
            if (typeof utype !== 'undefined') {
                urlStr = urlStr + "&utype=" + utype;
            }
            if (typeof includeSuperusers !== 'undefined') {
                urlStr = urlStr + "&includeSuperusers=" + includeSuperusers;
            }
            if (typeof includeChildDomainUsers !== 'undefined') {
                urlStr = urlStr + "&includeChildDomainUsers=" + includeChildDomainUsers;
            }
            return apiService.get(urlStr);
        },
        getFilteredDomainUsers : function(filters){
            var urlStr = '/s2/api/users/domain/users';
            return apiService.post(filters, urlStr);
        },
        getElementsByUserFilter : function(pv,pn){
           var urlStr="/s2/api/users/elements/?paramName=" +pv+ "&paramValue="+pn;
            return apiService.get(urlStr);
        },
        checkUserAvailability: function (userid) {
            return apiService.get('/s2/api/users/check/?userid=' + userid);
        },
        checkCustomIDAvailability: function (customId, userId) {
            var params = customId;
            if(checkNotNullEmpty(userId)){
                params += "&userId="+userId;
            }
            return apiService.get('/s2/api/users/check/custom?customId=' + params);
        },
        createUser: function (user) {
            return $q(function(resolve, reject) {

                var userRef = angular.copy(user);
                iAuthService.randomSalt().then(function(salt){
                    if (checkNullEmpty(userRef.pw) || checkNullEmpty(userRef.cpw)) {
                        reject("Password cannot be empty")
                    }
                    userRef.cpw = hex_sha512(salt.data.salt + userRef.cpw);
                    userRef.pw = salt.data.salt+"####"+hex_sha512(salt.data.salt + userRef.pw);
                    apiService.post(userRef, "/s2/api/users/").then(function (data) {
                        resolve(data);
                    }).catch(function (err) {
                        reject(err);
                    })
                }).catch(function(err){
                    reject(err);
                })
            });
        },
        deleteUsers: function (user) {
            return apiService.post("'" + user + "'", "/s2/api/users/delete/");
        },
        getUser: function (userid) {
            return apiService.get('/s2/api/users/user/' + userid);
        },
        getUserMeta: function (userid) {
            return apiService.get('/s2/api/users/user/meta/' + userid);
        },
        getUserDetails: function (userid) {
            return apiService.get('/s2/api/users/user/' + userid + '?isDetail=true');
        },
        getUsersDetailByIds: function (userIds) {
            return apiService.get('/s2/api/users/users/?userIds=' + userIds + '&isMessage=true');
        },
        sendUserMessage: function (message) {
            return apiService.post(message, '/s2/api/users/sendmessage/');
        },
        getUsersMessageStatus: function (offset, size) {
            return apiService.get('/s2/api/users/msgstatus/?offset=' + offset + '&size=' + size);
        },
        updateUser: function (user, userId) {
            return apiService.post(user, '/s2/api/users/user/' + userId);
        },
        updateUserPassword: function (user, userId) {
            var data = {uid: userId, is_enhanced: true};
            return $q(function(resolve, reject) {
                iAuthService.getSalt(userId).then(function(salt){
                    if (checkNullEmpty(user.opw) || checkNotNullEmpty(user.npw)) {
                        reject("Password cannot be empty")
                    }
                    data.old_password = hex_sha512(salt.data.salt + user.opw);
                    data.npd = hex_sha512(salt.data.salt + user.pw);
                    apiService.post(data, '/s2/api/users/updatepassword').then(function (response) {
                        resolve(response);
                    }).catch(function (err) {
                        reject(err);
                    })
                }).catch(function(err){
                    reject(err);
                })
            });
        },
        resetUserPassword: function (userId, sendType) {
            return apiService.get('/s2/api/users/resetpassword/?userId=' + userId + '&sendType=' + sendType);
        },
        enableDisableUser: function (userId, action) {
            return apiService.get('/s2/api/users/userstate/?userId=' + userId + '&action=' + action);
        },
        getRoles: function(edit,eUsrid) {
            edit = typeof edit !== 'undefined' ? edit : false;
            eUsrid = typeof eUsrid !== 'undefined' ? eUsrid : "";
            return apiService.get('/s2/api/users/roles?edit=' + edit + '&euid=' + eUsrid);
        },
        switchConsole: function(){
            return apiService.get('/s2/api/users/switch');
        },
        addAccessibleDomains: function (userId, accDids) {
            return apiService.post({userId: userId, accDids: accDids}, "/s2/api/users/addaccessibledomains");
        },
        removeAccessibleDomain: function (userId, domainId) {
            return apiService.get("/s2/api/users/removeaccessibledomain?userId=" + userId + "&domainId=" + domainId);
        },
        forceLogoutOnMobile: function (userId) {
            return apiService.get("/s2/api/users/forcelogoutonmobile?userId=" + userId);
        },
        getTokens: function () {
            return apiService.get("/s2/api/users/personal-access-token/");
        },
        generateToken: function (tokenDescription) {
            return apiService.post({payload: tokenDescription}, "/s2/api/users/personal-access-token/");
        },
        deleteToken: function(token) {
            return apiService.delete("/s2/api/users/personal-access-token/"+token);
        }
    }
}
]);
