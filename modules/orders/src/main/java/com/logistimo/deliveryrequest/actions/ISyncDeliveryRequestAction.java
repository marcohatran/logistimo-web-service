/*
 * Copyright © 2019 Logistimo.
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

package com.logistimo.deliveryrequest.actions;

import com.logistimo.deliveryrequest.entities.DeliveryRequest;
import com.logistimo.deliveryrequest.models.DeliveryRequestUpdateWrapper;
import com.logistimo.services.ServiceException;

/**
 * Created by chandrakant on 13/06/19.
 */
public interface ISyncDeliveryRequestAction {
  /**
   * method to sync the delivery request with 3PL transporter
   * @param userId: userId of the user initiating the action
   * @param id delivery request Id
   * @return updated DeliveryRequest entity
   * @throws ServiceException
   */
  DeliveryRequest invoke(String userId, Long id) throws ServiceException;

  /**
   * method to sync the delivery request with patch of the update recieved from webhook
   * @param userId
   * @param updateModel patch of the delivery request received through webhooks
   * @return updated DeliveryRequest entity
   * @throws ServiceException
   */
  DeliveryRequest invoke(String userId, DeliveryRequestUpdateWrapper updateModel)
      throws ServiceException;
}