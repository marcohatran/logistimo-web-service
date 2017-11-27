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

package com.logistimo.api.servlets.mobile.builders;

import com.logistimo.entities.entity.IApprover;
import com.logistimo.proto.MobileApproversModel;
import com.logistimo.proto.MobileEntityApproversModel;
import com.logistimo.services.Services;
import com.logistimo.users.service.UsersService;
import com.logistimo.users.service.impl.UsersServiceImpl;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Created by vani on 28/06/17.
 */
public class MobileEntityBuilder {

  /**
   * Builds a list of approver models for an entity as required by the mobile from a list of IApprover objects
   * @param approversList
   * @param isPurchaseApprovalEnabled
   * @param isSalesApprovalEnabled
   */
  public MobileEntityApproversModel buildApproversModel(List<IApprover> approversList,
                                                        boolean isPurchaseApprovalEnabled,
                                                        boolean isSalesApprovalEnabled) {
    if (approversList == null || approversList.isEmpty()) {
      return null;
    }
    List<String> pap = new ArrayList<>();
    List<String> sap = new ArrayList<>();
    List<String> pas = new ArrayList<>();
    List<String> sas = new ArrayList<>();
    Map<String, List<IApprover>> purchaseSalesApproversList = approversList.stream()
        .collect(Collectors.groupingBy(IApprover::getOrderType));
    purchaseSalesApproversList.entrySet().stream().forEach(
        entry -> processEntry(entry.getKey(), entry.getValue(), isPurchaseApprovalEnabled,
            isSalesApprovalEnabled, pap, pas, sap, sas));
    if (pap.isEmpty() && pas.isEmpty() && sap.isEmpty() && sas.isEmpty()) {
      return null;
    }
    MobileEntityApproversModel mobileEntityApproversModel = new MobileEntityApproversModel();
    MobileUserBuilder mobileUserBuilder = new MobileUserBuilder();
    UsersService us = Services.getService(UsersServiceImpl.class);
    if (!pap.isEmpty() || !sap.isEmpty()) {
      MobileApproversModel mobileApproversModel = new MobileApproversModel();
      mobileApproversModel.prm =
          mobileUserBuilder.buildMobileUserModels(mobileUserBuilder.constructUserAccount(
              us, pap));
      mobileApproversModel.scn =
          mobileUserBuilder.buildMobileUserModels(mobileUserBuilder.constructUserAccount(
              us, sap));
      mobileEntityApproversModel.prc = mobileApproversModel;
    }
    if (!pas.isEmpty() || !sas.isEmpty()) {
      MobileApproversModel mobileApproversModel = new MobileApproversModel();
      mobileApproversModel.prm =
          mobileUserBuilder.buildMobileUserModels(mobileUserBuilder.constructUserAccount(
              us, pas));
      mobileApproversModel.scn =
          mobileUserBuilder.buildMobileUserModels(mobileUserBuilder.constructUserAccount(us, sas));
      mobileEntityApproversModel.sle = mobileApproversModel;
    }
    return mobileEntityApproversModel;
  }

  private void processEntry(String orderType, List<IApprover> approvers,
                            boolean isPurchaseApprovalEnabled, boolean isSalesApprovalEnabled,
                            List<String> primaryApproversPurchaseOrder,
                            List<String> primaryApproversSalesOrder,
                            List<String> secondaryApproversPurchaseOrder,
                            List<String> secondaryApproversSalesOrder) {
    if (isPurchaseApprovalEnabled && IApprover.PURCHASE_ORDER.equals(orderType)) {
      populateLists(approvers, primaryApproversPurchaseOrder, secondaryApproversPurchaseOrder);
    } else if (isSalesApprovalEnabled && IApprover.SALES_ORDER.equals(orderType)) {
      populateLists(approvers, primaryApproversSalesOrder, secondaryApproversSalesOrder);
    }
  }

  private void populateLists(List<IApprover> approvers, List<String> primaryApprovers,
                             List<String> secondaryApprovers) {
    approvers.forEach(approver -> processApprover(approver, primaryApprovers, secondaryApprovers));
  }

  private void processApprover(IApprover approver, List<String> primaryApprovers,
                               List<String> secondaryApprovers) {
    if (approver.getType().intValue() == IApprover.PRIMARY_APPROVER) {
      primaryApprovers.add(approver.getUserId());
    } else {
      secondaryApprovers.add(approver.getUserId());
    }
  }
}
