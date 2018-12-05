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

package com.logistimo.utils;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;

/**
 * @author Mohan Raja.
 */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class Stream {

  public static <T, R> List<R> toList(Collection<T> list, Function<? super T, ? extends R> mapper) {
    return list.stream().map(mapper).collect(Collectors.toList());
  }

  public static <T, R> List<R> toListUsingMapper(Collection<T> list, Class<R> clz) {
    return list.stream().map(a -> ModelMapperUtil.map(a, clz)).collect(Collectors.toList());
  }

  public static <T, K, V> Map<K, V> toMap(Collection<T> list,
                                          Function<? super T, ? extends K> keyMapper,
                                          Function<? super T, ? extends V> valueMapper) {
    return list.stream().collect(Collectors.toMap(keyMapper, valueMapper));
  }

  public static <T, R> Map<R, T> toMap(Collection<T> list,
                                       Function<? super T, ? extends R> mapper) {
    return list.stream().collect(Collectors.toMap(mapper, Function.identity()));
  }
}
