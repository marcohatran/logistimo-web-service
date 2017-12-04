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
 * Created by kumargaurav on 14/11/17.
 */

public class LSSocialLikeModel implements Serializable {

  private String objectId;

  private String objectType;

  private String contextId;

  private String contextType;

  private String timestamp;

  private String textSummary;

  private LSSocialLikerModel liker;

  private String[] tags;

  private Integer counts;

  private transient String user;

  private transient String contextAttribute;

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

  public String getContextType() {
    return contextType;
  }

  public void setContextType(String contextType) {
    this.contextType = contextType;
  }

  public String getTimestamp() {
    return timestamp;
  }

  public void setTimestamp(String timestamp) {
    this.timestamp = timestamp;
  }

  public String getTextSummary() {
    return textSummary;
  }

  public void setTextSummary(String textSummary) {
    this.textSummary = textSummary;
  }

  public LSSocialLikerModel getLiker() {
    return liker;
  }

  public void setLiker(LSSocialLikerModel liker) {
    this.liker = liker;
  }

  public String[] getTags() {
    return tags;
  }

  public void setTags(String[] tags) {
    this.tags = tags;
  }

  public Integer getCounts() {
    return counts;
  }

  public void setCounts(Integer counts) {
    this.counts = counts;
  }

  public String getUser() {
    return user;
  }

  public void setUser(String user) {
    this.user = user;
  }

  public String getContextAttribute() {
    return contextAttribute;
  }

  public void setContextAttribute(String contextAttribute) {
    this.contextAttribute = contextAttribute;
  }

  @Override
  public String toString() {
    return "LSSocialLikeModel{" +
        "objectId='" + objectId + '\'' +
        ", objectType='" + objectType + '\'' +
        ", contextId='" + contextId + '\'' +
        ", contextType='" + contextType + '\'' +
        ", timestamp='" + timestamp + '\'' +
        ", counts=" + counts +
        ", user='" + user + '\'' +
        '}';
  }
}
