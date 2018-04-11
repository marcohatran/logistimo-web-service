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


import org.apache.commons.lang.builder.EqualsBuilder;
import org.apache.commons.lang.builder.HashCodeBuilder;
import org.apache.commons.lang.builder.ToStringBuilder;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

import java.io.Serializable;

/**
 * Created by nitisha.khandelwal on 11/08/17.
 */

public class OffsetBasedPageRequest implements Pageable, Serializable {

  private static final long serialVersionUID = -25822477129613575L;

  private int limit;
  private int offset;
  private final Sort sort;

  /**
   * Creates a new {@link OffsetBasedPageRequest} with sort parameters applied.
   *
   * @param offset zero-based offset.
   * @param limit  the size of the elements to be returned.
   * @param sort   can be {@literal null}.
   */

  public OffsetBasedPageRequest(int offset, int limit, Sort sort) {
    if (offset < 0) {
      throw new IllegalArgumentException("Offset index must not be less than zero!");
    }

    if (limit < 1) {
      throw new IllegalArgumentException("Limit must not be less than one!");
    }
    this.limit = limit;
    this.offset = offset;
    this.sort = sort;
  }

  /**
   * Creates a new {@link OffsetBasedPageRequest} with sort parameters applied.
   *
   * @param offset     zero-based offset.
   * @param limit      the size of the elements to be returned.
   * @param direction  the direction of the {@link Sort} to be specified, can be {@literal null}.
   * @param properties the properties to sort by, must not be {@literal null} or empty.
   */

  public OffsetBasedPageRequest(int offset, int limit, Sort.Direction direction,
                                String... properties) {
    this(offset, limit, new Sort(direction, properties));
  }

  /**
   * Creates a new {@link OffsetBasedPageRequest} with sort parameters applied.
   *
   * @param offset zero-based offset.
   * @param limit  the size of the elements to be returned.
   */

  public OffsetBasedPageRequest(int offset, int limit) {
    this(offset, limit, new Sort(Sort.Direction.ASC, "id"));
  }

  @Override
  public int getPageNumber() {
    return offset / limit;
  }

  @Override
  public int getPageSize() {
    return limit;
  }

  @Override
  public int getOffset() {
    return offset;
  }

  @Override
  public Sort getSort() {
    return sort;
  }

  @Override
  public Pageable next() {
    return new OffsetBasedPageRequest(getOffset() + getPageSize(), getPageSize(), getSort());
  }

  public OffsetBasedPageRequest previous() {
    return hasPrevious() ? new OffsetBasedPageRequest(getOffset() - getPageSize(), getPageSize(),
        getSort()) : this;
  }


  @Override
  public Pageable previousOrFirst() {
    return hasPrevious() ? previous() : first();
  }

  @Override
  public Pageable first() {
    return new OffsetBasedPageRequest(0, getPageSize(), getSort());
  }

  @Override
  public boolean hasPrevious() {
    return offset > limit;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }

    if (!(o instanceof OffsetBasedPageRequest)) {
      return false;
    }

    OffsetBasedPageRequest that = (OffsetBasedPageRequest) o;

    return new EqualsBuilder()
        .append(limit, that.limit)
        .append(offset, that.offset)
        .append(sort, that.sort)
        .isEquals();
  }

  @Override
  public int hashCode() {
    return new HashCodeBuilder(17, 37)
        .append(limit)
        .append(offset)
        .append(sort)
        .toHashCode();
  }

  @Override
  public String toString() {
    return new ToStringBuilder(this)
        .append("limit", limit)
        .append("offset", offset)
        .append("sort", sort)
        .toString();
  }

}
