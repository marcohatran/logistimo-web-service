package com.logistimo.jpa;

import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.Predicate;
import javax.persistence.criteria.Root;

/**
 * Created by nitisha.khandelwal on 28/03/18.
 */

public class OrSpecification<T> implements Specification<T> {

  private Specification<T> first;
  private Specification<T> second;

  public OrSpecification(Specification<T> first, Specification<T> second) {
    this.first = first;
    this.second = second;
  }

  @Override
  public Predicate toPredicate(Root<T> root, CriteriaBuilder cb) {
    return cb.or(first.toPredicate(root, cb), second.toPredicate(root, cb));
  }
}
