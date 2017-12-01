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

package com.logistimo.social.model;

import java.io.Serializable;

/**
 * Created by kumargaurav on 21/11/17.
 */
public class LSGetLikeModel implements Serializable {

  private String objectId;

  private String objectType;

  private String contextId;

  private Boolean count;

  private Integer offset;

  private Integer size;

  public LSGetLikeModel(String objectId, String objectType, String contextId,
                        Integer offset, Integer size) {
    this(objectId, objectType, contextId, null, offset, size);
  }

  public LSGetLikeModel(String objectId, String objectType, String contextId,
                        Boolean count,
                        Integer offset, Integer size) {
    this.objectId = objectId;
    this.objectType = objectType;
    this.contextId = contextId;
    this.count = count;
    this.offset = offset;
    this.size = size;
  }

  public String getObjectId() {
    return objectId;
  }

  public void setObjectId(String objectId) {
    this.objectId = objectId;
  }

  public String getObjectType() {
    return objectType;
  }

  public void setObjectType(String objectType) {
    this.objectType = objectType;
  }

  public String getContextId() {
    return contextId;
  }

  public void setContextId(String contextId) {
    this.contextId = contextId;
  }

  public Boolean getCount() {
    return count;
  }

  public void setCount(Boolean count) {
    this.count = count;
  }

  public Integer getOffset() {
    return offset;
  }

  public void setOffset(Integer offset) {
    this.offset = offset;
  }

  public Integer getSize() {
    return size;
  }

  public void setSize(Integer size) {
    this.size = size;
  }
}
