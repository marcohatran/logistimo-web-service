package com.logistimo.stockrebalancing.client.internal.models;

import com.google.gson.annotations.SerializedName;
import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import lombok.Builder;
import lombok.Data;

/**
 * Created by nitisha.khandelwal on 19/03/18.
 */

@Data
@Builder
public class Configuration {

  @SerializedName("source_triggers")
  private List<Map<String, String>> sourceTriggers;

  @SerializedName("destination_triggers")
  private List<Map<String, String>> destinationTriggers;

  @SerializedName("matching_strategy")
  private MatchingStrategy matchingStrategy;

  @SerializedName("handling_cost")
  private BigDecimal handlingCost;

  @SerializedName("transportation_cost")
  private BigDecimal transportationCost;

  @SerializedName("inventory_holding_cost")
  private BigDecimal inventoryHoldingCost;

  @SerializedName("material_tags")
  private List<String> materialTags;

  @SerializedName("transfer_matrix")
  private List<List<String>> transferMatrix;

  @Data
  @Builder
  public static class MatchingStrategy {

    private int distance;

    @SerializedName("minimum_required_percentage")
    private float minimumRequiredPercentage;
  }
}
