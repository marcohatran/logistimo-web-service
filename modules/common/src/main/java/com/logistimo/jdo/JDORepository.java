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

package com.logistimo.jdo;

import com.logistimo.services.impl.PMF;

import java.util.List;
import java.util.Map;

import javax.jdo.PersistenceManager;

import static com.logistimo.constants.Constants.JAVAX_JDO_QUERY_SQL;

/**
 * Generic jdo entity repository
 */
public abstract class JDORepository<T, ID> {

  public abstract Class<T> getClassMetadata();

  /**
   * Retrieves an entity by its id.
   *
   * @param id must not be {@literal null}.
   * @return the entity with the given id or {@literal null} if none found
   * @throws IllegalArgumentException if {@code id} is {@literal null}
   */
  public T findOne(ID id) {
    PersistenceManager pm = PMF.get().getPersistenceManager();
    try {
      return findOne(id, pm);
    } finally {
      pm.close();
    }
  }

  /**
   * Retrieves and entity using the persistence manager provided
   * @param id must not be {@literal null}.
   * @return the entity with the given id or {@literal null} if none found
   * @throws IllegalArgumentException if {@code id} is {@literal null}
   */
  public T findOne(ID id, PersistenceManager pm) {
    return pm.getObjectById(getClassMetadata(), id);
  }

  /**
   * Retrieves all entities matching the native sql query
   * @param sql - native sql query
   * @param params - parameters
   * @return all the entities
   */
  public List<T> findAll(String sql, List<Object> params) {
    PersistenceManager pm = PMF.get().getPersistenceManager();
    try {
      return findAll(sql, params, pm);
    } finally {
      pm.close();
    }
  }

  /**
   * Retrieves all entities matching the native sql query
   * @param sql - native sql query
   * @param params - parameters
   * @param pm - Persistence manager to use
   * @return all the entities
   */
  public List<T> findAll(String sql, List<Object> params, PersistenceManager pm) {
    return (List<T>) pm.newQuery(JAVAX_JDO_QUERY_SQL,sql).executeWithArray(params.toArray());
  }

  /**
   * Retrieves all entities matching the native sql query
   * @param jql - JDOQL query
   * @param params - parameters
   * @return all the entities
   */
  public List<T> findAll(String jql, Map<String, Object> params) {
    PersistenceManager pm = PMF.get().getPersistenceManager();
    try {
      return findAll(jql, params, pm);
    } finally {
      pm.close();
    }
  }

  /**
   * Retrieves all entities matching the native sql query
   * @param jql - JDO QL query
   * @param params - parameters
   * @param pm - Persistence manager to use
   * @return all the entities
   */
  private List<T> findAll(String jql, Map<String, Object> params, PersistenceManager pm) {
    return (List<T>) pm.newQuery(jql).executeWithMap(params);
  }


  /**
   * delete the entity associated with this id.
   * @param id must not be null
   */
  public void delete(ID id) {
    PersistenceManager pm = PMF.get().getPersistenceManager();
    try {
      delete(id, pm);
    } finally {
      pm.close();
    }
  }

  /**
   * delete the entity associated with this id.
   * @param id must not be null
   * @param pm Persistence manager to use.
   */
  private void delete(ID id, PersistenceManager pm) {
    pm.deletePersistent(findOne(id, pm));
  }
}
