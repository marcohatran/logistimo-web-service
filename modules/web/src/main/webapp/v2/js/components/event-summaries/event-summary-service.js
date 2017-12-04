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
 * Created by naveensnair on 30/11/17.
 */

var eventSummaryServices = angular.module('eventSummaryServices', []);


eventSummaryServices.factory('eventSummaryService', ['APIService', function (apiservice) {
    return {
        getEventSummariesByEventId: function(domainId, userId, eventId, offset, size) {
            var urlStr = "";
            if(checkNotNullEmpty(eventId)) {
                urlStr += "/" + eventId;
            }
            if(checkNotNullEmpty(domainId)) {
                urlStr += "?cn_domain_id=" + domainId;
            }
            if(checkNotNullEmpty(userId)) {
                urlStr += "&user_id=" + userId;
            }
            if(checkNotNullEmpty(offset)) {
                urlStr += "&offset=" + offset;
            } else {
                urlStr += "&offset=0";
            }
            if(checkNotNullEmpty(size)) {
                urlStr += "&size=" + size;
            } else {
                urlStr += "&size=10";
            }
            return apiservice.get(MAPI_URL + urlStr, undefined);

        },
        getEventSummariesDistribution: function (domainId, userId, eventId, include_distribution) {
            return apiservice.get(MAPI_URL + "?cn_domain_id=" + domainId + "&user_id=" + userId + "&event_id=" +
            eventId + "&include_distribution=" + include_distribution, undefined);
        }
    }
}]);


