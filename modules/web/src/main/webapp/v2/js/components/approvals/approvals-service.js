/**
 * Created by naveensnair on 30/05/17.
 */
var approvalServices = angular.module('approvalServices', []);
approvalServices.factory('approvalService', ['APIService', function (apiService) {
    return {
        createApproval: function(data) {
            return apiService.post(data, '/s2/api/order-approvals');
        },
        getApprovals: function(offset, size, entityId, orderId, reqStatus, expiry, reqType, reqId, aprId, domainId){
            offset = typeof offset !== 'undefined' ? offset : 0;
            size = typeof size !== 'undefined' ? size : 50;
            var urlStr = '/s2/api/order-approvals/?offset=' + offset + "&size=" + size;
            if(checkNotNullEmpty(entityId)) {
                urlStr = urlStr + "&entity_id=" + entityId;
            }
            if(checkNotNullEmpty(reqStatus)) {
                urlStr = urlStr + "&status=" + reqStatus;
            }
            if(checkNotNullEmpty(expiry)) {
                urlStr = urlStr + "&expiring_in=" + expiry * 60;
            }
            if(checkNotNullEmpty(reqType)) {
                urlStr = urlStr + "&request_type=" + reqType;
            }
            if(checkNotNullEmpty(reqId)) {
                urlStr = urlStr + "&requester_id=" + reqId;
            }
            if(checkNotNullEmpty(aprId)) {
                urlStr = urlStr + "&approver_id=" + aprId;
            }
            if(checkNotNullEmpty(domainId)) {
                urlStr = urlStr + "&domainId=" + domainId;
            }
            if(checkNotNullEmpty(orderId)) {
                urlStr = urlStr + "&order_id=" + orderId;
            }
            urlStr = urlStr + "&embed=order_meta";

            return this.fetch(urlStr);
        },
        fetchApproval: function(approvalId) {
            return apiService.get("/s2/api/order-approvals/" + approvalId);
        },
        updateApprovalStatus: function(approvalId, approval) {
            return apiService.put(approval, "/s2/api/order-approvals/" + approvalId + "/status")
        }
    }
}]);

