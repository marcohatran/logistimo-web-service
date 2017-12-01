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
import java.util.List;

/**
 * Created by kumargaurav on 09/11/17.
 */
public class LikeResponseModel implements Serializable{

  private String objectid;

  private String objectty;

  private String contextid;

  private String contextty;

  private String contxtattr;

  private List<LSSocialLikeModel> likes;

  private String activityid;

  private Integer likecount;

  public LikeResponseModel(String objectid, String objectty, String contextid,
                           String contextty, String activityid,
                           String contxtattr) {
    this.objectid = objectid;
    this.objectty = objectty;
    this.contextid = contextid;
    this.contextty = contextty;
    this.activityid = activityid;
    this.contxtattr = contxtattr;
  }

  public String getObjectid() {
    return objectid;
  }

  public void setObjectid(String objectid) {
    this.objectid = objectid;
  }

  public String getObjectty() {
    return objectty;
  }

  public void setObjectty(String objectty) {
    this.objectty = objectty;
  }

  public String getContextid() {
    return contextid;
  }

  public void setContextid(String contextid) {
    this.contextid = contextid;
  }

  public String getContextty() {
    return contextty;
  }

  public void setContextty(String contextty) {
    this.contextty = contextty;
  }

  public String getContxtattr() {
    return contxtattr;
  }

  public void setContxtattr(String contxtattr) {
    this.contxtattr = contxtattr;
  }

  public List<LSSocialLikeModel> getLikes() {
    return likes;
  }

  public void setLikes(List<LSSocialLikeModel> likes) {
    this.likes = likes;
  }

  public String getActivityid() {
    return activityid;
  }

  public void setActivityid(String activityid) {
    this.activityid = activityid;
  }

  public Integer getLikecount() {
    return likecount;
  }

  public void setLikecount(Integer likecount) {
    this.likecount = likecount;
  }
}
