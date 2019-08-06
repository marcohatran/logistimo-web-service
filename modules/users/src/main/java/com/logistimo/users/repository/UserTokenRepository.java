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

package com.logistimo.users.repository;

import com.logistimo.jdo.JDORepository;
import com.logistimo.users.entity.TokenType;
import com.logistimo.users.entity.UserToken;

import org.springframework.stereotype.Repository;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Repository
public class UserTokenRepository extends JDORepository<UserToken, String> {
  @Override
  public Class<UserToken> getClassMetadata() {
    return UserToken.class;
  }

  public List<UserToken> findAllPersonalAccessTokensByUserIdAndDomainId(String userId, Long domainId) {
    String query = "SELECT FROM "
    + getClassMetadata().getName()
    + " WHERE userId == userIdParam && domainId == domainIdParam && tokenType == tokenTypeParam PARAMETERS String userIdParam, Long domainIdParam, com.logistimo.users.entity.TokenType tokenTypeParam ORDER BY createdOn DESC";
    Map<String, Object> params = new HashMap<>();
    params.put("userIdParam", userId);
    params.put("domainIdParam", domainId);
    params.put("tokenTypeParam", TokenType.PERSONAL_ACCESS_TOKEN);
    return findAll(query, params);
  }

}
