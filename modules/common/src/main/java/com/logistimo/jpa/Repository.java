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

package com.logistimo.jpa;


import org.apache.commons.collections.CollectionUtils;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import javax.persistence.Query;
import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.Predicate;
import javax.persistence.criteria.Root;

/**
 * Created by pratheeka on 13/03/18.
 */

@Component

public class Repository {

  @PersistenceContext
  protected EntityManager entityManager;

  public <T> T save(T entity) {
    entityManager.persist(entity);
    return entity;
  }

  public <T> T update(T entity) {
    entityManager.merge(entity);
    return entity;
  }

  @SuppressWarnings("unchecked")
  public <T> List<T> findAll(String query, Map<String, Object> filters) {
    Query q = entityManager.createNamedQuery(query);
    filters.forEach(q::setParameter);
    return q.getResultList();
  }

  public <T> T find(String query, Map<String, Object> filters) {
    List<T> results = findAll(query, filters);
    if (CollectionUtils.isNotEmpty(results)) {
      return results.get(0);
    }
    return null;
  }

  public <T> T findById(Class<T> cls, Object id) {
    return entityManager.find(cls, id);
  }

  public <T> List<T> findAllByNativeQuery(String query, Map<String, Object> filters,
      Class mappingClass, int size, int offset) {
    Query query1 = entityManager.createNativeQuery(query, mappingClass);
    filters.forEach(query1::setParameter);
    query1.setFirstResult(offset);
    query1.setMaxResults(size);
    return query1.getResultList();
  }

  public <T> T findByNativeQuery(String query, Map<String, Object> filters) {
    Query query1 = entityManager.createNativeQuery(query);
    filters.forEach(query1::setParameter);
    return (T) query1.getSingleResult();
  }

  public <T> List<T> findAllBySpecification(Specification<T> specification, int offset, int size) {
    CriteriaBuilder criteriaBuilder = entityManager.getCriteriaBuilder();
    CriteriaQuery<T> criteriaQuery = criteriaBuilder.createQuery(specification.getType());
    Root<T> root = criteriaQuery.from(specification.getType());
    Predicate predicate = specification.toPredicate(root, criteriaBuilder);
    criteriaQuery.where(predicate);
    return entityManager.createQuery(criteriaQuery).setFirstResult(offset).setMaxResults(
        size).getResultList();
  }
}
