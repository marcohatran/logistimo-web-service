package com.logistimo.stockrebalancing.models;

import java.math.BigDecimal;
import lombok.Data;

/**
 * Created by nitisha.khandelwal on 27/03/18.
 */

@Data
public class RecommendedTransfersResponse {

  private String kioskId;

  private String name;

  private String city;

  private String district;

  private String state;

  private String triggerCode;

  private BigDecimal quantity;

  private BigDecimal benefit;

  private Integer count;
}
