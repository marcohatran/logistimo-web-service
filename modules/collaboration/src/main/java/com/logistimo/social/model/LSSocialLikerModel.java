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

import com.logistimo.models.MediaModel;

import java.io.Serializable;
import java.util.List;

/**
 * Created by kumargaurav on 14/11/17.
 */

public class LSSocialLikerModel implements Serializable {

  private String liker;

  private String createdOn;

  private String src;

  private String name;

  private String userTags;

  private List<MediaModel> userMedia;

  public String getLiker() {
    return liker;
  }

  public void setLiker(String liker) {
    this.liker = liker;
  }

  public String getCreatedOn() {
    return createdOn;
  }

  public void setCreatedOn(String createdOn) {
    this.createdOn = createdOn;
  }

  public String getSrc() {
    return src;
  }

  public void setSrc(String src) {
    this.src = src;
  }

  public String getName() {
    return name;
  }

  public void setName(String name) {
    this.name = name;
  }

  public String getUserTags() {
    return userTags;
  }

  public void setUserTags(String userTags) {
    this.userTags = userTags;
  }

  public List<MediaModel> getUserMedia() {
    return userMedia;
  }

  public void setUserMedia(List<MediaModel> userMedia) {
    this.userMedia = userMedia;
  }
}
