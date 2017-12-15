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

package com.logistimo.healthchecks;

import com.codahale.metrics.health.HealthCheck;
import com.logistimo.services.impl.PMF;

import javax.jdo.PersistenceManager;
import javax.jdo.Query;

/**
 * Created by charan on 15/12/17.
 */
public class PMHealthCheck extends HealthCheck {

  public Result check(PersistenceManager pm) {
    Query query = null;
    try {
      query = pm.newQuery("javax.jdo.query.SQL", "select 1");
      query.execute();
      return Result.healthy();
    } catch (Exception e) {
      return Result.unhealthy("Can't ping database");
    } finally {
      if (query != null) {
        query.closeAll();
      }
      pm.close();
    }
  }

  @Override
  protected Result check() throws Exception {
    return check(PMF.get().getPersistenceManager());
  }
}