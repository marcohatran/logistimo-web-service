package com.logistimo.jpa;

import java.lang.reflect.ParameterizedType;
import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.Predicate;
import javax.persistence.criteria.Root;

/**
 * Created by nitisha.khandelwal on 28/03/18.
 */

public interface Specification<T> {

  default Specification<T> and(Specification<T> other) {
    return new AndSpecification<>(this, other);
  }

  default Specification<T> or(Specification<T> other) {
    return new OrSpecification<T>(this, other);
  }

  default Class<T> getType() {
//    ParameterizedType type = (ParameterizedType) this.getClass().getGenericSuperclass();

    ParameterizedType type = (ParameterizedType) ((Class<T>) this.getClass()
        .getGenericInterfaces()[0]).getGenericInterfaces()[0];
    return (Class<T>) type.getActualTypeArguments()[0];
  }

  Predicate toPredicate(Root<T> root, CriteriaBuilder cb);
}
