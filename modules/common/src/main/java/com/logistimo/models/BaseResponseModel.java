package com.logistimo.models;

import com.google.gson.annotations.SerializedName;

/**
 * Created by smriti on 08/02/18.
 */
public class BaseResponseModel {

  @SerializedName("updated_time")
  private String updatedTime;


  public String getUpdatedTime() {
    return updatedTime;
  }

  public void setUpdatedTime(String updatedTime) {
    this.updatedTime = updatedTime;
  }
}
