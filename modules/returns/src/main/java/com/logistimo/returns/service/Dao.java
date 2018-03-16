/*
 * Copyright © 2018 Logistimo.
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

package com.logistimo.returns.service;


import org.apache.commons.collections.CollectionUtils;
import org.springframework.stereotype.Component;
import java.util.List;
import java.util.Map;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import javax.persistence.Query;

/**
 * Created by pratheeka on 13/03/18.
 */
@Component

public class Dao {

  @PersistenceContext
  private EntityManager entityManager;



  public <T> T save(T entity){
    entityManager.persist(entity);
    return entity;
  }

  public <T> List<T> findAll(String query,Map<String,Object> filters){
    Query query1=entityManager.createNamedQuery(query);
    filters.forEach(query1::setParameter);
    return query1.getResultList();
  }

  public <T> T find(String query,Map<String,Object> filters){
    List<T> results=findAll(query,filters);
    if(CollectionUtils.isNotEmpty(results)){
      return results.get(0);
    }
    return null;
  }

  public <T> T findById(Class cls,Long id){
    return (T)entityManager.find(cls,id);
  }

  public <T> List<T> findAllByNativeQuery(String query,Map<String,Object> filters,Class mappingClass){
    Query query1=entityManager.createNativeQuery(query, mappingClass);
    filters.forEach(query1::setParameter);
    return query1.getResultList();
  }

}
