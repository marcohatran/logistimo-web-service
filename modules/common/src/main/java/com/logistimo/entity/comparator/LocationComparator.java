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

package com.logistimo.entity.comparator;

import com.logistimo.entity.ILocation;

import java.util.Comparator;

/**
 * Created by kumargaurav on 16/07/17.
 */
public class LocationComparator implements Comparator<ILocation> {


  @Override
  public int compare(ILocation o1, ILocation o2) {
    if (o1.getCountry() != null && o2.getCountry() != null) {
      if (!o1.getCountry().equalsIgnoreCase(o2.getCountry())) {
        return 1;
      }
    }
    if (o1.getState() != null && o2.getState() != null) {
      if (!o1.getState().equalsIgnoreCase(o2.getState())) {
        return 1;
      }
    }
    if (o1.getDistrict() != null && o2.getDistrict() == null) {
      return 1;
    }
    if (o2.getDistrict() != null && o1.getDistrict() == null) {
      return 1;
    }
    if (o1.getDistrict() != null) {
      if (!o1.getDistrict().equalsIgnoreCase(o2.getDistrict())) {
        return 1;
      }
    }
    if (o1.getCity() != null && o2.getCity() != null) {
      if (!o1.getCity().equalsIgnoreCase(o2.getCity())) {
        return 1;
      }
    }
    return 0;
  }
}
